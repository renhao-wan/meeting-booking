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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 会议预订接口：创建（服务端派生语义幂等键）、取消、本人列表与详情查询。
 *
 * <p>所有接口要求鉴权上下文注入的 {@code userId}（Bearer JWT 或上游服务级凭证），
 * 响应统一包裹 {@link ApiResponse}，错误码见 {@link io.github.renhaowan.booking.common.ErrorCode}。</p>
 */
@Tag(name = "会议预订", description = "预订创建、取消与查询")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    /** 幂等键派生使用的摘要算法。 */
    private static final String SHA256_ALGORITHM = "SHA-256";

    /** 幂等键派生材料各字段间的分隔符。 */
    private static final String KEY_DELIMITER = "|";

    /** 幂等键截取长度（十六进制字符数），映射数据库 idempotent_key 列宽。 */
    private static final int IDEMPOTENT_KEY_HEX_LENGTH = 32;

    private final BookingService bookingService;

    /**
     * 创建会议室预订。日期与起止时分按契约分离传输，此处组装为时间戳；
     * 幂等键在服务端从操作语义派生（见 {@link #idempotentKey}），调用方无需自行生成。
     */
    @Operation(summary = "创建会议室预订")
    @PostMapping
    public ApiResponse<BookingResponse> create(
            @RequestAttribute(JwtFilter.ATTR_USER_ID) Long userId,
            @RequestBody @Valid CreateBookingRequest request) {
        LocalDateTime start = LocalDate.parse(request.date()).atTime(LocalTime.parse(request.startTime()));
        LocalDateTime end = LocalDate.parse(request.date()).atTime(LocalTime.parse(request.endTime()));
        String idempotentKey = idempotentKey(userId, request.roomId(), request.date(),
                request.startTime(), request.endTime(), request.title());
        BookingResponse response = BookingResponse.from(bookingService.create(
                userId, request.roomId(), request.title(), start, end, idempotentKey));
        return ApiResponse.ok(response);
    }

    /**
     * 派生语义幂等键：{@code SHA-256(userId|roomId|date|start|end|title) 截取前 32 个十六进制字符}。
     *
     * <p>规则：幂等键只由"谁、在哪个会议室、哪个时段、开什么会"这些操作语义决定，
     * 与请求到达的先后、请求头等传输细节无关——同一用户对同一时段同一主题的重复提交
     * （网络重发、大模型重试等）天然得到相同键，命中服务端幂等分支（1004 + 既有记录）。</p>
     *
     * <p>并发考虑：两个相同键的请求可能同时通过幂等预检，最终由数据库
     * idempotent_key 唯一索引兜底，并发冲突在服务层归一为同一幂等重放语义。</p>
     */
    private String idempotentKey(Long userId, Long roomId, String date, String start, String end, String title) {
        String material = userId + KEY_DELIMITER + roomId + KEY_DELIMITER + date + KEY_DELIMITER
                + start + KEY_DELIMITER + end + KEY_DELIMITER + title;
        try {
            byte[] digest = MessageDigest.getInstance(SHA256_ALGORITHM)
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, IDEMPOTENT_KEY_HEX_LENGTH);
        } catch (NoSuchAlgorithmException e) {
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
