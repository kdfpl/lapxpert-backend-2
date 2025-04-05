package com.lapxpert.backend.nguoidung.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "nguoi_dung",
        indexes = {
                @Index(name = "idx_nguoi_dung_email", columnList = "email"),
                @Index(name = "idx_nguoi_dung_so_dien_thoai", columnList = "so_dien_thoai"),
                @Index(name = "idx_nguoi_dung_vai_tro", columnList = "vai_tro")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_nguoi_dung", length = 50, unique = true)
    private String maNguoiDung;

    @Column(name = "avatar", length = 512)
    private String avatar;

    @Column(name = "ho_ten", length = 255, nullable = false)
    private String hoTen;

    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "gioi_tinh")
    private GioiTinh gioiTinh;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "cccd", length = 12, unique = true)
    private String cccd;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "so_dien_thoai", length = 20, unique = true)
    private String soDienThoai;

    @Column(name = "mat_khau", length = 255, nullable = false)
    private String matKhau;

    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "vai_tro", nullable = false)
    private VaiTro vaiTro = VaiTro.CUSTOMER;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", columnDefinition = "timestamp with time zone default CURRENT_TIMESTAMP")
    private ZonedDateTime ngayTao = ZonedDateTime.now();

    @Column(name = "ngay_cap_nhat", columnDefinition = "timestamp with time zone default CURRENT_TIMESTAMP")
    private ZonedDateTime ngayCapNhat = ZonedDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.ngayCapNhat = ZonedDateTime.now();
    }
}


