package com.lapxpert.backend.hoadon.application.controller;
import com.lapxpert.backend.hoadon.domain.service.LichSuHoaDonService;
import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.LichSuHoaDon;
import com.lapxpert.backend.hoadon.domain.repository.LichSuHoaDonRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lich-su-hoa-don")
public class LichSuHoaDonController {

    @Autowired
    private LichSuHoaDonService lichSuHoaDonService;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @GetMapping("/hoa-don/{hoaDonId}")
    public List<LichSuHoaDon> getLichSuByHoaDon(@PathVariable Long hoaDonId) {
        return lichSuHoaDonRepository.findByHoaDon_IdOrderByThoiGianDesc(hoaDonId);
    }


    // Cập nhật trạng thái hóa đơn và ghi vào lịch sử
    @PutMapping("/cap-nhat/{hoaDonId}")
    public ResponseEntity<Void> capNhatTrangThaiHoaDon(
            @PathVariable Long hoaDonId,
            @RequestParam HoaDon.TrangThaiGiaoHang trangThaiMoi,  // Tham số enum TrangThaiGiaoHang
            @RequestParam String mieuTa) {

        try {
            lichSuHoaDonService.addLichSuHoaDon(hoaDonId, trangThaiMoi, mieuTa);
            return ResponseEntity.ok().build(); // Trả về status 200 OK nếu thành công
        } catch (Exception e) {
            return ResponseEntity.status(500).build(); // Trả về lỗi nếu có lỗi xảy ra
        }
    }
}

