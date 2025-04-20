package com.lapxpert.backend.thongke.application.service;


import com.lapxpert.backend.hoadon.enity.HoaDon;
import com.lapxpert.backend.thongke.application.enity.DoanhThuHangNgay;
import com.lapxpert.backend.thongke.application.enity.DoanhThuThangDTO;
import com.lapxpert.backend.thongke.application.enity.HoaDonSanPhamView;
import com.lapxpert.backend.thongke.application.enity.TongDoanhThuThangDTO;
import com.lapxpert.backend.thongke.application.repository.ThongKeDTRepository;
import com.lapxpert.backend.thongke.application.repository.ThongKeHDRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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


