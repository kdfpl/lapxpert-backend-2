package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.MauSac;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.MauSacRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MauSacService extends AttributeCodeGeneratorService<MauSac> {
    private final MauSacRepository mauSacRepository;

    @Override
    protected JpaRepository<MauSac, Long> getRepository() {
        return mauSacRepository;
    }

    @Override
    protected String getLastCode() {
        return mauSacRepository.findLastMaMauSac();
    }

    @Override
    protected String getCodePrefix() {
        return "MS";
    }

    @Override
    protected int getPrefixLength() {
        return 2;
    }

    @Override
    protected String getEntityTypeName() {
        return "màu sắc";
    }

    @Override
    protected void setEntityCode(MauSac entity, String code) {
        entity.setMaMauSac(code);
    }

    @Override
    protected String getEntityCode(MauSac entity) {
        return entity.getMaMauSac();
    }

    // Keep the original method name for backward compatibility
    public String generateMaMauSac() {
        return generateCode();
    }
}
