package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cong_giao_tiep")
public class CongGiaoTiep {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cong_giao_tiep_id_gen")
    @SequenceGenerator(name = "cong_giao_tiep_id_gen", sequenceName = "cong_giao_tiep_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_cong", nullable = false, length = 512)
    private String moTaCong;

}