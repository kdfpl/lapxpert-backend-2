package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Ram;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.RamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RamService extends GenericCrudService<Ram, Long> {
    private final RamRepository ramRepository;

    @Override
    protected JpaRepository<Ram, Long> getRepository() {
        return ramRepository;
    }

    public String generateMaRam() {
        String lastMaRam = ramRepository.findLastMaRam();

        if (lastMaRam == null) {
            return "RAM001";
        }

        try {
            String numberPart = lastMaRam.substring(3);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã RAM (RAM999)");
            }

            return String.format("RAM%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã RAM không hợp lệ: " + lastMaRam);
        }
    }

    @Override
    @Transactional
    public Ram save(Ram ram) {
        if (ram.getMaRam() == null || ram.getMaRam().trim().isEmpty()) {
            ram.setMaRam(generateMaRam());
        }
        return super.save(ram);
    }

    @Override
    @Transactional
    public List<Ram> saveMultiple(List<Ram> rams) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String lastMaRam = ramRepository.findLastMaRam();
        int nextNumber = 1;

        if (lastMaRam != null) {
            try {
                String numberPart = lastMaRam.substring(3);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Định dạng mã RAM không hợp lệ: " + lastMaRam);
            }
        }

        for (Ram ram : rams) {
            if (ram.getMaRam() == null || ram.getMaRam().trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException("Đã đạt đến giới hạn mã RAM (RAM999)");
                }
                ram.setMaRam(String.format("RAM%03d", nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(rams);
    }
}