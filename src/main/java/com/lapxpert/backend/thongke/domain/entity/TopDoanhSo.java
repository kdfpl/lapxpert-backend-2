package com.lapxpert.backend.thongke.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "thong_ke_doanh_so_top_hang_ngay")
public class TopDoanhSo {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "sales_date")
    private Date salesDate;

    @Column(name = "brand")
    private String brand;

    @Column(name = "sale")
    private Integer sale;
}
