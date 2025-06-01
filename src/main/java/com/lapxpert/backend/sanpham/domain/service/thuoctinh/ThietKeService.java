package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThietKe;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.ThietKeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThietKeService extends GenericCrudService<ThietKe, Long> {
    private final ThietKeRepository thietKeRepository;

    @Override
    protected JpaRepository<ThietKe, Long> getRepository() {
        return thietKeRepository;
    }
}