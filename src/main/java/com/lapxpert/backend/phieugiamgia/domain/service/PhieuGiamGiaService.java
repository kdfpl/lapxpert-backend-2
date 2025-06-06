package com.lapxpert.backend.phieugiamgia.domain.service;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.common.enums.LoaiGiamGia;
import com.lapxpert.backend.common.service.EmailService;
import com.lapxpert.backend.common.service.VietnamTimeZoneService;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.application.mapper.PhieuGiamGiaMapper;
import com.lapxpert.backend.phieugiamgia.application.mapper.PhieuGiamGiaDtoMapper;
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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhieuGiamGiaService {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PhieuGiamGiaNguoiDungRepository phieuGiamGiaNguoiDungRepository;
    private final PhieuGiamGiaAuditHistoryRepository auditHistoryRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final EmailService emailService;
    private final HoaDonPhieuGiamGiaRepository hoaDonPhieuGiamGiaRepository;
    private final PhieuGiamGiaMapper phieuGiamGiaMapper;
    private final VietnamTimeZoneService vietnamTimeZoneService;
    private final PhieuGiamGiaDtoMapper phieuGiamGiaDtoMapper;

    public List<PhieuGiamGiaDto> getAllPhieuGiamGia() {
        List<PhieuGiamGia> phieuGiamGias = phieuGiamGiaRepository.findAll();
        return phieuGiamGiaDtoMapper.toDtosWithTimezone(phieuGiamGias);
    }

    @Transactional
    public PhieuGiamGiaDto taoPhieu(PhieuGiamGiaDto req) {
        return taoPhieuWithAudit(req, "Tạo phiếu giảm giá mới");
    }

    @Transactional
    public PhieuGiamGiaDto taoPhieuWithAudit(PhieuGiamGiaDto req, String reason) {
        // Use enhanced mapper for entity conversion
        PhieuGiamGia phieu = phieuGiamGiaDtoMapper.toEntity(req);

        // Set defaults if needed
        if (phieu.getLoaiGiamGia() == null) {
            phieu.setLoaiGiamGia(LoaiGiamGia.SO_TIEN_CO_DINH);
        }
        if (phieu.getSoLuongBanDau() == null) {
            phieu.setSoLuongBanDau(0);
        }
        if (phieu.getSoLuongDaDung() == null) {
            phieu.setSoLuongDaDung(0);
        }
        phieu.setMoTa(req.getMoTa());

        // Note: Audit information is handled by PhieuGiamGiaAuditHistory, not inline audit fields

        // Validation will be handled by @PrePersist in entity
        PhieuGiamGia savedPhieu = phieuGiamGiaRepository.save(phieu);

        // Save audit history entry for creation
        String newValues = buildAuditJson(savedPhieu);
        PhieuGiamGiaAuditHistory auditEntry = PhieuGiamGiaAuditHistory.createEntry(
            savedPhieu.getId(),
            newValues,
            savedPhieu.getNguoiTao(),
            reason != null ? reason : "Tạo phiếu giảm giá mới"
        );
        auditHistoryRepository.save(auditEntry);

        // Create user assignments for private vouchers
        if (req.getDanhSachNguoiDung() != null && !req.getDanhSachNguoiDung().isEmpty()) {
            // Nếu phiếu giảm giá là riêng tư
            for (Long nguoiDungId : req.getDanhSachNguoiDung()) {
                PhieuGiamGiaNguoiDungId id = new PhieuGiamGiaNguoiDungId(phieu.getId(), nguoiDungId);
                PhieuGiamGiaNguoiDung phieuND = new PhieuGiamGiaNguoiDung();
                phieuND.setId(id);
                phieuND.setPhieuGiamGia(phieu);
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
                    + "LapXpert trân trọng gửi đến bạn phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "** "
                    + "với những ưu đãi đặc biệt. Mã này được tạo dành riêng cho bạn và có hiệu lực từ ngày "
                    + phieu.getNgayBatDau() + " đến " + phieu.getNgayKetThuc() + ".\n\n"
                    +"Phiếu sẽ được áp dụng với hóa đơn từ"+phieu.getGiaTriDonHangToiThieu()+".\n\n"
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
                    + "Chúng tôi vừa phát hành Phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "** áp dụng cho tất cả khách hàng, "
                    + "có hiệu lực từ ngày " + phieu.getNgayBatDau() + " đến " + phieu.getNgayKetThuc() + ".\n\n"
                    + "Nhanh tay sử dụng để nhận ưu đãi hấp dẫn!\n\n"
                    + "Trân trọng,\nLapXpert Team";

            // Log notification for all customers
            log.info("[VOUCHER NOTIFICATION] Sending to {} all customers: {}",
                allCustomerEmails.size(), subject);
            log.debug("[VOUCHER NOTIFICATION] Email content: {}", text);

            // TODO: Implement actual email sending
            // emailService.sendBulkEmail(allCustomerEmails, subject, text);
        }

        // Return the created voucher as DTO
        return phieuGiamGiaMapper.toDto(savedPhieu);
    }
    @Transactional
    public void capNhatPhieu(PhieuGiamGiaDto req, Long phieuId) {
        capNhatPhieuWithAudit(req, phieuId, "Cập nhật thông tin phiếu giảm giá");
    }

    @Transactional
    public void capNhatPhieuWithAudit(PhieuGiamGiaDto req, Long phieuId, String reason) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(phieuId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu"));

        // Capture old values for audit trail
        String oldValues = buildAuditJson(phieu);
        TrangThaiCampaign trangThaiCu = phieu.getTrangThai(); // Lưu lại trạng thái cũ

        // Cập nhật thông tin phiếu giảm giá
        phieu.setMaPhieuGiamGia(req.getMaPhieuGiamGia());
        phieu.setGiaTriGiam(req.getGiaTriGiam());
        phieu.setGiaTriDonHangToiThieu(req.getGiaTriDonHangToiThieu());
        phieu.setNgayBatDau(req.getNgayBatDau());
        phieu.setNgayKetThuc(req.getNgayKetThuc());
        // Set discount type using enum
        phieu.setLoaiGiamGia(req.getLoaiGiamGia() != null ? req.getLoaiGiamGia() : LoaiGiamGia.SO_TIEN_CO_DINH);
        phieu.setSoLuongBanDau(req.getSoLuongBanDau());
        phieu.setTrangThai(PhieuGiamGia.fromDates(req.getNgayBatDau(), req.getNgayKetThuc()));
        phieu.setMoTa(req.getMoTa());

        // Save audit history entry for update
        String newValues = buildAuditJson(phieu);
        PhieuGiamGiaAuditHistory auditEntry = PhieuGiamGiaAuditHistory.updateEntry(
            phieu.getId(),
            oldValues,
            newValues,
            phieu.getNguoiCapNhat(),
            reason != null ? reason : "Cập nhật thông tin phiếu giảm giá"
        );
        auditHistoryRepository.save(auditEntry);

        phieuGiamGiaRepository.save(phieu);

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
        PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu không tồn tại với ID: " + id));

        return phieuGiamGiaMapper.toDto(phieuGiamGia);
    }

    /**
     * Get complete audit history for a voucher
     * @param phieuGiamGiaId ID of the voucher
     * @return List of audit history entries
     */
    public List<PhieuGiamGiaAuditHistory> getAuditHistory(Long phieuGiamGiaId) {
        return auditHistoryRepository.findByPhieuGiamGiaIdOrderByThoiGianThayDoiDesc(phieuGiamGiaId);
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

    // ==================== AUDIT TRAIL HELPER METHODS ====================

    /**
     * Build JSON representation of voucher for audit trail
     * @param phieu the voucher entity
     * @return JSON string representation
     */
    private String buildAuditJson(PhieuGiamGia phieu) {
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