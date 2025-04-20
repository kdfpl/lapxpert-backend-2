package com.lapxpert.backend.dotgiamgia.application.controller;

import com.lapxpert.backend.dotgiamgia.application.dto.DotGiamGiaDto;
import com.lapxpert.backend.dotgiamgia.domain.service.DotGiamGiaService;
import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDto;
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
    public ResponseEntity<List<DotGiamGiaDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PutMapping
    public ResponseEntity<DotGiamGiaDto> save(@RequestBody DotGiamGiaDto dto) {
        return service.save(dto);
    }

    @PostMapping("toggle/{id}")
    public ResponseEntity<DotGiamGiaDto> toggle(@PathVariable Long id) {
        return service.toggle(id);
    }

    @PostMapping("toggles")
    public ResponseEntity<List<DotGiamGiaDto>> toggleMultiple(@RequestBody List<Long> ids) {
        return service.toggleMultiple(ids);
    }

    @GetMapping("{id}/spct")
    public ResponseEntity<Set<SanPhamChiTietDto>> findAllSanPhamChiTietsByDotGiamGiaId(@PathVariable Long id) {
        return ResponseEntity.ok(service.findAllSanPhamChiTietsByDotGiamGiaId(id));
    }

    @PutMapping("{id}/spct")
    public ResponseEntity<DotGiamGiaDto> addSanPhamChiTiets(
            @PathVariable Long id,
            @RequestBody List<Long> sanPhamChiTietIds) {
        return ResponseEntity.ok(service.addSanPhamChiTiets(id, sanPhamChiTietIds));
    }

    @DeleteMapping("{id}/spct")
    public ResponseEntity<DotGiamGiaDto> removeSanPhamChiTiets(
            @PathVariable Long id,
            @RequestBody List<Long> sanPhamChiTietIds) {
        return ResponseEntity.ok(service.removeSanPhamChiTiets(id, sanPhamChiTietIds));
    }
}
