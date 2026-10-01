package io.github.renhaowan.booking.service;

import io.github.renhaowan.booking.domain.Room;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 会议室服务：可用会议室检索。
 *
 * <p>重叠条件与预订侧保持一致：existing.start &lt; :end AND existing.end &gt; :start（半开区间）。</p>
 */
public interface RoomService {

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
    List<Room> searchAvailable(LocalDate date, LocalTime startTime, LocalTime endTime,
                               Integer minCapacity, Boolean projector);
}
