package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.DanhMucRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DanhMucService extends AttributeCodeGeneratorService<DanhMuc> {
    private final DanhMucRepository danhMucRepository;

    @Override
    protected JpaRepository<DanhMuc, Long> getRepository() {
        return danhMucRepository;
    }

    @Override
    protected String getLastCode() {
        return danhMucRepository.findLastMaDanhMuc();
    }

    @Override
    protected String getCodePrefix() {
        return "DM";
    }

    @Override
    protected int getPrefixLength() {
        return 2;
    }

    @Override
    protected String getEntityTypeName() {
        return "danh mục";
    }

    @Override
    protected void setEntityCode(DanhMuc entity, String code) {
        entity.setMaDanhMuc(code);
    }

    @Override
    protected String getEntityCode(DanhMuc entity) {
        return entity.getMaDanhMuc();
    }

    // Keep the original method name for backward compatibility
    public String generateMaDanhMuc() {
        return generateCode();
    }
}