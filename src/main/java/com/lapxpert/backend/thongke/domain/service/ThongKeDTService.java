package com.lapxpert.backend.thongke.domain.service;


import com.lapxpert.backend.thongke.domain.entity.DoanhThuHangNgay;
import com.lapxpert.backend.thongke.domain.entity.DoanhThuThangDTO;
import com.lapxpert.backend.thongke.domain.entity.TongDoanhThuThangDTO;
import com.lapxpert.backend.thongke.domain.repository.ThongKeDTRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ThongKeDTService {

    @Autowired
    private ThongKeDTRepository thongKeDTRepository;

    public List<DoanhThuHangNgay> TongDoanhThuHangThang(){
        return thongKeDTRepository.getTongDoanhThuHangThang();
    }
    public List<Integer> TongDoanhThuTungNgayTrongThangNay(){
        return thongKeDTRepository.getTongDoanhThuTungNgayTrongThangNay();
    }
    public List<Integer> TongDoanhThuTungNgayTrongTuanNay(){
        return thongKeDTRepository.getTongDoanhThuTungNgayTrongTuanNay();
    }public List<Integer> TongDoanhThuTungNgayCustom(LocalDate start_date, LocalDate end_date){
        return thongKeDTRepository.getTongDoanhThuTungNgayCustom(start_date, end_date);
    }
    public List<TongDoanhThuThangDTO> TongDoanhThuTungThangTrongNamNay(){
        return thongKeDTRepository.getTongDoanhThuTungThangTrongNamNay();
    }




    public List<DoanhThuThangDTO> DoanhThuTungHangTrongNamNay(){
        return thongKeDTRepository.getDoanhThuTungHangTrongNamNay();
    }
    public List<DoanhThuHangNgay> DoanhThuTungHangTrongThangNay(){
        return thongKeDTRepository.getDoanhThuTungHangTrongThangNay();
    }
    public List<DoanhThuHangNgay> DoanhThuTungHangTrongTuanNay(){
        return thongKeDTRepository.getDoanhThuTungHangTrongTuanNay();
    }
    public List<DoanhThuHangNgay> DoanhThuCustomTime(LocalDate startTime, LocalDate endTime){
        return thongKeDTRepository.getDoanhThuCustomTime(startTime,endTime );
    }



    }


