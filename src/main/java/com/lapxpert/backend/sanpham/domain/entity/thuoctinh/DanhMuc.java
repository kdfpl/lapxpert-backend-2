package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "danh_muc")
public class DanhMuc {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "danh_muc_id_gen")
    @SequenceGenerator(name = "danh_muc_id_gen", sequenceName = "danh_muc_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_danh_muc", nullable = false)
    private String moTaDanhMuc;
}