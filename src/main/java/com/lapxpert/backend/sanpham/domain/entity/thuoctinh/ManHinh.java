package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "man_hinh")
public class ManHinh {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "man_hinh_id_gen")
    @SequenceGenerator(name = "man_hinh_id_gen", sequenceName = "man_hinh_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Mã màn hình không được để trống")
    @Size(max = 10, message = "Mã màn hình không được vượt quá 10 ký tự")
    @Column(name = "ma_man_hinh", nullable = false, length = 10, unique = true)
    private String maManHinh;

    @Column(name = "mo_ta_man_hinh", nullable = false, length = 300)
    private String moTaManHinh;

}