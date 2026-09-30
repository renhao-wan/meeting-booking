package io.github.renhaowan.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建预订请求（对大模型友好的契约：日期与时分分离，避免 ISO 格式歧义；
 * 幂等键由服务端从操作语义派生，不要求调用方生成）。
 */
public record CreateBookingRequest(

        @NotNull(message = "会议室 id 不能为空")
        Long roomId,

        @NotBlank(message = "会议主题不能为空")
        @Size(max = 128, message = "会议主题长度不能超过 128")
        String title,

        @NotBlank(message = "日期不能为空")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式应为 yyyy-MM-dd")
        String date,

        @NotBlank(message = "开始时间不能为空")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "开始时间格式应为 HH:mm")
        String startTime,

        @NotBlank(message = "结束时间不能为空")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "结束时间格式应为 HH:mm")
        String endTime) {
}
