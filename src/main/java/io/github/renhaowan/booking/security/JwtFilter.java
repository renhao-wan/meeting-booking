package io.github.renhaowan.booking.security;

import io.github.renhaowan.booking.common.ErrorCode;
import io.github.renhaowan.booking.common.Messages;
import io.github.renhaowan.booking.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 鉴权过滤器：拦截 /api/v1/**，放行登录与文档端点；
 * 校验 Bearer JWT 后将 userId 注入 request attribute。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "userId";

    public static final String ATTR_USERNAME = "username";

    private static final String BEARER_PREFIX = "Bearer ";

    /** 服务级凭证配置项：booking.service-key，生产环境应通过环境变量注入强随机值。 */
    @org.springframework.beans.factory.annotation.Value("${booking.service-key:booking-service-key}")
    private String serviceKey;

    public static final String SERVICE_KEY_HEADER = "X-Service-Key";
    public static final long SYSTEM_USER_ID = 1L;
    public static final String SYSTEM_USERNAME = "service";

    /** 免鉴权路径前缀。 */
    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/v3/api-docs",
            "/swagger-ui"
    );

    /** 免鉴权精确路径。 */
    private static final List<String> WHITELIST_EXACT = List.of(
            "/ping",
            "/api/v1/auth/login",
            "/swagger-ui.html"
    );

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 服务级凭证分支：X-Service-Key 是给上游 MCP 网关转发用的系统级凭证，与用户 JWT 并行的另一条鉴权通道。
        // 适用场景：网关代业务系统（或 Agent 工具执行）调用本服务时不宜持有任何真实用户的 JWT，
        // 携带匹配的 X-Service-Key 即以系统账号（userId=1，username=service）身份放行，
        // 请求 attribute 注入方式与 JWT 分支一致，下游代码无感知。
        // 安全要求：生产环境必须将 booking.service-key（对应环境变量 BOOKING_SERVICE_KEY）
        // 改为强随机值并从环境变量注入，禁止沿用代码/配置中的开发默认值。
        String serviceHeaderValue = request.getHeader(SERVICE_KEY_HEADER);
        if (serviceKey != null && !serviceKey.isBlank() && serviceKey.equals(serviceHeaderValue)) {
            request.setAttribute(ATTR_USER_ID, SYSTEM_USER_ID);
            request.setAttribute(ATTR_USERNAME, SYSTEM_USERNAME);
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            unauthorized(response, Messages.MISSING_BEARER_TOKEN);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Long userId = jwtService.parseUserId(token);
            String username = jwtService.parseUsername(token);
            if (userId == null || username == null) {
                unauthorized(response, Messages.TOKEN_MISSING_IDENTITY);
                return;
            }
            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_USERNAME, username);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            unauthorized(response, Messages.TOKEN_INVALID);
        }
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST_EXACT.contains(path)
                || WHITELIST_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"code\":" + ErrorCode.UNAUTHORIZED + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
