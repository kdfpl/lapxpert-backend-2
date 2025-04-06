package com.lapxpert.backend.phieugiamgia.domain.repository;

import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDungId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuGiamGiaNguoiDungRepository extends JpaRepository<PhieuGiamGiaNguoiDung, PhieuGiamGiaNguoiDungId> {
    void deleteByPhieuGiamGiaId(Long phieuGiamGiaId);

    boolean existsById(PhieuGiamGiaNguoiDungId id);
    long countByPhieuGiamGiaId(Long phieuGiamGiaId);

    List<PhieuGiamGiaNguoiDung> findByPhieuGiamGiaId(Long phieuGiamGiaId);
}
