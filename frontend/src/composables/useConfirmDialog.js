import { ref } from 'vue'

/**
 * Composable for managing confirmation dialogs using PrimeVue Dialog
 * Provides a reusable confirmation dialog system with customizable content
 */
export function useConfirmDialog() {
  // Dialog state
  const isVisible = ref(false)
  const dialogTitle = ref('')
  const dialogMessage = ref('')
  const dialogSeverity = ref('info')
  const confirmLabel = ref('Xác nhận')
  const cancelLabel = ref('Hủy bỏ')
  const isLoading = ref(false)

  // Promise resolver for async confirmation
  let resolvePromise = null

  /**
   * Show confirmation dialog
   * @param {Object} options - Dialog configuration
   * @param {string} options.title - Dialog title
   * @param {string} options.message - Dialog message
   * @param {string} options.severity - Dialog severity (info, warn, error, success)
   * @param {string} options.confirmLabel - Confirm button label
   * @param {string} options.cancelLabel - Cancel button label
   * @returns {Promise<boolean>} - Resolves to true if confirmed, false if cancelled
   */
  const showConfirmDialog = (options = {}) => {
    return new Promise((resolve) => {
      dialogTitle.value = options.title || 'Xác nhận'
      dialogMessage.value = options.message || 'Bạn có chắc chắn muốn thực hiện thao tác này?'
      dialogSeverity.value = options.severity || 'info'
      confirmLabel.value = options.confirmLabel || 'Xác nhận'
      cancelLabel.value = options.cancelLabel || 'Hủy bỏ'
      isLoading.value = false

      resolvePromise = resolve
      isVisible.value = true
    })
  }

  /**
   * Handle confirm action
   */
  const handleConfirm = () => {
    if (resolvePromise) {
      resolvePromise(true)
      resolvePromise = null
    }
    isVisible.value = false
  }

  /**
   * Handle cancel action
   */
  const handleCancel = () => {
    if (resolvePromise) {
      resolvePromise(false)
      resolvePromise = null
    }
    isVisible.value = false
  }

  /**
   * Set loading state for confirm button
   * @param {boolean} loading - Loading state
   */
  const setLoading = (loading) => {
    isLoading.value = loading
  }

  /**
   * Get severity-based button configuration
   */
  const getButtonSeverity = () => {
    switch (dialogSeverity.value) {
      case 'warn':
      case 'warning':
        return 'warn'
      case 'error':
      case 'danger':
        return 'danger'
      case 'success':
        return 'success'
      default:
        return 'primary'
    }
  }

  /**
   * Get severity-based icon
   */
  const getDialogIcon = () => {
    switch (dialogSeverity.value) {
      case 'warn':
      case 'warning':
        return 'pi pi-exclamation-triangle'
      case 'error':
      case 'danger':
        return 'pi pi-times-circle'
      case 'success':
        return 'pi pi-check-circle'
      case 'info':
      default:
        return 'pi pi-info-circle'
    }
  }

  /**
   * Get severity-based icon color class
   */
  const getIconColorClass = () => {
    switch (dialogSeverity.value) {
      case 'warn':
      case 'warning':
        return 'text-orange-500'
      case 'error':
      case 'danger':
        return 'text-red-500'
      case 'success':
        return 'text-green-500'
      case 'info':
      default:
        return 'text-blue-500'
    }
  }

  /**
   * Order-specific confirmation dialog templates
   */
  const showOrderCreationConfirm = (orderData) => {
    const productCount = orderData.sanPhamList?.length || 0
    const customerName = orderData.khachHang?.hoTen || 'Khách lẻ'
    const totalAmount = orderData.tongThanhToan || 0
    const paymentMethod = orderData.phuongThucThanhToan || 'TIEN_MAT'
    const hasDelivery = orderData.giaohang ? 'Có' : 'Không'

    return showConfirmDialog({
      title: 'Xác nhận tạo đơn hàng',
      message: `Bạn có chắc chắn muốn tạo đơn hàng với thông tin sau?\n\n• Khách hàng: ${customerName}\n• Số sản phẩm: ${productCount}\n• Tổng tiền: ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmount)}\n• Thanh toán: ${paymentMethod}\n• Giao hàng: ${hasDelivery}`,
      severity: 'success',
      confirmLabel: 'Tạo đơn hàng',
      cancelLabel: 'Hủy bỏ'
    })
  }

  const showOrderUpdateConfirm = (orderData) => {
    const productCount = orderData.sanPhamList?.length || 0
    const totalAmount = orderData.tongThanhToan || 0
    const customerName = orderData.khachHang?.hoTen || 'Chưa chọn'
    const hasDelivery = orderData.giaohang ? 'Có' : 'Không'

    return showConfirmDialog({
      title: 'Xác nhận cập nhật đơn hàng',
      message: `Bạn có chắc chắn muốn cập nhật đơn hàng ${orderData.maHoaDon}?\n\nThông tin sẽ được cập nhật:\n• ${productCount} sản phẩm\n• Tổng tiền: ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmount)}\n• Khách hàng: ${customerName}\n• Giao hàng: ${hasDelivery}`,
      severity: 'info',
      confirmLabel: 'Cập nhật đơn hàng',
      cancelLabel: 'Hủy bỏ'
    })
  }

  const showOrderCancelConfirm = (orderData) => {
    const totalAmount = orderData.tongThanhToan || 0
    const customerName = orderData.khachHang?.hoTen || 'Khách lẻ'

    return showConfirmDialog({
      title: 'Xác nhận hủy đơn hàng',
      message: `Bạn có chắc chắn muốn hủy đơn hàng ${orderData.maHoaDon}?\n\n• Khách hàng: ${customerName}\n• Tổng tiền: ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalAmount)}\n\nHành động này không thể hoàn tác và sẽ:\n• Hoàn trả tồn kho\n• Cập nhật báo cáo\n• Ghi nhận lịch sử hủy`,
      severity: 'warn',
      confirmLabel: 'Xác nhận hủy',
      cancelLabel: 'Giữ đơn hàng'
    })
  }

  const showTabCloseConfirm = (tabData) => {
    const productCount = tabData.sanPhamList?.length || 0

    return showConfirmDialog({
      title: 'Đóng tab đơn hàng',
      message: `Tab "${tabData.maHoaDon}" có ${productCount} sản phẩm chưa được thanh toán.\n\nBạn có chắc chắn muốn đóng tab này? Tất cả dữ liệu sẽ bị mất.`,
      severity: 'warn',
      confirmLabel: 'Đóng tab',
      cancelLabel: 'Hủy bỏ'
    })
  }

  return {
    // State
    isVisible,
    dialogTitle,
    dialogMessage,
    dialogSeverity,
    confirmLabel,
    cancelLabel,
    isLoading,

    // Methods
    showConfirmDialog,
    handleConfirm,
    handleCancel,
    setLoading,
    getButtonSeverity,
    getDialogIcon,
    getIconColorClass,

    // Order-specific templates
    showOrderCreationConfirm,
    showOrderUpdateConfirm,
    showOrderCancelConfirm,
    showTabCloseConfirm
  }
}
