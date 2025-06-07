package com.lapxpert.backend.sanpham.domain.repository.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Long> {
}