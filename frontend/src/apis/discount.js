import {privateApi} from "./axiosAPI";

const privateApi_URL = "/discounts";

const discountService = {
  async getAllDiscounts() {
    try {
      const response = await privateApi.get(`${privateApi_URL}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching discounts:", error.response?.data || error.message);
      throw error;
    }
  },

  async getDiscountById(discountId) {
    try {
      const response = await privateApi.get(`${privateApi_URL}/${discountId}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching discount by ID:", error.response?.data || error.message);
      throw error;
    }
  },

  async saveDiscount(discount) {
    try {
      const response = await privateApi.put(`${privateApi_URL}`, discount);
      return response.data;
    } catch (error) {
      console.error("Error save discount:", error.response?.data || error.message);
      throw error;
    }
  },

  async deleteDiscount(discountId) {
    try {
      const response = await privateApi.post(`${privateApi_URL}/toggle/${discountId}`);
      return response.data;
    } catch (error) {
      console.error("Error deleting discount:", error.response?.data || error.message);
      throw error;
    }
  },

  async deleteDiscounts(discountIds) {
    try {
      const response = await privateApi.post(`${privateApi_URL}/toggles`, discountIds);
      return response.data;
    } catch (error) {
      console.error("Error deleting discounts:", error.response?.data || error.message);
      throw error;
    }
  },

  async addDiscountToProducts(discountId, productIds) {
    try {
      const response = await privateApi.put(`${privateApi_URL}/${discountId}/spct`, productIds);
      return response.data;
    } catch (error) {
      console.error("Error adding discount to product:", error.response?.data || error.message);
      throw error;
    }
  },

  async removeDiscountFromProducts(discountId, productIds) {
    try {
      const response = await privateApi.delete(`${privateApi_URL}/${discountId}/spct`, { data: productIds });
      return response.data;
    } catch (error) {
      console.error("Error removing discount from products:", error.response?.data || error.message);
      throw error;
    }
  },

  async getDiscountProducts(discountId) {
    try {
      const response = await privateApi.get(`${privateApi_URL}/${discountId}/spct`);
      return response.data;
    } catch (error) {
      console.error("Error fetching discount products:", error.response?.data || error.message);
      throw error;
    }
  },

  async getDiscountAuditHistory(discountId) {
    try {
      const response = await privateApi.get(`${privateApi_URL}/${discountId}/audit-history`);
      return response.data;
    } catch (error) {
      console.error("Error fetching discount audit history:", error.response?.data || error.message);
      throw error;
    }
  },

  // ==================== ENHANCED AUDIT TRAIL METHODS ====================

  /**
   * Save discount with audit trail support
   * @param {Object} discountData - Discount data
   * @param {string} reason - Reason for the change
   * @returns {Promise<Object>} Saved discount
   */
  async saveDiscountWithAudit(discountData, reason) {
    try {
      const data = {
        ...discountData,
        lyDoThayDoi: reason || (discountData.id ? 'Cập nhật đợt giảm giá từ admin panel' : 'Tạo đợt giảm giá mới từ admin panel')
      };

      const response = await privateApi.put(`${privateApi_URL}`, data);
      return response.data;
    } catch (error) {
      console.error("Error saving discount with audit:", error.response?.data || error.message);
      throw error;
    }
  },

  /**
   * Delete discount with audit trail support
   * @param {number} discountId - Discount ID
   * @param {string} reason - Reason for deletion
   * @returns {Promise<Object>} Result
   */
  async deleteDiscountWithAudit(discountId, reason) {
    try {
      const requestBody = {
        campaignIds: [discountId],
        lyDoThayDoi: reason || 'Xóa đợt giảm giá từ admin panel'
      };

      const response = await privateApi.put(`${privateApi_URL}/batch/cancel`, requestBody);
      return response.data;
    } catch (error) {
      console.error("Error deleting discount with audit:", error.response?.data || error.message);
      throw error;
    }
  },

  /**
   * Batch delete discounts with audit trail support
   * @param {Array<number>} discountIds - Array of discount IDs
   * @param {string} reason - Reason for deletion
   * @returns {Promise<Object>} Result
   */
  async deleteDiscountsWithAudit(discountIds, reason) {
    try {
      const requestBody = {
        campaignIds: discountIds,
        lyDoThayDoi: reason || 'Xóa nhiều đợt giảm giá từ admin panel'
      };

      const response = await privateApi.put(`${privateApi_URL}/batch/cancel`, requestBody);
      return response.data;
    } catch (error) {
      console.error("Error batch deleting discounts with audit:", error.response?.data || error.message);
      throw error;
    }
  },

  // ==================== ADMIN MANAGEMENT METHODS ====================

  /**
   * Get all discounts including hidden ones (admin only)
   * @returns {Promise<Array>} All discounts including hidden
   */
  async getAllDiscountsIncludingHidden() {
    try {
      const response = await privateApi.get(`${privateApi_URL}/admin/all`);
      return response.data;
    } catch (error) {
      console.error("Error fetching all discounts including hidden:", error.response?.data || error.message);
      throw error;
    }
  },

  /**
   * Get only cancelled discounts (admin only)
   * @returns {Promise<Array>} Cancelled discounts
   */
  async getCancelledDiscounts() {
    try {
      const response = await privateApi.get(`${privateApi_URL}/admin/cancelled`);
      return response.data;
    } catch (error) {
      console.error("Error fetching cancelled discounts:", error.response?.data || error.message);
      throw error;
    }
  },

  /**
   * Restore a cancelled discount campaign
   * @param {number} discountId - Discount ID to restore
   * @returns {Promise<Object>} Restored discount
   */
  async restoreDiscount(discountId) {
    try {
      const response = await privateApi.post(`${privateApi_URL}/admin/restore/${discountId}`);
      return response.data;
    } catch (error) {
      console.error("Error restoring discount:", error.response?.data || error.message);
      throw error;
    }
  },

  /**
   * Batch restore multiple cancelled discount campaigns
   * @param {Array<number>} discountIds - Array of discount IDs to restore
   * @returns {Promise<Array>} Restored discounts
   */
  async restoreMultipleDiscounts(discountIds) {
    try {
      const response = await privateApi.post(`${privateApi_URL}/admin/restore-batch`, discountIds);
      return response.data;
    } catch (error) {
      console.error("Error batch restoring discounts:", error.response?.data || error.message);
      throw error;
    }
  },
}

export default discountService;
