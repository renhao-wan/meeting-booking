package io.github.renhaowan.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.common.ErrorCode;
import io.github.renhaowan.booking.common.Messages;
import io.github.renhaowan.booking.dao.UserMapper;
import io.github.renhaowan.booking.domain.User;
import io.github.renhaowan.booking.dto.LoginResponse;
import io.github.renhaowan.booking.service.AuthService;
import io.github.renhaowan.booking.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * {@link AuthService} 默认实现：SHA-256 摘要比对与 JWT 签发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 口令摘要算法。 */
    private static final String SHA256_ALGORITHM = "SHA-256";

    private final UserMapper userMapper;

    private final JwtService jwtService;

    @Override
    public LoginResponse login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !Objects.equals(user.getPasswordHash(), sha256Hex(password))) {
            log.info("登录失败：username={}", username);
            throw new BusinessException(ErrorCode.AUTH_FAILED, Messages.AUTH_FAILED);
        }
        String token = jwtService.generateToken(user.getUsername(), user.getId());
        log.info("登录成功：username={}", username);
        return new LoginResponse(token);
    }

    /**
     * 计算口令的 SHA-256 十六进制摘要（供单测同源构造期望摘要，故声明为 public）。
     */
    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
