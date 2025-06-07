import { privateApi } from "./axiosAPI";

const VOUCHER_BASE_URL = "/phieu-giam-gia";

/**
 * Consolidated PhieuGiamGia API service
 * Handles all voucher operations with proper error handling and DTO alignment
 */
const phieuGiamGiaApi = {
  /**
   * Get all vouchers with optional filtering
   * @param {Object} params - Query parameters for filtering
   * @returns {Promise<Array>} List of vouchers
   */
  async getAllVouchers(params = {}) {
    try {
      const queryParams = new URLSearchParams();

      if (params.status && params.status !== 'all') {
        queryParams.append('trangThai', params.status);
      }
      if (params.search) {
        queryParams.append('search', params.search);
      }
      if (params.startDate) {
        queryParams.append('startDate', params.startDate);
      }
      if (params.endDate) {
        queryParams.append('endDate', params.endDate);
      }

      const url = queryParams.toString()
        ? `${VOUCHER_BASE_URL}?${queryParams.toString()}`
        : VOUCHER_BASE_URL;

      const response = await privateApi.get(url);
      return response.data;
    } catch (error) {
      console.error("Error fetching vouchers:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể lấy danh sách phiếu giảm giá. Vui lòng thử lại sau.");
    }
  },

  /**
   * Get voucher by ID
   * @param {number} id - Voucher ID
   * @returns {Promise<Object>} Voucher data
   */
  async getVoucherById(id) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/${id}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching voucher by ID:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể lấy thông tin phiếu giảm giá. Vui lòng thử lại sau.");
    }
  },

  /**
   * Create new voucher
   * @param {Object} voucherData - Voucher data with proper DTO structure
   * @returns {Promise<Object>} Created voucher
   */
  async createVoucher(voucherData) {
    try {
      // Ensure proper DTO structure with enums
      const data = {
        ...voucherData,
        loaiGiamGia: voucherData.loaiGiamGia, // Should be 'PHAN_TRAM' or 'SO_TIEN_CO_DINH'
        trangThai: voucherData.trangThai || 'CHUA_DIEN_RA',
        danhSachNguoiDung: voucherData.danhSachNguoiDung || []
      };

      const response = await privateApi.post(VOUCHER_BASE_URL, data);
      return response.data;
    } catch (error) {
      console.error("Error creating voucher:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể tạo phiếu giảm giá. Vui lòng thử lại sau.");
    }
  },

  /**
   * Update existing voucher
   * @param {number} id - Voucher ID
   * @param {Object} voucherData - Updated voucher data
   * @returns {Promise<Object>} Updated voucher
   */
  async updateVoucher(id, voucherData) {
    try {
      // Ensure proper DTO structure with enums
      const data = {
        ...voucherData,
        loaiGiamGia: voucherData.loaiGiamGia, // Should be 'PHAN_TRAM' or 'SO_TIEN_CO_DINH'
        danhSachNguoiDung: voucherData.danhSachNguoiDung || []
      };

      const response = await privateApi.put(`${VOUCHER_BASE_URL}/${id}`, data);
      return response.data;
    } catch (error) {
      console.error("Error updating voucher:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể cập nhật phiếu giảm giá. Vui lòng thử lại sau.");
    }
  },

  /**
   * Soft delete voucher (close campaign)
   * @param {number} id - Voucher ID
   * @returns {Promise<string>} Success message
   */
  async deleteVoucher(id) {
    try {
      const response = await privateApi.put(`${VOUCHER_BASE_URL}/delete/${id}`);
      return response.data;
    } catch (error) {
      console.error("Error deleting voucher:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể đóng phiếu giảm giá. Vui lòng thử lại sau.");
    }
  },

  // ==================== ENHANCED AUDIT TRAIL METHODS ====================

  /**
   * Create new voucher with audit trail support
   * @param {Object} voucherData - Voucher data with proper DTO structure
   * @param {string} reason - Reason for creating the voucher
   * @returns {Promise<Object>} Created voucher
   */
  async createVoucherWithAudit(voucherData, reason) {
    try {
      // Ensure proper DTO structure with enums and audit information
      const data = {
        ...voucherData,
        loaiGiamGia: voucherData.loaiGiamGia, // Should be 'PHAN_TRAM' or 'SO_TIEN_CO_DINH'
        trangThai: voucherData.trangThai || 'CHUA_DIEN_RA',
        danhSachNguoiDung: voucherData.danhSachNguoiDung || [],
        // Add audit reason if provided
        lyDoThayDoi: reason || 'Tạo phiếu giảm giá mới từ admin panel'
      };

      const response = await privateApi.post(VOUCHER_BASE_URL, data);
      return response.data;
    } catch (error) {
      console.error("Error creating voucher with audit:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể tạo phiếu giảm giá. Vui lòng thử lại sau.");
    }
  },

  /**
   * Update existing voucher with audit trail support
   * @param {number} id - Voucher ID
   * @param {Object} voucherData - Updated voucher data
   * @param {string} reason - Reason for updating the voucher
   * @returns {Promise<Object>} Updated voucher
   */
  async updateVoucherWithAudit(id, voucherData, reason) {
    try {
      // Ensure proper DTO structure with enums and audit information
      const data = {
        ...voucherData,
        loaiGiamGia: voucherData.loaiGiamGia, // Should be 'PHAN_TRAM' or 'SO_TIEN_CO_DINH'
        danhSachNguoiDung: voucherData.danhSachNguoiDung || [],
        // Add audit reason if provided
        lyDoThayDoi: reason || 'Cập nhật thông tin phiếu giảm giá từ admin panel'
      };

      const response = await privateApi.put(`${VOUCHER_BASE_URL}/${id}`, data);
      return response.data;
    } catch (error) {
      console.error("Error updating voucher with audit:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể cập nhật phiếu giảm giá. Vui lòng thử lại sau.");
    }
  },

  /**
   * Soft delete voucher with audit trail support (close campaign)
   * @param {number} id - Voucher ID
   * @param {string} reason - Reason for closing the voucher
   * @returns {Promise<Object>} Closed voucher data
   */
  async deleteVoucherWithAudit(id, reason) {
    try {
      // Send reason as query parameter or in request body
      const params = new URLSearchParams();
      if (reason) {
        params.append('reason', reason);
      }

      const url = params.toString()
        ? `${VOUCHER_BASE_URL}/delete/${id}?${params.toString()}`
        : `${VOUCHER_BASE_URL}/delete/${id}`;

      const response = await privateApi.put(url);
      return response.data;
    } catch (error) {
      console.error("Error deleting voucher with audit:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể đóng phiếu giảm giá. Vui lòng thử lại sau.");
    }
  },

  /**
   * Get complete audit history for a voucher
   * @param {number} id - Voucher ID
   * @returns {Promise<Array>} Array of audit history entries
   */
  async getAuditHistory(id) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/${id}/audit-history`);
      return response.data;
    } catch (error) {
      console.error("Error fetching audit history:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể tải lịch sử thay đổi. Vui lòng thử lại sau.");
    }
  },

  /**
   * Batch close multiple vouchers with audit trail support
   * @param {Array<number>} voucherIds - Array of voucher IDs to close
   * @param {string} reason - Reason for closing the vouchers
   * @returns {Promise<Object>} Batch operation result
   */
  async batchCloseVouchers(voucherIds, reason) {
    try {
      const data = {
        voucherIds: voucherIds,
        reason: reason || 'Đóng nhiều phiếu giảm giá từ admin panel'
      };

      const response = await privateApi.put(`${VOUCHER_BASE_URL}/batch-close`, data);
      return response.data;
    } catch (error) {
      console.error("Error in batch close operation:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Không thể đóng các phiếu giảm giá. Vui lòng thử lại sau.");
    }
  }
}

export default phieuGiamGiaApi;
