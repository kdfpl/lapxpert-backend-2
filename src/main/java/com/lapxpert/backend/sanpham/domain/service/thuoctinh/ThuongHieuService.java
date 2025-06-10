package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.ThuongHieuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThuongHieuService extends GenericCrudService<ThuongHieu, Long> {
    private final ThuongHieuRepository thuongHieuRepository;

    @Override
    protected JpaRepository<ThuongHieu, Long> getRepository() {
        return thuongHieuRepository;
    }

    public String generateMaThuongHieu() {
        String lastMaThuongHieu = thuongHieuRepository.findLastMaThuongHieu();

        if (lastMaThuongHieu == null) {
            return "TH001";
        }

        try {
            String numberPart = lastMaThuongHieu.substring(2);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã thương hiệu (TH999)");
            }

            return String.format("TH%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã thương hiệu không hợp lệ: " + lastMaThuongHieu);
        }
    }

    @Override
    @Transactional
    public ThuongHieu save(ThuongHieu thuongHieu) {
        if (thuongHieu.getMaThuongHieu() == null || thuongHieu.getMaThuongHieu().trim().isEmpty()) {
            thuongHieu.setMaThuongHieu(generateMaThuongHieu());
        }
        return super.save(thuongHieu);
    }

    @Override
    @Transactional
    public List<ThuongHieu> saveMultiple(List<ThuongHieu> thuongHieus) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String lastMaThuongHieu = thuongHieuRepository.findLastMaThuongHieu();
        int nextNumber = 1;

        if (lastMaThuongHieu != null) {
            try {
                String numberPart = lastMaThuongHieu.substring(2);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Định dạng mã thương hiệu không hợp lệ: " + lastMaThuongHieu);
            }
        }

        for (ThuongHieu thuongHieu : thuongHieus) {
            if (thuongHieu.getMaThuongHieu() == null || thuongHieu.getMaThuongHieu().trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException("Đã đạt đến giới hạn mã thương hiệu (TH999)");
                }
                thuongHieu.setMaThuongHieu(String.format("TH%03d", nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(thuongHieus);
    }
}