package com.msmeerp.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long jwtExpirationMs;
    private final long refreshExpirationMs;
    private final long registrationTokenExpirationMs;
    private final long inviteTokenExpirationMs;
    private final long passwordResetTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
            @Value("${app.jwt.registration-token-expiration-ms:1800000}") long registrationTokenExpirationMs,
            @Value("${app.jwt.invite-token-expiration-ms:259200000}") long inviteTokenExpirationMs,
            @Value("${app.jwt.password-reset-token-expiration-ms:1800000}") long passwordResetTokenExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.registrationTokenExpirationMs = registrationTokenExpirationMs;
        this.inviteTokenExpirationMs = inviteTokenExpirationMs;
        this.passwordResetTokenExpirationMs = passwordResetTokenExpirationMs;
    }

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("userId", userPrincipal.getId())
                .claim("tenantId", userPrincipal.getTenantId())
                .claim("email", userPrincipal.getEmail())
                .claim("authorities", authorities)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username, String tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId)
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRegistrationToken(String adminEmail, Long onboardingId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + registrationTokenExpirationMs);

        return Jwts.builder()
                .subject(adminEmail)
                .claim("onboardingId", onboardingId)
                .claim("type", "REGISTRATION_TOKEN")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateUserInviteToken(String email, String tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + inviteTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("tenantId", tenantId)
                .claim("type", "USER_INVITE")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateUserInviteToken(String token, String expectedEmail, String expectedTenantId) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String type = claims.get("type", String.class);
            String tenantId = claims.get("tenantId", String.class);
            return "USER_INVITE".equals(type)
                    && expectedEmail.equalsIgnoreCase(claims.getSubject())
                    && expectedTenantId.equals(tenantId);
        } catch (Exception e) {
            log.error("Invite token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * A password-reset token carries a fingerprint of the user's current password hash, so the link
     * stops working as soon as the password changes — i.e. each link can be used only once.
     */
    public String generatePasswordResetToken(String email, String tenantId, String currentPasswordHash) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + passwordResetTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("tenantId", tenantId)
                .claim("pwd", passwordFingerprint(currentPasswordHash))
                .claim("type", "PASSWORD_RESET")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /** Claims of a valid, unexpired password-reset token, or {@code null} if the token is unusable. */
    public Claims parsePasswordResetToken(String token) {
        if (token == null || !validateToken(token)) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return "PASSWORD_RESET".equals(claims.get("type", String.class)) ? claims : null;
        } catch (Exception e) {
            log.error("Password reset token validation failed: {}", e.getMessage());
            return null;
        }
    }

    /** True if the token was issued for the password the user still has (it hasn't been used yet). */
    public boolean passwordResetTokenMatches(Claims claims, String currentPasswordHash) {
        String expected = passwordFingerprint(currentPasswordHash);
        String actual = claims.get("pwd", String.class);
        return actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    public long getPasswordResetTokenExpirationMs() {
        return passwordResetTokenExpirationMs;
    }

    private static String passwordFingerprint(String passwordHash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public String getEmailFromRegistrationToken(String token) {
        return getUsernameFromJwt(token);
    }

    public String getTenantIdFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("tenantId", String.class);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateRegistrationToken(String token, String expectedEmail) {
        if (!validateToken(token)) {
            return false;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String type = claims.get("type", String.class);
            return "REGISTRATION_TOKEN".equals(type) && expectedEmail.equalsIgnoreCase(claims.getSubject());
        } catch (Exception e) {
            log.error("Registration token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    public long getRegistrationTokenExpirationMs() {
        return registrationTokenExpirationMs;
    }
}
