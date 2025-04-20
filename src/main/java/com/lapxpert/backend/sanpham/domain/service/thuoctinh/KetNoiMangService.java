package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.KetNoiMang;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.KetNoiMangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KetNoiMangService extends GenericCrudService<KetNoiMang, Long> {
    private final KetNoiMangRepository ketNoiMangRepository;

    @Override
    protected JpaRepository<KetNoiMang, Long> getRepository() {
        return ketNoiMangRepository;
    }
}