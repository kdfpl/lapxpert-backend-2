package com.lapxpert.backend.nguoidung.domain.repository;

import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaChiRepository extends JpaRepository<DiaChi, Long> {
    List<DiaChi> findAllByNguoiDungId(Long id);
}
