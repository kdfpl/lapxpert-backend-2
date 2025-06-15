import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useRealTimeOrderManagement } from './useRealTimeOrderManagement'

/**
 * Voucher Monitoring Composable
 * Handles real-time voucher expiration, new voucher notifications, and alternative recommendations
 * Integrates with existing voucher system and follows Vietnamese business terminology
 */
export function useVoucherMonitoring() {
  const toast = useToast()
  const { messageHistory, isConnected, sendMessage } = useRealTimeOrderManagement()
  
  // Voucher monitoring state
  const expiredVouchers = ref([])
  const newVouchers = ref([])
  const alternativeRecommendations = ref([])
  const voucherNotifications = ref([])
  
  // Settings
  const showVoucherNotifications = ref(true)
  const autoApplyAlternatives = ref(false)
  
  // Watch for voucher-related messages
  watch(messageHistory, (newHistory) => {
    const voucherMessages = newHistory.filter(msg => 
      msg.type?.includes('VOUCHER') || 
      msg.topic?.includes('/topic/phieu-giam-gia/')
    )
    
    voucherMessages.forEach(processVoucherMessage)
  }, { deep: true })
  
  /**
   * Process incoming voucher message
   */
  const processVoucherMessage = (message) => {
    try {
      const messageType = message.type || extractTypeFromTopic(message.topic)
      
      switch (messageType) {
        case 'VOUCHER_EXPIRED':
          handleExpiredVoucher(message)
          break
        case 'VOUCHER_NEW':
          handleNewVoucher(message)
          break
        case 'VOUCHER_ALTERNATIVES':
          handleAlternativeRecommendations(message)
          break
        default:
          console.log('Unknown voucher message type:', messageType, message)
      }
    } catch (error) {
      console.error('Error processing voucher message:', error, message)
    }
  }
  
  /**
   * Extract message type from topic
   */
  const extractTypeFromTopic = (topic) => {
    if (!topic) return null
    
    if (topic.includes('/expired')) return 'VOUCHER_EXPIRED'
    if (topic.includes('/new')) return 'VOUCHER_NEW'
    if (topic.includes('/alternatives')) return 'VOUCHER_ALTERNATIVES'
    
    return null
  }
  
  /**
   * Handle expired voucher notification
   */
  const handleExpiredVoucher = (message) => {
    const expiredVoucher = {
      id: message.voucherId || Date.now(),
      code: message.voucherCode || message.maPhieuGiamGia,
      description: message.voucherDescription || message.moTa,
      discountValue: message.discountValue || message.giaTriGiam,
      discountType: message.discountType || message.loaiGiamGia,
      expirationTime: message.expirationTime || message.thoiGianHetHan,
      message: message.message || 'Phiếu giảm giá đã hết hạn',
      timestamp: new Date(message.timestamp || Date.now())
    }
    
    expiredVouchers.value.unshift(expiredVoucher)
    
    if (showVoucherNotifications.value) {
      showExpiredVoucherNotification(expiredVoucher)
    }
    
    // Keep only last 50 expired vouchers
    if (expiredVouchers.value.length > 50) {
      expiredVouchers.value = expiredVouchers.value.slice(0, 50)
    }
    
    console.log('🚨 Voucher expired:', expiredVoucher)
  }
  
  /**
   * Handle new voucher notification
   */
  const handleNewVoucher = (message) => {
    const newVoucher = {
      id: message.voucherId || Date.now(),
      code: message.voucherCode || message.maPhieuGiamGia,
      description: message.voucherDescription || message.moTa,
      discountValue: message.discountValue || message.giaTriGiam,
      discountType: message.discountType || message.loaiGiamGia,
      minimumOrderValue: message.minimumOrderValue || message.giaTriDonHangToiThieu,
      remainingQuantity: message.remainingQuantity || message.soLuongConLai,
      expirationTime: message.expirationTime || message.thoiGianHetHan,
      message: message.message || 'Phiếu giảm giá mới có hiệu lực',
      timestamp: new Date(message.timestamp || Date.now())
    }
    
    newVouchers.value.unshift(newVoucher)
    
    if (showVoucherNotifications.value) {
      showNewVoucherNotification(newVoucher)
    }
    
    // Keep only last 20 new vouchers
    if (newVouchers.value.length > 20) {
      newVouchers.value = newVouchers.value.slice(0, 20)
    }
    
    console.log('🎉 New voucher available:', newVoucher)
  }
  
  /**
   * Handle alternative voucher recommendations
   */
  const handleAlternativeRecommendations = (message) => {
    const recommendation = {
      id: Date.now(),
      expiredVoucherId: message.expiredVoucherId,
      expiredVoucherCode: message.expiredVoucherCode,
      primaryAlternative: message.primaryAlternative,
      additionalAlternatives: message.additionalAlternatives || [],
      message: message.message || 'Tìm thấy phiếu giảm giá thay thế',
      timestamp: new Date(message.timestamp || Date.now())
    }
    
    alternativeRecommendations.value.unshift(recommendation)
    
    if (showVoucherNotifications.value) {
      showAlternativeRecommendationNotification(recommendation)
    }
    
    // Keep only last 10 recommendations
    if (alternativeRecommendations.value.length > 10) {
      alternativeRecommendations.value = alternativeRecommendations.value.slice(0, 10)
    }
    
    console.log('💡 Alternative vouchers recommended:', recommendation)
  }
  
  /**
   * Show expired voucher notification
   */
  const showExpiredVoucherNotification = (voucher) => {
    const notification = {
      severity: 'warn',
      summary: '⏰ Voucher hết hạn',
      detail: `${voucher.code} - ${voucher.description || 'Phiếu giảm giá'} đã hết hiệu lực`,
      life: 8000,
      group: 'voucher-updates'
    }
    
    toast.add(notification)
    addToNotificationHistory(notification, voucher, 'EXPIRED')
  }
  
  /**
   * Show new voucher notification
   */
  const showNewVoucherNotification = (voucher) => {
    const notification = {
      severity: 'success',
      summary: '🎉 Voucher mới',
      detail: `${voucher.code} - Giảm ${formatCurrency(voucher.discountValue)} đã có hiệu lực`,
      life: 6000,
      group: 'voucher-updates'
    }
    
    toast.add(notification)
    addToNotificationHistory(notification, voucher, 'NEW')
  }
  
  /**
   * Show alternative recommendation notification
   */
  const showAlternativeRecommendationNotification = (recommendation) => {
    const alternativeCount = 1 + (recommendation.additionalAlternatives?.length || 0)
    
    const notification = {
      severity: 'info',
      summary: '💡 Voucher thay thế',
      detail: `Tìm thấy ${alternativeCount} phiếu giảm giá thay thế cho ${recommendation.expiredVoucherCode}`,
      life: 10000,
      group: 'voucher-updates'
    }
    
    toast.add(notification)
    addToNotificationHistory(notification, recommendation, 'ALTERNATIVES')
  }
  
  /**
   * Add notification to history
   */
  const addToNotificationHistory = (notification, data, type) => {
    voucherNotifications.value.unshift({
      ...notification,
      id: Date.now(),
      timestamp: new Date(),
      data,
      type
    })
    
    // Keep only last 50 notifications
    if (voucherNotifications.value.length > 50) {
      voucherNotifications.value = voucherNotifications.value.slice(0, 50)
    }
  }
  
  /**
   * Subscribe to voucher monitoring
   */
  const subscribeToVoucherMonitoring = () => {
    if (!isConnected.value) {
      console.warn('Cannot subscribe to voucher monitoring: WebSocket not connected')
      return false
    }
    
    const subscriptionMessage = {
      type: 'SUBSCRIBE_VOUCHER_MONITORING',
      topics: [
        '/topic/phieu-giam-gia/expired',
        '/topic/phieu-giam-gia/new',
        '/topic/phieu-giam-gia/alternatives'
      ],
      timestamp: new Date().toISOString()
    }
    
    return sendMessage(subscriptionMessage)
  }
  
  /**
   * Request voucher validation
   */
  const requestVoucherValidation = (voucherCode, customerId, orderTotal) => {
    if (!isConnected.value) {
      console.warn('Cannot request voucher validation: WebSocket not connected')
      return false
    }
    
    const validationMessage = {
      type: 'VALIDATE_VOUCHER',
      voucherCode,
      customerId,
      orderTotal,
      timestamp: new Date().toISOString()
    }
    
    return sendMessage(validationMessage)
  }
  
  /**
   * Toggle voucher notifications
   */
  const toggleVoucherNotifications = () => {
    showVoucherNotifications.value = !showVoucherNotifications.value
    
    toast.add({
      severity: 'info',
      summary: 'Cài đặt thông báo',
      detail: showVoucherNotifications.value 
        ? 'Đã bật thông báo voucher' 
        : 'Đã tắt thông báo voucher',
      life: 3000
    })
  }
  
  /**
   * Clear voucher history
   */
  const clearVoucherHistory = () => {
    expiredVouchers.value = []
    newVouchers.value = []
    alternativeRecommendations.value = []
    voucherNotifications.value = []
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
  const recentExpiredVouchers = computed(() => {
    const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000)
    return expiredVouchers.value.filter(voucher => 
      voucher.timestamp > oneHourAgo
    )
  })
  
  const recentNewVouchers = computed(() => {
    const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000)
    return newVouchers.value.filter(voucher => 
      voucher.timestamp > oneHourAgo
    )
  })
  
  const hasVoucherUpdates = computed(() => {
    return expiredVouchers.value.length > 0 || 
           newVouchers.value.length > 0 || 
           alternativeRecommendations.value.length > 0
  })
  
  return {
    // State
    expiredVouchers,
    newVouchers,
    alternativeRecommendations,
    voucherNotifications,
    showVoucherNotifications,
    autoApplyAlternatives,
    
    // Computed
    recentExpiredVouchers,
    recentNewVouchers,
    hasVoucherUpdates,
    
    // Methods
    subscribeToVoucherMonitoring,
    requestVoucherValidation,
    toggleVoucherNotifications,
    clearVoucherHistory,
    formatCurrency,
    
    // Internal methods (for testing)
    processVoucherMessage,
    handleExpiredVoucher,
    handleNewVoucher,
    handleAlternativeRecommendations
  }
}
