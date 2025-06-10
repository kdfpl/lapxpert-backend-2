package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Cpu;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.CpuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CpuService extends GenericCrudService<Cpu, Long> {
    private final CpuRepository cpuRepository;

    @Override
    protected JpaRepository<Cpu, Long> getRepository() {
        return cpuRepository;
    }

    public String generateMaCpu() {
        String lastMaCpu = cpuRepository.findLastMaCpu();

        if (lastMaCpu == null) {
            return "CPU001";
        }

        try {
            String numberPart = lastMaCpu.substring(3);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã CPU (CPU999)");
            }

            return String.format("CPU%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã CPU không hợp lệ: " + lastMaCpu);
        }
    }

    @Override
    @Transactional
    public Cpu save(Cpu cpu) {
        if (cpu.getMaCpu() == null || cpu.getMaCpu().trim().isEmpty()) {
            cpu.setMaCpu(generateMaCpu());
        }
        return super.save(cpu);
    }

    @Override
    @Transactional
    public List<Cpu> saveMultiple(List<Cpu> cpus) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String lastMaCpu = cpuRepository.findLastMaCpu();
        int nextNumber = 1;

        if (lastMaCpu != null) {
            try {
                String numberPart = lastMaCpu.substring(3);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Định dạng mã CPU không hợp lệ: " + lastMaCpu);
            }
        }

        for (Cpu cpu : cpus) {
            if (cpu.getMaCpu() == null || cpu.getMaCpu().trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException("Đã đạt đến giới hạn mã CPU (CPU999)");
                }
                cpu.setMaCpu(String.format("CPU%03d", nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(cpus);
    }
}
