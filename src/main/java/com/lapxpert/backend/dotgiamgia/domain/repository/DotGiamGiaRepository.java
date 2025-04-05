package com.lapxpert.backend.dotgiamgia.domain.repository;

import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Long> {
    List<DotGiamGia> findAllByDaAn(Boolean daAn);
}