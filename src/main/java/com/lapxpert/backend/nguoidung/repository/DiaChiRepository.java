package com.lapxpert.backend.nguoidung.repository;

import com.lapxpert.backend.nguoidung.entity.DiaChi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaChiRepository extends JpaRepository<DiaChi, Long> {
    List<DiaChi> findAllByNguoiDungId(Long id);
}
