package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.BanPhim;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.BanPhimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BanPhimService extends GenericCrudService<BanPhim, Long> {
    private final BanPhimRepository banPhimRepository;

    @Override
    protected JpaRepository<BanPhim, Long> getRepository() {
        return banPhimRepository;
    }
}