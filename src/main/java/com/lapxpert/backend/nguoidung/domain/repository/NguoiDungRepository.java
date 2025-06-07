package com.lapxpert.backend.nguoidung.domain.repository;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import com.lapxpert.backend.nguoidung.domain.entity.TrangThaiNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    List<NguoiDung> findByVaiTro(VaiTro vaiTro);

    // Find customers with addresses eagerly loaded
    @Query("SELECT DISTINCT n FROM NguoiDung n LEFT JOIN FETCH n.diaChis WHERE n.vaiTro = :vaiTro")
    List<NguoiDung> findByVaiTroWithAddresses(@Param("vaiTro") VaiTro vaiTro);

    // Find user by ID with addresses eagerly loaded
    @Query("SELECT n FROM NguoiDung n LEFT JOIN FETCH n.diaChis WHERE n.id = :id")
    Optional<NguoiDung> findByIdWithAddresses(@Param("id") Long id);

    Optional<NguoiDung> findTopByMaNguoiDungStartingWithOrderByMaNguoiDungDesc(String maNguoiDungPrefix);

    Optional<NguoiDung> findByEmail(String email);
    // Optional<NguoiDung> findByEmailAndMatKhau(String email,String matKhau); // Removed as unused and potentially insecure

    Optional<NguoiDung> findBySoDienThoai(String phone);

    Optional<NguoiDung> findByCccd(String cccd);

    List<NguoiDung> findByVaiTroIn(List<VaiTro> asList);

    // Search customers by name, phone, or email with addresses eagerly loaded
    @Query("SELECT DISTINCT n FROM NguoiDung n LEFT JOIN FETCH n.diaChis WHERE n.vaiTro = :vaiTro AND " +
           "(LOWER(n.hoTen) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.soDienThoai) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.maNguoiDung) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<NguoiDung> searchByVaiTroAndTermWithAddresses(@Param("vaiTro") VaiTro vaiTro, @Param("searchTerm") String searchTerm);

    // Original search method without addresses (for performance when addresses not needed)
    @Query("SELECT n FROM NguoiDung n WHERE n.vaiTro = :vaiTro AND " +
           "(LOWER(n.hoTen) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.soDienThoai) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.maNguoiDung) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<NguoiDung> searchByVaiTroAndTerm(@Param("vaiTro") VaiTro vaiTro, @Param("searchTerm") String searchTerm);

    // ==================== CUSTOMER STATISTICS QUERIES ====================

    /**
     * Count customers by role and status
     */
    Long countByVaiTroAndTrangThai(VaiTro vaiTro, TrangThaiNguoiDung trangThai);

    /**
     * Count new customers registered between dates
     */
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.vaiTro = :vaiTro " +
           "AND n.ngayTao BETWEEN :tuNgay AND :denNgay")
    Long countNewCustomersBetween(@Param("vaiTro") VaiTro vaiTro,
                                  @Param("tuNgay") Instant tuNgay,
                                  @Param("denNgay") Instant denNgay);

    /**
     * Get daily new customer registration counts
     */
    @Query(value = "SELECT DATE(n.ngay_tao) as ngay, COUNT(*) as so_luong " +
           "FROM nguoi_dung n " +
           "WHERE n.vai_tro = :vaiTro " +
           "AND n.ngay_tao BETWEEN :tuNgay AND :denNgay " +
           "GROUP BY DATE(n.ngay_tao) " +
           "ORDER BY ngay",
           nativeQuery = true)
    List<Object[]> getDailyNewCustomerCounts(@Param("vaiTro") String vaiTro,
                                            @Param("tuNgay") Instant tuNgay,
                                            @Param("denNgay") Instant denNgay);

    /**
     * Find customers who made their first order in a period
     */
    @Query("SELECT DISTINCT n FROM NguoiDung n " +
           "JOIN n.hoaDonsAsCustomer h " +
           "WHERE n.vaiTro = :vaiTro " +
           "AND h.ngayTao BETWEEN :tuNgay AND :denNgay " +
           "AND NOT EXISTS (SELECT h2 FROM HoaDon h2 WHERE h2.khachHang = n AND h2.ngayTao < :tuNgay)")
    List<NguoiDung> findFirstTimeCustomers(@Param("vaiTro") VaiTro vaiTro,
                                          @Param("tuNgay") Instant tuNgay,
                                          @Param("denNgay") Instant denNgay);

    /**
     * Find customers who made repeat purchases
     */
    @Query("SELECT DISTINCT n FROM NguoiDung n " +
           "JOIN n.hoaDonsAsCustomer h " +
           "WHERE n.vaiTro = :vaiTro " +
           "AND h.ngayTao BETWEEN :tuNgay AND :denNgay " +
           "AND EXISTS (SELECT h2 FROM HoaDon h2 WHERE h2.khachHang = n AND h2.ngayTao < :tuNgay)")
    List<NguoiDung> findReturningCustomers(@Param("vaiTro") VaiTro vaiTro,
                                          @Param("tuNgay") Instant tuNgay,
                                          @Param("denNgay") Instant denNgay);

}
