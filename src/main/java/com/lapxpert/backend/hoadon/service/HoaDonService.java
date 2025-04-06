package com.lapxpert.backend.hoadon.service;


import com.lapxpert.backend.hoadon.enity.HoaDon;
import com.lapxpert.backend.hoadon.repository.HoaDonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    // Lấy danh sách hóa đơn theo trạng thái
    public List<HoaDon> getHoaDonsByTrangThai(String trangThai) {
        if (trangThai == null) {
            return hoaDonRepository.findAll();  // Nếu không có trạng thái thì trả tất cả hóa đơn
        } else {
            try {
                // Chuyển đổi chuỗi trạng thái thành enum
                HoaDon.TrangThaiGiaoHang trangThaiEnum = HoaDon.TrangThaiGiaoHang.valueOf(trangThai);
                return hoaDonRepository.findByTrangThaiGiaoHang(trangThaiEnum);  // Lọc theo trạng thái
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái giao hàng không hợp lệ", e);
            }
        }
    }

    // Thêm mới hóa đơn
    public HoaDon createHoaDon(HoaDon hoaDon) {
        return hoaDonRepository.save(hoaDon);
    }

    // Lấy hóa đơn theo ID
    public HoaDon getHoaDonById(Long id) {
        return hoaDonRepository.findById(id).orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));
    }

    // Cập nhật hóa đơn
    public HoaDon updateHoaDon(Long id, HoaDon hoaDon) {
        hoaDon.setId(id);  // Đảm bảo set lại id trước khi cập nhật
        return hoaDonRepository.save(hoaDon);
    }
}
