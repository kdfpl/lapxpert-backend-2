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

    @Transactional
    public List<SanPhamDto> findAll() {
        List<SanPham> entities = sanPhamRepository.findAll();
        return sanPhamMapper.toDtos(entities);
    }
}
