package io.github.renhaowan.booking.controller;

import io.github.renhaowan.booking.common.ApiResponse;
import io.github.renhaowan.booking.dto.RoomResponse;
import io.github.renhaowan.booking.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 会议室接口。
 */
@Tag(name = "会议室", description = "可用会议室检索")
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(summary = "查询可用会议室",
            description = "返回 status=1 的会议室，可按容量下限与投影仪过滤；"
                    + "提供 startTime/endTime 时排除与已预订（BOOKED）时段重叠的会议室，date 缺省为当天")
    @GetMapping
    public ApiResponse<List<RoomResponse>> searchAvailable(
            @Parameter(description = "日期，ISO 格式 yyyy-MM-dd，缺省当天")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "开始时间，ISO 格式 HH:mm 或 HH:mm:ss")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,

            @Parameter(description = "结束时间，ISO 格式 HH:mm 或 HH:mm:ss")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,

            @Parameter(description = "容量下限（人）")
            @RequestParam(required = false) Integer capacity,

            @Parameter(description = "是否需要投影仪")
            @RequestParam(required = false) Boolean projector) {
        List<RoomResponse> rooms = roomService
                .searchAvailable(date, startTime, endTime, capacity, projector)
                .stream()
                .map(RoomResponse::from)
                .toList();
        return ApiResponse.ok(rooms);
    }
}
