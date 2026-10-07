package com.example.authentication_module.Service;

import com.example.authentication_module.Exception.InvalidGoogleTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Verifies Google "ID tokens" sent by the frontend (Google Identity
 * Services button / One Tap) without ever handling the user's Google
 * password. Signature verification is delegated to NimbusJwtDecoder against
 * Google's published JWKS; issuer/audience/email-verified are checked
 * manually since those are deployment-specific, not signature concerns.
 */
@Service
public class GoogleTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);

    private static final List<String> ALLOWED_ISSUERS =
            List.of("https://accounts.google.com", "accounts.google.com");

    private final String googleClientId;
    private final JwtDecoder jwtDecoder;

    public GoogleTokenVerifierService(
            @Value("${app.oauth.google.client-id:}") String googleClientId,
            @Value("${app.oauth.google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwksUri) {

        this.googleClientId = googleClientId;
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }

    public GoogleUserInfo verify(String idToken) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException ex) {
            log.warn("Google ID token verification failed: {}", ex.getMessage());
            throw new InvalidGoogleTokenException("Invalid Google ID token");
        }

        String issuer = jwt.getClaimAsString("iss");
        if (issuer == null || !ALLOWED_ISSUERS.contains(issuer)) {
            throw new InvalidGoogleTokenException("Unexpected token issuer");
        }

        List<String> audience = jwt.getAudience();
        if (googleClientId == null || googleClientId.isBlank()
                || audience == null || !audience.contains(googleClientId)) {
            throw new InvalidGoogleTokenException("Token was not issued for this application");
        }

        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        if (email == null || emailVerified == null || !emailVerified) {
            throw new InvalidGoogleTokenException("Google account email is not verified");
        }

        return new GoogleUserInfo(
                jwt.getSubject(),
                email,
                true,
                jwt.getClaimAsString("name")
        );
    }
}
