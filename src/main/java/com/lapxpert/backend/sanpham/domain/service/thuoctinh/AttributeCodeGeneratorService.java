package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Abstract base class for attribute services that require sequential code generation
 * Implements template method pattern to eliminate code duplication across attribute services
 * Preserves Vietnamese naming conventions and audit trail functionality
 * 
 * @param <T> The entity type (Cpu, Ram, Gpu, MauSac, ThuongHieu, BoNho, ManHinh, DanhMuc)
 */
public abstract class AttributeCodeGeneratorService<T> extends GenericCrudService<T, Long> {
    
    // Abstract methods to be implemented by concrete services
    protected abstract String getLastCode();
    protected abstract String getCodePrefix();
    protected abstract int getPrefixLength();
    protected abstract String getEntityTypeName(); // For Vietnamese error messages
    protected abstract void setEntityCode(T entity, String code);
    protected abstract String getEntityCode(T entity);
    
    /**
     * Template method for generating sequential attribute codes
     * Implements common logic while allowing customization through abstract methods
     */
    protected String generateCode() {
        String prefix = getCodePrefix();
        int prefixLength = getPrefixLength();
        String lastCode = getLastCode();
        
        if (lastCode == null) {
            return String.format("%s%03d", prefix, 1);
        }
        
        try {
            String numberPart = lastCode.substring(prefixLength);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;
            
            if (nextNumber > 999) {
                throw new RuntimeException(String.format("Đã đạt đến giới hạn mã %s (%s999)", 
                    getEntityTypeName(), prefix));
            }
            
            return String.format("%s%03d", prefix, nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException(String.format("Định dạng mã %s không hợp lệ: %s", 
                getEntityTypeName(), lastCode));
        }
    }
    
    @Override
    @Transactional
    public T save(T entity) {
        if (getEntityCode(entity) == null || getEntityCode(entity).trim().isEmpty()) {
            setEntityCode(entity, generateCode());
        }
        return super.save(entity);
    }
    
    @Override
    @Transactional
    public List<T> saveMultiple(List<T> entities) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String prefix = getCodePrefix();
        int prefixLength = getPrefixLength();
        String lastCode = getLastCode();
        int nextNumber = 1;
        
        if (lastCode != null) {
            try {
                String numberPart = lastCode.substring(prefixLength);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException(String.format("Định dạng mã %s không hợp lệ: %s", 
                    getEntityTypeName(), lastCode));
            }
        }
        
        for (T entity : entities) {
            if (getEntityCode(entity) == null || getEntityCode(entity).trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException(String.format("Đã đạt đến giới hạn mã %s (%s999)", 
                        getEntityTypeName(), prefix));
                }
                setEntityCode(entity, String.format("%s%03d", prefix, nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(entities);
    }
}
