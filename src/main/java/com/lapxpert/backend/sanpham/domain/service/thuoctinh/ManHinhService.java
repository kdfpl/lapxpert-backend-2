package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ManHinh;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.ManHinhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManHinhService extends GenericCrudService<ManHinh, Long> {
    private final ManHinhRepository manHinhRepository;

    @Override
    protected JpaRepository<ManHinh, Long> getRepository() {
        return manHinhRepository;
    }

    public String generateMaManHinh() {
        String lastMaManHinh = manHinhRepository.findLastMaManHinh();

        if (lastMaManHinh == null) {
            return "MH001";
        }

        try {
            String numberPart = lastMaManHinh.substring(2);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã màn hình (MH999)");
            }

            return String.format("MH%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã màn hình không hợp lệ: " + lastMaManHinh);
        }
    }

    @Override
    @Transactional
    public ManHinh save(ManHinh manHinh) {
        if (manHinh.getMaManHinh() == null || manHinh.getMaManHinh().trim().isEmpty()) {
            manHinh.setMaManHinh(generateMaManHinh());
        }
        return super.save(manHinh);
    }

    @Override
    @Transactional
    public List<ManHinh> saveMultiple(List<ManHinh> manHinhs) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String lastMaManHinh = manHinhRepository.findLastMaManHinh();
        int nextNumber = 1;

        if (lastMaManHinh != null) {
            try {
                String numberPart = lastMaManHinh.substring(2);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Định dạng mã màn hình không hợp lệ: " + lastMaManHinh);
            }
        }

        for (ManHinh manHinh : manHinhs) {
            if (manHinh.getMaManHinh() == null || manHinh.getMaManHinh().trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException("Đã đạt đến giới hạn mã màn hình (MH999)");
                }
                manHinh.setMaManHinh(String.format("MH%03d", nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(manHinhs);
    }
}