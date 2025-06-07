import { ref, computed } from 'vue'
import { useToast } from 'primevue/usetoast'

/**
 * Enhanced error handling composable
 * Provides comprehensive error recovery, user feedback, and retry mechanisms
 */

export function useErrorHandling() {
  const toast = useToast()
  
  // Error state
  const errors = ref(new Map())
  const retryAttempts = ref(new Map())
  const maxRetryAttempts = 3
  const retryDelay = 1000 // Base delay in milliseconds

  // Error statistics
  const errorStats = ref({
    total: 0,
    resolved: 0,
    pending: 0,
    critical: 0
  })

  // Computed error summary
  const hasErrors = computed(() => errors.value.size > 0)
  const criticalErrors = computed(() => {
    return Array.from(errors.value.values()).filter(error => error.severity === 'critical')
  })
  const errorCount = computed(() => errors.value.size)

  /**
   * Error severity levels
   */
  const ErrorSeverity = {
    INFO: 'info',
    WARNING: 'warning', 
    ERROR: 'error',
    CRITICAL: 'critical'
  }

  /**
   * Error categories for better handling
   */
  const ErrorCategory = {
    NETWORK: 'network',
    VALIDATION: 'validation',
    AUTHENTICATION: 'authentication',
    AUTHORIZATION: 'authorization',
    BUSINESS_LOGIC: 'business_logic',
    SYSTEM: 'system'
  }

  /**
   * Create standardized error object
   */
  const createError = (
    id,
    message,
    severity = ErrorSeverity.ERROR,
    category = ErrorCategory.SYSTEM,
    context = {},
    retryable = false
  ) => {
    return {
      id,
      message,
      severity,
      category,
      context,
      retryable,
      timestamp: new Date(),
      resolved: false,
      retryCount: 0
    }
  }

  /**
   * Add error to the error state
   */
  const addError = (error) => {
    errors.value.set(error.id, error)
    errorStats.value.total++
    errorStats.value.pending++
    
    if (error.severity === ErrorSeverity.CRITICAL) {
      errorStats.value.critical++
    }

    // Show toast notification based on severity
    showErrorToast(error)
  }

  /**
   * Remove error from state
   */
  const removeError = (errorId) => {
    const error = errors.value.get(errorId)
    if (error) {
      errors.value.delete(errorId)
      retryAttempts.value.delete(errorId)
      errorStats.value.pending--
      
      if (!error.resolved) {
        errorStats.value.resolved++
      }
    }
  }

  /**
   * Mark error as resolved
   */
  const resolveError = (errorId) => {
    const error = errors.value.get(errorId)
    if (error) {
      error.resolved = true
      errorStats.value.resolved++
      errorStats.value.pending--
      removeError(errorId)
    }
  }

  /**
   * Clear all errors
   */
  const clearErrors = () => {
    errors.value.clear()
    retryAttempts.value.clear()
    errorStats.value.pending = 0
  }

  /**
   * Show appropriate toast notification for error
   */
  const showErrorToast = (error) => {
    const severityMap = {
      [ErrorSeverity.INFO]: 'info',
      [ErrorSeverity.WARNING]: 'warn',
      [ErrorSeverity.ERROR]: 'error',
      [ErrorSeverity.CRITICAL]: 'error'
    }

    const lifeMap = {
      [ErrorSeverity.INFO]: 3000,
      [ErrorSeverity.WARNING]: 5000,
      [ErrorSeverity.ERROR]: 7000,
      [ErrorSeverity.CRITICAL]: 0 // Sticky for critical errors
    }

    toast.add({
      severity: severityMap[error.severity],
      summary: getErrorTitle(error),
      detail: error.message,
      life: lifeMap[error.severity],
      closable: error.severity !== ErrorSeverity.CRITICAL
    })
  }

  /**
   * Get appropriate error title based on category and severity
   */
  const getErrorTitle = (error) => {
    const titleMap = {
      [ErrorCategory.NETWORK]: 'Lỗi kết nối',
      [ErrorCategory.VALIDATION]: 'Lỗi xác thực',
      [ErrorCategory.AUTHENTICATION]: 'Lỗi đăng nhập',
      [ErrorCategory.AUTHORIZATION]: 'Lỗi phân quyền',
      [ErrorCategory.BUSINESS_LOGIC]: 'Lỗi nghiệp vụ',
      [ErrorCategory.SYSTEM]: 'Lỗi hệ thống'
    }

    if (error.severity === ErrorSeverity.CRITICAL) {
      return `🚨 ${titleMap[error.category] || 'Lỗi nghiêm trọng'}`
    }

    return titleMap[error.category] || 'Lỗi'
  }

  /**
   * Handle API errors with automatic categorization
   */
  const handleApiError = (error, context = {}) => {
    const errorId = `api_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    
    let category = ErrorCategory.SYSTEM
    let severity = ErrorSeverity.ERROR
    let message = 'Đã xảy ra lỗi không xác định'
    let retryable = false

    // Categorize based on HTTP status code
    if (error.response) {
      const status = error.response.status
      
      if (status >= 400 && status < 500) {
        // Client errors
        if (status === 401) {
          category = ErrorCategory.AUTHENTICATION
          message = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
          severity = ErrorSeverity.WARNING
        } else if (status === 403) {
          category = ErrorCategory.AUTHORIZATION
          message = 'Bạn không có quyền thực hiện thao tác này.'
          severity = ErrorSeverity.WARNING
        } else if (status === 422) {
          category = ErrorCategory.VALIDATION
          message = error.response.data?.message || 'Dữ liệu không hợp lệ'
          severity = ErrorSeverity.WARNING
        } else {
          category = ErrorCategory.BUSINESS_LOGIC
          message = error.response.data?.message || 'Yêu cầu không hợp lệ'
        }
      } else if (status >= 500) {
        // Server errors
        category = ErrorCategory.SYSTEM
        message = 'Lỗi máy chủ. Vui lòng thử lại sau.'
        severity = ErrorSeverity.ERROR
        retryable = true
      }
    } else if (error.request) {
      // Network errors
      category = ErrorCategory.NETWORK
      message = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.'
      severity = ErrorSeverity.ERROR
      retryable = true
    }

    const errorObj = createError(
      errorId,
      message,
      severity,
      category,
      { ...context, originalError: error },
      retryable
    )

    addError(errorObj)
    return errorObj
  }

  /**
   * Retry mechanism with exponential backoff
   */
  const retryOperation = async (errorId, operation) => {
    const error = errors.value.get(errorId)
    if (!error || !error.retryable) {
      return { success: false, error: 'Operation not retryable' }
    }

    const currentAttempts = retryAttempts.value.get(errorId) || 0
    if (currentAttempts >= maxRetryAttempts) {
      error.severity = ErrorSeverity.CRITICAL
      error.message += ` (Đã thử ${maxRetryAttempts} lần)`
      showErrorToast(error)
      return { success: false, error: 'Max retry attempts exceeded' }
    }

    try {
      // Exponential backoff delay
      const delay = retryDelay * Math.pow(2, currentAttempts)
      await new Promise(resolve => setTimeout(resolve, delay))

      retryAttempts.value.set(errorId, currentAttempts + 1)
      error.retryCount = currentAttempts + 1

      const result = await operation()
      
      // If successful, resolve the error
      resolveError(errorId)
      
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Thao tác đã được thực hiện thành công sau khi thử lại',
        life: 3000
      })

      return { success: true, data: result }
    } catch (retryError) {
      // Update error with retry information
      error.message = `${error.message} (Lần thử ${currentAttempts + 1}/${maxRetryAttempts})`
      
      if (currentAttempts + 1 >= maxRetryAttempts) {
        error.severity = ErrorSeverity.CRITICAL
        showErrorToast(error)
      }

      return { success: false, error: retryError }
    }
  }

  /**
   * Graceful degradation helper
   */
  const withFallback = async (operation, fallback, context = {}) => {
    try {
      return await operation()
    } catch (error) {
      const errorObj = handleApiError(error, context)
      
      // If fallback is provided and error is not critical, use fallback
      if (fallback && errorObj.severity !== ErrorSeverity.CRITICAL) {
        toast.add({
          severity: 'info',
          summary: 'Thông báo',
          detail: 'Đang sử dụng dữ liệu dự phòng do lỗi kết nối',
          life: 5000
        })
        
        return await fallback()
      }
      
      throw error
    }
  }

  /**
   * Validate and handle form errors
   */
  const handleValidationErrors = (validationErrors, formContext = {}) => {
    const errorId = `validation_${Date.now()}`
    
    const messages = Array.isArray(validationErrors) 
      ? validationErrors.join(', ')
      : validationErrors

    const error = createError(
      errorId,
      `Lỗi xác thực: ${messages}`,
      ErrorSeverity.WARNING,
      ErrorCategory.VALIDATION,
      formContext,
      false
    )

    addError(error)
    return error
  }

  /**
   * Get user-friendly error message
   */
  const getUserFriendlyMessage = (error) => {
    const friendlyMessages = {
      'Network Error': 'Lỗi kết nối mạng. Vui lòng kiểm tra kết nối internet.',
      'timeout': 'Yêu cầu quá thời gian chờ. Vui lòng thử lại.',
      'ECONNREFUSED': 'Không thể kết nối đến máy chủ.',
      'ENOTFOUND': 'Không tìm thấy máy chủ.',
      'ETIMEDOUT': 'Kết nối quá thời gian chờ.'
    }

    const errorMessage = error.message || error.toString()
    
    for (const [key, message] of Object.entries(friendlyMessages)) {
      if (errorMessage.includes(key)) {
        return message
      }
    }

    return errorMessage
  }

  return {
    // State
    errors: computed(() => Array.from(errors.value.values())),
    errorStats: computed(() => errorStats.value),
    hasErrors,
    criticalErrors,
    errorCount,

    // Error management
    addError,
    removeError,
    resolveError,
    clearErrors,

    // Error handling
    handleApiError,
    handleValidationErrors,
    retryOperation,
    withFallback,
    getUserFriendlyMessage,

    // Utilities
    createError,
    ErrorSeverity,
    ErrorCategory
  }
}
