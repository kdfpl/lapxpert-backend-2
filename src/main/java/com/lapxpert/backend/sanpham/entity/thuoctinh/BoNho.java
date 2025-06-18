package com.lapxpert.backend.sanpham.entity.thuoctinh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bo_nho")
public class BoNho {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bo_nho_id_gen")
    @SequenceGenerator(name = "bo_nho_id_gen", sequenceName = "bo_nho_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Mã bộ nhớ không được để trống")
    @Size(max = 10, message = "Mã bộ nhớ không được vượt quá 10 ký tự")
    @Column(name = "ma_bo_nho", nullable = false, length = 10, unique = true)
    private String maBoNho;

    @Column(name = "mo_ta_bo_nho", nullable = false, length = 150)
    private String moTaBoNho;

}