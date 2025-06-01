package com.lapxpert.backend.hoadon.domain.service;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonChiTiet;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating receipt preview data and PDF receipts.
 * Provides comprehensive receipt generation with preview capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptPreviewService {

    private final HoaDonRepository hoaDonRepository;

    /**
     * Receipt preview data structure containing all information needed for display.
     */
    public static class ReceiptPreviewData {
        private String maHoaDon;
        private String ngayTao;
        private String loaiHoaDon;
        private String trangThaiDonHang;
        private String trangThaiThanhToan;
        private String phuongThucThanhToan;
        
        // Customer information
        private String tenKhachHang;
        private String emailKhachHang;
        private String sdtKhachHang;
        
        // Staff information
        private String tenNhanVien;
        
        // Delivery information
        private String diaChiGiaoHang;
        private String nguoiNhanTen;
        private String nguoiNhanSdt;
        
        // Order items
        private List<ReceiptItemData> items;
        
        // Financial summary
        private BigDecimal tongTienHang;
        private BigDecimal phiVanChuyen;
        private BigDecimal giaTriGiamGiaVoucher;
        private BigDecimal tongThanhToan;
        
        // Additional information
        private String ghiChu;
        private String lyDoHuy;

        // Constructors, getters, and setters
        public ReceiptPreviewData() {
            this.items = new ArrayList<>();
        }

        // Getters and setters
        public String getMaHoaDon() { return maHoaDon; }
        public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }
        
        public String getNgayTao() { return ngayTao; }
        public void setNgayTao(String ngayTao) { this.ngayTao = ngayTao; }
        
        public String getLoaiHoaDon() { return loaiHoaDon; }
        public void setLoaiHoaDon(String loaiHoaDon) { this.loaiHoaDon = loaiHoaDon; }
        
        public String getTrangThaiDonHang() { return trangThaiDonHang; }
        public void setTrangThaiDonHang(String trangThaiDonHang) { this.trangThaiDonHang = trangThaiDonHang; }
        
        public String getTrangThaiThanhToan() { return trangThaiThanhToan; }
        public void setTrangThaiThanhToan(String trangThaiThanhToan) { this.trangThaiThanhToan = trangThaiThanhToan; }
        
        public String getPhuongThucThanhToan() { return phuongThucThanhToan; }
        public void setPhuongThucThanhToan(String phuongThucThanhToan) { this.phuongThucThanhToan = phuongThucThanhToan; }
        
        public String getTenKhachHang() { return tenKhachHang; }
        public void setTenKhachHang(String tenKhachHang) { this.tenKhachHang = tenKhachHang; }
        
        public String getEmailKhachHang() { return emailKhachHang; }
        public void setEmailKhachHang(String emailKhachHang) { this.emailKhachHang = emailKhachHang; }
        
        public String getSdtKhachHang() { return sdtKhachHang; }
        public void setSdtKhachHang(String sdtKhachHang) { this.sdtKhachHang = sdtKhachHang; }
        
        public String getTenNhanVien() { return tenNhanVien; }
        public void setTenNhanVien(String tenNhanVien) { this.tenNhanVien = tenNhanVien; }
        
        public String getDiaChiGiaoHang() { return diaChiGiaoHang; }
        public void setDiaChiGiaoHang(String diaChiGiaoHang) { this.diaChiGiaoHang = diaChiGiaoHang; }
        
        public String getNguoiNhanTen() { return nguoiNhanTen; }
        public void setNguoiNhanTen(String nguoiNhanTen) { this.nguoiNhanTen = nguoiNhanTen; }
        
        public String getNguoiNhanSdt() { return nguoiNhanSdt; }
        public void setNguoiNhanSdt(String nguoiNhanSdt) { this.nguoiNhanSdt = nguoiNhanSdt; }
        
        public List<ReceiptItemData> getItems() { return items; }
        public void setItems(List<ReceiptItemData> items) { this.items = items; }
        
        public BigDecimal getTongTienHang() { return tongTienHang; }
        public void setTongTienHang(BigDecimal tongTienHang) { this.tongTienHang = tongTienHang; }
        
        public BigDecimal getPhiVanChuyen() { return phiVanChuyen; }
        public void setPhiVanChuyen(BigDecimal phiVanChuyen) { this.phiVanChuyen = phiVanChuyen; }
        
        public BigDecimal getGiaTriGiamGiaVoucher() { return giaTriGiamGiaVoucher; }
        public void setGiaTriGiamGiaVoucher(BigDecimal giaTriGiamGiaVoucher) { this.giaTriGiamGiaVoucher = giaTriGiamGiaVoucher; }
        
        public BigDecimal getTongThanhToan() { return tongThanhToan; }
        public void setTongThanhToan(BigDecimal tongThanhToan) { this.tongThanhToan = tongThanhToan; }
        
        public String getGhiChu() { return ghiChu; }
        public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
        
        public String getLyDoHuy() { return lyDoHuy; }
        public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
    }

    /**
     * Receipt item data structure.
     */
    public static class ReceiptItemData {
        private String tenSanPham;
        private String sku;
        private Integer soLuong;
        private BigDecimal giaBan;
        private BigDecimal giaGoc;
        private BigDecimal thanhTien;
        private String hinhAnh;

        // Constructors, getters, and setters
        public ReceiptItemData() {}

        public ReceiptItemData(String tenSanPham, String sku, Integer soLuong, 
                              BigDecimal giaBan, BigDecimal giaGoc, BigDecimal thanhTien, String hinhAnh) {
            this.tenSanPham = tenSanPham;
            this.sku = sku;
            this.soLuong = soLuong;
            this.giaBan = giaBan;
            this.giaGoc = giaGoc;
            this.thanhTien = thanhTien;
            this.hinhAnh = hinhAnh;
        }

        // Getters and setters
        public String getTenSanPham() { return tenSanPham; }
        public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }
        
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public Integer getSoLuong() { return soLuong; }
        public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }
        
        public BigDecimal getGiaBan() { return giaBan; }
        public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
        
        public BigDecimal getGiaGoc() { return giaGoc; }
        public void setGiaGoc(BigDecimal giaGoc) { this.giaGoc = giaGoc; }
        
        public BigDecimal getThanhTien() { return thanhTien; }
        public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }
        
        public String getHinhAnh() { return hinhAnh; }
        public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
    }

    /**
     * Generate receipt preview data for an order.
     * 
     * @param orderId Order ID
     * @return Receipt preview data
     */
    @Transactional(readOnly = true)
    public ReceiptPreviewData generateReceiptPreview(Long orderId) {
        HoaDon hoaDon = hoaDonRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        return buildReceiptPreviewData(hoaDon);
    }

    /**
     * Build receipt preview data from order entity.
     */
    private ReceiptPreviewData buildReceiptPreviewData(HoaDon hoaDon) {
        ReceiptPreviewData preview = new ReceiptPreviewData();

        // Basic order information
        preview.setMaHoaDon(hoaDon.getMaHoaDon());
        preview.setNgayTao(hoaDon.getNgayTao() != null ?
            java.time.LocalDateTime.ofInstant(hoaDon.getNgayTao(), java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
        preview.setLoaiHoaDon(hoaDon.getLoaiHoaDon() != null ? hoaDon.getLoaiHoaDon().name() : "");
        preview.setTrangThaiDonHang(hoaDon.getTrangThaiDonHang() != null ? hoaDon.getTrangThaiDonHang().name() : "");
        preview.setTrangThaiThanhToan(hoaDon.getTrangThaiThanhToan() != null ? hoaDon.getTrangThaiThanhToan().name() : "");
        preview.setPhuongThucThanhToan(""); // Payment method not stored in entity

        // Customer information
        if (hoaDon.getKhachHang() != null) {
            preview.setTenKhachHang(hoaDon.getKhachHang().getHoTen());
            preview.setEmailKhachHang(hoaDon.getKhachHang().getEmail());
            preview.setSdtKhachHang(hoaDon.getKhachHang().getSoDienThoai());
        }

        // Staff information
        if (hoaDon.getNhanVien() != null) {
            preview.setTenNhanVien(hoaDon.getNhanVien().getHoTen());
        }

        // Delivery information
        if (hoaDon.getDiaChiGiaoHang() != null) {
            // Build full address from DiaChi components
            String fullAddress = String.format("%s, %s, %s, %s, %s",
                hoaDon.getDiaChiGiaoHang().getDuong(),
                hoaDon.getDiaChiGiaoHang().getPhuongXa(),
                hoaDon.getDiaChiGiaoHang().getQuanHuyen(),
                hoaDon.getDiaChiGiaoHang().getTinhThanh(),
                hoaDon.getDiaChiGiaoHang().getQuocGia());
            preview.setDiaChiGiaoHang(fullAddress);
        }
        preview.setNguoiNhanTen(hoaDon.getNguoiNhanTen());
        preview.setNguoiNhanSdt(hoaDon.getNguoiNhanSdt());

        // Order items
        if (hoaDon.getHoaDonChiTiets() != null) {
            for (HoaDonChiTiet chiTiet : hoaDon.getHoaDonChiTiets()) {
                ReceiptItemData item = new ReceiptItemData();
                item.setTenSanPham(chiTiet.getTenSanPhamSnapshot());
                item.setSku(chiTiet.getSkuSnapshot());
                item.setSoLuong(chiTiet.getSoLuong());
                item.setGiaBan(chiTiet.getGiaBan());
                item.setGiaGoc(chiTiet.getGiaGoc());
                item.setThanhTien(chiTiet.getThanhTien());
                item.setHinhAnh(chiTiet.getHinhAnhSnapshot());
                preview.getItems().add(item);
            }
        }

        // Financial summary
        preview.setTongTienHang(hoaDon.getTongTienHang());
        preview.setPhiVanChuyen(hoaDon.getPhiVanChuyen());
        preview.setGiaTriGiamGiaVoucher(hoaDon.getGiaTriGiamGiaVoucher());
        preview.setTongThanhToan(hoaDon.getTongThanhToan());

        // Additional information (these fields don't exist in current HoaDon entity)
        preview.setGhiChu(""); // Not available in current entity
        preview.setLyDoHuy(""); // Not available in current entity

        return preview;
    }

    /**
     * Generate receipt preview HTML for display.
     * 
     * @param orderId Order ID
     * @return HTML string for receipt preview
     */
    @Transactional(readOnly = true)
    public String generateReceiptPreviewHtml(Long orderId) {
        ReceiptPreviewData preview = generateReceiptPreview(orderId);
        return buildReceiptHtml(preview);
    }

    /**
     * Build HTML receipt from preview data.
     */
    private String buildReceiptHtml(ReceiptPreviewData preview) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Hóa đơn ").append(preview.getMaHoaDon()).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append(".company-name { font-size: 24px; font-weight: bold; color: #2c3e50; }");
        html.append(".receipt-title { font-size: 18px; margin-top: 10px; }");
        html.append(".section { margin-bottom: 20px; }");
        html.append(".section-title { font-weight: bold; font-size: 16px; margin-bottom: 10px; color: #34495e; }");
        html.append(".info-row { margin-bottom: 5px; }");
        html.append(".label { font-weight: bold; display: inline-block; width: 150px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }");
        html.append(".text-right { text-align: right; }");
        html.append(".total-row { font-weight: bold; background-color: #f8f9fa; }");
        html.append(".footer { margin-top: 30px; text-align: center; font-size: 12px; color: #7f8c8d; }");
        html.append("</style>");
        html.append("</head><body>");

        // Header
        html.append("<div class='header'>");
        html.append("<div class='company-name'>LAPXPERT</div>");
        html.append("<div class='receipt-title'>HÓA ĐƠN BÁN HÀNG</div>");
        html.append("</div>");

        // Order information
        html.append("<div class='section'>");
        html.append("<div class='section-title'>Thông tin đơn hàng</div>");
        html.append("<div class='info-row'><span class='label'>Mã hóa đơn:</span>").append(preview.getMaHoaDon()).append("</div>");
        html.append("<div class='info-row'><span class='label'>Ngày tạo:</span>").append(preview.getNgayTao()).append("</div>");
        html.append("<div class='info-row'><span class='label'>Loại hóa đơn:</span>").append(preview.getLoaiHoaDon()).append("</div>");
        html.append("<div class='info-row'><span class='label'>Trạng thái:</span>").append(preview.getTrangThaiDonHang()).append("</div>");
        html.append("<div class='info-row'><span class='label'>Thanh toán:</span>").append(preview.getTrangThaiThanhToan()).append("</div>");
        if (preview.getPhuongThucThanhToan() != null) {
            html.append("<div class='info-row'><span class='label'>Phương thức:</span>").append(preview.getPhuongThucThanhToan()).append("</div>");
        }
        html.append("</div>");

        // Customer information
        if (preview.getTenKhachHang() != null) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>Thông tin khách hàng</div>");
            html.append("<div class='info-row'><span class='label'>Tên khách hàng:</span>").append(preview.getTenKhachHang()).append("</div>");
            if (preview.getEmailKhachHang() != null) {
                html.append("<div class='info-row'><span class='label'>Email:</span>").append(preview.getEmailKhachHang()).append("</div>");
            }
            if (preview.getSdtKhachHang() != null) {
                html.append("<div class='info-row'><span class='label'>Số điện thoại:</span>").append(preview.getSdtKhachHang()).append("</div>");
            }
            html.append("</div>");
        }

        // Staff information
        if (preview.getTenNhanVien() != null) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>Nhân viên phụ trách</div>");
            html.append("<div class='info-row'><span class='label'>Tên nhân viên:</span>").append(preview.getTenNhanVien()).append("</div>");
            html.append("</div>");
        }

        // Delivery information
        if (preview.getDiaChiGiaoHang() != null || preview.getNguoiNhanTen() != null) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>Thông tin giao hàng</div>");
            if (preview.getNguoiNhanTen() != null) {
                html.append("<div class='info-row'><span class='label'>Người nhận:</span>").append(preview.getNguoiNhanTen()).append("</div>");
            }
            if (preview.getNguoiNhanSdt() != null) {
                html.append("<div class='info-row'><span class='label'>SĐT người nhận:</span>").append(preview.getNguoiNhanSdt()).append("</div>");
            }
            if (preview.getDiaChiGiaoHang() != null) {
                html.append("<div class='info-row'><span class='label'>Địa chỉ:</span>").append(preview.getDiaChiGiaoHang()).append("</div>");
            }
            html.append("</div>");
        }

        // Order items
        html.append("<div class='section'>");
        html.append("<div class='section-title'>Chi tiết đơn hàng</div>");
        html.append("<table>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Sản phẩm</th>");
        html.append("<th>SKU</th>");
        html.append("<th class='text-right'>Số lượng</th>");
        html.append("<th class='text-right'>Đơn giá</th>");
        html.append("<th class='text-right'>Thành tiền</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        for (ReceiptItemData item : preview.getItems()) {
            html.append("<tr>");
            html.append("<td>").append(item.getTenSanPham() != null ? item.getTenSanPham() : "").append("</td>");
            html.append("<td>").append(item.getSku() != null ? item.getSku() : "").append("</td>");
            html.append("<td class='text-right'>").append(item.getSoLuong()).append("</td>");
            html.append("<td class='text-right'>").append(formatCurrency(item.getGiaBan())).append("</td>");
            html.append("<td class='text-right'>").append(formatCurrency(item.getThanhTien())).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");

        // Financial summary
        html.append("<div class='section'>");
        html.append("<div class='section-title'>Tổng kết</div>");
        html.append("<table>");
        html.append("<tr><td>Tổng tiền hàng:</td><td class='text-right'>").append(formatCurrency(preview.getTongTienHang())).append("</td></tr>");
        if (preview.getPhiVanChuyen() != null && preview.getPhiVanChuyen().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<tr><td>Phí vận chuyển:</td><td class='text-right'>").append(formatCurrency(preview.getPhiVanChuyen())).append("</td></tr>");
        }
        if (preview.getGiaTriGiamGiaVoucher() != null && preview.getGiaTriGiamGiaVoucher().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<tr><td>Giảm giá voucher:</td><td class='text-right'>-").append(formatCurrency(preview.getGiaTriGiamGiaVoucher())).append("</td></tr>");
        }
        html.append("<tr class='total-row'><td>Tổng thanh toán:</td><td class='text-right'>").append(formatCurrency(preview.getTongThanhToan())).append("</td></tr>");
        html.append("</table>");
        html.append("</div>");

        // Additional information
        if (preview.getGhiChu() != null || preview.getLyDoHuy() != null) {
            html.append("<div class='section'>");
            html.append("<div class='section-title'>Ghi chú</div>");
            if (preview.getGhiChu() != null) {
                html.append("<div class='info-row'>").append(preview.getGhiChu()).append("</div>");
            }
            if (preview.getLyDoHuy() != null) {
                html.append("<div class='info-row'><strong>Lý do hủy:</strong> ").append(preview.getLyDoHuy()).append("</div>");
            }
            html.append("</div>");
        }

        // Footer
        html.append("<div class='footer'>");
        html.append("Cảm ơn quý khách đã mua hàng tại LAPXPERT!<br>");
        html.append("Hotline: 1900-xxxx | Email: support@lapxpert.com");
        html.append("</div>");

        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Format currency for display.
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 ₫";
        }
        return String.format("%,.0f ₫", amount);
    }
}
