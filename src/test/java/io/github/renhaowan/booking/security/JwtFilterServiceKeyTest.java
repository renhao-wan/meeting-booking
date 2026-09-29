package io.github.renhaowan.booking.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.renhaowan.booking.service.JwtService;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtFilterServiceKeyTest {

    private final JwtService jwtService = Mockito.mock(JwtService.class);
    private final JwtFilter filter = new JwtFilter(jwtService);

    private record Outcome(int status, Object userId) {
    }

    private Outcome run(String serviceKeyHeader, String bearer) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/rooms");
        if (serviceKeyHeader != null) {
            request.addHeader("X-Service-Key", serviceKeyHeader);
        }
        if (bearer != null) {
            request.addHeader("Authorization", "Bearer " + bearer);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Outcome(response.getStatus(),
                chain.getRequest() == null ? null : chain.getRequest().getAttribute(JwtFilter.ATTR_USER_ID));
    }

    @Test
    void validServiceKeyBypassesJwtAsSystemAccount() throws ServletException, IOException {
        Outcome outcome = run("booking-service-key", null);

        assertEquals(200, outcome.status());
        assertEquals(1L, outcome.userId());
        Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void wrongServiceKeyFallsBackToJwtCheck() throws ServletException, IOException {
        Outcome outcome = run("wrong-key", null);

        assertEquals(401, outcome.status());
        assertNull(outcome.userId());
    }

    @Test
    void noCredentialsRejected() throws ServletException, IOException {
        assertEquals(401, run(null, null).status());
    }
}
