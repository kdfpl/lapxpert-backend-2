package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "am_thanh")
public class AmThanh {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "am_thanh_id_gen")
    @SequenceGenerator(name = "am_thanh_id_gen", sequenceName = "am_thanh_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_am_thanh", nullable = false, length = 200)
    private String moTaAmThanh;

}