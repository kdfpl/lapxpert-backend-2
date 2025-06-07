package com.lapxpert.backend.auth.domain.jwt;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {
    private final String SECRET = "lapxpert_secret_key";

    public String generateToken(NguoiDung user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("vaiTro", user.getVaiTro().toString())
                .claim("id", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(5, ChronoUnit.HOURS)))
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
            return !extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }
}

