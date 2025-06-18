package com.lapxpert.backend.sanpham.service.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.Ram;
import com.lapxpert.backend.sanpham.repository.thuoctinh.RamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RamService extends AttributeCodeGeneratorService<Ram> {
    private final RamRepository ramRepository;

    @Override
    protected JpaRepository<Ram, Long> getRepository() {
        return ramRepository;
    }

    @Override
    protected String getLastCode() {
        return ramRepository.findLastMaRam();
    }

    @Override
    protected String getCodePrefix() {
        return "RAM";
    }

    @Override
    protected int getPrefixLength() {
        return 3;
    }

    @Override
    protected String getEntityTypeName() {
        return "RAM";
    }

    @Override
    protected void setEntityCode(Ram entity, String code) {
        entity.setMaRam(code);
    }

    @Override
    protected String getEntityCode(Ram entity) {
        return entity.getMaRam();
    }

    // Keep the original method name for backward compatibility
    public String generateMaRam() {
        return generateCode();
    }
}