import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useRealTimeOrderManagement } from './useRealTimeOrderManagement'

/**
 * Order Expiration Composable
 * Handles real-time order expiration monitoring, countdown timers, and notifications
 * Integrates with existing WebSocket system and follows Vietnamese business terminology
 */
export function useOrderExpiration() {
  const toast = useToast()
  const { messageHistory, isConnected, sendMessage } = useRealTimeOrderManagement()
  
  // Order expiration state
  const expiringOrders = ref([])
  const expiredOrders = ref([])
  const inventoryReleaseNotifications = ref([])
  const orderStatusChanges = ref([])
  
  // Settings
  const showExpirationNotifications = ref(true)
  const expirationWarningThreshold = ref(60) // minutes before expiration to show warning
  
  // Countdown timers
  const countdownTimers = ref(new Map())
  const countdownInterval = ref(null)
  
  // Watch for order expiration messages
  watch(messageHistory, (newHistory) => {
    const expirationMessages = newHistory.filter(msg => 
      msg.type?.includes('ORDER_') || 
      msg.topic?.includes('/topic/don-hang/') ||
      msg.type?.includes('INVENTORY_')
    )
    
    expirationMessages.forEach(processExpirationMessage)
  }, { deep: true })
  
  /**
   * Process incoming order expiration message
   */
  const processExpirationMessage = (message) => {
    try {
      const messageType = message.type || extractTypeFromTopic(message.topic)
      
      switch (messageType) {
        case 'ORDER_EXPIRING_SOON':
          handleOrderExpiringSoon(message)
          break
        case 'ORDER_EXPIRED':
          handleOrderExpired(message)
          break
        case 'INVENTORY_RELEASED':
          handleInventoryReleased(message)
          break
        case 'ORDER_STATUS_CHANGED':
          handleOrderStatusChanged(message)
          break
        default:
          console.log('Unknown order expiration message type:', messageType, message)
      }
    } catch (error) {
      console.error('Error processing order expiration message:', error, message)
    }
  }
  
  /**
   * Extract message type from topic
   */
  const extractTypeFromTopic = (topic) => {
    if (!topic) return null
    
    if (topic.includes('/expiring')) return 'ORDER_EXPIRING_SOON'
    if (topic.includes('/expired')) return 'ORDER_EXPIRED'
    if (topic.includes('/inventory-released')) return 'INVENTORY_RELEASED'
    if (topic.includes('/status-changed')) return 'ORDER_STATUS_CHANGED'
    
    return null
  }
  
  /**
   * Handle order expiring soon notification
   */
  const handleOrderExpiringSoon = (message) => {
    const expiringOrder = {
      id: message.orderId || message.donHangId,
      orderCode: message.orderCode || message.maDonHang,
      customerName: message.customerName || message.tenKhachHang,
      totalAmount: message.totalAmount || message.tongTien,
      expirationTime: new Date(message.expirationTime || message.thoiGianHetHan),
      remainingMinutes: message.remainingMinutes || message.soPhutConLai,
      message: message.message || 'Đơn hàng sắp hết hạn',
      timestamp: new Date(message.timestamp || Date.now())
    }
    
    // Add to expiring orders list
    const existingIndex = expiringOrders.value.findIndex(order => order.id === expiringOrder.id)
    if (existingIndex >= 0) {
      expiringOrders.value[existingIndex] = expiringOrder
    } else {
      expiringOrders.value.unshift(expiringOrder)
    }
    
    // Start countdown timer for this order
    startCountdownTimer(expiringOrder)
    
    if (showExpirationNotifications.value) {
      showExpiringOrderNotification(expiringOrder)
    }
    
    // Keep only last 20 expiring orders
    if (expiringOrders.value.length > 20) {
      expiringOrders.value = expiringOrders.value.slice(0, 20)
    }
    
    console.log('⏰ Order expiring soon:', expiringOrder)
  }
  
  /**
   * Handle order expired notification
   */
  const handleOrderExpired = (message) => {
    const expiredOrder = {
      id: message.orderId || message.donHangId,
      orderCode: message.orderCode || message.maDonHang,
      customerName: message.customerName || message.tenKhachHang,
      totalAmount: message.totalAmount || message.tongTien,
      expiredAt: new Date(message.expiredAt || message.thoiGianHetHan),
      reason: message.reason || message.lyDo || 'Hết hạn thanh toán',
      message: message.message || 'Đơn hàng đã hết hạn',
      timestamp: new Date(message.timestamp || Date.now())
    }
    
    expiredOrders.value.unshift(expiredOrder)
    
    // Remove from expiring orders list
    expiringOrders.value = expiringOrders.value.filter(order => order.id !== expiredOrder.id)
    
    // Stop countdown timer
    stopCountdownTimer(expiredOrder.id)
    
    if (showExpirationNotifications.value) {
      showExpiredOrderNotification(expiredOrder)
    }
    
    // Keep only last 50 expired orders
    if (expiredOrders.value.length > 50) {
      expiredOrders.value = expiredOrders.value.slice(0, 50)
    }
    
    console.log('❌ Order expired:', expiredOrder)
  }
  
  /**
   * Handle inventory released notification
   */
  const handleInventoryReleased = (message) => {
    const inventoryRelease = {
      id: Date.now(),
      orderId: message.orderId || message.donHangId,
      orderCode: message.orderCode || message.maDonHang,
      releasedItems: message.releasedItems || message.sanPhamDaGiai || [],
      totalItemsReleased: message.totalItemsReleased || message.tongSoLuongDaGiai || 0,
      message: message.message || 'Đã giải phóng hàng tồn kho',
      timestamp: new Date(message.timestamp || Date.now())
    }
    
    inventoryReleaseNotifications.value.unshift(inventoryRelease)
    
    if (showExpirationNotifications.value) {
      showInventoryReleaseNotification(inventoryRelease)
    }
    
    // Keep only last 20 notifications
    if (inventoryReleaseNotifications.value.length > 20) {
      inventoryReleaseNotifications.value = inventoryReleaseNotifications.value.slice(0, 20)
    }
    
    console.log('📦 Inventory released:', inventoryRelease)
  }
  
  /**
   * Handle order status changed notification
   */
  const handleOrderStatusChanged = (message) => {
    const statusChange = {
      id: Date.now(),
      orderId: message.orderId || message.donHangId,
      orderCode: message.orderCode || message.maDonHang,
      oldStatus: message.oldStatus || message.trangThaiCu,
      newStatus: message.newStatus || message.trangThaiMoi,
      reason: message.reason || message.lyDo || 'Thay đổi do hết hạn',
      message: message.message || 'Trạng thái đơn hàng đã thay đổi',
      timestamp: new Date(message.timestamp || Date.now())
    }
    
    orderStatusChanges.value.unshift(statusChange)
    
    if (showExpirationNotifications.value) {
      showOrderStatusChangeNotification(statusChange)
    }
    
    // Keep only last 30 status changes
    if (orderStatusChanges.value.length > 30) {
      orderStatusChanges.value = orderStatusChanges.value.slice(0, 30)
    }
    
    console.log('🔄 Order status changed:', statusChange)
  }
  
  /**
   * Show expiring order notification
   */
  const showExpiringOrderNotification = (order) => {
    const notification = {
      severity: 'warn',
      summary: '⏰ Đơn hàng sắp hết hạn',
      detail: `${order.orderCode} - Còn ${order.remainingMinutes} phút`,
      life: 8000,
      group: 'order-expiration'
    }
    
    toast.add(notification)
  }
  
  /**
   * Show expired order notification
   */
  const showExpiredOrderNotification = (order) => {
    const notification = {
      severity: 'error',
      summary: '❌ Đơn hàng đã hết hạn',
      detail: `${order.orderCode} - ${order.reason}`,
      life: 10000,
      group: 'order-expiration'
    }
    
    toast.add(notification)
  }
  
  /**
   * Show inventory release notification
   */
  const showInventoryReleaseNotification = (release) => {
    const notification = {
      severity: 'info',
      summary: '📦 Giải phóng tồn kho',
      detail: `${release.orderCode} - Đã giải phóng ${release.totalItemsReleased} sản phẩm`,
      life: 6000,
      group: 'order-expiration'
    }
    
    toast.add(notification)
  }
  
  /**
   * Show order status change notification
   */
  const showOrderStatusChangeNotification = (statusChange) => {
    const notification = {
      severity: 'info',
      summary: '🔄 Thay đổi trạng thái',
      detail: `${statusChange.orderCode} - ${statusChange.newStatus}`,
      life: 5000,
      group: 'order-expiration'
    }
    
    toast.add(notification)
  }
  
  /**
   * Start countdown timer for an order
   */
  const startCountdownTimer = (order) => {
    countdownTimers.value.set(order.id, {
      orderId: order.id,
      expirationTime: order.expirationTime,
      remainingTime: order.expirationTime.getTime() - Date.now()
    })
  }
  
  /**
   * Stop countdown timer for an order
   */
  const stopCountdownTimer = (orderId) => {
    countdownTimers.value.delete(orderId)
  }
  
  /**
   * Update countdown timers
   */
  const updateCountdowns = () => {
    const now = Date.now()
    
    countdownTimers.value.forEach((timer, orderId) => {
      const remainingTime = timer.expirationTime.getTime() - now
      
      if (remainingTime <= 0) {
        // Timer expired, remove it
        countdownTimers.value.delete(orderId)
      } else {
        // Update remaining time
        timer.remainingTime = remainingTime
      }
    })
  }
  
  /**
   * Format remaining time for display
   */
  const formatRemainingTime = (remainingTime) => {
    if (remainingTime <= 0) return 'Đã hết hạn'
    
    const hours = Math.floor(remainingTime / (1000 * 60 * 60))
    const minutes = Math.floor((remainingTime % (1000 * 60 * 60)) / (1000 * 60))
    
    if (hours > 0) {
      return `${hours}h ${minutes}m`
    } else {
      return `${minutes}m`
    }
  }
  
  /**
   * Get remaining time for specific order
   */
  const getRemainingTimeForOrder = (orderId) => {
    const timer = countdownTimers.value.get(orderId)
    return timer ? timer.remainingTime : 0
  }
  
  /**
   * Subscribe to order expiration monitoring
   */
  const subscribeToOrderExpiration = () => {
    if (!isConnected.value) {
      console.warn('Cannot subscribe to order expiration: WebSocket not connected')
      return false
    }
    
    const subscriptionMessage = {
      type: 'SUBSCRIBE_ORDER_EXPIRATION',
      topics: [
        '/topic/don-hang/expiring',
        '/topic/don-hang/expired',
        '/topic/don-hang/inventory-released',
        '/topic/don-hang/status-changed'
      ],
      timestamp: new Date().toISOString()
    }
    
    return sendMessage(subscriptionMessage)
  }
  
  /**
   * Toggle expiration notifications
   */
  const toggleExpirationNotifications = () => {
    showExpirationNotifications.value = !showExpirationNotifications.value
    
    toast.add({
      severity: 'info',
      summary: 'Cài đặt thông báo',
      detail: showExpirationNotifications.value 
        ? 'Đã bật thông báo hết hạn đơn hàng' 
        : 'Đã tắt thông báo hết hạn đơn hàng',
      life: 3000
    })
  }
  
  /**
   * Clear expiration history
   */
  const clearExpirationHistory = () => {
    expiringOrders.value = []
    expiredOrders.value = []
    inventoryReleaseNotifications.value = []
    orderStatusChanges.value = []
    countdownTimers.value.clear()
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
  const hasExpirationUpdates = computed(() => {
    return expiringOrders.value.length > 0 || 
           expiredOrders.value.length > 0 || 
           inventoryReleaseNotifications.value.length > 0
  })
  
  const criticalExpiringOrders = computed(() => {
    return expiringOrders.value.filter(order => order.remainingMinutes <= 30)
  })
  
  const activeCountdowns = computed(() => {
    return Array.from(countdownTimers.value.entries()).map(([orderId, timer]) => ({
      orderId,
      remainingTime: timer.remainingTime,
      formattedTime: formatRemainingTime(timer.remainingTime)
    }))
  })
  
  // Lifecycle hooks
  onMounted(() => {
    // Start countdown update interval
    countdownInterval.value = setInterval(updateCountdowns, 60000) // Update every minute
    
    // Subscribe to order expiration monitoring
    subscribeToOrderExpiration()
  })
  
  onUnmounted(() => {
    // Clear countdown interval
    if (countdownInterval.value) {
      clearInterval(countdownInterval.value)
    }
  })
  
  return {
    // State
    expiringOrders,
    expiredOrders,
    inventoryReleaseNotifications,
    orderStatusChanges,
    showExpirationNotifications,
    expirationWarningThreshold,
    countdownTimers,
    
    // Computed
    hasExpirationUpdates,
    criticalExpiringOrders,
    activeCountdowns,
    
    // Methods
    subscribeToOrderExpiration,
    toggleExpirationNotifications,
    clearExpirationHistory,
    formatRemainingTime,
    getRemainingTimeForOrder,
    formatCurrency,
    
    // Internal methods (for testing)
    processExpirationMessage,
    handleOrderExpiringSoon,
    handleOrderExpired,
    handleInventoryReleased,
    handleOrderStatusChanged
  }
}
