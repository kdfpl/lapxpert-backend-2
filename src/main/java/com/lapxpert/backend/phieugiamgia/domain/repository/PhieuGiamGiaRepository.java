package com.lapxpert.backend.phieugiamgia.domain.repository;

import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Long> {
}