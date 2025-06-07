import { ref } from 'vue'

/**
 * Order Validation Composable
 * Extracted from OrderCreate.vue for reusability across order components
 * Provides comprehensive validation for order data with Bean Validation alignment
 */
export function useOrderValidation() {
  // Validation state
  const validationErrors = ref({})
  const isValidating = ref(false)

  /**
   * Validate individual field with multiple rules
   * @param {string} fieldName - Name of the field being validated
   * @param {any} value - Value to validate
   * @param {Array} rules - Array of validation rules
   * @returns {Array} Array of error messages
   */
  const validateField = (fieldName, value, rules) => {
    const errors = []

    for (const rule of rules) {
      switch (rule.type) {
        case 'required':
          if (!value || (typeof value === 'string' && !value.trim())) {
            errors.push(rule.message || `${fieldName} không được để trống`)
          }
          break
        case 'email':
          if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
            errors.push(rule.message || 'Email không hợp lệ')
          }
          break
        case 'phone':
          if (value && !/^[0-9]{10,11}$/.test(value.replace(/\s/g, ''))) {
            errors.push(rule.message || 'Số điện thoại phải có 10-11 chữ số')
          }
          break
        case 'maxLength':
          if (value && value.length > rule.value) {
            errors.push(rule.message || `${fieldName} không được vượt quá ${rule.value} ký tự`)
          }
          break
        case 'minValue':
          if (value !== null && value !== undefined && Number(value) < rule.value) {
            errors.push(rule.message || `${fieldName} phải lớn hơn hoặc bằng ${rule.value}`)
          }
          break
        case 'pattern':
          if (value && !rule.value.test(value)) {
            errors.push(rule.message || `${fieldName} không đúng định dạng`)
          }
          break
      }
    }

    return errors
  }

  /**
   * Validate complete order data
   * @param {Object} orderData - Order data to validate
   * @returns {Object} Validation errors object
   */
  const validateOrderData = (orderData) => {
    const errors = {}

    // Validate customer information for delivery orders
    if (orderData.giaohang) {
      if (!orderData.khachHang) {
        errors.khachHang = ['Khách hàng không được để trống cho đơn giao hàng']
      }

      if (!orderData.diaChiGiaoHang) {
        errors.diaChiGiaoHang = ['Địa chỉ giao hàng không được để trống']
      }

      // Validate delivery contact information
      if (orderData.khachHang) {
        const phoneErrors = validateField('Số điện thoại', orderData.khachHang.soDienThoai, [
          { type: 'required' },
          { type: 'phone' }
        ])
        if (phoneErrors.length > 0) {
          errors.soDienThoai = phoneErrors
        }

        const nameErrors = validateField('Tên khách hàng', orderData.khachHang.hoTen, [
          { type: 'required' },
          { type: 'maxLength', value: 255 }
        ])
        if (nameErrors.length > 0) {
          errors.tenKhachHang = nameErrors
        }
      }
    }

    // Validate products
    if (!orderData.sanPhamList || orderData.sanPhamList.length === 0) {
      errors.sanPhamList = ['Đơn hàng phải có ít nhất một sản phẩm']
    } else {
      orderData.sanPhamList.forEach((item, index) => {
        const quantityErrors = validateField(`Số lượng sản phẩm ${index + 1}`, item.soLuong, [
          { type: 'required' },
          { type: 'minValue', value: 1 }
        ])
        if (quantityErrors.length > 0) {
          errors[`sanPham_${index}_soLuong`] = quantityErrors
        }
      })
    }

    // Validate payment method
    if (!orderData.phuongThucThanhToan) {
      errors.phuongThucThanhToan = ['Phương thức thanh toán không được để trống']
    }

    // Validate order notes
    if (orderData.ghiChu) {
      const noteErrors = validateField('Ghi chú', orderData.ghiChu, [
        { type: 'maxLength', value: 500 }
      ])
      if (noteErrors.length > 0) {
        errors.ghiChu = noteErrors
      }
    }

    // Validate order totals
    const totalErrors = validateField('Tổng tiền xác nhận', orderData.tongThanhToan, [
      { type: 'required' },
      { type: 'minValue', value: 0 }
    ])
    if (totalErrors.length > 0) {
      errors.tongThanhToan = totalErrors
    }

    return errors
  }

  /**
   * Validate order tab data
   * @param {Object} tabData - Tab data to validate
   * @returns {Object} Validation errors object
   */
  const validateTabData = (tabData) => {
    if (!tabData) return {}

    isValidating.value = true
    const errors = validateOrderData(tabData)
    validationErrors.value = errors
    isValidating.value = false

    return errors
  }

  /**
   * Clear validation errors
   */
  const clearValidationErrors = () => {
    validationErrors.value = {}
  }

  /**
   * Check if there are any validation errors
   * @returns {boolean} True if there are errors
   */
  const hasValidationErrors = () => {
    return Object.keys(validationErrors.value).length > 0
  }

  /**
   * Get error message for a specific field
   * @param {string} fieldName - Field name to get error for
   * @returns {string|null} Error message or null
   */
  const getFieldError = (fieldName) => {
    const errors = validationErrors.value[fieldName]
    return errors && errors.length > 0 ? errors[0] : null
  }

  return {
    // State
    validationErrors,
    isValidating,

    // Methods
    validateField,
    validateOrderData,
    validateTabData,
    clearValidationErrors,
    hasValidationErrors,
    getFieldError
  }
}
