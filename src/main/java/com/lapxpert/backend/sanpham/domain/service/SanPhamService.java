package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.sanpham.application.dto.BatchOperationResult;
import com.lapxpert.backend.sanpham.application.dto.SanPhamDto;
import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.application.mapper.SanPhamChiTietMapper;
import com.lapxpert.backend.sanpham.application.mapper.SanPhamMapper;
import com.lapxpert.backend.sanpham.domain.entity.SanPhamAuditHistory;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
// TrangThaiSanPham enum removed - using Boolean status instead
import com.lapxpert.backend.sanpham.domain.repository.SanPhamAuditHistoryRepository;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamRepository;
// TODO: Import CacheKeyBuilder for consistent cache key management
// import com.lapxpert.backend.common.cache.CacheKeyBuilder;
import com.lapxpert.backend.common.service.BusinessEntityService;
import com.lapxpert.backend.common.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SanPhamService extends BusinessEntityService<SanPham, Long, SanPhamDto, SanPhamAuditHistory> {
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamMapper sanPhamMapper;
    private final SanPhamAuditHistoryRepository auditHistoryRepository;

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamChiTietMapper sanPhamChiTietMapper;

    private final PricingService pricingService;
    private final ApplicationEventPublisher eventPublisher;

    public String generateMaSanPham() {
        String lastMaSanPham = sanPhamRepository.findLastMaSanPham();

        if (lastMaSanPham == null) {
            return "SP001";
        }

        try {
            String numberPart = lastMaSanPham.substring(2);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã sản phẩm (SP999)");
            }

            return String.format("SP%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã sản phẩm không hợp lệ: " + lastMaSanPham);
        }
    }

    @CacheEvict(value = {"sanPhamList", "activeSanPhamList"}, allEntries = true)
    @Transactional
    public SanPhamDto createSanPhamWithChiTiet(SanPhamDto sanPhamDto) {
        // Chuyển đổi DTO sang Entity
        SanPham sanPham = sanPhamMapper.toEntity(sanPhamDto);
        sanPham.setMaSanPham(generateMaSanPham());
        // Lưu sản phẩm chính trước
        SanPham savedSanPham = sanPhamRepository.save(sanPham);

        // Xử lý danh sách sản phẩm chi tiết
        if (sanPhamDto.getSanPhamChiTiets() != null && !sanPhamDto.getSanPhamChiTiets().isEmpty()) {
            Set<String> incomingSkus = new HashSet<>();
            Set<SanPhamChiTiet> chiTiets = sanPhamDto.getSanPhamChiTiets().stream()
                    .map(dto -> {
                        SanPhamChiTiet chiTiet = sanPhamChiTietMapper.toEntity(dto);
                        // Generate SKU based on product code and variant attributes
                        if (chiTiet.getSku() == null || chiTiet.getSku().isEmpty()) {
                            String baseSku = generateVariantSku(savedSanPham.getMaSanPham(), chiTiet);
                            // Generate unique SKU to avoid database conflicts
                            String uniqueSku = generateUniqueSku(baseSku, incomingSkus, new HashSet<>());
                            chiTiet.setSku(uniqueSku);
                            incomingSkus.add(uniqueSku);
                        }
                        chiTiet.setSanPham(savedSanPham);
                        return chiTiet;
                    })
                    .collect(Collectors.toSet());

            // Lưu danh sách sản phẩm chi tiết
            Set<SanPhamChiTiet> savedChiTiets = sanPhamChiTietRepository.saveAll(chiTiets)
                    .stream()
                    .collect(Collectors.toSet());

            savedSanPham.setSanPhamChiTiets(savedChiTiets);
        }

        return sanPhamMapper.toDto(savedSanPham);
    }

    @Cacheable(value = "sanPhamList")
    @Transactional(readOnly = true)
    public List<SanPhamDto> findAll() {
        // Use inherited findAll method with caching from BusinessEntityService
        List<SanPhamDto> dtos = super.findAll();

        // Apply promotional pricing from DotGiamGia campaigns to all product variants
        return applyPromotionalPricingToProducts(dtos);
    }

    /**
     * Find product by ID with complete variant data including all 6-core attributes
     * @param id Product ID
     * @return SanPhamDto with complete variant information
     * @throws RuntimeException if product not found
     */
    @Transactional(readOnly = true)
    public SanPhamDto getSanPhamById(Long id) {
        // Use inherited findById method with caching from BusinessEntityService
        SanPhamDto dto = findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));

        // Apply promotional pricing to variants if available
        if (dto.getSanPhamChiTiets() != null && !dto.getSanPhamChiTiets().isEmpty()) {
            for (SanPhamChiTietDto variantDto : dto.getSanPhamChiTiets()) {
                // Get the actual entity to calculate promotional pricing
                SanPhamChiTiet variant = sanPhamChiTietRepository.findById(variantDto.getId()).orElse(null);
                if (variant != null) {
                    // Calculate effective price using PricingService (includes DotGiamGia campaigns)
                    BigDecimal effectivePrice = pricingService.calculateEffectivePrice(variant);

                    // Only set giaKhuyenMai if there's actually a discount
                    if (effectivePrice.compareTo(variant.getGiaBan()) < 0) {
                        variantDto.setGiaKhuyenMai(effectivePrice);
                    } else {
                        // No discount, clear promotional price
                        variantDto.setGiaKhuyenMai(null);
                    }
                }
            }
        }

        return dto;
    }

    @Cacheable(value = "activeSanPhamList")
    @Transactional(readOnly = true)
    public List<SanPhamDto> getActiveProducts() {
        List<SanPham> entities = sanPhamRepository.findAllByTrangThai(true);
        List<SanPhamDto> dtos = sanPhamMapper.toDtos(entities);

        // Apply promotional pricing from DotGiamGia campaigns to all product variants
        return applyPromotionalPricingToProducts(dtos);
    }

    // Search products with filters and promotional pricing
    @Transactional(readOnly = true)
    public List<SanPhamDto> searchProducts(Map<String, Object> searchFilters) {
        if (searchFilters == null || searchFilters.isEmpty()) {
            return getActiveProducts();
        }

        String tenSanPham = (String) searchFilters.get("tenSanPham");
        String maSanPham = (String) searchFilters.get("maSanPham");
        String moTa = (String) searchFilters.get("moTa");

        // If it's a simple string search (from frontend), use it as product name
        if (searchFilters.size() == 1 && searchFilters.containsKey("tenSanPham") == false) {
            Object searchValue = searchFilters.values().iterator().next();
            if (searchValue instanceof String) {
                tenSanPham = (String) searchValue;
            }
        }

        List<SanPham> entities = sanPhamRepository.searchProducts(tenSanPham, maSanPham, moTa);
        List<SanPhamDto> dtos = sanPhamMapper.toDtos(entities);

        // Apply promotional pricing from DotGiamGia campaigns to all product variants
        return applyPromotionalPricingToProducts(dtos);
    }

    /**
     * Apply promotional pricing from active DotGiamGia campaigns to product variants
     * This method calculates effective prices using PricingService and updates giaKhuyenMai field
     * @param products List of SanPhamDto to apply promotional pricing to
     * @return List of SanPhamDto with updated promotional prices
     */
    private List<SanPhamDto> applyPromotionalPricingToProducts(List<SanPhamDto> products) {
        for (SanPhamDto product : products) {
            if (product.getSanPhamChiTiets() != null && !product.getSanPhamChiTiets().isEmpty()) {
                for (SanPhamChiTietDto variantDto : product.getSanPhamChiTiets()) {
                    // Get the actual entity to calculate promotional pricing
                    SanPhamChiTiet variant = sanPhamChiTietRepository.findById(variantDto.getId()).orElse(null);
                    if (variant != null) {
                        // Calculate effective price using PricingService (includes DotGiamGia campaigns)
                        BigDecimal effectivePrice = pricingService.calculateEffectivePrice(variant);

                        // Only set giaKhuyenMai if there's actually a discount
                        if (effectivePrice.compareTo(variant.getGiaBan()) < 0) {
                            variantDto.setGiaKhuyenMai(effectivePrice);
                        } else {
                            // No discount, clear promotional price
                            variantDto.setGiaKhuyenMai(null);
                        }
                    }
                }
            }
        }
        return products;
    }

    // Thêm sản phẩm mới
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    @Transactional
    public SanPham addProduct(SanPham sanPham) {
        return addProductWithAudit(sanPham, "Tạo sản phẩm mới");
    }

    // Thêm sản phẩm mới với audit trail
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    @Transactional
    public SanPham addProductWithAudit(SanPham sanPham, String reason) {
        sanPham.setTrangThai(true);
        if (sanPham.getMaSanPham() == null) {
            sanPham.setMaSanPham(generateMaSanPham());
        }

        // Convert to DTO and use inherited create method with audit trail and cache management
        SanPhamDto dto = sanPhamMapper.toDto(sanPham);
        SanPhamDto createdDto = create(dto, "SYSTEM", reason != null ? reason : "Tạo sản phẩm mới");

        // Convert back to entity for return compatibility
        return sanPhamMapper.toEntity(createdDto);
    }

    // ==================== BUSINESSENTITYSERVICE ABSTRACT METHOD IMPLEMENTATIONS ====================

    @Override
    protected JpaRepository<SanPham, Long> getRepository() {
        return sanPhamRepository;
    }

    @Override
    protected JpaRepository<SanPhamAuditHistory, Long> getAuditRepository() {
        return auditHistoryRepository;
    }

    @Override
    protected SanPhamDto toDto(SanPham entity) {
        return sanPhamMapper.toDto(entity);
    }

    @Override
    protected SanPham toEntity(SanPhamDto dto) {
        return sanPhamMapper.toEntity(dto);
    }

    @Override
    protected SanPhamAuditHistory createAuditEntry(Long entityId, String action, String oldValues, String newValues, String nguoiThucHien, String lyDo) {
        switch (action) {
            case "CREATE":
                return SanPhamAuditHistory.createEntry(entityId, newValues, nguoiThucHien, lyDo);
            case "UPDATE":
                return SanPhamAuditHistory.updateEntry(entityId, oldValues, newValues, nguoiThucHien, lyDo);
            case "SOFT_DELETE":
            case "DELETE":
                return SanPhamAuditHistory.deleteEntry(entityId, oldValues, nguoiThucHien, lyDo);
            default:
                return SanPhamAuditHistory.createEntry(entityId, newValues, nguoiThucHien, lyDo);
        }
    }

    @Override
    protected String getEntityName() {
        return "Sản phẩm";
    }

    @Override
    protected void validateEntity(SanPham entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Sản phẩm không được null");
        }

        // Validate product code
        if (entity.getMaSanPham() != null) {
            ValidationUtils.validateProductCode(entity.getMaSanPham(), "Mã sản phẩm");
        }

        // Validate product name
        if (entity.getTenSanPham() == null || entity.getTenSanPham().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }

        if (entity.getTenSanPham().trim().length() < 3) {
            throw new IllegalArgumentException("Tên sản phẩm phải có ít nhất 3 ký tự");
        }

        // Validate image count
        if (entity.getHinhAnh() != null && entity.getHinhAnh().size() > 10) {
            throw new IllegalArgumentException("Sản phẩm không được có quá 10 hình ảnh");
        }
    }

    @Override
    protected void evictCache() {
        // Cache eviction is handled by @CacheEvict annotations in BusinessEntityService
        log.debug("Cache evicted for SanPham");
    }

    @Override
    protected Long getEntityId(SanPham entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(SanPham entity, Long id) {
        entity.setId(id);
    }

    @Override
    protected void setSoftDeleteStatus(SanPham entity, boolean status) {
        // SanPham uses Boolean trangThai for soft delete
        entity.setTrangThai(status);
    }

    @Override
    protected List<SanPhamAuditHistory> getAuditHistoryByEntityId(Long entityId) {
        return auditHistoryRepository.findBySanPhamIdOrderByThoiGianThayDoiDesc(entityId);
    }

    @Override
    protected ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    @Override
    protected String getCacheName() {
        return "sanPhamCache";
    }

    @Override
    protected void publishEntityCreatedEvent(SanPham entity) {
        // TODO: Implement product created event publishing for real-time updates
        log.debug("Publishing product created event for ID: {}", entity.getId());
    }

    @Override
    protected void publishEntityUpdatedEvent(SanPham entity, SanPham oldEntity) {
        // TODO: Implement product updated event publishing for real-time updates
        log.debug("Publishing product updated event for ID: {}", entity.getId());
    }

    @Override
    protected void publishEntityDeletedEvent(Long entityId) {
        // TODO: Implement product deleted event publishing for real-time updates
        log.debug("Publishing product deleted event for ID: {}", entityId);
    }

    @Override
    protected void validateBusinessRules(SanPham entity) {
        // Validate product-specific business rules
        validateProductBusinessRules(entity);

        // Check for duplicate product code only for new entities (ID is null)
        if (entity.getId() == null && entity.getMaSanPham() != null) {
            if (!isProductCodeUnique(entity.getMaSanPham(), null)) {
                throw new IllegalArgumentException("Mã sản phẩm đã tồn tại: " + entity.getMaSanPham());
            }
        }
    }

    @Override
    protected void validateBusinessRulesForUpdate(SanPham entity, SanPham existingEntity) {
        validateBusinessRules(entity);

        // Additional validation for updates
        if (entity.getMaSanPham() != null && !entity.getMaSanPham().equals(existingEntity.getMaSanPham())) {
            if (!isProductCodeUnique(entity.getMaSanPham(), existingEntity.getId())) {
                throw new IllegalArgumentException("Mã sản phẩm đã tồn tại: " + entity.getMaSanPham());
            }
        }
    }

    @Override
    protected SanPham cloneEntity(SanPham entity) {
        SanPham clone = new SanPham();
        clone.setId(entity.getId());
        clone.setMaSanPham(entity.getMaSanPham());
        clone.setTenSanPham(entity.getTenSanPham());
        clone.setMoTa(entity.getMoTa());
        clone.setTrangThai(entity.getTrangThai());
        clone.setThuongHieu(entity.getThuongHieu());
        clone.setNgayRaMat(entity.getNgayRaMat());
        if (entity.getHinhAnh() != null) {
            clone.setHinhAnh(entity.getHinhAnh());
        }
        if (entity.getDanhMucs() != null) {
            clone.setDanhMucs(new HashSet<>(entity.getDanhMucs()));
        }
        return clone;
    }

    /**
     * Build JSON string for audit trail from SanPham entity
     * @param sanPham SanPham entity
     * @return JSON string representation
     */
    @Override
    protected String buildAuditJson(SanPham sanPham) {
        // Get all category names as JSON array
        String danhMucNames = "[]";
        if (sanPham.getDanhMucs() != null && !sanPham.getDanhMucs().isEmpty()) {
            danhMucNames = "[" + sanPham.getDanhMucs().stream()
                .map(danhMuc -> "\"" + danhMuc.getMoTaDanhMuc() + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("") + "]";
        }

        return String.format(
            "{\"maSanPham\":\"%s\",\"tenSanPham\":\"%s\",\"moTa\":\"%s\",\"trangThai\":%s,\"thuongHieu\":\"%s\",\"danhMucs\":%s}",
            sanPham.getMaSanPham() != null ? sanPham.getMaSanPham() : "",
            sanPham.getTenSanPham() != null ? sanPham.getTenSanPham() : "",
            sanPham.getMoTa() != null ? sanPham.getMoTa() : "",
            sanPham.getTrangThai(),
            sanPham.getThuongHieu() != null ? sanPham.getThuongHieu().getMoTaThuongHieu() : "",
            danhMucNames
        );
    }



    // Cập nhật sản phẩm
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    @Transactional
    public SanPham updateProduct(Long id, SanPham sanPham) {
        return updateProductWithAudit(id, sanPham, "Cập nhật thông tin sản phẩm", null, null);
    }

    // Cập nhật sản phẩm với DTO
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    @Transactional
    public SanPhamDto updateProductDto(Long id, SanPhamDto sanPhamDto) {
        SanPham sanPham = sanPhamMapper.toEntity(sanPhamDto);
        SanPham updatedProduct = updateProductWithAudit(id, sanPham, "Cập nhật thông tin sản phẩm", null, null);
        return sanPhamMapper.toDto(updatedProduct);
    }

    // Cập nhật sản phẩm với biến thể
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    @Transactional
    public SanPhamDto updateProductWithVariants(Long id, SanPhamDto sanPhamDto) {
        // Get existing product
        SanPham existingProduct = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // Capture old values for audit
        String oldValues = buildAuditJson(existingProduct);

        // Update main product fields
        existingProduct.setMaSanPham(sanPhamDto.getMaSanPham());
        existingProduct.setTenSanPham(sanPhamDto.getTenSanPham());
        existingProduct.setThuongHieu(sanPhamMapper.toEntity(sanPhamDto).getThuongHieu());
        existingProduct.setMoTa(sanPhamDto.getMoTa());
        existingProduct.setHinhAnh(sanPhamDto.getHinhAnh());
        existingProduct.setNgayRaMat(sanPhamDto.getNgayRaMat());
        existingProduct.setTrangThai(sanPhamDto.getTrangThai());

        // Update DanhMucs many-to-many relationship
        if (sanPhamDto.getDanhMucs() != null) {
            existingProduct.getDanhMucs().clear();
            existingProduct.getDanhMucs().addAll(
                sanPhamDto.getDanhMucs().stream()
                    .map(dto -> {
                        DanhMuc danhMuc = new DanhMuc();
                        danhMuc.setId(dto.getId());
                        danhMuc.setMoTaDanhMuc(dto.getMoTaDanhMuc());
                        return danhMuc;
                    })
                    .collect(Collectors.toSet())
            );
        }

        // Handle variants (SanPhamChiTiets) with incremental updates
        if (sanPhamDto.getSanPhamChiTiets() != null) {
            // Get existing variants for comparison
            Set<SanPhamChiTiet> existingVariants = existingProduct.getSanPhamChiTiets() != null ?
                new HashSet<>(existingProduct.getSanPhamChiTiets()) : new HashSet<>();

            // Process incoming variants
            Set<SanPhamChiTiet> incomingVariants = new HashSet<>();
            Set<String> incomingSerialNumbers = new HashSet<>();

            for (var dto : sanPhamDto.getSanPhamChiTiets()) {
                if (dto.getId() != null) {
                    // Update existing variant
                    SanPhamChiTiet existingVariant = existingVariants.stream()
                        .filter(v -> v.getId().equals(dto.getId()))
                        .findFirst()
                        .orElse(null);

                    if (existingVariant != null) {
                        // Update existing variant fields
                        updateVariantFromDto(existingVariant, dto);
                        incomingVariants.add(existingVariant);
                        incomingSerialNumbers.add(existingVariant.getSku());
                    }
                } else {
                    // Create new variant
                    SanPhamChiTiet newVariant = sanPhamChiTietMapper.toEntity(dto);

                    // Generate SKU based on product code and variant attributes
                    if (newVariant.getSku() == null || newVariant.getSku().isEmpty()) {
                        newVariant.setSku(generateVariantSku(existingProduct.getMaSanPham(), newVariant));
                    }

                    // Check for SKU conflicts and generate unique one if needed
                    String uniqueSku = generateUniqueSku(newVariant.getSku(), incomingSerialNumbers, existingVariants);
                    newVariant.setSku(uniqueSku);
                    incomingSerialNumbers.add(uniqueSku);

                    newVariant.setSanPham(existingProduct);
                    newVariant.setTrangThai(true); // Available status
                    incomingVariants.add(newVariant);
                }
            }

            // Identify variants to soft delete (existing variants not in incoming list)
            Set<SanPhamChiTiet> variantsToDelete = existingVariants.stream()
                .filter(existing -> incomingVariants.stream()
                    .noneMatch(incoming -> incoming.getId() != null && incoming.getId().equals(existing.getId())))
                .collect(Collectors.toSet());

            // Soft delete removed variants
            variantsToDelete.forEach(variant -> {
                variant.setTrangThai(false); // Unavailable status
                sanPhamChiTietRepository.save(variant);
            });

            // Save all incoming variants (new and updated)
            Set<SanPhamChiTiet> savedVariants = sanPhamChiTietRepository.saveAll(incomingVariants)
                    .stream()
                    .collect(Collectors.toSet());

            // Update product's variant collection (keep existing available variants + new variants)
            Set<SanPhamChiTiet> finalVariants = new HashSet<>();
            finalVariants.addAll(savedVariants);
            finalVariants.addAll(existingVariants.stream()
                .filter(v -> v.getTrangThai() == true &&
                           !variantsToDelete.contains(v))
                .collect(Collectors.toSet()));

            existingProduct.setSanPhamChiTiets(finalVariants);
        }

        // Save the updated product
        SanPham savedProduct = sanPhamRepository.save(existingProduct);

        // Capture new values for audit
        String newValues = buildAuditJson(savedProduct);

        // Create audit trail entry for update with variants
        SanPhamAuditHistory auditEntry = SanPhamAuditHistory.updateEntry(
            savedProduct.getId(),
            oldValues,
            newValues,
            savedProduct.getNguoiCapNhat(),
            "Cập nhật sản phẩm với biến thể"
        );
        auditHistoryRepository.save(auditEntry);

        return sanPhamMapper.toDto(savedProduct);
    }

    /**
     * Update variant fields from DTO
     * @param variant Existing variant to update
     * @param dto DTO with new values
     */
    private void updateVariantFromDto(SanPhamChiTiet variant, com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDto dto) {
        // Update basic fields
        if (dto.getGiaBan() != null) variant.setGiaBan(dto.getGiaBan());
        if (dto.getGiaKhuyenMai() != null) variant.setGiaKhuyenMai(dto.getGiaKhuyenMai());
        if (dto.getHinhAnh() != null) variant.setHinhAnh(dto.getHinhAnh());
        if (dto.getTrangThai() != null) variant.setTrangThai(dto.getTrangThai());

        // Update the 6 core attribute relationships using mapper
        SanPhamChiTiet tempVariant = sanPhamChiTietMapper.toEntity(dto);
        if (tempVariant.getMauSac() != null) variant.setMauSac(tempVariant.getMauSac());
        if (tempVariant.getCpu() != null) variant.setCpu(tempVariant.getCpu());
        if (tempVariant.getRam() != null) variant.setRam(tempVariant.getRam());
        if (tempVariant.getGpu() != null) variant.setGpu(tempVariant.getGpu());
        if (tempVariant.getBoNho() != null) variant.setBoNho(tempVariant.getBoNho());
        if (tempVariant.getManHinh() != null) variant.setManHinh(tempVariant.getManHinh());
    }

    /**
     * Generate variant SKU based on product code and attributes
     * @param productCode Product code
     * @param variant Variant with attributes
     * @return Generated SKU
     */
    private String generateVariantSku(String productCode, SanPhamChiTiet variant) {
        StringBuilder sku = new StringBuilder(productCode);

        // Add core attributes to SKU
        if (variant.getCpu() != null && variant.getCpu().getMoTaCpu() != null) {
            sku.append("-").append(variant.getCpu().getMoTaCpu().replaceAll("\\s+", "").toUpperCase());
        }
        if (variant.getRam() != null && variant.getRam().getMoTaRam() != null) {
            sku.append("-").append(variant.getRam().getMoTaRam().replaceAll("\\s+", "").toUpperCase());
        }
        if (variant.getBoNho() != null && variant.getBoNho().getMoTaBoNho() != null) {
            sku.append("-").append(variant.getBoNho().getMoTaBoNho().replaceAll("\\s+", "").toUpperCase());
        }
        if (variant.getMauSac() != null && variant.getMauSac().getMoTaMauSac() != null) {
            sku.append("-").append(variant.getMauSac().getMoTaMauSac().replaceAll("\\s+", "").toUpperCase());
        }

        return sku.toString();
    }

    /**
     * Generate unique SKU to avoid conflicts
     * @param baseSku Base SKU
     * @param incomingSkus Set of incoming SKUs
     * @param existingVariants Set of existing variants
     * @return Unique SKU
     */
    private String generateUniqueSku(String baseSku, Set<String> incomingSkus, Set<SanPhamChiTiet> existingVariants) {
        int counter = 1;

        // Helper method to check if SKU exists (includes database check)
        java.util.function.Predicate<String> skuExists = sku ->
            incomingSkus.contains(sku) ||
            existingVariants.stream().anyMatch(v -> v.getSku() != null && v.getSku().equals(sku)) ||
            sanPhamChiTietRepository.existsBySku(sku);

        // Start with base SKU
        if (!skuExists.test(baseSku)) {
            return baseSku;
        }

        // Generate numbered variants
        while (counter <= 999) {
            String candidateSku = baseSku + "-" + String.format("%03d", counter);
            if (!skuExists.test(candidateSku)) {
                return candidateSku;
            }
            counter++;
        }

        // If numbered approach fails, use random suffix as fallback
        return generateSkuWithRandomSuffix(baseSku, incomingSkus, existingVariants);
    }

    /**
     * Generate SKU with random suffix as fallback when numbered approach fails
     * @param baseSku Base SKU
     * @param incomingSkus Set of incoming SKUs
     * @param existingVariants Set of existing variants
     * @return Unique SKU with random suffix
     */
    private String generateSkuWithRandomSuffix(String baseSku, Set<String> incomingSkus, Set<SanPhamChiTiet> existingVariants) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        // Helper method to check if SKU exists (includes database check)
        java.util.function.Predicate<String> skuExists = sku ->
            incomingSkus.contains(sku) ||
            existingVariants.stream().anyMatch(v -> v.getSku() != null && v.getSku().equals(sku)) ||
            sanPhamChiTietRepository.existsBySku(sku);

        // Try up to 100 random suffixes
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                suffix.append(chars.charAt(random.nextInt(chars.length())));
            }

            String candidateSku = baseSku + "-" + suffix.toString();
            if (!skuExists.test(candidateSku)) {
                return candidateSku;
            }
        }

        throw new RuntimeException("Không thể tạo SKU duy nhất cho: " + baseSku + " sau 100 lần thử với suffix ngẫu nhiên");
    }

    // Cập nhật sản phẩm với audit trail chi tiết
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    @Transactional
    public SanPham updateProductWithAudit(Long id, SanPham sanPham, String reason, String ipAddress, String userAgent) {
        // Convert to DTO and use inherited update method with audit trail and cache management
        SanPhamDto dto = sanPhamMapper.toDto(sanPham);
        SanPhamDto updatedDto = update(id, dto, "SYSTEM", reason != null ? reason : "Cập nhật thông tin sản phẩm");

        // Convert back to entity for return compatibility
        return sanPhamMapper.toEntity(updatedDto);
    }

    // Xóa mềm sản phẩm (đặt trạng thái thành false)
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    public void softDeleteProduct(Long id) {
        softDeleteProductWithAudit(id, "Xóa mềm sản phẩm", null, null);
    }

    // Xóa mềm sản phẩm với audit trail
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    public void softDeleteProductWithAudit(Long id, String reason, String ipAddress, String userAgent) {
        // Use inherited softDelete method with audit trail and cache management
        softDelete(id, "SYSTEM", reason != null ? reason : "Xóa mềm sản phẩm");
    }

    // Cập nhật trạng thái hàng loạt sản phẩm
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    public BatchOperationResult updateMultipleProductStatus(List<Long> productIds, Boolean trangThai, String lyDoThayDoi) {
        int successCount = 0;
        int failureCount = 0;

        for (Long productId : productIds) {
            try {
                sanPhamRepository.findById(productId).ifPresentOrElse(
                    sanPham -> {
                        // Capture old status for audit
                        Boolean oldStatus = sanPham.getTrangThai();

                        // Update status
                        sanPham.setTrangThai(trangThai);
                        SanPham savedProduct = sanPhamRepository.save(sanPham);

                        // Create audit trail entry for batch status change
                        SanPhamAuditHistory auditEntry = SanPhamAuditHistory.statusChangeEntry(
                            savedProduct.getId(),
                            oldStatus.toString(),
                            trangThai.toString(),
                            savedProduct.getNguoiCapNhat(),
                            lyDoThayDoi != null ? lyDoThayDoi : "Cập nhật trạng thái hàng loạt"
                        );
                        auditHistoryRepository.save(auditEntry);
                    },
                    () -> {
                        throw new RuntimeException("Sản phẩm không tồn tại với ID: " + productId);
                    }
                );
                successCount++;
            } catch (Exception e) {
                failureCount++;
            }
        }

        String message = String.format("Đã cập nhật %d sản phẩm thành công", successCount);
        if (failureCount > 0) {
            message += String.format(", %d sản phẩm thất bại", failureCount);
        }

        return new BatchOperationResult("BATCH_STATUS_UPDATE", successCount, failureCount, message);
    }

    /**
     * Validate product business rules
     * @param sanPham Product to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateProductBusinessRules(SanPham sanPham) {
        if (sanPham.getMaSanPham() != null && !sanPham.getMaSanPham().matches("^SP\\d{3}$")) {
            throw new IllegalArgumentException("Mã sản phẩm phải có định dạng SP + 3 chữ số (ví dụ: SP001)");
        }

        if (sanPham.getTenSanPham() != null && sanPham.getTenSanPham().trim().length() < 3) {
            throw new IllegalArgumentException("Tên sản phẩm phải có ít nhất 3 ký tự");
        }

        if (sanPham.getHinhAnh() != null && sanPham.getHinhAnh().size() > 10) {
            throw new IllegalArgumentException("Sản phẩm không được có quá 10 hình ảnh");
        }
    }

    /**
     * Check if product code is unique
     * @param maSanPham Product code to check
     * @param excludeId ID to exclude from check (for updates)
     * @return true if unique, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isProductCodeUnique(String maSanPham, Long excludeId) {
        List<SanPham> existingProducts = sanPhamRepository.findAll();
        return existingProducts.stream()
                .filter(p -> !p.getId().equals(excludeId))
                .noneMatch(p -> p.getMaSanPham().equals(maSanPham));
    }

    /**
     * Get product statistics
     * @return ProductStatistics object with counts
     */
    @Transactional(readOnly = true)
    public ProductStatistics getProductStatistics() {
        List<SanPham> allProducts = sanPhamRepository.findAll();
        long totalProducts = allProducts.size();
        long activeProducts = allProducts.stream().filter(SanPham::getTrangThai).count();
        long inactiveProducts = totalProducts - activeProducts;

        return new ProductStatistics(totalProducts, activeProducts, inactiveProducts);
    }

    /**
     * Get audit history for a product
     * @param productId Product ID to get audit history for
     * @return List of audit history entries
     */
    @Transactional(readOnly = true)
    public List<SanPhamAuditHistory> getAuditHistory(Long productId) {
        // Use inherited getAuditHistory method from BaseService
        return super.getAuditHistory(productId);
    }

    /**
     * Inner class for product statistics
     */
    public static class ProductStatistics {
        private final long totalProducts;
        private final long activeProducts;
        private final long inactiveProducts;

        public ProductStatistics(long totalProducts, long activeProducts, long inactiveProducts) {
            this.totalProducts = totalProducts;
            this.activeProducts = activeProducts;
            this.inactiveProducts = inactiveProducts;
        }

        public long getTotalProducts() { return totalProducts; }
        public long getActiveProducts() { return activeProducts; }
        public long getInactiveProducts() { return inactiveProducts; }
    }

}
