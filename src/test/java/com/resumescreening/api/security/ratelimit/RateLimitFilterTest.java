package com.resumescreening.api.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.resumescreening.api.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RateLimitFilter filter = new RateLimitFilter(rateLimitService, jwtUtil, objectMapper);

    @Test
    void exceededLimitReturnsTooManyRequestsWithHeaders() throws Exception {
        when(rateLimitService.checkLimit(eq("ip"), eq("127.0.0.1"), any(RateLimitRule.class)))
                .thenReturn(new RateLimitDecision(false, 5, 0, 30));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
    }

    @Test
    void authenticatedRequestsUseUserSubjectKey() throws Exception {
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.extractUserId("token")).thenReturn(42L);
        when(rateLimitService.checkLimit(eq("user"), eq("42"), any(RateLimitRule.class)))
                .thenReturn(new RateLimitDecision(true, 120, 119, 60));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/applications/my-applications");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimitService).checkLimit(eq("user"), eq("42"), any(RateLimitRule.class));
    }

    @Test
    void anonymousRequestsUseIpSubjectKey() throws Exception {
        when(rateLimitService.checkLimit(eq("ip"), eq("10.0.0.1"), any(RateLimitRule.class)))
                .thenReturn(new RateLimitDecision(true, 60, 59, 60));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/jobs");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(rateLimitService).checkLimit(eq("ip"), eq("10.0.0.1"), any(RateLimitRule.class));
    }
}
