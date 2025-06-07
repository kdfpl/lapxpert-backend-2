import { privateApi } from './axiosAPI'

const VOUCHER_BASE_URL = '/phieu-giam-gia'

/**
 * Voucher API service for PhieuGiamGia module
 * Handles voucher validation, search, and management operations
 */
const voucherApi = {
  /**
   * Get all vouchers with optional filtering
   * @param {Object} params - Query parameters
   * @param {string} params.status - Voucher status filter
   * @param {string} params.type - Voucher type filter (public/private)
   * @param {string} params.search - Search term
   * @param {Array} params.dateRange - Date range filter
   * @returns {Promise<Object>} API response with vouchers data
   */
  async getAllVouchers(params = {}) {
    try {
      const queryParams = new URLSearchParams()

      if (params.status && params.status !== 'all') {
        queryParams.append('trangThai', params.status)
      }
      if (params.search) {
        queryParams.append('search', params.search)
      }
      if (params.dateRange && params.dateRange.length === 2) {
        const [startDate, endDate] = params.dateRange
        queryParams.append('tuNgay', startDate.toISOString().split('T')[0])
        queryParams.append('denNgay', endDate.toISOString().split('T')[0])
      }

      const url = `${VOUCHER_BASE_URL}?${queryParams.toString()}`
      const response = await privateApi.get(url)

      return {
        success: true,
        data: response.data,
        message: 'Vouchers fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching vouchers:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to fetch vouchers'
      }
    }
  },

  /**
   * Search vouchers by code or name
   * @param {string} query - Search query
   * @returns {Promise<Object>} API response with search results
   */
  async searchVouchers(query) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/search`, {
        params: { q: query }
      })

      return {
        success: true,
        data: response.data,
        message: 'Search completed successfully'
      }
    } catch (error) {
      console.error('Error searching vouchers:', error)
      return {
        success: false,
        data: [],
        message: error.response?.data?.message || error.message || 'Search failed'
      }
    }
  },

  /**
   * Validate voucher for order application
   * @param {string} voucherCode - Voucher code to validate
   * @param {number} customerId - Customer ID
   * @param {number} orderTotal - Order total amount
   * @returns {Promise<Object>} API response with validation result
   */
  async validateVoucher(voucherCode, customerId, orderTotal) {
    try {
      const response = await privateApi.post(`${VOUCHER_BASE_URL}/validate`, {
        voucherCode,
        customerId,
        orderTotal
      })

      return {
        success: true,
        data: {
          valid: response.data.valid,
          voucher: response.data.voucher,
          discountAmount: response.data.discountAmount,
          error: response.data.errorMessage
        },
        message: 'Validation completed successfully'
      }
    } catch (error) {
      console.error('Error validating voucher:', error)
      return {
        success: false,
        data: {
          valid: false,
          voucher: null,
          discountAmount: 0,
          error: error.response?.data?.message || error.message || 'Validation failed'
        },
        message: error.response?.data?.message || error.message || 'Validation failed'
      }
    }
  },

  /**
   * Check if customer is eligible for a voucher
   * @param {string} voucherCode - Voucher code
   * @param {number} customerId - Customer ID
   * @returns {Promise<Object>} API response with eligibility result
   */
  async checkEligibility(voucherCode, customerId) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/eligibility`, {
        params: { voucherCode, customerId }
      })

      return {
        success: true,
        data: {
          eligible: response.data.eligible
        },
        message: 'Eligibility check completed'
      }
    } catch (error) {
      console.error('Error checking voucher eligibility:', error)
      return {
        success: false,
        data: {
          eligible: false
        },
        message: error.response?.data?.message || error.message || 'Eligibility check failed'
      }
    }
  },

  /**
   * Get voucher by code
   * @param {string} voucherCode - Voucher code
   * @returns {Promise<Object>} API response with voucher data
   */
  async getVoucherByCode(voucherCode) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/code/${voucherCode}`)

      return {
        success: true,
        data: response.data,
        message: 'Voucher fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching voucher by code:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to fetch voucher'
      }
    }
  },

  /**
   * Get voucher by ID
   * @param {number} id - Voucher ID
   * @returns {Promise<Object>} API response with voucher data
   */
  async getVoucherById(id) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/${id}`)

      return {
        success: true,
        data: response.data,
        message: 'Voucher fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching voucher by ID:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to fetch voucher'
      }
    }
  },

  /**
   * Get vouchers available for a specific customer
   * @param {number} customerId - Customer ID
   * @param {number} orderTotal - Order total for validation
   * @returns {Promise<Object>} API response with available vouchers
   */
  async getAvailableVouchers(customerId, orderTotal = 0) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/available`, {
        params: { customerId, orderTotal }
      })

      return {
        success: true,
        data: response.data,
        message: 'Available vouchers fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching available vouchers:', error)
      return {
        success: false,
        data: [],
        message: error.response?.data?.message || error.message || 'Failed to fetch available vouchers'
      }
    }
  },

  /**
   * Get voucher usage statistics
   * @param {number} voucherId - Voucher ID
   * @returns {Promise<Object>} API response with usage statistics
   */
  async getVoucherUsageStats(voucherId) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/${voucherId}/usage-stats`)

      return {
        success: true,
        data: response.data,
        message: 'Usage statistics fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching voucher usage stats:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to fetch usage statistics'
      }
    }
  },

  /**
   * Apply voucher to order (used during order creation)
   * @param {number} orderId - Order ID
   * @param {string} voucherCode - Voucher code
   * @returns {Promise<Object>} API response with application result
   */
  async applyVoucherToOrder(orderId, voucherCode) {
    try {
      const response = await privateApi.post(`${VOUCHER_BASE_URL}/apply`, {
        orderId,
        voucherCode
      })

      return {
        success: true,
        data: response.data,
        message: 'Voucher applied successfully'
      }
    } catch (error) {
      console.error('Error applying voucher to order:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to apply voucher'
      }
    }
  },

  /**
   * Remove voucher from order
   * @param {number} orderId - Order ID
   * @param {string} voucherCode - Voucher code
   * @returns {Promise<Object>} API response with removal result
   */
  async removeVoucherFromOrder(orderId, voucherCode) {
    try {
      const response = await privateApi.delete(`${VOUCHER_BASE_URL}/remove`, {
        data: { orderId, voucherCode }
      })

      return {
        success: true,
        data: response.data,
        message: 'Voucher removed successfully'
      }
    } catch (error) {
      console.error('Error removing voucher from order:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to remove voucher'
      }
    }
  },

  /**
   * Get applied vouchers for an order
   * @param {number} orderId - Order ID
   * @returns {Promise<Object>} API response with applied vouchers
   */
  async getAppliedVouchers(orderId) {
    try {
      const response = await privateApi.get(`${VOUCHER_BASE_URL}/applied/${orderId}`)

      return {
        success: true,
        data: response.data,
        message: 'Applied vouchers fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching applied vouchers:', error)
      return {
        success: false,
        data: [],
        message: error.response?.data?.message || error.message || 'Failed to fetch applied vouchers'
      }
    }
  },

  /**
   * Find the best voucher for automatic selection
   * @param {number} customerId - Customer ID (optional)
   * @param {number} orderTotal - Order total amount
   * @returns {Promise<Object>} API response with best voucher result
   */
  async getBestVoucher(customerId, orderTotal) {
    try {
      const params = { orderTotal }
      if (customerId) {
        params.customerId = customerId
      }

      const response = await privateApi.get(`${VOUCHER_BASE_URL}/best`, { params })

      return {
        success: true,
        data: {
          found: response.data.found || false,
          voucher: response.data.voucher || null,
          discountAmount: response.data.discountAmount || 0,
          message: response.data.message || ''
        },
        message: 'Best voucher search completed successfully'
      }
    } catch (error) {
      console.error('Error finding best voucher:', error)
      return {
        success: false,
        data: {
          found: false,
          voucher: null,
          discountAmount: 0,
          message: 'No voucher found'
        },
        message: error.response?.data?.message || error.message || 'Failed to find best voucher'
      }
    }
  },

  /**
   * Get top N best vouchers for recommendations
   * @param {number} customerId - Customer ID (optional)
   * @param {number} orderTotal - Order total amount
   * @param {number} limit - Maximum number of vouchers to return (default: 3)
   * @returns {Promise<Object>} API response with top vouchers
   */
  async getTopVouchers(customerId, orderTotal, limit = 3) {
    try {
      const params = { orderTotal, limit }
      if (customerId) {
        params.customerId = customerId
      }

      const response = await privateApi.get(`${VOUCHER_BASE_URL}/top`, { params })

      return {
        success: true,
        data: response.data || [],
        message: 'Top vouchers fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching top vouchers:', error)
      return {
        success: false,
        data: [],
        message: error.response?.data?.message || error.message || 'Failed to fetch top vouchers'
      }
    }
  }
}

export default voucherApi
