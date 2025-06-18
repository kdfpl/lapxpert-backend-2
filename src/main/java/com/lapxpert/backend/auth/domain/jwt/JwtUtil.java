package com.lapxpert.backend.auth.domain.jwt;

import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:lapxpert_secret_key_for_development_only}")
    private String SECRET;

    @Value("${jwt.expiration-hours:5}")
    private int expirationHours;

    public String generateToken(NguoiDung user) {
        Date issuedAt = new Date();
        Date expiration = Date.from(Instant.now().plus(expirationHours, ChronoUnit.HOURS));

        log.debug("Generating JWT token for user: {} with expiration: {}", user.getEmail(), expiration);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("vaiTro", user.getVaiTro().toString())
                .claim("id", user.getId())
                .claim("hoTen", user.getHoTen())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            boolean isValid = !expiration.before(new Date());

            if (!isValid) {
                log.debug("Token expired at: {}", expiration);
            }

            return isValid;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("vaiTro", String.class);
    }

    public Long extractUserId(String token) {
        return extractClaims(token).get("id", Long.class);
    }

    public String extractUserName(String token) {
        return extractClaims(token).get("hoTen", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public Date extractIssuedAt(String token) {
        return extractClaims(token).getIssuedAt();
    }
}

