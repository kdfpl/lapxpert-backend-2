package com.lapxpert.backend.hoadon.controller;


import com.lapxpert.backend.hoadon.enity.HoaDon;
import com.lapxpert.backend.hoadon.service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/hoa-don")
public class HoaDonController {

    @Autowired
    private HoaDonService hoaDonService;

    // Lấy tất cả hóa đơn hoặc lọc theo trạng thái giao hàng
    @GetMapping
    public List<HoaDon> getAllHoaDon(@RequestParam(value = "trangThai", required = false) String trangThai) {
        return hoaDonService.getHoaDonsByTrangThai(trangThai);
    }

    // Thêm mới hóa đơn
    @PostMapping("/add")
    public HoaDon addHoaDon(@RequestBody HoaDon hoaDon) {
        return hoaDonService.createHoaDon(hoaDon);
    }

    // Lấy hóa đơn theo ID
    @GetMapping("/{id}")
    public HoaDon getHoaDonById(@PathVariable Long id) {
        return hoaDonService.getHoaDonById(id);
    }

    // Cập nhật hóa đơn
    @PutMapping("/{id}")
    public HoaDon updateHoaDon(@PathVariable Long id, @RequestBody HoaDon hoaDon) {
        return hoaDonService.updateHoaDon(id, hoaDon);
    }
}
