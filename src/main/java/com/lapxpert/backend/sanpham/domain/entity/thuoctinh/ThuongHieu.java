package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "thuong_hieu")
public class ThuongHieu {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "thuong_hieu_id_gen")
    @SequenceGenerator(name = "thuong_hieu_id_gen", sequenceName = "thuong_hieu_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Mã thương hiệu không được để trống")
    @Size(max = 10, message = "Mã thương hiệu không được vượt quá 10 ký tự")
    @Column(name = "ma_thuong_hieu", nullable = false, length = 10, unique = true)
    private String maThuongHieu;

    @Column(name = "mo_ta_thuong_hieu", nullable = false, length = 300)
    private String moTaThuongHieu;

}