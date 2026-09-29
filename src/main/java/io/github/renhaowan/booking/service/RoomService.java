package io.github.renhaowan.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.common.ErrorCode;
import io.github.renhaowan.booking.dao.BookingMapper;
import io.github.renhaowan.booking.dao.RoomMapper;
import io.github.renhaowan.booking.domain.Booking;
import io.github.renhaowan.booking.domain.BookingStatus;
import io.github.renhaowan.booking.domain.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会议室服务：可用会议室检索。
 *
 * <p>重叠条件与预订侧保持一致：existing.start &lt; :end AND existing.end &gt; :start（半开区间）。</p>
 */
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomMapper roomMapper;

    private final BookingMapper bookingMapper;

    public RoomService(RoomMapper roomMapper, BookingMapper bookingMapper) {
        this.roomMapper = roomMapper;
        this.bookingMapper = bookingMapper;
    }

    /**
     * 检索可用会议室（status=1），可按容量下限与投影仪过滤；
     * 若提供时间窗口，则排除与 status=BOOKED 预订时段重叠的会议室。
     *
     * @param date       日期（与 startTime/endTime 配合使用，缺省当天）
     * @param startTime  开始时间（含）
     * @param endTime    结束时间（不含）
     * @param minCapacity 容量下限，可空
     * @param projector  是否需要投影仪，可空
     */
    public List<Room> searchAvailable(LocalDate date, LocalTime startTime, LocalTime endTime,
                                      Integer minCapacity, Boolean projector) {
        boolean hasWindow = startTime != null || endTime != null;
        if (hasWindow && (startTime == null || endTime == null)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "startTime 与 endTime 必须同时提供");
        }
        if (hasWindow && !startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "开始时间必须早于结束时间");
        }

        LambdaQueryWrapper<Room> query = new LambdaQueryWrapper<>();
        query.eq(Room::getStatus, Room.STATUS_AVAILABLE);
        if (minCapacity != null) {
            query.ge(Room::getCapacity, minCapacity);
        }
        if (projector != null) {
            query.eq(Room::getHasProjector, projector);
        }
        List<Room> rooms = roomMapper.selectList(query);

        if (rooms.isEmpty() || !hasWindow) {
            return rooms;
        }

        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        LocalDateTime windowStart = effectiveDate.atTime(startTime);
        LocalDateTime windowEnd = effectiveDate.atTime(endTime);
        Set<Long> conflictedRoomIds = findBookedRoomIds(
                rooms.stream().map(Room::getId).toList(), windowStart, windowEnd);
        log.debug("可用检索：候选 {} 间，时段冲突排除 {} 间", rooms.size(), conflictedRoomIds.size());
        return rooms.stream()
                .filter(room -> !conflictedRoomIds.contains(room.getId()))
                .toList();
    }

    /**
     * 查询给定会议室中，与 [windowStart, windowEnd) 存在 BOOKED 重叠的会议室 id 集合。
     */
    private Set<Long> findBookedRoomIds(List<Long> roomIds, LocalDateTime windowStart, LocalDateTime windowEnd) {
        LambdaQueryWrapper<Booking> query = new LambdaQueryWrapper<>();
        query.select(Booking::getRoomId)
                .in(Booking::getRoomId, roomIds)
                .eq(Booking::getStatus, BookingStatus.BOOKED)
                .lt(Booking::getStartTime, windowEnd)
                .gt(Booking::getEndTime, windowStart);
        return bookingMapper.selectList(query).stream()
                .map(Booking::getRoomId)
                .collect(Collectors.toSet());
    }
}
