package com.example.authentication_module.Service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Per-user JWT revocation list. Rather than blacklisting individual tokens
 * by id, this stores a single "valid after" timestamp per user in Redis.
 * Any access token issued (iat) before that timestamp is rejected, which
 * invalidates every access token the user currently holds at once (e.g. on
 * logout or password change) without having to track individual JTIs.
 *
 * Note: new tokens minted after the revocation point (e.g. via
 * /auth/refresh-token on another device) are issued with a fresh iat and
 * therefore remain valid - this revokes outstanding access tokens, it does
 * not log out other devices permanently.
 */
@Service
public class TokenRevocationService {

    private static final String KEY_PREFIX = "auth:valid-after:";

    // Must cover the longest possible lifetime of a still-valid access token
    // (refresh-token lifetime) so the marker doesn't expire while an old
    // token minted just before revocation could still be replayed.
    private static final Duration MARKER_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public TokenRevocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revokeAllTokens(Integer userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(
                key,
                Long.toString(System.currentTimeMillis()),
                MARKER_TTL
        );
    }

    public boolean isTokenValid(Integer userId, Date issuedAt) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);

        if (value == null || issuedAt == null) {
            return true;
        }

        long validAfter = Long.parseLong(value);
        return issuedAt.getTime() >= validAfter;
    }
}
