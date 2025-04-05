package com.lapxpert.backend.sanpham.application.dto;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SanPhamChiTietDto implements Serializable {
    private Long id;
    private String sku;
    private String mauSac;
    private Integer soLuongTonKho;
    private BigDecimal giaBan;
    private BigDecimal giaKhuyenMai;
    private List<String> hinhAnh;
    private Boolean trangThai;
    private Cpu cpu;
    private Ram ram;
    private OCung oCung;
    private Gpu gpu;
    private ManHinh manHinh;
    private CongGiaoTiep congGiaoTiep;
    private BanPhim banPhim;
    private KetNoiMang ketNoiMang;
    private AmThanh amThanh;
    private Webcam webcam;
    private BaoMat baoMat;
    private HeDieuHanh heDieuHanh;
    private ThietKe thietKe;
    private Pin pin;
}
