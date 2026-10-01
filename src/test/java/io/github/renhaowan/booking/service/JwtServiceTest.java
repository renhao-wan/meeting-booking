package io.github.renhaowan.booking.service;

import io.github.renhaowan.booking.service.impl.JwtServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JWT 签发与解析单元测试（无外部依赖）。
 */
class JwtServiceTest {

    private final JwtService jwtService = new JwtServiceImpl("unit-test-secret", 2);

    @Test
    void generateThenParseRoundTrip() {
        String token = jwtService.generateToken("admin", 42L);

        assertEquals("admin", jwtService.parseUsername(token));
        assertEquals(42L, jwtService.parseUserId(token));
    }

    @Test
    void expiredTokenThrows() {
        // expire-hours < 0 保证签发即过期
        JwtService expiredService = new JwtServiceImpl("unit-test-secret", -1);
        String token = expiredService.generateToken("admin", 1L);

        assertThrows(ExpiredJwtException.class, () -> expiredService.parseToken(token));
    }

    @Test
    void tamperedTokenThrows() {
        String token = jwtService.generateToken("admin", 1L);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertNotEquals(token, tampered);
        assertThrows(JwtException.class, () -> jwtService.parseToken(tampered));
    }
}
