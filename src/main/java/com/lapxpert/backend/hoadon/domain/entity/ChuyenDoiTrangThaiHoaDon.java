package com.lapxpert.backend.hoadon.domain.entity;

import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Entity định nghĩa các quy tắc chuyển đổi trạng thái hóa đơn hợp lệ.
 * Kiểm soát việc thay đổi trạng thái nào được phép và trong điều kiện nào.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chuyen_doi_trang_thai_hoa_don",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"trang_thai_tu", "trang_thai_den"})
    },
    indexes = {
        @Index(name = "idx_chuyen_doi_trang_thai_tu", columnList = "trang_thai_tu"),
        @Index(name = "idx_chuyen_doi_trang_thai_den", columnList = "trang_thai_den")
    })
@EntityListeners(AuditingEntityListener.class)
public class ChuyenDoiTrangThaiHoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_tu", nullable = false)
    private TrangThaiDonHang trangThaiTu;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_den", nullable = false)
    private TrangThaiDonHang trangThaiDen;

    @Column(name = "cho_phep", nullable = false)
    private Boolean choPhep = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "vai_tro_yeu_cau")
    private VaiTro vaiTroYeuCau;

    @Column(name = "quy_tac_kinh_doanh", length = 500)
    private String quyTacKinhDoanh;

    @Column(name = "yeu_cau_ly_do", nullable = false)
    private Boolean yeuCauLyDo = false;

    @Column(name = "chi_he_thong", nullable = false)
    private Boolean chiHeThong = false;

    @CreatedDate
    @Column(name = "ngay_tao", updatable = false)
    private Instant ngayTao;

    /**
     * Kiểm tra xem việc chuyển đổi có được phép cho vai trò đã cho không
     *
     * @param vaiTroNguoiDung Vai trò của người dùng đang cố gắng chuyển đổi
     * @return true nếu việc chuyển đổi được phép cho vai trò này
     */
    public boolean kiemTraChoPhepChoVaiTro(VaiTro vaiTroNguoiDung) {
        if (!choPhep) {
            return false;
        }

        if (vaiTroYeuCau == null) {
            return true; // Không yêu cầu vai trò cụ thể
        }

        // Admin có thể thực hiện bất kỳ chuyển đổi nào
        if (vaiTroNguoiDung == VaiTro.ADMIN) {
            return true;
        }

        // Kiểm tra xem người dùng có vai trò yêu cầu hoặc cao hơn không
        return vaiTroNguoiDung == vaiTroYeuCau ||
               (vaiTroYeuCau == VaiTro.STAFF && vaiTroNguoiDung == VaiTro.ADMIN);
    }

    /**
     * Kiểm tra xem việc chuyển đổi này chỉ có thể được thực hiện bởi hệ thống không
     *
     * @return true nếu đây là chuyển đổi chỉ dành cho hệ thống
     */
    public boolean laChuyenDoiChiHeThong() {
        return chiHeThong != null && chiHeThong;
    }
}
