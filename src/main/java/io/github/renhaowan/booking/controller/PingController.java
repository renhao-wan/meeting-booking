package io.github.renhaowan.booking.controller;

import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    public record PingResponse(String service, String status, OffsetDateTime time) {
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("meeting-booking", "ok", OffsetDateTime.now());
    }
}
