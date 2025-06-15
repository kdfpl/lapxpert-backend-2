package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.BoNho;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.BoNhoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoNhoService extends AttributeCodeGeneratorService<BoNho> {
    private final BoNhoRepository boNhoRepository;

    @Override
    protected JpaRepository<BoNho, Long> getRepository() {
        return boNhoRepository;
    }

    @Override
    protected String getLastCode() {
        return boNhoRepository.findLastMaBoNho();
    }

    @Override
    protected String getCodePrefix() {
        return "BN";
    }

    @Override
    protected int getPrefixLength() {
        return 2;
    }

    @Override
    protected String getEntityTypeName() {
        return "bộ nhớ";
    }

    @Override
    protected void setEntityCode(BoNho entity, String code) {
        entity.setMaBoNho(code);
    }

    @Override
    protected String getEntityCode(BoNho entity) {
        return entity.getMaBoNho();
    }

    // Keep the original method name for backward compatibility
    public String generateMaBoNho() {
        return generateCode();
    }
}