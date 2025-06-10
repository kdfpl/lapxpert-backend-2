package com.lapxpert.backend.auth.domain.service;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.TrangThaiNguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service responsible for initializing default users on application startup
 * This ensures there's always an admin user available for system access
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultUserInitializationService implements CommandLineRunner {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.enabled:true}")
    private boolean defaultAdminEnabled;

    @Value("${app.default-admin.email:admin@lapxpert.com}")
    private String defaultAdminEmail;

    @Value("${app.default-admin.password:admin123456}")
    private String defaultAdminPassword;

    @Value("${app.default-admin.name:System Administrator}")
    private String defaultAdminName;

    @Value("${app.default-admin.phone:0123456789}")
    private String defaultAdminPhone;

    @Override
    @Transactional
    public void run(String... args) {
        if (defaultAdminEnabled) {
            createDefaultAdminIfNotExists();
        } else {
            log.info("Default admin user creation is disabled");
        }
    }

    /**
     * Creates a default admin user if one doesn't exist
     * This ensures there's always an admin account available for system access
     */
    private void createDefaultAdminIfNotExists() {
        try {
            // Check if admin user already exists
            Optional<NguoiDung> existingAdmin = nguoiDungRepository.findByEmail(defaultAdminEmail);
            
            if (existingAdmin.isPresent()) {
                log.info("Default admin user already exists with email: {}", defaultAdminEmail);
                
                // Ensure the existing user has admin role and is active
                NguoiDung admin = existingAdmin.get();
                boolean needsUpdate = false;
                
                if (admin.getVaiTro() != VaiTro.ADMIN) {
                    admin.setVaiTro(VaiTro.ADMIN);
                    needsUpdate = true;
                    log.info("Updated user role to ADMIN for: {}", defaultAdminEmail);
                }
                
                if (admin.getTrangThai() != TrangThaiNguoiDung.HOAT_DONG) {
                    admin.setTrangThai(TrangThaiNguoiDung.HOAT_DONG);
                    needsUpdate = true;
                    log.info("Activated admin user: {}", defaultAdminEmail);
                }
                
                if (needsUpdate) {
                    nguoiDungRepository.save(admin);
                }
                
                return;
            }

            // Create new admin user
            NguoiDung adminUser = NguoiDung.builder()
                    .maNguoiDung(generateAdminCode())
                    .hoTen(defaultAdminName)
                    .email(defaultAdminEmail)
                    .soDienThoai(defaultAdminPhone)
                    .matKhau(passwordEncoder.encode(defaultAdminPassword))
                    .vaiTro(VaiTro.ADMIN)
                    .trangThai(TrangThaiNguoiDung.HOAT_DONG)
                    .ngaySinh(LocalDate.of(1990, 1, 1)) // Default birth date
                    .build();

            nguoiDungRepository.save(adminUser);
            
            log.info("✅ Default admin user created successfully:");
            log.info("   Email: {}", defaultAdminEmail);
            log.info("   Password: {}", defaultAdminPassword);
            log.info("   Name: {}", defaultAdminName);
            log.info("   Phone: {}", defaultAdminPhone);
            log.warn("⚠️  SECURITY WARNING: Change default admin credentials in production!");
            
        } catch (Exception e) {
            log.error("❌ Failed to create default admin user", e);
            throw new RuntimeException("Failed to initialize default admin user", e);
        }
    }

    /**
     * Generates a unique admin code
     * @return admin user code
     */
    private String generateAdminCode() {
        // Find the highest existing admin code
        Optional<NguoiDung> lastAdmin = nguoiDungRepository
                .findTopByMaNguoiDungStartingWithOrderByMaNguoiDungDesc("ADM");
        
        if (lastAdmin.isPresent()) {
            String lastCode = lastAdmin.get().getMaNguoiDung();
            try {
                // Extract number from code like "ADM001"
                String numberPart = lastCode.substring(3);
                int nextNumber = Integer.parseInt(numberPart) + 1;
                return String.format("ADM%03d", nextNumber);
            } catch (Exception e) {
                log.warn("Could not parse admin code: {}, using default", lastCode);
            }
        }
        
        return "ADM001";
    }
}
