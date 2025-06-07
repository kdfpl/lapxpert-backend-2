package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "man_hinh")
public class ManHinh {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "man_hinh_id_gen")
    @SequenceGenerator(name = "man_hinh_id_gen", sequenceName = "man_hinh_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_man_hinh", nullable = false, length = 300)
    private String moTaManHinh;

}