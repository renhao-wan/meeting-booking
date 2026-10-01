package io.github.renhaowan.booking.controller;

import io.github.renhaowan.booking.common.ApiResponse;
import io.github.renhaowan.booking.dto.BookingResponse;
import io.github.renhaowan.booking.dto.CreateBookingRequest;
import io.github.renhaowan.booking.security.JwtFilter;
import io.github.renhaowan.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会议预订接口。
 */
@Tag(name = "会议预订", description = "预订创建、取消与查询")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "创建会议室预订")
    @PostMapping
    public ApiResponse<BookingResponse> create(
            @RequestAttribute(JwtFilter.ATTR_USER_ID) Long userId,
            @RequestBody @Valid CreateBookingRequest request) {
        // 组装时间与语义幂等键：幂等键 = SHA-256(userId|roomId|date|start|end|title)，
        // 同一用户对同一时段同一主题的重复提交天然幂等，调用方（含大模型）无需自行生成
        java.time.LocalDateTime start = java.time.LocalDate.parse(request.date())
                .atTime(java.time.LocalTime.parse(request.startTime()));
        java.time.LocalDateTime end = java.time.LocalDate.parse(request.date())
                .atTime(java.time.LocalTime.parse(request.endTime()));
        String idempotentKey = idempotentKey(userId, request.roomId(), request.date(),
                request.startTime(), request.endTime(), request.title());
        BookingResponse response = BookingResponse.from(bookingService.create(
                userId, request.roomId(), request.title(), start, end, idempotentKey));
        return ApiResponse.ok(response);
    }

    private String idempotentKey(Long userId, Long roomId, String date, String start, String end, String title) {
        String material = userId + "|" + roomId + "|" + date + "|" + start + "|" + end + "|" + title;
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Operation(summary = "取消指定预订")
    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancel(
            @RequestAttribute(JwtFilter.ATTR_USER_ID) Long userId,
            @Parameter(description = "预订 id") @PathVariable("id") Long id) {
        return ApiResponse.ok(BookingResponse.from(bookingService.cancel(id, userId)));
    }

    @Operation(summary = "查询我的预订列表")
    @GetMapping
    public ApiResponse<List<BookingResponse>> list(
            @RequestAttribute(JwtFilter.ATTR_USER_ID) Long userId) {
        List<BookingResponse> bookings = bookingService.listByUser(userId)
                .stream()
                .map(BookingResponse::from)
                .toList();
        return ApiResponse.ok(bookings);
    }

    @Operation(summary = "查询预订详情")
    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> get(
            @RequestAttribute(JwtFilter.ATTR_USER_ID) Long userId,
            @Parameter(description = "预订 id") @PathVariable("id") Long id) {
        return ApiResponse.ok(BookingResponse.from(bookingService.getOwned(id, userId)));
    }
}
