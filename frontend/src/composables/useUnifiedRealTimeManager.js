import { ref, watch, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useRealTimeOrderManagement } from './useRealTimeOrderManagement'
import { useRealTimePricing } from './useRealTimePricing'
import { useVoucherMonitoring } from './useVoucherMonitoring'

/**
 * Unified Real-Time State Manager with Enhanced Connection Management
 * Coordinates all real-time composables with enhanced integration, cross-tab synchronization,
 * message queuing for offline scenarios, and centralized notification management
 *
 * Enhanced Features:
 * - Intelligent connection management with network awareness
 * - Message queuing coordination for offline scenarios
 * - Enhanced state synchronization coordination
 * - Cross-tab synchronization with WebSocket coordination
 * - Network status monitoring and adaptive reconnection
 * - Centralized notification management with connection status
 *
 * Follows LapXpert patterns and Vietnamese business terminology
 */
export function useUnifiedRealTimeManager() {
  const toast = useToast()

  // Initialize individual composables
  const orderManagement = useRealTimeOrderManagement()
  const pricingManager = useRealTimePricing()
  const voucherMonitoring = useVoucherMonitoring()

  // Unified state management
  const isUnifiedConnected = ref(false)
  const unifiedConnectionQuality = ref('UNKNOWN')
  const lastUnifiedUpdate = ref(null)
  const unifiedMessageHistory = ref([])
  const activeSubscriptions = ref(new Set())

  // Cross-tab synchronization using BroadcastChannel API
  const crossTabChannel = ref(null)
  const crossTabSupported = ref(false)
  const crossTabEnabled = ref(true)
  const tabId = ref(generateTabId())

  // Message queuing for offline scenarios
  const messageQueue = ref([])
  const maxQueueSize = ref(100)
  const queuePersistenceEnabled = ref(true)
  const isProcessingQueue = ref(false)

  // Enhanced notification management
  const notificationHistory = ref([])
  const notificationSettings = ref({
    showPriceChanges: true,
    showVoucherUpdates: true,
    showOrderUpdates: true,
    showConnectionStatus: true,
    autoHideDelay: 5000
  })

  // Performance monitoring
  const performanceMetrics = ref({
    messagesProcessed: 0,
    averageProcessingTime: 0,
    queuedMessages: 0,
    crossTabMessages: 0,
    lastPerformanceCheck: null
  })

  /**
   * Generate unique tab identifier
   */
  function generateTabId() {
    return `tab_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
  }

  /**
   * Initialize cross-tab synchronization
   */
  function initializeCrossTabSync() {
    try {
      if (typeof BroadcastChannel !== 'undefined') {
        crossTabChannel.value = new BroadcastChannel('lapxpert_realtime_sync')
        crossTabSupported.value = true

        crossTabChannel.value.onmessage = (event) => {
          handleCrossTabMessage(event.data)
        }

        // Send initial tab registration
        broadcastToOtherTabs({
          type: 'TAB_REGISTRATION',
          tabId: tabId.value,
          timestamp: new Date().toISOString(),
          connectionStatus: isUnifiedConnected.value
        })

        console.log('✅ Cross-tab synchronization initialized for tab:', tabId.value)
      } else {
        crossTabSupported.value = false
        console.warn('⚠️ BroadcastChannel not supported, cross-tab sync disabled')
      }
    } catch (error) {
      crossTabSupported.value = false
      console.error('❌ Failed to initialize cross-tab sync:', error)
    }
  }

  /**
   * Handle cross-tab messages
   */
  function handleCrossTabMessage(data) {
    if (!crossTabEnabled.value || data.tabId === tabId.value) return

    try {
      performanceMetrics.value.crossTabMessages++

      switch (data.type) {
        case 'TAB_REGISTRATION':
          console.log('📡 New tab registered:', data.tabId)
          break

        case 'REAL_TIME_UPDATE':
          // Sync real-time updates across tabs
          syncRealTimeUpdate(data.payload)
          break

        case 'CONNECTION_STATUS_CHANGE':
          // Sync connection status changes
          if (data.payload.isConnected !== isUnifiedConnected.value) {
            showCrossTabNotification('Trạng thái kết nối đã thay đổi từ tab khác')
          }
          break

        case 'NOTIFICATION_SYNC':
          // Sync notifications across tabs
          syncNotificationAcrossTabs(data.payload)
          break

        case 'QUEUE_SYNC':
          // Sync message queue status
          if (data.payload.queueSize > 0) {
            showCrossTabNotification(`${data.payload.queueSize} tin nhắn đang chờ xử lý`)
          }
          break

        case 'ENTITY_STATE_SYNC':
          // Handle normalized entity state synchronization
          handleEntityStateSync(data.payload)
          break

        case 'OPTIMISTIC_OPERATION':
          // Handle optimistic operation synchronization
          handleOptimisticOperation(data.payload)
          break

        default:
          console.log('📨 Unknown cross-tab message type:', data.type)
      }
    } catch (error) {
      console.error('❌ Error handling cross-tab message:', error)
    }
  }

  /**
   * Broadcast message to other tabs
   */
  function broadcastToOtherTabs(data) {
    if (!crossTabSupported.value || !crossTabEnabled.value || !crossTabChannel.value) return

    try {
      crossTabChannel.value.postMessage({
        ...data,
        tabId: tabId.value,
        timestamp: new Date().toISOString()
      })
    } catch (error) {
      console.error('❌ Failed to broadcast to other tabs:', error)
    }
  }

  /**
   * Sync real-time update across tabs
   */
  function syncRealTimeUpdate(payload) {
    // Update unified message history
    unifiedMessageHistory.value.unshift({
      ...payload,
      crossTabSync: true,
      receivedAt: new Date()
    })

    // Limit history size
    if (unifiedMessageHistory.value.length > 200) {
      unifiedMessageHistory.value = unifiedMessageHistory.value.slice(0, 200)
    }

    lastUnifiedUpdate.value = new Date()
  }

  /**
   * Show cross-tab notification
   */
  function showCrossTabNotification(message) {
    if (!notificationSettings.value.showConnectionStatus) return

    toast.add({
      severity: 'info',
      summary: 'Đồng bộ đa tab',
      detail: message,
      life: 3000
    })
  }

  /**
   * Sync notification across tabs
   */
  function syncNotificationAcrossTabs(notification) {
    // Add to notification history without showing toast (to avoid duplicates)
    notificationHistory.value.unshift({
      ...notification,
      crossTabSync: true,
      receivedAt: new Date()
    })

    // Limit notification history
    if (notificationHistory.value.length > 50) {
      notificationHistory.value = notificationHistory.value.slice(0, 50)
    }
  }

  /**
   * Handle entity state synchronization across tabs
   */
  function handleEntityStateSync(payload) {
    try {
      const { entityType, operation, data, timestamp } = payload

      // Emit custom event for entity stores to listen to
      window.dispatchEvent(new CustomEvent('entity-state-sync', {
        detail: {
          entityType,
          operation,
          data,
          timestamp,
          source: 'cross-tab'
        }
      }))

      console.log(`🔄 Entity state sync: ${entityType} - ${operation}`)
    } catch (error) {
      console.error('❌ Error handling entity state sync:', error)
    }
  }

  /**
   * Handle optimistic operation synchronization
   */
  function handleOptimisticOperation(payload) {
    try {
      const { operationId, type, status, entityType, data } = payload

      // Emit custom event for optimistic update handlers
      window.dispatchEvent(new CustomEvent('optimistic-operation-sync', {
        detail: {
          operationId,
          type,
          status,
          entityType,
          data,
          source: 'cross-tab'
        }
      }))

      // Show notification for failed operations from other tabs
      if (status === 'failed') {
        showUnifiedNotification(
          'warn',
          'Thao tác thất bại',
          `Thao tác ${type} cho ${entityType} đã thất bại ở tab khác`,
          { life: 4000 }
        )
      }

      console.log(`⚡ Optimistic operation sync: ${type} - ${status}`)
    } catch (error) {
      console.error('❌ Error handling optimistic operation sync:', error)
    }
  }

  /**
   * Message queuing for offline scenarios
   */
  function addToMessageQueue(message) {
    const queuedMessage = {
      id: generateMessageId(),
      ...message,
      queuedAt: new Date().toISOString(),
      retryCount: 0,
      maxRetries: 3
    }

    messageQueue.value.unshift(queuedMessage)

    // Enforce queue size limit
    if (messageQueue.value.length > maxQueueSize.value) {
      messageQueue.value = messageQueue.value.slice(0, maxQueueSize.value)
    }

    // Persist to localStorage if enabled
    if (queuePersistenceEnabled.value) {
      persistMessageQueue()
    }

    // Update performance metrics
    performanceMetrics.value.queuedMessages++

    // Broadcast queue status to other tabs
    broadcastToOtherTabs({
      type: 'QUEUE_SYNC',
      payload: {
        queueSize: messageQueue.value.length,
        latestMessage: queuedMessage
      }
    })

    console.log('📥 Message added to queue:', queuedMessage.id)
  }

  /**
   * Process message queue when connection is restored
   */
  async function processMessageQueue() {
    if (isProcessingQueue.value || messageQueue.value.length === 0) return

    isProcessingQueue.value = true
    const startTime = Date.now()

    try {
      console.log(`🔄 Processing ${messageQueue.value.length} queued messages...`)

      const processedMessages = []
      const failedMessages = []

      for (const queuedMessage of [...messageQueue.value]) {
        try {
          // Attempt to send the message
          const success = await sendQueuedMessage(queuedMessage)

          if (success) {
            processedMessages.push(queuedMessage.id)
            // Remove from queue
            messageQueue.value = messageQueue.value.filter(msg => msg.id !== queuedMessage.id)
          } else {
            // Increment retry count
            queuedMessage.retryCount++
            if (queuedMessage.retryCount >= queuedMessage.maxRetries) {
              failedMessages.push(queuedMessage.id)
              // Remove failed message from queue
              messageQueue.value = messageQueue.value.filter(msg => msg.id !== queuedMessage.id)
            }
          }
        } catch (error) {
          console.error('❌ Error processing queued message:', queuedMessage.id, error)
          failedMessages.push(queuedMessage.id)
          messageQueue.value = messageQueue.value.filter(msg => msg.id !== queuedMessage.id)
        }
      }

      // Update performance metrics
      const processingTime = Date.now() - startTime
      performanceMetrics.value.messagesProcessed += processedMessages.length
      performanceMetrics.value.averageProcessingTime =
        (performanceMetrics.value.averageProcessingTime + processingTime) / 2

      // Persist updated queue
      if (queuePersistenceEnabled.value) {
        persistMessageQueue()
      }

      // Show completion notification
      if (processedMessages.length > 0) {
        showUnifiedNotification(
          'success',
          'Đồng bộ hoàn tất',
          `Đã xử lý ${processedMessages.length} tin nhắn chờ`
        )
      }

      if (failedMessages.length > 0) {
        showUnifiedNotification(
          'warn',
          'Một số tin nhắn thất bại',
          `${failedMessages.length} tin nhắn không thể xử lý`
        )
      }

      console.log(`✅ Queue processing completed: ${processedMessages.length} success, ${failedMessages.length} failed`)

    } catch (error) {
      console.error('❌ Error processing message queue:', error)
      showUnifiedNotification('error', 'Lỗi xử lý hàng đợi', 'Không thể xử lý tin nhắn chờ')
    } finally {
      isProcessingQueue.value = false
    }
  }

  /**
   * Send a queued message
   */
  async function sendQueuedMessage(queuedMessage) {
    try {
      // Use the appropriate composable based on message type
      switch (queuedMessage.type) {
        case 'PRICE_UPDATE':
        case 'SUBSCRIBE_PRICE_UPDATES':
        case 'REQUEST_CURRENT_PRICES':
          return pricingManager.sendMessage ? pricingManager.sendMessage(queuedMessage) : false

        case 'VOUCHER_VALIDATION':
        case 'SUBSCRIBE_VOUCHER_MONITORING':
          return voucherMonitoring.sendMessage ? voucherMonitoring.sendMessage(queuedMessage) : false

        default:
          return orderManagement.sendMessage(queuedMessage)
      }
    } catch (error) {
      console.error('❌ Error sending queued message:', error)
      return false
    }
  }

  /**
   * Generate unique message ID
   */
  function generateMessageId() {
    return `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
  }

  /**
   * Persist message queue to localStorage
   */
  function persistMessageQueue() {
    try {
      localStorage.setItem('lapxpert_message_queue', JSON.stringify(messageQueue.value))
    } catch (error) {
      console.error('❌ Failed to persist message queue:', error)
    }
  }

  /**
   * Load message queue from localStorage
   */
  function loadPersistedMessageQueue() {
    try {
      const persistedQueue = localStorage.getItem('lapxpert_message_queue')
      if (persistedQueue) {
        const parsed = JSON.parse(persistedQueue)
        messageQueue.value = Array.isArray(parsed) ? parsed : []
        console.log(`📥 Loaded ${messageQueue.value.length} persisted messages from localStorage`)
      }
    } catch (error) {
      console.error('❌ Failed to load persisted message queue:', error)
      messageQueue.value = []
    }
  }

  /**
   * Clear message queue
   */
  function clearMessageQueue() {
    messageQueue.value = []
    if (queuePersistenceEnabled.value) {
      localStorage.removeItem('lapxpert_message_queue')
    }
    console.log('🗑️ Message queue cleared')
  }

  /**
   * Enhanced notification management with Vietnamese messages
   */
  function showUnifiedNotification(severity, summary, detail, options = {}) {
    const notification = {
      id: generateMessageId(),
      severity,
      summary,
      detail,
      timestamp: new Date().toISOString(),
      tabId: tabId.value,
      ...options
    }

    // Add to notification history
    notificationHistory.value.unshift(notification)

    // Limit notification history
    if (notificationHistory.value.length > 50) {
      notificationHistory.value = notificationHistory.value.slice(0, 50)
    }

    // Show toast notification
    toast.add({
      severity,
      summary,
      detail,
      life: options.life || notificationSettings.value.autoHideDelay
    })

    // Broadcast to other tabs
    broadcastToOtherTabs({
      type: 'NOTIFICATION_SYNC',
      payload: notification
    })

    console.log('📢 Unified notification:', summary, detail)
  }

  /**
   * Show price change notification with Vietnamese formatting
   */
  function showPriceChangeNotification(priceUpdate) {
    if (!notificationSettings.value.showPriceChanges) return

    const { variantId, oldPrice, newPrice, productName } = priceUpdate
    const priceChange = newPrice - oldPrice
    const changePercent = ((priceChange / oldPrice) * 100).toFixed(1)
    const isIncrease = priceChange > 0

    const severity = isIncrease ? 'warn' : 'success'
    const changeText = isIncrease ? 'tăng' : 'giảm'
    const formattedChange = Math.abs(priceChange).toLocaleString('vi-VN')
    const formattedNewPrice = newPrice.toLocaleString('vi-VN')

    showUnifiedNotification(
      severity,
      `Giá ${changeText} ${changePercent}%`,
      `${productName || `Sản phẩm #${variantId}`}: ${formattedChange}₫ → ${formattedNewPrice}₫`,
      { life: 6000 }
    )
  }

  /**
   * Show voucher notification with Vietnamese formatting
   */
  function showVoucherNotification(voucherUpdate) {
    if (!notificationSettings.value.showVoucherUpdates) return

    const { type, voucherCode, message } = voucherUpdate

    let severity = 'info'
    let summary = 'Cập nhật voucher'

    switch (type) {
      case 'EXPIRED':
        severity = 'warn'
        summary = 'Voucher hết hạn'
        break
      case 'NEW':
        severity = 'success'
        summary = 'Voucher mới'
        break
      case 'ALTERNATIVE':
        severity = 'info'
        summary = 'Gợi ý voucher thay thế'
        break
      case 'BETTER_SUGGESTION':
        severity = 'success'
        summary = 'Voucher tốt hơn có sẵn'
        break
    }

    showUnifiedNotification(
      severity,
      summary,
      message || `Voucher ${voucherCode} đã được cập nhật`,
      { life: 7000 }
    )
  }

  /**
   * Show order notification with Vietnamese formatting
   */
  function showOrderNotification(orderUpdate) {
    if (!notificationSettings.value.showOrderUpdates) return

    const { type, orderId, status, message } = orderUpdate

    let severity = 'info'
    let summary = 'Cập nhật đơn hàng'

    switch (type) {
      case 'STATUS_CHANGE':
        severity = 'info'
        summary = 'Trạng thái đơn hàng thay đổi'
        break
      case 'PAYMENT_CONFIRMED':
        severity = 'success'
        summary = 'Thanh toán thành công'
        break
      case 'SHIPPED':
        severity = 'success'
        summary = 'Đơn hàng đã giao'
        break
      case 'CANCELLED':
        severity = 'warn'
        summary = 'Đơn hàng đã hủy'
        break
    }

    showUnifiedNotification(
      severity,
      summary,
      message || `Đơn hàng #${orderId} - ${status}`,
      { life: 8000 }
    )
  }

  // Unified state coordination - watch for changes in individual composables
  watch(
    () => orderManagement.isConnected,
    (newValue) => {
      isUnifiedConnected.value = newValue

      // Broadcast connection status change
      broadcastToOtherTabs({
        type: 'CONNECTION_STATUS_CHANGE',
        payload: {
          isConnected: newValue,
          quality: orderManagement.connectionQuality?.value || 'UNKNOWN'
        }
      })

      // Process message queue when connection is restored
      if (newValue && messageQueue.value.length > 0) {
        setTimeout(() => {
          processMessageQueue()
        }, 1000) // Small delay to ensure connection is stable
      }

      // Show connection status notification
      if (notificationSettings.value.showConnectionStatus) {
        const severity = newValue ? 'success' : 'warn'
        const summary = newValue ? 'Kết nối thành công' : 'Mất kết nối'
        const detail = newValue
          ? 'Real-time đã được kích hoạt'
          : 'Đang thử kết nối lại...'

        showUnifiedNotification(severity, summary, detail, { life: 3000 })
      }
    },
    { immediate: true }
  )

  // Watch for connection quality changes
  watch(
    () => orderManagement.connectionQuality,
    (newQuality) => {
      unifiedConnectionQuality.value = newQuality
    },
    { immediate: true }
  )

  // Setup integration callbacks with individual composables
  const setupIntegrationCallbacks = () => {
    // Setup order management callbacks
    orderManagement.setIntegrationCallback('onMessage', (message) => {
      // Add to unified message history
      unifiedMessageHistory.value.unshift({
        ...message,
        source: 'orderManagement',
        processedAt: new Date()
      })

      // Broadcast to other tabs
      broadcastToOtherTabs({
        type: 'REAL_TIME_UPDATE',
        payload: message
      })

      lastUnifiedUpdate.value = new Date()
      performanceMetrics.value.messagesProcessed++
    })

    orderManagement.setIntegrationCallback('onConnectionChange', (isConnected, quality) => {
      isUnifiedConnected.value = isConnected
      unifiedConnectionQuality.value = quality

      // Enhanced connection status with network awareness
      const enhancedStatus = {
        isConnected,
        quality,
        networkStatus: orderManagement.networkStatus?.value || 'UNKNOWN',
        stability: orderManagement.connectionStability?.value || 0,
        hasQueuedMessages: orderManagement.hasQueuedMessages?.value || false
      }

      // Broadcast enhanced connection status change
      broadcastToOtherTabs({
        type: 'CONNECTION_STATUS_CHANGE',
        payload: enhancedStatus
      })

      if (isConnected) {
        console.log('🔗 Enhanced WebSocket connected with quality:', quality,
                    'network:', enhancedStatus.networkStatus,
                    'stability:', enhancedStatus.stability + '%')

        // Process message queue when connection is restored
        if (messageQueue.value.length > 0) {
          setTimeout(() => {
            processMessageQueue()
          }, 1000)
        }

        // Process WebSocket queued messages if available
        if (orderManagement.hasQueuedMessages?.value && orderManagement.processQueuedMessages) {
          setTimeout(() => {
            orderManagement.processQueuedMessages()
          }, 500)
        }

        // Show connection restored notification with enhanced info
        if (notificationSettings.value.showConnectionStatus) {
          showUnifiedNotification(
            'success',
            'Kết nối khôi phục',
            `WebSocket đã kết nối (${quality}, mạng: ${enhancedStatus.networkStatus})`,
            { life: 3000 }
          )
        }
      } else {
        console.log('🔗 Enhanced WebSocket disconnected, network:', enhancedStatus.networkStatus)

        // Show disconnection notification with network status
        if (notificationSettings.value.showConnectionStatus) {
          showUnifiedNotification(
            'warn',
            'Mất kết nối',
            `WebSocket ngắt kết nối (mạng: ${enhancedStatus.networkStatus})`,
            { life: 4000 }
          )
        }
      }
    })

    orderManagement.setIntegrationCallback('onQueueMessage', (message) => {
      addToMessageQueue(message)
    })

    // Setup pricing callbacks
    pricingManager.setIntegrationCallback('onPriceUpdate', (priceUpdate) => {
      showPriceChangeNotification(priceUpdate)

      // Broadcast to other tabs
      broadcastToOtherTabs({
        type: 'REAL_TIME_UPDATE',
        payload: {
          type: 'PRICE_UPDATE',
          data: priceUpdate
        }
      })
    })

    pricingManager.setIntegrationCallback('onVariantAffected', (variantId) => {
      // Track affected variants in unified state
      activeSubscriptions.value.add(`price_${variantId}`)
    })

    // Setup voucher monitoring callbacks
    voucherMonitoring.setIntegrationCallback('onVoucherExpired', (voucher) => {
      showVoucherNotification({
        type: 'EXPIRED',
        voucherCode: voucher.code,
        message: `Voucher ${voucher.code} đã hết hạn`
      })
    })

    voucherMonitoring.setIntegrationCallback('onNewVoucher', (voucher) => {
      showVoucherNotification({
        type: 'NEW',
        voucherCode: voucher.code,
        message: `Voucher mới ${voucher.code} đã có hiệu lực`
      })
    })

    voucherMonitoring.setIntegrationCallback('onAlternativeRecommendation', (recommendation) => {
      showVoucherNotification({
        type: 'ALTERNATIVE',
        voucherCode: recommendation.expiredVoucherCode,
        message: `Tìm thấy voucher thay thế cho ${recommendation.expiredVoucherCode}`
      })
    })

    voucherMonitoring.setIntegrationCallback('onBetterSuggestion', (suggestion) => {
      showVoucherNotification({
        type: 'BETTER_SUGGESTION',
        voucherCode: suggestion.betterVoucher?.code,
        message: `Tìm thấy voucher tốt hơn tiết kiệm ${formatCurrency(suggestion.savingsAmount)}`
      })
    })

    console.log('✅ Integration callbacks setup completed')
  }

  /**
   * Format currency for Vietnamese display
   */
  function formatCurrency(amount) {
    if (amount == null) return '0₫'
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount)
  }

  // Initialize persisted message queue and cross-tab sync
  loadPersistedMessageQueue()
  initializeCrossTabSync()
  setupIntegrationCallbacks()

  // Cleanup on unmount
  onUnmounted(() => {
    if (crossTabChannel.value) {
      crossTabChannel.value.close()
    }
  })

  return {
    // Unified state
    isUnifiedConnected,
    unifiedConnectionQuality,
    lastUnifiedUpdate,
    unifiedMessageHistory,
    activeSubscriptions,

    // Cross-tab synchronization
    crossTabSupported,
    crossTabEnabled,
    tabId,

    // Message queuing
    messageQueue,
    maxQueueSize,
    queuePersistenceEnabled,
    isProcessingQueue,

    // Notification management
    notificationHistory,
    notificationSettings,

    // Performance monitoring
    performanceMetrics,

    // Individual composables (for direct access if needed)
    orderManagement,
    pricingManager,
    voucherMonitoring,

    // Methods
    broadcastToOtherTabs,
    syncRealTimeUpdate,
    showCrossTabNotification,

    // Message queue methods
    addToMessageQueue,
    processMessageQueue,
    clearMessageQueue,

    // Notification methods
    showUnifiedNotification,
    showPriceChangeNotification,
    showVoucherNotification,
    showOrderNotification
  }
}
