package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.ThuongHieuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThuongHieuService extends GenericCrudService<ThuongHieu, Long> {
    private final ThuongHieuRepository thuongHieuRepository;

    @Override
    protected JpaRepository<ThuongHieu, Long> getRepository() {
        return thuongHieuRepository;
    }
}