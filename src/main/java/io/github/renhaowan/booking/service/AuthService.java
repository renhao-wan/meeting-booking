package io.github.renhaowan.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.common.ErrorCode;
import io.github.renhaowan.booking.dao.UserMapper;
import io.github.renhaowan.booking.domain.User;
import io.github.renhaowan.booking.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 认证服务：用户名密码校验（SHA-256 摘要比对）并签发 JWT。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;

    private final JwtService jwtService;

    /**
     * 登录：校验用户名与 SHA-256 口令摘要，成功签发 JWT（subject=用户名，2 小时过期）。
     *
     * @throws BusinessException 1000 用户名或密码错误
     */
    public LoginResponse login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !Objects.equals(user.getPasswordHash(), sha256Hex(password))) {
            log.info("登录失败：username={}", username);
            throw new BusinessException(ErrorCode.AUTH_FAILED, "用户名或密码错误");
        }
        String token = jwtService.generateToken(user.getUsername(), user.getId());
        log.info("登录成功：username={}", username);
        return new LoginResponse(token);
    }

    static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
