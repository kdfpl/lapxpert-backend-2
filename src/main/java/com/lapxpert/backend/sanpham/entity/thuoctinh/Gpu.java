package com.lapxpert.backend.sanpham.entity.thuoctinh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "gpu")
public class Gpu {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gpu_id_gen")
    @SequenceGenerator(name = "gpu_id_gen", sequenceName = "gpu_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Mã GPU không được để trống")
    @Size(max = 10, message = "Mã GPU không được vượt quá 10 ký tự")
    @Column(name = "ma_gpu", nullable = false, length = 10, unique = true)
    private String maGpu;

    @Column(name = "mo_ta_gpu", nullable = false)
    private String moTaGpu;

}