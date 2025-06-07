import { ref } from 'vue'
import orderApi from '@/apis/orderApi'

/**
 * Order Audit Trail Composable
 * Extracted from OrderCreate.vue for reusability across order components
 * Provides comprehensive audit trail functionality for order operations
 */
export function useOrderAudit() {
  // Audit state
  const auditHistory = ref([])
  const auditLoading = ref(false)
  const auditError = ref(null)

  /**
   * Create audit trail entry for order operations
   * @param {string|number} hoaDonId - Order ID
   * @param {string} hanhDong - Action performed (CREATE, UPDATE, DELETE, etc.)
   * @param {string} lyDoThayDoi - Reason for change
   * @param {Object} orderData - Order data (optional, for CREATE operations)
   * @returns {Promise<boolean>} Success status
   */
  const createAuditTrailEntry = async (hoaDonId, hanhDong, lyDoThayDoi, orderData = null) => {
    try {
      const auditData = {
        hoaDonId,
        hanhDong,
        thoiGianThayDoi: new Date().toISOString(),
        nguoiThucHien: 'CURRENT_USER', // Will be set by backend from authentication
        lyDoThayDoi,
        giaTriMoi: orderData ? JSON.stringify({
          maHoaDon: orderData.maHoaDon,
          loaiHoaDon: orderData.loaiHoaDon,
          trangThaiDonHang: orderData.trangThaiDonHang,
          trangThaiThanhToan: orderData.trangThaiThanhToan,
          tongThanhToan: orderData.tongThanhToan,
          khachHangId: orderData.khachHangId
        }) : null
      }

      // Note: Audit trail creation might be handled automatically by the backend
      // For now, we'll log the audit data and assume success
      console.log('Audit trail entry would be created:', auditData)

      // TODO: Implement actual API call when backend audit endpoint is available
      // const response = await orderApi.createAuditTrail(auditData)

      return true
    } catch (error) {
      console.error('Error creating audit trail entry:', error)
      auditError.value = error.message || 'Lỗi tạo audit trail'
      // Don't throw error to avoid disrupting main flow
      return false
    }
  }

  /**
   * Load audit history for an order
   * @param {string|number} hoaDonId - Order ID
   * @returns {Promise<Array>} Audit history array
   */
  const loadAuditHistory = async (hoaDonId) => {
    if (!hoaDonId) {
      console.warn('No order ID provided for audit history')
      return []
    }

    try {
      auditLoading.value = true
      auditError.value = null

      const response = await orderApi.getEnhancedAuditHistory(hoaDonId)

      if (response.success) {
        const processedHistory = response.data.map(entry => {
          try {
            return {
              ...entry,
              thoiGianThayDoi: new Date(entry.thoiGianThayDoi),
              giaTriCu: entry.giaTriCu ? (typeof entry.giaTriCu === 'string' ? JSON.parse(entry.giaTriCu) : entry.giaTriCu) : null,
              giaTriMoi: entry.giaTriMoi ? (typeof entry.giaTriMoi === 'string' ? JSON.parse(entry.giaTriMoi) : entry.giaTriMoi) : null
            }
          } catch (parseError) {
            console.warn('Error parsing audit entry:', parseError, entry)
            return {
              ...entry,
              thoiGianThayDoi: new Date(entry.thoiGianThayDoi),
              giaTriCu: entry.giaTriCu || null,
              giaTriMoi: entry.giaTriMoi || null
            }
          }
        })

        auditHistory.value = processedHistory
        return processedHistory
      } else {
        console.warn('Failed to load audit history:', response.message)
        auditError.value = response.message || 'Không thể tải lịch sử audit'
        auditHistory.value = []
        return []
      }
    } catch (error) {
      console.error('Error loading audit history:', error)
      auditError.value = error.message || 'Lỗi tải lịch sử audit'
      auditHistory.value = []
      return []
    } finally {
      auditLoading.value = false
    }
  }

  /**
   * Refresh audit history for current order
   * @param {string|number} hoaDonId - Order ID
   * @returns {Promise<Array>} Updated audit history
   */
  const refreshAuditHistory = async (hoaDonId) => {
    return await loadAuditHistory(hoaDonId)
  }

  /**
   * Clear audit history and error state
   */
  const clearAuditHistory = () => {
    auditHistory.value = []
    auditError.value = null
    auditLoading.value = false
  }

  /**
   * Create audit entry for order creation
   * @param {string|number} hoaDonId - Order ID
   * @param {Object} orderData - Complete order data
   * @returns {Promise<boolean>} Success status
   */
  const auditOrderCreation = async (hoaDonId, orderData) => {
    return await createAuditTrailEntry(
      hoaDonId,
      'CREATE',
      'Tạo đơn hàng mới',
      orderData
    )
  }

  /**
   * Create audit entry for order update
   * @param {string|number} hoaDonId - Order ID
   * @param {string} reason - Reason for update
   * @param {Object} oldData - Previous order data (optional)
   * @param {Object} newData - Updated order data (optional)
   * @returns {Promise<boolean>} Success status
   */
  const auditOrderUpdate = async (hoaDonId, reason, oldData = null, newData = null) => {
    return await createAuditTrailEntry(
      hoaDonId,
      'UPDATE',
      reason,
      newData
    )
  }

  /**
   * Create audit entry for order cancellation
   * @param {string|number} hoaDonId - Order ID
   * @param {string} reason - Reason for cancellation
   * @returns {Promise<boolean>} Success status
   */
  const auditOrderCancellation = async (hoaDonId, reason) => {
    return await createAuditTrailEntry(
      hoaDonId,
      'CANCEL',
      reason || 'Hủy đơn hàng'
    )
  }

  /**
   * Create audit entry for status change
   * @param {string|number} hoaDonId - Order ID
   * @param {string} newStatus - New status
   * @param {string} reason - Reason for status change
   * @returns {Promise<boolean>} Success status
   */
  const auditStatusChange = async (hoaDonId, newStatus, reason) => {
    return await createAuditTrailEntry(
      hoaDonId,
      'STATUS_CHANGE',
      reason || `Thay đổi trạng thái thành ${newStatus}`
    )
  }

  /**
   * Create audit entry for payment update
   * @param {string|number} hoaDonId - Order ID
   * @param {string} paymentStatus - New payment status
   * @param {string} reason - Reason for payment update
   * @returns {Promise<boolean>} Success status
   */
  const auditPaymentUpdate = async (hoaDonId, paymentStatus, reason) => {
    return await createAuditTrailEntry(
      hoaDonId,
      'PAYMENT_UPDATE',
      reason || `Cập nhật thanh toán: ${paymentStatus}`
    )
  }

  /**
   * Get formatted audit history for display
   * @returns {Array} Formatted audit history
   */
  const getFormattedAuditHistory = () => {
    return auditHistory.value.map(entry => ({
      ...entry,
      formattedTime: entry.thoiGianThayDoi.toLocaleString('vi-VN'),
      actionLabel: getActionLabel(entry.hanhDong),
      hasChanges: entry.giaTriCu || entry.giaTriMoi
    }))
  }

  /**
   * Get human-readable label for audit action
   * @param {string} action - Audit action
   * @returns {string} Human-readable label
   */
  const getActionLabel = (action) => {
    const labels = {
      'CREATE': 'Tạo mới',
      'UPDATE': 'Cập nhật',
      'DELETE': 'Xóa',
      'CANCEL': 'Hủy',
      'STATUS_CHANGE': 'Thay đổi trạng thái',
      'PAYMENT_UPDATE': 'Cập nhật thanh toán'
    }
    return labels[action] || action
  }

  return {
    // State
    auditHistory,
    auditLoading,
    auditError,

    // Core methods
    createAuditTrailEntry,
    loadAuditHistory,
    refreshAuditHistory,
    clearAuditHistory,

    // Convenience methods
    auditOrderCreation,
    auditOrderUpdate,
    auditOrderCancellation,
    auditStatusChange,
    auditPaymentUpdate,

    // Utility methods
    getFormattedAuditHistory,
    getActionLabel
  }
}
