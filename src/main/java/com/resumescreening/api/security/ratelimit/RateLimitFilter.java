package com.resumescreening.api.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.api.model.dto.response.ErrorResponse;
import com.resumescreening.api.security.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final RateLimitRule LOGIN = new RateLimitRule("login", 5, Duration.ofMinutes(1));
    private static final RateLimitRule REGISTER = new RateLimitRule("register", 3, Duration.ofMinutes(1));
    private static final RateLimitRule PUBLIC_JOBS = new RateLimitRule("public-jobs", 60, Duration.ofMinutes(1));
    private static final RateLimitRule AUTHENTICATED = new RateLimitRule("authenticated", 120, Duration.ofMinutes(1));
    private static final RateLimitRule RESUME_UPLOAD = new RateLimitRule("resume-upload", 10, Duration.ofHours(1));
    private static final RateLimitRule SCREENING = new RateLimitRule("screening", 20, Duration.ofHours(1));
    private static final RateLimitRule BATCH_SCREENING = new RateLimitRule("batch-screening", 5, Duration.ofHours(1));

    private final RateLimitService rateLimitService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Subject subject = resolveSubject(request);
        RateLimitDecision decision = rateLimitService.checkLimit(subject.type(), subject.value(), rule);
        addRateLimitHeaders(response, decision);

        if (!decision.allowed()) {
            writeRateLimitResponse(request, response, decision);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return null;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        if (path.equals("/api/v1/auth/login")) {
            return LOGIN;
        }
        if (path.equals("/api/v1/auth/register")) {
            return REGISTER;
        }
        if (HttpMethod.GET.matches(method) && isPublicJobPath(path)) {
            return PUBLIC_JOBS;
        }
        if (path.equals("/api/v1/resumes/upload")) {
            return RESUME_UPLOAD;
        }
        if (path.equals("/api/v1/screening/batch")) {
            return BATCH_SCREENING;
        }
        if (path.startsWith("/api/v1/screening/")) {
            return SCREENING;
        }
        if (path.startsWith("/api/v1/")) {
            return AUTHENTICATED;
        }
        return null;
    }

    private boolean isPublicJobPath(String path) {
        return path.equals("/api/v1/jobs")
                || path.equals("/api/v1/jobs/search")
                || path.matches("/api/v1/jobs/\\d+");
    }

    private Subject resolveSubject(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.extractUserId(token);
            if (userId != null) {
                return new Subject("user", userId.toString());
            }
        }
        return new Subject("ip", clientIp(request));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader("X-RateLimit-Limit", Long.toString(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", Long.toString(decision.remaining()));
        if (!decision.allowed()) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        }
    }

    private void writeRateLimitResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            RateLimitDecision decision
    ) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message("Rate limit exceeded. Please retry later.")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private record Subject(String type, String value) {
    }
}
