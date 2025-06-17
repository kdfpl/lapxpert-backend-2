package com.lapxpert.backend.common.service;

import com.lapxpert.backend.common.util.ValidationUtils;
import com.lapxpert.backend.common.util.ExceptionHandlingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

/**
 * Abstract base service providing common CRUD operations with audit trail integration.
 * Designed for complex business entities that require comprehensive audit tracking,
 * cache management, and Vietnamese business rule validation.
 * 
 * This template serves as the foundation for business entity services like:
 * SanPhamService, HoaDonService, NguoiDungService, PhieuGiamGiaService, DotGiamGiaService
 * 
 * @param <T> Entity type (e.g., SanPham, HoaDon, NguoiDung)
 * @param <ID> Primary key type (typically Long)
 * @param <DTO> Data Transfer Object type for API responses
 * @param <AUDIT> Audit history entity type (e.g., SanPhamAuditHistory)
 */
@Slf4j
public abstract class BaseService<T, ID, DTO, AUDIT> {

    // Abstract methods to be implemented by concrete services
    protected abstract JpaRepository<T, ID> getRepository();
    protected abstract JpaRepository<AUDIT, Long> getAuditRepository();
    protected abstract DTO toDto(T entity);
    protected abstract T toEntity(DTO dto);
    protected abstract String buildAuditJson(T entity);
    protected abstract AUDIT createAuditEntry(ID entityId, String action, String oldValues, String newValues, String nguoiThucHien, String lyDo);
    protected abstract String getEntityName(); // For Vietnamese error messages
    protected abstract void validateEntity(T entity); // Business-specific validation
    protected abstract void evictCache(); // Cache invalidation strategy

    /**
     * Find all entities with DTO conversion
     */
    @Transactional(readOnly = true)
    public List<DTO> findAll() {
        try {
            List<T> entities = getRepository().findAll();
            return entities.stream()
                    .map(this::toDto)
                    .toList();
        } catch (Exception e) {
            log.error("Lỗi khi tìm tất cả {}: {}", getEntityName(), e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                String.format("Không thể tải danh sách %s", getEntityName()), e);
        }
    }

    /**
     * Find entity by ID with DTO conversion
     */
    @Transactional(readOnly = true)
    public Optional<DTO> findById(ID id) {
        try {
            ValidationUtils.validateId(id, getEntityName());
            return getRepository().findById(id)
                    .map(this::toDto);
        } catch (Exception e) {
            log.error("Lỗi khi tìm {} với ID {}: {}", getEntityName(), id, e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                String.format("Không thể tìm %s với ID %s", getEntityName(), id), e);
        }
    }

    /**
     * Create new entity with audit trail
     */
    @Transactional
    public DTO create(DTO dto, String nguoiThucHien, String lyDo) {
        try {
            T entity = toEntity(dto);
            validateEntity(entity);
            
            // Save entity
            T savedEntity = getRepository().save(entity);
            
            // Create audit trail
            String newValues = buildAuditJson(savedEntity);
            AUDIT auditEntry = createAuditEntry(
                getEntityId(savedEntity),
                "CREATE",
                null,
                newValues,
                nguoiThucHien,
                lyDo != null ? lyDo : String.format("Tạo %s mới", getEntityName())
            );
            getAuditRepository().save(auditEntry);
            
            // Evict cache
            evictCache();
            
            log.info("Đã tạo {} mới với ID: {}", getEntityName(), getEntityId(savedEntity));
            return toDto(savedEntity);
            
        } catch (Exception e) {
            log.error("Lỗi khi tạo {}: {}", getEntityName(), e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                String.format("Không thể tạo %s mới", getEntityName()), e);
        }
    }

    /**
     * Update existing entity with audit trail
     */
    @Transactional
    public DTO update(ID id, DTO dto, String nguoiThucHien, String lyDo) {
        try {
            ValidationUtils.validateId(id, getEntityName());
            
            T existingEntity = getRepository().findById(id)
                    .orElseThrow(() -> ExceptionHandlingUtils.createNotFoundException(
                        String.format("%s không tồn tại với ID: %s", getEntityName(), id)));
            
            // Capture old values for audit
            String oldValues = buildAuditJson(existingEntity);
            
            // Update entity
            T updatedEntity = toEntity(dto);
            setEntityId(updatedEntity, id);
            validateEntity(updatedEntity);
            
            // Save updated entity
            T savedEntity = getRepository().save(updatedEntity);
            
            // Create audit trail
            String newValues = buildAuditJson(savedEntity);
            AUDIT auditEntry = createAuditEntry(
                id,
                "UPDATE",
                oldValues,
                newValues,
                nguoiThucHien,
                lyDo != null ? lyDo : String.format("Cập nhật thông tin %s", getEntityName())
            );
            getAuditRepository().save(auditEntry);
            
            // Evict cache
            evictCache();
            
            log.info("Đã cập nhật {} với ID: {}", getEntityName(), id);
            return toDto(savedEntity);
            
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật {} với ID {}: {}", getEntityName(), id, e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                String.format("Không thể cập nhật %s với ID %s", getEntityName(), id), e);
        }
    }

    /**
     * Soft delete entity with audit trail
     */
    @Transactional
    public void softDelete(ID id, String nguoiThucHien, String lyDo) {
        try {
            ValidationUtils.validateId(id, getEntityName());
            
            T entity = getRepository().findById(id)
                    .orElseThrow(() -> ExceptionHandlingUtils.createNotFoundException(
                        String.format("%s không tồn tại với ID: %s", getEntityName(), id)));
            
            // Capture old values for audit
            String oldValues = buildAuditJson(entity);
            
            // Perform soft delete (set status to false)
            setSoftDeleteStatus(entity, false);
            getRepository().save(entity);
            
            // Create audit trail
            AUDIT auditEntry = createAuditEntry(
                id,
                "SOFT_DELETE",
                oldValues,
                null,
                nguoiThucHien,
                lyDo != null ? lyDo : String.format("Xóa mềm %s", getEntityName())
            );
            getAuditRepository().save(auditEntry);
            
            // Evict cache
            evictCache();
            
            log.info("Đã xóa mềm {} với ID: {}", getEntityName(), id);
            
        } catch (Exception e) {
            log.error("Lỗi khi xóa mềm {} với ID {}: {}", getEntityName(), id, e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                String.format("Không thể xóa %s với ID %s", getEntityName(), id), e);
        }
    }

    /**
     * Batch create entities with audit trail
     */
    @Transactional
    public List<DTO> createBatch(List<DTO> dtos, String nguoiThucHien, String lyDo) {
        try {
            List<T> entities = dtos.stream()
                    .map(this::toEntity)
                    .toList();

            // Validate all entities
            entities.forEach(this::validateEntity);

            // Save all entities
            List<T> savedEntities = getRepository().saveAll(entities);

            // Create audit trails for all entities
            List<AUDIT> auditEntries = savedEntities.stream()
                    .map(entity -> {
                        String newValues = buildAuditJson(entity);
                        return createAuditEntry(
                            getEntityId(entity),
                            "BATCH_CREATE",
                            null,
                            newValues,
                            nguoiThucHien,
                            lyDo != null ? lyDo : String.format("Tạo hàng loạt %s", getEntityName())
                        );
                    })
                    .toList();
            getAuditRepository().saveAll(auditEntries);

            // Evict cache
            evictCache();

            log.info("Đã tạo hàng loạt {} {} thực thể", savedEntities.size(), getEntityName());
            return savedEntities.stream()
                    .map(this::toDto)
                    .toList();

        } catch (Exception e) {
            log.error("Lỗi khi tạo hàng loạt {}: {}", getEntityName(), e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                String.format("Không thể tạo hàng loạt %s", getEntityName()), e);
        }
    }

    /**
     * Get audit history for an entity
     */
    @Transactional(readOnly = true)
    public List<AUDIT> getAuditHistory(ID entityId) {
        try {
            ValidationUtils.validateId(entityId, getEntityName());
            return getAuditHistoryByEntityId(entityId);
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử audit cho {} với ID {}: {}", getEntityName(), entityId, e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                String.format("Không thể lấy lịch sử audit cho %s với ID %s", getEntityName(), entityId), e);
        }
    }

    /**
     * Check if entity exists
     */
    @Transactional(readOnly = true)
    public boolean exists(ID id) {
        try {
            ValidationUtils.validateId(id, getEntityName());
            return getRepository().existsById(id);
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra tồn tại {} với ID {}: {}", getEntityName(), id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Count total entities
     */
    @Transactional(readOnly = true)
    public long count() {
        try {
            return getRepository().count();
        } catch (Exception e) {
            log.error("Lỗi khi đếm tổng số {}: {}", getEntityName(), e.getMessage(), e);
            throw ExceptionHandlingUtils.createBusinessException(
                String.format("Không thể đếm tổng số %s", getEntityName()), e);
        }
    }

    // Abstract methods for entity-specific operations
    protected abstract ID getEntityId(T entity);
    protected abstract void setEntityId(T entity, ID id);
    protected abstract void setSoftDeleteStatus(T entity, boolean status);
    protected abstract List<AUDIT> getAuditHistoryByEntityId(ID entityId);
}
