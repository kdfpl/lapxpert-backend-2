package com.lapxpert.backend.dotgiamgia.service;

import com.lapxpert.backend.dotgiamgia.dto.DotGiamGiaDto;
import com.lapxpert.backend.dotgiamgia.dto.DotGiamGiaAuditHistoryDto;
import com.lapxpert.backend.dotgiamgia.dto.DotGiamGiaMapper;
import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.common.service.BusinessEntityService;
import com.lapxpert.backend.common.service.VietnamTimeZoneService;
import com.lapxpert.backend.common.util.ExceptionHandlingUtils;
import com.lapxpert.backend.common.util.ValidationUtils;
import com.lapxpert.backend.dotgiamgia.entity.DotGiamGia;
import com.lapxpert.backend.dotgiamgia.entity.DotGiamGiaAuditHistory;
import com.lapxpert.backend.dotgiamgia.exception.CampaignConflictException;
import com.lapxpert.backend.dotgiamgia.exception.CampaignNotFoundException;
import com.lapxpert.backend.dotgiamgia.exception.CampaignValidationException;
import com.lapxpert.backend.dotgiamgia.repository.DotGiamGiaRepository;
import com.lapxpert.backend.dotgiamgia.repository.DotGiamGiaAuditHistoryRepository;
import com.lapxpert.backend.sanpham.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.mapper.SanPhamChiTietMapper;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.repository.SanPhamChiTietRepository;
import com.lapxpert.backend.common.event.VoucherChangeEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DotGiamGiaService extends BusinessEntityService<DotGiamGia, Long, DotGiamGiaDto, DotGiamGiaAuditHistory> {
    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final DotGiamGiaAuditHistoryRepository auditHistoryRepository;
    private final DotGiamGiaMapper dotGiamGiaMapper;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamChiTietMapper sanPhamChiTietMapper;
    private final VietnamTimeZoneService vietnamTimeZoneService;
    private final ApplicationEventPublisher eventPublisher;

    public DotGiamGiaService(DotGiamGiaRepository dotGiamGiaRepository,
                           DotGiamGiaAuditHistoryRepository auditHistoryRepository,
                           @Qualifier("dotGiamGiaMapperImpl") DotGiamGiaMapper dotGiamGiaMapper,
                           SanPhamChiTietRepository sanPhamChiTietRepository,
                           SanPhamChiTietMapper sanPhamChiTietMapper,
                           VietnamTimeZoneService vietnamTimeZoneService,
                           ApplicationEventPublisher eventPublisher) {
        this.dotGiamGiaRepository = dotGiamGiaRepository;
        this.auditHistoryRepository = auditHistoryRepository;
        this.dotGiamGiaMapper = dotGiamGiaMapper;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.sanPhamChiTietMapper = sanPhamChiTietMapper;
        this.vietnamTimeZoneService = vietnamTimeZoneService;
        this.eventPublisher = eventPublisher;
    }

    public List<DotGiamGiaDto> findAll() {
        // Fixed self-invocation: directly implement instead of calling super.findAll()
        try {
            List<DotGiamGia> entities = dotGiamGiaRepository.findAll();
            List<DotGiamGiaDto> allCampaigns = entities.stream()
                    .map(dotGiamGiaMapper::toDto)
                    .toList();

            // Filter out cancelled campaigns (BI_HUY) for public API
            return allCampaigns.stream()
                    .filter(campaign -> campaign.getTrangThai() != TrangThaiCampaign.BI_HUY)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Lỗi khi tìm tất cả đợt giảm giá: {}", e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                "Không thể tải danh sách đợt giảm giá", e);
        }
    }

    // ==================== ADMIN MANAGEMENT METHODS ====================

    /**
     * Find all campaigns including hidden ones (admin only)
     */
    @Transactional(readOnly = true)
    public List<DotGiamGiaDto> findAllIncludingHidden() {
        return dotGiamGiaMapper.toDtos(dotGiamGiaRepository.findAll());
    }

    /**
     * Find only cancelled campaigns (admin only)
     */
    @Transactional(readOnly = true)
    public List<DotGiamGiaDto> findCancelledCampaigns() {
        return dotGiamGiaMapper.toDtos(dotGiamGiaRepository.findByTrangThai(TrangThaiCampaign.BI_HUY));
    }

    /**
     * Restore a cancelled campaign (change status from BI_HUY)
     */
    @Transactional
    public DotGiamGiaDto restoreCampaign(Long id) {
        DotGiamGia campaign = dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new CampaignNotFoundException(id));

        // Capture old values for audit
        String oldValues = buildAuditJson(campaign);

        // Restore campaign by updating status based on dates
        campaign.activate();
        DotGiamGia savedCampaign = dotGiamGiaRepository.save(campaign);

        // Create audit trail entry for restoration
        DotGiamGiaAuditHistory auditEntry = DotGiamGiaAuditHistory.updateEntry(
            savedCampaign.getId(),
            oldValues,
            buildAuditJson(savedCampaign),
            savedCampaign.getNguoiCapNhat(),
            "Khôi phục chiến dịch từ trạng thái bị hủy"
        );
        auditHistoryRepository.save(auditEntry);

        return dotGiamGiaMapper.toDto(savedCampaign);
    }

    /**
     * Batch restore multiple campaigns
     */
    @Transactional
    public List<DotGiamGiaDto> restoreMultipleCampaigns(List<Long> ids) {
        List<DotGiamGia> campaigns = dotGiamGiaRepository.findAllById(ids);

        for (DotGiamGia campaign : campaigns) {
            // Capture old values for audit
            String oldValues = buildAuditJson(campaign);

            // Restore campaign by updating status based on dates
            campaign.activate();
            DotGiamGia savedCampaign = dotGiamGiaRepository.save(campaign);

            // Create audit trail entry for restoration
            DotGiamGiaAuditHistory auditEntry = DotGiamGiaAuditHistory.updateEntry(
                savedCampaign.getId(),
                oldValues,
                buildAuditJson(savedCampaign),
                savedCampaign.getNguoiCapNhat(),
                "Khôi phục hàng loạt chiến dịch từ trạng thái bị hủy"
            );
            auditHistoryRepository.save(auditEntry);
        }

        return dotGiamGiaMapper.toDtos(campaigns);
    }

    @Transactional(readOnly = true)
    public DotGiamGiaDto getDotGiamGiaById(Long id) {
        // Use inherited findById method with caching from BusinessEntityService
        return findById(id)
                .orElseThrow(() -> new CampaignNotFoundException(id));
    }

    @Transactional
    public ResponseEntity<DotGiamGiaDto> save(DotGiamGiaDto dto) {
        return saveWithAudit(dto, null);
    }

    /**
     * Save discount campaign with audit trail support
     * Following PhieuGiamGia pattern exactly for proper audit capture
     */
    @Transactional
    public ResponseEntity<DotGiamGiaDto> saveWithAudit(DotGiamGiaDto dto, String lyDoThayDoi) {
        // Validate campaign data
        validateCampaignData(dto);

        // Update status based on dates using Vietnam timezone
        dto.setTrangThai(DotGiamGia.fromDates(dto.getNgayBatDau(), dto.getNgayKetThuc()));

        if (dto.getId() != null) {
            // Update existing campaign using inherited update method
            String auditReason = lyDoThayDoi != null && !lyDoThayDoi.trim().isEmpty()
                ? lyDoThayDoi.trim()
                : "Cập nhật thông tin đợt giảm giá";

            DotGiamGiaDto updatedDto = update(dto.getId(), dto, "SYSTEM", auditReason);
            return ResponseEntity.ok(updatedDto);
        } else {
            // Create new campaign using inherited create method
            DotGiamGiaDto createdDto = create(dto, "SYSTEM", "Tạo đợt giảm giá mới");
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
        }
    }

    /**
     * Validate campaign data before saving
     */
    private void validateCampaignData(DotGiamGiaDto dto) {
        if (dto.getMaDotGiamGia() == null || dto.getMaDotGiamGia().trim().isEmpty()) {
            throw new CampaignValidationException("Mã đợt giảm giá không được để trống");
        }

        if (dto.getTenDotGiamGia() == null || dto.getTenDotGiamGia().trim().isEmpty()) {
            throw new CampaignValidationException("Tên đợt giảm giá không được để trống");
        }

        if (dto.getPhanTramGiam() == null) {
            throw new CampaignValidationException("Phần trăm giảm không được để trống");
        }

        if (dto.getPhanTramGiam().compareTo(BigDecimal.ZERO) <= 0 ||
            dto.getPhanTramGiam().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new CampaignValidationException("Phần trăm giảm phải từ 0.01% đến 100%");
        }

        if (dto.getNgayBatDau() == null || dto.getNgayKetThuc() == null) {
            throw new CampaignValidationException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }

        if (dto.getNgayKetThuc().isBefore(dto.getNgayBatDau()) ||
            dto.getNgayKetThuc().equals(dto.getNgayBatDau())) {
            throw new CampaignValidationException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Campaign must run for at least 1 hour
        if (dto.getNgayKetThuc().isBefore(dto.getNgayBatDau().plusSeconds(3600))) {
            throw new CampaignValidationException("Đợt giảm giá phải diễn ra ít nhất 1 giờ");
        }
    }

    @Transactional
    public ResponseEntity<DotGiamGiaDto> toggle(Long id) {
        DotGiamGia entity = dotGiamGiaRepository.findById(id).orElse(null);
        if (entity != null) {
            // Store old values for audit
            String oldValues = buildAuditJson(entity);

            // Tạo một bản copy của Set để tránh ConcurrentModificationException khi duyệt và sửa đổi
            Set<SanPhamChiTiet> associatedProducts = Set.copyOf(entity.getSanPhamChiTiets());
            for (SanPhamChiTiet spct : associatedProducts) {
                spct.getDotGiamGias().remove(entity);
            }
            entity.getSanPhamChiTiets().clear();
            // Set status to BI_HUY for soft delete, allowing deletion regardless of current status
            entity.setTrangThai(TrangThaiCampaign.BI_HUY);
            DotGiamGia savedEntity = dotGiamGiaRepository.save(entity);

            // Create audit entry for deletion/closure
            DotGiamGiaAuditHistory auditEntry = DotGiamGiaAuditHistory.deleteEntry(
                savedEntity.getId(),
                oldValues,
                savedEntity.getNguoiCapNhat(),
                "Đóng đợt giảm giá"
            );
            auditHistoryRepository.save(auditEntry);

            return ResponseEntity.ok(dotGiamGiaMapper.toDto(savedEntity));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public ResponseEntity<List<DotGiamGiaDto>> toggleMultiple(List<Long> ids) {
        List<DotGiamGia> entities = dotGiamGiaRepository.findAllById(ids);
        if (!entities.isEmpty()) {
            for (DotGiamGia entity : entities) {
                // Tạo một bản copy của Set để tránh ConcurrentModificationException
                Set<SanPhamChiTiet> associatedProducts = Set.copyOf(entity.getSanPhamChiTiets());
                for (SanPhamChiTiet spct : associatedProducts) {
                    spct.getDotGiamGias().remove(entity);
                }
                entity.getSanPhamChiTiets().clear();
                // Set status to BI_HUY for soft delete, allowing deletion regardless of current status
                entity.setTrangThai(TrangThaiCampaign.BI_HUY);
            }
            List<DotGiamGia> savedEntities = dotGiamGiaRepository.saveAll(entities);
            return ResponseEntity.ok(dotGiamGiaMapper.toDtos(savedEntities));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public Set<SanPhamChiTietDto> findAllSanPhamChiTietsByDotGiamGiaId(Long id) {
        DotGiamGia dgg = dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new CampaignNotFoundException(id));
        Set<SanPhamChiTiet> sanPhamChiTiets = dgg.getSanPhamChiTiets();
        return sanPhamChiTietMapper.toDtoSet(sanPhamChiTiets);
    }

    @Transactional
    public DotGiamGiaDto addSanPhamChiTiets(Long dotGiamGiaId, List<Long> sanPhamChiTietIds) {
        DotGiamGia dotGiamGia = dotGiamGiaRepository.findById(dotGiamGiaId)
                .orElseThrow(() -> new CampaignNotFoundException(dotGiamGiaId));

        for (Long sanPhamChiTietId : sanPhamChiTietIds) {
            SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietRepository.findById(sanPhamChiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm chi tiết với ID: " + sanPhamChiTietId));
            dotGiamGia.getSanPhamChiTiets().add(sanPhamChiTiet);
            sanPhamChiTiet.getDotGiamGias().add(dotGiamGia);
        }
        return dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(dotGiamGia));
    }

    @Transactional
    public DotGiamGiaDto removeSanPhamChiTiets(Long dotGiamGiaId, List<Long> sanPhamChiTietIds) {
        DotGiamGia dotGiamGia = dotGiamGiaRepository.findById(dotGiamGiaId)
                .orElseThrow(() -> new CampaignNotFoundException(dotGiamGiaId));

        for (Long sanPhamChiTietId : sanPhamChiTietIds) {
            SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietRepository.findById(sanPhamChiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm chi tiết với ID: " + sanPhamChiTietId));
            dotGiamGia.getSanPhamChiTiets().remove(sanPhamChiTiet);
            sanPhamChiTiet.getDotGiamGias().remove(dotGiamGia);
        }
        return dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(dotGiamGia));
    }

    /**
     * Activate a campaign manually
     */
    @Transactional
    public DotGiamGiaDto activateCampaign(Long campaignId) {
        DotGiamGia campaign = dotGiamGiaRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        campaign.activate();
        return dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(campaign));
    }

    /**
     * Deactivate a campaign manually
     */
    @Transactional
    public DotGiamGiaDto deactivateCampaign(Long campaignId) {
        DotGiamGia campaign = dotGiamGiaRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        campaign.deactivate();
        return dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(campaign));
    }

    /**
     * Find campaigns affecting a specific product
     */
    @Transactional(readOnly = true)
    public List<DotGiamGiaDto> findCampaignsForProduct(Long productId) {
        List<DotGiamGia> campaigns = dotGiamGiaRepository.findActiveCampaignsForProduct(productId, TrangThaiCampaign.DA_DIEN_RA);
        return dotGiamGiaMapper.toDtos(campaigns);
    }

    /**
     * Calculate discount percentage for a specific product
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscountForProduct(Long productId) {
        List<DotGiamGia> campaigns = dotGiamGiaRepository.findActiveCampaignsForProduct(productId, TrangThaiCampaign.DA_DIEN_RA);

        // Return the highest discount percentage if multiple campaigns apply
        return campaigns.stream()
                .filter(DotGiamGia::isCurrentlyActive)
                .map(DotGiamGia::getPhanTramGiam)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Find conflicting campaigns (campaigns that target the same products)
     */
    @Transactional(readOnly = true)
    public List<DotGiamGiaDto> findConflictingCampaigns(Long campaignId) {
        DotGiamGia campaign = dotGiamGiaRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        // Find other active campaigns that share products with this campaign
        Set<SanPhamChiTiet> targetProducts = campaign.getSanPhamChiTiets();
        List<DotGiamGia> conflictingCampaigns = dotGiamGiaRepository.findCurrentlyActiveCampaigns()
                .stream()
                .filter(c -> !c.getId().equals(campaignId))
                .filter(c -> c.getSanPhamChiTiets().stream().anyMatch(targetProducts::contains))
                .toList();

        return dotGiamGiaMapper.toDtos(conflictingCampaigns);
    }

    /**
     * Enhanced scheduler with Vietnam timezone support
     * Runs every hour to check for status updates with precise timing
     * Following PhieuGiamGia pattern exactly
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void capNhatTrangThaiDotGiamGia() {
        try {
            // Use Vietnam timezone for business logic
            Instant currentTime = vietnamTimeZoneService.getCurrentVietnamTime().toInstant();

            // Find campaigns needing status updates
            List<DotGiamGia> campaignsToUpdate = dotGiamGiaRepository.findCampaignsNeedingStatusUpdate(
                currentTime,
                TrangThaiCampaign.CHUA_DIEN_RA,
                TrangThaiCampaign.DA_DIEN_RA
            );

            for (DotGiamGia campaign : campaignsToUpdate) {
                TrangThaiCampaign oldStatus = campaign.getTrangThai();
                TrangThaiCampaign newStatus = campaign.calculateStatusInVietnamTime();

                if (oldStatus != newStatus) {
                    campaign.setTrangThai(newStatus);
                    dotGiamGiaRepository.save(campaign);

                    // Send timezone-aware notifications if needed
                    sendStatusChangeNotification(campaign, oldStatus, newStatus);
                }
            }
        } catch (Exception e) {
            // Log error but don't throw to prevent scheduler from stopping
            System.err.println("Error in discount campaign status update scheduler: " + e.getMessage());
        }
    }

    /**
     * Send status change notification with Vietnam timezone formatting
     * Following PhieuGiamGia pattern exactly
     */
    private void sendStatusChangeNotification(DotGiamGia campaign, TrangThaiCampaign oldStatus, TrangThaiCampaign newStatus) {
        String subject = "Cập nhật trạng thái đợt giảm giá";

        // Format dates in Vietnam timezone for notification
        String vietnamStartTime = vietnamTimeZoneService.formatAsVietnamDateTime(campaign.getNgayBatDau());
        String vietnamEndTime = vietnamTimeZoneService.formatAsVietnamDateTime(campaign.getNgayKetThuc());

        String text = String.format(
            "Đợt giảm giá %s đã chuyển từ trạng thái %s sang %s.\n" +
            "Thời gian hiệu lực: %s đến %s (giờ Việt Nam)",
            campaign.getMaDotGiamGia(),
            oldStatus.getDescription(),
            newStatus.getDescription(),
            vietnamStartTime,
            vietnamEndTime
        );

        // Log notification using proper logging framework
        log.info("[CAMPAIGN NOTIFICATION] {}: {}", subject, text);

        // TODO: Implement actual notification sending (email, SMS, etc.)
        // emailService.sendNotification(subject, text);
    }

    /**
     * Get campaign statistics
     */
    @Transactional(readOnly = true)
    public CampaignStatistics getCampaignStatistics() {
        long totalCampaigns = dotGiamGiaRepository.count();
        long activeCampaigns = dotGiamGiaRepository.countByTrangThai(TrangThaiCampaign.DA_DIEN_RA);
        long upcomingCampaigns = dotGiamGiaRepository.countByTrangThai(TrangThaiCampaign.CHUA_DIEN_RA);
        long finishedCampaigns = dotGiamGiaRepository.countByTrangThai(TrangThaiCampaign.KET_THUC);

        return new CampaignStatistics(totalCampaigns, activeCampaigns, upcomingCampaigns, finishedCampaigns);
    }

    /**
     * Update multiple campaign statuses with audit trail
     */
    @Transactional
    public BatchOperationResult updateMultipleCampaignStatus(List<Long> campaignIds,
                                                           com.lapxpert.backend.common.enums.TrangThaiCampaign trangThai,
                                                           String lyDoThayDoi) {
        int successCount = 0;
        int failureCount = 0;

        for (Long campaignId : campaignIds) {
            try {
                dotGiamGiaRepository.findById(campaignId).ifPresentOrElse(
                    campaign -> {
                        // Capture old values for audit
                        String oldValues = buildAuditJson(campaign);

                        // Update status
                        campaign.setTrangThai(trangThai);
                        DotGiamGia savedCampaign = dotGiamGiaRepository.save(campaign);

                        // Create audit trail entry for batch status change
                        String newValues = buildAuditJson(savedCampaign);
                        DotGiamGiaAuditHistory auditEntry = DotGiamGiaAuditHistory.updateEntry(
                            savedCampaign.getId(),
                            oldValues,
                            newValues,
                            savedCampaign.getNguoiCapNhat(),
                            lyDoThayDoi != null ? lyDoThayDoi : "Cập nhật trạng thái hàng loạt"
                        );
                        auditHistoryRepository.save(auditEntry);
                    },
                    () -> {
                        throw new RuntimeException("Chiến dịch không tồn tại với ID: " + campaignId);
                    }
                );
                successCount++;
            } catch (Exception e) {
                failureCount++;
            }
        }

        String message = String.format("Đã cập nhật %d chiến dịch thành công", successCount);
        if (failureCount > 0) {
            message += String.format(", %d chiến dịch thất bại", failureCount);
        }

        return new BatchOperationResult("BATCH_STATUS_UPDATE", successCount, failureCount, message);
    }

    /**
     * Cancel multiple campaigns with audit trail (set status to BI_HUY)
     */
    @Transactional
    public BatchOperationResult cancelMultipleCampaigns(List<Long> campaignIds, String lyDoThayDoi) {
        int successCount = 0;
        int failureCount = 0;

        for (Long campaignId : campaignIds) {
            try {
                dotGiamGiaRepository.findById(campaignId).ifPresentOrElse(
                    campaign -> {
                        // Capture old values for audit
                        String oldValues = buildAuditJson(campaign);

                        // Cancel campaign (soft delete)
                        campaign.deactivate();
                        DotGiamGia savedCampaign = dotGiamGiaRepository.save(campaign);

                        // Create audit trail entry for cancellation
                        DotGiamGiaAuditHistory auditEntry = DotGiamGiaAuditHistory.updateEntry(
                            savedCampaign.getId(),
                            oldValues,
                            buildAuditJson(savedCampaign),
                            savedCampaign.getNguoiCapNhat(),
                            lyDoThayDoi != null ? lyDoThayDoi : "Hủy chiến dịch hàng loạt"
                        );
                        auditHistoryRepository.save(auditEntry);
                    },
                    () -> {
                        throw new RuntimeException("Chiến dịch không tồn tại với ID: " + campaignId);
                    }
                );
                successCount++;
            } catch (Exception e) {
                failureCount++;
            }
        }

        String message = String.format("Đã hủy %d chiến dịch thành công", successCount);
        if (failureCount > 0) {
            message += String.format(", %d chiến dịch thất bại", failureCount);
        }

        return new BatchOperationResult("BATCH_CANCEL", successCount, failureCount, message);
    }

    /**
     * Batch operation result class
     */
    public static class BatchOperationResult {
        private String operation;
        private int successCount;
        private int failureCount;
        private String message;

        public BatchOperationResult(String operation, int successCount, int failureCount, String message) {
            this.operation = operation;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.message = message;
        }

        // Getters and setters
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * Inner class for campaign statistics
     */
    public static class CampaignStatistics {
        private final long totalCampaigns;
        private final long activeCampaigns;
        private final long upcomingCampaigns;
        private final long finishedCampaigns;

        public CampaignStatistics(long totalCampaigns, long activeCampaigns, long upcomingCampaigns, long finishedCampaigns) {
            this.totalCampaigns = totalCampaigns;
            this.activeCampaigns = activeCampaigns;
            this.upcomingCampaigns = upcomingCampaigns;
            this.finishedCampaigns = finishedCampaigns;
        }

        // Getters
        public long getTotalCampaigns() { return totalCampaigns; }
        public long getActiveCampaigns() { return activeCampaigns; }
        public long getUpcomingCampaigns() { return upcomingCampaigns; }
        public long getFinishedCampaigns() { return finishedCampaigns; }
    }

    // ==================== BUSINESSENTITYSERVICE ABSTRACT METHOD IMPLEMENTATIONS ====================

    @Override
    protected JpaRepository<DotGiamGia, Long> getRepository() {
        return dotGiamGiaRepository;
    }

    @Override
    protected JpaRepository<DotGiamGiaAuditHistory, Long> getAuditRepository() {
        return auditHistoryRepository;
    }

    @Override
    protected DotGiamGiaDto toDto(DotGiamGia entity) {
        return dotGiamGiaMapper.toDto(entity);
    }

    @Override
    protected DotGiamGia toEntity(DotGiamGiaDto dto) {
        return dotGiamGiaMapper.toEntity(dto);
    }

    @Override
    protected DotGiamGiaAuditHistory createAuditEntry(Long entityId, String action, String oldValues, String newValues, String nguoiThucHien, String lyDo) {
        switch (action) {
            case "CREATE":
                return DotGiamGiaAuditHistory.createEntry(entityId, newValues, nguoiThucHien, lyDo);
            case "UPDATE":
                return DotGiamGiaAuditHistory.updateEntry(entityId, oldValues, newValues, nguoiThucHien, lyDo);
            case "SOFT_DELETE":
            case "DELETE":
                return DotGiamGiaAuditHistory.deleteEntry(entityId, oldValues, nguoiThucHien, lyDo);
            default:
                return DotGiamGiaAuditHistory.createEntry(entityId, newValues, nguoiThucHien, lyDo);
        }
    }

    @Override
    protected String getEntityName() {
        return "Đợt giảm giá";
    }

    @Override
    protected void validateEntity(DotGiamGia entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Đợt giảm giá không được null");
        }

        // Validate campaign code
        ValidationUtils.validateProductCode(entity.getMaDotGiamGia(), "Mã đợt giảm giá");

        // Validate campaign name
        if (entity.getTenDotGiamGia() == null || entity.getTenDotGiamGia().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đợt giảm giá không được để trống");
        }

        // Validate discount percentage
        ValidationUtils.validatePercentage(entity.getPhanTramGiam(), "Phần trăm giảm");

        // Validate date range
        if (entity.getNgayBatDau() != null && entity.getNgayKetThuc() != null) {
            if (entity.getNgayKetThuc().isBefore(entity.getNgayBatDau())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
            }

            // Campaign must run for at least 1 hour
            if (entity.getNgayKetThuc().isBefore(entity.getNgayBatDau().plusSeconds(3600))) {
                throw new IllegalArgumentException("Đợt giảm giá phải diễn ra ít nhất 1 giờ");
            }
        }
    }

    @Override
    public void evictCache() {
        // Cache eviction is handled by @CacheEvict annotations in BusinessEntityService
        log.debug("Cache evicted for DotGiamGia");
    }

    @Override
    protected Long getEntityId(DotGiamGia entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(DotGiamGia entity, Long id) {
        entity.setId(id);
    }

    @Override
    protected void setSoftDeleteStatus(DotGiamGia entity, boolean status) {
        // DotGiamGia uses TrangThaiCampaign.BI_HUY for soft delete
        if (!status) {
            entity.setTrangThai(TrangThaiCampaign.BI_HUY);
        }
    }

    @Override
    protected List<DotGiamGiaAuditHistory> getAuditHistoryByEntityId(Long entityId) {
        return auditHistoryRepository.findByDotGiamGiaIdOrderByThoiGianThayDoiDesc(entityId);
    }

    @Override
    protected ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    @Override
    protected String getCacheName() {
        return "dotGiamGiaCache";
    }

    @Override
    protected void publishEntityCreatedEvent(DotGiamGia entity) {
        try {
            VoucherChangeEvent event = VoucherChangeEvent.builder()
                    .voucherId(entity.getId())
                    .maVoucher(entity.getMaDotGiamGia())
                    .tenVoucher(entity.getTenDotGiamGia())
                    .loaiVoucher("DOT_GIAM_GIA")
                    .trangThaiCu(null)
                    .trangThaiMoi(entity.getTrangThai() != null ? entity.getTrangThai().name() : null)
                    .giaTriGiamCu(null)
                    .giaTriGiamMoi(entity.getPhanTramGiam())
                    .ngayHetHanCu(null)
                    .ngayHetHanMoi(entity.getNgayKetThuc())
                    .loaiThayDoi("CREATED")
                    .nguoiThucHien(entity.getNguoiTao())
                    .lyDoThayDoi("Tạo đợt giảm giá mới")
                    .timestamp(java.time.Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.info("Published campaign created event for campaign ID: {}", entity.getId());

        } catch (Exception e) {
            log.error("Failed to publish campaign created event for ID {}: {}", entity.getId(), e.getMessage(), e);
        }
    }

    @Override
    protected void publishEntityUpdatedEvent(DotGiamGia entity, DotGiamGia oldEntity) {
        try {
            VoucherChangeEvent event = VoucherChangeEvent.builder()
                    .voucherId(entity.getId())
                    .maVoucher(entity.getMaDotGiamGia())
                    .tenVoucher(entity.getTenDotGiamGia())
                    .loaiVoucher("DOT_GIAM_GIA")
                    .trangThaiCu(oldEntity.getTrangThai() != null ? oldEntity.getTrangThai().name() : null)
                    .trangThaiMoi(entity.getTrangThai() != null ? entity.getTrangThai().name() : null)
                    .giaTriGiamCu(oldEntity.getPhanTramGiam())
                    .giaTriGiamMoi(entity.getPhanTramGiam())
                    .ngayHetHanCu(oldEntity.getNgayKetThuc())
                    .ngayHetHanMoi(entity.getNgayKetThuc())
                    .loaiThayDoi("UPDATED")
                    .nguoiThucHien(entity.getNguoiCapNhat())
                    .lyDoThayDoi("Cập nhật đợt giảm giá")
                    .timestamp(java.time.Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.info("Published campaign updated event for campaign ID: {}", entity.getId());

        } catch (Exception e) {
            log.error("Failed to publish campaign updated event for ID {}: {}", entity.getId(), e.getMessage(), e);
        }
    }

    @Override
    protected void publishEntityDeletedEvent(Long entityId) {
        try {
            VoucherChangeEvent event = VoucherChangeEvent.builder()
                    .voucherId(entityId)
                    .maVoucher("DELETED-" + entityId)
                    .tenVoucher("Đợt giảm giá đã bị xóa")
                    .loaiVoucher("DOT_GIAM_GIA")
                    .trangThaiCu(null)
                    .trangThaiMoi("DELETED")
                    .giaTriGiamCu(null)
                    .giaTriGiamMoi(null)
                    .ngayHetHanCu(null)
                    .ngayHetHanMoi(null)
                    .loaiThayDoi("DELETED")
                    .nguoiThucHien("SYSTEM")
                    .lyDoThayDoi("Xóa đợt giảm giá")
                    .timestamp(java.time.Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.info("Published campaign deleted event for campaign ID: {}", entityId);

        } catch (Exception e) {
            log.error("Failed to publish campaign deleted event for ID {}: {}", entityId, e.getMessage(), e);
        }
    }

    @Override
    protected void validateBusinessRules(DotGiamGia entity) {
        // Validate campaign-specific business rules
        if (entity.getPhanTramGiam().compareTo(BigDecimal.ZERO) <= 0 ||
            entity.getPhanTramGiam().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm phải từ 0.01% đến 100%");
        }

        // Check for duplicate campaign code only for new entities (ID is null)
        if (entity.getId() == null && dotGiamGiaRepository.existsByMaDotGiamGia(entity.getMaDotGiamGia())) {
            throw CampaignConflictException.duplicateCode(entity.getMaDotGiamGia());
        }
    }

    @Override
    protected void validateBusinessRulesForUpdate(DotGiamGia entity, DotGiamGia existingEntity) {
        validateBusinessRules(entity);

        // Additional validation for updates
        if (existingEntity.getTrangThai() == TrangThaiCampaign.KET_THUC) {
            throw new IllegalArgumentException("Không thể cập nhật đợt giảm giá đã kết thúc");
        }

        if (existingEntity.getTrangThai() == TrangThaiCampaign.BI_HUY) {
            throw new IllegalArgumentException("Không thể cập nhật đợt giảm giá đã bị hủy");
        }
    }

    @Override
    protected DotGiamGia cloneEntity(DotGiamGia entity) {
        DotGiamGia clone = new DotGiamGia();
        clone.setId(entity.getId());
        clone.setMaDotGiamGia(entity.getMaDotGiamGia());
        clone.setTenDotGiamGia(entity.getTenDotGiamGia());
        clone.setPhanTramGiam(entity.getPhanTramGiam());
        clone.setNgayBatDau(entity.getNgayBatDau());
        clone.setNgayKetThuc(entity.getNgayKetThuc());
        clone.setTrangThai(entity.getTrangThai());
        return clone;
    }

    /**
     * Build JSON representation of discount campaign for audit trail
     * Following PhieuGiamGia pattern exactly
     * @param entity the discount campaign entity
     * @return JSON string representation
     */
    @Override
    protected String buildAuditJson(DotGiamGia entity) {
        if (entity == null) return "{}";

        try {
            return String.format(
                "{\"maDotGiamGia\":\"%s\",\"tenDotGiamGia\":\"%s\",\"phanTramGiam\":%s," +
                "\"ngayBatDau\":\"%s\",\"ngayKetThuc\":\"%s\",\"trangThai\":\"%s\"}",
                entity.getMaDotGiamGia() != null ? entity.getMaDotGiamGia() : "",
                entity.getTenDotGiamGia() != null ? entity.getTenDotGiamGia() : "",
                entity.getPhanTramGiam() != null ? entity.getPhanTramGiam() : "null",
                entity.getNgayBatDau() != null ? entity.getNgayBatDau().toString() : "",
                entity.getNgayKetThuc() != null ? entity.getNgayKetThuc().toString() : "",
                entity.getTrangThai() != null ? entity.getTrangThai().name() : ""
            );
        } catch (Exception e) {
            System.err.println("Error building audit JSON: " + e.getMessage());
            return "{\"error\":\"Could not serialize discount campaign data\"}";
        }
    }



    /**
     * Get audit history for a specific campaign as DTOs
     */
    @Transactional(readOnly = true)
    public List<DotGiamGiaAuditHistoryDto> getAuditHistoryDtos(Long dotGiamGiaId) {
        // Use inherited getAuditHistory method from BaseService
        List<DotGiamGiaAuditHistory> auditHistory = super.getAuditHistory(dotGiamGiaId);
        return dotGiamGiaMapper.toAuditHistoryDtos(auditHistory);
    }
}
