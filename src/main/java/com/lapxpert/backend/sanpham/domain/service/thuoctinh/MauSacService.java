package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.MauSac;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.MauSacRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MauSacService extends GenericCrudService<MauSac, Long> {
    private final MauSacRepository mauSacRepository;

    @Override
    protected JpaRepository<MauSac, Long> getRepository() {
        return mauSacRepository;
    }
}
