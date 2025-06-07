package com.lapxpert.backend.sanpham.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO for batch status update request
 * Used for updating multiple products' status in a single operation
 */
@Data
public class BatchStatusUpdateRequest {
    
    @NotEmpty(message = "Danh sách ID sản phẩm không được để trống")
    private List<Long> productIds;
    
    @NotNull(message = "Trạng thái không được để trống")
    private Boolean trangThai;
    
    @NotNull(message = "Lý do thay đổi không được để trống")
    @Size(min = 1, max = 500, message = "Lý do thay đổi phải từ 1 đến 500 ký tự")
    private String lyDoThayDoi;
}
