package com.lapxpert.backend.dotgiamgia.application.controller;

import com.lapxpert.backend.dotgiamgia.application.dto.DotGiamGiaDTO;
import com.lapxpert.backend.dotgiamgia.domain.service.DotGiamGiaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("toggle/{id}")
    public ResponseEntity<DotGiamGiaDTO> toggle(@PathVariable Long id) {
        return service.toggle(id);
    }
}
