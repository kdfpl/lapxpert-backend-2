import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useRealTimeOrderManagement } from './useRealTimeOrderManagement'

/**
 * Real-time Pricing Composable
 * Handles real-time price updates and notifications for product variants
 * Integrates with existing order management and follows Vietnamese business terminology
 */
export function useRealTimePricing() {
  const toast = useToast()
  const { messageHistory, isConnected, sendMessage } = useRealTimeOrderManagement()

  // Price update state
  const priceUpdates = ref([])
  const lastPriceUpdate = ref(null)
  const affectedVariants = ref(new Set())

  // Price change notifications
  const priceChangeNotifications = ref([])
  const showPriceWarnings = ref(true)

  // Enhanced integration support
  const integrationCallbacks = ref({
    onPriceUpdate: null,
    onVariantAffected: null,
    onPriceNotification: null
  })

  // Watch for price update messages
  watch(messageHistory, (newHistory) => {
    const priceMessages = newHistory.filter(msg =>
      msg.type === 'PRICE_UPDATE' ||
      msg.topic?.includes('/topic/gia-san-pham/')
    )

    priceMessages.forEach(processePriceUpdate)
  }, { deep: true })

  /**
   * Process incoming price update message
   */
  const processePriceUpdate = (message) => {
    try {
      const priceUpdate = {
        id: message.id || Date.now(),
        variantId: message.variantId,
        productName: message.productName || message.tenSanPham,
        variantInfo: message.variantInfo || message.thongTinBienThe,
        oldPrice: message.oldPrice || message.giaCu,
        newPrice: message.newPrice || message.giaMoi,
        changeAmount: message.changeAmount || message.soTienThayDoi,
        changePercent: message.changePercent || message.phanTramThayDoi,
        reason: message.reason || message.lyDo || 'Cập nhật giá tự động',
        timestamp: message.timestamp || new Date(),
        severity: determinePriceSeverity(message)
      }

      // Add to price updates list
      priceUpdates.value.unshift(priceUpdate)
      lastPriceUpdate.value = priceUpdate

      // Track affected variants
      if (priceUpdate.variantId) {
        affectedVariants.value.add(priceUpdate.variantId)
      }

      // Call integration callbacks
      if (integrationCallbacks.value.onPriceUpdate) {
        integrationCallbacks.value.onPriceUpdate(priceUpdate)
      }

      if (integrationCallbacks.value.onVariantAffected && priceUpdate.variantId) {
        integrationCallbacks.value.onVariantAffected(priceUpdate.variantId, priceUpdate)
      }

      // Show notification if enabled
      if (showPriceWarnings.value) {
        showPriceChangeNotification(priceUpdate)
      }

      // Keep only last 100 price updates
      if (priceUpdates.value.length > 100) {
        priceUpdates.value = priceUpdates.value.slice(0, 100)
      }

      console.log('💰 Price update processed:', priceUpdate)

    } catch (error) {
      console.error('Error processing price update:', error, message)
    }
  }

  /**
   * Determine severity level for price change
   */
  const determinePriceSeverity = (update) => {
    const changePercent = Math.abs(update.changePercent || update.phanTramThayDoi || 0)

    if (changePercent >= 20) return 'error'    // 20%+ change
    if (changePercent >= 10) return 'warn'     // 10-19% change
    if (changePercent >= 5) return 'info'      // 5-9% change
    return 'success'                           // <5% change
  }

  /**
   * Show price change notification
   */
  const showPriceChangeNotification = (priceUpdate) => {
    const isIncrease = priceUpdate.newPrice > priceUpdate.oldPrice
    const changeText = isIncrease ? 'tăng' : 'giảm'
    const icon = isIncrease ? '📈' : '📉'

    const notification = {
      severity: priceUpdate.severity,
      summary: `${icon} Giá ${changeText}`,
      detail: `${priceUpdate.productName || 'Sản phẩm'} - ${priceUpdate.variantInfo || ''}\nGiá mới: ${formatCurrency(priceUpdate.newPrice)}`,
      life: 5000,
      group: 'price-updates'
    }

    toast.add(notification)

    // Add to notifications history
    priceChangeNotifications.value.unshift({
      ...notification,
      id: Date.now(),
      timestamp: new Date(),
      priceUpdate
    })

    // Keep only last 20 notifications
    if (priceChangeNotifications.value.length > 20) {
      priceChangeNotifications.value = priceChangeNotifications.value.slice(0, 20)
    }
  }

  /**
   * Check if variant has recent price changes
   */
  const hasRecentPriceChange = (variantId, minutesAgo = 30) => {
    const cutoffTime = new Date(Date.now() - minutesAgo * 60 * 1000)
    return priceUpdates.value.some(update =>
      update.variantId === variantId &&
      new Date(update.timestamp) > cutoffTime
    )
  }

  /**
   * Get price updates for specific variant
   */
  const getPriceUpdatesForVariant = (variantId) => {
    return priceUpdates.value.filter(update => update.variantId === variantId)
  }

  /**
   * Get latest price for variant
   */
  const getLatestPriceForVariant = (variantId) => {
    const updates = getPriceUpdatesForVariant(variantId)
    return updates.length > 0 ? updates[0].newPrice : null
  }

  /**
   * Subscribe to price updates for specific variants
   */
  const subscribeToPriceUpdates = (variantIds) => {
    if (!isConnected.value) {
      console.warn('Cannot subscribe to price updates: WebSocket not connected')
      return false
    }

    const subscriptionMessage = {
      type: 'SUBSCRIBE_PRICE_UPDATES',
      variantIds: Array.isArray(variantIds) ? variantIds : [variantIds],
      timestamp: new Date().toISOString()
    }

    return sendMessage(subscriptionMessage)
  }

  /**
   * Request current prices for variants
   */
  const requestCurrentPrices = (variantIds) => {
    if (!isConnected.value) {
      console.warn('Cannot request prices: WebSocket not connected')
      return false
    }

    const requestMessage = {
      type: 'REQUEST_CURRENT_PRICES',
      variantIds: Array.isArray(variantIds) ? variantIds : [variantIds],
      timestamp: new Date().toISOString()
    }

    return sendMessage(requestMessage)
  }

  /**
   * Toggle price change notifications
   */
  const togglePriceWarnings = () => {
    showPriceWarnings.value = !showPriceWarnings.value

    toast.add({
      severity: 'info',
      summary: 'Cài đặt thông báo',
      detail: showPriceWarnings.value
        ? 'Đã bật thông báo thay đổi giá'
        : 'Đã tắt thông báo thay đổi giá',
      life: 3000
    })
  }

  /**
   * Clear price update history
   */
  const clearPriceHistory = () => {
    priceUpdates.value = []
    priceChangeNotifications.value = []
    affectedVariants.value.clear()
    lastPriceUpdate.value = null
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

  // Computed properties
  const recentPriceUpdates = computed(() => {
    const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000)
    return priceUpdates.value.filter(update =>
      new Date(update.timestamp) > oneHourAgo
    )
  })

  const affectedVariantsList = computed(() => {
    return Array.from(affectedVariants.value)
  })

  const hasPriceUpdates = computed(() => {
    return priceUpdates.value.length > 0
  })

  return {
    // State
    priceUpdates,
    lastPriceUpdate,
    affectedVariants: affectedVariantsList,
    priceChangeNotifications,
    showPriceWarnings,

    // Computed
    recentPriceUpdates,
    hasPriceUpdates,

    // Methods
    hasRecentPriceChange,
    getPriceUpdatesForVariant,
    getLatestPriceForVariant,
    subscribeToPriceUpdates,
    requestCurrentPrices,
    togglePriceWarnings,
    clearPriceHistory,
    formatCurrency,

    // Internal methods (for testing)
    processePriceUpdate,
    determinePriceSeverity,

    // Integration support
    integrationCallbacks,
    setIntegrationCallback: (type, callback) => {
      if (integrationCallbacks.value.hasOwnProperty(type)) {
        integrationCallbacks.value[type] = callback
      }
    }
  }
}
