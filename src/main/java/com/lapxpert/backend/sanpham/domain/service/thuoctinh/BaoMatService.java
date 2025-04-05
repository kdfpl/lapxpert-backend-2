package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.BaoMat;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.BaoMatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BaoMatService extends GenericCrudService<BaoMat, Long> {
    private final BaoMatRepository baoMatRepository;

    @Override
    protected JpaRepository<BaoMat, Long> getRepository() {
        return baoMatRepository;
    }
}