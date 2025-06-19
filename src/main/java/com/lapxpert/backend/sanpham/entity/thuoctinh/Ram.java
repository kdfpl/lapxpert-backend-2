package com.lapxpert.backend.sanpham.entity.thuoctinh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Mã RAM không được để trống")
    @Size(max = 10, message = "Mã RAM không được vượt quá 10 ký tự")
    @Column(name = "ma_ram", nullable = false, length = 10, unique = true)
    private String maRam;

    @Column(name = "mo_ta_ram", nullable = false, length = 100)
    private String moTaRam;

}