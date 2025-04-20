package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "gpu")
public class Gpu {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gpu_id_gen")
    @SequenceGenerator(name = "gpu_id_gen", sequenceName = "gpu_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_gpu", nullable = false)
    private String moTaGpu;

}