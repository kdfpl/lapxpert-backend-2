package com.lapxpert.backend.sanpham.domain.repository.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Cpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CpuRepository extends JpaRepository<Cpu, Long> {

    @Query(value = "SELECT c.ma_cpu FROM cpu c WHERE c.ma_cpu LIKE 'CPU%' ORDER BY c.ma_cpu DESC LIMIT 1", nativeQuery = true)
    String findLastMaCpu();
}