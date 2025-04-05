package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.HeDieuHanh;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.HeDieuHanhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HeDieuHanhService extends GenericCrudService<HeDieuHanh, Long> {
    private final HeDieuHanhRepository heDieuHanhRepository;

    @Override
    protected JpaRepository<HeDieuHanh, Long> getRepository() {
        return heDieuHanhRepository;
    }
}