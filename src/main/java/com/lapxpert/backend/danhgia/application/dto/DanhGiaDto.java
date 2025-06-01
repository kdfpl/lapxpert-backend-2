package com.lapxpert.backend.danhgia.application.dto;

import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for DanhGia entity representing product reviews
 * Follows established DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DanhGiaDto {

    private Long id;

    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;

    /**
     * User information for display
     */
    private String tenNguoiDung;

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long sanPhamId;

    /**
     * Product information for display
     */
    private String tenSanPham;

    @NotNull(message = "ID hóa đơn chi tiết không được để trống")
    private Long hoaDonChiTietId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5")
    private Integer diemDanhGia;

    @Size(max = 1000, message = "Nội dung đánh giá không được vượt quá 1000 ký tự")
    private String noiDung;

    /**
     * Optional title for the review
     */
    @Size(max = 200, message = "Tiêu đề đánh giá không được vượt quá 200 ký tự")
    private String tieuDe;

    /**
     * List of image URLs attached to the review
     */
    @Builder.Default
    private List<String> hinhAnh = new ArrayList<>();

    private TrangThaiDanhGia trangThai;

    /**
     * Standard audit fields for online modules
     */
    private Instant ngayTao;
    private Instant ngayCapNhat;

    // Business logic fields
    /**
     * Indicates if this is a verified purchase review
     */
    private boolean isVerifiedPurchase;

    /**
     * Purchase date for verification display
     */
    private Instant purchaseDate;

    /**
     * Indicates if review has images
     */
    private boolean hasImages;

    /**
     * Number of images in review
     */
    private int imageCount;

    /**
     * Indicates if review is visible to public
     */
    private boolean isVisible;

    /**
     * Indicates if review is pending approval
     */
    private boolean isPending;

    /**
     * Number of helpful votes from other users
     */
    private int helpfulVotes;

    /**
     * Total number of votes (helpful + not helpful)
     */
    private int totalVotes;

    /**
     * Moderator note if any
     */
    private String moderatorNote;

    /**
     * Check if review has text content
     * @return true if review has non-empty content
     */
    public boolean hasTextContent() {
        return noiDung != null && !noiDung.trim().isEmpty();
    }

    /**
     * Get helpfulness percentage
     * @return percentage of helpful votes
     */
    public double getHelpfulnessPercentage() {
        if (totalVotes == 0) {
            return 0.0;
        }
        return (double) helpfulVotes / totalVotes * 100.0;
    }

    /**
     * Check if review is complete (has rating and either text or images)
     * @return true if review is complete
     */
    public boolean isComplete() {
        return diemDanhGia != null &&
               diemDanhGia >= 1 &&
               diemDanhGia <= 5 &&
               (hasTextContent() || hasImages);
    }
}
