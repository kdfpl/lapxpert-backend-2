package com.lapxpert.backend.sanpham.repository.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.Gpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GpuRepository extends JpaRepository<Gpu, Long> {

    @Query(value = "SELECT g.ma_gpu FROM gpu g WHERE g.ma_gpu LIKE 'GPU%' ORDER BY g.ma_gpu DESC LIMIT 1", nativeQuery = true)
    String findLastMaGpu();
}