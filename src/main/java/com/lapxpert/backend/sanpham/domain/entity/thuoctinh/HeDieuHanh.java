package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "he_dieu_hanh")
public class HeDieuHanh {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "he_dieu_hanh_id_gen")
    @SequenceGenerator(name = "he_dieu_hanh_id_gen", sequenceName = "he_dieu_hanh_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_he_dieu_hanh", nullable = false, length = 100)
    private String moTaHeDieuHanh;

}