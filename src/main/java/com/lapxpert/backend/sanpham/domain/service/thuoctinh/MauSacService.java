package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.MauSac;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.MauSacRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MauSacService extends GenericCrudService<MauSac, Long> {
    private final MauSacRepository mauSacRepository;

    @Override
    protected JpaRepository<MauSac, Long> getRepository() {
        return mauSacRepository;
    }

    public String generateMaMauSac() {
        String lastMaMauSac = mauSacRepository.findLastMaMauSac();

        if (lastMaMauSac == null) {
            return "MS001";
        }

        try {
            String numberPart = lastMaMauSac.substring(2);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã màu sắc (MS999)");
            }

            return String.format("MS%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã màu sắc không hợp lệ: " + lastMaMauSac);
        }
    }

    @Override
    @Transactional
    public MauSac save(MauSac mauSac) {
        if (mauSac.getMaMauSac() == null || mauSac.getMaMauSac().trim().isEmpty()) {
            mauSac.setMaMauSac(generateMaMauSac());
        }
        return super.save(mauSac);
    }

    @Override
    @Transactional
    public List<MauSac> saveMultiple(List<MauSac> mauSacs) {
        // Generate sequential codes for bulk creation to avoid duplicates
        String lastMaMauSac = mauSacRepository.findLastMaMauSac();
        int nextNumber = 1;

        if (lastMaMauSac != null) {
            try {
                String numberPart = lastMaMauSac.substring(2);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Định dạng mã màu sắc không hợp lệ: " + lastMaMauSac);
            }
        }

        for (MauSac mauSac : mauSacs) {
            if (mauSac.getMaMauSac() == null || mauSac.getMaMauSac().trim().isEmpty()) {
                if (nextNumber > 999) {
                    throw new RuntimeException("Đã đạt đến giới hạn mã màu sắc (MS999)");
                }
                mauSac.setMaMauSac(String.format("MS%03d", nextNumber));
                nextNumber++;
            }
        }
        return super.saveMultiple(mauSacs);
    }
}
