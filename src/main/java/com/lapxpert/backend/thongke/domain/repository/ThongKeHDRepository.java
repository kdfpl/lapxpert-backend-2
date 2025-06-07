package com.lapxpert.backend.thongke.domain.repository;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.thongke.domain.entity.HoaDonSanPhamView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThongKeHDRepository extends JpaRepository<HoaDon, Long> {
    @Query( nativeQuery = true, value = "SELECT hd.id,\n" +
            "    sp.ten_san_pham,\n" +
            "    hd.ngay_tao,\n" +
            "    hdct.gia_ban,\n" +
            "    hd.trang_thai_don_hang\n" +
            "FROM hoa_don hd\n" +
            "INNER JOIN hoa_don_chi_tiet hdct ON hd.id = hdct.hoa_don_id\n" +
            "INNER JOIN san_pham_chi_tiet spct ON hdct.san_pham_chi_tiet_id = spct.id\n" +
            "INNER JOIN san_pham sp ON spct.san_pham_id = sp.id;")
    List<HoaDonSanPhamView> findAllhaveSP();


    @Query(value = """
    SELECT
        sp.ten_san_pham AS tenSanPham,
        hd.ngay_tao AS ngayTao,
        hdct.gia_ban AS giaBan,
        hd.trang_thai_don_hang AS trangThaiDonHang
    FROM hoa_don hd
    INNER JOIN hoa_don_chi_tiet hdct ON hd.id = hdct.hoa_don_id
    INNER JOIN san_pham_chi_tiet spct ON hdct.san_pham_chi_tiet_id = spct.id
    INNER JOIN san_pham sp ON spct.san_pham_id = sp.id
    WHERE hd.trang_thai_don_hang = :trangThaiDonHang
    """, nativeQuery = true)
    List<HoaDonSanPhamView> findByTrangThaiDonHang(@Param("trangThaiDonHang") String trangThaiDonHang);



}
