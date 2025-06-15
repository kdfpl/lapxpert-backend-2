package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ManHinh;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.ManHinhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManHinhService extends AttributeCodeGeneratorService<ManHinh> {
    private final ManHinhRepository manHinhRepository;

    @Override
    protected JpaRepository<ManHinh, Long> getRepository() {
        return manHinhRepository;
    }

    @Override
    protected String getLastCode() {
        return manHinhRepository.findLastMaManHinh();
    }

    @Override
    protected String getCodePrefix() {
        return "MH";
    }

    @Override
    protected int getPrefixLength() {
        return 2;
    }

    @Override
    protected String getEntityTypeName() {
        return "màn hình";
    }

    @Override
    protected void setEntityCode(ManHinh entity, String code) {
        entity.setMaManHinh(code);
    }

    @Override
    protected String getEntityCode(ManHinh entity) {
        return entity.getMaManHinh();
    }

    // Keep the original method name for backward compatibility
    public String generateMaManHinh() {
        return generateCode();
    }
}