import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { Client } from '@stomp/stompjs'

/**
 * Real-time Order Management Composable
 * Provides STOMP WebSocket connection for real-time order updates, price changes, and voucher monitoring
 * Follows existing LapXpert patterns and Vietnamese business terminology
 */
export function useRealTimeOrderManagement() {
  const toast = useToast()

  // Enhanced WebSocket connection state
  const isConnected = ref(false)
  const connectionError = ref(null)
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 15 // Increased for better reliability
  const connectionStartTime = ref(null)
  const lastSuccessfulConnection = ref(null)

  // Enhanced health monitoring state
  const connectionQuality = ref('UNKNOWN') // EXCELLENT, GOOD, POOR, CRITICAL
  const lastHeartbeat = ref(null)
  const connectionLatency = ref(0)
  const messagesSent = ref(0)
  const messagesReceived = ref(0)
  const errorCount = ref(0)
  const isRecovering = ref(false)
  const networkStatus = ref('ONLINE') // ONLINE, OFFLINE, UNSTABLE
  const connectionStability = ref(100) // 0-100 percentage

  // Message queuing for offline scenarios
  const messageQueue = ref([])
  const maxQueueSize = ref(50)
  const queueProcessingEnabled = ref(true)

  // State synchronization tracking
  const stateSyncPending = ref(false)
  const lastStateSyncTime = ref(null)
  const syncRetryCount = ref(0)
  const maxSyncRetries = ref(3)

  // Real-time data
  const lastMessage = ref(null)
  const messageHistory = ref([])

  // Health status from server
  const serverHealthStatus = ref({
    healthy: false,
    status: 'UNKNOWN',
    activeConnections: 0,
    totalMessages: 0,
    lastCheck: null
  })

  // Enhanced integration support for unified manager
  const integrationCallbacks = ref({
    onMessage: null,
    onConnectionChange: null,
    onQueueMessage: null
  })

  // WebSocket URL - using environment variable or default
  const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'

  // STOMP client instance
  let stompClient = null
  let heartbeatTimer = null
  let healthCheckTimer = null
  let reconnectTimer = null

  // No authentication token needed for public WebSocket
  // WebSocket is used only for push notifications

  // Connection status
  const status = ref('CLOSED')

  // Enhanced computed properties for health monitoring
  const connectionHealthy = computed(() => {
    return isConnected.value &&
           connectionQuality.value !== 'CRITICAL' &&
           errorCount.value < 5 &&
           networkStatus.value !== 'OFFLINE'
  })

  const shouldReconnect = computed(() => {
    return !isConnected.value &&
           reconnectAttempts.value < maxReconnectAttempts &&
           !isRecovering.value &&
           networkStatus.value !== 'OFFLINE'
  })

  const connectionStable = computed(() => {
    return connectionStability.value >= 70 &&
           errorCount.value < 3 &&
           connectionQuality.value !== 'POOR'
  })

  const hasQueuedMessages = computed(() => {
    return messageQueue.value.length > 0
  })

  // Initialize STOMP WebSocket connection (using native WebSocket for simplicity)
  const initializeWebSocket = () => {
    try {
      // WebSocket is now public - no authentication required
      // Used only for push notifications (price updates, voucher alerts, etc.)
      console.log('🔄 Initializing public WebSocket connection...')

      // Create STOMP client with native WebSocket (simpler than SockJS)
      stompClient = new Client({
        // Use native WebSocket endpoint
        brokerURL: 'ws://localhost:8080/ws',

        // No authentication headers needed for public WebSocket
        // connectHeaders: {} // Not needed for public notifications

        // Debug logging (reduced for cleaner console)
        debug: (str) => {
          // Only log critical STOMP events, not all debug messages
          if (import.meta.env.DEV && (str.includes('ERROR') || str.includes('DISCONNECT'))) {
            console.log('🔧 STOMP Debug:', str)
          }
        },

        // Enhanced reconnection configuration with intelligent exponential backoff
        reconnectDelay: () => {
          // Adaptive backoff based on connection stability and network status
          let baseDelay = 1000

          if (networkStatus.value === 'UNSTABLE') {
            baseDelay = 2000 // Longer delay for unstable networks
          } else if (connectionStability.value < 50) {
            baseDelay = 1500 // Moderate delay for poor stability
          }

          const exponentialDelay = baseDelay * Math.pow(2, reconnectAttempts.value)
          const maxDelay = networkStatus.value === 'UNSTABLE' ? 60000 : 30000 // 1 min for unstable, 30s for normal
          const delay = Math.min(exponentialDelay, maxDelay)

          console.log(`🔄 Intelligent reconnect delay: ${delay}ms (attempt ${reconnectAttempts.value + 1}, network: ${networkStatus.value}, stability: ${connectionStability.value}%)`)
          return delay
        },
        heartbeatIncoming: 10000, // Match server heartbeat configuration
        heartbeatOutgoing: 10000,

        // Enhanced connection callbacks with comprehensive state tracking
        onConnect: (frame) => {
          isConnected.value = true
          connectionError.value = null
          reconnectAttempts.value = 0
          status.value = 'OPEN'
          connectionQuality.value = 'EXCELLENT'
          errorCount.value = 0
          isRecovering.value = false
          lastHeartbeat.value = new Date()
          connectionStartTime.value = new Date()
          lastSuccessfulConnection.value = new Date()
          networkStatus.value = 'ONLINE'
          connectionStability.value = 100

          console.log('✅ WebSocket connected successfully')

          // Subscribe to relevant topics including health monitoring
          subscribeToTopics()
          subscribeToHealthTopics()

          // Start enhanced health monitoring
          startHealthMonitoring()

          // Process any queued messages
          processQueuedMessages()

          // Trigger state synchronization if needed
          triggerStateSynchronization()

          // Call integration callback for connection change
          if (integrationCallbacks.value.onConnectionChange) {
            integrationCallbacks.value.onConnectionChange(true, connectionQuality.value)
          }

          // Show success notification (optional)
          // toast.add({
          //   severity: 'info',
          //   summary: 'Real-time kết nối',
          //   detail: 'Đã kích hoạt thông báo tự động với giám sát nâng cao',
          //   life: 2000
          // })
        },

        onDisconnect: (frame) => {
          isConnected.value = false
          status.value = 'CLOSED'
          connectionQuality.value = 'CRITICAL'
          networkStatus.value = 'OFFLINE'
          connectionStability.value = Math.max(0, connectionStability.value - 20)
          stopHealthMonitoring()

          console.log('❌ STOMP WebSocket disconnected', frame)

          // Update network status based on disconnection reason
          updateNetworkStatus()

          // Attempt reconnection if appropriate
          if (shouldReconnect.value) {
            scheduleIntelligentReconnection()
          }

          // Call integration callback for connection change
          if (integrationCallbacks.value.onConnectionChange) {
            integrationCallbacks.value.onConnectionChange(false, connectionQuality.value)
          }
        },

        onStompError: (frame) => {
          errorCount.value++
          connectionError.value = 'WebSocket không khả dụng'
          status.value = 'CLOSED'
          connectionQuality.value = 'CRITICAL'

          console.error('STOMP error:', frame.headers['message'], frame.body)
          recordError('STOMP_ERROR', frame.headers['message'] || 'Unknown STOMP error')

          // No authentication errors expected in public mode
          console.warn('WebSocket connection failed - server may be unavailable')
          connectionError.value = 'Kết nối WebSocket thất bại. Máy chủ có thể không khả dụng.'

          // Attempt recovery
          if (shouldReconnect.value) {
            initiateErrorRecovery('STOMP_ERROR')
          }
        },

        onWebSocketError: (error) => {
          errorCount.value++
          connectionError.value = 'WebSocket không khả dụng'
          status.value = 'CLOSED'
          connectionQuality.value = 'CRITICAL'

          console.warn('WebSocket error (non-critical):', error?.message || 'Connection failed')
          recordError('WEBSOCKET_ERROR', error?.message || 'Connection failed')
        },

        onWebSocketClose: (event) => {
          isConnected.value = false
          status.value = 'CLOSED'
          connectionQuality.value = 'CRITICAL'
          stopHealthMonitoring()

          console.log('WebSocket connection closed:', event.code, event.reason)
          recordError('CONNECTION_CLOSED', `Code: ${event.code}, Reason: ${event.reason}`)

          // Attempt reconnection if appropriate
          if (shouldReconnect.value) {
            scheduleReconnection()
          }
        }
      })

      // Activate the client (start connection)
      stompClient.activate()
      status.value = 'CONNECTING'
      console.log('🔄 Connecting to WebSocket...')

    } catch (error) {
      console.warn('STOMP WebSocket initialization failed (non-critical):', error?.message || 'Unknown error')
      connectionError.value = 'WebSocket không khả dụng'
      status.value = 'CLOSED'
    }
  }



  // STOMP client management functions
  const send = (destination, message, headers = {}) => {
    if (stompClient && stompClient.connected) {
      try {
        const messageToSend = typeof message === 'string' ? message : JSON.stringify(message)
        stompClient.publish({
          destination,
          body: messageToSend,
          headers
        })
        return true
      } catch (error) {
        console.error('Error sending STOMP message:', error)
        return false
      }
    }
    console.warn('STOMP client not connected, message not sent:', message)
    return false
  }

  const open = () => {
    if (stompClient && !stompClient.connected) {
      stompClient.activate()
      status.value = 'CONNECTING'
    } else {
      initializeWebSocket()
    }
  }

  const close = () => {
    if (stompClient && stompClient.connected) {
      stompClient.deactivate()
      status.value = 'CLOSING'
    }
  }

  // Computed properties
  const connectionStatus = computed(() => {
    switch (status.value) {
      case 'CONNECTING':
        return { text: 'Đang kết nối...', severity: 'info' }
      case 'OPEN':
        return { text: 'Đã kết nối', severity: 'success' }
      case 'CLOSING':
        return { text: 'Đang ngắt kết nối...', severity: 'warn' }
      case 'CLOSED':
        return { text: 'Đã ngắt kết nối', severity: 'error' }
      default:
        return { text: 'Không xác định', severity: 'secondary' }
    }
  })

  // Watch for incoming messages
  const processIncomingMessage = (message) => {
    try {
      const parsedMessage = typeof message === 'string' ? JSON.parse(message) : message
      lastMessage.value = parsedMessage
      messageHistory.value.unshift({
        ...parsedMessage,
        timestamp: new Date(),
        id: Date.now()
      })

      // Keep only last 50 messages
      if (messageHistory.value.length > 50) {
        messageHistory.value = messageHistory.value.slice(0, 50)
      }

      // Call integration callback if available
      if (integrationCallbacks.value.onMessage) {
        integrationCallbacks.value.onMessage(parsedMessage)
      }

      console.log('📨 Received WebSocket message:', parsedMessage)
    } catch (error) {
      console.error('Error parsing WebSocket message:', error, message)
    }
  }

  // Subscribe to relevant topics
  const subscribeToTopics = () => {
    if (!stompClient || !stompClient.connected) {
      console.warn('Cannot subscribe: STOMP client not connected')
      return
    }

    try {
      // Subscribe to comprehensive real-time topics for all DataTable entities
      const topics = [
        // Voucher monitoring topics
        '/topic/phieu-giam-gia/expired',
        '/topic/phieu-giam-gia/new',
        '/topic/phieu-giam-gia/alternatives',

        // Product and pricing topics
        '/topic/gia-san-pham/updates',
        '/topic/san-pham/new',
        '/topic/san-pham/updated',

        // Order management topics
        '/topic/hoa-don/new',
        '/topic/hoa-don/updated',
        '/topic/hoa-don/status-changed',

        // Discount campaign topics
        '/topic/dot-giam-gia/new',
        '/topic/dot-giam-gia/updated',
        '/topic/dot-giam-gia/status-changed',

        // User management topics
        '/topic/nguoi-dung/new',
        '/topic/nguoi-dung/updated',

        // Inventory management topics
        '/topic/ton-kho/updates',
        '/topic/ton-kho/low-stock',

        // Statistics and dashboard topics
        '/topic/thong-ke/updated',
        '/topic/dashboard/refresh'
      ]

      topics.forEach(topic => {
        stompClient.subscribe(topic, (message) => {
          try {
            messagesReceived.value++
            const parsedMessage = JSON.parse(message.body)
            processIncomingMessage({
              ...parsedMessage,
              topic: topic,
              timestamp: new Date()
            })
            updateConnectionQuality()
          } catch (error) {
            errorCount.value++
            console.error('Error parsing STOMP message from topic', topic, ':', error)
            recordError('MESSAGE_PARSE_ERROR', `Failed to parse message from ${topic}`)
          }
        })
        // Reduced logging: only log subscription count, not individual topics
        if (topics.indexOf(topic) === topics.length - 1) {
          console.log(`📡 Subscribed to ${topics.length} WebSocket topics`)
        }
      })
    } catch (error) {
      errorCount.value++
      console.error('Error subscribing to topics:', error)
      recordError('SUBSCRIPTION_ERROR', 'Failed to subscribe to topics')
    }
  }

  // Subscribe to health monitoring topics
  const subscribeToHealthTopics = () => {
    if (!stompClient || !stompClient.connected) {
      console.warn('Cannot subscribe to health topics: STOMP client not connected')
      return
    }

    try {
      // Subscribe to WebSocket health monitoring topics
      const healthTopics = [
        '/topic/websocket/health',
        '/topic/websocket/heartbeat',
        '/topic/websocket/recovery',
        '/topic/websocket/errors'
      ]

      healthTopics.forEach(topic => {
        stompClient.subscribe(topic, (message) => {
          try {
            const parsedMessage = JSON.parse(message.body)
            handleHealthMessage(topic, parsedMessage)
          } catch (error) {
            console.error('Error parsing health message from topic', topic, ':', error)
          }
        })
        console.log('🏥 Subscribed to health topic:', topic)
      })
    } catch (error) {
      console.error('Error subscribing to health topics:', error)
    }
  }

  // Health monitoring functions
  const startHealthMonitoring = () => {
    // Start heartbeat monitoring
    heartbeatTimer = setInterval(() => {
      if (isConnected.value) {
        lastHeartbeat.value = new Date()
        updateConnectionQuality()
      }
    }, 10000) // Every 10 seconds to match server heartbeat

    // Start periodic health checks
    healthCheckTimer = setInterval(() => {
      if (isConnected.value) {
        checkConnectionHealth()
      }
    }, 30000) // Every 30 seconds

    console.log('🏥 Health monitoring started')
  }

  const stopHealthMonitoring = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
    if (healthCheckTimer) {
      clearInterval(healthCheckTimer)
      healthCheckTimer = null
    }
    console.log('🏥 Health monitoring stopped')
  }

  const updateConnectionQuality = () => {
    if (!isConnected.value) {
      connectionQuality.value = 'CRITICAL'
      connectionStability.value = 0
      return
    }

    const now = new Date()
    const timeSinceLastHeartbeat = lastHeartbeat.value ?
      now - lastHeartbeat.value : Infinity

    // Enhanced connection quality assessment
    let qualityScore = 100

    // Deduct points for errors
    qualityScore -= errorCount.value * 10

    // Deduct points for heartbeat delays
    if (timeSinceLastHeartbeat > 30000) {
      qualityScore -= 40
    } else if (timeSinceLastHeartbeat > 15000) {
      qualityScore -= 20
    } else if (timeSinceLastHeartbeat > 10000) {
      qualityScore -= 10
    }

    // Deduct points for network instability
    if (networkStatus.value === 'UNSTABLE') {
      qualityScore -= 30
    } else if (networkStatus.value === 'OFFLINE') {
      qualityScore = 0
    }

    // Update connection stability
    connectionStability.value = Math.max(0, Math.min(100, qualityScore))

    // Determine connection quality based on score
    if (qualityScore >= 80) {
      connectionQuality.value = 'EXCELLENT'
    } else if (qualityScore >= 60) {
      connectionQuality.value = 'GOOD'
    } else if (qualityScore >= 30) {
      connectionQuality.value = 'POOR'
    } else {
      connectionQuality.value = 'CRITICAL'
    }

    // Update network status
    updateNetworkStatus()
  }

  const checkConnectionHealth = () => {
    if (!isConnected.value) return

    const now = new Date()
    const timeSinceLastHeartbeat = lastHeartbeat.value ?
      now - lastHeartbeat.value : Infinity

    // Check if connection is stale
    if (timeSinceLastHeartbeat > 60000) { // 1 minute
      console.warn('🚨 Connection appears stale, initiating recovery')
      initiateErrorRecovery('HEARTBEAT_TIMEOUT')
    }

    updateConnectionQuality()
  }

  const handleHealthMessage = (topic, message) => {
    switch (topic) {
      case '/topic/websocket/health':
        serverHealthStatus.value = message
        console.log('🏥 Received health status:', message)
        break
      case '/topic/websocket/heartbeat':
        lastHeartbeat.value = new Date()
        updateConnectionQuality()
        break
      case '/topic/websocket/recovery':
        console.log('🔧 Recovery notification:', message)
        if (message.sessionId && message.type === 'RECOVERY_SUCCESS') {
          isRecovering.value = false
          errorCount.value = Math.max(0, errorCount.value - 1)
        }
        break
      case '/topic/websocket/errors':
        console.warn('⚠️ Error notification:', message)
        break
    }
  }

  const recordError = (errorType, errorMessage) => {
    const errorRecord = {
      type: errorType,
      message: errorMessage,
      timestamp: new Date(),
      connectionQuality: connectionQuality.value
    }

    console.error('📝 Recording error:', errorRecord)

    // Add to message history for debugging
    messageHistory.value.unshift({
      ...errorRecord,
      topic: 'ERROR',
      id: Date.now()
    })

    updateConnectionQuality()
  }

  const initiateErrorRecovery = (errorType) => {
    if (isRecovering.value) {
      console.log('🔧 Recovery already in progress, skipping')
      return
    }

    isRecovering.value = true
    console.log(`🔧 Initiating error recovery for: ${errorType}`)

    // Implement exponential backoff for reconnection
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value), 30000)

    reconnectTimer = setTimeout(() => {
      if (shouldReconnect.value) {
        reconnectAttempts.value++
        console.log(`🔄 Recovery attempt ${reconnectAttempts.value}/${maxReconnectAttempts}`)
        reconnect()
      } else {
        isRecovering.value = false
        console.error('🚨 Max reconnection attempts reached or recovery not appropriate')
      }
    }, delay)
  }

  // Enhanced network status monitoring
  const updateNetworkStatus = () => {
    // Check browser online status
    if (!navigator.onLine) {
      networkStatus.value = 'OFFLINE'
      connectionStability.value = 0
      return
    }

    // Determine network status based on connection patterns
    const recentErrors = errorCount.value
    const connectionAge = connectionStartTime.value ?
      (new Date() - connectionStartTime.value) / 1000 : 0

    if (recentErrors >= 5 || connectionStability.value < 30) {
      networkStatus.value = 'UNSTABLE'
    } else if (recentErrors >= 2 || connectionStability.value < 70) {
      networkStatus.value = 'UNSTABLE'
    } else {
      networkStatus.value = 'ONLINE'
    }

    // Update connection stability based on error rate and connection age
    if (connectionAge > 60) { // After 1 minute of connection
      const errorRate = recentErrors / (connectionAge / 60) // errors per minute
      connectionStability.value = Math.max(0, 100 - (errorRate * 20))
    }
  }

  // Intelligent reconnection scheduling
  const scheduleIntelligentReconnection = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
    }

    updateNetworkStatus()

    // Use the enhanced reconnect delay logic
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value),
      networkStatus.value === 'UNSTABLE' ? 60000 : 30000)

    console.log(`⏰ Scheduling intelligent reconnection in ${delay}ms (network: ${networkStatus.value})`)

    reconnectTimer = setTimeout(() => {
      if (shouldReconnect.value) {
        reconnectAttempts.value++
        connect()
      }
    }, delay)
  }

  // State synchronization trigger
  const triggerStateSynchronization = () => {
    if (stateSyncPending.value) {
      console.log('🔄 State sync already pending, skipping')
      return
    }

    stateSyncPending.value = true
    lastStateSyncTime.value = new Date()

    // Emit state sync event for other components to handle
    if (integrationCallbacks.value.onStateSync) {
      integrationCallbacks.value.onStateSync()
    }

    // Reset sync state after a delay
    setTimeout(() => {
      stateSyncPending.value = false
    }, 5000)

    console.log('🔄 State synchronization triggered')
  }

  // Legacy reconnection function (kept for backward compatibility)
  const scheduleReconnection = () => {
    scheduleIntelligentReconnection()
  }

  // Enhanced message queuing for offline scenarios
  const queueMessage = (message, destination = '/app/message') => {
    if (messageQueue.value.length >= maxQueueSize.value) {
      // Remove oldest message if queue is full
      messageQueue.value.shift()
      console.warn('📦 Message queue full, removing oldest message')
    }

    messageQueue.value.push({
      message,
      destination,
      timestamp: new Date(),
      retryCount: 0
    })

    console.log('📦 Message queued for later delivery:', message)
  }

  // Process queued messages when connection is restored
  const processQueuedMessages = () => {
    if (!queueProcessingEnabled.value || messageQueue.value.length === 0) {
      return
    }

    console.log(`📦 Processing ${messageQueue.value.length} queued messages`)

    const messagesToProcess = [...messageQueue.value]
    messageQueue.value = []

    messagesToProcess.forEach(queuedMessage => {
      const success = sendMessage(queuedMessage.message, queuedMessage.destination)
      if (!success && queuedMessage.retryCount < 3) {
        // Re-queue failed messages with retry count
        queuedMessage.retryCount++
        messageQueue.value.push(queuedMessage)
      }
    })
  }

  // Enhanced send message with queuing support
  const sendMessage = (message, destination = '/app/message') => {
    if (!isConnected.value) {
      if (queueProcessingEnabled.value) {
        queueMessage(message, destination)
        return true // Queued for later delivery
      }
      console.warn('Cannot send message: STOMP client not connected and queuing disabled')
      return false
    }

    try {
      const success = send(destination, message)
      if (success) {
        messagesSent.value++
        console.log('📤 Sent STOMP message to', destination, ':', message)
        updateConnectionQuality()
      } else {
        errorCount.value++
        recordError('MESSAGE_SEND_FAILED', `Failed to send message to ${destination}`)

        // Queue message if sending failed and queuing is enabled
        if (queueProcessingEnabled.value) {
          queueMessage(message, destination)
        }
      }
      return success
    } catch (error) {
      errorCount.value++
      console.error('Error sending STOMP message:', error)
      recordError('MESSAGE_SEND_ERROR', error.message || 'Unknown send error')

      // Queue message if error occurred and queuing is enabled
      if (queueProcessingEnabled.value) {
        queueMessage(message, destination)
      }

      return false
    }
  }

  // Connection management
  const connect = () => {
    if (status.value === 'CLOSED') {
      open()
    }
  }

  const disconnect = () => {
    if (status.value === 'OPEN' || status.value === 'CONNECTING') {
      close()
    }
  }

  const reconnect = () => {
    disconnect()
    setTimeout(() => {
      connect()
    }, 1000)
  }

  // Error handling
  const showConnectionError = () => {
    toast.add({
      severity: 'error',
      summary: 'Lỗi kết nối',
      detail: connectionError.value || 'Không thể kết nối đến server real-time',
      life: 5000
    })
  }

  // Clear message history
  const clearMessageHistory = () => {
    messageHistory.value = []
    lastMessage.value = null
  }

  // Get messages by type
  const getMessagesByType = (type) => {
    return messageHistory.value.filter(msg => msg.type === type)
  }

  // Enhanced browser online/offline event handling
  const handleOnlineStatusChange = () => {
    if (navigator.onLine) {
      console.log('🌐 Browser came online')
      networkStatus.value = 'ONLINE'

      // Attempt reconnection if disconnected
      if (!isConnected.value && shouldReconnect.value) {
        setTimeout(() => {
          connect()
        }, 1000) // Small delay to ensure network is stable
      }
    } else {
      console.log('🌐 Browser went offline')
      networkStatus.value = 'OFFLINE'
      connectionStability.value = 0
    }
    updateConnectionQuality()
  }

  // Enhanced lifecycle hooks
  onMounted(() => {
    // Set up browser online/offline event listeners
    window.addEventListener('online', handleOnlineStatusChange)
    window.addEventListener('offline', handleOnlineStatusChange)

    // Initialize network status
    networkStatus.value = navigator.onLine ? 'ONLINE' : 'OFFLINE'

    // Auto-connect on mount with a small delay to ensure component is ready
    setTimeout(() => {
      if (status.value === 'CLOSED' && navigator.onLine) {
        connect()
      }
    }, 100)
  })

  onUnmounted(() => {
    // Remove browser event listeners
    window.removeEventListener('online', handleOnlineStatusChange)
    window.removeEventListener('offline', handleOnlineStatusChange)

    // Clean disconnect on unmount
    disconnect()

    // Stop health monitoring
    stopHealthMonitoring()

    // Clear timers
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    // Clear the client reference
    stompClient = null
  })

  return {
    // Enhanced connection state
    isConnected,
    connectionStatus,
    connectionError,
    reconnectAttempts,
    connectionStartTime,
    lastSuccessfulConnection,

    // Enhanced health monitoring
    connectionHealthy,
    connectionQuality,
    lastHeartbeat,
    connectionLatency,
    messagesSent,
    messagesReceived,
    errorCount,
    isRecovering,
    serverHealthStatus,
    networkStatus,
    connectionStability,
    connectionStable,

    // Message data and queuing
    lastMessage,
    messageHistory,
    messageQueue,
    hasQueuedMessages,
    queueProcessingEnabled,

    // State synchronization
    stateSyncPending,
    lastStateSyncTime,
    syncRetryCount,

    // Connection management
    connect,
    disconnect,
    reconnect,

    // Enhanced message handling
    sendMessage,
    queueMessage,
    processQueuedMessages,
    processIncomingMessage,
    clearMessageHistory,
    getMessagesByType,

    // Enhanced health monitoring functions
    startHealthMonitoring,
    stopHealthMonitoring,
    checkConnectionHealth,
    updateConnectionQuality,
    updateNetworkStatus,
    triggerStateSynchronization,

    // Enhanced connection management
    scheduleIntelligentReconnection,
    handleOnlineStatusChange,

    // Utility
    subscribeToTopics,
    subscribeToHealthTopics,

    // Integration support
    integrationCallbacks,
    setIntegrationCallback: (type, callback) => {
      if (integrationCallbacks.value.hasOwnProperty(type)) {
        integrationCallbacks.value[type] = callback
      }
    }
  }
}
