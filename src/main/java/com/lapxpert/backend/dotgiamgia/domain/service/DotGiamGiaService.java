package com.lapxpert.backend.dotgiamgia.domain.service;

import com.lapxpert.backend.dotgiamgia.application.dto.DotGiamGiaDTO;
import com.lapxpert.backend.dotgiamgia.application.dto.DotGiamGiaMapper;
import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import com.lapxpert.backend.dotgiamgia.domain.repository.DotGiamGiaRepository;
import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDTO;
import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietMapper;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class DotGiamGiaService {
    private final DotGiamGiaRepository repository;
    private final DotGiamGiaMapper mapper;
    private final SanPhamChiTietMapper sanPhamChiTietMapper;

    public DotGiamGiaService(DotGiamGiaRepository repository, @Qualifier("dotGiamGiaMapperImpl") DotGiamGiaMapper mapper, SanPhamChiTietMapper sanPhamChiTietMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.sanPhamChiTietMapper = sanPhamChiTietMapper;
    }

    public List<DotGiamGiaDTO> findAll() {
        return mapper.toDtos(repository.findAllByDaAn(false));
    }

    @Transactional
    public ResponseEntity<DotGiamGiaDTO> save(DotGiamGiaDTO dto) {
        DotGiamGia entity = mapper.toEntity(dto);

        if (entity.getId() != null) {
            DotGiamGia existingEntity = repository.findById(entity.getId()).orElse(null);
            if (existingEntity != null) {
                entity.setNgayTao(existingEntity.getNgayTao());
                return ResponseEntity.ok(mapper.toDto(repository.save(entity)));
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(repository.save(entity)));
    }

    @Transactional
    public ResponseEntity<DotGiamGiaDTO> toggle(Long id) {
        DotGiamGia entity = repository.findById(id).orElse(null);
        if (entity != null) {
            entity.setDaAn(!entity.getDaAn());
            return ResponseEntity.ok(mapper.toDto(repository.save(entity)));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public ResponseEntity<List<DotGiamGiaDTO>> toggleMultiple(List<Long> ids) {
        List<DotGiamGia> entities = repository.findAllById(ids);
        if (!entities.isEmpty()) {
            for (DotGiamGia entity : entities) {
                entity.setDaAn(!entity.getDaAn());
            }
            return ResponseEntity.ok(mapper.toDtos(repository.saveAll(entities)));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public DotGiamGia findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional
    public Set<SanPhamChiTietDTO> findAllSanPhamChiTietsByDotGiamGiaId(Long id) {
        DotGiamGia dgg = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("DotGiamGia not found with id: " + id));
        Set<SanPhamChiTiet> sanPhamChiTiets = dgg.getSanPhamChiTiets();
        return sanPhamChiTietMapper.toDtoSet(sanPhamChiTiets);
    }
}
