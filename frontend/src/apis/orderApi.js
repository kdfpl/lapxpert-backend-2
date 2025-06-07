import { privateApi } from './axiosAPI'

// Updated to match backend HoaDonController endpoint
const ORDER_BASE_URL = '/hoa-don'

/**
 * Order API service for HoaDon module
 * Handles all order-related API operations
 */
const orderApi = {
  /**
   * Get all orders with optional filtering and pagination
   * @param {Object} params - Query parameters
   * @param {number} params.page - Page number (0-based)
   * @param {number} params.size - Page size
   * @param {string} params.status - Order status filter
   * @param {string} params.type - Order type filter (ONLINE/TAI_QUAY)
   * @param {string} params.search - Search term
   * @param {Array} params.dateRange - Date range filter [startDate, endDate]
   * @param {number} params.customer - Customer ID filter
   * @param {number} params.staff - Staff ID filter
   * @returns {Promise<Object>} API response with orders data
   */
  async getAllOrders(params = {}) {
    try {
      const queryParams = new URLSearchParams()

      // Add pagination
      if (params.page !== undefined) queryParams.append('page', params.page)
      if (params.size !== undefined) queryParams.append('size', params.size)

      // Add filters
      if (params.status && params.status !== 'all') {
        queryParams.append('trangThaiDonHang', params.status)
      }
      if (params.type && params.type !== 'all') {
        queryParams.append('loaiHoaDon', params.type)
      }
      if (params.search) {
        queryParams.append('search', params.search)
      }
      if (params.customer) {
        queryParams.append('khachHangId', params.customer)
      }
      if (params.staff) {
        queryParams.append('nhanVienId', params.staff)
      }

      // Add date range
      if (params.dateRange && params.dateRange.length === 2) {
        const [startDate, endDate] = params.dateRange
        queryParams.append('tuNgay', startDate.toISOString().split('T')[0])
        queryParams.append('denNgay', endDate.toISOString().split('T')[0])
      }

      const url = `${ORDER_BASE_URL}?${queryParams.toString()}`
      const response = await privateApi.get(url)

      return {
        success: true,
        data: response.data,
        message: 'Orders fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching orders:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to fetch orders'
      }
    }
  },

  /**
   * Get order by ID
   * @param {number} id - Order ID
   * @returns {Promise<Object>} API response with order data
   */
  async getOrderById(id) {
    try {
      const response = await privateApi.get(`${ORDER_BASE_URL}/${id}`)

      return {
        success: true,
        data: response.data,
        message: 'Order fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching order:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to fetch order'
      }
    }
  },

  /**
   * Create new order
   * @param {Object} orderData - Order creation data
   * @param {string} orderData.loaiHoaDon - Order type (ONLINE/TAI_QUAY)
   * @param {number} orderData.khachHangId - Customer ID
   * @param {number} orderData.nhanVienId - Staff ID
   * @param {Array} orderData.chiTiet - Order line items
   * @param {Array} orderData.voucherCodes - Voucher codes to apply
   * @param {number} orderData.phiVanChuyen - Shipping fee
   * @param {string} orderData.ghiChu - Order notes
   * @returns {Promise<Object>} API response with created order data
   */
  async createOrder(orderData) {
    try {
      const response = await privateApi.post(ORDER_BASE_URL, orderData)

      return {
        success: true,
        data: response.data,
        message: 'Order created successfully'
      }
    } catch (error) {
      console.error('Error creating order:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to create order'
      }
    }
  },

  /**
   * Update order
   * @param {number} id - Order ID
   * @param {Object} updateData - Order update data
   * @returns {Promise<Object>} API response with updated order data
   */
  async updateOrder(id, updateData) {
    try {
      const response = await privateApi.put(`${ORDER_BASE_URL}/${id}`, updateData)

      return {
        success: true,
        data: response.data,
        message: 'Order updated successfully'
      }
    } catch (error) {
      console.error('Error updating order:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to update order'
      }
    }
  },

  /**
   * Update order status using the general update endpoint
   * @param {number} id - Order ID
   * @param {string} status - New status
   * @param {string} reason - Reason for status change
   * @returns {Promise<Object>} API response with updated order data
   */
  async updateOrderStatus(id, status, reason = '') {
    try {
      // Use the general update endpoint with only the status field
      const response = await privateApi.put(`${ORDER_BASE_URL}/${id}`, {
        trangThaiDonHang: status,
        lyDoThayDoi: reason || `Cập nhật trạng thái thành ${status}`
      })

      return {
        success: true,
        data: response.data,
        message: 'Order status updated successfully'
      }
    } catch (error) {
      console.error('Error updating order status:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to update order status'
      }
    }
  },

  /**
   * Cancel order
   * @param {number} id - Order ID
   * @param {string} reason - Cancellation reason
   * @returns {Promise<Object>} API response with updated order data
   */
  async cancelOrder(id, reason) {
    try {
      const response = await privateApi.post(`${ORDER_BASE_URL}/${id}/cancel`, null, {
        params: { reason: reason }
      })

      return {
        success: true,
        data: response.data,
        message: 'Order cancelled successfully'
      }
    } catch (error) {
      console.error('Error cancelling order:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to cancel order'
      }
    }
  },

  /**
   * Cancel multiple orders
   * @param {Array<number>} orderIds - Array of order IDs
   * @param {string} reason - Cancellation reason
   * @returns {Promise<Object>} API response with updated orders data
   */
  async cancelMultipleOrders(orderIds, reason) {
    try {
      // Since there's no batch cancel endpoint, cancel orders individually
      const cancelPromises = orderIds.map(id => this.cancelOrder(id, reason))
      const results = await Promise.allSettled(cancelPromises)

      const successful = results.filter(result => result.status === 'fulfilled' && result.value.success)
      const failed = results.filter(result => result.status === 'rejected' || !result.value.success)

      if (failed.length > 0) {
        console.warn(`${failed.length} orders failed to cancel`)
      }

      return {
        success: successful.length > 0,
        data: successful.map(result => result.value.data),
        message: `Successfully cancelled ${successful.length} out of ${orderIds.length} orders`
      }
    } catch (error) {
      console.error('Error cancelling multiple orders:', error)
      return {
        success: false,
        data: null,
        message: error.message || 'Failed to cancel multiple orders'
      }
    }
  },

  /**
   * Get order statistics
   * @param {Object} params - Query parameters for statistics
   * @returns {Promise<Object>} API response with statistics data
   */
  async getOrderStatistics(params = {}) {
    try {
      const queryParams = new URLSearchParams()

      if (params.dateRange && params.dateRange.length === 2) {
        const [startDate, endDate] = params.dateRange
        queryParams.append('tuNgay', startDate.toISOString().split('T')[0])
        queryParams.append('denNgay', endDate.toISOString().split('T')[0])
      }

      const url = `${ORDER_BASE_URL}/statistics?${queryParams.toString()}`
      const response = await privateApi.get(url)

      return {
        success: true,
        data: response.data,
        message: 'Statistics fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching order statistics:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to fetch statistics'
      }
    }
  },

  /**
   * Export orders to Excel/CSV
   * @param {Object} params - Export parameters
   * @param {string} params.format - Export format (excel/csv)
   * @returns {Promise<Object>} API response with file data
   */
  async exportOrders(params = {}) {
    try {
      const queryParams = new URLSearchParams()

      if (params.format) queryParams.append('format', params.format)
      if (params.status && params.status !== 'all') {
        queryParams.append('trangThaiDonHang', params.status)
      }
      if (params.type && params.type !== 'all') {
        queryParams.append('loaiHoaDon', params.type)
      }
      if (params.dateRange && params.dateRange.length === 2) {
        const [startDate, endDate] = params.dateRange
        queryParams.append('tuNgay', startDate.toISOString().split('T')[0])
        queryParams.append('denNgay', endDate.toISOString().split('T')[0])
      }

      const url = `${ORDER_BASE_URL}/export?${queryParams.toString()}`
      const response = await privateApi.get(url, {
        responseType: 'blob'
      })

      return {
        success: true,
        data: response.data,
        message: 'Export completed successfully'
      }
    } catch (error) {
      console.error('Error exporting orders:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to export orders'
      }
    }
  },

  /**
   * Get order history/timeline
   * @param {number} id - Order ID
   * @returns {Promise<Object>} API response with order history
   */
  async getOrderHistory(id) {
    try {
      const response = await privateApi.get(`${ORDER_BASE_URL}/${id}/history`)

      return {
        success: true,
        data: response.data,
        message: 'Order history fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching order history:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to fetch order history'
      }
    }
  },

  /**
   * Confirm payment for an order
   * @param {number} orderId - Order ID
   * @param {string} paymentMethod - Payment method (TIEN_MAT, COD, VNPAY)
   * @param {Object} paymentData - Additional payment data (transaction ID, etc.)
   * @returns {Promise<Object>} API response with updated order data
   */
  async confirmPayment(orderId, paymentMethod, paymentData = {}) {
    try {
      const response = await privateApi.post(`${ORDER_BASE_URL}/${orderId}/confirm-payment`, null, {
        params: { phuongThucThanhToan: paymentMethod }
      })

      return {
        success: true,
        data: response.data,
        message: 'Payment confirmed successfully'
      }
    } catch (error) {
      console.error('Error confirming payment:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to confirm payment'
      }
    }
  },

  /**
   * Process refund for an order
   * @param {number} orderId - Order ID
   * @param {number} refundAmount - Amount to refund
   * @param {string} reason - Refund reason
   * @returns {Promise<Object>} API response with refund data
   */
  async processRefund(orderId, refundAmount, reason) {
    try {
      const response = await privateApi.post(`${ORDER_BASE_URL}/${orderId}/refund`, {
        soTienHoan: refundAmount,
        lyDoHoan: reason
      })

      return {
        success: true,
        data: response.data,
        message: 'Refund processed successfully'
      }
    } catch (error) {
      console.error('Error processing refund:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to process refund'
      }
    }
  },

  /**
   * Update payment status
   * @param {number} orderId - Order ID
   * @param {string} paymentStatus - New payment status (TrangThaiThanhToan enum)
   * @param {string} note - Optional note
   * @returns {Promise<Object>} API response with updated order data
   */
  async updatePaymentStatus(orderId, paymentStatus, note = '') {
    try {
      const params = new URLSearchParams()
      params.append('trangThaiThanhToan', paymentStatus)
      if (note) {
        params.append('ghiChu', note)
      }

      const response = await privateApi.put(`${ORDER_BASE_URL}/${orderId}/payment-status?${params.toString()}`)

      return {
        success: true,
        data: response.data,
        message: 'Payment status updated successfully'
      }
    } catch (error) {
      console.error('Error updating payment status:', error)

      return {
        success: false,
        data: null,
        message: error.response?.data?.message || 'Failed to update payment status'
      }
    }
  },

  /**
   * Get receipt preview data
   * @param {number} orderId - Order ID
   * @returns {Promise<Object>} API response with receipt preview data
   */
  async getReceiptPreview(orderId) {
    try {
      const response = await privateApi.get(`${ORDER_BASE_URL}/${orderId}/receipt-preview`)

      return {
        success: true,
        data: response.data,
        message: 'Receipt preview data retrieved successfully'
      }
    } catch (error) {
      console.error('Error getting receipt preview:', error)

      return {
        success: false,
        data: null,
        message: error.response?.data?.message || 'Failed to get receipt preview'
      }
    }
  },

  /**
   * Get receipt preview HTML
   * @param {number} orderId - Order ID
   * @returns {Promise<Object>} API response with receipt HTML
   */
  async getReceiptPreviewHtml(orderId) {
    try {
      const response = await privateApi.get(`${ORDER_BASE_URL}/${orderId}/receipt-preview-html`)

      return {
        success: true,
        data: response.data,
        message: 'Receipt preview HTML retrieved successfully'
      }
    } catch (error) {
      console.error('Error getting receipt preview HTML:', error)

      return {
        success: false,
        data: null,
        message: error.response?.data?.message || 'Failed to get receipt preview HTML'
      }
    }
  },

  /**
   * Process VNPAY payment
   * @param {number} orderId - Order ID
   * @param {Object} vnpayData - VNPAY payment data
   * @returns {Promise<Object>} API response with payment URL or result
   */
  async processVNPayPayment(orderId, vnpayData) {
    try {
      const response = await privateApi.post(`${ORDER_BASE_URL}/${orderId}/vnpay-payment`, vnpayData)

      return {
        success: true,
        data: response.data,
        message: 'VNPAY payment initiated successfully'
      }
    } catch (error) {
      console.error('Error processing VNPAY payment:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to process VNPAY payment'
      }
    }
  },

  /**
   * Print order receipt
   * @param {number} id - Order ID
   * @returns {Promise<Object>} API response with receipt data
   */
  async printOrderReceipt(id) {
    try {
      const response = await privateApi.get(`${ORDER_BASE_URL}/${id}/receipt`, {
        responseType: 'blob'
      })

      return {
        success: true,
        data: response.data,
        message: 'Receipt generated successfully'
      }
    } catch (error) {
      console.error('Error generating receipt:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to generate receipt'
      }
    }
  },

  /**
   * Get enhanced audit history with granular tracking
   * @param {number} id - Order ID
   * @returns {Promise<Object>} API response with enhanced audit history
   */
  async getEnhancedAuditHistory(id) {
    try {
      const response = await privateApi.get(`${ORDER_BASE_URL}/${id}/audit-history`)

      return {
        success: true,
        data: response.data.map(entry => ({
          ...entry,
          thoiGianThayDoi: new Date(entry.thoiGianThayDoi),
          giaTriCu: entry.giaTriCu ? JSON.parse(entry.giaTriCu) : null,
          giaTriMoi: entry.giaTriMoi ? JSON.parse(entry.giaTriMoi) : null,
          chiTietThayDoi: entry.chiTietThayDoi ? JSON.parse(entry.chiTietThayDoi) : null
        })),
        message: 'Enhanced audit history fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching enhanced audit history:', error)
      return {
        success: false,
        data: [],
        message: error.response?.data?.message || error.message || 'Failed to fetch enhanced audit history'
      }
    }
  },

  /**
   * Create order with enhanced audit context
   * @param {Object} orderData - Order creation data
   * @param {Object} auditContext - Additional audit context
   * @param {string} auditContext.userAgent - User agent string
   * @param {string} auditContext.ipAddress - Client IP address
   * @param {string} auditContext.sessionId - Session ID
   * @returns {Promise<Object>} API response with created order data
   */
  async createOrderWithEnhancedAudit(orderData, auditContext = {}) {
    try {
      const headers = {}

      // Add audit context to headers
      if (auditContext.userAgent) {
        headers['User-Agent'] = auditContext.userAgent
      }
      if (auditContext.sessionId) {
        headers['X-Session-ID'] = auditContext.sessionId
      }

      const response = await privateApi.post(ORDER_BASE_URL, orderData, { headers })

      return {
        success: true,
        data: response.data,
        message: 'Order created successfully with enhanced audit'
      }
    } catch (error) {
      console.error('Error creating order with enhanced audit:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to create order'
      }
    }
  },

  /**
   * Create specific audit entry for order operations
   * @param {number} orderId - Order ID
   * @param {string} entryType - Audit entry type (CUSTOMER_CHANGE, VOUCHER_APPLIED, etc.)
   * @param {Object} auditData - Audit data
   * @param {Object} auditData.oldValue - Previous value
   * @param {Object} auditData.newValue - New value
   * @param {string} auditData.reason - Reason for change
   * @returns {Promise<Object>} API response with audit entry creation result
   */
  async createAuditEntry(orderId, entryType, auditData) {
    try {
      const response = await privateApi.post(`${ORDER_BASE_URL}/${orderId}/audit`, {
        loaiThayDoi: entryType,
        giaTriCu: auditData.oldValue ? JSON.stringify(auditData.oldValue) : null,
        giaTriMoi: auditData.newValue ? JSON.stringify(auditData.newValue) : null,
        lyDoThayDoi: auditData.reason || '',
        chiTietThayDoi: auditData.details ? JSON.stringify(auditData.details) : null
      })

      return {
        success: true,
        data: response.data,
        message: 'Audit entry created successfully'
      }
    } catch (error) {
      console.error('Error creating audit entry:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to create audit entry'
      }
    }
  }
}

export default orderApi
