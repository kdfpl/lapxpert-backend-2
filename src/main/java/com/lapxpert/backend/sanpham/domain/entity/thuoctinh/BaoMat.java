package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bao_mat")
public class BaoMat {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bao_mat_id_gen")
    @SequenceGenerator(name = "bao_mat_id_gen", sequenceName = "bao_mat_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_bao_mat", nullable = false, length = 200)
    private String moTaBaoMat;

}