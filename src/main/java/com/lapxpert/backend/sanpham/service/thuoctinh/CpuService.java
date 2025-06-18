package com.lapxpert.backend.sanpham.service.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.Cpu;
import com.lapxpert.backend.sanpham.repository.thuoctinh.CpuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CpuService extends AttributeCodeGeneratorService<Cpu> {
    private final CpuRepository cpuRepository;

    @Override
    protected JpaRepository<Cpu, Long> getRepository() {
        return cpuRepository;
    }

    @Override
    protected String getLastCode() {
        return cpuRepository.findLastMaCpu();
    }

    @Override
    protected String getCodePrefix() {
        return "CPU";
    }

    @Override
    protected int getPrefixLength() {
        return 3;
    }

    @Override
    protected String getEntityTypeName() {
        return "CPU";
    }

    @Override
    protected void setEntityCode(Cpu entity, String code) {
        entity.setMaCpu(code);
    }

    @Override
    protected String getEntityCode(Cpu entity) {
        return entity.getMaCpu();
    }

    // Keep the original method name for backward compatibility
    public String generateMaCpu() {
        return generateCode();
    }
}
