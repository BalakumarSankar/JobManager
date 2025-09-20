package com.example.jobdispatcher.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility class for token generation and validation.
 * Much simpler and more secure than handshake mechanism.
 */
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secret;
    
    @Value("${jwt.expiration:3600000}") // 1 hour default
    private long expiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    /**
     * Generate JWT token for app server.
     */
    public String generateToken(String appServerId, String apiKeyId, String[] permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("appServerId", appServerId);
        claims.put("apiKeyId", apiKeyId);
        claims.put("permissions", permissions);
        claims.put("tokenType", "access");
        
        return createToken(claims, appServerId);
    }
    
    /**
     * Create JWT token with claims.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Extract app server ID from token.
     */
    public String getAppServerIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("appServerId", String.class);
    }
    
    /**
     * Extract API key ID from token.
     */
    public String getApiKeyIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("apiKeyId", String.class);
    }
    
    /**
     * Extract permissions from token.
     */
    @SuppressWarnings("unchecked")
    public String[] getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("permissions", String[].class);
    }
    
    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Extract claims from token.
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Get token expiration time.
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }
}

