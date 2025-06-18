package com.lapxpert.backend.sanpham.repository.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Long> {

    @Query(value = "SELECT d.ma_danh_muc FROM danh_muc d WHERE d.ma_danh_muc LIKE 'DM%' ORDER BY d.ma_danh_muc DESC LIMIT 1", nativeQuery = true)
    String findLastMaDanhMuc();
}