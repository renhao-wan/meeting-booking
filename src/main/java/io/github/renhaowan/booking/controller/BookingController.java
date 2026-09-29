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
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "创建会议室预订")
    @PostMapping
    public ApiResponse<BookingResponse> create(
            @RequestAttribute(JwtFilter.ATTR_USER_ID) Long userId,
            @RequestBody @Valid CreateBookingRequest request) {
        BookingResponse response = BookingResponse.from(bookingService.create(
                userId,
                request.roomId(),
                request.title(),
                request.startTime(),
                request.endTime(),
                request.idempotentKey()));
        return ApiResponse.ok(response);
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
