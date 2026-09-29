package io.github.renhaowan.booking.security;

import io.github.renhaowan.booking.common.ErrorCode;
import io.github.renhaowan.booking.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Component
public class JwtFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "userId";

    public static final String ATTR_USERNAME = "username";

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    /** 服务级凭证：MCP 网关泛化转发时携带（X-Service-Key），以系统账号（userId=1）身份执行 */
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

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 服务级凭证：网关代业务系统调用（如 Agent 工具执行）以系统账号身份放行
        String serviceHeaderValue = request.getHeader(SERVICE_KEY_HEADER);
        if (serviceKey != null && !serviceKey.isBlank() && serviceKey.equals(serviceHeaderValue)) {
            request.setAttribute(ATTR_USER_ID, SYSTEM_USER_ID);
            request.setAttribute(ATTR_USERNAME, SYSTEM_USERNAME);
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            unauthorized(response, "缺少 Bearer 访问令牌");
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Long userId = jwtService.parseUserId(token);
            String username = jwtService.parseUsername(token);
            if (userId == null || username == null) {
                unauthorized(response, "访问令牌缺少用户信息");
                return;
            }
            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_USERNAME, username);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            unauthorized(response, "访问令牌无效或已过期");
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
