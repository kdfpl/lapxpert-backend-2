package com.lapxpert.backend.sanpham.application.controller;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.*;
import com.lapxpert.backend.sanpham.domain.service.thuoctinh.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for product attributes management
 * Updated for SanPham module refactoring - only includes 8 core attributes:
 * 6 Core Product Attributes: CPU, RAM, GPU, MauSac, OCung, ManHinh
 * 2 Additional Required: DanhMuc, ThuongHieu
 */
@RestController
@RequestMapping("/api/v1/products/attributes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ThuocTinhController {
    // 6 Core Product Attributes (as per SanPham module refactoring)
    private final CpuService cpuService;
    private final RamService ramService;
    private final GpuService gpuService;
    private final MauSacService mauSacService;
    private final OCungService oCungService;
    private final ManHinhService manHinhService;

    // Additional Required Attributes
    private final DanhMucService danhMucService;
    private final ThuongHieuService thuongHieuService;

    // === 6 CORE PRODUCT ATTRIBUTES ===

    // CPU CRUD
    @GetMapping("/cpu")
    public ResponseEntity<List<Cpu>> findAllCpu() {
        return ResponseEntity.ok(cpuService.findAll());
    }

    @PutMapping("/cpu")
    public ResponseEntity<Cpu> saveCpu(@RequestBody Cpu cpu) {
        return ResponseEntity.ok(cpuService.save(cpu));
    }

    @PutMapping("/cpu/multiple")
    public ResponseEntity<List<Cpu>> saveMultipleCpu(@RequestBody List<Cpu> cpus) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cpuService.saveMultiple(cpus));
    }

    @DeleteMapping("/cpu/{id}")
    public ResponseEntity<Void> deleteCpu(@PathVariable Long id) {
        Cpu existingCpu = cpuService.findById(id);
        if (existingCpu == null) {
            return ResponseEntity.notFound().build();
        }
        cpuService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cpu")
    public ResponseEntity<Void> deleteMultipleCpu(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        cpuService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // RAM CRUD
    @GetMapping("/ram")
    public ResponseEntity<List<Ram>> findAllRam() {
        return ResponseEntity.ok(ramService.findAll());
    }

    @PutMapping("/ram")
    public ResponseEntity<Ram> saveRam(@RequestBody Ram ram) {
        return ResponseEntity.ok(ramService.save(ram));
    }

    @PutMapping("/ram/multiple")
    public ResponseEntity<List<Ram>> saveMultipleRam(@RequestBody List<Ram> rams) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ramService.saveMultiple(rams));
    }

    @DeleteMapping("/ram/{id}")
    public ResponseEntity<Void> deleteRam(@PathVariable Long id) {
        Ram existingRam = ramService.findById(id);
        if (existingRam == null) {
            return ResponseEntity.notFound().build();
        }
        ramService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/ram")
    public ResponseEntity<Void> deleteMultipleRam(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ramService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // GPU CRUD
    @GetMapping("/gpu")
    public ResponseEntity<List<Gpu>> findAllGpu() {
        return ResponseEntity.ok(gpuService.findAll());
    }

    @PutMapping("/gpu")
    public ResponseEntity<Gpu> saveGpu(@RequestBody Gpu gpu) {
        return ResponseEntity.ok(gpuService.save(gpu));
    }

    @PutMapping("/gpu/multiple")
    public ResponseEntity<List<Gpu>> saveMultipleGpu(@RequestBody List<Gpu> gpus) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gpuService.saveMultiple(gpus));
    }

    @DeleteMapping("/gpu/{id}")
    public ResponseEntity<Void> deleteGpu(@PathVariable Long id) {
        Gpu existingGpu = gpuService.findById(id);
        if (existingGpu == null) {
            return ResponseEntity.notFound().build();
        }
        gpuService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/gpu")
    public ResponseEntity<Void> deleteMultipleGpu(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        gpuService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // MauSac (Color) CRUD
    @GetMapping("/colors")
    public ResponseEntity<List<MauSac>> findAllColors() {
        return ResponseEntity.ok(mauSacService.findAll());
    }

    @PutMapping("/colors")
    public ResponseEntity<MauSac> saveColor(@RequestBody MauSac color) {
        return ResponseEntity.ok(mauSacService.save(color));
    }

    @PutMapping("/colors/multiple")
    public ResponseEntity<List<MauSac>> saveMultipleColors(@RequestBody List<MauSac> colors) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mauSacService.saveMultiple(colors));
    }

    @DeleteMapping("/colors/{id}")
    public ResponseEntity<Void> deleteColor(@PathVariable Long id) {
        MauSac existingColor = mauSacService.findById(id);
        if (existingColor == null) {
            return ResponseEntity.notFound().build();
        }
        mauSacService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/colors")
    public ResponseEntity<Void> deleteMultipleColors(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        mauSacService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // OCung (Storage) CRUD
    @GetMapping("/storage")
    public ResponseEntity<List<OCung>> findAllStorage() {
        return ResponseEntity.ok(oCungService.findAll());
    }

    @PutMapping("/storage")
    public ResponseEntity<OCung> saveStorage(@RequestBody OCung storage) {
        return ResponseEntity.ok(oCungService.save(storage));
    }

    @PutMapping("/storage/multiple")
    public ResponseEntity<List<OCung>> saveMultipleStorage(@RequestBody List<OCung> storages) {
        return ResponseEntity.status(HttpStatus.CREATED).body(oCungService.saveMultiple(storages));
    }

    @DeleteMapping("/storage/{id}")
    public ResponseEntity<Void> deleteStorage(@PathVariable Long id) {
        OCung existingStorage = oCungService.findById(id);
        if (existingStorage == null) {
            return ResponseEntity.notFound().build();
        }
        oCungService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/storage")
    public ResponseEntity<Void> deleteMultipleStorage(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        oCungService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // ManHinh (Screen) CRUD
    @GetMapping("/screen")
    public ResponseEntity<List<ManHinh>> findAllScreen() {
        return ResponseEntity.ok(manHinhService.findAll());
    }

    @PutMapping("/screen")
    public ResponseEntity<ManHinh> saveScreen(@RequestBody ManHinh screen) {
        return ResponseEntity.ok(manHinhService.save(screen));
    }

    @PutMapping("/screen/multiple")
    public ResponseEntity<List<ManHinh>> saveMultipleScreen(@RequestBody List<ManHinh> screens) {
        return ResponseEntity.status(HttpStatus.CREATED).body(manHinhService.saveMultiple(screens));
    }

    @DeleteMapping("/screen/{id}")
    public ResponseEntity<Void> deleteScreen(@PathVariable Long id) {
        ManHinh existingScreen = manHinhService.findById(id);
        if (existingScreen == null) {
            return ResponseEntity.notFound().build();
        }
        manHinhService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/screen")
    public ResponseEntity<Void> deleteMultipleScreen(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        manHinhService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // === ADDITIONAL REQUIRED ATTRIBUTES ===

    // DanhMuc (Category) CRUD
    @GetMapping("/category")
    public ResponseEntity<List<DanhMuc>> findAllCategory() {
        return ResponseEntity.ok(danhMucService.findAll());
    }

    @PutMapping("/category")
    public ResponseEntity<DanhMuc> saveCategory(@RequestBody DanhMuc category) {
        return ResponseEntity.ok(danhMucService.save(category));
    }

    @DeleteMapping("/category/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        DanhMuc existingCategory = danhMucService.findById(id);
        if (existingCategory == null) {
            return ResponseEntity.notFound().build();
        }
        danhMucService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ThuongHieu (Brand) CRUD
    @GetMapping("/brand")
    public ResponseEntity<List<ThuongHieu>> findAllBrand() {
        return ResponseEntity.ok(thuongHieuService.findAll());
    }

    @PutMapping("/brand")
    public ResponseEntity<ThuongHieu> saveBrand(@RequestBody ThuongHieu brand) {
        return ResponseEntity.ok(thuongHieuService.save(brand));
    }

    @PutMapping("/brand/multiple")
    public ResponseEntity<List<ThuongHieu>> saveMultipleBrand(@RequestBody List<ThuongHieu> brands) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thuongHieuService.saveMultiple(brands));
    }

    @DeleteMapping("/brand/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        ThuongHieu existingBrand = thuongHieuService.findById(id);
        if (existingBrand == null) {
            return ResponseEntity.notFound().build();
        }
        thuongHieuService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/brand")
    public ResponseEntity<Void> deleteMultipleBrand(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        thuongHieuService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }
}