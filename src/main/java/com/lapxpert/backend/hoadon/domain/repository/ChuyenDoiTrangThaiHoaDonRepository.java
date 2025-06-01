package com.lapxpert.backend.hoadon.domain.repository;

import com.lapxpert.backend.hoadon.domain.entity.ChuyenDoiTrangThaiHoaDon;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho entity ChuyenDoiTrangThaiHoaDon.
 * Cung cấp các phương thức để truy vấn quy tắc chuyển đổi trạng thái hợp lệ và quy tắc kinh doanh.
 */
@Repository
public interface ChuyenDoiTrangThaiHoaDonRepository extends JpaRepository<ChuyenDoiTrangThaiHoaDon, Long> {

    /**
     * Tìm quy tắc chuyển đổi trạng thái cụ thể
     * 
     * @param trangThaiTu Trạng thái hiện tại
     * @param trangThaiDen Trạng thái đích
     * @return Optional chứa quy tắc chuyển đổi nếu tồn tại
     */
    Optional<ChuyenDoiTrangThaiHoaDon> findByTrangThaiTuAndTrangThaiDen(
        TrangThaiDonHang trangThaiTu, 
        TrangThaiDonHang trangThaiDen
    );

    /**
     * Tìm tất cả các chuyển đổi được phép từ một trạng thái cụ thể
     * 
     * @param trangThaiTu Trạng thái hiện tại
     * @return Danh sách các chuyển đổi được phép
     */
    @Query("SELECT cdtt FROM ChuyenDoiTrangThaiHoaDon cdtt WHERE cdtt.trangThaiTu = :trangThaiTu AND cdtt.choPhep = true")
    List<ChuyenDoiTrangThaiHoaDon> timCacChuyenDoiChoPhepTu(@Param("trangThaiTu") TrangThaiDonHang trangThaiTu);

    /**
     * Tìm tất cả các chuyển đổi được phép từ một trạng thái cụ thể cho một vai trò nhất định
     * 
     * @param trangThaiTu Trạng thái hiện tại
     * @param vaiTroNguoiDung Vai trò của người dùng
     * @return Danh sách các chuyển đổi được phép cho vai trò
     */
    @Query("SELECT cdtt FROM ChuyenDoiTrangThaiHoaDon cdtt WHERE cdtt.trangThaiTu = :trangThaiTu " +
           "AND cdtt.choPhep = true " +
           "AND (cdtt.vaiTroYeuCau IS NULL OR cdtt.vaiTroYeuCau = :vaiTroNguoiDung OR :vaiTroNguoiDung = 'ADMIN')")
    List<ChuyenDoiTrangThaiHoaDon> timCacChuyenDoiChoPhepChoVaiTro(
        @Param("trangThaiTu") TrangThaiDonHang trangThaiTu,
        @Param("vaiTroNguoiDung") VaiTro vaiTroNguoiDung
    );

    /**
     * Kiểm tra xem một chuyển đổi có được phép không
     * 
     * @param trangThaiTu Trạng thái hiện tại
     * @param trangThaiDen Trạng thái đích
     * @return true nếu chuyển đổi được phép
     */
    @Query("SELECT CASE WHEN COUNT(cdtt) > 0 THEN true ELSE false END " +
           "FROM ChuyenDoiTrangThaiHoaDon cdtt WHERE cdtt.trangThaiTu = :trangThaiTu " +
           "AND cdtt.trangThaiDen = :trangThaiDen AND cdtt.choPhep = true")
    boolean kiemTraChuyenDoiChoPhep(
        @Param("trangThaiTu") TrangThaiDonHang trangThaiTu,
        @Param("trangThaiDen") TrangThaiDonHang trangThaiDen
    );

    /**
     * Tìm tất cả các chuyển đổi chỉ dành cho hệ thống
     * 
     * @return Danh sách các chuyển đổi chỉ có thể được thực hiện bởi hệ thống
     */
    @Query("SELECT cdtt FROM ChuyenDoiTrangThaiHoaDon cdtt WHERE cdtt.chiHeThong = true AND cdtt.choPhep = true")
    List<ChuyenDoiTrangThaiHoaDon> timCacChuyenDoiChiHeThong();
}
