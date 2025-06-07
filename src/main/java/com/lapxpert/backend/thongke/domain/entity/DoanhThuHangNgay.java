package com.lapxpert.backend.thongke.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "thong_ke_doanh_thu_hang_ngay")
public class DoanhThuHangNgay {
    @Id

    @Column(name = "id")
    private int id;

    @Column(name = "revenue_date")
    private Date revenueDate;

    @Column(name = "brand")
    private String brand;

    @Column(name = "revenue")
    private int revenue;
}
