package com.example.authentication_module.Filter;

import com.example.authentication_module.Service.CustomUserDetailsService;
import com.example.authentication_module.Service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final com.example.authentication_module.Service.TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService,
            com.example.authentication_module.Service.TokenRevocationService tokenRevocationService) {

        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        List<String> publicPaths = Arrays.asList(
                "/auth/register",
                "/auth/login",
                "/auth/oauth/google",
                "/auth/verify-email",
                "/auth/forgot-password",
                "/auth/reset-password",
                "/auth/refresh-token",
                "/auth/logout",
                "/actuator/health"
        );

        boolean isPublic = publicPaths.stream()
                .anyMatch(p -> path.equals(p) || path.startsWith(p + "/"));

        if (isPublic) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            log.warn("Empty Bearer token received");
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(token);

            if (email != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        customUserDetailsService.loadUserByUsername(email);

                Integer userId = jwtService.extractUserId(token);

                boolean revoked = userId != null
                        && !tokenRevocationService.isTokenValid(
                                userId, jwtService.extractIssuedAt(token));

                if (revoked) {
                    log.debug("JWT token has been revoked for user: {}", email);
                    SecurityContextHolder.clearContext();
                } else if (jwtService.isTokenValid(token, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);

                    log.debug("Authentication successful for user: {}", email);

                } else {
                    log.warn("JWT token validation failed");
                    SecurityContextHolder.clearContext();
                }
            }

        } catch (ExpiredJwtException ex) {
            log.debug("JWT token has expired");
            SecurityContextHolder.clearContext();

        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token received");
            SecurityContextHolder.clearContext();

        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token format");
            SecurityContextHolder.clearContext();

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid JWT token");
            SecurityContextHolder.clearContext();

        } catch (Exception ex) {
            log.error(
                    "Unexpected JWT processing error: {}",
                    ex.getClass().getSimpleName()
            );
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}