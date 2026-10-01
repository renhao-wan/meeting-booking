package io.github.renhaowan.booking.service.impl;

import io.github.renhaowan.booking.service.JwtService;
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
 * {@link JwtService} 默认实现：jjwt 0.12 API（HS256），secret 经 SHA-256 派生 32 字节密钥。
 */
@Service
public class JwtServiceImpl implements JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** 密钥派生摘要算法：将任意长度口令摘要为 32 字节以满足 HS256 密钥长度要求。 */
    private static final String SHA256_ALGORITHM = "SHA-256";

    private final SecretKey secretKey;

    private final long expireHours;

    public JwtServiceImpl(@Value("${booking.jwt.secret}") String secret,
                          @Value("${booking.jwt.expire-hours}") long expireHours) {
        this.secretKey = deriveKey(secret);
        this.expireHours = expireHours;
    }

    @Override
    public String generateToken(String username, Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(JwtService.CLAIM_UID, userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public String parseUsername(String token) {
        return parseToken(token).getSubject();
    }

    @Override
    public Long parseUserId(String token) {
        Object uid = parseToken(token).get(JwtService.CLAIM_UID);
        return uid instanceof Number number ? number.longValue() : null;
    }

    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用，无法派生 JWT 密钥", e);
        }
    }
}
