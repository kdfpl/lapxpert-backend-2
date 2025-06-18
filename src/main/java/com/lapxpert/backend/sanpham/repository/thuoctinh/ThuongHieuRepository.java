package com.lapxpert.backend.sanpham.repository.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.ThuongHieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Long> {

    @Query(value = "SELECT t.ma_thuong_hieu FROM thuong_hieu t WHERE t.ma_thuong_hieu LIKE 'TH%' ORDER BY t.ma_thuong_hieu DESC LIMIT 1", nativeQuery = true)
    String findLastMaThuongHieu();
}