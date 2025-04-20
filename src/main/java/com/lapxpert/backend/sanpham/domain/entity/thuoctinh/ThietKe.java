package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "thiet_ke")
public class ThietKe {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "thiet_ke_id_gen")
    @SequenceGenerator(name = "thiet_ke_id_gen", sequenceName = "thiet_ke_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_thiet_ke", nullable = false, length = 300)
    private String moTaThietKe;

}