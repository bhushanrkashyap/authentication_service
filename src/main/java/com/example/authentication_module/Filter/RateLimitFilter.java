package com.example.authentication_module.Filter;

import com.example.authentication_module.Service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * IP-based rate limiting for the /auth/** endpoints, backed by the Redis
 * fixed-window counters in {@link RateLimiterService}. Runs as a plain
 * servlet filter (outside the Spring Security chain) so it rejects abusive
 * traffic before any authentication/JPA work happens.
 *
 * Login brute-force protection by *email* lives in UserService, since the
 * email is only available after the request body is parsed.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterService rateLimiterService;

    @Value("${app.ratelimit.login.max-attempts}")
    private int loginMaxAttempts;

    @Value("${app.ratelimit.login.window-minutes}")
    private long loginWindowMinutes;

    @Value("${app.ratelimit.forgot-password.max-attempts}")
    private int forgotPasswordMaxAttempts;

    @Value("${app.ratelimit.forgot-password.window-minutes}")
    private long forgotPasswordWindowMinutes;

    @Value("${app.ratelimit.register.max-attempts}")
    private int registerMaxAttempts;

    @Value("${app.ratelimit.register.window-minutes}")
    private long registerWindowMinutes;

    @Value("${app.ratelimit.global.max-requests}")
    private int globalMaxRequests;

    @Value("${app.ratelimit.global.window-seconds}")
    private long globalWindowSeconds;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);

        String perEndpointMessage = checkPerEndpointLimit(path, ip);
        if (perEndpointMessage != null) {
            reject(request, response, perEndpointMessage);
            return;
        }

        boolean withinGlobalLimit = rateLimiterService.isAllowed(
                "ratelimit:ip:global:" + ip,
                globalMaxRequests,
                Duration.ofSeconds(globalWindowSeconds));

        if (!withinGlobalLimit) {
            reject(request, response, "Too many requests. Please slow down and try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns a rejection message if the per-endpoint limit for this path was
     * exceeded, or null if the request is allowed (or the path has no
     * dedicated limit beyond the global one).
     */
    private String checkPerEndpointLimit(String path, String ip) {
        if (path.equals("/auth/login")) {
            boolean allowed = rateLimiterService.isAllowed(
                    "ratelimit:ip:login:" + ip,
                    loginMaxAttempts,
                    Duration.ofMinutes(loginWindowMinutes));
            return allowed ? null : "Too many login attempts from this network. Please try again later.";
        }

        if (path.equals("/auth/forgot-password")) {
            boolean allowed = rateLimiterService.isAllowed(
                    "ratelimit:ip:forgot-password:" + ip,
                    forgotPasswordMaxAttempts,
                    Duration.ofMinutes(forgotPasswordWindowMinutes));
            return allowed ? null : "Too many password reset requests. Please try again later.";
        }

        if (path.equals("/auth/register")) {
            boolean allowed = rateLimiterService.isAllowed(
                    "ratelimit:ip:register:" + ip,
                    registerMaxAttempts,
                    Duration.ofMinutes(registerWindowMinutes));
            return allowed ? null : "Too many registration attempts. Please try again later.";
        }

        return null;
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            String message) throws IOException {

        log.warn("Rate limit exceeded for path {} from {}", request.getRequestURI(), clientIp(request));

        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                message.replace("\"", "'"),
                request.getRequestURI().replace("\"", "'")
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(body);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
