package com.lapxpert.backend.sanpham.entity.thuoctinh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "danh_muc")
public class DanhMuc {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "danh_muc_id_gen")
    @SequenceGenerator(name = "danh_muc_id_gen", sequenceName = "danh_muc_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Mã danh mục không được để trống")
    @Size(max = 10, message = "Mã danh mục không được vượt quá 10 ký tự")
    @Column(name = "ma_danh_muc", nullable = false, length = 10, unique = true)
    private String maDanhMuc;

    @Column(name = "mo_ta_danh_muc", nullable = false)
    private String moTaDanhMuc;
}