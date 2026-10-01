package io.github.renhaowan.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.common.ErrorCode;
import io.github.renhaowan.booking.dao.BookingMapper;
import io.github.renhaowan.booking.dao.RoomMapper;
import io.github.renhaowan.booking.domain.Booking;
import io.github.renhaowan.booking.domain.BookingStatus;
import io.github.renhaowan.booking.domain.Room;
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
 * 预订服务：创建（幂等 + 冲突校验 + 乐观锁写入重试）与取消（状态机 + 仅本人 + 乐观锁更新）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    /** 写入最大尝试次数：首次 + 重试 1 次。 */
    static final int MAX_WRITE_ATTEMPTS = 2;

    private final BookingMapper bookingMapper;

    private final RoomMapper roomMapper;

    /**
     * 创建预订，事务内执行：
     * <ol>
     *   <li>① 幂等：同 idempotentKey 既有记录 → 1004 携带既有记录（插入时唯一键冲突亦归一为 1004）</li>
     *   <li>② 房间存在校验 → 1001</li>
     *   <li>③ 时段重叠校验（仅 BOOKED）→ 1002</li>
     *   <li>④ 乐观锁插入（@Version），写入失败重试 1 次后仍失败 → 1002</li>
     * </ol>
     */
    @Transactional
    public Booking create(Long userId, Long roomId, String title,
                          LocalDateTime startTime, LocalDateTime endTime, String idempotentKey) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "开始时间必须早于结束时间");
        }

        // ① 幂等预检：同幂等键的既有记录直接判定为重放
        Booking existing = selectByIdempotentKey(idempotentKey);
        if (existing != null) {
            log.info("幂等重放：idempotentKey={}, existingBookingId={}", idempotentKey, existing.getId());
            throw new BusinessException(ErrorCode.IDEMPOTENT_REPLAY, "重复请求：相同幂等键的预订已存在", existing);
        }

        // ② 房间存在校验
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND, "会议室不存在: " + roomId);
        }

        // ③ 时段重叠校验（重叠条件：existing.start < end AND existing.end > start）
        if (hasTimeConflict(roomId, startTime, endTime)) {
            throw new BusinessException(ErrorCode.TIME_CONFLICT, "该会议室在所选时段已被预订");
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

    /**
     * 取消预订：仅 BOOKED 可取消（状态机），仅本人可取消，乐观锁更新 status=CANCELLED。
     */
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
        throw new BusinessException(ErrorCode.TIME_CONFLICT, "取消失败：并发修改冲突，请稍后重试");
    }

    /** 当前用户的预订列表，按创建时间倒序。 */
    public List<Booking> listByUser(Long userId) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, userId)
                .orderByDesc(Booking::getCreatedAt));
    }

    /** 查询本人预订详情；不存在或非本人的记录一律按不存在处理。 */
    public Booking getOwned(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || !Objects.equals(booking.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "预订不存在: " + bookingId);
        }
        return booking;
    }

    private Booking selectByIdempotentKey(String idempotentKey) {
        return bookingMapper.selectOne(
                new LambdaQueryWrapper<Booking>().eq(Booking::getIdempotentKey, idempotentKey));
    }

    private boolean hasTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        Long count = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRoomId, roomId)
                .eq(Booking::getStatus, BookingStatus.BOOKED)
                .lt(Booking::getStartTime, endTime)
                .gt(Booking::getEndTime, startTime));
        return count != null && count > 0;
    }

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
                throw new BusinessException(ErrorCode.IDEMPOTENT_REPLAY, "重复请求：相同幂等键的预订已存在", existing);
            } catch (DataAccessException e) {
                log.warn("预订写入失败：attempt={}, cause={}", attempt, e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.TIME_CONFLICT, "预订写入冲突，请稍后重试");
    }

    private Booking loadCancellable(Long bookingId, Long userId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "预订不存在: " + bookingId);
        }
        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.BOOKING_FORBIDDEN, "仅预订人本人可取消预订");
        }
        if (!booking.getStatus().canTransitTo(BookingStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "预订状态为 " + booking.getStatus() + "，不允许取消");
        }
        return booking;
    }
}
