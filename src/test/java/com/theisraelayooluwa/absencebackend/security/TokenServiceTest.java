package com.theisraelayooluwa.absencebackend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This service hand-rolls HMAC-SHA256 JWT-shaped tokens rather than using a
 * vetted library (e.g. jjwt), which is a deliberate trade-off noted elsewhere
 * in this project's write-up. These tests exercise exactly the properties a
 * token implementation must hold regardless of how it's built: a valid token
 * round-trips, a tampered one is rejected, and an expired one is rejected.
 */
class TokenServiceTest {

    private final TokenService tokenService = new TokenService("unit-test-secret", 60);

    @Test
    void createToken_thenValidate_roundTripsTheSubject() {
        String token = tokenService.createToken("ava.smith@company.com");

        String subject = tokenService.validateAndGetSubject(token);

        assertEquals("ava.smith@company.com", subject);
    }

    @Test
    void createToken_producesThreeDotSeparatedSegments() {
        String token = tokenService.createToken("ava.smith@company.com");

        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void validate_rejectsATokenWithATamperedSignature() {
        String token = tokenService.createToken("ava.smith@company.com");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + reverse(parts[2]);

        assertThrows(IllegalArgumentException.class, () -> tokenService.validateAndGetSubject(tampered));
    }

    @Test
    void validate_rejectsATokenSignedWithADifferentSecret() {
        TokenService otherIssuer = new TokenService("a-completely-different-secret", 60);
        String token = otherIssuer.createToken("ava.smith@company.com");

        assertThrows(IllegalArgumentException.class, () -> tokenService.validateAndGetSubject(token));
    }

    @Test
    void validate_rejectsAMalformedToken() {
        assertThrows(IllegalArgumentException.class, () -> tokenService.validateAndGetSubject("not-a-jwt"));
    }

    @Test
    void validate_rejectsAnExpiredToken() {
        // Negative expiry means the token's "exp" claim is already in the past the instant it's minted.
        TokenService alreadyExpired = new TokenService("unit-test-secret", -1);
        String token = alreadyExpired.createToken("ava.smith@company.com");

        assertThrows(IllegalArgumentException.class, () -> alreadyExpired.validateAndGetSubject(token));
    }

    private static String reverse(String value) {
        return new StringBuilder(value).reverse().toString();
    }
}
