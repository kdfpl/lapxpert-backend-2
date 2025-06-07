package com.lapxpert.backend.danhgia.domain.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonChiTiet;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing product reviews from verified purchasers
 * Supports ratings, text reviews, and image attachments
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "danh_gia",
    indexes = {
        @Index(name = "idx_danh_gia_nguoi_dung", columnList = "nguoi_dung_id"),
        @Index(name = "idx_danh_gia_san_pham", columnList = "san_pham_id"),
        @Index(name = "idx_danh_gia_trang_thai", columnList = "trang_thai"),
        @Index(name = "idx_danh_gia_diem", columnList = "diem_danh_gia"),
        @Index(name = "idx_danh_gia_ngay_tao", columnList = "ngay_tao")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_hoa_don_chi_tiet",
                         columnNames = {"hoa_don_chi_tiet_id"})
    })
public class DanhGia extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "danh_gia_id_gen")
    @SequenceGenerator(name = "danh_gia_id_gen", sequenceName = "danh_gia_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    /**
     * Reference to the order item that allows this review
     * Ensures only verified purchasers can review
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoa_don_chi_tiet_id", nullable = false, unique = true)
    private HoaDonChiTiet hoaDonChiTiet;

    @Column(name = "diem_danh_gia", nullable = false)
    private Integer diemDanhGia;

    @Column(name = "noi_dung", length = 1000)
    private String noiDung;

    /**
     * Optional title for the review
     */
    @Column(name = "tieu_de", length = 200)
    private String tieuDe;

    /**
     * List of image URLs attached to the review
     */
    @Column(name = "hinh_anh", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<String> hinhAnh = new ArrayList<>();

    @Column(name = "trang_thai", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TrangThaiDanhGia trangThai = TrangThaiDanhGia.CHO_DUYET;

    /**
     * Check if review is visible to public
     * @return true if review can be displayed
     */
    public boolean isVisible() {
        return trangThai != null && trangThai.isVisible();
    }

    /**
     * Check if review is pending approval
     * @return true if review needs moderation
     */
    public boolean isPending() {
        return trangThai != null && trangThai.isPending();
    }

    /**
     * Check if review was rejected or hidden
     * @return true if review is not visible
     */
    public boolean isHidden() {
        return trangThai != null && trangThai.isHidden();
    }

    /**
     * Approve the review for public display
     */
    public void approve() {
        this.trangThai = TrangThaiDanhGia.DA_DUYET;
    }

    /**
     * Reject the review
     */
    public void reject() {
        this.trangThai = TrangThaiDanhGia.BI_TU_CHOI;
    }

    /**
     * Hide the review from public view
     */
    public void hide() {
        this.trangThai = TrangThaiDanhGia.DA_AN;
    }

    /**
     * Check if review has images
     * @return true if review contains images
     */
    public boolean hasImages() {
        return hinhAnh != null && !hinhAnh.isEmpty();
    }

    /**
     * Get number of images in review
     * @return image count
     */
    public int getImageCount() {
        return hinhAnh != null ? hinhAnh.size() : 0;
    }

    /**
     * Add image to review
     * @param imageUrl URL of the image to add
     */
    public void addImage(String imageUrl) {
        if (hinhAnh == null) {
            hinhAnh = new ArrayList<>();
        }
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            hinhAnh.add(imageUrl.trim());
        }
    }

    /**
     * Remove image from review
     * @param imageUrl URL of the image to remove
     */
    public void removeImage(String imageUrl) {
        if (hinhAnh != null) {
            hinhAnh.remove(imageUrl);
        }
    }

    /**
     * Check if review has text content
     * @return true if review has non-empty content
     */
    public boolean hasTextContent() {
        return noiDung != null && !noiDung.trim().isEmpty();
    }

    /**
     * Get reviewer name for display
     * @return reviewer's name or anonymous if not available
     */
    public String getReviewerName() {
        return nguoiDung != null && nguoiDung.getHoTen() != null
            ? nguoiDung.getHoTen()
            : "Anonymous";
    }

    /**
     * Get product name for display
     * @return product name or empty string if not available
     */
    public String getProductName() {
        return sanPham != null ? sanPham.getTenSanPham() : "";
    }

    /**
     * Check if this is a verified purchase review
     * @return true if review is from verified purchase
     */
    public boolean isVerifiedPurchase() {
        return hoaDonChiTiet != null;
    }

    /**
     * Get purchase date for verification display
     * @return purchase date or null if not available
     */
    public Instant getPurchaseDate() {
        return hoaDonChiTiet != null ? hoaDonChiTiet.getNgayTao() : null;
    }

    /**
     * Validate review before persist
     */
    @PrePersist
    public void onPrePersist() {
        validateReview();
    }

    /**
     * Validate review before update
     */
    @PreUpdate
    public void onPreUpdate() {
        validateReview();
    }

    /**
     * Private validation method
     */
    private void validateReview() {
        if (diemDanhGia == null || diemDanhGia < 1 || diemDanhGia > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (nguoiDung == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (sanPham == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (hoaDonChiTiet == null) {
            throw new IllegalArgumentException("Order item cannot be null - only verified purchases can review");
        }
        if (noiDung != null && noiDung.length() > 1000) {
            throw new IllegalArgumentException("Review content cannot exceed 1000 characters");
        }
    }
}
