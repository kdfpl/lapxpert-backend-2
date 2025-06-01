package com.lapxpert.backend.hoadon.domain.service;

import com.lapxpert.backend.hoadon.domain.dto.HoaDonDto;
import com.lapxpert.backend.hoadon.domain.dto.HoaDonChiTietDto;
import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonAuditHistory;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonChiTiet;
import com.lapxpert.backend.hoadon.domain.mapper.HoaDonMapper;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonRepository;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonAuditHistoryRepository;
import com.lapxpert.backend.hoadon.domain.enums.LoaiHoaDon;
import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiThanhToan;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.nguoidung.domain.repository.DiaChiRepository;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import com.lapxpert.backend.sanpham.domain.service.InventoryService;
import com.lapxpert.backend.sanpham.domain.service.PricingService;
import com.lapxpert.backend.phieugiamgia.domain.service.PhieuGiamGiaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HoaDonService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonAuditHistoryRepository auditHistoryRepository;
    private final HoaDonMapper hoaDonMapper;
    private final NguoiDungRepository nguoiDungRepository;
    private final DiaChiRepository diaChiRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final InventoryService inventoryService;
    private final PricingService pricingService;
    private final PhieuGiamGiaService phieuGiamGiaService;
    private final KiemTraTrangThaiHoaDonService kiemTraTrangThaiService;
    private final PaymentMethodValidationService paymentValidationService;

    public HoaDonService(HoaDonRepository hoaDonRepository,
                         HoaDonAuditHistoryRepository auditHistoryRepository,
                         HoaDonMapper hoaDonMapper,
                         NguoiDungRepository nguoiDungRepository,
                         DiaChiRepository diaChiRepository,
                         SanPhamChiTietRepository sanPhamChiTietRepository,
                         InventoryService inventoryService,
                         PricingService pricingService,
                         PhieuGiamGiaService phieuGiamGiaService,
                         KiemTraTrangThaiHoaDonService kiemTraTrangThaiService,
                         PaymentMethodValidationService paymentValidationService) {
        this.hoaDonRepository = hoaDonRepository;
        this.auditHistoryRepository = auditHistoryRepository;
        this.hoaDonMapper = hoaDonMapper;
        this.nguoiDungRepository = nguoiDungRepository;
        this.diaChiRepository = diaChiRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
        this.phieuGiamGiaService = phieuGiamGiaService;
        this.kiemTraTrangThaiService = kiemTraTrangThaiService;
        this.paymentValidationService = paymentValidationService;
    }

    @Transactional(readOnly = true)
    public List<HoaDonDto> getHoaDonsByTrangThai(String trangThaiStr) {
        List<HoaDon> hoaDons;
        if (trangThaiStr == null || trangThaiStr.trim().isEmpty()) {
            hoaDons = hoaDonRepository.findAll();
        } else {
            try {
                TrangThaiDonHang trangThaiEnum = TrangThaiDonHang.valueOf(trangThaiStr.toUpperCase());
                hoaDons = hoaDonRepository.findByTrangThaiDonHang(trangThaiEnum);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái đơn hàng không hợp lệ: " + trangThaiStr, e);
            }
        }
        return hoaDonMapper.toDtoList(hoaDons);
    }

    @Transactional
    public HoaDonDto createHoaDon(HoaDonDto hoaDonDto, NguoiDung currentUser) {
        // Step 1: Validate inventory availability before processing
        if (!inventoryService.isInventoryAvailable(hoaDonDto.getChiTiet())) {
            throw new IllegalArgumentException("Insufficient inventory for one or more items in the order");
        }

        // Step 2: Reserve inventory items with order tracking (this will throw exception if insufficient)
        String orderChannel = hoaDonDto.getLoaiHoaDon() == LoaiHoaDon.TAI_QUAY ? "POS" : "ONLINE";
        String tempOrderId = "TEMP-" + System.currentTimeMillis(); // Temporary ID until order is saved
        List<Long> reservedItemIds = inventoryService.reserveItemsWithTracking(
            hoaDonDto.getChiTiet(),
            orderChannel,
            tempOrderId
        );

        try {
            // Create HoaDon entity manually to avoid mapper issues with nested entities
            HoaDon hoaDon = new HoaDon();
            hoaDon.setMaHoaDon(hoaDonDto.getMaHoaDon());
            hoaDon.setLoaiHoaDon(hoaDonDto.getLoaiHoaDon());

            // Step 3: Set customer (khachHang) - prioritize DTO khachHangId over currentUser
            if (hoaDonDto.getKhachHangId() != null) {
                // Use customer ID from DTO (for POS orders where staff creates order for specific customer)
                NguoiDung customer = nguoiDungRepository.findById(hoaDonDto.getKhachHangId())
                    .orElseThrow(() -> new EntityNotFoundException("Khách hàng không tồn tại với ID: " + hoaDonDto.getKhachHangId()));
                hoaDon.setKhachHang(customer);
            } else if (currentUser != null) {
                // Use current authenticated user as customer (for online orders)
                hoaDon.setKhachHang(currentUser);
            } else {
                if (hoaDonDto.getLoaiHoaDon() == LoaiHoaDon.ONLINE) {
                     throw new IllegalArgumentException("Thông tin khách hàng (khachHang) là bắt buộc cho đơn hàng online.");
                }
            }

            // Step 4: Set employee (nhanVien) if nhanVienId is provided in DTO (typically for POS)
            if (hoaDonDto.getNhanVienId() != null) {
                NguoiDung nhanVien = nguoiDungRepository.findById(hoaDonDto.getNhanVienId())
                    .orElseThrow(() -> new EntityNotFoundException("Nhân viên không tồn tại với ID: " + hoaDonDto.getNhanVienId()));
                hoaDon.setNhanVien(nhanVien);
            }

            // Step 4.5: Validate and set delivery address
            validateAndSetDeliveryAddress(hoaDon, hoaDonDto);

            // Step 4.7: Map order items from DTO to entity
            mapOrderItemsFromDto(hoaDon, hoaDonDto);

            // Step 5: Process order items and calculate totals
            BigDecimal tongTienHang = processOrderItems(hoaDon, hoaDonDto);

            // Step 6: Validate and apply vouchers
            BigDecimal totalVoucherDiscount = processVouchers(hoaDon, hoaDonDto, tongTienHang);

            // Step 7: Set order totals
            hoaDon.setTongTienHang(tongTienHang);
            hoaDon.setPhiVanChuyen(hoaDonDto.getPhiVanChuyen() != null ? hoaDonDto.getPhiVanChuyen() : BigDecimal.ZERO);
            hoaDon.setGiaTriGiamGiaVoucher(totalVoucherDiscount);

            BigDecimal tongCong = tongTienHang.add(hoaDon.getPhiVanChuyen()).subtract(totalVoucherDiscount);
            hoaDon.setTongThanhToan(tongCong.max(BigDecimal.ZERO));

            // Step 7: Set order status
            setOrderStatus(hoaDon, hoaDonDto);

            // Step 8: Save order
            HoaDon savedHoaDon = hoaDonRepository.save(hoaDon);

            // Step 8: Update reserved items with actual order ID
            inventoryService.updateReservationOrderId(reservedItemIds, tempOrderId, savedHoaDon.getId().toString());

            // Step 8.5: Create audit entry for order creation
            String newValues = createAuditValues(savedHoaDon);
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.createEntry(
                savedHoaDon.getId(),
                newValues,
                savedHoaDon.getNguoiTao(),
                "Tạo hóa đơn mới"
            );
            auditHistoryRepository.save(auditEntry);

            // Step 9: For POS orders with immediate payment, confirm the sale
            if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.TAI_QUAY &&
                hoaDon.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                inventoryService.confirmSale(reservedItemIds);
                log.info("POS order {} completed with immediate payment confirmation", savedHoaDon.getId());
            } else {
                log.info("Order {} created with inventory reserved. Payment pending.", savedHoaDon.getId());
            }

            // Step 10: Apply vouchers to the saved order in a separate transaction
            applyVouchersToOrderSeparateTransaction(savedHoaDon.getId(), hoaDonDto, tongTienHang);

            return hoaDonMapper.toDto(savedHoaDon);

        } catch (Exception e) {
            // Step 10: Release reserved inventory if order creation fails
            log.error("Order creation failed, releasing reserved inventory: {}", e.getMessage());
            try {
                // Find items reserved with the temporary order ID and release them
                List<Long> tempReservedItems = inventoryService.getReservedItemsForOrder(tempOrderId);
                if (!tempReservedItems.isEmpty()) {
                    inventoryService.releaseReservationSafely(tempReservedItems);
                } else {
                    // Fallback to the original list if temp order ID tracking fails
                    inventoryService.releaseReservationSafely(reservedItemIds);
                }
            } catch (Exception releaseException) {
                log.error("Failed to release inventory reservations after order creation failure: {}", releaseException.getMessage());
                // Don't throw this exception as it would mask the original error
            }
            throw e;
        }
    }

    /**
     * Map order items from DTO to entity.
     * This creates the HoaDonChiTiet entities from the DTO data.
     */
    private void mapOrderItemsFromDto(HoaDon hoaDon, HoaDonDto hoaDonDto) {
        if (hoaDonDto.getChiTiet() == null || hoaDonDto.getChiTiet().isEmpty()) {
            return;
        }

        List<HoaDonChiTiet> hoaDonChiTiets = new ArrayList<>();
        for (HoaDonChiTietDto chiTietDto : hoaDonDto.getChiTiet()) {
            HoaDonChiTiet chiTiet = new HoaDonChiTiet();
            chiTiet.setHoaDon(hoaDon);
            chiTiet.setSoLuong(chiTietDto.getSoLuong());
            chiTiet.setGiaBan(chiTietDto.getGiaBan()); // Will be recalculated in processOrderItems

            // Set SanPhamChiTiet reference using ID
            SanPhamChiTiet sanPhamChiTiet = new SanPhamChiTiet();
            sanPhamChiTiet.setId(chiTietDto.getSanPhamChiTietId());
            chiTiet.setSanPhamChiTiet(sanPhamChiTiet);

            hoaDonChiTiets.add(chiTiet);
        }
        hoaDon.setHoaDonChiTiets(hoaDonChiTiets);
    }

    /**
     * Process order items and calculate line totals.
     * Items are already reserved by InventoryService.
     */
    private BigDecimal processOrderItems(HoaDon hoaDon, HoaDonDto hoaDonDto) {
        BigDecimal tongTienHang = BigDecimal.ZERO;
        List<HoaDonChiTiet> processedChiTietList = new ArrayList<>();

        if (hoaDon.getHoaDonChiTiets() != null && !hoaDon.getHoaDonChiTiets().isEmpty()) {
            for (HoaDonChiTiet mappedChiTiet : hoaDon.getHoaDonChiTiets()) {
                if (mappedChiTiet.getSanPhamChiTiet() == null || mappedChiTiet.getSanPhamChiTiet().getId() == null) {
                    throw new IllegalArgumentException("Thông tin sản phẩm chi tiết không hợp lệ trong chi tiết hóa đơn.");
                }
                Long sanPhamChiTietId = mappedChiTiet.getSanPhamChiTiet().getId();
                SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietRepository.findById(sanPhamChiTietId)
                    .orElseThrow(() -> new EntityNotFoundException("Sản phẩm chi tiết không tồn tại với ID: " + sanPhamChiTietId));

                mappedChiTiet.setSanPhamChiTiet(sanPhamChiTiet);
                mappedChiTiet.setHoaDon(hoaDon);

                // Calculate dynamic selling price using PricingService
                BigDecimal sellingPrice = pricingService.calculateEffectivePrice(sanPhamChiTiet);
                mappedChiTiet.setGiaBan(sellingPrice);
                mappedChiTiet.setGiaGoc(sanPhamChiTiet.getGiaBan());

                BigDecimal lineTotal = sellingPrice.multiply(BigDecimal.valueOf(mappedChiTiet.getSoLuong()));
                mappedChiTiet.setThanhTien(lineTotal);

                // Set snapshots for audit trail
                if (sanPhamChiTiet.getSanPham() != null) {
                    mappedChiTiet.setTenSanPhamSnapshot(sanPhamChiTiet.getSanPham().getTenSanPham());
                }
                mappedChiTiet.setSkuSnapshot(sanPhamChiTiet.getSerialNumber());
                if (sanPhamChiTiet.getHinhAnh() != null && !sanPhamChiTiet.getHinhAnh().isEmpty()) {
                    mappedChiTiet.setHinhAnhSnapshot(sanPhamChiTiet.getHinhAnh().get(0));
                }

                tongTienHang = tongTienHang.add(lineTotal);
                processedChiTietList.add(mappedChiTiet);
            }
        }
        hoaDon.setHoaDonChiTiets(processedChiTietList);
        return tongTienHang;
    }

    /**
     * Process and validate vouchers for the order.
     * Returns the total discount amount from all valid vouchers.
     */
    private BigDecimal processVouchers(HoaDon hoaDon, HoaDonDto hoaDonDto, BigDecimal orderTotal) {
        if (hoaDonDto.getVoucherCodes() == null || hoaDonDto.getVoucherCodes().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;
        NguoiDung customer = hoaDon.getKhachHang();

        if (customer == null) {
            log.warn("Cannot apply vouchers to order without customer");
            return BigDecimal.ZERO;
        }

        for (String voucherCode : hoaDonDto.getVoucherCodes()) {
            try {
                // Validate voucher
                PhieuGiamGiaService.VoucherValidationResult validationResult =
                    phieuGiamGiaService.validateVoucher(voucherCode, customer, orderTotal);

                if (validationResult.isValid()) {
                    // Apply voucher to order (this will be done after order is saved)
                    totalDiscount = totalDiscount.add(validationResult.getDiscountAmount());
                    log.info("Voucher {} validated successfully for order. Discount: {}",
                            voucherCode, validationResult.getDiscountAmount());
                } else {
                    log.warn("Voucher {} validation failed: {}", voucherCode, validationResult.getErrorMessage());
                    throw new IllegalArgumentException("Voucher validation failed for " + voucherCode + ": " + validationResult.getErrorMessage());
                }
            } catch (Exception e) {
                log.error("Error processing voucher {}: {}", voucherCode, e.getMessage());
                throw new IllegalArgumentException("Error processing voucher " + voucherCode + ": " + e.getMessage());
            }
        }

        return totalDiscount;
    }

    /**
     * Apply vouchers to order in a separate transaction to avoid transient entity issues.
     * Uses ID-based approach to completely avoid entity reference issues.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void applyVouchersToOrderSeparateTransaction(Long orderId, HoaDonDto hoaDonDto, BigDecimal orderTotal) {
        if (hoaDonDto.getVoucherCodes() == null || hoaDonDto.getVoucherCodes().isEmpty()) {
            return;
        }

        // Get customer ID directly without fetching full entities
        Long customerId = hoaDonRepository.findCustomerIdByOrderId(orderId);
        if (customerId == null) {
            log.warn("No customer found for order {}, skipping voucher application", orderId);
            return;
        }

        // Fetch customer separately to avoid transient entity issues
        NguoiDung customer = nguoiDungRepository.findById(customerId)
            .orElse(null);
        if (customer == null) {
            log.warn("Customer {} not found, skipping voucher application", customerId);
            return;
        }

        for (String voucherCode : hoaDonDto.getVoucherCodes()) {
            try {
                // Re-validate voucher using customer entity
                PhieuGiamGiaService.VoucherValidationResult validationResult =
                    phieuGiamGiaService.validateVoucher(voucherCode, customer, orderTotal);

                if (validationResult.isValid()) {
                    // Apply voucher using ID-based approach to avoid entity references
                    phieuGiamGiaService.applyVoucherToOrderById(
                        validationResult.getVoucher().getId(),
                        orderId,
                        validationResult.getDiscountAmount()
                    );
                    log.info("Applied voucher {} to order {} with discount {}",
                            voucherCode, orderId, validationResult.getDiscountAmount());
                }
            } catch (Exception e) {
                log.error("Failed to apply voucher {} to order {}: {}", voucherCode, orderId, e.getMessage());
                // Note: At this point the order is already saved, so we log the error but don't fail the order
            }
        }
    }

    /**
     * Set order status based on order type and payment method.
     * Simplified logic for TIEN_MAT and COD payment methods.
     */
    private void setOrderStatus(HoaDon hoaDon, HoaDonDto hoaDonDto) {
        // Ensure loaiHoaDon is set, defaulting to ONLINE if not specified
        if (hoaDon.getLoaiHoaDon() == null) {
            hoaDon.setLoaiHoaDon(LoaiHoaDon.ONLINE);
        }

        // Validate payment method based on order type
        validatePaymentMethodForOrderType(hoaDon, hoaDonDto);

        // Set default status fields if not provided by DTO
        if (hoaDon.getTrangThaiDonHang() == null) {
            if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.TAI_QUAY) {
                // POS orders default to pending confirmation (payment method will determine final status)
                hoaDon.setTrangThaiDonHang(TrangThaiDonHang.CHO_XAC_NHAN);
                hoaDon.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
            } else {
                // Online orders start as pending confirmation
                hoaDon.setTrangThaiDonHang(TrangThaiDonHang.CHO_XAC_NHAN);
                // Payment status depends on payment method (will be updated during payment confirmation)
                hoaDon.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
            }
        }

        if (hoaDon.getTrangThaiThanhToan() == null) {
            hoaDon.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
        }
    }

    /**
     * Validate payment method is appropriate for order type.
     * Updated to support flexible payment scenarios.
     */
    private void validatePaymentMethodForOrderType(HoaDon hoaDon, HoaDonDto hoaDonDto) {
        // For now, we'll infer payment method from order type since it's not in the DTO
        // This can be enhanced later when payment method is added to the DTO

        if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.TAI_QUAY) {
            // POS orders can use TIEN_MAT, COD (for delivery), or VNPAY
            log.debug("POS order supports TIEN_MAT, COD, or VNPAY payment methods");
        } else if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.ONLINE) {
            // Online orders can use COD or VNPAY (TIEN_MAT requires physical presence)
            log.debug("Online order supports COD or VNPAY payment methods");
        }
    }

    @Transactional(readOnly = true)
    public HoaDonDto getHoaDonById(Long id) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hóa đơn không tồn tại với ID: " + id));
        return hoaDonMapper.toDto(hoaDon);
    }

    /**
     * Get order by ID with security check.
     * Users can only access their own orders, admins can access any order.
     */
    @Transactional(readOnly = true)
    public HoaDonDto getHoaDonByIdSecure(Long id, NguoiDung currentUser) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hóa đơn không tồn tại với ID: " + id));

        // Security check: user can only access their own orders
        if (!isOrderAccessible(hoaDon, currentUser)) {
            throw new SecurityException("Bạn không có quyền truy cập hóa đơn này");
        }

        return hoaDonMapper.toDto(hoaDon);
    }

    /**
     * Check if a user can access a specific order.
     * Users can access their own orders, admins can access any order.
     */
    public boolean isOrderAccessible(HoaDon hoaDon, NguoiDung currentUser) {
        if (currentUser == null) {
            return false;
        }

        // Admin can access any order
        if (isAdmin(currentUser)) {
            return true;
        }

        // Staff can access orders they are assigned to or any order (depending on business rules)
        if (isStaff(currentUser)) {
            return true; // For now, staff can access all orders
        }

        // Customer can only access their own orders
        return hoaDon.getKhachHang() != null &&
               hoaDon.getKhachHang().getId().equals(currentUser.getId());
    }

    /**
     * Check if user is an admin.
     */
    private boolean isAdmin(NguoiDung user) {
        return user.getVaiTro() != null &&
               (user.getVaiTro().name().equals("ADMIN") || user.getVaiTro().name().equals("MANAGER"));
    }

    /**
     * Check if user is staff.
     */
    private boolean isStaff(NguoiDung user) {
        return user.getVaiTro() != null &&
               user.getVaiTro().name().equals("STAFF");
    }

    @Transactional
    public HoaDonDto updateHoaDon(Long id, HoaDonDto hoaDonDto, NguoiDung currentUser) {
        HoaDon existingHoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hóa đơn để cập nhật với ID: " + id));

        // Security check: user can only update their own orders or admin can update any
        if (!isOrderAccessible(existingHoaDon, currentUser)) {
            throw new SecurityException("Bạn không có quyền cập nhật hóa đơn này");
        }

        // Prevent certain status updates if the order is in a final state (e.g., completed, cancelled)
        // This logic needs to be very specific based on business rules.
        // Example: if (existingHoaDon.getTrangThaiDonHang() == TrangThaiDonHang.HOAN_THANH || existingHoaDon.getTrangThaiDonHang() == TrangThaiDonHang.DA_HUY) {
        //     throw new IllegalStateException("Không thể cập nhật hóa đơn đã hoàn thành hoặc đã hủy.");
        // }

        // Map basic fields from DTO, but preserve critical existing data
        // Note: MapStruct might be too aggressive here if not configured carefully for updates.
        // A more controlled approach might be to manually set fields.

        // Update delivery address if provided
        if (hoaDonDto.getDiaChiGiaoHangId() != null || hoaDonDto.getDiaChiGiaoHang() != null) {
            validateAndSetDeliveryAddress(existingHoaDon, hoaDonDto);
        }

        // Update delivery contact information if provided
        if (hoaDonDto.getNguoiNhanTen() != null || hoaDonDto.getNguoiNhanSdt() != null) {
            setDeliveryContactInfo(existingHoaDon, hoaDonDto);
        }

        existingHoaDon.setPhiVanChuyen(hoaDonDto.getPhiVanChuyen() != null ? hoaDonDto.getPhiVanChuyen() : existingHoaDon.getPhiVanChuyen());
        existingHoaDon.setGiaTriGiamGiaVoucher(hoaDonDto.getGiaTriGiamGiaVoucher() != null ? hoaDonDto.getGiaTriGiamGiaVoucher() : existingHoaDon.getGiaTriGiamGiaVoucher());

        // Update NhanVien if ID is provided in DTO and different from existing
        if (hoaDonDto.getNhanVienId() != null) {
            if (existingHoaDon.getNhanVien() == null || !hoaDonDto.getNhanVienId().equals(existingHoaDon.getNhanVien().getId())) {
                NguoiDung nhanVien = nguoiDungRepository.findById(hoaDonDto.getNhanVienId())
                    .orElseThrow(() -> new EntityNotFoundException("Nhân viên không tồn tại với ID: " + hoaDonDto.getNhanVienId()));
                existingHoaDon.setNhanVien(nhanVien);
            }
        } else {
            existingHoaDon.setNhanVien(null); // Allow unsetting the staff if DTO provides null ID
        }

        // Status updates - should be handled carefully, possibly in a separate method
        // For now, allow direct update from DTO if provided
        if (hoaDonDto.getTrangThaiDonHang() != null) {
            existingHoaDon.setTrangThaiDonHang(hoaDonDto.getTrangThaiDonHang());
        }
        if (hoaDonDto.getTrangThaiThanhToan() != null) {
            existingHoaDon.setTrangThaiThanhToan(hoaDonDto.getTrangThaiThanhToan());
        }
        if (hoaDonDto.getLoaiHoaDon() != null) {
            existingHoaDon.setLoaiHoaDon(hoaDonDto.getLoaiHoaDon());
        }

        // Handle updates to HoaDonChiTiet (add, remove, update quantity)
        if (hoaDonDto.getChiTiet() != null && !hoaDonDto.getChiTiet().isEmpty()) {
            updateOrderLineItems(existingHoaDon, hoaDonDto);
        }

        // Recalculate totals after line item updates
        recalculateOrderTotals(existingHoaDon);

        HoaDon savedHoaDon = hoaDonRepository.save(existingHoaDon);
        return hoaDonMapper.toDto(savedHoaDon);
    }

    @Transactional(readOnly = true)
    public List<HoaDonDto> getAllHoaDons() {
        List<HoaDon> hoaDons = hoaDonRepository.findAll();
        return hoaDonMapper.toDtoList(hoaDons);
    }

    @Transactional(readOnly = true)
    public List<HoaDonDto> findByNguoiDungEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return List.of();
        }
        List<HoaDon> hoaDons = hoaDonRepository.findByKhachHang_Email(email);
        return hoaDonMapper.toDtoList(hoaDons);
    }

    /**
     * Cancel an order and release reserved inventory.
     * This method should be called when an order is cancelled before payment.
     */
    @Transactional
    public HoaDonDto cancelOrder(Long orderId, String reason) {
        HoaDon hoaDon = hoaDonRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        return cancelOrderInternal(hoaDon, reason);
    }

    /**
     * Cancel an order with security check.
     */
    @Transactional
    public HoaDonDto cancelOrderSecure(Long orderId, String reason, NguoiDung currentUser) {
        HoaDon hoaDon = hoaDonRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Security check: user can only cancel their own orders or admin can cancel any
        if (!isOrderAccessible(hoaDon, currentUser)) {
            throw new SecurityException("Bạn không có quyền hủy hóa đơn này");
        }

        return cancelOrderInternal(hoaDon, reason);
    }

    /**
     * Internal method to handle order cancellation logic.
     */
    private HoaDonDto cancelOrderInternal(HoaDon hoaDon, String reason) {

        // Only allow cancellation of pending orders
        if (hoaDon.getTrangThaiDonHang() != TrangThaiDonHang.CHO_XAC_NHAN &&
            hoaDon.getTrangThaiDonHang() != TrangThaiDonHang.DA_XAC_NHAN) {
            throw new IllegalStateException("Cannot cancel order in status: " + hoaDon.getTrangThaiDonHang());
        }

        // Release inventory for all items in the order
        // Find items that are actually reserved for this order
        List<Long> itemIdsToRelease = inventoryService.getReservedItemsForOrder(hoaDon.getId().toString());

        if (!itemIdsToRelease.isEmpty()) {
            // Use safe release method to avoid exceptions for items that aren't actually reserved
            inventoryService.releaseReservationSafely(itemIdsToRelease);
            log.info("Released {} reserved items for cancelled order {}", itemIdsToRelease.size(), hoaDon.getId());
        } else {
            log.info("No reserved items found to release for cancelled order {}", hoaDon.getId());
        }

        // Remove vouchers and decrement usage counts
        phieuGiamGiaService.removeVouchersFromOrder(hoaDon.getId());

        // Store old values for audit
        String oldValues = createAuditValues(hoaDon);

        // Update order status
        hoaDon.setTrangThaiDonHang(TrangThaiDonHang.DA_HUY);
        HoaDon savedHoaDon = hoaDonRepository.save(hoaDon);

        // Create audit entry for cancellation
        HoaDonAuditHistory auditEntry = HoaDonAuditHistory.cancelEntry(
            savedHoaDon.getId(),
            oldValues,
            savedHoaDon.getNguoiCapNhat(),
            reason != null ? reason : "Hủy hóa đơn"
        );
        auditHistoryRepository.save(auditEntry);

        log.info("Order {} cancelled. Reason: {}", hoaDon.getId(), reason);
        return hoaDonMapper.toDto(savedHoaDon);
    }

    /**
     * Confirm payment for an order and finalize the sale.
     * Supports flexible payment methods: TIEN_MAT, COD, and VNPAY.
     */
    @Transactional
    public HoaDonDto confirmPayment(Long orderId, PhuongThucThanhToan phuongThucThanhToan) {
        HoaDon hoaDon = hoaDonRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        return confirmPaymentInternal(hoaDon, phuongThucThanhToan);
    }

    /**
     * Confirm payment with security check.
     */
    @Transactional
    public HoaDonDto confirmPaymentSecure(Long orderId, PhuongThucThanhToan phuongThucThanhToan, NguoiDung currentUser) {
        HoaDon hoaDon = hoaDonRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Security check: user can only confirm payment for their own orders or admin can confirm any
        if (!isOrderAccessible(hoaDon, currentUser)) {
            throw new SecurityException("Bạn không có quyền xác nhận thanh toán cho hóa đơn này");
        }

        return confirmPaymentInternal(hoaDon, phuongThucThanhToan);
    }

    /**
     * Update payment status with security check.
     */
    @Transactional
    public HoaDonDto updatePaymentStatusSecure(Long orderId, TrangThaiThanhToan trangThaiThanhToan, String ghiChu, NguoiDung currentUser) {
        HoaDon hoaDon = hoaDonRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Security check: user can only update payment status for their own orders or admin can update any
        if (!isOrderAccessible(hoaDon, currentUser)) {
            throw new SecurityException("Bạn không có quyền cập nhật trạng thái thanh toán cho hóa đơn này");
        }

        return updatePaymentStatusInternal(hoaDon, trangThaiThanhToan, ghiChu, currentUser);
    }

    /**
     * Internal method to handle payment status update logic.
     */
    private HoaDonDto updatePaymentStatusInternal(HoaDon hoaDon, TrangThaiThanhToan trangThaiThanhToan, String ghiChu, NguoiDung currentUser) {
        // Store old payment status for audit
        TrangThaiThanhToan oldPaymentStatus = hoaDon.getTrangThaiThanhToan();

        // Validate payment status transition
        validatePaymentStatusTransition(oldPaymentStatus, trangThaiThanhToan);

        // Update payment status
        hoaDon.setTrangThaiThanhToan(trangThaiThanhToan);

        // Handle specific payment status changes
        handlePaymentStatusChange(hoaDon, oldPaymentStatus, trangThaiThanhToan);

        HoaDon savedHoaDon = hoaDonRepository.save(hoaDon);

        // Create audit entry for payment status change
        HoaDonAuditHistory auditEntry = HoaDonAuditHistory.paymentStatusChangeEntry(
            savedHoaDon.getId(),
            oldPaymentStatus.name(),
            trangThaiThanhToan.name(),
            currentUser != null ? currentUser.getEmail() : "SYSTEM",
            ghiChu != null ? ghiChu : "Cập nhật trạng thái thanh toán"
        );
        auditHistoryRepository.save(auditEntry);

        log.info("Payment status updated for order {} from {} to {} by user {}",
                hoaDon.getId(), oldPaymentStatus, trangThaiThanhToan,
                currentUser != null ? currentUser.getEmail() : "SYSTEM");

        return hoaDonMapper.toDto(savedHoaDon);
    }

    /**
     * Validate payment status transition.
     */
    private void validatePaymentStatusTransition(TrangThaiThanhToan fromStatus, TrangThaiThanhToan toStatus) {
        // Define valid transitions
        switch (fromStatus) {
            case CHUA_THANH_TOAN:
                if (toStatus != TrangThaiThanhToan.DA_THANH_TOAN &&
                    toStatus != TrangThaiThanhToan.THANH_TOAN_LOI) {
                    throw new IllegalArgumentException("Invalid payment status transition from " + fromStatus + " to " + toStatus);
                }
                break;
            case DA_THANH_TOAN:
                if (toStatus != TrangThaiThanhToan.CHO_XU_LY_HOAN_TIEN &&
                    toStatus != TrangThaiThanhToan.DA_HOAN_TIEN) {
                    throw new IllegalArgumentException("Invalid payment status transition from " + fromStatus + " to " + toStatus);
                }
                break;
            case THANH_TOAN_LOI:
                if (toStatus != TrangThaiThanhToan.CHUA_THANH_TOAN &&
                    toStatus != TrangThaiThanhToan.DA_THANH_TOAN) {
                    throw new IllegalArgumentException("Invalid payment status transition from " + fromStatus + " to " + toStatus);
                }
                break;
            case CHO_XU_LY_HOAN_TIEN:
                if (toStatus != TrangThaiThanhToan.DA_HOAN_TIEN &&
                    toStatus != TrangThaiThanhToan.DA_THANH_TOAN) {
                    throw new IllegalArgumentException("Invalid payment status transition from " + fromStatus + " to " + toStatus);
                }
                break;
            case DA_HOAN_TIEN:
                // Generally, refunded orders shouldn't change status
                throw new IllegalArgumentException("Cannot change payment status from refunded state");
        }
    }

    /**
     * Handle specific payment status changes.
     */
    private void handlePaymentStatusChange(HoaDon hoaDon, TrangThaiThanhToan oldStatus, TrangThaiThanhToan newStatus) {
        // Handle inventory implications
        if (oldStatus == TrangThaiThanhToan.CHUA_THANH_TOAN && newStatus == TrangThaiThanhToan.DA_THANH_TOAN) {
            // Payment confirmed - finalize inventory sale
            confirmInventorySale(hoaDon);
        } else if (newStatus == TrangThaiThanhToan.DA_HOAN_TIEN) {
            // Refund processed - release inventory back to available
            releaseInventoryForRefund(hoaDon);
        }
    }

    /**
     * Release inventory back to available when refund is processed.
     */
    private void releaseInventoryForRefund(HoaDon hoaDon) {
        List<Long> itemIdsToRelease = new ArrayList<>();

        for (HoaDonChiTiet chiTiet : hoaDon.getHoaDonChiTiets()) {
            // Get sold items for this product variant
            List<SanPhamChiTiet> soldItems = inventoryService.getSoldItems(
                chiTiet.getSanPhamChiTiet().getId(), chiTiet.getSoLuong());

            for (SanPhamChiTiet item : soldItems) {
                itemIdsToRelease.add(item.getId());
            }
        }

        if (!itemIdsToRelease.isEmpty()) {
            inventoryService.releaseFromSold(itemIdsToRelease);
            log.info("Released {} items back to inventory for refunded order {}",
                    itemIdsToRelease.size(), hoaDon.getId());
        }
    }

    /**
     * Internal method to handle payment confirmation logic.
     */
    private HoaDonDto confirmPaymentInternal(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan) {

        // Enhanced payment method validation
        PaymentMethodValidationService.ValidationResult validationResult =
            paymentValidationService.validatePaymentConfirmation(hoaDon, phuongThucThanhToan);

        if (!validationResult.isValid()) {
            String errorMessage = String.join("; ", validationResult.getErrors());
            throw new IllegalArgumentException("Payment validation failed: " + errorMessage);
        }

        // Log warnings if any
        if (validationResult.hasWarnings()) {
            log.warn("Payment confirmation warnings for order {}: {}",
                    hoaDon.getId(), String.join("; ", validationResult.getWarnings()));
        }

        // Validate payment method matches order type (legacy validation)
        validatePaymentMethodForConfirmation(hoaDon, phuongThucThanhToan);

        // Update payment status
        hoaDon.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);

        // Update order status based on payment method and order type
        if (phuongThucThanhToan == PhuongThucThanhToan.TIEN_MAT) {
            // Cash payments complete immediately (POS only)
            hoaDon.setTrangThaiDonHang(TrangThaiDonHang.HOAN_THANH);
        } else if (phuongThucThanhToan == PhuongThucThanhToan.COD) {
            // COD payments happen at delivery, so order moves to delivered status
            hoaDon.setTrangThaiDonHang(TrangThaiDonHang.DA_GIAO_HANG);
        } else if (phuongThucThanhToan == PhuongThucThanhToan.VNPAY) {
            // VNPAY payments are processed immediately, move to processing for fulfillment
            if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.TAI_QUAY) {
                // POS orders with VNPAY can complete immediately if no delivery needed
                hoaDon.setTrangThaiDonHang(TrangThaiDonHang.HOAN_THANH);
            } else {
                // Online orders with VNPAY move to processing for shipping
                hoaDon.setTrangThaiDonHang(TrangThaiDonHang.DANG_XU_LY);
            }
        }

        HoaDon savedHoaDon = hoaDonRepository.save(hoaDon);

        // Confirm the sale in inventory (items are already reserved)
        confirmInventorySale(savedHoaDon);

        log.info("Payment confirmed for order {} using {}. Sale finalized.",
                hoaDon.getId(), phuongThucThanhToan);

        return hoaDonMapper.toDto(savedHoaDon);
    }

    /**
     * Validate that payment method is appropriate for the order type.
     * Updated to support flexible payment scenarios based on business requirements.
     */
    private void validatePaymentMethodForConfirmation(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan) {
        // TIEN_MAT requires physical presence - only allowed for POS orders
        if (phuongThucThanhToan == PhuongThucThanhToan.TIEN_MAT && hoaDon.getLoaiHoaDon() == LoaiHoaDon.ONLINE) {
            throw new IllegalArgumentException("Cash payment (TIEN_MAT) requires physical presence and is not available for online orders");
        }

        // COD and VNPAY are flexible and can be used with both order types
        if (phuongThucThanhToan == PhuongThucThanhToan.COD) {
            log.debug("COD payment accepted for {} order", hoaDon.getLoaiHoaDon());
        }

        if (phuongThucThanhToan == PhuongThucThanhToan.VNPAY) {
            log.debug("VNPAY payment accepted for {} order", hoaDon.getLoaiHoaDon());
        }
    }

    /**
     * Confirm inventory sale for the order.
     * For simplified implementation, we'll use the existing available items approach.
     */
    private void confirmInventorySale(HoaDon hoaDon) {
        List<Long> itemIdsToConfirm = new ArrayList<>();

        for (HoaDonChiTiet chiTiet : hoaDon.getHoaDonChiTiets()) {
            // In a more sophisticated system, we'd track which specific items were reserved
            // For now, we'll get available items and confirm the sale
            List<SanPhamChiTiet> availableItems = inventoryService.getAvailableItems(
                chiTiet.getSanPhamChiTiet().getId());

            // Take the first N items that match the quantity ordered
            int itemsToConfirm = Math.min(chiTiet.getSoLuong(), availableItems.size());
            for (int i = 0; i < itemsToConfirm; i++) {
                itemIdsToConfirm.add(availableItems.get(i).getId());
            }
        }

        if (!itemIdsToConfirm.isEmpty()) {
            inventoryService.confirmSale(itemIdsToConfirm);
            log.info("Confirmed sale of {} items for order {}", itemIdsToConfirm.size(), hoaDon.getId());
        }
    }

    /**
     * Cập nhật trạng thái hóa đơn với kiểm tra và audit trail.
     * Phương thức này thực thi quy tắc kinh doanh và tạo lịch sử audit.
     *
     * @param hoaDonId ID hóa đơn cần cập nhật
     * @param trangThaiMoi Trạng thái mới cần đặt
     * @param lyDo Lý do thay đổi trạng thái
     * @param nguoiDungHienTai Người dùng thực hiện thay đổi
     * @param diaChiIp Địa chỉ IP của người dùng
     * @return DTO hóa đơn đã cập nhật
     */
    @Transactional
    public HoaDonDto capNhatTrangThaiHoaDon(Long hoaDonId, TrangThaiDonHang trangThaiMoi, String lyDo,
                                          NguoiDung nguoiDungHienTai, String diaChiIp) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hóa đơn với ID: " + hoaDonId));

        TrangThaiDonHang trangThaiHienTai = hoaDon.getTrangThaiDonHang();

        // Kiểm tra việc chuyển đổi trạng thái
        KiemTraTrangThaiHoaDonService.KetQuaKiemTra ketQuaKiemTra =
            kiemTraTrangThaiService.kiemTraChuyenDoi(trangThaiHienTai, trangThaiMoi, nguoiDungHienTai, false);

        if (!ketQuaKiemTra.isHopLe()) {
            throw new IllegalStateException("Chuyển đổi trạng thái không được phép: " + ketQuaKiemTra.getThongBao());
        }

        // Kiểm tra xem có yêu cầu lý do cho chuyển đổi này không
        if (ketQuaKiemTra.yeuCauLyDo() && (lyDo == null || lyDo.trim().isEmpty())) {
            throw new IllegalArgumentException("Lý do là bắt buộc cho việc chuyển đổi trạng thái này");
        }

        // Create audit history entry for status change
        HoaDonAuditHistory auditEntry = HoaDonAuditHistory.statusChangeEntry(
            hoaDonId,
            trangThaiHienTai.name(),
            trangThaiMoi.name(),
            nguoiDungHienTai != null ? nguoiDungHienTai.getEmail() : "SYSTEM",
            lyDo
        );
        auditHistoryRepository.save(auditEntry);

        // Tải lại hóa đơn đã cập nhật
        HoaDon hoaDonDaCapNhat = hoaDonRepository.findById(hoaDonId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hóa đơn sau khi cập nhật: " + hoaDonId));

        log.info("Trạng thái hóa đơn {} đã được cập nhật từ {} thành {} bởi người dùng {}",
                hoaDonId, trangThaiHienTai, trangThaiMoi,
                nguoiDungHienTai != null ? nguoiDungHienTai.getEmail() : "SYSTEM");

        return hoaDonMapper.toDto(hoaDonDaCapNhat);
    }

    /**
     * Lấy các chuyển đổi trạng thái được phép cho một hóa đơn.
     *
     * @param hoaDonId ID hóa đơn
     * @param nguoiDungHienTai Người dùng yêu cầu các chuyển đổi
     * @return Danh sách các chuyển đổi được phép
     */
    @Transactional(readOnly = true)
    public List<TrangThaiDonHang> layCacChuyenDoiTrangThaiChoPhep(Long hoaDonId, NguoiDung nguoiDungHienTai) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hóa đơn với ID: " + hoaDonId));

        return kiemTraTrangThaiService.layCacChuyenDoiChoPhep(hoaDon.getTrangThaiDonHang(), nguoiDungHienTai)
            .stream()
            .map(chuyenDoi -> chuyenDoi.getTrangThaiDen())
            .toList();
    }

    /**
     * Validate and set delivery address for the order.
     * Supports both existing address ID and new address creation.
     */
    private void validateAndSetDeliveryAddress(HoaDon hoaDon, HoaDonDto hoaDonDto) {
        // If diaChiGiaoHangId is provided, use existing address
        if (hoaDonDto.getDiaChiGiaoHangId() != null) {
            DiaChi diaChi = diaChiRepository.findById(hoaDonDto.getDiaChiGiaoHangId())
                .orElseThrow(() -> new EntityNotFoundException("Địa chỉ giao hàng không tồn tại với ID: " + hoaDonDto.getDiaChiGiaoHangId()));

            // Validate that the address belongs to the customer
            if (hoaDon.getKhachHang() != null && !diaChi.getNguoiDung().getId().equals(hoaDon.getKhachHang().getId())) {
                throw new IllegalArgumentException("Địa chỉ giao hàng không thuộc về khách hàng này");
            }

            hoaDon.setDiaChiGiaoHang(diaChi);
        }
        // If diaChiGiaoHang object is provided, create new address or use existing
        else if (hoaDonDto.getDiaChiGiaoHang() != null) {
            DiaChi diaChi;

            if (hoaDonDto.getDiaChiGiaoHang().getId() != null) {
                // Use existing address
                diaChi = diaChiRepository.findById(hoaDonDto.getDiaChiGiaoHang().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Địa chỉ giao hàng không tồn tại với ID: " + hoaDonDto.getDiaChiGiaoHang().getId()));

                // Validate ownership
                if (hoaDon.getKhachHang() != null && !diaChi.getNguoiDung().getId().equals(hoaDon.getKhachHang().getId())) {
                    throw new IllegalArgumentException("Địa chỉ giao hàng không thuộc về khách hàng này");
                }
            } else {
                // Create new address for the customer
                if (hoaDon.getKhachHang() == null) {
                    throw new IllegalArgumentException("Không thể tạo địa chỉ mới khi không có thông tin khách hàng");
                }

                diaChi = DiaChi.builder()
                    .nguoiDung(hoaDon.getKhachHang())
                    .duong(hoaDonDto.getDiaChiGiaoHang().getDuong())
                    .phuongXa(hoaDonDto.getDiaChiGiaoHang().getPhuongXa())
                    .quanHuyen(hoaDonDto.getDiaChiGiaoHang().getQuanHuyen())
                    .tinhThanh(hoaDonDto.getDiaChiGiaoHang().getTinhThanh())
                    .quocGia(hoaDonDto.getDiaChiGiaoHang().getQuocGia() != null ? hoaDonDto.getDiaChiGiaoHang().getQuocGia() : "Việt Nam")
                    .loaiDiaChi(hoaDonDto.getDiaChiGiaoHang().getLoaiDiaChi())
                    .laMacDinh(false) // New addresses for orders are not default
                    .build();

                diaChi = diaChiRepository.save(diaChi);
                log.info("Created new delivery address for customer {} and order", hoaDon.getKhachHang().getId());
            }

            hoaDon.setDiaChiGiaoHang(diaChi);
        }
        // For POS orders, delivery address might not be required
        else if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.ONLINE) {
            throw new IllegalArgumentException("Địa chỉ giao hàng là bắt buộc cho đơn hàng online");
        }

        // Set delivery contact information
        setDeliveryContactInfo(hoaDon, hoaDonDto);
    }

    /**
     * Set delivery contact information for the order.
     * Defaults to customer's information if not provided.
     */
    private void setDeliveryContactInfo(HoaDon hoaDon, HoaDonDto hoaDonDto) {
        // Use provided delivery contact info, or default to customer's info
        if (hoaDonDto.getNguoiNhanTen() != null && !hoaDonDto.getNguoiNhanTen().trim().isEmpty()) {
            hoaDon.setNguoiNhanTen(hoaDonDto.getNguoiNhanTen());
        } else if (hoaDon.getKhachHang() != null) {
            hoaDon.setNguoiNhanTen(hoaDon.getKhachHang().getHoTen());
        }

        if (hoaDonDto.getNguoiNhanSdt() != null && !hoaDonDto.getNguoiNhanSdt().trim().isEmpty()) {
            hoaDon.setNguoiNhanSdt(hoaDonDto.getNguoiNhanSdt());
        } else if (hoaDon.getKhachHang() != null) {
            hoaDon.setNguoiNhanSdt(hoaDon.getKhachHang().getSoDienThoai());
        }
    }

    /**
     * Update order line items with inventory management.
     * This handles adding, removing, and updating quantities of line items.
     */
    @Transactional
    public void updateOrderLineItems(HoaDon existingHoaDon, HoaDonDto hoaDonDto) {
        // Only allow line item updates for orders that haven't been shipped
        if (existingHoaDon.getTrangThaiDonHang() == TrangThaiDonHang.DANG_GIAO_HANG ||
            existingHoaDon.getTrangThaiDonHang() == TrangThaiDonHang.DA_GIAO_HANG ||
            existingHoaDon.getTrangThaiDonHang() == TrangThaiDonHang.HOAN_THANH ||
            existingHoaDon.getTrangThaiDonHang() == TrangThaiDonHang.DA_HUY) {
            throw new IllegalStateException("Cannot modify line items for orders in status: " + existingHoaDon.getTrangThaiDonHang());
        }

        // Get current line items
        List<HoaDonChiTiet> currentItems = new ArrayList<>(existingHoaDon.getHoaDonChiTiets());
        List<HoaDonChiTietDto> newItems = hoaDonDto.getChiTiet();

        // Track inventory changes
        List<Long> itemsToRelease = new ArrayList<>();

        // Process removals and updates
        for (HoaDonChiTiet currentItem : currentItems) {
            boolean found = false;
            for (HoaDonChiTietDto newItem : newItems) {
                if (newItem.getId() != null && newItem.getId().equals(currentItem.getId())) {
                    found = true;
                    // Update existing item
                    if (!newItem.getSoLuong().equals(currentItem.getSoLuong())) {
                        int quantityDiff = newItem.getSoLuong() - currentItem.getSoLuong();
                        if (quantityDiff > 0) {
                            // Need to reserve more items - check availability first
                            int availableQuantity = inventoryService.getAvailableQuantity(currentItem.getSanPhamChiTiet().getId());
                            if (availableQuantity < quantityDiff) {
                                throw new IllegalArgumentException("Insufficient inventory to increase quantity for product: " +
                                    currentItem.getSanPhamChiTiet().getId() + ". Requested: " + quantityDiff + ", Available: " + availableQuantity);
                            }
                            log.info("Validated availability for {} additional items for product {}", quantityDiff, currentItem.getSanPhamChiTiet().getId());
                        } else if (quantityDiff < 0) {
                            // Need to release some items
                            int itemsToReleaseCount = Math.abs(quantityDiff);
                            List<SanPhamChiTiet> availableItems = inventoryService.getAvailableItems(
                                currentItem.getSanPhamChiTiet().getId());
                            for (int i = 0; i < Math.min(itemsToReleaseCount, availableItems.size()); i++) {
                                itemsToRelease.add(availableItems.get(i).getId());
                            }
                        }

                        // Update quantity and recalculate line total
                        currentItem.setSoLuong(newItem.getSoLuong());
                        BigDecimal lineTotal = currentItem.getGiaBan().multiply(BigDecimal.valueOf(newItem.getSoLuong()));
                        currentItem.setThanhTien(lineTotal);
                    }
                    break;
                }
            }

            if (!found) {
                // Item was removed, release its inventory
                List<SanPhamChiTiet> availableItems = inventoryService.getAvailableItems(
                    currentItem.getSanPhamChiTiet().getId());
                for (int i = 0; i < Math.min(currentItem.getSoLuong(), availableItems.size()); i++) {
                    itemsToRelease.add(availableItems.get(i).getId());
                }
                existingHoaDon.getHoaDonChiTiets().remove(currentItem);
            }
        }

        // Apply inventory releases
        if (!itemsToRelease.isEmpty()) {
            inventoryService.releaseReservation(itemsToRelease);
            log.info("Released {} items during order update", itemsToRelease.size());
        }

        // Recalculate order totals
        recalculateOrderTotals(existingHoaDon);

        log.info("Updated line items for order {}", existingHoaDon.getId());
    }

    /**
     * Recalculate order totals based on current line items.
     */
    private void recalculateOrderTotals(HoaDon hoaDon) {
        BigDecimal tongTienHang = BigDecimal.ZERO;

        for (HoaDonChiTiet chiTiet : hoaDon.getHoaDonChiTiets()) {
            tongTienHang = tongTienHang.add(chiTiet.getThanhTien());
        }

        hoaDon.setTongTienHang(tongTienHang);

        BigDecimal phiVanChuyen = hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal giamGia = hoaDon.getGiaTriGiamGiaVoucher() != null ? hoaDon.getGiaTriGiamGiaVoucher() : BigDecimal.ZERO;

        BigDecimal tongThanhToan = tongTienHang.add(phiVanChuyen).subtract(giamGia);
        hoaDon.setTongThanhToan(tongThanhToan.max(BigDecimal.ZERO));
    }

    /**
     * Create JSON representation of entity for audit trail
     */
    private String createAuditValues(HoaDon entity) {
        return String.format(
            "{\"maHoaDon\":\"%s\",\"loaiHoaDon\":\"%s\",\"trangThaiDonHang\":\"%s\",\"trangThaiThanhToan\":\"%s\",\"tongThanhToan\":\"%s\",\"khachHangId\":\"%s\"}",
            entity.getMaHoaDon(),
            entity.getLoaiHoaDon(),
            entity.getTrangThaiDonHang(),
            entity.getTrangThaiThanhToan(),
            entity.getTongThanhToan(),
            entity.getKhachHang() != null ? entity.getKhachHang().getId() : null
        );
    }

    /**
     * Get audit history for a specific order
     */
    @Transactional(readOnly = true)
    public List<HoaDonAuditHistory> getAuditHistory(Long hoaDonId) {
        return auditHistoryRepository.findByHoaDonIdOrderByThoiGianThayDoiDesc(hoaDonId);
    }
}