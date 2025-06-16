import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import shippingApi from '@/apis/shippingApi'

/**
 * Shipping Calculator Composable
 * Handles GHTK shipping fee calculation with automatic and manual override functionality
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

  // Provider comparison state
  const comparisonResults = ref(null)
  const selectedProvider = ref('AUTO') // AUTO, GHTK, GHN, MANUAL
  const availableProviders = ref([])
  const isComparingProviders = ref(false)
  const comparisonError = ref(null)

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

  const hasProviderComparison = computed(() => {
    return comparisonResults.value && comparisonResults.value.hasValidOptions
  })

  const canSelectProvider = computed(() => {
    return hasProviderComparison.value && !isManualOverride.value
  })

  const selectedProviderInfo = computed(() => {
    if (!hasProviderComparison.value) return null

    if (selectedProvider.value === 'AUTO') {
      return comparisonResults.value.selectedProvider
    }

    const providerResult = comparisonResults.value.allProviders?.find(
      p => p.providerName === selectedProvider.value
    )
    return providerResult?.response || null
  })

  /**
   * Calculate shipping fee with provider comparison
   */
  const calculateShippingFeeWithComparison = async (deliveryAddress, orderValue = 0, weight = null) => {
    if (!deliveryAddress || !deliveryAddress.province || !deliveryAddress.district) {
      calculationError.value = 'Thiếu thông tin địa chỉ giao hàng'
      return false
    }

    isComparingProviders.value = true
    comparisonError.value = null

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

      const result = await shippingApi.compareShippingProviders(shippingRequest)

      if (result.success && result.data) {
        comparisonResults.value = result.data

        if (result.data.hasValidOptions) {
          // Use the selected provider from comparison
          const selectedProviderResponse = result.data.selectedProvider
          shippingFee.value = selectedProviderResponse.totalFee || 0
          isAutoCalculated.value = true
          isManualOverride.value = false
          estimatedDeliveryTime.value = selectedProviderResponse.estimatedTime || ''

          // Update available providers list
          availableProviders.value = result.data.allProviders?.filter(p => p.isSuccessful) || []

          toast.add({
            severity: 'success',
            summary: 'So sánh nhà vận chuyển thành công',
            detail: `Đã chọn ${result.data.selectedProviderName}: ${formatCurrency(selectedProviderResponse.totalFee)}`,
            life: 4000
          })

          return true
        } else {
          // No valid providers, fall back to manual
          isManualOverride.value = true
          isAutoCalculated.value = false

          toast.add({
            severity: 'warn',
            summary: 'Không tìm thấy nhà vận chuyển phù hợp',
            detail: result.data.selectionReason || 'Vui lòng nhập phí vận chuyển thủ công',
            life: 5000
          })

          return false
        }
      } else {
        throw new Error(result.message)
      }

    } catch (error) {
      console.error('Error comparing providers:', error)
      comparisonError.value = error.message

      // Fall back to single provider calculation
      return await calculateShippingFee(deliveryAddress, orderValue, weight)

    } finally {
      isComparingProviders.value = false
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
   * Select a specific provider from comparison results
   */
  const selectProvider = (providerName) => {
    if (!hasProviderComparison.value) return false

    if (providerName === 'MANUAL') {
      enableManualOverride()
      return true
    }

    if (providerName === 'AUTO') {
      selectedProvider.value = 'AUTO'
      const autoSelected = comparisonResults.value.selectedProvider
      if (autoSelected) {
        shippingFee.value = autoSelected.totalFee || 0
        estimatedDeliveryTime.value = autoSelected.estimatedTime || ''
        isAutoCalculated.value = true
        isManualOverride.value = false

        toast.add({
          severity: 'info',
          summary: 'Chọn tự động',
          detail: `Hệ thống đã chọn ${comparisonResults.value.selectedProviderName}`,
          life: 3000
        })
      }
      return true
    }

    // Select specific provider
    const providerResult = comparisonResults.value.allProviders?.find(
      p => p.providerName === providerName && p.isSuccessful
    )

    if (providerResult) {
      selectedProvider.value = providerName
      shippingFee.value = providerResult.response.totalFee || 0
      estimatedDeliveryTime.value = providerResult.response.estimatedTime || ''
      isAutoCalculated.value = true
      isManualOverride.value = false

      toast.add({
        severity: 'info',
        summary: 'Đã chọn nhà vận chuyển',
        detail: `${providerName}: ${formatCurrency(providerResult.response.totalFee)}`,
        life: 3000
      })

      return true
    }

    return false
  }

  /**
   * Get provider comparison summary for display
   */
  const getProviderComparisonSummary = () => {
    if (!hasProviderComparison.value) return null

    return {
      selectedProvider: comparisonResults.value.selectedProviderName,
      selectionReason: comparisonResults.value.selectionReason,
      totalProviders: comparisonResults.value.allProviders?.length || 0,
      successfulProviders: comparisonResults.value.allProviders?.filter(p => p.isSuccessful).length || 0,
      comparedAt: comparisonResults.value.comparedAt
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

    // Reset provider comparison state
    comparisonResults.value = null
    selectedProvider.value = 'AUTO'
    availableProviders.value = []
    comparisonError.value = null
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

    // Provider comparison state
    comparisonResults,
    selectedProvider,
    availableProviders,
    isComparingProviders,
    comparisonError,

    // Computed
    canAutoCalculate,
    shippingStatus,
    hasValidShippingFee,
    hasProviderComparison,
    canSelectProvider,
    selectedProviderInfo,

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
    formatCurrency
  }
}
