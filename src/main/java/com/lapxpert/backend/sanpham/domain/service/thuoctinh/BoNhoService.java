package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.BoNho;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.BoNhoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoNhoService extends GenericCrudService<BoNho, Long> {
    private final BoNhoRepository boNhoRepository;

    @Override
    protected JpaRepository<BoNho, Long> getRepository() {
        return boNhoRepository;
    }

    public String generateMaBoNho() {
        String lastMaBoNho = boNhoRepository.findLastMaBoNho();

        if (lastMaBoNho == null) {
            return "BN001";
        }

        try {
            String numberPart = lastMaBoNho.substring(2);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã bộ nhớ (BN999)");
            }

            return String.format("BN%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã bộ nhớ không hợp lệ: " + lastMaBoNho);
        }
    }

    @Override
    @Transactional
    public BoNho save(BoNho boNho) {
        if (boNho.getMaBoNho() == null || boNho.getMaBoNho().trim().isEmpty()) {
            boNho.setMaBoNho(generateMaBoNho());
        }
        return super.save(boNho);
    }

    @Override
    @Transactional
    public List<BoNho> saveMultiple(List<BoNho> boNhos) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String lastMaBoNho = boNhoRepository.findLastMaBoNho();
        int nextNumber = 1;

        if (lastMaBoNho != null) {
            try {
                String numberPart = lastMaBoNho.substring(2);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Định dạng mã bộ nhớ không hợp lệ: " + lastMaBoNho);
            }
        }

        for (BoNho boNho : boNhos) {
            if (boNho.getMaBoNho() == null || boNho.getMaBoNho().trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException("Đã đạt đến giới hạn mã bộ nhớ (BN999)");
                }
                boNho.setMaBoNho(String.format("BN%03d", nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(boNhos);
    }
}