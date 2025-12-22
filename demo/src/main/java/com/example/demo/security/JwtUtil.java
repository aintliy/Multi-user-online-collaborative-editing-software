package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.example.demo.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(Long userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从请求中提取用户 ID
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            throw new IllegalArgumentException("Token not found in request");
        }
        return getUserIdFromToken(token);
    }

    /**
     * 从请求头中提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 从 Token 中获取邮箱
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析 Token
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
