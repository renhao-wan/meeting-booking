package io.github.renhaowan.booking.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

/**
 * JWT 签发与解析（jjwt 0.12 API，HS256）。
 *
 * <p>secret 通过 SHA-256 派生为 32 字节密钥，保证满足 HS256 的 256 位最低密钥长度要求。</p>
 */
@Service
public class JwtService {

    /** 自定义 claim：用户 id。 */
    public static final String CLAIM_UID = "uid";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKey secretKey;

    private final long expireHours;

    public JwtService(@Value("${booking.jwt.secret}") String secret,
                      @Value("${booking.jwt.expire-hours}") long expireHours) {
        this.secretKey = deriveKey(secret);
        this.expireHours = expireHours;
    }

    /**
     * 签发 JWT：subject = 用户名，附 uid claim，过期时间 = 签发时间 + expire-hours。
     */
    public String generateToken(String username, Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_UID, userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token，返回 Claims；签名无效或已过期抛 {@link io.jsonwebtoken.JwtException} 体系异常。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String parseUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Long parseUserId(String token) {
        Object uid = parseToken(token).get(CLAIM_UID);
        return uid instanceof Number number ? number.longValue() : null;
    }

    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用，无法派生 JWT 密钥", e);
        }
    }
}
