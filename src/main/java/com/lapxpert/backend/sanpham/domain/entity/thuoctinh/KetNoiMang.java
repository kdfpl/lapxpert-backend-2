package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ket_noi_mang")
public class KetNoiMang {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ket_noi_mang_id_gen")
    @SequenceGenerator(name = "ket_noi_mang_id_gen", sequenceName = "ket_noi_mang_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_ket_noi", nullable = false, length = 200)
    private String moTaKetNoi;

}