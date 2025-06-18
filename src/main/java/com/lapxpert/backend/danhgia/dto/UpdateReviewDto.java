package com.lapxpert.backend.danhgia.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for updating existing product reviews
 * Follows established request DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewDto {

    @NotNull(message = "ID đánh giá không được để trống")
    private Long reviewId;

    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5")
    private Integer diemDanhGia;

    @Size(max = 1000, message = "Nội dung đánh giá không được vượt quá 1000 ký tự")
    private String noiDung;

    /**
     * List of image URLs to attach to the review
     * Maximum 5 images per review
     */
    @Size(max = 5, message = "Tối đa 5 hình ảnh cho mỗi đánh giá")
    @Builder.Default
    private List<String> hinhAnh = new ArrayList<>();

    /**
     * Optional: Title for the review
     */
    @Size(max = 200, message = "Tiêu đề đánh giá không được vượt quá 200 ký tự")
    private String tieuDe;

    /**
     * Optional: Reason for updating the review
     */
    @Size(max = 255, message = "Lý do cập nhật không được vượt quá 255 ký tự")
    private String lyDoCapNhat;

    /**
     * Validation method to ensure review has content
     */
    @AssertTrue(message = "Đánh giá phải có nội dung văn bản hoặc hình ảnh")
    public boolean isHasContent() {
        boolean hasText = noiDung != null && !noiDung.trim().isEmpty();
        boolean hasImages = hinhAnh != null && !hinhAnh.isEmpty();
        return hasText || hasImages;
    }
}
