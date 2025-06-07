package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mau_sac")
public class MauSac {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mau_sac_id_gen")
    @SequenceGenerator(name = "mau_sac_id_gen", sequenceName = "mau_sac_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_mau_sac", nullable = false, length = 100)
    private String moTaMauSac;

}
