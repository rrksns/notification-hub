package com.notificationhub.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpireMs;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        this.accessTokenExpireMs = jwtProperties.accessTokenExpireMs();
    }

    public String generateAccessToken(String subject, String tenantId, String role) {
        return generateAccessToken(subject, tenantId, role, "FREE");
    }

    public String generateAccessToken(String subject, String tenantId, String role, String plan) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("tenantId", tenantId)
                .claim("role", role)
                .claim("plan", plan)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpireMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    public String getTenantId(String token) {
        return parseToken(token).get("tenantId", String.class);
    }

    public String getPlan(String token) {
        return parseToken(token).get("plan", String.class);
    }
}
