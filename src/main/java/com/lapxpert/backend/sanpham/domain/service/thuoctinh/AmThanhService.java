package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.AmThanh;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.AmThanhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AmThanhService extends GenericCrudService<AmThanh, Long> {
    private final AmThanhRepository amThanhRepository;

    @Override
    protected JpaRepository<AmThanh, Long> getRepository() {
        return amThanhRepository;
    }
}
