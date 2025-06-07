import { computed } from 'vue'

/**
 * Enhanced payment method validation composable
 * Provides sophisticated validation rules for different order types and contexts
 */
export function usePaymentValidation() {
  
  /**
   * Payment method definitions with enhanced metadata
   */
  const paymentMethods = computed(() => [
    {
      value: 'TIEN_MAT',
      label: 'Tiền mặt',
      icon: 'pi pi-money-bill',
      description: 'Thanh toán bằng tiền mặt tại quầy',
      allowedChannels: ['TAI_QUAY'], // Only POS
      requiresOnlineGateway: false,
      instantConfirmation: true,
      supportedCurrencies: ['VND'],
      minimumAmount: 0,
      maximumAmount: 50000000, // 50 million VND limit for cash
      processingTime: 'Tức thì',
      fees: 0
    },
    {
      value: 'COD',
      label: 'Thanh toán khi nhận hàng',
      icon: 'pi pi-truck',
      description: 'Thanh toán bằng tiền mặt khi nhận hàng',
      allowedChannels: ['ONLINE', 'TAI_QUAY'], // Both channels
      requiresOnlineGateway: false,
      instantConfirmation: false,
      supportedCurrencies: ['VND'],
      minimumAmount: 0,
      maximumAmount: 20000000, // 20 million VND limit for COD
      processingTime: 'Khi giao hàng',
      fees: 0
    },
    {
      value: 'VNPAY',
      label: 'VNPay',
      icon: 'pi pi-credit-card',
      description: 'Thanh toán qua cổng VNPay',
      allowedChannels: ['ONLINE', 'TAI_QUAY'], // Both channels
      requiresOnlineGateway: true,
      instantConfirmation: false,
      supportedCurrencies: ['VND'],
      minimumAmount: 10000, // 10,000 VND minimum
      maximumAmount: 500000000, // 500 million VND limit
      processingTime: '1-3 phút',
      fees: 0.025 // 2.5% fee
    },
    {
      value: 'CHUYEN_KHOAN',
      label: 'Chuyển khoản ngân hàng',
      icon: 'pi pi-building',
      description: 'Chuyển khoản trực tiếp qua ngân hàng',
      allowedChannels: ['ONLINE', 'TAI_QUAY'], // Both channels
      requiresOnlineGateway: false,
      instantConfirmation: false,
      supportedCurrencies: ['VND'],
      minimumAmount: 0,
      maximumAmount: 1000000000, // 1 billion VND limit
      processingTime: '1-24 giờ',
      fees: 0
    }
  ])

  /**
   * Get available payment methods for a specific order type and context
   * @param {string} orderType - Order type (ONLINE, TAI_QUAY)
   * @param {Object} context - Additional context (amount, customer, etc.)
   * @returns {Array} Available payment methods
   */
  const getAvailablePaymentMethods = (orderType, context = {}) => {
    const { amount = 0, customerType = 'REGULAR', isDelivery = false } = context

    return paymentMethods.value.filter(method => {
      // Check if method is allowed for this channel
      if (!method.allowedChannels.includes(orderType)) {
        return false
      }

      // Check amount limits
      if (amount < method.minimumAmount || amount > method.maximumAmount) {
        return false
      }

      // Special business rules
      if (method.value === 'TIEN_MAT') {
        // Cash only for POS orders (already filtered by allowedChannels)
        return true
      }

      if (method.value === 'COD') {
        // COD requires delivery for online orders
        if (orderType === 'ONLINE' && !isDelivery) {
          return false
        }
        return true
      }

      return true
    })
  }

  /**
   * Validate a specific payment method for an order
   * @param {string} paymentMethod - Payment method to validate
   * @param {string} orderType - Order type (ONLINE, TAI_QUAY)
   * @param {Object} context - Validation context
   * @returns {Object} Validation result
   */
  const validatePaymentMethod = (paymentMethod, orderType, context = {}) => {
    const method = paymentMethods.value.find(m => m.value === paymentMethod)
    
    if (!method) {
      return {
        isValid: false,
        errors: ['Phương thức thanh toán không hợp lệ'],
        warnings: []
      }
    }

    const errors = []
    const warnings = []
    const { amount = 0, isDelivery = false, customerInfo = null } = context

    // Channel validation
    if (!method.allowedChannels.includes(orderType)) {
      errors.push(`${method.label} không được hỗ trợ cho loại đơn hàng ${orderType === 'ONLINE' ? 'trực tuyến' : 'tại quầy'}`)
    }

    // Amount validation
    if (amount < method.minimumAmount) {
      errors.push(`Số tiền tối thiểu cho ${method.label} là ${formatCurrency(method.minimumAmount)}`)
    }

    if (amount > method.maximumAmount) {
      errors.push(`Số tiền tối đa cho ${method.label} là ${formatCurrency(method.maximumAmount)}`)
    }

    // Business rule validation
    if (paymentMethod === 'COD' && orderType === 'ONLINE' && !isDelivery) {
      errors.push('Thanh toán khi nhận hàng chỉ áp dụng cho đơn hàng giao tận nơi')
    }

    if (paymentMethod === 'TIEN_MAT' && orderType === 'ONLINE') {
      errors.push('Thanh toán tiền mặt chỉ áp dụng cho đơn hàng tại quầy')
    }

    // Warnings for high amounts
    if (paymentMethod === 'COD' && amount > 10000000) {
      warnings.push('Đơn hàng COD có giá trị cao, khuyến nghị sử dụng phương thức thanh toán trực tuyến')
    }

    if (paymentMethod === 'TIEN_MAT' && amount > 20000000) {
      warnings.push('Đơn hàng tiền mặt có giá trị cao, cần xác nhận với quản lý')
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
      method
    }
  }

  /**
   * Get payment method by value
   * @param {string} value - Payment method value
   * @returns {Object|null} Payment method object
   */
  const getPaymentMethod = (value) => {
    return paymentMethods.value.find(method => method.value === value) || null
  }

  /**
   * Calculate payment fees for a method and amount
   * @param {string} paymentMethod - Payment method
   * @param {number} amount - Order amount
   * @returns {number} Fee amount
   */
  const calculatePaymentFee = (paymentMethod, amount) => {
    const method = getPaymentMethod(paymentMethod)
    if (!method) return 0

    if (method.fees === 0) return 0
    
    // Percentage-based fee
    if (method.fees < 1) {
      return Math.round(amount * method.fees)
    }
    
    // Fixed fee
    return method.fees
  }

  /**
   * Get payment method recommendations based on context
   * @param {string} orderType - Order type
   * @param {Object} context - Context for recommendations
   * @returns {Array} Recommended payment methods
   */
  const getRecommendedPaymentMethods = (orderType, context = {}) => {
    const availableMethods = getAvailablePaymentMethods(orderType, context)
    const { amount = 0, isDelivery = false } = context

    // Sort by recommendation score
    return availableMethods.map(method => {
      let score = 0

      // Prefer instant confirmation methods
      if (method.instantConfirmation) score += 10

      // Prefer methods with lower fees
      const fee = calculatePaymentFee(method.value, amount)
      score += (1000 - fee) / 100

      // Context-specific preferences
      if (orderType === 'TAI_QUAY') {
        if (method.value === 'TIEN_MAT') score += 20 // Prefer cash for POS
      } else {
        if (method.value === 'VNPAY') score += 15 // Prefer online payment for online orders
        if (isDelivery && method.value === 'COD') score += 10 // COD good for delivery
      }

      return { ...method, recommendationScore: score }
    }).sort((a, b) => b.recommendationScore - a.recommendationScore)
  }

  /**
   * Format currency amount
   * @param {number} amount - Amount to format
   * @returns {string} Formatted currency
   */
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount)
  }

  /**
   * Check if payment method requires additional verification
   * @param {string} paymentMethod - Payment method
   * @param {number} amount - Order amount
   * @returns {boolean} Whether additional verification is needed
   */
  const requiresAdditionalVerification = (paymentMethod, amount) => {
    const method = getPaymentMethod(paymentMethod)
    if (!method) return false

    // High-value cash transactions need verification
    if (paymentMethod === 'TIEN_MAT' && amount > 20000000) {
      return true
    }

    // High-value COD orders need verification
    if (paymentMethod === 'COD' && amount > 15000000) {
      return true
    }

    return false
  }

  return {
    // Data
    paymentMethods,
    
    // Methods
    getAvailablePaymentMethods,
    validatePaymentMethod,
    getPaymentMethod,
    calculatePaymentFee,
    getRecommendedPaymentMethods,
    requiresAdditionalVerification,
    formatCurrency
  }
}
