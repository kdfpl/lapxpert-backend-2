package com.lapxpert.backend.hoadon.service;

import com.lapxpert.backend.hoadon.entity.ChuyenDoiTrangThaiHoaDon;
import com.lapxpert.backend.hoadon.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.repository.ChuyenDoiTrangThaiHoaDonRepository;
import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.entity.VaiTro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service để kiểm tra việc chuyển đổi trạng thái hóa đơn dựa trên quy tắc kinh doanh.
 * Đảm bảo rằng việc thay đổi trạng thái tuân theo quy trình đã định và quy tắc ủy quyền.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KiemTraTrangThaiHoaDonService {

    private final ChuyenDoiTrangThaiHoaDonRepository chuyenDoiTrangThaiRepository;

    /**
     * Kiểm tra xem việc chuyển đổi trạng thái có được phép cho người dùng cụ thể không
     * 
     * @param trangThaiTu Trạng thái hóa đơn hiện tại
     * @param trangThaiDen Trạng thái hóa đơn đích
     * @param nguoiDung Người dùng đang cố gắng chuyển đổi
     * @param laHanhDongHeThong Liệu đây có phải là hành động do hệ thống khởi tạo không
     * @return KetQuaKiemTra chứa trạng thái kiểm tra và chi tiết
     */
    public KetQuaKiemTra kiemTraChuyenDoi(TrangThaiDonHang trangThaiTu, 
                                        TrangThaiDonHang trangThaiDen, 
                                        NguoiDung nguoiDung, 
                                        boolean laHanhDongHeThong) {
        
        // Chuyển đổi cùng trạng thái luôn được phép (không có thay đổi)
        if (trangThaiTu == trangThaiDen) {
            return KetQuaKiemTra.thanhCong("Không cần thay đổi trạng thái");
        }

        // Tìm quy tắc chuyển đổi
        Optional<ChuyenDoiTrangThaiHoaDon> chuyenDoiOpt = 
            chuyenDoiTrangThaiRepository.findByTrangThaiTuAndTrangThaiDen(trangThaiTu, trangThaiDen);

        if (chuyenDoiOpt.isEmpty()) {
            return KetQuaKiemTra.thatBai(
                String.format("Không có quy tắc chuyển đổi được định nghĩa từ %s đến %s", trangThaiTu, trangThaiDen)
            );
        }

        ChuyenDoiTrangThaiHoaDon chuyenDoi = chuyenDoiOpt.get();

        // Kiểm tra xem chuyển đổi có được phép không
        if (!chuyenDoi.getChoPhep()) {
            return KetQuaKiemTra.thatBai(
                String.format("Chuyển đổi từ %s đến %s không được phép", trangThaiTu, trangThaiDen)
            );
        }

        // Kiểm tra xem đây có phải là chuyển đổi chỉ dành cho hệ thống không
        if (chuyenDoi.laChuyenDoiChiHeThong() && !laHanhDongHeThong) {
            return KetQuaKiemTra.thatBai(
                String.format("Chuyển đổi từ %s đến %s chỉ có thể được thực hiện bởi hệ thống", 
                    trangThaiTu, trangThaiDen)
            );
        }

        // Kiểm tra yêu cầu vai trò (bỏ qua cho hành động hệ thống)
        if (!laHanhDongHeThong && nguoiDung != null) {
            VaiTro vaiTroNguoiDung = nguoiDung.getVaiTro();
            if (!chuyenDoi.kiemTraChoPhepChoVaiTro(vaiTroNguoiDung)) {
                return KetQuaKiemTra.thatBai(
                    String.format("Vai trò người dùng %s không được ủy quyền cho chuyển đổi từ %s đến %s. Vai trò yêu cầu: %s", 
                        vaiTroNguoiDung, trangThaiTu, trangThaiDen, chuyenDoi.getVaiTroYeuCau())
                );
            }
        }

        return KetQuaKiemTra.thanhCong("Chuyển đổi được phép", chuyenDoi);
    }

    /**
     * Lấy tất cả các chuyển đổi được phép từ trạng thái hiện tại cho người dùng cụ thể
     * 
     * @param trangThaiTu Trạng thái hóa đơn hiện tại
     * @param nguoiDung Người dùng yêu cầu các chuyển đổi
     * @return Danh sách các chuyển đổi được phép
     */
    public List<ChuyenDoiTrangThaiHoaDon> layCacChuyenDoiChoPhep(TrangThaiDonHang trangThaiTu, NguoiDung nguoiDung) {
        if (nguoiDung == null) {
            return List.of();
        }
        
        return chuyenDoiTrangThaiRepository.timCacChuyenDoiChoPhepChoVaiTro(trangThaiTu, nguoiDung.getVaiTro());
    }

    /**
     * Kiểm tra xem một chuyển đổi có yêu cầu lý do không
     * 
     * @param trangThaiTu Trạng thái hóa đơn hiện tại
     * @param trangThaiDen Trạng thái hóa đơn đích
     * @return true nếu yêu cầu lý do
     */
    public boolean yeuCauLyDo(TrangThaiDonHang trangThaiTu, TrangThaiDonHang trangThaiDen) {
        return chuyenDoiTrangThaiRepository.findByTrangThaiTuAndTrangThaiDen(trangThaiTu, trangThaiDen)
            .map(ChuyenDoiTrangThaiHoaDon::getYeuCauLyDo)
            .orElse(false);
    }

    /**
     * Kết quả của việc kiểm tra chuyển đổi trạng thái
     */
    public static class KetQuaKiemTra {
        private final boolean hopLe;
        private final String thongBao;
        private final ChuyenDoiTrangThaiHoaDon chuyenDoi;

        private KetQuaKiemTra(boolean hopLe, String thongBao, ChuyenDoiTrangThaiHoaDon chuyenDoi) {
            this.hopLe = hopLe;
            this.thongBao = thongBao;
            this.chuyenDoi = chuyenDoi;
        }

        public static KetQuaKiemTra thanhCong(String thongBao) {
            return new KetQuaKiemTra(true, thongBao, null);
        }

        public static KetQuaKiemTra thanhCong(String thongBao, ChuyenDoiTrangThaiHoaDon chuyenDoi) {
            return new KetQuaKiemTra(true, thongBao, chuyenDoi);
        }

        public static KetQuaKiemTra thatBai(String thongBao) {
            return new KetQuaKiemTra(false, thongBao, null);
        }

        public boolean isHopLe() {
            return hopLe;
        }

        public String getThongBao() {
            return thongBao;
        }

        public ChuyenDoiTrangThaiHoaDon getChuyenDoi() {
            return chuyenDoi;
        }

        public boolean yeuCauLyDo() {
            return chuyenDoi != null && chuyenDoi.getYeuCauLyDo();
        }
    }
}
