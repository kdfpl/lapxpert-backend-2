package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "thuong_hieu")
public class ThuongHieu {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "thuong_hieu_id_gen")
    @SequenceGenerator(name = "thuong_hieu_id_gen", sequenceName = "thuong_hieu_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_thuong_hieu", nullable = false, length = 300)
    private String moTaThuongHieu;

}