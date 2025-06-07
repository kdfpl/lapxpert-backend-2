package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ManHinh;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.ManHinhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManHinhService extends GenericCrudService<ManHinh, Long> {
    private final ManHinhRepository manHinhRepository;

    @Override
    protected JpaRepository<ManHinh, Long> getRepository() {
        return manHinhRepository;
    }
}