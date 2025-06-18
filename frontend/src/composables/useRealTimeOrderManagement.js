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

  // WebSocket connection state
  const isConnected = ref(false)
  const connectionError = ref(null)
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 10

  // Health monitoring state
  const connectionQuality = ref('UNKNOWN') // EXCELLENT, GOOD, POOR, CRITICAL
  const lastHeartbeat = ref(null)
  const connectionLatency = ref(0)
  const messagesSent = ref(0)
  const messagesReceived = ref(0)
  const errorCount = ref(0)
  const isRecovering = ref(false)

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

  // Computed properties for health monitoring
  const connectionHealthy = computed(() => {
    return isConnected.value &&
           connectionQuality.value !== 'CRITICAL' &&
           errorCount.value < 5
  })

  const shouldReconnect = computed(() => {
    return !isConnected.value &&
           reconnectAttempts.value < maxReconnectAttempts &&
           !isRecovering.value
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

        // Debug logging (disable in production)
        debug: (str) => {
          if (import.meta.env.DEV) {
            console.log('🔧 STOMP Debug:', str)
          }
        },

        // Enhanced reconnection configuration with exponential backoff
        reconnectDelay: () => {
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value), 30000)
          console.log(`🔄 Reconnect delay: ${delay}ms (attempt ${reconnectAttempts.value + 1})`)
          return delay
        },
        heartbeatIncoming: 10000, // Match server heartbeat configuration
        heartbeatOutgoing: 10000,

        // Enhanced connection callbacks with health monitoring
        onConnect: (frame) => {
          isConnected.value = true
          connectionError.value = null
          reconnectAttempts.value = 0
          status.value = 'OPEN'
          connectionQuality.value = 'EXCELLENT'
          errorCount.value = 0
          isRecovering.value = false
          lastHeartbeat.value = new Date()

          console.log('✅ STOMP WebSocket connected successfully with health monitoring', frame)

          // Subscribe to relevant topics including health monitoring
          subscribeToTopics()
          subscribeToHealthTopics()

          // Start health monitoring
          startHealthMonitoring()

          // Call integration callback for connection change
          if (integrationCallbacks.value.onConnectionChange) {
            integrationCallbacks.value.onConnectionChange(true, connectionQuality.value)
          }

          // Show success notification (optional)
          // toast.add({
          //   severity: 'info',
          //   summary: 'Real-time kết nối',
          //   detail: 'Đã kích hoạt thông báo tự động với giám sát sức khỏe',
          //   life: 2000
          // })
        },

        onDisconnect: (frame) => {
          isConnected.value = false
          status.value = 'CLOSED'
          connectionQuality.value = 'CRITICAL'
          stopHealthMonitoring()
          console.log('❌ STOMP WebSocket disconnected', frame)

          // Attempt reconnection if appropriate
          if (shouldReconnect.value) {
            scheduleReconnection()
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
      console.log('🔄 Attempting public STOMP connection via SockJS...')

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
      // Subscribe to voucher monitoring topics
      const topics = [
        '/topic/phieu-giam-gia/expired',
        '/topic/phieu-giam-gia/new',
        '/topic/phieu-giam-gia/alternatives',
        '/topic/gia-san-pham/updates'
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
        console.log('📡 Subscribed to topic:', topic)
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
      return
    }

    const now = new Date()
    const timeSinceLastHeartbeat = lastHeartbeat.value ?
      now - lastHeartbeat.value : Infinity

    // Determine connection quality based on various factors
    if (errorCount.value >= 10) {
      connectionQuality.value = 'CRITICAL'
    } else if (errorCount.value >= 5 || timeSinceLastHeartbeat > 30000) {
      connectionQuality.value = 'POOR'
    } else if (errorCount.value >= 2 || timeSinceLastHeartbeat > 15000) {
      connectionQuality.value = 'GOOD'
    } else {
      connectionQuality.value = 'EXCELLENT'
    }
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

  const scheduleReconnection = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
    }

    const delay = Math.min(2000 * Math.pow(2, reconnectAttempts.value), 30000)
    console.log(`⏰ Scheduling reconnection in ${delay}ms`)

    reconnectTimer = setTimeout(() => {
      if (shouldReconnect.value) {
        reconnectAttempts.value++
        connect()
      }
    }, delay)
  }

  // Send message with error handling (for application messages)
  const sendMessage = (message, destination = '/app/message') => {
    if (!isConnected.value) {
      console.warn('Cannot send message: STOMP client not connected')
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
      }
      return success
    } catch (error) {
      errorCount.value++
      console.error('Error sending STOMP message:', error)
      recordError('MESSAGE_SEND_ERROR', error.message || 'Unknown send error')
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

  // Lifecycle hooks
  onMounted(() => {
    // Auto-connect on mount with a small delay to ensure component is ready
    setTimeout(() => {
      if (status.value === 'CLOSED') {
        connect()
      }
    }, 100)
  })

  onUnmounted(() => {
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
    // Connection state
    isConnected,
    connectionStatus,
    connectionError,
    reconnectAttempts,

    // Health monitoring
    connectionHealthy,
    connectionQuality,
    lastHeartbeat,
    connectionLatency,
    messagesSent,
    messagesReceived,
    errorCount,
    isRecovering,
    serverHealthStatus,

    // Message data
    lastMessage,
    messageHistory,

    // Connection management
    connect,
    disconnect,
    reconnect,

    // Message handling
    sendMessage,
    processIncomingMessage,
    clearMessageHistory,
    getMessagesByType,

    // Health monitoring functions
    startHealthMonitoring,
    stopHealthMonitoring,
    checkConnectionHealth,
    updateConnectionQuality,

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
