package io.github.renhaowan.booking.controller;

import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 存活探针：GET /ping 返回服务名、状态与当前时间，供容器/网关健康检查使用（免鉴权）。
 */
@RestController
public class PingController {

    public record PingResponse(String service, String status, OffsetDateTime time) {
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("meeting-booking", "ok", OffsetDateTime.now());
    }
}
