package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.CongGiaoTiep;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.CongGiaoTiepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CongGiaoTiepService extends GenericCrudService<CongGiaoTiep, Long> {
    private final CongGiaoTiepRepository congGiaoTiepRepository;

    @Override
    protected JpaRepository<CongGiaoTiep, Long> getRepository() {
        return congGiaoTiepRepository;
    }
}