package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.sanpham.application.dto.SanPhamDto;
import com.lapxpert.backend.sanpham.application.mapper.SanPhamMapper;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SanPhamService {
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamMapper sanPhamMapper;

    public SanPhamService(SanPhamRepository sanPhamRepository, SanPhamMapper sanPhamMapper) {
        this.sanPhamRepository = sanPhamRepository;
        this.sanPhamMapper = sanPhamMapper;
    }

    //Hàm cho đợt giảm giá
    @Transactional
    public List<SanPhamDto> findAll() {
        List<SanPham> entities = sanPhamRepository.findAll();
        return sanPhamMapper.toDtos(entities);
    }

    // Lấy danh sách sản phẩm có trạng thái = true
    @Transactional(readOnly = true)
    public List<SanPhamDto> getActiveProducts() {
        List<SanPham> entities = sanPhamRepository.findAllByTrangThai(true);
        return sanPhamMapper.toDtos(entities);
    }

    // Thêm sản phẩm mới
    @Transactional
    public SanPham addProduct(SanPham sanPham) {
        return sanPhamRepository.save(sanPham);
    }

    // Cập nhật sản phẩm
    @Transactional
    public SanPham updateProduct(Long id, SanPham sanPham) {
        return sanPhamRepository.findById(id).map(existing -> {
            existing.setMaSanPham(sanPham.getMaSanPham());
            existing.setTenSanPham(sanPham.getTenSanPham());
            existing.setThuongHieu(sanPham.getThuongHieu());
            existing.setMoTa(sanPham.getMoTa());
            existing.setHinhAnh(sanPham.getHinhAnh());
            existing.setNgayRaMat(sanPham.getNgayRaMat());
            return sanPhamRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
    }

    // Xóa mềm sản phẩm (đặt trạng thái thành false)
    @Transactional
    public void softDeleteProduct(Long id) {
        sanPhamRepository.findById(id).ifPresent(sanPham -> {
            sanPham.setTrangThai(false);
            sanPhamRepository.save(sanPham);
        });
    }

}
