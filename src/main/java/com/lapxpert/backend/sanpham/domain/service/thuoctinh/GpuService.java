package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Gpu;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.GpuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GpuService extends GenericCrudService<Gpu, Long> {
    private final GpuRepository gpuRepository;

    @Override
    protected JpaRepository<Gpu, Long> getRepository() {
        return gpuRepository;
    }

    public String generateMaGpu() {
        String lastMaGpu = gpuRepository.findLastMaGpu();

        if (lastMaGpu == null) {
            return "GPU001";
        }

        try {
            String numberPart = lastMaGpu.substring(3);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã GPU (GPU999)");
            }

            return String.format("GPU%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã GPU không hợp lệ: " + lastMaGpu);
        }
    }

    @Override
    @Transactional
    public Gpu save(Gpu gpu) {
        if (gpu.getMaGpu() == null || gpu.getMaGpu().trim().isEmpty()) {
            gpu.setMaGpu(generateMaGpu());
        }
        return super.save(gpu);
    }

    @Override
    @Transactional
    public List<Gpu> saveMultiple(List<Gpu> gpus) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String lastMaGpu = gpuRepository.findLastMaGpu();
        int nextNumber = 1;

        if (lastMaGpu != null) {
            try {
                String numberPart = lastMaGpu.substring(3);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Định dạng mã GPU không hợp lệ: " + lastMaGpu);
            }
        }

        for (Gpu gpu : gpus) {
            if (gpu.getMaGpu() == null || gpu.getMaGpu().trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException("Đã đạt đến giới hạn mã GPU (GPU999)");
                }
                gpu.setMaGpu(String.format("GPU%03d", nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(gpus);
    }
}