package com.lapxpert.backend.nguoidung.domain.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nguoi_dung",
        indexes = {
                @Index(name = "idx_nguoi_dung_email", columnList = "email"),
                @Index(name = "idx_nguoi_dung_so_dien_thoai", columnList = "so_dien_thoai"),
                @Index(name = "idx_nguoi_dung_vai_tro", columnList = "vai_tro"),
                @Index(name = "idx_nguoi_dung_trang_thai", columnList = "trang_thai")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiDung extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nguoi_dung_id_gen")
    @SequenceGenerator(name = "nguoi_dung_id_gen", sequenceName = "nguoi_dung_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ma_nguoi_dung", length = 50, unique = true)
    private String maNguoiDung;

    @Size(max = 512, message = "URL avatar không được vượt quá 512 ký tự")
    @Column(name = "avatar", length = 512)
    private String avatar;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    @Column(name = "ho_ten", length = 255, nullable = false)
    private String hoTen;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "gioi_tinh")
    private GioiTinh gioiTinh;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Size(max = 12, message = "CCCD không được vượt quá 12 ký tự")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "CCCD phải có từ 9-12 chữ số")
    @Column(name = "cccd", length = 12, unique = true)
    private String cccd;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    @Column(name = "email", length = 255, nullable = true, unique = true)
    private String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    @Column(name = "so_dien_thoai", length = 20, unique = true)
    private String soDienThoai;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 255, message = "Mật khẩu phải có từ 6-255 ký tự")
    @Column(name = "mat_khau", length = 255, nullable = false)
    private String matKhau;

    @NotNull(message = "Vai trò không được để trống")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "vai_tro", nullable = false)
    @Builder.Default
    private VaiTro vaiTro = VaiTro.CUSTOMER;

    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private TrangThaiNguoiDung trangThai = TrangThaiNguoiDung.HOAT_DONG;

    // Bidirectional relationships
    @OneToMany(mappedBy = "nguoiDung", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiaChi> diaChis = new ArrayList<>();

    @OneToMany(mappedBy = "khachHang", fetch = FetchType.LAZY)
    @Builder.Default
    private List<com.lapxpert.backend.hoadon.domain.entity.HoaDon> hoaDonsAsCustomer = new ArrayList<>();

    @OneToMany(mappedBy = "nhanVien", fetch = FetchType.LAZY)
    @Builder.Default
    private List<com.lapxpert.backend.hoadon.domain.entity.HoaDon> hoaDonsAsStaff = new ArrayList<>();

    @OneToMany(mappedBy = "nguoiDung", fetch = FetchType.LAZY)
    @Builder.Default
    private List<com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung> phieuGiamGias = new ArrayList<>();

    @OneToOne(mappedBy = "nguoiDung", cascade = CascadeType.ALL, orphanRemoval = true)
    private com.lapxpert.backend.giohang.domain.entity.GioHang gioHang;

    @OneToMany(mappedBy = "nguoiDung", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.lapxpert.backend.danhsachyeuthich.domain.entity.DanhSachYeuThich> danhSachYeuThichs = new ArrayList<>();

    @OneToMany(mappedBy = "nguoiDung", fetch = FetchType.LAZY)
    @Builder.Default
    private List<com.lapxpert.backend.danhgia.domain.entity.DanhGia> danhGias = new ArrayList<>();

    /**
     * Check if user is an admin
     * @return true if user has admin role
     */
    public boolean isAdmin() {
        return vaiTro == VaiTro.ADMIN;
    }

    /**
     * Check if user is staff
     * @return true if user has staff role
     */
    public boolean isStaff() {
        return vaiTro == VaiTro.STAFF;
    }

    /**
     * Check if user is customer
     * @return true if user has customer role
     */
    public boolean isCustomer() {
        return vaiTro == VaiTro.CUSTOMER;
    }

    /**
     * Check if user account is active
     * @return true if account is active
     */
    public boolean isActive() {
        return trangThai != null && trangThai.isActive();
    }

    /**
     * Activate user account
     */
    public void activate() {
        this.trangThai = TrangThaiNguoiDung.HOAT_DONG;
    }

    /**
     * Deactivate user account
     */
    public void deactivate() {
        this.trangThai = TrangThaiNguoiDung.KHONG_HOAT_DONG;
    }

    /**
     * Check if user can modify security-critical fields
     * Only admins can modify certain fields for other users
     * @param currentUser the user making the modification
     * @return true if modification is allowed
     */
    public boolean canModifySecurityFields(NguoiDung currentUser) {
        // Users can modify their own non-security fields
        if (this.equals(currentUser)) {
            return false; // Cannot modify own security fields (id, maNguoiDung, vaiTro)
        }
        // Only admins can modify other users' security fields
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Check if user can modify profile information
     * @param currentUser the user making the modification
     * @return true if modification is allowed
     */
    public boolean canModifyProfile(NguoiDung currentUser) {
        // Users can modify their own profile
        if (this.equals(currentUser)) {
            return true;
        }
        // Admins can modify any profile
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Get default address for this user
     * @return default address or null if none set
     */
    public DiaChi getDefaultAddress() {
        return diaChis.stream()
            .filter(DiaChi::getLaMacDinh)
            .findFirst()
            .orElse(null);
    }

    /**
     * Add address to user
     * @param diaChi address to add
     */
    public void addAddress(DiaChi diaChi) {
        diaChi.setNguoiDung(this);
        diaChis.add(diaChi);
    }

    /**
     * Remove address from user
     * @param diaChi address to remove
     */
    public void removeAddress(DiaChi diaChi) {
        diaChis.remove(diaChi);
        diaChi.setNguoiDung(null);
    }
}
