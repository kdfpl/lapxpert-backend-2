package com.lapxpert.backend.sanpham.domain.repository.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Cpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CpuRepository extends JpaRepository<Cpu, Long> {
}