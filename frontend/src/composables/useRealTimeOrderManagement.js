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
  const maxReconnectAttempts = 3

  // Real-time data
  const lastMessage = ref(null)
  const messageHistory = ref([])

  // WebSocket URL - using environment variable or default
  const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'

  // STOMP client instance
  let stompClient = null

  // No authentication token needed for public WebSocket
  // WebSocket is used only for push notifications

  // Connection status
  const status = ref('CLOSED')

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

        // Reconnection configuration
        reconnectDelay: 2000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,

        // Connection callbacks
        onConnect: (frame) => {
          isConnected.value = true
          connectionError.value = null
          reconnectAttempts.value = 0
          status.value = 'OPEN'
          console.log('✅ STOMP WebSocket connected successfully via SockJS (public mode)', frame)

          // Subscribe to relevant topics
          subscribeToTopics()

          // Show success notification (optional)
          // toast.add({
          //   severity: 'info',
          //   summary: 'Real-time kết nối',
          //   detail: 'Đã kích hoạt thông báo tự động',
          //   life: 2000
          // })
        },

        onDisconnect: (frame) => {
          isConnected.value = false
          status.value = 'CLOSED'
          console.log('❌ STOMP WebSocket disconnected', frame)
        },

        onStompError: (frame) => {
          connectionError.value = 'WebSocket không khả dụng'
          status.value = 'CLOSED'
          console.error('STOMP error:', frame.headers['message'], frame.body)

          // No authentication errors expected in public mode
          console.warn('WebSocket connection failed - server may be unavailable')
          connectionError.value = 'Kết nối WebSocket thất bại. Máy chủ có thể không khả dụng.'
        },

        onWebSocketError: (error) => {
          connectionError.value = 'WebSocket không khả dụng'
          status.value = 'CLOSED'
          console.warn('WebSocket error (non-critical):', error?.message || 'Connection failed')
        },

        onWebSocketClose: (event) => {
          isConnected.value = false
          status.value = 'CLOSED'
          console.log('WebSocket connection closed:', event.code, event.reason)
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
            const parsedMessage = JSON.parse(message.body)
            processIncomingMessage({
              ...parsedMessage,
              topic: topic,
              timestamp: new Date()
            })
          } catch (error) {
            console.error('Error parsing STOMP message from topic', topic, ':', error)
          }
        })
        console.log('📡 Subscribed to topic:', topic)
      })
    } catch (error) {
      console.error('Error subscribing to topics:', error)
    }
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
        console.log('📤 Sent STOMP message to', destination, ':', message)
      }
      return success
    } catch (error) {
      console.error('Error sending STOMP message:', error)
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
    // Clear the client reference
    stompClient = null
  })

  return {
    // Connection state
    isConnected,
    connectionStatus,
    connectionError,
    reconnectAttempts,

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

    // Utility
    subscribeToTopics
  }
}
