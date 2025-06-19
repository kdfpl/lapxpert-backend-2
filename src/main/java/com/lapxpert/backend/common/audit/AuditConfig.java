package com.lapxpert.backend.common.audit;

import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuration for JPA auditing with enhanced audit trail support.
 * Enables automatic population of @CreatedBy and @LastModifiedBy fields.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * Custom AuditorAware implementation that extracts the current user
     * from Spring Security context for audit trail purposes.
     */
    public static class SpringSecurityAuditorAware implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("SYSTEM");
            }

            Object principal = authentication.getPrincipal();
            
            // If principal is our NguoiDung entity
            if (principal instanceof NguoiDung) {
                NguoiDung user = (NguoiDung) principal;
                return Optional.of(user.getMaNguoiDung());
            }
            
            // If principal is a string (username/email)
            if (principal instanceof String) {
                return Optional.of((String) principal);
            }
            
            // Fallback to authentication name
            return Optional.of(authentication.getName());
        }
    }
}
