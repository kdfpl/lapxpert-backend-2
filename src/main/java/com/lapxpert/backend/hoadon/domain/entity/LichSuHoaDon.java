package com.lapxpert.backend.hoadon.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lich_su_hoa_don")
public class LichSuHoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hoa_don_id", referencedColumnName = "id")
    private HoaDon hoaDon;

    @Column(name = "trang_thai")
    @Enumerated(EnumType.STRING)
    private HoaDon.TrangThaiGiaoHang trangThai;


    @Column(name = "mieu_ta")
    private String mieuTa;

    @Column(name = "thoi_gian")

    private LocalDateTime thoiGian;

    // PrePersist method for setting the current time before inserting the entity into the database
    @PrePersist
    public void prePersist() {
        if (thoiGian == null) {
            thoiGian = LocalDateTime.now(); // Set current time if thoiGian is not provided
        }

        // Đặt trạng thái mặc định là "CHO_XAC_NHAN" (Chờ xác nhận) nếu không có trạng thái
        if (trangThai == null) {
            trangThai = HoaDon.TrangThaiGiaoHang.CHO_XAC_NHAN;
        }
    }
}
