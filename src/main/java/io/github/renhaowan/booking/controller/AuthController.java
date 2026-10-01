package io.github.renhaowan.booking.controller;

import io.github.renhaowan.booking.common.ApiResponse;
import io.github.renhaowan.booking.dto.LoginRequest;
import io.github.renhaowan.booking.dto.LoginResponse;
import io.github.renhaowan.booking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：POST /api/v1/auth/login 校验用户名密码并签发 2 小时有效的 JWT，
 * 后续请求以 {@code Authorization: Bearer <token>} 携带。
 */
@Tag(name = "认证", description = "用户登录与令牌签发")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录并签发访问令牌")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(authService.login(request.username(), request.password()));
    }
}
