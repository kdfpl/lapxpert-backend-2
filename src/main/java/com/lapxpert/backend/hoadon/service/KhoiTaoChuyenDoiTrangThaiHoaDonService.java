package com.lapxpert.backend.hoadon.service;

import com.lapxpert.backend.hoadon.entity.ChuyenDoiTrangThaiHoaDon;
import com.lapxpert.backend.hoadon.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.repository.ChuyenDoiTrangThaiHoaDonRepository;
import com.lapxpert.backend.nguoidung.entity.VaiTro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service để khởi tạo các quy tắc chuyển đổi trạng thái hóa đơn.
 * Chạy khi khởi động ứng dụng để đảm bảo các quy tắc chuyển đổi được thiết lập.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KhoiTaoChuyenDoiTrangThaiHoaDonService implements CommandLineRunner {

    private final ChuyenDoiTrangThaiHoaDonRepository chuyenDoiTrangThaiRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (chuyenDoiTrangThaiRepository.count() == 0) {
            log.info("Đang khởi tạo các quy tắc chuyển đổi trạng thái hóa đơn...");
            khoiTaoCacQuyTacChuyenDoi();
            log.info("Các quy tắc chuyển đổi trạng thái hóa đơn đã được khởi tạo thành công");
        } else {
            log.debug("Các quy tắc chuyển đổi trạng thái hóa đơn đã tồn tại, bỏ qua khởi tạo");
        }
    }

    private void khoiTaoCacQuyTacChuyenDoi() {
        // CHO_XAC_NHAN (Chờ xác nhận) transitions
        taoChuyenDoi(TrangThaiDonHang.CHO_XAC_NHAN, TrangThaiDonHang.DA_XAC_NHAN, 
                    VaiTro.STAFF, "Xác nhận đơn hàng chờ xử lý", false, false);
        taoChuyenDoi(TrangThaiDonHang.CHO_XAC_NHAN, TrangThaiDonHang.DA_HUY, 
                    VaiTro.STAFF, "Hủy đơn hàng chờ xử lý", true, false);

        // DA_XAC_NHAN (Đã xác nhận) transitions
        taoChuyenDoi(TrangThaiDonHang.DA_XAC_NHAN, TrangThaiDonHang.DANG_XU_LY, 
                    VaiTro.STAFF, "Bắt đầu xử lý đơn hàng đã xác nhận", false, false);
        taoChuyenDoi(TrangThaiDonHang.DA_XAC_NHAN, TrangThaiDonHang.DA_HUY, 
                    VaiTro.STAFF, "Hủy đơn hàng đã xác nhận", true, false);

        // DANG_XU_LY (Đang xử lý) transitions
        taoChuyenDoi(TrangThaiDonHang.DANG_XU_LY, TrangThaiDonHang.CHO_GIAO_HANG, 
                    VaiTro.STAFF, "Sẵn sàng để giao hàng", false, false);
        taoChuyenDoi(TrangThaiDonHang.DANG_XU_LY, TrangThaiDonHang.DA_HUY, 
                    VaiTro.ADMIN, "Hủy đơn hàng đang xử lý", true, false);

        // CHO_GIAO_HANG (Sẵn sàng giao hàng) transitions
        taoChuyenDoi(TrangThaiDonHang.CHO_GIAO_HANG, TrangThaiDonHang.DANG_GIAO_HANG, 
                    VaiTro.STAFF, "Bắt đầu giao hàng", false, false);
        taoChuyenDoi(TrangThaiDonHang.CHO_GIAO_HANG, TrangThaiDonHang.DA_HUY, 
                    VaiTro.ADMIN, "Hủy trước khi giao hàng", true, false);

        // DANG_GIAO_HANG (Đang giao hàng) transitions
        taoChuyenDoi(TrangThaiDonHang.DANG_GIAO_HANG, TrangThaiDonHang.DA_GIAO_HANG, 
                    VaiTro.STAFF, "Giao hàng hoàn tất", false, false);
        taoChuyenDoi(TrangThaiDonHang.DANG_GIAO_HANG, TrangThaiDonHang.DA_HUY, 
                    VaiTro.ADMIN, "Giao hàng thất bại - hủy đơn hàng", true, false);

        // DA_GIAO_HANG (Đã giao hàng) transitions
        taoChuyenDoi(TrangThaiDonHang.DA_GIAO_HANG, TrangThaiDonHang.HOAN_THANH, 
                    null, "Hoàn thành đơn hàng đã giao", false, true); // Chỉ hệ thống
        taoChuyenDoi(TrangThaiDonHang.DA_GIAO_HANG, TrangThaiDonHang.YEU_CAU_TRA_HANG, 
                    VaiTro.CUSTOMER, "Khách hàng yêu cầu trả hàng", true, false);

        // HOAN_THANH (Hoàn thành) transitions
        taoChuyenDoi(TrangThaiDonHang.HOAN_THANH, TrangThaiDonHang.YEU_CAU_TRA_HANG, 
                    VaiTro.CUSTOMER, "Yêu cầu trả hàng cho đơn hàng hoàn thành", true, false);

        // YEU_CAU_TRA_HANG (Yêu cầu trả hàng) transitions
        taoChuyenDoi(TrangThaiDonHang.YEU_CAU_TRA_HANG, TrangThaiDonHang.DA_TRA_HANG, 
                    VaiTro.STAFF, "Xử lý trả hàng", false, false);
        taoChuyenDoi(TrangThaiDonHang.YEU_CAU_TRA_HANG, TrangThaiDonHang.HOAN_THANH, 
                    VaiTro.STAFF, "Từ chối yêu cầu trả hàng", true, false);

        // Chuyển đổi chỉ dành cho hệ thống để xử lý tự động
        taoChuyenDoi(TrangThaiDonHang.CHO_XAC_NHAN, TrangThaiDonHang.HOAN_THANH, 
                    null, "Hoàn thành ngay lập tức đơn hàng POS", false, true);
        taoChuyenDoi(TrangThaiDonHang.DA_XAC_NHAN, TrangThaiDonHang.DANG_XU_LY, 
                    null, "Tự động xử lý sau thanh toán", false, true);

        // Trạng thái cuối (không có chuyển đổi ra ngoài ngoại trừ trả hàng)
        // DA_HUY và DA_TRA_HANG là trạng thái cuối không có chuyển đổi tiếp theo
    }

    private void taoChuyenDoi(TrangThaiDonHang trangThaiTu, TrangThaiDonHang trangThaiDen, 
                            VaiTro vaiTroYeuCau, String quyTacKinhDoanh, 
                            boolean yeuCauLyDo, boolean chiHeThong) {
        if (chuyenDoiTrangThaiRepository.findByTrangThaiTuAndTrangThaiDen(trangThaiTu, trangThaiDen).isEmpty()) {
            ChuyenDoiTrangThaiHoaDon chuyenDoi = new ChuyenDoiTrangThaiHoaDon();
            chuyenDoi.setTrangThaiTu(trangThaiTu);
            chuyenDoi.setTrangThaiDen(trangThaiDen);
            chuyenDoi.setChoPhep(true);
            chuyenDoi.setVaiTroYeuCau(vaiTroYeuCau);
            chuyenDoi.setQuyTacKinhDoanh(quyTacKinhDoanh);
            chuyenDoi.setYeuCauLyDo(yeuCauLyDo);
            chuyenDoi.setChiHeThong(chiHeThong);
            
            chuyenDoiTrangThaiRepository.save(chuyenDoi);
            log.debug("Đã tạo chuyển đổi: {} -> {} (Vai trò: {}, Hệ thống: {})", 
                     trangThaiTu, trangThaiDen, vaiTroYeuCau, chiHeThong);
        }
    }
}
