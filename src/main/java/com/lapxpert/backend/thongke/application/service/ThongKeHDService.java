package com.lapxpert.backend.thongke.application.service;


import com.lapxpert.backend.hoadon.enity.HoaDon;
import com.lapxpert.backend.thongke.application.enity.HoaDonSanPhamView;
import com.lapxpert.backend.thongke.application.repository.ThongKeHDRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                HoaDon.TrangThaiGiaoHang trangThaiEnum = HoaDon.TrangThaiGiaoHang.valueOf(trangThai);
                return hoaDonRepository.findByTrangThaiGiaoHang(trangThai);  // Lọc theo trạng thái
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái giao hàng không hợp lệ", e);
            }
        }
    }

    }


