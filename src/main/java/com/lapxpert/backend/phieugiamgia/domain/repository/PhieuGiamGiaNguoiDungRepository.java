package com.lapxpert.backend.phieugiamgia.domain.repository;

import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDungId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuGiamGiaNguoiDungRepository extends JpaRepository<PhieuGiamGiaNguoiDung, PhieuGiamGiaNguoiDungId> {
    @Modifying
    @Transactional
    @Query("DELETE FROM PhieuGiamGiaNguoiDung p WHERE p.phieuGiamGia.id = :phieuGiamGiaId")
    void deleteByPhieuGiamGiaId(@Param("phieuGiamGiaId") Long phieuGiamGiaId);
    @Modifying
    @Transactional
    @Query("DELETE FROM PhieuGiamGiaNguoiDung p WHERE p.phieuGiamGia.id = :phieuGiamGiaId AND p.nguoiDung.id NOT IN :nguoiDungIds")
    void deleteByPhieuGiamGiaIdAndNguoiDungIdNotIn(@Param("phieuGiamGiaId") Long phieuGiamGiaId, @Param("nguoiDungIds") List<Long> nguoiDungIds);


    boolean existsById(PhieuGiamGiaNguoiDungId id);
    long countByPhieuGiamGiaId(Long phieuGiamGiaId);

    List<PhieuGiamGiaNguoiDung> findByPhieuGiamGiaId(Long phieuGiamGiaId);
}
