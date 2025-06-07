package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ram")
public class Ram {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ram_id_gen")
    @SequenceGenerator(name = "ram_id_gen", sequenceName = "ram_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_ram", nullable = false, length = 100)
    private String moTaRam;

}