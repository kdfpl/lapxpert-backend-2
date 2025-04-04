package com.lapxpert.backend.dotgiamgia.application.controller;

import com.lapxpert.backend.dotgiamgia.application.dto.DotGiamGiaDTO;
import com.lapxpert.backend.dotgiamgia.domain.service.DotGiamGiaService;
import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/discounts")
@CrossOrigin(origins = "*")
public class DotGiamGiaController {

    private final DotGiamGiaService service;

    public DotGiamGiaController(DotGiamGiaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DotGiamGiaDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PutMapping
    public ResponseEntity<DotGiamGiaDTO> save(@RequestBody DotGiamGiaDTO dto) {
        return service.save(dto);
    }

    @PostMapping("toggle/{id}")
    public ResponseEntity<DotGiamGiaDTO> toggle(@PathVariable Long id) {
        return service.toggle(id);
    }

    @PostMapping("toggles")
    public ResponseEntity<List<DotGiamGiaDTO>> toggleMultiple(@RequestBody List<Long> ids) {
        return service.toggleMultiple(ids);
    }

    @GetMapping("{id}/spct")
    public ResponseEntity<Set<SanPhamChiTietDTO>> findAllSanPhamChiTietsByDotGiamGiaId(@PathVariable Long id) {
        return ResponseEntity.ok(service.findAllSanPhamChiTietsByDotGiamGiaId(id));
    }

    @PutMapping("{id}/spct")
    public ResponseEntity<DotGiamGiaDTO> addSanPhamChiTiets(
            @PathVariable Long id,
            @RequestBody List<Long> sanPhamChiTietIds) {
        return service.addSanPhamChiTiets(id, sanPhamChiTietIds);
    }

    @DeleteMapping("{id}/spct")
    public ResponseEntity<DotGiamGiaDTO> removeSanPhamChiTiets(
            @PathVariable Long id,
            @RequestBody List<Long> sanPhamChiTietIds) {
        return service.removeSanPhamChiTiets(id, sanPhamChiTietIds);
    }
}
