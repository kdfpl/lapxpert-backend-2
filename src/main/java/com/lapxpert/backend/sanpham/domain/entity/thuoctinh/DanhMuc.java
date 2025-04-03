package com.lapxpert.backend.sanpham.domain.entity.thuoctinh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

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

    @Column(name = "ma_danh_muc", nullable = false, length = 50)
    private String maDanhMuc;

    @Column(name = "ten_danh_muc", nullable = false)
    private String tenDanhMuc;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_tao")
    private OffsetDateTime ngayTao;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_cap_nhat")
    private OffsetDateTime ngayCapNhat;

}