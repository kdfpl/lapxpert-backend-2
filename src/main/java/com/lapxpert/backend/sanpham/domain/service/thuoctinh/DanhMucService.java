package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.DanhMucRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DanhMucService extends GenericCrudService<DanhMuc, Long> {
    private final DanhMucRepository danhMucRepository;

    @Override
    protected JpaRepository<DanhMuc, Long> getRepository() {
        return danhMucRepository;
    }
}