package com.lapxpert.backend.sanpham.service.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.Gpu;
import com.lapxpert.backend.sanpham.repository.thuoctinh.GpuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GpuService extends AttributeCodeGeneratorService<Gpu> {
    private final GpuRepository gpuRepository;

    @Override
    protected JpaRepository<Gpu, Long> getRepository() {
        return gpuRepository;
    }

    @Override
    protected String getLastCode() {
        return gpuRepository.findLastMaGpu();
    }

    @Override
    protected String getCodePrefix() {
        return "GPU";
    }

    @Override
    protected int getPrefixLength() {
        return 3;
    }

    @Override
    protected String getEntityTypeName() {
        return "GPU";
    }

    @Override
    protected void setEntityCode(Gpu entity, String code) {
        entity.setMaGpu(code);
    }

    @Override
    protected String getEntityCode(Gpu entity) {
        return entity.getMaGpu();
    }

    // Keep the original method name for backward compatibility
    public String generateMaGpu() {
        return generateCode();
    }
}