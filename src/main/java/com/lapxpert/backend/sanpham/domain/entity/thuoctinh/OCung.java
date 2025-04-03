package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "o_cung")
public class OCung {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "o_cung_id_gen")
    @SequenceGenerator(name = "o_cung_id_gen", sequenceName = "o_cung_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_o_cung", nullable = false, length = 150)
    private String moTaOCung;

}