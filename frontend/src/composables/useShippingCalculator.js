import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import shippingApi from '@/apis/shippingApi'

/**
 * Shipping Calculator Composable
 * Handles shipping fee calculation using GHN service
 * Enhanced with GHN integration and robust fallback mechanisms
 * Follows existing LapXpert patterns and Vietnamese business terminology
 */
export function useShippingCalculator() {
  const toast = useToast()

  // Shipping calculation state
  const isCalculating = ref(false)
  const calculationError = ref(null)
  const lastCalculationResult = ref(null)

  // Shipping fee state
  const shippingFee = ref(0)
  const isManualOverride = ref(false)
  const isAutoCalculated = ref(false)
  const estimatedDeliveryTime = ref('')

  // GHN service state (simplified from provider comparison)
  const ghnServiceAvailable = ref(true)
  const lastGHNResult = ref(null)

  // Address validation state
  const isValidatingAddress = ref(false)
  const addressValidationResult = ref(null)

  // Configuration
  const shippingConfig = ref({
    defaultWeight: 500, // 500g default
    defaultTransport: 'road',
    pickupAddress: {
      province: 'Hà Nội',
      district: 'Cầu Giấy',
      ward: 'Dịch Vọng',
      address: 'Số 1 Đại Cồ Việt'
    }
  })

  // Computed properties
  const canAutoCalculate = computed(() => {
    return !isCalculating.value && !isManualOverride.value
  })

  const shippingStatus = computed(() => {
    if (isCalculating.value) {
      return { text: 'Đang tính phí vận chuyển...', severity: 'info' }
    }
    if (calculationError.value) {
      return { text: 'Lỗi tính phí vận chuyển', severity: 'error' }
    }
    if (isManualOverride.value) {
      return { text: 'Nhập thủ công', severity: 'warn' }
    }
    if (isAutoCalculated.value) {
      return { text: 'Tự động tính', severity: 'success' }
    }
    return { text: 'Chưa tính phí', severity: 'secondary' }
  })

  const hasValidShippingFee = computed(() => {
    return shippingFee.value > 0 || isManualOverride.value
  })

  const hasGHNResult = computed(() => {
    return lastGHNResult.value && lastGHNResult.value.success
  })

  const ghnServiceInfo = computed(() => {
    if (!hasGHNResult.value) return null
    return {
      provider: 'GHN',
      totalFee: lastGHNResult.value.data?.totalFee || 0,
      shippingFee: lastGHNResult.value.data?.shippingFee || 0,
      estimatedTime: lastGHNResult.value.data?.estimatedDeliveryTime || ''
    }
  })

  /**
   * Calculate shipping fee using GHN service directly
   */
  const calculateShippingFeeWithComparison = async (deliveryAddress, orderValue = 0, weight = null) => {
    if (!deliveryAddress || !deliveryAddress.province || !deliveryAddress.district) {
      calculationError.value = 'Thiếu thông tin địa chỉ giao hàng'
      return false
    }

    isCalculating.value = true
    calculationError.value = null

    try {
      const shippingRequest = {
        // Pickup address (from config)
        pickProvince: shippingConfig.value.pickupAddress.province,
        pickDistrict: shippingConfig.value.pickupAddress.district,
        pickWard: shippingConfig.value.pickupAddress.ward,
        pickAddress: shippingConfig.value.pickupAddress.address,

        // Delivery address
        province: deliveryAddress.province,
        district: deliveryAddress.district,
        ward: deliveryAddress.ward || '',
        address: deliveryAddress.address || '',

        // Package details
        weight: weight || shippingConfig.value.defaultWeight,
        value: orderValue,
        transport: shippingConfig.value.defaultTransport,
        tags: []
      }

      console.log('Shipping request for GHN calculation:', shippingRequest)
      const result = await shippingApi.calculateShippingFeeWithFallback(shippingRequest)

      if (result.success && result.data) {
        lastGHNResult.value = result
        console.log('GHN calculation results:', result.data)

        if (!result.data.isManualOverride) {
          // GHN calculation succeeded
          shippingFee.value = result.data.fee || result.data.totalFee || 0
          isAutoCalculated.value = true
          isManualOverride.value = false
          estimatedDeliveryTime.value = result.data.estimatedDeliveryTime || result.data.estimatedTime || ''
          ghnServiceAvailable.value = true

          toast.add({
            severity: 'success',
            summary: 'Tính phí vận chuyển thành công',
            detail: `GHN: ${formatCurrency(shippingFee.value)}`,
            life: 4000
          })

          return true
        } else {
          // GHN calculation failed, fall back to manual
          isManualOverride.value = true
          isAutoCalculated.value = false
          ghnServiceAvailable.value = false

          // Enhanced error message for GHN address resolution failures
          let errorDetail = result.data.errorMessage || 'Vui lòng nhập phí vận chuyển thủ công'
          if (errorDetail.includes('address resolution') || errorDetail.includes('GHN')) {
            errorDetail = 'Không thể xác định địa chỉ giao hàng cho GHN. Vui lòng kiểm tra địa chỉ hoặc nhập phí vận chuyển thủ công.'
          }

          toast.add({
            severity: 'warn',
            summary: 'Không thể tính phí tự động',
            detail: errorDetail,
            life: 5000
          })

          return false
        }
      } else {
        throw new Error(result.message)
      }

    } catch (error) {
      console.error('Error calculating shipping fee:', error)
      calculationError.value = error.message
      ghnServiceAvailable.value = false

      // Enhanced error handling for GHN-specific issues
      let errorMessage = 'Lỗi tính phí vận chuyển'
      let errorDetail = 'Vui lòng nhập phí vận chuyển thủ công'

      if (error.message.includes('GHN') || error.message.includes('address resolution')) {
        errorMessage = 'Lỗi tích hợp GHN'
        errorDetail = 'Không thể kết nối với dịch vụ GHN. Vui lòng nhập phí thủ công.'
      }

      // Switch to manual mode on error
      isManualOverride.value = true
      isAutoCalculated.value = false

      toast.add({
        severity: 'error',
        summary: errorMessage,
        detail: errorDetail,
        life: 5000
      })

      return false

    } finally {
      isCalculating.value = false
    }
  }

  /**
   * Calculate shipping fee automatically (legacy method for backward compatibility)
   */
  const calculateShippingFee = async (deliveryAddress, orderValue = 0, weight = null) => {
    if (!deliveryAddress || !deliveryAddress.province || !deliveryAddress.district) {
      calculationError.value = 'Thiếu thông tin địa chỉ giao hàng'
      return false
    }

    isCalculating.value = true
    calculationError.value = null

    try {
      const shippingRequest = {
        // Pickup address (from config)
        pickProvince: shippingConfig.value.pickupAddress.province,
        pickDistrict: shippingConfig.value.pickupAddress.district,
        pickWard: shippingConfig.value.pickupAddress.ward,
        pickAddress: shippingConfig.value.pickupAddress.address,

        // Delivery address
        province: deliveryAddress.province,
        district: deliveryAddress.district,
        ward: deliveryAddress.ward || '',
        address: deliveryAddress.address || '',

        // Package details
        weight: weight || shippingConfig.value.defaultWeight,
        value: orderValue,
        transport: shippingConfig.value.defaultTransport,
        tags: []
      }

      const result = await shippingApi.calculateShippingFeeWithFallback(shippingRequest)

      if (result.success) {
        lastCalculationResult.value = result.data

        if (result.data.isManualOverride) {
          // API failed, switch to manual mode
          isManualOverride.value = true
          isAutoCalculated.value = false

          toast.add({
            severity: 'warn',
            summary: 'Không thể tính phí tự động',
            detail: result.data.errorMessage || 'Vui lòng nhập phí vận chuyển thủ công',
            life: 5000
          })
        } else {
          // API succeeded
          shippingFee.value = result.data.fee || 0
          isAutoCalculated.value = true
          isManualOverride.value = false
          estimatedDeliveryTime.value = result.data.estimatedTime || ''

          toast.add({
            severity: 'success',
            summary: 'Tính phí thành công',
            detail: `Phí vận chuyển: ${formatCurrency(result.data.fee)}`,
            life: 3000
          })
        }

        return true
      } else {
        throw new Error(result.message)
      }
    } catch (error) {
      console.error('Error calculating shipping fee:', error)
      calculationError.value = error.message

      // Switch to manual mode on error
      isManualOverride.value = true
      isAutoCalculated.value = false

      toast.add({
        severity: 'error',
        summary: 'Lỗi tính phí vận chuyển',
        detail: 'Vui lòng nhập phí vận chuyển thủ công',
        life: 5000
      })

      return false
    } finally {
      isCalculating.value = false
    }
  }

  /**
   * Validate delivery address
   */
  const validateDeliveryAddress = async (address) => {
    if (!address || !address.province || !address.district) {
      return false
    }

    isValidatingAddress.value = true

    try {
      const result = await shippingApi.validateAddress({
        province: address.province,
        district: address.district,
        ward: address.ward || '',
        address: address.address || ''
      })

      addressValidationResult.value = result.data
      return result.success && result.data.isValid
    } catch (error) {
      console.error('Error validating address:', error)
      return false
    } finally {
      isValidatingAddress.value = false
    }
  }

  /**
   * Switch to manual override mode
   */
  const enableManualOverride = () => {
    isManualOverride.value = true
    isAutoCalculated.value = false
    calculationError.value = null

    toast.add({
      severity: 'info',
      summary: 'Chế độ nhập thủ công',
      detail: 'Bạn có thể nhập phí vận chuyển tùy chỉnh',
      life: 3000
    })
  }

  /**
   * Switch back to automatic calculation
   */
  const enableAutoCalculation = () => {
    isManualOverride.value = false
    shippingFee.value = 0
    calculationError.value = null

    toast.add({
      severity: 'info',
      summary: 'Chế độ tự động',
      detail: 'Phí vận chuyển sẽ được tính tự động',
      life: 3000
    })
  }

  /**
   * Set manual shipping fee
   */
  const setManualShippingFee = (fee) => {
    if (isManualOverride.value) {
      shippingFee.value = fee || 0
    }
  }

  /**
   * Select shipping calculation mode (for backward compatibility)
   */
  const selectProvider = (providerName) => {
    if (providerName === 'MANUAL') {
      enableManualOverride()
      return true
    }

    if (providerName === 'AUTO' || providerName === 'GHN') {
      // Switch to automatic calculation using GHN
      enableAutoCalculation()
      return true
    }

    // For any other provider name, default to manual mode
    enableManualOverride()
    return false
  }

  /**
   * Get GHN service summary for display (simplified from provider comparison)
   */
  const getProviderComparisonSummary = () => {
    if (!hasGHNResult.value) return null

    return {
      selectedProvider: 'GHN',
      selectionReason: 'GHN service selected automatically',
      totalProviders: 1,
      successfulProviders: ghnServiceAvailable.value ? 1 : 0,
      comparedAt: new Date().toISOString()
    }
  }

  /**
   * Reset shipping calculation
   */
  const resetShippingCalculation = () => {
    shippingFee.value = 0
    isManualOverride.value = false
    isAutoCalculated.value = false
    calculationError.value = null
    lastCalculationResult.value = null
    estimatedDeliveryTime.value = ''
    addressValidationResult.value = null

    // Reset GHN service state
    lastGHNResult.value = null
    ghnServiceAvailable.value = true
  }

  /**
   * Load shipping configuration
   */
  const loadShippingConfig = async () => {
    try {
      const result = await shippingApi.getShippingConfig()
      if (result.success) {
        shippingConfig.value = { ...shippingConfig.value, ...result.data }
      }
    } catch (error) {
      console.error('Error loading shipping config:', error)
    }
  }

  /**
   * Get estimated delivery time
   */
  const getEstimatedDeliveryTime = async (deliveryAddress) => {
    if (!deliveryAddress || !deliveryAddress.province || !deliveryAddress.district) {
      return null
    }

    try {
      const result = await shippingApi.getEstimatedDeliveryTime({
        pickProvince: shippingConfig.value.pickupAddress.province,
        pickDistrict: shippingConfig.value.pickupAddress.district,
        province: deliveryAddress.province,
        district: deliveryAddress.district,
        transport: shippingConfig.value.defaultTransport
      })

      if (result.success) {
        estimatedDeliveryTime.value = result.data.estimatedTime || ''
        return result.data
      }
    } catch (error) {
      console.error('Error getting estimated delivery time:', error)
    }

    return null
  }

  /**
   * Test GHN integration specifically
   */
  const testGHNIntegration = async (deliveryAddress, orderValue = 0, weight = null) => {
    if (!deliveryAddress || !deliveryAddress.province || !deliveryAddress.district) {
      return { success: false, error: 'Thiếu thông tin địa chỉ giao hàng' }
    }

    try {
      const shippingRequest = {
        // Pickup address (from config)
        pickProvince: shippingConfig.value.pickupAddress.province,
        pickDistrict: shippingConfig.value.pickupAddress.district,
        pickWard: shippingConfig.value.pickupAddress.ward,
        pickAddress: shippingConfig.value.pickupAddress.address,

        // Delivery address
        province: deliveryAddress.province,
        district: deliveryAddress.district,
        ward: deliveryAddress.ward || '',
        address: deliveryAddress.address || '',

        // Package details
        weight: weight || shippingConfig.value.defaultWeight,
        value: orderValue,
        transport: shippingConfig.value.defaultTransport,
        tags: []
      }

      console.log('Testing GHN integration with request:', shippingRequest)
      const result = await shippingApi.calculateGHNShippingFee(shippingRequest)

      console.log('GHN test result:', result)
      return result
    } catch (error) {
      console.error('GHN integration test failed:', error)
      return { success: false, error: error.message }
    }
  }

  /**
   * Get GHN provider information for display (simplified from provider comparison)
   */
  const getProviderInfo = (providerName) => {
    if (providerName !== 'GHN' || !hasGHNResult.value) return null

    return {
      name: 'GHN',
      isSuccessful: lastGHNResult.value.success,
      isAvailable: ghnServiceAvailable.value,
      totalFee: lastGHNResult.value.data?.totalFee || lastGHNResult.value.data?.fee || 0,
      shippingFee: lastGHNResult.value.data?.shippingFee || 0,
      insuranceFee: lastGHNResult.value.data?.insuranceFee || 0,
      estimatedTime: lastGHNResult.value.data?.estimatedDeliveryTime || lastGHNResult.value.data?.estimatedTime || '',
      responseTime: null,
      failureReason: lastGHNResult.value.data?.errorMessage || null,
      totalScore: null
    }
  }

  /**
   * Format currency for display
   */
  const formatCurrency = (amount) => {
    if (amount == null) return '0 ₫'
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount)
  }

  // Watch for manual override changes
  watch(isManualOverride, (newValue) => {
    if (newValue) {
      isAutoCalculated.value = false
    }
  })

  return {
    // State
    isCalculating,
    calculationError,
    lastCalculationResult,
    shippingFee,
    isManualOverride,
    isAutoCalculated,
    estimatedDeliveryTime,
    isValidatingAddress,
    addressValidationResult,
    shippingConfig,

    // GHN service state (simplified from provider comparison)
    ghnServiceAvailable,
    lastGHNResult,

    // Computed
    canAutoCalculate,
    shippingStatus,
    hasValidShippingFee,
    hasGHNResult,
    ghnServiceInfo,

    // Backward compatibility computed properties (maintained for existing components)
    hasProviderComparison: hasGHNResult,
    canSelectProvider: computed(() => hasGHNResult.value && !isManualOverride.value),
    selectedProviderInfo: ghnServiceInfo,

    // Methods
    calculateShippingFee,
    calculateShippingFeeWithComparison,
    validateDeliveryAddress,
    enableManualOverride,
    enableAutoCalculation,
    setManualShippingFee,
    selectProvider,
    getProviderComparisonSummary,
    resetShippingCalculation,
    loadShippingConfig,
    getEstimatedDeliveryTime,
    formatCurrency,

    // GHN-specific methods
    testGHNIntegration,
    getProviderInfo
  }
}
