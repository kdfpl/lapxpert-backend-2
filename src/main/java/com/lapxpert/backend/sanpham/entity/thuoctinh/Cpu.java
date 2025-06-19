package com.lapxpert.backend.sanpham.entity.thuoctinh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Mã CPU không được để trống")
    @Size(max = 10, message = "Mã CPU không được vượt quá 10 ký tự")
    @Column(name = "ma_cpu", nullable = false, length = 10, unique = true)
    private String maCpu;

    @Column(name = "mo_ta_cpu", nullable = false)
    private String moTaCpu;

}