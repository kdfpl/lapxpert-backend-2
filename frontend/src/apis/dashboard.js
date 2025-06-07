import { privateApi } from './axiosAPI';

/**
 * ThongKe (Statistics) API Service
 * Updated to use correct backend API endpoints
 * Backend uses /api/v1/thong-ke (privateApi already includes /api/v1)
 */
const API_URL = '/thong-ke';

export default {
    // ==================== DOANH THU (REVENUE) STATISTICS ====================

    /**
     * Get daily revenue statistics
     * @param {string} tuNgay - Start date (YYYY-MM-DD format)
     * @param {string} denNgay - End date (YYYY-MM-DD format)
     */
    layDoanhThuTheoNgay(tuNgay = null, denNgay = null) {
        const params = {};
        if (tuNgay) params.tuNgay = tuNgay;
        if (denNgay) params.denNgay = denNgay;
        return privateApi.get(`${API_URL}/doanh-thu/theo-ngay`, { params });
    },

    /**
     * Get monthly revenue statistics
     * @param {number} nam - Year (optional, defaults to current year)
     */
    layDoanhThuTheoThang(nam = null) {
        const params = {};
        if (nam) params.nam = nam;
        return privateApi.get(`${API_URL}/doanh-thu/theo-thang`, { params });
    },

    /**
     * Get revenue overview/summary
     */
    layDoanhThuTongQuan() {
        return privateApi.get(`${API_URL}/doanh-thu/tong-quan`);
    },

    // ==================== DON HANG (ORDER) STATISTICS ====================

    /**
     * Get order overview statistics
     */
    layDonHangTongQuan() {
        return privateApi.get(`${API_URL}/don-hang/tong-quan`);
    },

    /**
     * Get order statistics by status
     */
    layDonHangTheoTrangThai() {
        return privateApi.get(`${API_URL}/don-hang/theo-trang-thai`);
    },

    /**
     * Get average order value statistics
     */
    layGiaTriDonHangTrungBinh() {
        return privateApi.get(`${API_URL}/don-hang/gia-tri-trung-binh`);
    },

    // ==================== SAN PHAM (PRODUCT) STATISTICS ====================

    /**
     * Get top selling products
     * @param {number} soLuong - Number of top products to return
     * @param {string} tuNgay - Start date for analysis period
     * @param {string} denNgay - End date for analysis period
     */
    laySanPhamBanChayNhat(soLuong = 10, tuNgay = null, denNgay = null) {
        const params = { soLuong };
        if (tuNgay) params.tuNgay = tuNgay;
        if (denNgay) params.denNgay = denNgay;
        return privateApi.get(`${API_URL}/san-pham/ban-chay-nhat`, { params });
    },

    /**
     * Get low stock products
     * @param {number} nguongTonKho - Stock threshold
     */
    laySanPhamSapHetHang(nguongTonKho = 10) {
        return privateApi.get(`${API_URL}/san-pham/sap-het-hang`, {
            params: { nguongTonKho }
        });
    },

    /**
     * Get product performance by category
     */
    laySanPhamTheoDanhMuc() {
        return privateApi.get(`${API_URL}/san-pham/theo-danh-muc`);
    },

    // ==================== KHACH HANG (CUSTOMER) STATISTICS ====================

    /**
     * Get new customer statistics
     * @param {string} tuNgay - Start date for analysis period
     * @param {string} denNgay - End date for analysis period
     */
    layKhachHangMoi(tuNgay = null, denNgay = null) {
        const params = {};
        if (tuNgay) params.tuNgay = tuNgay;
        if (denNgay) params.denNgay = denNgay;
        return privateApi.get(`${API_URL}/khach-hang/moi`, { params });
    },

    /**
     * Get customer retention rate
     */
    layTyLeGiuChanKhachHang() {
        return privateApi.get(`${API_URL}/khach-hang/ty-le-giu-chan`);
    },

    /**
     * Get average customer value
     */
    layGiaTriKhachHangTrungBinh() {
        return privateApi.get(`${API_URL}/khach-hang/gia-tri-trung-binh`);
    },

    // ==================== DASHBOARD SUMMARY ====================

    /**
     * Get dashboard summary with key metrics
     */
    layDashboardSummary() {
        return privateApi.get(`${API_URL}/dashboard`);
    },

    // ==================== LEGACY ENDPOINTS (for backward compatibility) ====================
    // These will be removed once frontend is fully migrated

    getAllHoaDons() {
        return privateApi.get(`${API_URL}/hoa-don`);
    },

    getDoanhSo() {
        return privateApi.get(`${API_URL}/doanh-so`);
    },

    getDoanhThu() {
        return this.layDoanhThuTongQuan(); // Redirect to new endpoint
    },

    getDonHang() {
        return this.layDonHangTongQuan(); // Redirect to new endpoint
    },

    getBinhLuan() {
        return privateApi.get(`${API_URL}/binh-luan`);
    }
};
