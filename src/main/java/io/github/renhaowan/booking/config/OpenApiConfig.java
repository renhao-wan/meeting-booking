package io.github.renhaowan.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc OpenAPI 文档配置：接口元信息 + Bearer JWT 安全方案。
 *
 * <p>敏感操作（如取消预订）已在对应 Controller 的 @Operation 注解中标注动作语义与说明。</p>
 */
@Configuration
public class OpenApiConfig {

    /** Swagger UI 全局安全方案名称，与 BookingController 上 @SecurityRequirement 对应。 */
    public static final String SECURITY_SCHEME_BEARER_JWT = "bearer-jwt";

    @Bean
    public OpenAPI meetingBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("meeting-booking API")
                        .description("会议预约业务单体：认证、会议室检索、预订创建/取消/查询。"
                                + "除登录与文档端点外均需携带 Authorization: Bearer &lt;JWT&gt;。")
                        .version("0.1.0"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_BEARER_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("通过 POST /api/v1/auth/login 获取，有效期 2 小时")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_BEARER_JWT));
    }
}
