package com.lapxpert.backend.phieugiamgia.domain.service;

import com.lapxpert.backend.nguoidung.application.dto.KhachHangDTO;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDungId;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaNguoiDungRepository;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PhieuGiamGiaService {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private PhieuGiamGiaNguoiDungRepository phieuGiamGiaNguoiDungRepository;
    public List<PhieuGiamGia> getAllPhieuGiamGia() {
        List<PhieuGiamGia> danhSach = phieuGiamGiaRepository.findAll();
        for (PhieuGiamGia p : danhSach) {
            PhieuGiamGia.TrangThaiPhieuGiamGia trangThai = PhieuGiamGia.fromDates(p.getNgayBatDau(), p.getNgayKetThuc());
            if (!p.getTrangThai().equals(trangThai)) {
                p.setTrangThai(trangThai);
                p.setNgayCapNhat(OffsetDateTime.now());
                phieuGiamGiaRepository.save(p);
            }
        }
        return danhSach;
    }
    public PhieuGiamGia getPhieuGiamGiaById(Long id) {
        return phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá với ID: " + id));
    }
    // Thêm phiếu giảm giá mới
    @Transactional
    public PhieuGiamGia createPhieuGiamGia(PhieuGiamGia phieuGiamGia) {
        phieuGiamGia.setNgayTao(OffsetDateTime.now());
        phieuGiamGia.setNgayCapNhat(OffsetDateTime.now());

        // Xác định trạng thái của phiếu giảm giá dựa trên ngày bắt đầu và ngày kết thúc
        PhieuGiamGia.TrangThaiPhieuGiamGia trangThai = PhieuGiamGia.fromDates(phieuGiamGia.getNgayBatDau(), phieuGiamGia.getNgayKetThuc());
        phieuGiamGia.setTrangThai(trangThai); // Đảm bảo set đúng giá trị enum cho trạng thái

        return phieuGiamGiaRepository.save(phieuGiamGia);
    }
    // Cập nhật phiếu giảm giá
    @Transactional
    public PhieuGiamGia updatePhieuGiamGia(Long id, PhieuGiamGia phieuGiamGiaMoi) {
        PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá."));

        // Cập nhật thông tin của phiếu
        phieuGiamGia.setMaPhieuGiamGia(phieuGiamGiaMoi.getMaPhieuGiamGia());
        phieuGiamGia.setGiaTriGiam(phieuGiamGiaMoi.getGiaTriGiam());
        phieuGiamGia.setGiaTriDonHangToiThieu(phieuGiamGiaMoi.getGiaTriDonHangToiThieu());
        phieuGiamGia.setNgayBatDau(phieuGiamGiaMoi.getNgayBatDau());
        phieuGiamGia.setNgayKetThuc(phieuGiamGiaMoi.getNgayKetThuc());
        phieuGiamGia.setMoTa(phieuGiamGiaMoi.getMoTa());
        phieuGiamGia.setPhieuRiengTu(phieuGiamGiaMoi.getPhieuRiengTu());
        phieuGiamGia.setSoLuongBanDau(phieuGiamGiaMoi.getSoLuongBanDau());
        phieuGiamGia.setLoaiPhieuGiamGia(phieuGiamGiaMoi.isLoaiPhieuGiamGia());

        // Tự động cập nhật trạng thái
        phieuGiamGia.setTrangThai(
                PhieuGiamGia.fromDates(phieuGiamGia.getNgayBatDau(), phieuGiamGia.getNgayKetThuc())
        );

        // Cập nhật ngày sửa
        phieuGiamGia.setNgayCapNhat(OffsetDateTime.now());

        return phieuGiamGiaRepository.save(phieuGiamGia);
    }

    @Transactional
    public void endPhieuGiamGia(Long id) {
        Optional<PhieuGiamGia> phieuGiamGiaOpt = phieuGiamGiaRepository.findById(id);
        if (!phieuGiamGiaOpt.isPresent()) {
            throw new RuntimeException("Không tìm thấy phiếu giảm giá để thay đổi trạng thái.");
        }

        PhieuGiamGia phieuGiamGia = phieuGiamGiaOpt.get();

        // Đổi trạng thái của phiếu giảm giá thành "Kết thúc"
        phieuGiamGia.setTrangThai(PhieuGiamGia.TrangThaiPhieuGiamGia.KET_THUC);

        // Cập nhật ngày kết thúc là ngày hiện tại
        phieuGiamGia.setNgayKetThuc(OffsetDateTime.now());

        // Lưu lại thay đổi
        phieuGiamGiaRepository.save(phieuGiamGia);
    }
}