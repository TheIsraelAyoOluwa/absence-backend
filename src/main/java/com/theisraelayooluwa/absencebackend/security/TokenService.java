package com.theisraelayooluwa.absencebackend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {

    private final byte[] secret;
    private final long expirationSeconds;

    public TokenService(@Value("${app.security.token-secret:absence-backend-dev-secret-change-me}") String secret,
                        @Value("${app.security.token-expiration-minutes:480}") long expirationMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationMinutes * 60;
    }

    public String createToken(String subject) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
                escape(subject), now.getEpochSecond(), expiresAt.getEpochSecond());

        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public Instant expirationInstant() {
        return Instant.now().plusSeconds(expirationSeconds);
    }

    public String validateAndGetSubject(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token");
        }

        String signedContent = parts[0] + "." + parts[1];
        String expectedSignature = sign(signedContent);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new IllegalArgumentException("Invalid token signature");
        }

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String subject = extractJsonString(payloadJson, "sub");
        long exp = extractJsonLong(payloadJson, "exp");

        if (Instant.now().getEpochSecond() >= exp) {
            throw new IllegalArgumentException("Token expired");
        }

        return subject;
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length; i++) {
            result |= left[i] ^ right[i];
        }
        return result == 0;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("Invalid token payload");
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        if (end < 0) {
            throw new IllegalArgumentException("Invalid token payload");
        }
        return json.substring(start, end);
    }

    private long extractJsonLong(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("Invalid token payload");
        }
        start += marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }
}
