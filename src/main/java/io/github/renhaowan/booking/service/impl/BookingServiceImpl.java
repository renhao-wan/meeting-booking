package io.github.renhaowan.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.common.ErrorCode;
import io.github.renhaowan.booking.common.Messages;
import io.github.renhaowan.booking.dao.BookingMapper;
import io.github.renhaowan.booking.dao.RoomMapper;
import io.github.renhaowan.booking.domain.Booking;
import io.github.renhaowan.booking.domain.BookingStatus;
import io.github.renhaowan.booking.domain.Room;
import io.github.renhaowan.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * {@link BookingService} 默认实现：幂等创建、状态机取消与乐观锁写入重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    /** 写入最大尝试次数：首次 + 重试 1 次。 */
    static final int MAX_WRITE_ATTEMPTS = 2;

    private final BookingMapper bookingMapper;

    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public Booking create(Long userId, Long roomId, String title,
                          LocalDateTime startTime, LocalDateTime endTime, String idempotentKey) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, Messages.TIME_ORDER_INVALID);
        }

        // ① 幂等预检：同幂等键的既有记录直接判定为重放
        Booking existing = selectByIdempotentKey(idempotentKey);
        if (existing != null) {
            log.info("幂等重放：idempotentKey={}, existingBookingId={}", idempotentKey, existing.getId());
            throw new BusinessException(ErrorCode.IDEMPOTENT_REPLAY, Messages.IDEMPOTENT_REPLAY, existing);
        }

        // ② 房间存在校验
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND, String.format(Messages.ROOM_NOT_FOUND, roomId));
        }

        // ③ 时段重叠校验（重叠条件：existing.start < end AND existing.end > start）
        if (hasTimeConflict(roomId, startTime, endTime)) {
            throw new BusinessException(ErrorCode.TIME_CONFLICT, Messages.TIME_CONFLICT);
        }

        // ④ 乐观锁插入
        Booking booking = new Booking();
        booking.setRoomId(roomId);
        booking.setUserId(userId);
        booking.setTitle(title);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(BookingStatus.BOOKED);
        booking.setVersion(0);
        booking.setIdempotentKey(idempotentKey);
        booking.setCreatedAt(LocalDateTime.now());
        return insertWithRetry(booking);
    }

    @Override
    @Transactional
    public Booking cancel(Long bookingId, Long userId) {
        Booking booking = loadCancellable(bookingId, userId);
        for (int attempt = 1; attempt <= MAX_WRITE_ATTEMPTS; attempt++) {
            booking.setStatus(BookingStatus.CANCELLED);
            if (bookingMapper.updateById(booking) > 0) {
                log.info("预订取消成功：bookingId={}, userId={}", bookingId, userId);
                return booking;
            }
            log.warn("取消乐观锁冲突：bookingId={}, attempt={}", bookingId, attempt);
            if (attempt < MAX_WRITE_ATTEMPTS) {
                // 乐观锁冲突：重读最新版本并重新校验状态机后再试
                booking = loadCancellable(bookingId, userId);
            }
        }
        throw new BusinessException(ErrorCode.TIME_CONFLICT, Messages.CANCEL_WRITE_CONFLICT);
    }

    @Override
    public List<Booking> listByUser(Long userId) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, userId)
                .orderByDesc(Booking::getCreatedAt));
    }

    @Override
    public Booking getOwned(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || !Objects.equals(booking.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND, String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }
        return booking;
    }

    /** 按幂等键精确查询既有预订；命中即视为重放请求。 */
    private Booking selectByIdempotentKey(String idempotentKey) {
        return bookingMapper.selectOne(
                new LambdaQueryWrapper<Booking>().eq(Booking::getIdempotentKey, idempotentKey));
    }

    /**
     * 时段重叠判定：存在 BOOKED 预订满足 {@code existing.start_time < endTime AND existing.end_time > startTime}
     * 即视为冲突（半开区间 [start, end)，首尾相接的两个时段不冲突）。
     *
     * <p>并发考虑：该判定与后续插入之间没有数据库级串行化保证，两个并发请求可能同时
     * 判定"无冲突"并各自插入；此后由幂等键唯一索引拦截重复请求，
     * 由 @Version 乐观锁拦截并发更新，重叠校验失败兜底为 1002，不产生双写成功。</p>
     */
    private boolean hasTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        Long count = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRoomId, roomId)
                .eq(Booking::getStatus, BookingStatus.BOOKED)
                .lt(Booking::getStartTime, endTime)
                .gt(Booking::getEndTime, startTime));
        return count != null && count > 0;
    }

    /**
     * 乐观锁插入，带有限次重试：
     * <ul>
     *   <li>插入成功 → 直接返回（MyBatis-Plus 回填自增 id 与递增后的 version）；</li>
     *   <li>{@link DuplicateKeyException} → 幂等键唯一索引冲突，属并发重放，
     *       回读既有记录并归一为 1004（与幂等预检分支同一语义、同一文案）；</li>
     *   <li>其他 {@link DataAccessException}（如死锁回滚）→ 记录告警后重试，
     *       重试耗尽仍失败则归一为 1002。</li>
     * </ul>
     */
    private Booking insertWithRetry(Booking booking) {
        for (int attempt = 1; attempt <= MAX_WRITE_ATTEMPTS; attempt++) {
            try {
                if (bookingMapper.insert(booking) > 0) {
                    log.info("预订创建成功：id={}, room={}, user={}, key={}",
                            booking.getId(), booking.getRoomId(), booking.getUserId(), booking.getIdempotentKey());
                    return booking;
                }
                log.warn("预订写入影响行数为 0：attempt={}", attempt);
            } catch (DuplicateKeyException e) {
                // 幂等键唯一索引冲突：并发重放场景，归一为 1004 并携带既有记录
                Booking existing = selectByIdempotentKey(booking.getIdempotentKey());
                log.info("幂等键并发冲突：key={}", booking.getIdempotentKey());
                throw new BusinessException(ErrorCode.IDEMPOTENT_REPLAY, Messages.IDEMPOTENT_REPLAY, existing);
            } catch (DataAccessException e) {
                log.warn("预订写入失败：attempt={}, cause={}", attempt, e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.TIME_CONFLICT, Messages.CREATE_WRITE_CONFLICT);
    }

    /**
     * 加载一条"可取消"的预订：存在 → 是本人 → 状态机允许流转到 CANCELLED，任一不满足即抛业务异常。
     * 取消重试路径复用该方法，保证乐观锁冲突重读后仍重新过一遍全部校验。
     */
    private Booking loadCancellable(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND, String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }
        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.BOOKING_FORBIDDEN, Messages.CANCEL_FORBIDDEN);
        }
        if (!booking.getStatus().canTransitTo(BookingStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    String.format(Messages.STATUS_NOT_CANCELLABLE, booking.getStatus()));
        }
        return booking;
    }
}
