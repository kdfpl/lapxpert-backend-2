package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.DanhMucRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DanhMucService extends GenericCrudService<DanhMuc, Long> {
    private final DanhMucRepository danhMucRepository;

    @Override
    protected JpaRepository<DanhMuc, Long> getRepository() {
        return danhMucRepository;
    }

    public String generateMaDanhMuc() {
        String lastMaDanhMuc = danhMucRepository.findLastMaDanhMuc();

        if (lastMaDanhMuc == null) {
            return "DM001";
        }

        try {
            String numberPart = lastMaDanhMuc.substring(2);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã danh mục (DM999)");
            }

            return String.format("DM%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã danh mục không hợp lệ: " + lastMaDanhMuc);
        }
    }

    @Override
    @Transactional
    public DanhMuc save(DanhMuc danhMuc) {
        if (danhMuc.getMaDanhMuc() == null || danhMuc.getMaDanhMuc().trim().isEmpty()) {
            danhMuc.setMaDanhMuc(generateMaDanhMuc());
        }
        return super.save(danhMuc);
    }

    @Override
    @Transactional
    public List<DanhMuc> saveMultiple(List<DanhMuc> danhMucs) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String lastMaDanhMuc = danhMucRepository.findLastMaDanhMuc();
        int nextNumber = 1;

        if (lastMaDanhMuc != null) {
            try {
                String numberPart = lastMaDanhMuc.substring(2);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Định dạng mã danh mục không hợp lệ: " + lastMaDanhMuc);
            }
        }

        for (DanhMuc danhMuc : danhMucs) {
            if (danhMuc.getMaDanhMuc() == null || danhMuc.getMaDanhMuc().trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException("Đã đạt đến giới hạn mã danh mục (DM999)");
                }
                danhMuc.setMaDanhMuc(String.format("DM%03d", nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(danhMucs);
    }
}