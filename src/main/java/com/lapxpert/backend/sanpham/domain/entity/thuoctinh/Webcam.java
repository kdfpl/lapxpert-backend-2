package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "webcam")
public class Webcam {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webcam_id_gen")
    @SequenceGenerator(name = "webcam_id_gen", sequenceName = "webcam_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_wc", nullable = false, length = 200)
    private String moTaWc;

}