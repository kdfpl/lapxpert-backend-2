package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Mã màu sắc không được để trống")
    @Size(max = 10, message = "Mã màu sắc không được vượt quá 10 ký tự")
    @Column(name = "ma_mau_sac", nullable = false, length = 10, unique = true)
    private String maMauSac;

    @Column(name = "mo_ta_mau_sac", nullable = false, length = 100)
    private String moTaMauSac;

}
