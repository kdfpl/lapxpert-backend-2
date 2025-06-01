package com.lapxpert.backend.thongke.domain.service;

import com.lapxpert.backend.thongke.domain.entity.HoaDonSanPhamView;
import com.lapxpert.backend.thongke.domain.repository.ThongKeHDRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import java.util.List;

@Service
public class ThongKeHDService {

    @Autowired
    private ThongKeHDRepository hoaDonRepository;

    public List<HoaDonSanPhamView> getHoaDonsCoSanPhamByTrangThai(String trangThai) {
        if (trangThai == null) {
            return hoaDonRepository.findAllhaveSP();
        } else {
            try {
                TrangThaiDonHang.valueOf(trangThai.toUpperCase());
                return hoaDonRepository.findByTrangThaiDonHang(trangThai);  // Updated method name
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái đơn hàng không hợp lệ: '" + trangThai + "'", e);
            }
        }
    }
}
