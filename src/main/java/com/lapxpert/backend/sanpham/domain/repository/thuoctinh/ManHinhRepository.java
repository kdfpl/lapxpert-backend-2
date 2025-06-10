package com.lapxpert.backend.sanpham.domain.repository.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ManHinh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ManHinhRepository extends JpaRepository<ManHinh, Long> {

    @Query(value = "SELECT m.ma_man_hinh FROM man_hinh m WHERE m.ma_man_hinh LIKE 'MH%' ORDER BY m.ma_man_hinh DESC LIMIT 1", nativeQuery = true)
    String findLastMaManHinh();
}