package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ban_phim")
public class BanPhim {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ban_phim_id_gen")
    @SequenceGenerator(name = "ban_phim_id_gen", sequenceName = "ban_phim_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_ban_phim", nullable = false, length = 200)
    private String moTaBanPhim;

}