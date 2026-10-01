package io.github.renhaowan.booking.service;

import io.jsonwebtoken.Claims;

/**
 * JWT 签发与解析（jjwt 0.12 API，HS256）。
 *
 * <p>secret 通过 SHA-256 派生为 32 字节密钥，保证满足 HS256 的 256 位最低密钥长度要求。</p>
 */
public interface JwtService {

    /** 自定义 claim：用户 id。 */
    String CLAIM_UID = "uid";

    /**
     * 签发 JWT：subject = 用户名，附 uid claim，过期时间 = 签发时间 + expire-hours。
     */
    String generateToken(String username, Long userId);

    /**
     * 解析 token，返回 Claims；签名无效或已过期抛 {@link io.jsonwebtoken.JwtException} 体系异常。
     */
    Claims parseToken(String token);

    String parseUsername(String token);

    Long parseUserId(String token);
}
