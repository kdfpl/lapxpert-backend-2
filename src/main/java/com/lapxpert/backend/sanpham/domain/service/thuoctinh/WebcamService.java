package com.lapxpert.backend.sanpham.domain.service.thuoctinh;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Webcam;
import com.lapxpert.backend.sanpham.domain.repository.GenericCrudService;
import com.lapxpert.backend.sanpham.domain.repository.thuoctinh.WebcamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebcamService extends GenericCrudService<Webcam, Long> {
    private final WebcamRepository webcamRepository;

    @Override
    protected JpaRepository<Webcam, Long> getRepository() {
        return webcamRepository;
    }
}