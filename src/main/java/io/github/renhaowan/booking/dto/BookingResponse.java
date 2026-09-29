package io.github.renhaowan.booking.dto;

import io.github.renhaowan.booking.domain.Booking;
import io.github.renhaowan.booking.domain.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 预订响应。
 */
@Schema(description = "会议预订")
public record BookingResponse(
        @Schema(description = "预订 id") Long id,
        @Schema(description = "会议室 id") Long roomId,
        @Schema(description = "预订人 id") Long userId,
        @Schema(description = "会议主题") String title,
        @Schema(description = "开始时间") LocalDateTime startTime,
        @Schema(description = "结束时间") LocalDateTime endTime,
        @Schema(description = "状态：BOOKED / CANCELLED / COMPLETED") BookingStatus status,
        @Schema(description = "乐观锁版本号") Integer version,
        @Schema(description = "幂等键") String idempotentKey,
        @Schema(description = "创建时间") LocalDateTime createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getRoomId(), booking.getUserId(),
                booking.getTitle(), booking.getStartTime(), booking.getEndTime(),
                booking.getStatus(), booking.getVersion(), booking.getIdempotentKey(),
                booking.getCreatedAt());
    }
}
