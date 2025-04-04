package com.lapxpert.backend.dotgiamgia.domain.service;

import com.lapxpert.backend.dotgiamgia.application.dto.DotGiamGiaDTO;
import com.lapxpert.backend.dotgiamgia.application.dto.DotGiamGiaMapper;
import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import com.lapxpert.backend.dotgiamgia.domain.repository.DotGiamGiaRepository;
import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDTO;
import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietMapper;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class DotGiamGiaService {
    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final DotGiamGiaMapper dotGiamGiaMapper;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamChiTietMapper sanPhamChiTietMapper;

    public DotGiamGiaService(DotGiamGiaRepository dotGiamGiaRepository, @Qualifier("dotGiamGiaMapperImpl") DotGiamGiaMapper dotGiamGiaMapper, SanPhamChiTietRepository sanPhamChiTietRepository, SanPhamChiTietMapper sanPhamChiTietMapper) {
        this.dotGiamGiaRepository = dotGiamGiaRepository;
        this.dotGiamGiaMapper = dotGiamGiaMapper;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.sanPhamChiTietMapper = sanPhamChiTietMapper;
    }

    public List<DotGiamGiaDTO> findAll() {
        return dotGiamGiaMapper.toDtos(dotGiamGiaRepository.findAllByDaAn(false));
    }

    @Transactional
    public ResponseEntity<DotGiamGiaDTO> save(DotGiamGiaDTO dto) {
        DotGiamGia entity = dotGiamGiaMapper.toEntity(dto);

        if (entity.getId() != null) {
            DotGiamGia existingEntity = dotGiamGiaRepository.findById(entity.getId()).orElse(null);
            if (existingEntity != null) {
                entity.setNgayTao(existingEntity.getNgayTao());
                return ResponseEntity.ok(dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(entity)));
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(entity)));
    }

    @Transactional
    public ResponseEntity<DotGiamGiaDTO> toggle(Long id) {
        DotGiamGia entity = dotGiamGiaRepository.findById(id).orElse(null);
        if (entity != null) {
            entity.setDaAn(!entity.getDaAn());
            return ResponseEntity.ok(dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(entity)));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public ResponseEntity<List<DotGiamGiaDTO>> toggleMultiple(List<Long> ids) {
        List<DotGiamGia> entities = dotGiamGiaRepository.findAllById(ids);
        if (!entities.isEmpty()) {
            for (DotGiamGia entity : entities) {
                entity.setDaAn(!entity.getDaAn());
            }
            return ResponseEntity.ok(dotGiamGiaMapper.toDtos(dotGiamGiaRepository.saveAll(entities)));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public DotGiamGia findById(Long id) {
        return dotGiamGiaRepository.findById(id).orElse(null);
    }

    @Transactional
    public Set<SanPhamChiTietDTO> findAllSanPhamChiTietsByDotGiamGiaId(Long id) {
        DotGiamGia dgg = dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DotGiamGia not found with id: " + id));
        Set<SanPhamChiTiet> sanPhamChiTiets = dgg.getSanPhamChiTiets();
        return sanPhamChiTietMapper.toDtoSet(sanPhamChiTiets);
    }

    @Transactional
    public ResponseEntity<DotGiamGiaDTO> addSanPhamChiTiets(Long dotGiamGiaId, List<Long> sanPhamChiTietIds) {
        DotGiamGia dotGiamGia = dotGiamGiaRepository.findById(dotGiamGiaId)
                .orElseThrow(() -> new RuntimeException("DotGiamGia not found with id: " + dotGiamGiaId));

        for (Long sanPhamChiTietId : sanPhamChiTietIds) {
            SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietRepository.findById(sanPhamChiTietId)
                    .orElseThrow(() -> new RuntimeException("SanPhamChiTiet not found with id: " + sanPhamChiTietId));
            dotGiamGia.getSanPhamChiTiets().add(sanPhamChiTiet);
            sanPhamChiTiet.getDotGiamGias().add(dotGiamGia);
        }
        return ResponseEntity.ok(dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(dotGiamGia)));
    }

    @Transactional
    public ResponseEntity<DotGiamGiaDTO> removeSanPhamChiTiets(Long dotGiamGiaId, List<Long> sanPhamChiTietIds) {
        DotGiamGia dotGiamGia = dotGiamGiaRepository.findById(dotGiamGiaId)
                .orElseThrow(() -> new RuntimeException("DotGiamGia not found with id: " + dotGiamGiaId));

        for (Long sanPhamChiTietId : sanPhamChiTietIds) {
            SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietRepository.findById(sanPhamChiTietId)
                    .orElseThrow(() -> new RuntimeException("SanPhamChiTiet not found with id: " + sanPhamChiTietId));
            dotGiamGia.getSanPhamChiTiets().remove(sanPhamChiTiet);
            sanPhamChiTiet.getDotGiamGias().remove(dotGiamGia);
        }
        return ResponseEntity.ok(dotGiamGiaMapper.toDto(dotGiamGiaRepository.save(dotGiamGia)));
    }
}
