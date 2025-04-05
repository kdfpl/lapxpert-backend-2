package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Gpu;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.GpuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GpuService extends GenericCrudService<Gpu, Long> {
    private final GpuRepository gpuRepository;

    @Override
    protected JpaRepository<Gpu, Long> getRepository() {
        return gpuRepository;
    }
}