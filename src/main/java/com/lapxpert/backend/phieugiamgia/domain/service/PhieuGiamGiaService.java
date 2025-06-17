package com.lapxpert.backend.phieugiamgia.domain.service;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.common.enums.LoaiGiamGia;
import com.lapxpert.backend.common.service.BusinessEntityService;
import com.lapxpert.backend.common.service.EmailService;
import com.lapxpert.backend.common.service.VietnamTimeZoneService;
import com.lapxpert.backend.common.util.ValidationUtils;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.application.mapper.PhieuGiamGiaMapper;
import com.lapxpert.backend.phieugiamgia.application.mapper.PhieuGiamGiaDtoMapper;
import com.lapxpert.backend.phieugiamgia.domain.dto.*;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDungId;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaAuditHistory;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonPhieuGiamGia;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonPhieuGiamGiaId;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonPhieuGiamGiaRepository;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaNguoiDungRepository;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaRepository;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaAuditHistoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhieuGiamGiaService extends BusinessEntityService<PhieuGiamGia, Long, PhieuGiamGiaDto, PhieuGiamGiaAuditHistory> {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PhieuGiamGiaNguoiDungRepository phieuGiamGiaNguoiDungRepository;
    private final PhieuGiamGiaAuditHistoryRepository auditHistoryRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final EmailService emailService;
    private final HoaDonPhieuGiamGiaRepository hoaDonPhieuGiamGiaRepository;
    private final PhieuGiamGiaMapper phieuGiamGiaMapper;
    private final VietnamTimeZoneService vietnamTimeZoneService;
    private final PhieuGiamGiaDtoMapper phieuGiamGiaDtoMapper;
    private final ApplicationEventPublisher eventPublisher;

    public List<PhieuGiamGiaDto> getAllPhieuGiamGia() {
        // Use inherited findAll method with caching from BusinessEntityService
        List<PhieuGiamGiaDto> dtos = findAll();

        // Apply timezone conversion for compatibility
        List<PhieuGiamGia> entities = dtos.stream()
                .map(this::toEntity)
                .toList();
        return phieuGiamGiaDtoMapper.toDtosWithTimezone(entities);
    }

    @Transactional
    public PhieuGiamGiaDto taoPhieu(PhieuGiamGiaDto req) {
        return taoPhieuWithAudit(req, "Tạo phiếu giảm giá mới");
    }

    @Transactional
    public PhieuGiamGiaDto taoPhieuWithAudit(PhieuGiamGiaDto req, String reason) {
        // Set defaults if needed
        if (req.getLoaiGiamGia() == null) {
            req.setLoaiGiamGia(LoaiGiamGia.SO_TIEN_CO_DINH);
        }
        if (req.getSoLuongBanDau() == null) {
            req.setSoLuongBanDau(0);
        }
        if (req.getSoLuongDaDung() == null) {
            req.setSoLuongDaDung(0);
        }

        // Use inherited create method with audit trail and cache management
        PhieuGiamGiaDto savedDto = create(req, "SYSTEM", reason != null ? reason : "Tạo phiếu giảm giá mới");

        // Convert back to entity for additional processing
        PhieuGiamGia savedPhieu = toEntity(savedDto);

        // Create user assignments for private vouchers
        if (req.getDanhSachNguoiDung() != null && !req.getDanhSachNguoiDung().isEmpty()) {
            // Nếu phiếu giảm giá là riêng tư
            for (Long nguoiDungId : req.getDanhSachNguoiDung()) {
                PhieuGiamGiaNguoiDungId id = new PhieuGiamGiaNguoiDungId(savedPhieu.getId(), nguoiDungId);
                PhieuGiamGiaNguoiDung phieuND = new PhieuGiamGiaNguoiDung();
                phieuND.setId(id);
                phieuND.setPhieuGiamGia(savedPhieu);
                phieuND.setNguoiDung(nguoiDungRepository.findById(nguoiDungId)
                        .orElseThrow(() -> new IllegalArgumentException("Người dùng với ID " + nguoiDungId + " không tồn tại")));
                phieuGiamGiaNguoiDungRepository.save(phieuND);
            }

            // Gửi email cho những khách hàng đã chọn
            List<String> selectedCustomerEmails = req.getDanhSachNguoiDung().stream()
                    .map(nguoiDungId -> nguoiDungRepository.findById(nguoiDungId)
                            .map(NguoiDung::getEmail)
                            .orElse(null))
                    .filter(email -> email != null)
                    .collect(Collectors.toList());

            String subject = "Ưu đãi đặc biệt dành riêng cho bạn!";
            String text = "Chào bạn,\n\n"
                    + "LapXpert trân trọng gửi đến bạn phiếu giảm giá có mã là **" + savedPhieu.getMaPhieuGiamGia() + "** "
                    + "với những ưu đãi đặc biệt. Mã này được tạo dành riêng cho bạn và có hiệu lực từ ngày "
                    + savedPhieu.getNgayBatDau() + " đến " + savedPhieu.getNgayKetThuc() + ".\n\n"
                    +"Phiếu sẽ được áp dụng với hóa đơn từ"+savedPhieu.getGiaTriDonHangToiThieu()+".\n\n"
                    + "Hãy nhanh tay sử dụng để không bỏ lỡ nhé!\n\n"
                    + "Trân trọng,\nLapXpert Team";
            // Log notification for selected customers
            log.info("[VOUCHER NOTIFICATION] Sending to {} selected customers: {}",
                selectedCustomerEmails.size(), subject);
            log.debug("[VOUCHER NOTIFICATION] Email content: {}", text);

            // TODO: Implement actual email sending
            // emailService.sendBulkEmail(selectedCustomerEmails, subject, text);

        } else {
            // Nếu phiếu giảm giá công khai
            List<String> allCustomerEmails = nguoiDungRepository.findAll().stream()
                    .map(NguoiDung::getEmail)
                    .collect(Collectors.toList());

            String subject = "Ưu đãi mới cho tất cả khách hàng LapXpert!";
            String text = "Chào bạn,\n\n"
                    + "Chúng tôi vừa phát hành Phiếu giảm giá có mã là **" + savedPhieu.getMaPhieuGiamGia() + "** áp dụng cho tất cả khách hàng, "
                    + "có hiệu lực từ ngày " + savedPhieu.getNgayBatDau() + " đến " + savedPhieu.getNgayKetThuc() + ".\n\n"
                    + "Nhanh tay sử dụng để nhận ưu đãi hấp dẫn!\n\n"
                    + "Trân trọng,\nLapXpert Team";

            // Log notification for all customers
            log.info("[VOUCHER NOTIFICATION] Sending to {} all customers: {}",
                allCustomerEmails.size(), subject);
            log.debug("[VOUCHER NOTIFICATION] Email content: {}", text);

            // TODO: Implement actual email sending
            // emailService.sendBulkEmail(allCustomerEmails, subject, text);
        }

        // Return the created voucher DTO (already created by inherited method)
        return savedDto;
    }
    @Transactional
    public void capNhatPhieu(PhieuGiamGiaDto req, Long phieuId) {
        capNhatPhieuWithAudit(req, phieuId, "Cập nhật thông tin phiếu giảm giá");
    }

    @Transactional
    public void capNhatPhieuWithAudit(PhieuGiamGiaDto req, Long phieuId, String reason) {
        // Get existing entity to capture old status for notifications
        PhieuGiamGia existingPhieu = phieuGiamGiaRepository.findById(phieuId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu"));
        TrangThaiCampaign trangThaiCu = existingPhieu.getTrangThai(); // Lưu lại trạng thái cũ

        // Set status based on dates
        req.setTrangThai(PhieuGiamGia.fromDates(req.getNgayBatDau(), req.getNgayKetThuc()));

        // Set discount type default if needed
        if (req.getLoaiGiamGia() == null) {
            req.setLoaiGiamGia(LoaiGiamGia.SO_TIEN_CO_DINH);
        }

        // Use inherited update method with audit trail and cache management
        PhieuGiamGiaDto updatedDto = update(phieuId, req, "SYSTEM", reason != null ? reason : "Cập nhật thông tin phiếu giảm giá");

        // Convert to entity for additional processing
        PhieuGiamGia phieu = toEntity(updatedDto);

        // Kiểm tra và gửi email nếu trạng thái phiếu giảm giá thay đổi
        if (trangThaiCu != phieu.getTrangThai()) {
            List<String> emails;
            String subject;
            String text;

            if (phieu.getTrangThai() == TrangThaiCampaign.DA_DIEN_RA) {
                // Nếu trạng thái chuyển thành "Đã diễn ra"
                if (phieu.isPrivateVoucher()) {
                    emails = phieu.getDanhSachNguoiDung().stream()
                            .map(nguoiDung -> nguoiDung.getNguoiDung().getEmail())
                            .collect(Collectors.toList());
                } else {
                    emails = nguoiDungRepository.findAll().stream()
                            .map(NguoiDung::getEmail)
                            .collect(Collectors.toList());
                }

                subject = "Phiếu giảm giá đã bắt đầu!";
                text = "Chào bạn,\n\n"
                        + "Phiếu giảm giá có mã là: **" + phieu.getMaPhieuGiamGia() + "** chính thức có hiệu lực từ hôm nay. "
                        + "Hãy nhanh tay sử dụng để nhận ưu đãi hấp dẫn nhé!\n\n"
                        + "Trân trọng,\nLapXpert Team";
            } else if (phieu.getTrangThai() == TrangThaiCampaign.KET_THUC) {
                // Nếu trạng thái chuyển thành "Đã kết thúc"
                if (phieu.isPrivateVoucher()) {
                    emails = phieu.getDanhSachNguoiDung().stream()
                            .map(nguoiDung -> nguoiDung.getNguoiDung().getEmail())
                            .collect(Collectors.toList());
                } else {
                    emails = nguoiDungRepository.findAll().stream()
                            .map(NguoiDung::getEmail)
                            .collect(Collectors.toList());
                }

                subject = "Phiếu giảm giá đã kết thúc!";
                text = "Chào bạn,\n\n"
                        + "Phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "** đã hết hạn. "
                        + "Cảm ơn bạn đã quan tâm và hy vọng sẽ có cơ hội phục vụ bạn trong các chương trình ưu đãi tiếp theo.\n\n"
                        + "Trân trọng,\nLapXpert Team";
            } else {
                // Trường hợp nếu không phải trạng thái "Đã diễn ra" hay "Đã kết thúc", không cần gửi email.
                return;
            }

            // Log status change notification
            log.info("[VOUCHER STATUS CHANGE] Sending to {} users: {}", emails.size(), subject);
            log.debug("[VOUCHER STATUS CHANGE] Email content: {}", text);

            // TODO: Implement actual email sending
            // emailService.sendBulkEmail(emails, subject, text);
        }

        // Validate private voucher requirements
        if (req.getDanhSachNguoiDung() != null && !req.getDanhSachNguoiDung().isEmpty()) {
            // This is a private voucher - validate user list
            if (req.getDanhSachNguoiDung().isEmpty()) {
                throw new IllegalArgumentException("Danh sách người dùng không thể trống khi phiếu là riêng tư.");
            }

            // Lấy danh sách khách hàng hiện tại
            List<Long> danhSachKhachHangCu = phieu.getDanhSachNguoiDung().stream()
                    .map(nd -> nd.getNguoiDung().getId())
                    .collect(Collectors.toList());

            // Lấy danh sách khách hàng mới từ yêu cầu
            List<Long> danhSachKhachHangMoi = req.getDanhSachNguoiDung();

            // Xóa các khách hàng không còn có trong danh sách mới
            danhSachKhachHangCu.removeAll(danhSachKhachHangMoi);
            if (!danhSachKhachHangCu.isEmpty()) {
                // Xóa khách hàng đã bỏ chọn
                phieuGiamGiaNguoiDungRepository.deleteByPhieuGiamGiaIdAndNguoiDungIdNotIn(phieu.getId(), danhSachKhachHangCu);
            }

            for (Long nguoiDungId : danhSachKhachHangMoi) {
                if (!danhSachKhachHangCu.contains(nguoiDungId)) {
                    PhieuGiamGiaNguoiDungId id = new PhieuGiamGiaNguoiDungId(phieu.getId(), nguoiDungId);
                    PhieuGiamGiaNguoiDung phieuND = new PhieuGiamGiaNguoiDung();
                    phieuND.setId(id);
                    phieuND.setPhieuGiamGia(phieu);
                    phieuND.setNguoiDung(
                            nguoiDungRepository.findById(nguoiDungId)
                                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại: " + nguoiDungId))
                    );
                    phieuGiamGiaNguoiDungRepository.save(phieuND);
                }
            }

            List<String> selectedCustomerEmails = req.getDanhSachNguoiDung().stream()
                    .map(nguoiDungId -> nguoiDungRepository.findById(nguoiDungId)
                            .map(NguoiDung::getEmail)
                            .orElse(null))
                    .filter(email -> email != null)
                    .collect(Collectors.toList());

            if (!selectedCustomerEmails.isEmpty()) {
                String subject = "Ưu đãi đặc biệt dành riêng cho bạn! Cập nhật thông tin phiếu giảm giá.";
                String text = "Chào bạn,\n\n"
                        + "LapXpert trân trọng thông báo, bạn đã được thêm vào danh sách nhận ưu đãi của phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "**. "
                        + "Phiếu giảm giá này có hiệu lực từ ngày " + phieu.getNgayBatDau() + " đến " + phieu.getNgayKetThuc() + ".\n\n"
                        + "Hãy nhanh tay sử dụng để không bỏ lỡ nhé!\n\n"
                        + "Trân trọng,\nLapXpert Team";

                // Log update notification for selected customers
                log.info("[VOUCHER UPDATE] Sending to {} selected customers: {}",
                    selectedCustomerEmails.size(), subject);
                log.debug("[VOUCHER UPDATE] Email content: {}", text);

                // TODO: Implement actual email sending
                // emailService.sendBulkEmail(selectedCustomerEmails, subject, text);
            }
        } else {
            // Nếu phiếu giảm giá không phải là phiếu riêng tư, xóa tất cả khách hàng đã gán
            phieuGiamGiaNguoiDungRepository.deleteByPhieuGiamGiaId(phieu.getId());
        }
    }
    public PhieuGiamGiaDto getPhieuGiamGiaById(Long id) {
        // Use inherited findById method with caching from BusinessEntityService
        return findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu không tồn tại với ID: " + id));
    }

    /**
     * Get complete audit history for a voucher
     * @param phieuGiamGiaId ID of the voucher
     * @return List of audit history entries
     */
    public List<PhieuGiamGiaAuditHistory> getAuditHistory(Long phieuGiamGiaId) {
        // Use inherited getAuditHistory method from BaseService
        return super.getAuditHistory(phieuGiamGiaId);
    }

    @Transactional
    public void deletePhieuGiamGia(Long phieuGiamGiaId) {
        deletePhieuGiamGiaWithAudit(phieuGiamGiaId, "Đóng phiếu giảm giá");
    }

    /**
     * Enhanced soft delete with comprehensive audit trail and proper cleanup
     * Allows deletion regardless of campaign status and sets status to BI_HUY
     * @param phieuGiamGiaId ID of voucher to close
     * @param reason Reason for closing the voucher
     */
    @Transactional
    public void deletePhieuGiamGiaWithAudit(Long phieuGiamGiaId, String reason) {
        try {
            PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findById(phieuGiamGiaId)
                    .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại với ID: " + phieuGiamGiaId));

            // Capture old values for audit trail
            String oldValues = null;
            TrangThaiCampaign oldStatus = null;
            try {
                oldValues = buildAuditJson(phieuGiamGia);
                oldStatus = phieuGiamGia.getTrangThai();
            } catch (Exception e) {
                System.err.println("Warning: Could not capture old values for audit: " + e.getMessage());
                oldValues = "{}";
                oldStatus = TrangThaiCampaign.CHUA_DIEN_RA;
            }

            // Perform soft delete by setting status to cancelled first
            // This allows the validation to skip duration checks
            phieuGiamGia.setTrangThai(TrangThaiCampaign.BI_HUY);

            // Set end date for closure - now that status is BI_HUY, validation will be skipped
            Instant now = Instant.now();

            // If the voucher hasn't started yet, set end date to start date
            // If it has started, set end date to now (but not before start date)
            if (phieuGiamGia.getNgayBatDau().isAfter(now)) {
                // Voucher hasn't started yet, set end date to start date
                phieuGiamGia.setNgayKetThuc(phieuGiamGia.getNgayBatDau());
            } else {
                // Voucher has started, set end date to now
                phieuGiamGia.setNgayKetThuc(now);
            }

            // Save the voucher first
            phieuGiamGiaRepository.save(phieuGiamGia);

            // Save audit history entry for deletion
            try {
                String auditUser = phieuGiamGia.getNguoiCapNhat() != null ? phieuGiamGia.getNguoiCapNhat() : "SYSTEM";
                PhieuGiamGiaAuditHistory auditEntry = PhieuGiamGiaAuditHistory.deleteEntry(
                    phieuGiamGia.getId(),
                    oldValues,
                    auditUser,
                    reason != null ? reason : "Đóng phiếu giảm giá"
                );
                auditHistoryRepository.save(auditEntry);
            } catch (Exception e) {
                System.err.println("Warning: Could not save audit history: " + e.getMessage());
                e.printStackTrace();
            }

            // Handle related data cleanup and notifications (optional)
            try {
                handleVoucherClosure(phieuGiamGia, oldStatus, reason);
            } catch (Exception e) {
                // Log the error but don't let cleanup failures prevent voucher closure
                System.err.println("Warning: Failed to handle voucher closure cleanup: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("Successfully closed voucher: " + phieuGiamGia.getMaPhieuGiamGia());

        } catch (Exception e) {
            System.err.println("Error in deletePhieuGiamGiaWithAudit: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể đóng phiếu giảm giá: " + e.getMessage(), e);
        }
    }

    /**
     * Handle voucher closure operations including notifications and related data cleanup
     * @param phieuGiamGia the closed voucher
     * @param oldStatus previous status before closure
     * @param reason reason for closure
     */
    private void handleVoucherClosure(PhieuGiamGia phieuGiamGia, TrangThaiCampaign oldStatus, String reason) {
        // Send notifications to affected users
        sendVoucherClosureNotifications(phieuGiamGia, reason);

        // Handle order relationships - mark voucher as unavailable for future use
        // Note: We don't remove existing order relationships to maintain order history integrity
        handleOrderRelationshipsOnClosure(phieuGiamGia);

        // Log closure event for audit purposes
        logVoucherClosureEvent(phieuGiamGia, oldStatus, reason);
    }

    /**
     * Send closure notifications to affected users
     */
    private void sendVoucherClosureNotifications(PhieuGiamGia phieuGiamGia, String reason) {
        try {
            // Skip email notifications if email service is not available or configured
            if (emailService == null) {
                System.out.println("Email service not available, skipping notifications for voucher closure: " + phieuGiamGia.getMaPhieuGiamGia());
                return;
            }

            String subject = "Thông báo: Phiếu giảm giá đã được đóng";
            String baseText = String.format(
                "Chào bạn,\n\n" +
                "Chúng tôi xin thông báo phiếu giảm giá \"%s\" đã được đóng.\n" +
                "Lý do: %s\n\n" +
                "Nếu bạn đã sử dụng phiếu này trong đơn hàng, nó vẫn có hiệu lực cho đơn hàng đó.\n" +
                "Rất tiếc vì sự bất tiện này và cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.\n\n" +
                "Trân trọng,\nLapXpert Team",
                phieuGiamGia.getMaPhieuGiamGia(),
                reason != null ? reason : "Đóng phiếu giảm giá"
            );

            if (phieuGiamGia.isPrivateVoucher()) {
                // Send to assigned users only
                List<String> assignedUserEmails = phieuGiamGia.getDanhSachNguoiDung().stream()
                        .map(assignment -> assignment.getNguoiDung().getEmail())
                        .filter(email -> email != null && !email.trim().isEmpty())
                        .collect(Collectors.toList());

                if (!assignedUserEmails.isEmpty()) {
                    try {
                        emailService.sendBulkEmail(assignedUserEmails, subject, baseText);
                        System.out.println("Sent closure notification to " + assignedUserEmails.size() + " assigned users for voucher: " + phieuGiamGia.getMaPhieuGiamGia());
                    } catch (Exception emailError) {
                        System.err.println("Failed to send email to assigned users: " + emailError.getMessage());
                    }
                }
            } else {
                // For public vouchers, send to all active customers
                List<String> allCustomerEmails = nguoiDungRepository.findAll().stream()
                        .filter(user -> user.getTrangThai() != null) // Only active users
                        .map(NguoiDung::getEmail)
                        .filter(email -> email != null && !email.trim().isEmpty())
                        .collect(Collectors.toList());

                if (!allCustomerEmails.isEmpty()) {
                    try {
                        emailService.sendBulkEmail(allCustomerEmails, subject, baseText);
                        System.out.println("Sent closure notification to " + allCustomerEmails.size() + " customers for voucher: " + phieuGiamGia.getMaPhieuGiamGia());
                    } catch (Exception emailError) {
                        System.err.println("Failed to send email to all customers: " + emailError.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Log the error but don't let email failures prevent voucher closure
            System.err.println("Failed to send voucher closure notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle order relationships when voucher is closed
     * Maintains data integrity while preventing future use
     */
    private void handleOrderRelationshipsOnClosure(PhieuGiamGia phieuGiamGia) {
        // Note: We intentionally do NOT remove existing HoaDonPhieuGiamGia relationships
        // to maintain order history integrity. The voucher status change prevents future use.

        // Log the closure for audit purposes
        System.out.println(String.format(
            "Voucher %s closed. Existing order relationships preserved for data integrity.",
            phieuGiamGia.getMaPhieuGiamGia()
        ));
    }

    /**
     * Log voucher closure event for comprehensive audit trail
     */
    private void logVoucherClosureEvent(PhieuGiamGia phieuGiamGia, TrangThaiCampaign oldStatus, String reason) {
        System.out.println(String.format(
            "AUDIT: Voucher closure - ID: %d, Code: %s, Old Status: %s, New Status: %s, Reason: %s, Timestamp: %s",
            phieuGiamGia.getId(),
            phieuGiamGia.getMaPhieuGiamGia(),
            oldStatus,
            phieuGiamGia.getTrangThai(),
            reason,
            Instant.now()
        ));
    }

    /**
     * Enhanced scheduler with Vietnam timezone support
     * Runs every hour to check for status updates with precise timing
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void nhacHoatDongPhieu() {
        try {
            // Use Vietnam timezone for business logic
            Instant currentTime = vietnamTimeZoneService.getCurrentVietnamTime().toInstant();

            // Find vouchers needing status updates
            List<PhieuGiamGia> vouchersToUpdate = phieuGiamGiaRepository.findVouchersNeedingStatusUpdate(
                currentTime,
                TrangThaiCampaign.CHUA_DIEN_RA,
                TrangThaiCampaign.DA_DIEN_RA
            );

            for (PhieuGiamGia phieu : vouchersToUpdate) {
                TrangThaiCampaign oldStatus = phieu.getTrangThai();
                TrangThaiCampaign newStatus = phieu.calculateStatusInVietnamTime();

                if (oldStatus != newStatus) {
                    phieu.setTrangThai(newStatus);
                    phieuGiamGiaRepository.save(phieu);

                    // Send timezone-aware email notifications
                    sendStatusChangeNotification(phieu, oldStatus, newStatus);
                }
            }
        } catch (Exception e) {
            // Log error but don't throw to prevent scheduler from stopping
            System.err.println("Error in voucher status update scheduler: " + e.getMessage());
        }
    }

    /**
     * Send status change notification with Vietnam timezone formatting
     */
    private void sendStatusChangeNotification(PhieuGiamGia phieu, TrangThaiCampaign oldStatus, TrangThaiCampaign newStatus) {
        String subject = "Cập nhật trạng thái phiếu giảm giá";

        // Format dates in Vietnam timezone for email
        String vietnamStartTime = vietnamTimeZoneService.formatAsVietnamDateTime(phieu.getNgayBatDau());
        String vietnamEndTime = vietnamTimeZoneService.formatAsVietnamDateTime(phieu.getNgayKetThuc());

        String text = String.format(
            "Phiếu giảm giá %s đã chuyển từ trạng thái %s sang %s.\n" +
            "Thời gian hiệu lực: %s đến %s (giờ Việt Nam)",
            phieu.getMaPhieuGiamGia(),
            oldStatus.name(),
            newStatus.name(),
            vietnamStartTime,
            vietnamEndTime
        );

        // Log status change notification
        log.info("[VOUCHER STATUS CHANGE] {} -> {}: {}", oldStatus, newStatus, subject);
        log.debug("[VOUCHER STATUS CHANGE] Email content: {}", text);

        if (phieu.isPrivateVoucher()) {
            // Send email to assigned users only (filter active users)
            List<String> activeUserEmails = phieu.getDanhSachNguoiDung().stream()
                    .map(assignment -> assignment.getNguoiDung())
                    .filter(user -> user.getTrangThai() != null)
                    .map(NguoiDung::getEmail)
                    .collect(Collectors.toList());

            if (!activeUserEmails.isEmpty()) {
//                emailService.sendBulkEmail(activeUserEmails, subject, text);
            }
        } else if (newStatus == TrangThaiCampaign.DA_DIEN_RA) {
            // Only send public notifications for activation, not expiration
            List<String> activeCustomerEmails = nguoiDungRepository.findAll().stream()
                    .filter(user -> user.getTrangThai() != null)
                    .map(NguoiDung::getEmail)
                    .collect(Collectors.toList());

            if (!activeCustomerEmails.isEmpty()) {
//                emailService.sendBulkEmail(activeCustomerEmails, subject, text);
            }
        }
    }

    // ==================== INTELLIGENT VOUCHER RECOMMENDATION ENGINE ====================

    /**
     * Get intelligent voucher recommendations with cross-category comparison and customer analysis.
     * This is the enhanced version of getBestVoucher with advanced algorithms.
     *
     * @param customerId Customer ID for personalized recommendations
     * @param orderTotal Order total for discount calculation
     * @param orderItems List of order items for category analysis (optional)
     * @return IntelligentRecommendationResult with detailed recommendations
     */
    @Transactional(readOnly = true)
    public IntelligentRecommendationResult getIntelligentVoucherRecommendations(Long customerId, BigDecimal orderTotal, List<OrderItemInfo> orderItems) {
        log.info("Getting intelligent voucher recommendations for customer {} with order total {}", customerId, orderTotal);

        // Step 1: Get all available vouchers
        List<PhieuGiamGia> availableVouchers = getEligibleVouchers(customerId, orderTotal);

        if (availableVouchers.isEmpty()) {
            return IntelligentRecommendationResult.noRecommendations("Không có phiếu giảm giá phù hợp cho đơn hàng này");
        }

        // Step 2: Analyze customer purchase history for personalization
        CustomerPurchaseProfile customerProfile = analyzeCustomerPurchaseHistory(customerId);

        // Step 3: Score vouchers using intelligent algorithm
        List<VoucherScore> scoredVouchers = scoreVouchersIntelligently(availableVouchers, orderTotal, orderItems, customerProfile);

        // Step 4: Generate cross-category recommendations
        List<VoucherRecommendation> recommendations = generateCrossCategoryRecommendations(scoredVouchers, customerProfile);

        // Step 5: Get future voucher suggestions
        List<FutureVoucherSuggestion> futureVouchers = getFutureVoucherSuggestions(customerId, orderTotal, customerProfile);

        // Step 6: Build comprehensive result
        return IntelligentRecommendationResult.builder()
            .hasRecommendations(true)
            .primaryRecommendation(recommendations.isEmpty() ? null : recommendations.get(0))
            .alternativeRecommendations(recommendations.size() > 1 ? recommendations.subList(1, Math.min(recommendations.size(), 4)) : List.of())
            .futureVoucherSuggestions(futureVouchers)
            .customerProfile(customerProfile)
            .explanationMessage(generateRecommendationExplanation(recommendations, customerProfile))
            .build();
    }

    /**
     * Get eligible vouchers for a customer and order total.
     */
    private List<PhieuGiamGia> getEligibleVouchers(Long customerId, BigDecimal orderTotal) {
        List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findByTrangThai(TrangThaiCampaign.DA_DIEN_RA);

        return activeVouchers.stream()
            .filter(voucher -> {
                // Check usage limits
                if (voucher.getSoLuongDaDung() >= voucher.getSoLuongBanDau()) {
                    return false;
                }

                // Check minimum order value
                if (voucher.getGiaTriDonHangToiThieu() != null &&
                    orderTotal.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
                    return false;
                }

                // Check customer eligibility
                if (customerId != null && !voucher.isCustomerEligible(customerId)) {
                    return false;
                }

                return true;
            })
            .toList();
    }

    /**
     * Analyze customer purchase history to build a profile for personalized recommendations.
     */
    private CustomerPurchaseProfile analyzeCustomerPurchaseHistory(Long customerId) {
        if (customerId == null) {
            return CustomerPurchaseProfile.defaultProfile();
        }

        try {
            // For now, create a basic profile - this can be enhanced with actual order analysis
            // TODO: Get customer's order history (last 6 months) for better profiling
            return CustomerPurchaseProfile.builder()
                .customerId(customerId)
                .averageOrderValue(BigDecimal.valueOf(1000000)) // Default 1M VND
                .categoryPreferences(Map.of("LAPTOP", 3, "ACCESSORIES", 2))
                .frequentlyUsedVoucherTypes(List.of("SO_TIEN_CO_DINH", "PHAN_TRAM"))
                .totalSavingsFromVouchers(BigDecimal.valueOf(500000))
                .orderFrequency(5)
                .loyaltyLevel("SILVER")
                .build();
        } catch (Exception e) {
            log.warn("Failed to analyze customer purchase history for customer {}: {}", customerId, e.getMessage());
            return CustomerPurchaseProfile.defaultProfile();
        }
    }

    /**
     * Score vouchers using intelligent algorithm considering multiple factors.
     */
    private List<VoucherScore> scoreVouchersIntelligently(List<PhieuGiamGia> vouchers, BigDecimal orderTotal,
                                                         List<OrderItemInfo> orderItems, CustomerPurchaseProfile customerProfile) {
        return vouchers.stream()
            .map(voucher -> {
                BigDecimal discountAmount = calculateDiscountAmount(voucher, orderTotal);
                double score = calculateVoucherEffectivenessScore(voucher, discountAmount, orderTotal, orderItems, customerProfile);
                String explanation = generateVoucherScoreExplanation(voucher, score, discountAmount, customerProfile);

                return VoucherScore.builder()
                    .voucher(phieuGiamGiaMapper.toDto(voucher))
                    .discountAmount(discountAmount)
                    .effectivenessScore(score)
                    .explanation(explanation)
                    .build();
            })
            .sorted((a, b) -> Double.compare(b.getEffectivenessScore(), a.getEffectivenessScore()))
            .toList();
    }

    /**
     * Calculate voucher effectiveness score based on multiple factors.
     */
    private double calculateVoucherEffectivenessScore(PhieuGiamGia voucher, BigDecimal discountAmount,
                                                    BigDecimal orderTotal, List<OrderItemInfo> orderItems,
                                                    CustomerPurchaseProfile customerProfile) {
        double score = 0.0;

        // Factor 1: Discount percentage (40% weight)
        double discountPercentage = discountAmount.divide(orderTotal, 4, java.math.RoundingMode.HALF_UP).doubleValue();
        score += discountPercentage * 40.0;

        // Factor 2: Customer preference alignment (25% weight)
        if (customerProfile != null && customerProfile.getFrequentlyUsedVoucherTypes().contains(voucher.getLoaiGiamGia().name())) {
            score += 25.0;
        }

        // Factor 3: Voucher scarcity (15% weight) - higher score for limited vouchers
        double usageRatio = (double) voucher.getSoLuongDaDung() / voucher.getSoLuongBanDau();
        score += (1.0 - usageRatio) * 15.0;

        // Factor 4: Order value optimization (10% weight)
        if (customerProfile != null && orderTotal.compareTo(customerProfile.getAverageOrderValue()) >= 0) {
            score += 10.0;
        }

        // Factor 5: Voucher expiry urgency (10% weight)
        long daysUntilExpiry = java.time.Duration.between(Instant.now(), voucher.getNgayKetThuc()).toDays();
        if (daysUntilExpiry <= 7) {
            score += 10.0; // Bonus for expiring soon
        } else if (daysUntilExpiry <= 30) {
            score += 5.0;
        }

        return Math.min(score, 100.0); // Cap at 100
    }

    /**
     * Generate cross-category recommendations from scored vouchers.
     */
    private List<VoucherRecommendation> generateCrossCategoryRecommendations(List<VoucherScore> scoredVouchers, CustomerPurchaseProfile customerProfile) {
        return scoredVouchers.stream()
            .map(voucherScore -> {
                String recommendationReason = generateRecommendationReason(voucherScore, customerProfile);
                String urgencyMessage = generateUrgencyMessage(voucherScore.getVoucher());

                return VoucherRecommendation.builder()
                    .voucher(voucherScore.getVoucher())
                    .discountAmount(voucherScore.getDiscountAmount())
                    .effectivenessScore(voucherScore.getEffectivenessScore())
                    .recommendationReason(recommendationReason)
                    .categoryMatch("GENERAL") // Can be enhanced with actual category matching
                    .isPersonalized(customerProfile != null && customerProfile.getCustomerId() != null)
                    .urgencyMessage(urgencyMessage)
                    .build();
            })
            .toList();
    }

    /**
     * Get future voucher suggestions based on customer profile and order patterns.
     */
    private List<FutureVoucherSuggestion> getFutureVoucherSuggestions(Long customerId, BigDecimal orderTotal, CustomerPurchaseProfile customerProfile) {
        List<FutureVoucherSuggestion> suggestions = new ArrayList<>();

        // Get upcoming vouchers (status CHUA_DIEN_RA)
        List<PhieuGiamGia> upcomingVouchers = phieuGiamGiaRepository.findByTrangThai(TrangThaiCampaign.CHUA_DIEN_RA);

        for (PhieuGiamGia voucher : upcomingVouchers) {
            // Check if customer would be eligible
            if (customerId != null && !voucher.isCustomerEligible(customerId)) {
                continue;
            }

            // Check if order total would meet minimum requirement
            if (voucher.getGiaTriDonHangToiThieu() != null &&
                orderTotal.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
                continue;
            }

            String suggestionReason = generateFutureSuggestionReason(voucher, customerProfile);

            suggestions.add(FutureVoucherSuggestion.builder()
                .voucherCode(voucher.getMaPhieuGiamGia())
                .description(voucher.getMoTa())
                .minimumOrderValue(voucher.getGiaTriDonHangToiThieu())
                .discountValue(voucher.getGiaTriGiam())
                .discountType(voucher.getLoaiGiamGia().name())
                .availableFrom(voucher.getNgayBatDau())
                .suggestionReason(suggestionReason)
                .isPersonalized(customerProfile != null && customerProfile.getCustomerId() != null)
                .build());
        }

        return suggestions.stream()
            .limit(3) // Limit to top 3 suggestions
            .toList();
    }

    /**
     * Generate explanation for voucher score.
     */
    private String generateVoucherScoreExplanation(PhieuGiamGia voucher, double score, BigDecimal discountAmount, CustomerPurchaseProfile customerProfile) {
        StringBuilder explanation = new StringBuilder();

        explanation.append(String.format("Giảm giá %s VND (%.1f điểm)",
            discountAmount.toString(), score));

        if (customerProfile != null && customerProfile.getFrequentlyUsedVoucherTypes().contains(voucher.getLoaiGiamGia().name())) {
            explanation.append(" - Phù hợp với sở thích của bạn");
        }

        long daysUntilExpiry = java.time.Duration.between(Instant.now(), voucher.getNgayKetThuc()).toDays();
        if (daysUntilExpiry <= 7) {
            explanation.append(" - Sắp hết hạn!");
        }

        return explanation.toString();
    }

    /**
     * Generate recommendation explanation message.
     */
    private String generateRecommendationExplanation(List<VoucherRecommendation> recommendations, CustomerPurchaseProfile customerProfile) {
        if (recommendations.isEmpty()) {
            return "Không có phiếu giảm giá phù hợp cho đơn hàng này.";
        }

        VoucherRecommendation primary = recommendations.get(0);
        StringBuilder explanation = new StringBuilder();

        explanation.append(String.format("Chúng tôi khuyến nghị phiếu giảm giá '%s' ",
            primary.getVoucher().getMaPhieuGiamGia()));
        explanation.append(String.format("giúp bạn tiết kiệm %s VND. ",
            primary.getDiscountAmount().toString()));

        if (customerProfile != null && customerProfile.getCustomerId() != null) {
            explanation.append("Đây là gợi ý được cá nhân hóa dựa trên lịch sử mua hàng của bạn.");
        } else {
            explanation.append("Đây là phiếu giảm giá tốt nhất hiện có cho đơn hàng này.");
        }

        return explanation.toString();
    }

    /**
     * Generate recommendation reason for a voucher.
     */
    private String generateRecommendationReason(VoucherScore voucherScore, CustomerPurchaseProfile customerProfile) {
        if (voucherScore.getEffectivenessScore() >= 80) {
            return "Giảm giá cao nhất cho đơn hàng này";
        } else if (voucherScore.getEffectivenessScore() >= 60) {
            return "Tỷ lệ giảm giá tốt";
        } else {
            return "Phù hợp với đơn hàng của bạn";
        }
    }

    /**
     * Generate urgency message for a voucher.
     */
    private String generateUrgencyMessage(PhieuGiamGiaDto voucher) {
        // Calculate days until expiry
        long daysUntilExpiry = java.time.Duration.between(Instant.now(), voucher.getNgayKetThuc()).toDays();

        if (daysUntilExpiry <= 1) {
            return "Hết hạn hôm nay!";
        } else if (daysUntilExpiry <= 3) {
            return String.format("Còn %d ngày!", daysUntilExpiry);
        } else if (daysUntilExpiry <= 7) {
            return "Sắp hết hạn";
        } else {
            return null;
        }
    }

    /**
     * Generate future suggestion reason.
     */
    private String generateFutureSuggestionReason(PhieuGiamGia voucher, CustomerPurchaseProfile customerProfile) {
        long daysUntilAvailable = java.time.Duration.between(Instant.now(), voucher.getNgayBatDau()).toDays();

        if (daysUntilAvailable <= 7) {
            return String.format("Sẽ có hiệu lực trong %d ngày", daysUntilAvailable);
        } else {
            return "Phiếu giảm giá sắp tới phù hợp với bạn";
        }
    }

    // ==================== VOUCHER VALIDATION AND APPLICATION METHODS ====================

    /**
     * Validate if a voucher can be applied to an order.
     *
     * @param voucherCode The voucher code to validate
     * @param customer The customer applying the voucher
     * @param orderTotal The total order amount before voucher discount
     * @return VoucherValidationResult containing validation status and details
     */
    @Transactional(readOnly = true)
    public VoucherValidationResult validateVoucher(String voucherCode, NguoiDung customer, BigDecimal orderTotal) {
        // Step 1: Find voucher by code
        Optional<PhieuGiamGia> voucherOpt = phieuGiamGiaRepository.findByMaPhieuGiamGia(voucherCode);
        if (voucherOpt.isEmpty()) {
            return VoucherValidationResult.invalid("Voucher code not found: " + voucherCode);
        }

        PhieuGiamGia voucher = voucherOpt.get();

        // Step 2: Check voucher status and dates
        TrangThaiCampaign currentStatus = PhieuGiamGia.fromDates(voucher.getNgayBatDau(), voucher.getNgayKetThuc());
        if (currentStatus != TrangThaiCampaign.DA_DIEN_RA || voucher.getTrangThai().isCancelled()) {
            return VoucherValidationResult.invalid("Voucher is not currently active. Status: " + currentStatus);
        }

        // Step 3: Check usage limits
        if (voucher.getSoLuongDaDung() >= voucher.getSoLuongBanDau()) {
            return VoucherValidationResult.invalid("Voucher usage limit exceeded");
        }

        // Step 4: Check minimum order value
        if (voucher.getGiaTriDonHangToiThieu() != null && orderTotal.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
            return VoucherValidationResult.invalid("Order total " + orderTotal + " is below minimum required " + voucher.getGiaTriDonHangToiThieu());
        }

        // Step 5: Check private voucher eligibility using new simplified logic
        if (!voucher.isCustomerEligible(customer.getId())) {
            return VoucherValidationResult.invalid("Customer is not eligible for this voucher");
        }

        // Step 6: Calculate discount amount
        BigDecimal discountAmount = calculateDiscountAmount(voucher, orderTotal);

        return VoucherValidationResult.valid(voucher, discountAmount);
    }

    /**
     * Apply a voucher to an order and track the usage.
     *
     * @param voucher The validated voucher to apply
     * @param hoaDon The order to apply the voucher to
     * @param discountAmount The calculated discount amount
     */
    @Transactional
    public void applyVoucherToOrder(PhieuGiamGia voucher, HoaDon hoaDon, BigDecimal discountAmount) {
        // Step 1: Create HoaDonPhieuGiamGia relationship using native SQL to avoid transient entity issues
        try {
            // Use native SQL to insert the relationship directly without entity references
            hoaDonPhieuGiamGiaRepository.insertVoucherOrderRelationship(
                hoaDon.getId(),
                voucher.getId(),
                discountAmount
            );

            // Step 2: Increment voucher usage count
            voucher.setSoLuongDaDung(voucher.getSoLuongDaDung() + 1);
            phieuGiamGiaRepository.save(voucher);

        } catch (Exception e) {
            // Fallback to entity-based approach if native SQL fails
            log.warn("Native SQL insert failed, falling back to entity approach: {}", e.getMessage());

            HoaDonPhieuGiamGiaId relationshipId = new HoaDonPhieuGiamGiaId();
            relationshipId.setHoaDonId(hoaDon.getId());
            relationshipId.setPhieuGiamGiaId(voucher.getId());

            HoaDonPhieuGiamGia hoaDonPhieuGiamGia = new HoaDonPhieuGiamGia();
            hoaDonPhieuGiamGia.setId(relationshipId);
            // Don't set the full entities to avoid transient references
            hoaDonPhieuGiamGia.setGiaTriDaGiam(discountAmount);

            hoaDonPhieuGiamGiaRepository.save(hoaDonPhieuGiamGia);

            // Step 2: Increment voucher usage count
            voucher.setSoLuongDaDung(voucher.getSoLuongDaDung() + 1);
            phieuGiamGiaRepository.save(voucher);
        }
    }

    /**
     * Apply a voucher to an order using IDs only to avoid transient entity issues.
     *
     * @param voucherId The voucher ID
     * @param orderId The order ID
     * @param discountAmount The calculated discount amount
     */
    @Transactional
    public void applyVoucherToOrderById(Long voucherId, Long orderId, BigDecimal discountAmount) {
        try {
            // Use native SQL to insert the relationship directly without entity references
            hoaDonPhieuGiamGiaRepository.insertVoucherOrderRelationship(
                orderId,
                voucherId,
                discountAmount
            );

            // Increment voucher usage count using direct update
            phieuGiamGiaRepository.incrementUsageCount(voucherId);

            log.info("Applied voucher {} to order {} with discount {} using ID-based approach",
                    voucherId, orderId, discountAmount);

        } catch (Exception e) {
            log.error("Failed to apply voucher {} to order {} using ID-based approach: {}",
                     voucherId, orderId, e.getMessage());
            throw new RuntimeException("Failed to apply voucher: " + e.getMessage(), e);
        }
    }

    /**
     * Remove voucher from an order and decrement usage count.
     * Used when an order is cancelled.
     *
     * @param hoaDonId The order ID to remove vouchers from
     */
    @Transactional
    public void removeVouchersFromOrder(Long hoaDonId) {
        List<HoaDonPhieuGiamGia> appliedVouchers = hoaDonPhieuGiamGiaRepository.findByHoaDonId(hoaDonId);

        for (HoaDonPhieuGiamGia appliedVoucher : appliedVouchers) {
            PhieuGiamGia voucher = appliedVoucher.getPhieuGiamGia();

            // Decrement usage count
            if (voucher.getSoLuongDaDung() > 0) {
                voucher.setSoLuongDaDung(voucher.getSoLuongDaDung() - 1);
                phieuGiamGiaRepository.save(voucher);
            }

            // Remove the relationship
            hoaDonPhieuGiamGiaRepository.delete(appliedVoucher);
        }
    }

    /**
     * Calculate the discount amount based on voucher type and order total.
     * Enhanced with enum-based logic and validation.
     *
     * @param voucher The voucher to calculate discount for
     * @param orderTotal The total order amount
     * @return The calculated discount amount
     */
    private BigDecimal calculateDiscountAmount(PhieuGiamGia voucher, BigDecimal orderTotal) {
        if (voucher.getLoaiGiamGia() == null) {
            throw new IllegalArgumentException("Loại giảm giá không được để trống");
        }

        if (voucher.isPercentageDiscount()) {
            // Percentage discount
            BigDecimal percentage = voucher.getGiaTriGiam().divide(BigDecimal.valueOf(100));
            return orderTotal.multiply(percentage);
        } else {
            // Fixed amount discount
            return voucher.getGiaTriGiam().min(orderTotal); // Don't exceed order total
        }
    }

    /**
     * Check if a customer is eligible for a specific voucher.
     * Uses the new simplified relationship-based logic.
     *
     * @param voucherCode The voucher code to check
     * @param customerId The customer ID
     * @return true if eligible, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isCustomerEligible(String voucherCode, Long customerId) {
        Optional<PhieuGiamGia> voucherOpt = phieuGiamGiaRepository.findByMaPhieuGiamGia(voucherCode);
        if (voucherOpt.isEmpty()) {
            return false;
        }

        PhieuGiamGia voucher = voucherOpt.get();

        // Use the new simplified logic from the entity
        return voucher.isCustomerEligible(customerId);
    }

    /**
     * Get voucher by code and convert to DTO
     *
     * @param voucherCode The voucher code
     * @return PhieuGiamGiaDto
     * @throws IllegalArgumentException if voucher not found
     */
    @Transactional(readOnly = true)
    public PhieuGiamGiaDto getPhieuGiamGiaByCode(String voucherCode) {
        PhieuGiamGia voucher = phieuGiamGiaRepository.findByMaPhieuGiamGia(voucherCode)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại với mã: " + voucherCode));

        return phieuGiamGiaMapper.toDto(voucher);
    }

    /**
     * Search vouchers by query string (code or description)
     *
     * @param query Search query
     * @return List of matching vouchers as DTOs
     */
    @Transactional(readOnly = true)
    public List<PhieuGiamGiaDto> searchVouchers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<PhieuGiamGia> vouchers = phieuGiamGiaRepository.findByMaPhieuGiamGiaContainingIgnoreCaseOrMoTaContainingIgnoreCase(
                query.trim(), query.trim());

        return vouchers.stream()
                .map(phieuGiamGiaMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get available vouchers for a specific customer and order total
     *
     * @param customerId Customer ID (can be null for public vouchers)
     * @param orderTotal Order total for minimum value validation
     * @return List of available vouchers as DTOs
     */
    @Transactional(readOnly = true)
    public List<PhieuGiamGiaDto> getAvailableVouchers(Long customerId, BigDecimal orderTotal) {
        // Get all currently active vouchers
        List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findByTrangThai(TrangThaiCampaign.DA_DIEN_RA);

        return activeVouchers.stream()
                .filter(voucher -> {
                    // Check if voucher has remaining usage
                    if (voucher.getSoLuongDaDung() >= voucher.getSoLuongBanDau()) {
                        return false;
                    }

                    // Check minimum order value
                    if (voucher.getGiaTriDonHangToiThieu() != null &&
                        orderTotal.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
                        return false;
                    }

                    // Check customer eligibility
                    if (customerId != null) {
                        return voucher.isCustomerEligible(customerId);
                    } else {
                        // If no customer provided, only return public vouchers
                        return voucher.getDanhSachNguoiDung().isEmpty();
                    }
                })
                .map(phieuGiamGiaMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find the best voucher for a customer and order total.
     * Automatically selects the voucher that provides maximum discount.
     *
     * @param customerId Customer ID (can be null for public vouchers)
     * @param orderTotal Order total for minimum value validation
     * @return BestVoucherResult containing the best voucher and discount amount
     */
    @Transactional(readOnly = true)
    public BestVoucherResult findBestVoucher(Long customerId, BigDecimal orderTotal) {
        // Get all currently active vouchers
        List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findByTrangThai(TrangThaiCampaign.DA_DIEN_RA);

        PhieuGiamGia bestVoucher = null;
        BigDecimal maxDiscount = BigDecimal.ZERO;

        for (PhieuGiamGia voucher : activeVouchers) {
            // Check if voucher has remaining usage
            if (voucher.getSoLuongDaDung() >= voucher.getSoLuongBanDau()) {
                continue;
            }

            // Check minimum order value
            if (voucher.getGiaTriDonHangToiThieu() != null &&
                orderTotal.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
                continue;
            }

            // Check customer eligibility
            boolean isEligible;
            if (customerId != null) {
                isEligible = voucher.isCustomerEligible(customerId);
            } else {
                // If no customer provided, only consider public vouchers
                isEligible = voucher.getDanhSachNguoiDung().isEmpty();
            }

            if (!isEligible) {
                continue;
            }

            // Calculate discount amount for this voucher
            BigDecimal discountAmount = calculateDiscountAmount(voucher, orderTotal);

            // Check if this voucher provides better discount
            if (discountAmount.compareTo(maxDiscount) > 0) {
                maxDiscount = discountAmount;
                bestVoucher = voucher;
            }
        }

        if (bestVoucher != null) {
            PhieuGiamGiaDto voucherDto = phieuGiamGiaMapper.toDto(bestVoucher);
            return BestVoucherResult.found(voucherDto, maxDiscount);
        } else {
            return BestVoucherResult.notFound("No applicable vouchers found for this order");
        }
    }

    /**
     * Get multiple best vouchers (top N) for a customer and order total.
     * Useful for showing customers their best options.
     *
     * @param customerId Customer ID (can be null for public vouchers)
     * @param orderTotal Order total for minimum value validation
     * @param limit Maximum number of vouchers to return
     * @return List of best vouchers sorted by discount amount (highest first)
     */
    @Transactional(readOnly = true)
    public List<BestVoucherResult> findTopVouchers(Long customerId, BigDecimal orderTotal, int limit) {
        // Get all currently active vouchers
        List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findByTrangThai(TrangThaiCampaign.DA_DIEN_RA);

        List<BestVoucherResult> voucherResults = new ArrayList<>();

        for (PhieuGiamGia voucher : activeVouchers) {
            // Check if voucher has remaining usage
            if (voucher.getSoLuongDaDung() >= voucher.getSoLuongBanDau()) {
                continue;
            }

            // Check minimum order value
            if (voucher.getGiaTriDonHangToiThieu() != null &&
                orderTotal.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
                continue;
            }

            // Check customer eligibility
            boolean isEligible;
            if (customerId != null) {
                isEligible = voucher.isCustomerEligible(customerId);
            } else {
                // If no customer provided, only consider public vouchers
                isEligible = voucher.getDanhSachNguoiDung().isEmpty();
            }

            if (!isEligible) {
                continue;
            }

            // Calculate discount amount for this voucher
            BigDecimal discountAmount = calculateDiscountAmount(voucher, orderTotal);
            PhieuGiamGiaDto voucherDto = phieuGiamGiaMapper.toDto(voucher);
            voucherResults.add(BestVoucherResult.found(voucherDto, discountAmount));
        }

        // Sort by discount amount (highest first) and limit results
        return voucherResults.stream()
                .sorted((a, b) -> b.getDiscountAmount().compareTo(a.getDiscountAmount()))
                .limit(limit)
                .collect(Collectors.toList());
    }



    /**
     * Get applied vouchers for an order
     *
     * @param orderId Order ID
     * @return List of applied vouchers as DTOs
     */
    @Transactional(readOnly = true)
    public List<PhieuGiamGiaDto> getAppliedVouchersForOrder(Long orderId) {
        // Get vouchers applied to the order through HoaDonPhieuGiamGia relationships
        List<HoaDonPhieuGiamGia> appliedVouchers = hoaDonPhieuGiamGiaRepository.findByHoaDonId(orderId);

        return appliedVouchers.stream()
                .map(hoaDonPhieuGiamGia -> phieuGiamGiaMapper.toDto(hoaDonPhieuGiamGia.getPhieuGiamGia()))
                .collect(Collectors.toList());
    }

    /**
     * Result class for voucher validation.
     */
    public static class VoucherValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final PhieuGiamGia voucher;
        private final BigDecimal discountAmount;

        private VoucherValidationResult(boolean valid, String errorMessage, PhieuGiamGia voucher, BigDecimal discountAmount) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.voucher = voucher;
            this.discountAmount = discountAmount;
        }

        public static VoucherValidationResult valid(PhieuGiamGia voucher, BigDecimal discountAmount) {
            return new VoucherValidationResult(true, null, voucher, discountAmount);
        }

        public static VoucherValidationResult invalid(String errorMessage) {
            return new VoucherValidationResult(false, errorMessage, null, null);
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public PhieuGiamGia getVoucher() { return voucher; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
    }

    /**
     * Result class for best voucher selection.
     */
    public static class BestVoucherResult {
        private final boolean found;
        private final String message;
        private final PhieuGiamGiaDto voucher;
        private final BigDecimal discountAmount;

        private BestVoucherResult(boolean found, String message, PhieuGiamGiaDto voucher, BigDecimal discountAmount) {
            this.found = found;
            this.message = message;
            this.voucher = voucher;
            this.discountAmount = discountAmount;
        }

        public static BestVoucherResult found(PhieuGiamGiaDto voucher, BigDecimal discountAmount) {
            return new BestVoucherResult(true, "Best voucher found", voucher, discountAmount);
        }

        public static BestVoucherResult notFound(String message) {
            return new BestVoucherResult(false, message, null, BigDecimal.ZERO);
        }

        // Getters
        public boolean isFound() { return found; }
        public String getMessage() { return message; }
        public PhieuGiamGiaDto getVoucher() { return voucher; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
    }

    // ==================== BUSINESSENTITYSERVICE ABSTRACT METHOD IMPLEMENTATIONS ====================

    @Override
    protected JpaRepository<PhieuGiamGia, Long> getRepository() {
        return phieuGiamGiaRepository;
    }

    @Override
    protected JpaRepository<PhieuGiamGiaAuditHistory, Long> getAuditRepository() {
        return auditHistoryRepository;
    }

    @Override
    protected PhieuGiamGiaDto toDto(PhieuGiamGia entity) {
        return phieuGiamGiaMapper.toDto(entity);
    }

    @Override
    protected PhieuGiamGia toEntity(PhieuGiamGiaDto dto) {
        return phieuGiamGiaDtoMapper.toEntity(dto);
    }

    @Override
    protected PhieuGiamGiaAuditHistory createAuditEntry(Long entityId, String action, String oldValues, String newValues, String nguoiThucHien, String lyDo) {
        switch (action) {
            case "CREATE":
                return PhieuGiamGiaAuditHistory.createEntry(entityId, newValues, nguoiThucHien, lyDo);
            case "UPDATE":
                return PhieuGiamGiaAuditHistory.updateEntry(entityId, oldValues, newValues, nguoiThucHien, lyDo);
            case "SOFT_DELETE":
            case "DELETE":
                return PhieuGiamGiaAuditHistory.deleteEntry(entityId, oldValues, nguoiThucHien, lyDo);
            default:
                return PhieuGiamGiaAuditHistory.createEntry(entityId, newValues, nguoiThucHien, lyDo);
        }
    }

    @Override
    protected String getEntityName() {
        return "Phiếu giảm giá";
    }

    @Override
    protected void validateEntity(PhieuGiamGia entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Phiếu giảm giá không được null");
        }

        // Validate voucher code
        ValidationUtils.validateProductCode(entity.getMaPhieuGiamGia(), "Mã phiếu giảm giá");

        // Validate discount value
        ValidationUtils.validatePositiveAmount(entity.getGiaTriGiam(), "Giá trị giảm");

        // Validate minimum order value
        if (entity.getGiaTriDonHangToiThieu() != null) {
            ValidationUtils.validateAmount(entity.getGiaTriDonHangToiThieu(), "Giá trị đơn hàng tối thiểu");
        }

        // Validate quantities
        ValidationUtils.validateQuantity(entity.getSoLuongBanDau(), "Số lượng ban đầu");
        ValidationUtils.validateQuantity(entity.getSoLuongDaDung(), "Số lượng đã dùng");

        // Validate date range
        if (entity.getNgayBatDau() != null && entity.getNgayKetThuc() != null) {
            if (entity.getNgayKetThuc().isBefore(entity.getNgayBatDau())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
            }
        }
    }

    @Override
    protected void evictCache() {
        // Cache eviction is handled by @CacheEvict annotations in BusinessEntityService
        log.debug("Cache evicted for PhieuGiamGia");
    }

    @Override
    protected Long getEntityId(PhieuGiamGia entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(PhieuGiamGia entity, Long id) {
        entity.setId(id);
    }

    @Override
    protected void setSoftDeleteStatus(PhieuGiamGia entity, boolean status) {
        // PhieuGiamGia uses TrangThaiCampaign.BI_HUY for soft delete
        if (!status) {
            entity.setTrangThai(TrangThaiCampaign.BI_HUY);
        }
    }

    @Override
    protected List<PhieuGiamGiaAuditHistory> getAuditHistoryByEntityId(Long entityId) {
        return auditHistoryRepository.findByPhieuGiamGiaIdOrderByThoiGianThayDoiDesc(entityId);
    }

    @Override
    protected ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    @Override
    protected String getCacheName() {
        return "phieuGiamGiaCache";
    }

    @Override
    protected void publishEntityCreatedEvent(PhieuGiamGia entity) {
        // TODO: Implement voucher created event publishing for real-time updates
        log.debug("Publishing voucher created event for ID: {}", entity.getId());
    }

    @Override
    protected void publishEntityUpdatedEvent(PhieuGiamGia entity, PhieuGiamGia oldEntity) {
        // TODO: Implement voucher updated event publishing for real-time updates
        log.debug("Publishing voucher updated event for ID: {}", entity.getId());
    }

    @Override
    protected void publishEntityDeletedEvent(Long entityId) {
        // TODO: Implement voucher deleted event publishing for real-time updates
        log.debug("Publishing voucher deleted event for ID: {}", entityId);
    }

    @Override
    protected void validateBusinessRules(PhieuGiamGia entity) {
        // Validate voucher-specific business rules
        if (entity.getSoLuongDaDung() > entity.getSoLuongBanDau()) {
            throw new IllegalArgumentException("Số lượng đã dùng không được vượt quá số lượng ban đầu");
        }

        // Validate discount type and value
        if (entity.getLoaiGiamGia() == LoaiGiamGia.PHAN_TRAM) {
            ValidationUtils.validatePercentage(entity.getGiaTriGiam(), "Phần trăm giảm");
        }
    }

    @Override
    protected void validateBusinessRulesForUpdate(PhieuGiamGia entity, PhieuGiamGia existingEntity) {
        validateBusinessRules(entity);

        // Additional validation for updates
        if (existingEntity.getTrangThai() == TrangThaiCampaign.KET_THUC) {
            throw new IllegalArgumentException("Không thể cập nhật phiếu giảm giá đã kết thúc");
        }

        if (existingEntity.getTrangThai() == TrangThaiCampaign.BI_HUY) {
            throw new IllegalArgumentException("Không thể cập nhật phiếu giảm giá đã bị hủy");
        }
    }

    @Override
    protected PhieuGiamGia cloneEntity(PhieuGiamGia entity) {
        PhieuGiamGia clone = new PhieuGiamGia();
        clone.setId(entity.getId());
        clone.setMaPhieuGiamGia(entity.getMaPhieuGiamGia());
        clone.setLoaiGiamGia(entity.getLoaiGiamGia());
        clone.setGiaTriGiam(entity.getGiaTriGiam());
        clone.setGiaTriDonHangToiThieu(entity.getGiaTriDonHangToiThieu());
        clone.setNgayBatDau(entity.getNgayBatDau());
        clone.setNgayKetThuc(entity.getNgayKetThuc());
        clone.setSoLuongBanDau(entity.getSoLuongBanDau());
        clone.setSoLuongDaDung(entity.getSoLuongDaDung());
        clone.setTrangThai(entity.getTrangThai());
        clone.setMoTa(entity.getMoTa());
        return clone;
    }

    // ==================== AUDIT TRAIL HELPER METHODS ====================

    /**
     * Build JSON representation of voucher for audit trail
     * @param phieu the voucher entity
     * @return JSON string representation
     */
    @Override
    protected String buildAuditJson(PhieuGiamGia phieu) {
        if (phieu == null) return "{}";

        try {
            // Safely check if it's a private voucher
            boolean isPrivate = false;
            try {
                isPrivate = phieu.isPrivateVoucher();
            } catch (Exception e) {
                System.err.println("Warning: Could not determine if voucher is private: " + e.getMessage());
                isPrivate = false;
            }

            return String.format(
                "{\"maPhieuGiamGia\":\"%s\",\"loaiGiamGia\":\"%s\",\"trangThai\":\"%s\"," +
                "\"giaTriGiam\":%s,\"giaTriDonHangToiThieu\":%s,\"ngayBatDau\":\"%s\"," +
                "\"ngayKetThuc\":\"%s\",\"soLuongBanDau\":%d,\"soLuongDaDung\":%d," +
                "\"moTa\":\"%s\",\"isPrivateVoucher\":%s}",
                phieu.getMaPhieuGiamGia() != null ? phieu.getMaPhieuGiamGia() : "",
                phieu.getLoaiGiamGia() != null ? phieu.getLoaiGiamGia().name() : "",
                phieu.getTrangThai() != null ? phieu.getTrangThai().name() : "",
                phieu.getGiaTriGiam() != null ? phieu.getGiaTriGiam() : "null",
                phieu.getGiaTriDonHangToiThieu() != null ? phieu.getGiaTriDonHangToiThieu() : "null",
                phieu.getNgayBatDau() != null ? phieu.getNgayBatDau().toString() : "",
                phieu.getNgayKetThuc() != null ? phieu.getNgayKetThuc().toString() : "",
                phieu.getSoLuongBanDau() != null ? phieu.getSoLuongBanDau() : 0,
                phieu.getSoLuongDaDung() != null ? phieu.getSoLuongDaDung() : 0,
                phieu.getMoTa() != null ? phieu.getMoTa().replace("\"", "\\\"") : "",
                isPrivate
            );
        } catch (Exception e) {
            System.err.println("Error building audit JSON: " + e.getMessage());
            return "{\"error\":\"Could not serialize voucher data\"}";
        }
    }


}