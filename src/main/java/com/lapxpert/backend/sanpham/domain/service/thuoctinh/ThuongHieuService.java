package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.ThuongHieuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThuongHieuService extends AttributeCodeGeneratorService<ThuongHieu> {
    private final ThuongHieuRepository thuongHieuRepository;

    @Override
    protected JpaRepository<ThuongHieu, Long> getRepository() {
        return thuongHieuRepository;
    }

    @Override
    protected String getLastCode() {
        return thuongHieuRepository.findLastMaThuongHieu();
    }

    @Override
    protected String getCodePrefix() {
        return "TH";
    }

    @Override
    protected int getPrefixLength() {
        return 2;
    }

    @Override
    protected String getEntityTypeName() {
        return "thương hiệu";
    }

    @Override
    protected void setEntityCode(ThuongHieu entity, String code) {
        entity.setMaThuongHieu(code);
    }

    @Override
    protected String getEntityCode(ThuongHieu entity) {
        return entity.getMaThuongHieu();
    }

    // Keep the original method name for backward compatibility
    public String generateMaThuongHieu() {
        return generateCode();
    }
}