package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.application.mapper.SanPhamChiTietMapper;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SanPhamChiTietService {
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamChiTietMapper sanPhamChiTietMapper;

    // Lấy danh sách sản phẩm có trạng thái = true
    @Transactional(readOnly = true)
    public List<SanPhamChiTietDto> getActiveProducts() {
        List<SanPhamChiTiet> entities = sanPhamChiTietRepository.findAllByTrangThai(true);
        return sanPhamChiTietMapper.toDtos(entities);
    }

    // Thêm sản phẩm mới
    @Transactional
    public SanPhamChiTiet addProduct(SanPhamChiTiet sanPham) {
        return sanPhamChiTietRepository.save(sanPham);
    }

    // Cập nhật sản phẩm
    @Transactional
    public SanPhamChiTiet updateProduct(Long id, SanPhamChiTiet sanPham) {
        return sanPhamChiTietRepository.findById(id).map(existing -> {
            existing.setSanPham(sanPham.getSanPham());
            existing.setSku(sanPham.getSku());
            existing.setMauSac(sanPham.getMauSac());
            existing.setSoLuongTonKho(sanPham.getSoLuongTonKho());
            existing.setGiaBan(sanPham.getGiaBan());
            existing.setHinhAnh(sanPham.getHinhAnh());
            existing.setNgayTao(sanPham.getNgayTao());
            existing.setCpu(sanPham.getCpu());
            existing.setRam(sanPham.getRam());
            existing.setOCung(sanPham.getOCung());
            existing.setGpu(sanPham.getGpu());
            existing.setManHinh(sanPham.getManHinh());
            existing.setCongGiaoTiep(sanPham.getCongGiaoTiep());
            existing.setBanPhim(sanPham.getBanPhim());
            existing.setKetNoiMang(sanPham.getKetNoiMang());
            existing.setAmThanh(sanPham.getAmThanh());
            existing.setWebcam(sanPham.getWebcam());
            existing.setBaoMat(sanPham.getBaoMat());
            existing.setHeDieuHanh(sanPham.getHeDieuHanh());
            existing.setPin(sanPham.getPin());
            existing.setThietKe(sanPham.getThietKe());
            return sanPhamChiTietRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Sản phẩm chi tiết không tồn tại"));
    }

    // Xóa mềm sản phẩm (đặt trạng thái thành false)
    @Transactional
    public void softDeleteProduct(Long id) {
        sanPhamChiTietRepository.findById(id).ifPresent(sanPham -> {
            sanPham.setTrangThai(false);
            sanPhamChiTietRepository.save(sanPham);
        });
    }

}
