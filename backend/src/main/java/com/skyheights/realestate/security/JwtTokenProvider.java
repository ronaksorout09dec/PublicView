package com.skyheights.realestate.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Enterprise JWT Provider - Prop-OS
 * Generates and validates tokens with enterprise claims: userId, orgId, roles, permissions, hierarchy
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret:U2VjcmV0S2V5Rm9yUHJvcE9TU3VwZXJTZWN1cmVNb25vbGl0aHlCdWlsZGluZ1NlY3JldEtleVRoYXRJc1ZlcnlMb25nQW5kU2VjdXJlMTIzNDU2Nzg5MDEyMw==}")
    private String jwtSecretBase64;

    @Value("${app.jwt.expiration-ms:900000}") // 15 min access
    private Long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // 7 days refresh
    private Long refreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecretBase64);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT Secret initialized, expiration {} ms, refresh {}", jwtExpirationMs, refreshExpirationMs);
        } catch (Exception e) {
            // Fallback: if not base64, use raw as base64 of sha256? But better generate
            log.warn("JWT secret not valid Base64, generating fallback key. Please set app.jwt.secret as Base64 64+ bytes");
            this.key = Jwts.SIG.HS512.key().build();
        }
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername()) // email
                .claim("userId", userPrincipal.getId())
                .claim("uuid", userPrincipal.getUuid())
                .claim("orgId", userPrincipal.getOrgId())
                .claim("orgSlug", userPrincipal.getOrgSlug())
                .claim("fullName", userPrincipal.getFullName())
                .claim("roles", userPrincipal.getRoles())
                .claim("permissions", userPrincipal.getPermissions())
                .claim("hierarchyLevel", userPrincipal.getMaxHierarchyLevel())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("userId", userPrincipal.getId())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateTokenFromUserId(Long userId, String email, Long orgId, String orgSlug,
                                          java.util.List<String> roles, java.util.List<String> permissions,
                                          int hierarchy, String fullName, String uuid) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("uuid", uuid)
                .claim("orgId", orgId)
                .claim("orgSlug", orgSlug)
                .claim("fullName", fullName)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("hierarchyLevel", hierarchy)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
}
