package com.lapxpert.backend.auth.domain.jwt;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.*;
import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private NguoiDungRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Bỏ qua các request không cần xác thực
        if (path.startsWith("/api/auth") || path.startsWith("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (token != null && !token.isEmpty()) {
                try {
                    if (jwtUtil.isTokenValid(token)) {
                        String email = jwtUtil.extractEmail(token);
                        NguoiDung nguoiDung = repository.findByEmail(email).orElse(null);

                        if (nguoiDung != null && nguoiDung.isActive()) {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    nguoiDung, null, List.of(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro()))
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        } else if (nguoiDung != null && !nguoiDung.isActive()) {
                            // User exists but is inactive
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"error\":\"Account is inactive\",\"code\":\"ACCOUNT_INACTIVE\"}");
                            response.setContentType("application/json");
                            return;
                        }
                    } else {
                        // Token is invalid or expired
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\":\"Token expired or invalid\",\"code\":\"TOKEN_EXPIRED\"}");
                        response.setContentType("application/json");
                        return;
                    }
                } catch (Exception e) {
                    // Token parsing failed
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid token format\",\"code\":\"TOKEN_INVALID\"}");
                    response.setContentType("application/json");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

}
