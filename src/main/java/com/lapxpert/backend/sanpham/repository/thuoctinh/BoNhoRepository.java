package com.lapxpert.backend.sanpham.repository.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.BoNho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BoNhoRepository extends JpaRepository<BoNho, Long> {

    @Query(value = "SELECT b.ma_bo_nho FROM bo_nho b WHERE b.ma_bo_nho LIKE 'BN%' ORDER BY b.ma_bo_nho DESC LIMIT 1", nativeQuery = true)
    String findLastMaBoNho();
}