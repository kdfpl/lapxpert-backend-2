package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cpu")
public class Cpu {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cpu_id_gen")
    @SequenceGenerator(name = "cpu_id_gen", sequenceName = "cpu_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_cpu", nullable = false)
    private String moTaCpu;

}