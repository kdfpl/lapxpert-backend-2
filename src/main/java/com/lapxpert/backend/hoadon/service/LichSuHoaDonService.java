package com.lapxpert.backend.hoadon.service;
import com.lapxpert.backend.hoadon.enity.HoaDon;
import com.lapxpert.backend.hoadon.enity.LichSuHoaDon;
import com.lapxpert.backend.hoadon.repository.HoaDonRepository;
import com.lapxpert.backend.hoadon.repository.LichSuHoaDonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LichSuHoaDonService {

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Transactional
    public void addLichSuHoaDon(Long hoaDonId, HoaDon.TrangThaiGiaoHang trangThaiMoi, String mieuTa) {
        // Kiểm tra tồn tại hóa đơn
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId).orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));

        // Thêm bản ghi vào bảng LichSuHoaDon
        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(trangThaiMoi);
        lichSu.setMieuTa(mieuTa);
        lichSu.setThoiGian(LocalDateTime.now());

        lichSuHoaDonRepository.save(lichSu);
    }
}
