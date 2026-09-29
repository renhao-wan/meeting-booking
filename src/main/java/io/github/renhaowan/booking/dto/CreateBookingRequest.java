package io.github.renhaowan.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 创建预订请求。
 */
public record CreateBookingRequest(

        @NotNull(message = "会议室 id 不能为空")
        Long roomId,

        @NotBlank(message = "会议主题不能为空")
        @Size(max = 128, message = "会议主题长度不能超过 128")
        String title,

        @NotNull(message = "开始时间不能为空")
        LocalDateTime startTime,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endTime,

        @NotBlank(message = "幂等键不能为空")
        @Size(max = 64, message = "幂等键长度不能超过 64")
        String idempotentKey) {
}
