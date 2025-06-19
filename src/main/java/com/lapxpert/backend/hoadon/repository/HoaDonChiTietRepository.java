package com.lapxpert.backend.hoadon.repository;

import com.lapxpert.backend.hoadon.entity.HoaDonChiTiet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Long> {

    /**
     * Find order items eligible for review (completed orders for specific customer and product)
     * @param customerId customer ID
     * @param productId product ID
     * @return list of eligible order items
     */
    @Query("SELECT hct FROM HoaDonChiTiet hct " +
           "JOIN hct.hoaDon hd " +
           "JOIN hct.sanPhamChiTiet spc " +
           "WHERE hd.khachHang.id = :customerId " +
           "AND spc.sanPham.id = :productId " +
           "AND hd.trangThaiDonHang IN ('DA_GIAO_HANG', 'HOAN_THANH') " +
           "ORDER BY hd.ngayCapNhat DESC")
    List<HoaDonChiTiet> findEligibleForReview(@Param("customerId") Long customerId,
                                             @Param("productId") Long productId);

    /**
     * Find top selling products by quantity sold in completed orders
     * @param tuNgay Start date
     * @param denNgay End date
     * @param pageable Pagination information
     * @return Array of [sanPhamId, tenSanPham, hinhAnh, thuongHieu, soLuongBan, doanhThu]
     */
    @Query(value = "SELECT " +
           "sp.id, " +
           "sp.ten_san_pham, " +
           "CASE WHEN spc.hinh_anh IS NOT NULL AND jsonb_array_length(spc.hinh_anh) > 0 " +
           "     THEN spc.hinh_anh->>0 ELSE '' END, " +
           "CASE WHEN th.mo_ta_thuong_hieu IS NOT NULL THEN th.mo_ta_thuong_hieu ELSE 'Không có' END, " +
           "SUM(hct.so_luong), " +
           "SUM(hct.thanh_tien) " +
           "FROM hoa_don_chi_tiet hct " +
           "JOIN san_pham_chi_tiet spc ON hct.san_pham_chi_tiet_id = spc.id " +
           "JOIN san_pham sp ON spc.san_pham_id = sp.id " +
           "LEFT JOIN thuong_hieu th ON sp.thuong_hieu_id = th.id " +
           "JOIN hoa_don hd ON hct.hoa_don_id = hd.id " +
           "WHERE hd.ngay_tao BETWEEN :tuNgay AND :denNgay " +
           "AND hd.trang_thai_don_hang = 'HOAN_THANH' " +
           "GROUP BY sp.id, sp.ten_san_pham, spc.hinh_anh, th.mo_ta_thuong_hieu " +
           "ORDER BY SUM(hct.so_luong) DESC",
           nativeQuery = true)
    List<Object[]> findTopSellingProducts(@Param("tuNgay") Instant tuNgay,
                                         @Param("denNgay") Instant denNgay,
                                         Pageable pageable);

    /**
     * Find top selling categories by quantity sold in completed orders
     * @param tuNgay Start date
     * @param denNgay End date
     * @param pageable Pagination information
     * @return Array of [danhMucId, tenDanhMuc, soLuong, doanhThu]
     */
    @Query(value = "SELECT " +
           "dm.id, " +
           "dm.mo_ta_danh_muc, " +
           "SUM(hct.so_luong), " +
           "SUM(hct.thanh_tien) " +
           "FROM hoa_don_chi_tiet hct " +
           "JOIN san_pham_chi_tiet spc ON hct.san_pham_chi_tiet_id = spc.id " +
           "JOIN san_pham sp ON spc.san_pham_id = sp.id " +
           "JOIN san_pham_danh_muc spdm ON sp.id = spdm.san_pham_id " +
           "JOIN danh_muc dm ON spdm.danh_muc_id = dm.id " +
           "JOIN hoa_don hd ON hct.hoa_don_id = hd.id " +
           "WHERE hd.ngay_tao BETWEEN :tuNgay AND :denNgay " +
           "AND hd.trang_thai_don_hang = 'HOAN_THANH' " +
           "GROUP BY dm.id, dm.mo_ta_danh_muc " +
           "ORDER BY SUM(hct.so_luong) DESC",
           nativeQuery = true)
    List<Object[]> findTopSellingCategories(@Param("tuNgay") Instant tuNgay,
                                           @Param("denNgay") Instant denNgay,
                                           Pageable pageable);
}
