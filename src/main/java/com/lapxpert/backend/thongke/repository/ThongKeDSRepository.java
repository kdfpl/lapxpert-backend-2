package com.lapxpert.backend.thongke.repository;

import com.lapxpert.backend.thongke.entity.TopDoanhSo;
import com.lapxpert.backend.thongke.entity.TopDoanhSoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface ThongKeDSRepository extends JpaRepository<TopDoanhSo, Integer> {
    @Query(value = "SELECT brand, \n" +
            "       SUM(sale) AS total_sales\n" +
            "FROM thong_ke_doanh_so_top_hang_ngay\n" +
            "WHERE sales_date BETWEEN date_trunc('month', CURRENT_DATE) AND CURRENT_DATE\n" +
            "GROUP BY brand\n" +
            "ORDER BY total_sales", nativeQuery = true)
    List<TopDoanhSoDTO> getTopDoanhSoThang();

    @Query(value = "SELECT brand, \n" +
            "       SUM(sale) AS total_sales\n" +
            "FROM thong_ke_doanh_so_top_hang_ngay\n" +
            "WHERE sales_date BETWEEN date_trunc('week', CURRENT_DATE) AND CURRENT_DATE\n" +
            "GROUP BY brand\n" +
            "ORDER BY total_sales ", nativeQuery = true)
    List<TopDoanhSoDTO> getTopDoanhSoTuan();
    @Query(value = "SELECT brand, \n" +
            "       SUM(sale) AS total_sales\n" +
            "FROM thong_ke_doanh_so_top_hang_ngay\n" +
            "WHERE sales_date = CURRENT_DATE\n" +
            "GROUP BY brand\n" +
            "ORDER BY total_sales ;", nativeQuery = true)
    List<TopDoanhSoDTO> getTopDoanhSoNgay();


    @Query(value = "SELECT brand, \n" +
            "       SUM(sale) AS total_sales\n" +
            "FROM thong_ke_doanh_so_top_hang_ngay\n" +
            "WHERE sales_date between :start_dateTop and :end_dateTop\n" +
            "GROUP BY brand\n" +
            "ORDER BY total_sales", nativeQuery = true)
    List<TopDoanhSoDTO> getTopDoanhSoCustom(LocalDate start_dateTop, LocalDate end_dateTop);
}
