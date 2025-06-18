package com.lapxpert.backend.dotgiamgia.controller;

import com.lapxpert.backend.dotgiamgia.dto.DotGiamGiaDto;
import com.lapxpert.backend.dotgiamgia.dto.DotGiamGiaAuditHistoryDto;
import com.lapxpert.backend.dotgiamgia.service.DotGiamGiaService;
import com.lapxpert.backend.sanpham.dto.SanPhamChiTietDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/api/v1/discounts")
@CrossOrigin(origins = "*")
@Validated
public class DotGiamGiaController {

    private final DotGiamGiaService service;

    public DotGiamGiaController(DotGiamGiaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DotGiamGiaDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("{id}")
    public ResponseEntity<DotGiamGiaDto> findById(@PathVariable @NotNull Long id) {
        return ResponseEntity.ok(service.getDotGiamGiaById(id));
    }

    @PutMapping
    public ResponseEntity<DotGiamGiaDto> save(@Valid @RequestBody DotGiamGiaDto dto) {
        // Extract audit reason from DTO (following PhieuGiamGia pattern)
        String reason = dto.getLyDoThayDoi() != null && !dto.getLyDoThayDoi().trim().isEmpty()
                ? dto.getLyDoThayDoi().trim()
                : null; // Let service use default reason

        return service.saveWithAudit(dto, reason);
    }

    @PostMapping("toggle/{id}")
    public ResponseEntity<DotGiamGiaDto> toggle(@PathVariable @NotNull Long id) {
        return service.toggle(id);
    }

    @PostMapping("toggles")
    public ResponseEntity<List<DotGiamGiaDto>> toggleMultiple(@RequestBody @NotEmpty List<Long> ids) {
        return service.toggleMultiple(ids);
    }

    @GetMapping("{id}/spct")
    public ResponseEntity<Set<SanPhamChiTietDto>> findAllSanPhamChiTietsByDotGiamGiaId(@PathVariable @NotNull Long id) {
        return ResponseEntity.ok(service.findAllSanPhamChiTietsByDotGiamGiaId(id));
    }

    @PutMapping("{id}/spct")
    public ResponseEntity<DotGiamGiaDto> addSanPhamChiTiets(
            @PathVariable @NotNull Long id,
            @RequestBody @NotEmpty List<Long> sanPhamChiTietIds) {
        return ResponseEntity.ok(service.addSanPhamChiTiets(id, sanPhamChiTietIds));
    }

    @DeleteMapping("{id}/spct")
    public ResponseEntity<DotGiamGiaDto> removeSanPhamChiTiets(
            @PathVariable @NotNull Long id,
            @RequestBody @NotEmpty List<Long> sanPhamChiTietIds) {
        return ResponseEntity.ok(service.removeSanPhamChiTiets(id, sanPhamChiTietIds));
    }

    @PostMapping("{id}/activate")
    public ResponseEntity<DotGiamGiaDto> activateCampaign(@PathVariable @NotNull Long id) {
        try {
            DotGiamGiaDto result = service.activateCampaign(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("{id}/deactivate")
    public ResponseEntity<DotGiamGiaDto> deactivateCampaign(@PathVariable @NotNull Long id) {
        try {
            DotGiamGiaDto result = service.deactivateCampaign(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("product/{productId}")
    public ResponseEntity<List<DotGiamGiaDto>> findCampaignsForProduct(@PathVariable @NotNull Long productId) {
        List<DotGiamGiaDto> campaigns = service.findCampaignsForProduct(productId);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("product/{productId}/discount")
    public ResponseEntity<BigDecimal> calculateDiscountForProduct(@PathVariable @NotNull Long productId) {
        BigDecimal discount = service.calculateDiscountForProduct(productId);
        return ResponseEntity.ok(discount);
    }

    @GetMapping("{id}/conflicts")
    public ResponseEntity<List<DotGiamGiaDto>> findConflictingCampaigns(@PathVariable @NotNull Long id) {
        List<DotGiamGiaDto> conflicts = service.findConflictingCampaigns(id);
        return ResponseEntity.ok(conflicts);
    }

    @GetMapping("statistics")
    public ResponseEntity<DotGiamGiaService.CampaignStatistics> getCampaignStatistics() {
        DotGiamGiaService.CampaignStatistics stats = service.getCampaignStatistics();
        return ResponseEntity.ok(stats);
    }

    // ==================== ADMIN MANAGEMENT ENDPOINTS ====================

    /**
     * Get all campaigns including hidden ones (admin only)
     */
    @GetMapping("admin/all")
    public ResponseEntity<List<DotGiamGiaDto>> findAllIncludingHidden() {
        return ResponseEntity.ok(service.findAllIncludingHidden());
    }

    /**
     * Get only hidden campaigns (admin only)
     */
    @GetMapping("admin/cancelled")
    public ResponseEntity<List<DotGiamGiaDto>> findCancelledCampaigns() {
        return ResponseEntity.ok(service.findCancelledCampaigns());
    }

    /**
     * Restore hidden campaign (set daAn = false)
     */
    @PostMapping("admin/restore/{id}")
    public ResponseEntity<DotGiamGiaDto> restoreCampaign(@PathVariable @NotNull Long id) {
        return ResponseEntity.ok(service.restoreCampaign(id));
    }

    /**
     * Batch restore multiple campaigns
     */
    @PostMapping("admin/restore-batch")
    public ResponseEntity<List<DotGiamGiaDto>> restoreMultipleCampaigns(@RequestBody @NotEmpty List<Long> ids) {
        return ResponseEntity.ok(service.restoreMultipleCampaigns(ids));
    }

    // Batch Operations for Campaign Management
    @PutMapping("batch/status")
    public ResponseEntity<DotGiamGiaService.BatchOperationResult> updateMultipleCampaignStatus(
            @Valid @RequestBody BatchStatusUpdateRequest request) {
        try {
            DotGiamGiaService.BatchOperationResult result = service.updateMultipleCampaignStatus(
                    request.getCampaignIds(),
                    request.getTrangThai(),
                    request.getLyDoThayDoi()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new DotGiamGiaService.BatchOperationResult("BATCH_STATUS_UPDATE", 0, request.getCampaignIds().size(),
                    "Lỗi cập nhật trạng thái: " + e.getMessage())
            );
        }
    }

    @PutMapping("batch/cancel")
    public ResponseEntity<DotGiamGiaService.BatchOperationResult> cancelMultipleCampaigns(
            @Valid @RequestBody BatchCancelRequest request) {
        try {
            DotGiamGiaService.BatchOperationResult result = service.cancelMultipleCampaigns(
                    request.getCampaignIds(),
                    request.getLyDoThayDoi()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new DotGiamGiaService.BatchOperationResult("BATCH_CANCEL", 0, request.getCampaignIds().size(),
                    "Lỗi hủy chiến dịch: " + e.getMessage())
            );
        }
    }

    // Batch Request DTOs
    public static class BatchStatusUpdateRequest {
        @NotEmpty(message = "Danh sách ID chiến dịch không được để trống")
        private List<Long> campaignIds;

        @NotNull(message = "Trạng thái không được để trống")
        private com.lapxpert.backend.common.enums.TrangThaiCampaign trangThai;

        private String lyDoThayDoi;

        // Getters and setters
        public List<Long> getCampaignIds() { return campaignIds; }
        public void setCampaignIds(List<Long> campaignIds) { this.campaignIds = campaignIds; }
        public com.lapxpert.backend.common.enums.TrangThaiCampaign getTrangThai() { return trangThai; }
        public void setTrangThai(com.lapxpert.backend.common.enums.TrangThaiCampaign trangThai) { this.trangThai = trangThai; }
        public String getLyDoThayDoi() { return lyDoThayDoi; }
        public void setLyDoThayDoi(String lyDoThayDoi) { this.lyDoThayDoi = lyDoThayDoi; }
    }

    public static class BatchCancelRequest {
        @NotEmpty(message = "Danh sách ID chiến dịch không được để trống")
        private List<Long> campaignIds;

        private String lyDoThayDoi;

        // Getters and setters
        public List<Long> getCampaignIds() { return campaignIds; }
        public void setCampaignIds(List<Long> campaignIds) { this.campaignIds = campaignIds; }
        public String getLyDoThayDoi() { return lyDoThayDoi; }
        public void setLyDoThayDoi(String lyDoThayDoi) { this.lyDoThayDoi = lyDoThayDoi; }
    }

    @GetMapping("{id}/audit-history")
    public ResponseEntity<List<DotGiamGiaAuditHistoryDto>> getAuditHistory(@PathVariable @NotNull Long id) {
        List<DotGiamGiaAuditHistoryDto> auditHistory = service.getAuditHistoryDtos(id);
        return ResponseEntity.ok(auditHistory);
    }
}
