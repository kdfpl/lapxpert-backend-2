package com.lapxpert.backend.sanpham.repository.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Long> {

    @Query(value = "SELECT m.ma_mau_sac FROM mau_sac m WHERE m.ma_mau_sac LIKE 'MS%' ORDER BY m.ma_mau_sac DESC LIMIT 1", nativeQuery = true)
    String findLastMaMauSac();
}
