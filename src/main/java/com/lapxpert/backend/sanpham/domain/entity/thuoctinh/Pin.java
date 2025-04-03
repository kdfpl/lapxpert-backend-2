package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pin")
public class Pin {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pin_id_gen")
    @SequenceGenerator(name = "pin_id_gen", sequenceName = "pin_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mo_ta_pin", nullable = false, length = 150)
    private String moTaPin;

}