package com.lapxpert.backend.thongke.domain.service;

import com.lapxpert.backend.thongke.domain.entity.TopDoanhSoDTO;
import com.lapxpert.backend.thongke.domain.repository.ThongKeDSRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ThongKeDSService {
    @Autowired
    private ThongKeDSRepository thongKeDSRepository;

    public List<TopDoanhSoDTO> getTopDoanhSoThang(){
        return thongKeDSRepository.getTopDoanhSoThang();
    }
    public List<TopDoanhSoDTO> getTopDoanhSoTuan(){
        return thongKeDSRepository.getTopDoanhSoTuan();
    }
    public List<TopDoanhSoDTO> getTopDoanhSoNgay(){
        return thongKeDSRepository.getTopDoanhSoNgay();
    }
    public List<TopDoanhSoDTO> getTopDoanhSoCustom(LocalDate start_dateTop, LocalDate end_dateTop){
        return thongKeDSRepository.getTopDoanhSoCustom(start_dateTop, end_dateTop);
    }

}
