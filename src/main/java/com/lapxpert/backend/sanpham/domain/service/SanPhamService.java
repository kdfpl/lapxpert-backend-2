package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.sanpham.application.dto.SanPhamDto;
import com.lapxpert.backend.sanpham.application.mapper.SanPhamChiTietMapper;
import com.lapxpert.backend.sanpham.application.mapper.SanPhamMapper;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamRepository;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.ThuongHieuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SanPhamService {
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamMapper sanPhamMapper;

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamChiTietMapper sanPhamChiTietMapper;

    public String generateMaSanPham() {
        String lastMaSanPham = sanPhamRepository.findLastMaSanPham();

        if (lastMaSanPham == null) {
            return "SP001";
        }

        try {
            String numberPart = lastMaSanPham.substring(2);
            int lastNumber = Integer.parseInt(numberPart);
            int nextNumber = lastNumber + 1;

            if (nextNumber > 999) {
                throw new RuntimeException("Đã đạt đến giới hạn mã sản phẩm (SP999)");
            }

            return String.format("SP%03d", nextNumber);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Định dạng mã sản phẩm không hợp lệ: " + lastMaSanPham);
        }
    }

    @CacheEvict(value = {"sanPhamList", "activeSanPhamList"}, allEntries = true)
    @Transactional
    public SanPhamDto createSanPhamWithChiTiet(SanPhamDto sanPhamDto) {
        // Chuyển đổi DTO sang Entity
        SanPham sanPham = sanPhamMapper.toEntity(sanPhamDto);
        sanPham.setMaSanPham(generateMaSanPham());
        // Lưu sản phẩm chính trước
        SanPham savedSanPham = sanPhamRepository.save(sanPham);

        // Xử lý danh sách sản phẩm chi tiết
        if (sanPhamDto.getSanPhamChiTiets() != null && !sanPhamDto.getSanPhamChiTiets().isEmpty()) {
            Set<SanPhamChiTiet> chiTiets = sanPhamDto.getSanPhamChiTiets().stream()
                    .map(dto -> {
                        SanPhamChiTiet chiTiet = sanPhamChiTietMapper.toEntity(dto);
                        chiTiet.setSku(savedSanPham.getMaSanPham()+chiTiet.getSku());
                        chiTiet.setSanPham(savedSanPham);
                        return chiTiet;
                    })
                    .collect(Collectors.toSet());

            // Lưu danh sách sản phẩm chi tiết
            Set<SanPhamChiTiet> savedChiTiets = sanPhamChiTietRepository.saveAll(chiTiets)
                    .stream()
                    .collect(Collectors.toSet());

            savedSanPham.setSanPhamChiTiets(savedChiTiets);
        }

        return sanPhamMapper.toDto(savedSanPham);
    }

//    @Cacheable(value = "sanPhamList")
    @Transactional(readOnly = true)
    public List<SanPhamDto> findAll() {
        List<SanPham> entities = sanPhamRepository.findAll();
        return sanPhamMapper.toDtos(entities);
    }

//    @Cacheable(value = "activeSanPhamList")
    @Transactional(readOnly = true)
    public List<SanPhamDto> getActiveProducts() {
        List<SanPham> entities = sanPhamRepository.findAllByTrangThai(true);
        return sanPhamMapper.toDtos(entities);
    }

    // Thêm sản phẩm mới
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    @Transactional
    public SanPham addProduct(SanPham sanPham) {
        sanPham.setTrangThai(true);
        return sanPhamRepository.save(sanPham);
    }

    // Cập nhật sản phẩm
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(value = "sanPhamList", allEntries = true),
            @CacheEvict(value = "activeSanPhamList", allEntries = true)
    })
    public void softDeleteProduct(Long id) {
        sanPhamRepository.findById(id).ifPresent(sanPham -> {
            sanPham.setTrangThai(false);
            sanPhamRepository.save(sanPham);
        });
    }

}
