package com.example.authentication_module.Service;

import com.example.authentication_module.Exception.InvalidGoogleTokenException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies GoogleTokenVerifierService's signature/issuer/audience/
 * email_verified checks against a locally-run stand-in for Google's JWKS
 * endpoint, since the real accounts.google.com can't be exercised without
 * a real Google account.
 */
class GoogleTokenVerifierServiceTest {

    private static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";

    private static HttpServer server;
    private static String jwksUri;
    private static RSAKey rsaJwk;

    @BeforeAll
    static void startFakeJwksServer() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        rsaJwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID("test-kid")
                .build();

        String jwksJson = "{\"keys\":[" + rsaJwk.toPublicJWK().toJSONString() + "]}";

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/certs", exchange -> {
            byte[] body = jwksJson.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        jwksUri = "http://localhost:" + server.getAddress().getPort() + "/certs";
    }

    @AfterAll
    static void stopFakeJwksServer() {
        server.stop(0);
    }

    private static String signedToken(
            String issuer, List<String> audience, String email, Boolean emailVerified, Date expiry) throws Exception {

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject("google-user-123")
                .issuer(issuer)
                .audience(audience)
                .issueTime(new Date())
                .expirationTime(expiry)
                .claim("name", "Test User");

        if (email != null) {
            claims.claim("email", email);
        }
        if (emailVerified != null) {
            claims.claim("email_verified", emailVerified);
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .keyID(rsaJwk.getKeyID())
                        .build(),
                claims.build());

        jwt.sign(new RSASSASigner(rsaJwk));
        return jwt.serialize();
    }

    private GoogleTokenVerifierService verifier() {
        return new GoogleTokenVerifierService(CLIENT_ID, jwksUri);
    }

    @Test
    void acceptsValidGoogleToken() throws Exception {
        String token = signedToken(
                "https://accounts.google.com",
                List.of(CLIENT_ID),
                "user@example.com",
                true,
                new Date(System.currentTimeMillis() + 300_000));

        GoogleUserInfo info = verifier().verify(token);

        assertEquals("user@example.com", info.email());
        assertTrue(info.emailVerified());
        assertEquals("Test User", info.name());
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String token = signedToken(
                "https://accounts.google.com",
                List.of("someone-elses-client-id"),
                "user@example.com",
                true,
                new Date(System.currentTimeMillis() + 300_000));

        assertThrows(InvalidGoogleTokenException.class, () -> verifier().verify(token));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String token = signedToken(
                "https://evil.example.com",
                List.of(CLIENT_ID),
                "user@example.com",
                true,
                new Date(System.currentTimeMillis() + 300_000));

        assertThrows(InvalidGoogleTokenException.class, () -> verifier().verify(token));
    }

    @Test
    void rejectsUnverifiedEmail() throws Exception {
        String token = signedToken(
                "https://accounts.google.com",
                List.of(CLIENT_ID),
                "user@example.com",
                false,
                new Date(System.currentTimeMillis() + 300_000));

        assertThrows(InvalidGoogleTokenException.class, () -> verifier().verify(token));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = signedToken(
                "https://accounts.google.com",
                List.of(CLIENT_ID),
                "user@example.com",
                true,
                new Date(System.currentTimeMillis() - 60_000));

        assertThrows(InvalidGoogleTokenException.class, () -> verifier().verify(token));
    }

    @Test
    void rejectsTamperedSignature() throws Exception {
        String token = signedToken(
                "https://accounts.google.com",
                List.of(CLIENT_ID),
                "user@example.com",
                true,
                new Date(System.currentTimeMillis() + 300_000));

        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThrows(InvalidGoogleTokenException.class, () -> verifier().verify(tampered));
    }
}
