package com.lapxpert.backend.thongke.application.repository;

import com.lapxpert.backend.hoadon.enity.HoaDon;
import com.lapxpert.backend.thongke.application.enity.DoanhThuHangNgay;
import com.lapxpert.backend.thongke.application.enity.DoanhThuThangDTO;
import com.lapxpert.backend.thongke.application.enity.HoaDonSanPhamView;
import com.lapxpert.backend.thongke.application.enity.TongDoanhThuThangDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ThongKeDTRepository extends JpaRepository<DoanhThuHangNgay, Integer> {
@Query(nativeQuery = true, value = "SELECT \n" +
        "    DATE_TRUNC('month', revenue_date) AS month,\n" +
        "    SUM(revenue) AS total_revenue\n" +
        "FROM \n" +
        "    thong_ke_doanh_thu_hang_ngay\n" +
        "GROUP BY \n" +
        "    DATE_TRUNC('month', revenue_date)\n" +
        "ORDER BY \n" +
        "    month;")
    List<DoanhThuHangNgay> getTongDoanhThuHangThang();

    @Query(value = """
    SELECT 
        brand,
        TO_CHAR(revenue_date, 'YYYY-MM') AS month,
        SUM(revenue) AS total_revenue,
        MIN(id) AS sample_id
    FROM thong_ke_doanh_thu_hang_ngay
    WHERE revenue_date >= DATE_TRUNC('year', CURRENT_DATE)
    GROUP BY brand, TO_CHAR(revenue_date, 'YYYY-MM')
    ORDER BY brand, month
""", nativeQuery = true)
    List<DoanhThuThangDTO> getDoanhThuTungHangTrongNamNay();

@Query(nativeQuery = true,value = "SELECT *\n" +
        "FROM thong_ke_doanh_thu_hang_ngay\n" +
        "WHERE revenue_date IN (\n" +
        "    SELECT revenue_date\n" +
        "    FROM thong_ke_doanh_thu_hang_ngay\n" +
        "    WHERE revenue_date BETWEEN DATE_TRUNC('month', CURRENT_DATE) AND CURRENT_DATE\n" +
        "    GROUP BY revenue_date\n" +
        "    HAVING COUNT(DISTINCT brand) = 7\n" +
        ")\n" +
        "ORDER BY revenue_date, brand;\n")
List<DoanhThuHangNgay> getDoanhThuTungHangTrongThangNay();

    @Query(nativeQuery = true,value = "SELECT *\n" +
            "FROM thong_ke_doanh_thu_hang_ngay\n" +
            "WHERE revenue_date IN (\n" +
            "    SELECT revenue_date\n" +
            "    FROM thong_ke_doanh_thu_hang_ngay\n" +
            "    WHERE revenue_date BETWEEN DATE_TRUNC('week', CURRENT_DATE) AND CURRENT_DATE\n" +
            "    GROUP BY revenue_date\n" +
            "    HAVING COUNT(DISTINCT brand) = 7\n" +
            ")\n" +
            "ORDER BY revenue_date, brand;\n")
    List<DoanhThuHangNgay> getDoanhThuTungHangTrongTuanNay();

@Query(nativeQuery = true, value = "SELECT *\n" +
        "FROM thong_ke_doanh_thu_hang_ngay\n" +
        "WHERE revenue_date IN (\n" +
        "    SELECT revenue_date\n" +
        "    FROM thong_ke_doanh_thu_hang_ngay\n" +
        "    WHERE revenue_date BETWEEN :start_date AND :end_date\n" +
        "    GROUP BY revenue_date\n" +
        "    HAVING COUNT(DISTINCT brand) = 7\n" +
        ")\n" +
        "ORDER BY revenue_date, brand;\n")
    List<DoanhThuHangNgay> getDoanhThuCustomTime(LocalDate start_date, LocalDate end_date);




@Query(nativeQuery = true, value = "SELECT \n" +
//        "    revenue_date,\n" +
        "    SUM(revenue) AS total_revenue\n" +
        "FROM \n" +
        "    thong_ke_doanh_thu_hang_ngay\n" +
        "    WHERE revenue_date BETWEEN DATE_TRUNC('month', CURRENT_DATE) AND CURRENT_DATE\n" +
        "GROUP BY \n" +
        "    revenue_date\n" +
        "ORDER BY \n" +
        "    revenue_date;\n")
    List<Integer> getTongDoanhThuTungNgayTrongThangNay ();



@Query(nativeQuery = true, value = "SELECT \n" +
        "    DATE_TRUNC('month', revenue_date) AS month,\n" +
        "    SUM(revenue) AS total_revenue\n" +
        "FROM \n" +
        "    thong_ke_doanh_thu_hang_ngay\n" +
        "WHERE \n" +
        "    revenue_date >= DATE_TRUNC('year', CURRENT_DATE)  -- Từ đầu năm\n" +
        "    AND revenue_date <= CURRENT_DATE                   -- Đến hiện tại\n" +
        "GROUP BY \n" +
        "    DATE_TRUNC('month', revenue_date)\n" +
        "ORDER BY \n" +
        "    month;\n")
    List<TongDoanhThuThangDTO> getTongDoanhThuTungThangTrongNamNay();




    @Query(nativeQuery = true, value = "SELECT \n" +
            "    SUM(revenue) AS total_revenue\n" +
            "FROM \n" +
            "    thong_ke_doanh_thu_hang_ngay\n" +
            "    WHERE revenue_date BETWEEN DATE_TRUNC('week', CURRENT_DATE) AND CURRENT_DATE\n" +
            "GROUP BY \n" +
            "    revenue_date\n" +
            "ORDER BY \n" +
            "    revenue_date")
    List<Integer> getTongDoanhThuTungNgayTrongTuanNay ();



    @Query(nativeQuery = true, value = "SELECT \n" +
//            "    revenue_date,\n" +
            "    SUM(revenue) AS total_revenue\n" +
            "FROM \n" +
            "    thong_ke_doanh_thu_hang_ngay\n" +
            "WHERE \n" +
            "    revenue_date BETWEEN  :start_date AND :end_date\n" +
            "GROUP BY \n" +
            "    revenue_date\n" +
            "ORDER BY \n" +
            "    revenue_date ;")
    List<Integer> getTongDoanhThuTungNgayCustom(LocalDate start_date, LocalDate end_date);



}
