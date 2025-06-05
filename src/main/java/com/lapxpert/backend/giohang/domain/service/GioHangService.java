package com.lapxpert.backend.giohang.domain.service;

import com.lapxpert.backend.giohang.application.dto.GioHangDto;
import com.lapxpert.backend.giohang.application.dto.GioHangChiTietDto;
import com.lapxpert.backend.giohang.application.dto.GioHangConversionDto;
import com.lapxpert.backend.giohang.application.dto.ThemSanPhamVaoGioRequest;
import com.lapxpert.backend.giohang.application.dto.CapNhatSoLuongRequest;
import com.lapxpert.backend.giohang.application.dto.CartToOrderRequestDto;
import com.lapxpert.backend.giohang.application.mapper.GioHangMapper;
import com.lapxpert.backend.giohang.application.mapper.GioHangChiTietMapper;
import com.lapxpert.backend.giohang.domain.entity.GioHang;
import com.lapxpert.backend.giohang.domain.entity.GioHangChiTiet;
import com.lapxpert.backend.giohang.domain.repository.GioHangRepository;
import com.lapxpert.backend.giohang.domain.repository.GioHangChiTietRepository;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import com.lapxpert.backend.sanpham.domain.service.PricingService;
import com.lapxpert.backend.hoadon.domain.service.HoaDonService;
import com.lapxpert.backend.hoadon.domain.dto.HoaDonDto;
import com.lapxpert.backend.hoadon.domain.dto.HoaDonChiTietDto;
import com.lapxpert.backend.hoadon.domain.enums.LoaiHoaDon;
import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.nguoidung.domain.repository.DiaChiRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class for GioHang (Shopping Cart) operations
 * Implements comprehensive cart management with business logic validation
 * Follows established service patterns with Vietnamese naming conventions
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GioHangService {

    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final GioHangMapper gioHangMapper;
    private final GioHangChiTietMapper gioHangChiTietMapper;
    private final PricingService pricingService;
    private final HoaDonService hoaDonService;
    private final DiaChiRepository diaChiRepository;

    /**
     * Get cart for a specific user, create if doesn't exist
     * @param nguoiDungId user ID
     * @return user's cart DTO
     */
    @Transactional(readOnly = true)
    public GioHangDto getCartByUserId(Long nguoiDungId) {
        log.debug("Getting cart for user ID: {}", nguoiDungId);

        Optional<GioHang> gioHangOpt = gioHangRepository.findByNguoiDung_Id(nguoiDungId);

        if (gioHangOpt.isPresent()) {
            GioHang gioHang = gioHangOpt.get();
            // Update prices and availability before returning
            updateCartItemPrices(gioHang);
            return gioHangMapper.toDto(gioHang);
        } else {
            // Create new empty cart for user
            return createEmptyCartForUser(nguoiDungId);
        }
    }

    /**
     * Get cart by user email
     * @param email user email
     * @return user's cart DTO
     */
    @Transactional(readOnly = true)
    public GioHangDto getCartByUserEmail(String email) {
        log.debug("Getting cart for user email: {}", email);

        Optional<GioHang> gioHangOpt = gioHangRepository.findByNguoiDung_Email(email);

        if (gioHangOpt.isPresent()) {
            GioHang gioHang = gioHangOpt.get();
            updateCartItemPrices(gioHang);
            return gioHangMapper.toDto(gioHang);
        } else {
            // Find user by email and create cart
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
            return createEmptyCartForUser(nguoiDung.getId());
        }
    }

    /**
     * Add product to cart
     * @param request add to cart request
     * @return updated cart DTO
     */
    public GioHangDto addProductToCart(ThemSanPhamVaoGioRequest request) {
        log.info("Adding product {} to cart for user {}", request.getSanPhamChiTietId(), request.getNguoiDungId());

        // Validate request
        validateAddToCartRequest(request);

        // Get or create cart
        GioHang gioHang = getOrCreateCart(request.getNguoiDungId());

        // Get product variant
        SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietRepository.findById(request.getSanPhamChiTietId())
            .orElseThrow(() -> new EntityNotFoundException("Product variant not found: " + request.getSanPhamChiTietId()));

        // Validate product availability
        validateProductAvailability(sanPhamChiTiet, request.getSoLuong());

        // Get current price (with discounts if applicable)
        BigDecimal currentPrice = pricingService.calculateEffectivePrice(sanPhamChiTiet);

        // Check if item already exists in cart
        Optional<GioHangChiTiet> existingItemOpt = gioHangChiTietRepository
            .findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), request.getSanPhamChiTietId());

        if (existingItemOpt.isPresent()) {
            // Update existing item
            GioHangChiTiet existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getSoLuong() + request.getSoLuong();

            // Validate total quantity
            validateProductAvailability(sanPhamChiTiet, newQuantity);

            existingItem.setSoLuong(newQuantity);
            // Update price if it has changed significantly
            if (shouldUpdatePrice(existingItem.getGiaTaiThoiDiemThem(), currentPrice)) {
                existingItem.setGiaTaiThoiDiemThem(currentPrice);
            }

            gioHangChiTietRepository.save(existingItem);
            log.info("Updated existing cart item quantity to {}", newQuantity);
        } else {
            // Add new item
            GioHangChiTiet newItem = GioHangChiTiet.builder()
                .gioHang(gioHang)
                .sanPhamChiTiet(sanPhamChiTiet)
                .soLuong(request.getSoLuong())
                .giaTaiThoiDiemThem(currentPrice)
                .build();

            gioHangChiTietRepository.save(newItem);
            gioHang.getChiTiets().add(newItem);
            log.info("Added new item to cart with quantity {}", request.getSoLuong());
        }

        gioHangRepository.save(gioHang);
        return gioHangMapper.toDto(gioHang);
    }

    /**
     * Update item quantity in cart
     * @param request update quantity request
     * @return updated cart DTO
     */
    public GioHangDto updateItemQuantity(CapNhatSoLuongRequest request) {
        log.info("Updating cart item quantity for user {} product {} to {}",
                request.getNguoiDungId(), request.getSanPhamChiTietId(), request.getSoLuongMoi());

        // Get cart
        GioHang gioHang = gioHangRepository.findByNguoiDung_Id(request.getNguoiDungId())
            .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + request.getNguoiDungId()));

        // Get cart item
        GioHangChiTiet cartItem = gioHangChiTietRepository
            .findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), request.getSanPhamChiTietId())
            .orElseThrow(() -> new EntityNotFoundException("Cart item not found"));

        if (request.getSoLuongMoi() <= 0) {
            // Remove item if quantity is 0 or negative
            gioHangChiTietRepository.delete(cartItem);
            gioHang.getChiTiets().remove(cartItem);
            log.info("Removed item from cart");
        } else {
            // Validate new quantity
            validateProductAvailability(cartItem.getSanPhamChiTiet(), request.getSoLuongMoi());

            cartItem.setSoLuong(request.getSoLuongMoi());
            gioHangChiTietRepository.save(cartItem);
            log.info("Updated cart item quantity to {}", request.getSoLuongMoi());
        }

        gioHangRepository.save(gioHang);
        return gioHangMapper.toDto(gioHang);
    }

    /**
     * Remove item from cart
     * @param nguoiDungId user ID
     * @param sanPhamChiTietId product variant ID
     * @return updated cart DTO
     */
    public GioHangDto removeItemFromCart(Long nguoiDungId, Long sanPhamChiTietId) {
        log.info("Removing item {} from cart for user {}", sanPhamChiTietId, nguoiDungId);

        GioHang gioHang = gioHangRepository.findByNguoiDung_Id(nguoiDungId)
            .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + nguoiDungId));

        GioHangChiTiet cartItem = gioHangChiTietRepository
            .findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), sanPhamChiTietId)
            .orElseThrow(() -> new EntityNotFoundException("Cart item not found"));

        gioHangChiTietRepository.delete(cartItem);
        gioHang.getChiTiets().remove(cartItem);
        gioHangRepository.save(gioHang);

        log.info("Successfully removed item from cart");
        return gioHangMapper.toDto(gioHang);
    }

    /**
     * Clear all items from cart
     * @param nguoiDungId user ID
     * @return empty cart DTO
     */
    public GioHangDto clearCart(Long nguoiDungId) {
        log.info("Clearing cart for user {}", nguoiDungId);

        GioHang gioHang = gioHangRepository.findByNguoiDung_Id(nguoiDungId)
            .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + nguoiDungId));

        gioHangChiTietRepository.deleteAll(gioHang.getChiTiets());
        gioHang.getChiTiets().clear();
        gioHangRepository.save(gioHang);

        log.info("Successfully cleared cart");
        return gioHangMapper.toDto(gioHang);
    }

    /**
     * Get cart items with price changes
     * @param nguoiDungId user ID
     * @return list of cart items with price changes
     */
    @Transactional(readOnly = true)
    public List<GioHangChiTietDto> getItemsWithPriceChanges(Long nguoiDungId) {
        GioHang gioHang = gioHangRepository.findByNguoiDung_Id(nguoiDungId)
            .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + nguoiDungId));

        List<GioHangChiTiet> itemsWithPriceChanges = gioHang.getChiTiets().stream()
            .filter(GioHangChiTiet::hasPriceChanged)
            .toList();

        return gioHangChiTietMapper.toDtoList(itemsWithPriceChanges);
    }

    /**
     * Validate cart for order conversion (preview mode)
     * @param request cart to order validation request
     * @return validation result with detailed information
     */
    @Transactional(readOnly = true)
    public CartValidationResult validateCartForOrderPreview(CartToOrderRequestDto request) {
        log.debug("Validating cart for order conversion preview for user {}", request.getNguoiDungId());

        // Get user's cart
        GioHang gioHang = gioHangRepository.findByNguoiDung_Id(request.getNguoiDungId())
            .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + request.getNguoiDungId()));

        // Validate cart is not empty
        if (gioHang.getChiTiets().isEmpty()) {
            CartValidationResult result = new CartValidationResult();
            result.setTotalMismatch(true);
            return result;
        }

        // Perform validation
        return validateCartContents(gioHang, request);
    }

    /**
     * Get cart conversion preview with detailed information
     * @param request cart to order conversion request
     * @return conversion preview with validation results and pricing breakdown
     */
    @Transactional(readOnly = true)
    public GioHangConversionDto getCartConversionPreview(CartToOrderRequestDto request) {
        log.debug("Getting cart conversion preview for user {}", request.getNguoiDungId());

        // Get user's cart
        GioHang gioHang = gioHangRepository.findByNguoiDung_Id(request.getNguoiDungId())
            .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + request.getNguoiDungId()));

        GioHangDto cartDto = gioHangMapper.toDto(gioHang);

        // Validate cart contents
        CartValidationResult validationResult = validateCartContents(gioHang, request);

        // Build conversion DTO
        GioHangConversionDto.GioHangConversionDtoBuilder builder = GioHangConversionDto.builder()
            .gioHang(cartDto)
            .tongTienTruocGiam(cartDto.getTongTien())
            .tongTienSauGiam(cartDto.getTongTien()) // Will be updated with discounts
            .tongTienThanhToan(cartDto.getTongTien()) // Will be updated with shipping
            .canProceedToOrder(validationResult.isValid())
            .conversionStatus(validationResult.isValid() ? "READY" : "VALIDATION_REQUIRED");

        // Add validation warnings
        if (!validationResult.isValid()) {
            builder.warnings(List.of(validationResult.getValidationSummary()));
        }

        return builder.build();
    }

    /**
     * Sync cart prices with current product prices
     * @param nguoiDungId user ID
     * @return updated cart DTO
     */
    public GioHangDto syncCartPrices(Long nguoiDungId) {
        log.info("Syncing cart prices for user {}", nguoiDungId);

        GioHang gioHang = gioHangRepository.findByNguoiDung_Id(nguoiDungId)
            .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + nguoiDungId));

        updateCartItemPrices(gioHang);
        gioHangRepository.save(gioHang);

        return gioHangMapper.toDto(gioHang);
    }

    /**
     * Convert cart to order
     * @param request cart to order conversion request
     * @return created order DTO
     */
    public HoaDonDto convertCartToOrder(CartToOrderRequestDto request) {
        log.info("Converting cart to order for user {}", request.getNguoiDungId());

        // Step 1: Validate request
        validateCartToOrderRequest(request);

        // Step 2: Get user's cart
        GioHang gioHang = gioHangRepository.findByNguoiDung_Id(request.getNguoiDungId())
            .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + request.getNguoiDungId()));

        // Step 3: Validate cart is not empty
        if (gioHang.getChiTiets().isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from empty cart");
        }

        // Step 4: Validate cart contents
        CartValidationResult validationResult = validateCartContents(gioHang, request);

        // Step 5: Handle validation issues based on force flags
        handleCartValidationIssues(validationResult, request);

        // Step 6: Create order DTO from cart
        HoaDonDto hoaDonDto = createOrderFromCart(gioHang, request);

        // Step 7: Create order through HoaDonService
        HoaDonDto createdOrder;
        try {
            NguoiDung currentUser = nguoiDungRepository.findById(request.getNguoiDungId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getNguoiDungId()));

            createdOrder = hoaDonService.createHoaDon(hoaDonDto, currentUser);
            log.info("Successfully created order {} from cart for user {}",
                    createdOrder.getId(), request.getNguoiDungId());
        } catch (Exception e) {
            log.error("Failed to create order from cart for user {}: {}",
                    request.getNguoiDungId(), e.getMessage());
            throw new RuntimeException("Failed to create order from cart: " + e.getMessage(), e);
        }

        // Step 8: Clear cart after successful order creation
        try {
            clearCart(request.getNguoiDungId());
            log.info("Successfully cleared cart after order creation for user {}", request.getNguoiDungId());
        } catch (Exception e) {
            log.warn("Order created successfully but failed to clear cart for user {}: {}",
                    request.getNguoiDungId(), e.getMessage());
            // Don't fail the entire operation if cart clearing fails
        }

        return createdOrder;
    }

    // Private helper methods for cart-to-order conversion

    /**
     * Validate cart to order request
     */
    private void validateCartToOrderRequest(CartToOrderRequestDto request) {
        if (request.getNguoiDungId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request.getDiaChiGiaoHangId() == null) {
            throw new IllegalArgumentException("Delivery address ID cannot be null");
        }
        if (request.getPhuongThucThanhToan() == null || request.getPhuongThucThanhToan().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method cannot be null or empty");
        }
        if (request.getTongTienXacNhan() == null || request.getTongTienXacNhan().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount confirmation must be provided and non-negative");
        }

        // Validate delivery address exists and belongs to user
        DiaChi deliveryAddress = diaChiRepository.findById(request.getDiaChiGiaoHangId())
            .orElseThrow(() -> new EntityNotFoundException("Delivery address not found: " + request.getDiaChiGiaoHangId()));

        if (!deliveryAddress.getNguoiDung().getId().equals(request.getNguoiDungId())) {
            throw new IllegalArgumentException("Delivery address does not belong to the specified user");
        }
    }

    /**
     * Validate cart contents for order conversion
     */
    private CartValidationResult validateCartContents(GioHang gioHang, CartToOrderRequestDto request) {
        CartValidationResult result = new CartValidationResult();
        BigDecimal calculatedTotal = BigDecimal.ZERO;

        for (GioHangChiTiet cartItem : gioHang.getChiTiets()) {
            // Check product availability
            if (!cartItem.getSanPhamChiTiet().getTrangThai()) {
                result.addUnavailableItem(cartItem);
                continue; // Skip price calculation for unavailable items
            }

            // Check for price changes
            BigDecimal currentPrice = pricingService.calculateEffectivePrice(cartItem.getSanPhamChiTiet());
            if (!cartItem.getGiaTaiThoiDiemThem().equals(currentPrice)) {
                result.addPriceChangedItem(cartItem, currentPrice);
            }

            // Calculate total using cart prices (not current prices)
            calculatedTotal = calculatedTotal.add(cartItem.getThanhTien());
        }

        result.setCalculatedTotal(calculatedTotal);

        // Validate total amount matches request
        BigDecimal tolerance = BigDecimal.valueOf(0.01); // 1 cent tolerance
        if (calculatedTotal.subtract(request.getTongTienXacNhan()).abs().compareTo(tolerance) > 0) {
            result.setTotalMismatch(true);
            result.setExpectedTotal(calculatedTotal);
            result.setProvidedTotal(request.getTongTienXacNhan());
        }

        return result;
    }

    /**
     * Handle cart validation issues based on force flags
     */
    private void handleCartValidationIssues(CartValidationResult validationResult, CartToOrderRequestDto request) {
        // Handle unavailable items
        if (validationResult.hasUnavailableItems() && !request.getForceConvertWithUnavailableItems()) {
            throw new IllegalArgumentException(
                String.format("Cart contains %d unavailable items. Use forceConvertWithUnavailableItems=true to proceed anyway.",
                    validationResult.getUnavailableItems().size()));
        }

        // Handle price changes
        if (validationResult.hasPriceChanges() && !request.getForceConvertWithPriceChanges()) {
            throw new IllegalArgumentException(
                String.format("Cart contains %d items with price changes. Use forceConvertWithPriceChanges=true to proceed anyway.",
                    validationResult.getPriceChangedItems().size()));
        }

        // Handle total mismatch (always fail, regardless of force flags)
        if (validationResult.isTotalMismatch()) {
            throw new IllegalArgumentException(
                String.format("Total amount mismatch. Expected: %s, Provided: %s",
                    validationResult.getExpectedTotal(), validationResult.getProvidedTotal()));
        }
    }

    /**
     * Create order DTO from cart
     */
    private HoaDonDto createOrderFromCart(GioHang gioHang, CartToOrderRequestDto request) {
        HoaDonDto hoaDonDto = new HoaDonDto();

        // Set basic order information
        hoaDonDto.setKhachHangId(request.getNguoiDungId());
        hoaDonDto.setDiaChiGiaoHangId(request.getDiaChiGiaoHangId());
        hoaDonDto.setLoaiHoaDon(LoaiHoaDon.ONLINE);

        // Set delivery information
        if (request.getTenNguoiNhan() != null) {
            hoaDonDto.setNguoiNhanTen(request.getTenNguoiNhan());
        }
        if (request.getSoDienThoaiGiaoHang() != null) {
            hoaDonDto.setNguoiNhanSdt(request.getSoDienThoaiGiaoHang());
        }

        // Set voucher codes if provided
        if (request.getPhieuGiamGiaIds() != null && !request.getPhieuGiamGiaIds().isEmpty()) {
            // Convert IDs to codes - this would need to be implemented based on voucher service
            // For now, we'll leave this empty and let the order service handle it
            hoaDonDto.setVoucherCodes(List.of());
        }

        // Convert cart items to order items
        List<HoaDonChiTietDto> orderItems = new ArrayList<>();
        for (GioHangChiTiet cartItem : gioHang.getChiTiets()) {
            // Skip unavailable items if force flag is not set
            if (!cartItem.getSanPhamChiTiet().getTrangThai() && !request.getForceConvertWithUnavailableItems()) {
                continue;
            }

            HoaDonChiTietDto orderItem = createOrderItemFromCartItem(cartItem);
            orderItems.add(orderItem);
        }

        hoaDonDto.setChiTiet(orderItems);

        return hoaDonDto;
    }

    /**
     * Create order item from cart item
     */
    private HoaDonChiTietDto createOrderItemFromCartItem(GioHangChiTiet cartItem) {
        HoaDonChiTietDto orderItem = new HoaDonChiTietDto();

        orderItem.setSanPhamChiTietId(cartItem.getSanPhamChiTiet().getId());
        orderItem.setSoLuong(cartItem.getSoLuong());

        // Use cart price as the sale price (preserving user's pricing at time of adding to cart)
        orderItem.setGiaBan(cartItem.getGiaTaiThoiDiemThem());
        orderItem.setGiaGoc(cartItem.getSanPhamChiTiet().getGiaBan()); // Current product price as original price
        orderItem.setThanhTien(cartItem.getThanhTien());

        // Set snapshot information for order history
        orderItem.setTenSanPhamSnapshot(cartItem.getSanPhamChiTiet().getSanPham().getTenSanPham());
        orderItem.setSkuSnapshot(cartItem.getSanPhamChiTiet().getSku());

        // Set first image as snapshot
        if (cartItem.getSanPhamChiTiet().getSanPham().getHinhAnh() != null &&
            !cartItem.getSanPhamChiTiet().getSanPham().getHinhAnh().isEmpty()) {
            orderItem.setHinhAnhSnapshot(cartItem.getSanPhamChiTiet().getSanPham().getHinhAnh().get(0));
        }

        return orderItem;
    }

    // Existing private helper methods

    /**
     * Create empty cart for user
     */
    private GioHangDto createEmptyCartForUser(Long nguoiDungId) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(nguoiDungId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + nguoiDungId));

        GioHang gioHang = GioHang.builder()
            .nguoiDung(nguoiDung)
            .build();

        gioHang = gioHangRepository.save(gioHang);
        log.info("Created new empty cart for user {}", nguoiDungId);

        return gioHangMapper.toDto(gioHang);
    }

    /**
     * Get or create cart for user
     */
    private GioHang getOrCreateCart(Long nguoiDungId) {
        Optional<GioHang> gioHangOpt = gioHangRepository.findByNguoiDung_Id(nguoiDungId);

        if (gioHangOpt.isPresent()) {
            return gioHangOpt.get();
        } else {
            NguoiDung nguoiDung = nguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + nguoiDungId));

            GioHang gioHang = GioHang.builder()
                .nguoiDung(nguoiDung)
                .build();

            return gioHangRepository.save(gioHang);
        }
    }

    /**
     * Validate add to cart request
     */
    private void validateAddToCartRequest(ThemSanPhamVaoGioRequest request) {
        if (request.getNguoiDungId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request.getSanPhamChiTietId() == null) {
            throw new IllegalArgumentException("Product variant ID cannot be null");
        }
        if (request.getSoLuong() == null || request.getSoLuong() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (request.getSoLuong() > 999) {
            throw new IllegalArgumentException("Quantity cannot exceed 999");
        }
    }

    /**
     * Validate product availability
     */
    private void validateProductAvailability(SanPhamChiTiet sanPhamChiTiet, Integer requestedQuantity) {
        if (!sanPhamChiTiet.getTrangThai()) {
            throw new IllegalArgumentException("Product is not available: " + sanPhamChiTiet.getSku());
        }

        // Additional inventory checks can be added here
        // For now, we assume if product is AVAILABLE, it can be added to cart
        // Real inventory checking would be done during checkout
    }

    /**
     * Check if price should be updated (significant change)
     */
    private boolean shouldUpdatePrice(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null || newPrice == null) {
            return false;
        }

        // Update if price difference is more than 5%
        BigDecimal difference = newPrice.subtract(oldPrice).abs();
        BigDecimal threshold = oldPrice.multiply(BigDecimal.valueOf(0.05));

        return difference.compareTo(threshold) > 0;
    }

    /**
     * Update cart item prices with current product prices
     * This method ensures current prices are available for comparison in DTOs
     */
    private void updateCartItemPrices(GioHang gioHang) {
        // This method is called to trigger price recalculation in the DTO mapper
        // The actual price comparison is done in the mapper methods
        // We don't automatically update cart prices to preserve user's original pricing
        log.debug("Refreshing price information for cart with {} items", gioHang.getChiTiets().size());
    }
}
