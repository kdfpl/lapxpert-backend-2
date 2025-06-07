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
    getIconColorClass
  }
}
