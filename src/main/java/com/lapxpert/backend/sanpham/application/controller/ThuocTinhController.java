package com.lapxpert.backend.sanpham.application.controller;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.*;
import com.lapxpert.backend.sanpham.domain.service.thuoctinh.*;
import com.lapxpert.backend.sanpham.application.mapper.DanhMucMapper;
import com.lapxpert.backend.sanpham.application.dto.thuoctinh.DanhMucDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/attributes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class    ThuocTinhController {
    private final AmThanhService amThanhService;
    private final CpuService cpuService;
    private final BanPhimService banPhimService;
    private final BaoMatService baoMatService;
    private final CongGiaoTiepService congGiaoTiepService;
    private final GpuService gpuService;
    private final HeDieuHanhService heDieuHanhService;
    private final KetNoiMangService ketNoiMangService;
    private final ManHinhService manHinhService;
    private final OCungService oCungService;
    private final PinService pinService;
    private final RamService ramService;
    private final ThietKeService thietKeService;
    private final WebcamService webcamService;
    private final DanhMucService danhMucService;
    private final DanhMucMapper danhMucMapper;
    private final ThuongHieuService thuongHieuService;
    private final MauSacService mauSacService;

    // AmThanh CRUD
    @GetMapping("/audio")
    public ResponseEntity<List<AmThanh>> findAllAudio() {
        return ResponseEntity.ok(amThanhService.findAll());
    }

    @PutMapping("/audio")
    public ResponseEntity<AmThanh> saveAudio(@RequestBody AmThanh audio) {
        return ResponseEntity.ok(amThanhService.save(audio));
    }

    @PutMapping("/audio/multiple")
    public ResponseEntity<List<AmThanh>> saveMultipleAudio(@RequestBody List<AmThanh> audios) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amThanhService.saveMultiple(audios));
    }

    @DeleteMapping("/audio/{id}")
    public ResponseEntity<Void> deleteAudio(@PathVariable Long id) {
        AmThanh existingAudio = amThanhService.findById(id);
        if (existingAudio == null) {
            return ResponseEntity.notFound().build();
        }
        amThanhService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/audio")
    public ResponseEntity<Void> deleteMultipleAudio(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        amThanhService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Cpu CRUD
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

    // Storage CRUD
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

    // Screen CRUD
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

    // Interface CRUD
    @GetMapping("/interface")
    public ResponseEntity<List<CongGiaoTiep>> findAllInterface() {
        return ResponseEntity.ok(congGiaoTiepService.findAll());
    }

    @PutMapping("/interface")
    public ResponseEntity<CongGiaoTiep> saveInterface(@RequestBody CongGiaoTiep interface_) {
        return ResponseEntity.ok(congGiaoTiepService.save(interface_));
    }

    @PutMapping("/interface/multiple")
    public ResponseEntity<List<CongGiaoTiep>> saveMultipleInterface(@RequestBody List<CongGiaoTiep> interfaces) {
        return ResponseEntity.status(HttpStatus.CREATED).body(congGiaoTiepService.saveMultiple(interfaces));
    }

    @DeleteMapping("/interface/{id}")
    public ResponseEntity<Void> deleteInterface(@PathVariable Long id) {
        CongGiaoTiep existingInterface = congGiaoTiepService.findById(id);
        if (existingInterface == null) {
            return ResponseEntity.notFound().build();
        }
        congGiaoTiepService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/interface")
    public ResponseEntity<Void> deleteMultipleInterface(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        congGiaoTiepService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Network CRUD
    @GetMapping("/network")
    public ResponseEntity<List<KetNoiMang>> findAllNetwork() {
        return ResponseEntity.ok(ketNoiMangService.findAll());
    }

    @PutMapping("/network")
    public ResponseEntity<KetNoiMang> saveNetwork(@RequestBody KetNoiMang network) {
        return ResponseEntity.ok(ketNoiMangService.save(network));
    }

    @PutMapping("/network/multiple")
    public ResponseEntity<List<KetNoiMang>> saveMultipleNetwork(@RequestBody List<KetNoiMang> networks) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ketNoiMangService.saveMultiple(networks));
    }

    @DeleteMapping("/network/{id}")
    public ResponseEntity<Void> deleteNetwork(@PathVariable Long id) {
        KetNoiMang existingNetwork = ketNoiMangService.findById(id);
        if (existingNetwork == null) {
            return ResponseEntity.notFound().build();
        }
        ketNoiMangService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/network")
    public ResponseEntity<Void> deleteMultipleNetwork(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ketNoiMangService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Webcam CRUD
    @GetMapping("/webcam")
    public ResponseEntity<List<Webcam>> findAllWebcam() {
        return ResponseEntity.ok(webcamService.findAll());
    }

    @PutMapping("/webcam")
    public ResponseEntity<Webcam> saveWebcam(@RequestBody Webcam webcam) {
        return ResponseEntity.ok(webcamService.save(webcam));
    }

    @PutMapping("/webcam/multiple")
    public ResponseEntity<List<Webcam>> saveMultipleWebcam(@RequestBody List<Webcam> webcams) {
        return ResponseEntity.status(HttpStatus.CREATED).body(webcamService.saveMultiple(webcams));
    }

    @DeleteMapping("/webcam/{id}")
    public ResponseEntity<Void> deleteWebcam(@PathVariable Long id) {
        Webcam existingWebcam = webcamService.findById(id);
        if (existingWebcam == null) {
            return ResponseEntity.notFound().build();
        }
        webcamService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/webcam")
    public ResponseEntity<Void> deleteMultipleWebcam(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        webcamService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Security CRUD
    @GetMapping("/security")
    public ResponseEntity<List<BaoMat>> findAllSecurity() {
        return ResponseEntity.ok(baoMatService.findAll());
    }

    @PutMapping("/security")
    public ResponseEntity<BaoMat> saveSecurity(@RequestBody BaoMat security) {
        return ResponseEntity.ok(baoMatService.save(security));
    }

    @PutMapping("/security/multiple")
    public ResponseEntity<List<BaoMat>> saveMultipleSecurity(@RequestBody List<BaoMat> securities) {
        return ResponseEntity.status(HttpStatus.CREATED).body(baoMatService.saveMultiple(securities));
    }

    @DeleteMapping("/security/{id}")
    public ResponseEntity<Void> deleteSecurity(@PathVariable Long id) {
        BaoMat existingSecurity = baoMatService.findById(id);
        if (existingSecurity == null) {
            return ResponseEntity.notFound().build();
        }
        baoMatService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/security")
    public ResponseEntity<Void> deleteMultipleSecurity(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        baoMatService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // OS CRUD
    @GetMapping("/os")
    public ResponseEntity<List<HeDieuHanh>> findAllOs() {
        return ResponseEntity.ok(heDieuHanhService.findAll());
    }

    @PutMapping("/os")
    public ResponseEntity<HeDieuHanh> saveOs(@RequestBody HeDieuHanh os) {
        return ResponseEntity.ok(heDieuHanhService.save(os));
    }

    @PutMapping("/os/multiple")
    public ResponseEntity<List<HeDieuHanh>> saveMultipleOs(@RequestBody List<HeDieuHanh> osList) {
        return ResponseEntity.status(HttpStatus.CREATED).body(heDieuHanhService.saveMultiple(osList));
    }

    @DeleteMapping("/os/{id}")
    public ResponseEntity<Void> deleteOs(@PathVariable Long id) {
        HeDieuHanh existingOs = heDieuHanhService.findById(id);
        if (existingOs == null) {
            return ResponseEntity.notFound().build();
        }
        heDieuHanhService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/os")
    public ResponseEntity<Void> deleteMultipleOs(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        heDieuHanhService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Battery CRUD
    @GetMapping("/battery")
    public ResponseEntity<List<Pin>> findAllBattery() {
        return ResponseEntity.ok(pinService.findAll());
    }

    @PutMapping("/battery")
    public ResponseEntity<Pin> saveBattery(@RequestBody Pin battery) {
        return ResponseEntity.ok(pinService.save(battery));
    }

    @PutMapping("/battery/multiple")
    public ResponseEntity<List<Pin>> saveMultipleBattery(@RequestBody List<Pin> batteries) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pinService.saveMultiple(batteries));
    }

    @DeleteMapping("/battery/{id}")
    public ResponseEntity<Void> deleteBattery(@PathVariable Long id) {
        Pin existingBattery = pinService.findById(id);
        if (existingBattery == null) {
            return ResponseEntity.notFound().build();
        }
        pinService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/battery")
    public ResponseEntity<Void> deleteMultipleBattery(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        pinService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Keyboard CRUD
    @GetMapping("/keyboard")
    public ResponseEntity<List<BanPhim>> findAllKeyboard() {
        return ResponseEntity.ok(banPhimService.findAll());
    }

    @PutMapping("/keyboard")
    public ResponseEntity<BanPhim> saveKeyboard(@RequestBody BanPhim keyboard) {
        return ResponseEntity.ok(banPhimService.save(keyboard));
    }

    @PutMapping("/keyboard/multiple")
    public ResponseEntity<List<BanPhim>> saveMultipleKeyboard(@RequestBody List<BanPhim> keyboards) {
        return ResponseEntity.status(HttpStatus.CREATED).body(banPhimService.saveMultiple(keyboards));
    }

    @DeleteMapping("/keyboard/{id}")
    public ResponseEntity<Void> deleteKeyboard(@PathVariable Long id) {
        BanPhim existingKeyboard = banPhimService.findById(id);
        if (existingKeyboard == null) {
            return ResponseEntity.notFound().build();
        }
        banPhimService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/keyboard")
    public ResponseEntity<Void> deleteMultipleKeyboard(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        banPhimService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Design CRUD
    @GetMapping("/design")
    public ResponseEntity<List<ThietKe>> findAllDesign() {
        return ResponseEntity.ok(thietKeService.findAll());
    }

    @PutMapping("/design")
    public ResponseEntity<ThietKe> saveDesign(@RequestBody ThietKe design) {
        return ResponseEntity.ok(thietKeService.save(design));
    }

    @PutMapping("/design/multiple")
    public ResponseEntity<List<ThietKe>> saveMultipleDesign(@RequestBody List<ThietKe> designs) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thietKeService.saveMultiple(designs));
    }

    @DeleteMapping("/design/{id}")
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        ThietKe existingDesign = thietKeService.findById(id);
        if (existingDesign == null) {
            return ResponseEntity.notFound().build();
        }
        thietKeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/design")
    public ResponseEntity<Void> deleteMultipleDesign(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        thietKeService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Category (DanhMuc) CRUD
    @GetMapping("/categories")
    public ResponseEntity<List<DanhMucDto>> findAllCategories() {
        List<DanhMuc> categories = danhMucService.findAll();
        return ResponseEntity.ok(danhMucMapper.toDtos(categories));
    }

    @PutMapping("/categories")
    public ResponseEntity<DanhMucDto> saveCategory(@RequestBody DanhMucDto categoryDto) {
        DanhMuc category = danhMucMapper.toEntity(categoryDto);
        DanhMuc savedCategory = danhMucService.save(category);
        return ResponseEntity.ok(danhMucMapper.toDto(savedCategory));
    }

    @PutMapping("/categories/multiple")
    public ResponseEntity<List<DanhMucDto>> saveMultipleCategories(@RequestBody List<DanhMucDto> categoryDtos) {
        List<DanhMuc> categories = categoryDtos.stream()
                .map(danhMucMapper::toEntity)
                .toList();
        List<DanhMuc> savedCategories = danhMucService.saveMultiple(categories);
        return ResponseEntity.status(HttpStatus.CREATED).body(danhMucMapper.toDtos(savedCategories));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        DanhMuc existingCategory = danhMucService.findById(id);
        if (existingCategory == null) {
            return ResponseEntity.notFound().build();
        }
        danhMucService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/categories")
    public ResponseEntity<Void> deleteMultipleCategories(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        danhMucService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Brand (ThuongHieu) CRUD
    @GetMapping("/brands")
    public ResponseEntity<List<ThuongHieu>> findAllBrands() {
        return ResponseEntity.ok(thuongHieuService.findAll());
    }

    @PutMapping("/brands")
    public ResponseEntity<ThuongHieu> saveBrand(@RequestBody ThuongHieu brand) {
        return ResponseEntity.ok(thuongHieuService.save(brand));
    }

    @PutMapping("/brands/multiple")
    public ResponseEntity<List<ThuongHieu>> saveMultipleBrands(@RequestBody List<ThuongHieu> brands) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thuongHieuService.saveMultiple(brands));
    }

    @DeleteMapping("/brands/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        ThuongHieu existingBrand = thuongHieuService.findById(id);
        if (existingBrand == null) {
            return ResponseEntity.notFound().build();
        }
        thuongHieuService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/brands")
    public ResponseEntity<Void> deleteMultipleBrands(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        thuongHieuService.deleteMultiple(ids);
        return ResponseEntity.noContent().build();
    }

    // Color CRUD
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

}