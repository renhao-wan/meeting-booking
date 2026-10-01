package io.github.renhaowan.booking.service;

import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.dto.LoginResponse;

/**
 * 认证服务：用户名密码校验（SHA-256 摘要比对）并签发 JWT。
 */
public interface AuthService {

    /**
     * 登录：校验用户名与 SHA-256 口令摘要，成功签发 JWT（subject=用户名，2 小时过期）。
     *
     * @throws BusinessException 1000 用户名或密码错误
     */
    LoginResponse login(String username, String password);
}
