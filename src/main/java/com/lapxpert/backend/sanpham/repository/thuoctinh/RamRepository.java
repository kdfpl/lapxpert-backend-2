package com.lapxpert.backend.sanpham.repository.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.Ram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RamRepository extends JpaRepository<Ram, Long> {

    @Query(value = "SELECT r.ma_ram FROM ram r WHERE r.ma_ram LIKE 'RAM%' ORDER BY r.ma_ram DESC LIMIT 1", nativeQuery = true)
    String findLastMaRam();
}