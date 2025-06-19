import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useRealTimeOrderManagement } from './useRealTimeOrderManagement.js'
import { resolveStateConflict, RESOLUTION_STRATEGIES } from '../utils/StateConflictResolver.js'

/**
 * useRealTimeSync - Real-Time State Synchronization Composable
 * Provides cross-component state synchronization with event-driven updates,
 * state validation, and seamless integration with WebSocket infrastructure
 * Follows LapXpert patterns and Vietnamese business terminology
 */

/**
 * Create real-time sync composable
 * @param {Object} options - Configuration options
 * @param {String} options.entityName - Vietnamese entity name (e.g., 'hoaDon', 'sanPham')
 * @param {String} options.storeKey - Unique store identifier
 * @param {Function} options.validateState - State validation function
 * @param {Function} options.mergeStrategy - State merge strategy function
 * @param {Boolean} options.enablePersistence - Enable state persistence
 * @param {Boolean} options.enableCrossTab - Enable cross-tab synchronization
 * @returns {Object} Real-time sync composable
 */
export function useRealTimeSync(options = {}) {
  const {
    entityName = 'dữ liệu',
    storeKey = 'default',
    validateState = null,
    mergeStrategy = null,
    enablePersistence = true,
    enableCrossTab = true,
    enableWebSocketIntegration = true,
    enableOptimisticUpdates = true,
    conflictResolutionStrategy = RESOLUTION_STRATEGIES.LAST_WRITE_WINS,
    autoRefreshDelay = 100
  } = options

  const toast = useToast()

  // WebSocket integration for real-time updates
  const webSocketManager = enableWebSocketIntegration ? useRealTimeOrderManagement() : null
  const webSocketMessageHandler = ref(null)

  // Core state management
  const syncState = ref({
    isConnected: false,
    lastSyncTime: null,
    syncVersion: 0,
    pendingChanges: new Map(),
    conflictQueue: [],
    syncErrors: [],
    webSocketConnected: false,
    optimisticUpdates: new Map(),
    rollbackQueue: []
  })

  // Event system for component reactivity
  const eventListeners = ref(new Map())
  const stateChangeEvents = ref([])
  const maxEventHistory = 100

  // Cross-tab synchronization
  const crossTabChannel = ref(null)
  const tabId = ref(`tab_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`)

  // State persistence
  const persistenceKey = `lapxpert_sync_${storeKey}`
  const persistedState = ref(null)

  // Cache invalidation tracking
  const cacheInvalidationState = ref({
    lastInvalidationTime: null,
    invalidatedScopes: new Set(),
    pendingRefresh: false,
    cacheVersion: '1.0.0'
  })

  // Performance monitoring
  const syncMetrics = ref({
    totalSyncs: 0,
    successfulSyncs: 0,
    failedSyncs: 0,
    averageSyncTime: 0,
    lastSyncDuration: 0,
    cacheInvalidations: 0,
    cacheRefreshes: 0
  })

  /**
   * Initialize WebSocket integration for real-time state updates
   * Vietnamese Business Context: Khởi tạo tích hợp WebSocket cho cập nhật trạng thái thời gian thực
   */
  function initializeWebSocketIntegration() {
    if (!enableWebSocketIntegration || !webSocketManager) {
      console.warn('⚠️ WebSocket integration not enabled or available')
      return
    }

    try {
      // Watch WebSocket connection status
      watch(() => webSocketManager.isConnected.value, (connected) => {
        syncState.value.webSocketConnected = connected
        console.log(`🔌 WebSocket connection status changed: ${connected ? 'Connected' : 'Disconnected'}`)

        if (connected) {
          // Request fresh state sync when WebSocket reconnects
          requestSyncFromOtherTabs()
        }
      })

      // Watch for incoming WebSocket messages
      watch(() => webSocketManager.messageHistory.value, (newHistory, oldHistory) => {
        if (!newHistory || newHistory.length === 0) return

        // Process new messages
        const newMessages = newHistory.slice(0, newHistory.length - (oldHistory?.length || 0))
        newMessages.forEach(handleWebSocketMessage)
      }, { deep: true })

      // Set up message handler
      webSocketMessageHandler.value = handleWebSocketMessage

      console.log(`📡 WebSocket integration initialized for ${entityName} (${storeKey})`)
    } catch (error) {
      console.error('❌ Failed to initialize WebSocket integration:', error)
    }
  }

  /**
   * Initialize cross-tab synchronization
   */
  function initializeCrossTabSync() {
    if (!enableCrossTab || !window.BroadcastChannel) {
      console.warn('⚠️ Cross-tab synchronization not supported')
      return
    }

    try {
      crossTabChannel.value = new BroadcastChannel(`lapxpert_sync_${storeKey}`)

      crossTabChannel.value.addEventListener('message', handleCrossTabMessage)

      console.log(`📡 Cross-tab sync initialized for ${entityName} (${storeKey})`)
    } catch (error) {
      console.error('❌ Failed to initialize cross-tab sync:', error)
    }
  }

  /**
   * Handle WebSocket messages for real-time state updates
   * Vietnamese Business Context: Xử lý tin nhắn WebSocket cho cập nhật trạng thái thời gian thực
   * @param {Object} message - WebSocket message
   */
  function handleWebSocketMessage(message) {
    if (!message || !message.type) return

    try {
      console.log('📨 Processing WebSocket message for state sync:', message)

      switch (message.type) {
        case 'CACHE_INVALIDATION':
          handleCacheInvalidationFromWebSocket(message)
          break
        case 'STATE_UPDATE':
          handleStateUpdateFromWebSocket(message)
          break
        case 'PRICE_UPDATE':
          handlePriceUpdateFromWebSocket(message)
          break
        case 'VOUCHER_UPDATE':
          handleVoucherUpdateFromWebSocket(message)
          break
        case 'ORDER_UPDATE':
          handleOrderUpdateFromWebSocket(message)
          break
        default:
          // Check if message topic matches our entity
          if (message.topic && isRelevantTopic(message.topic)) {
            handleGenericWebSocketUpdate(message)
          }
      }
    } catch (error) {
      console.error('❌ Error handling WebSocket message:', error)
      syncState.value.syncErrors.push({
        type: 'WEBSOCKET_MESSAGE_ERROR',
        error: error.message,
        message,
        timestamp: new Date().toISOString()
      })
    }
  }

  /**
   * Handle cross-tab messages
   * @param {MessageEvent} event - Broadcast channel message event
   */
  function handleCrossTabMessage(event) {
    const { type, data, sourceTabId, timestamp } = event.data

    // Ignore messages from same tab
    if (sourceTabId === tabId.value) return

    try {
      switch (type) {
        case 'STATE_SYNC':
          handleRemoteStateSync(data, timestamp)
          break
        case 'STATE_CHANGE':
          handleRemoteStateChange(data, timestamp)
          break
        case 'CONFLICT_RESOLUTION':
          handleRemoteConflictResolution(data, timestamp)
          break
        case 'SYNC_REQUEST':
          handleSyncRequest(sourceTabId)
          break
        case 'CACHE_INVALIDATION':
          handleCacheInvalidationSignal(data)
          break
        case 'WEBSOCKET_UPDATE':
          handleWebSocketUpdateFromCrossTab(data, timestamp)
          break
        default:
          console.warn(`⚠️ Unknown cross-tab message type: ${type}`)
      }
    } catch (error) {
      console.error('❌ Error handling cross-tab message:', error)
    }
  }

  /**
   * Broadcast message to other tabs
   * @param {String} type - Message type
   * @param {*} data - Message data
   */
  function broadcastToOtherTabs(type, data) {
    if (!crossTabChannel.value) return

    try {
      crossTabChannel.value.postMessage({
        type,
        data,
        sourceTabId: tabId.value,
        timestamp: new Date().toISOString(),
        entityName,
        storeKey
      })
    } catch (error) {
      console.error(`❌ Failed to broadcast ${type}:`, error)
    }
  }

  /**
   * Register event listener for state changes
   * @param {String} eventType - Event type
   * @param {Function} callback - Event callback function
   * @returns {Function} Unsubscribe function
   */
  function addEventListener(eventType, callback) {
    if (!eventListeners.value.has(eventType)) {
      eventListeners.value.set(eventType, new Set())
    }

    eventListeners.value.get(eventType).add(callback)

    // Return unsubscribe function
    return () => {
      const listeners = eventListeners.value.get(eventType)
      if (listeners) {
        listeners.delete(callback)
        if (listeners.size === 0) {
          eventListeners.value.delete(eventType)
        }
      }
    }
  }

  /**
   * Emit state change event
   * @param {String} eventType - Event type
   * @param {*} data - Event data
   */
  function emitStateChangeEvent(eventType, data) {
    const event = {
      type: eventType,
      data,
      timestamp: new Date().toISOString(),
      tabId: tabId.value,
      entityName,
      storeKey
    }

    // Add to event history
    stateChangeEvents.value.unshift(event)
    if (stateChangeEvents.value.length > maxEventHistory) {
      stateChangeEvents.value = stateChangeEvents.value.slice(0, maxEventHistory)
    }

    // Notify local listeners
    const listeners = eventListeners.value.get(eventType)
    if (listeners) {
      listeners.forEach(callback => {
        try {
          callback(event)
        } catch (error) {
          console.error(`❌ Error in event listener for ${eventType}:`, error)
        }
      })
    }

    // Broadcast to other tabs
    broadcastToOtherTabs('STATE_CHANGE', event)
  }

  /**
   * Validate state using provided validation function
   * @param {*} state - State to validate
   * @returns {Object} Validation result
   */
  function validateStateData(state) {
    if (!validateState) {
      return { isValid: true, errors: [] }
    }

    try {
      const result = validateState(state)
      return typeof result === 'boolean'
        ? { isValid: result, errors: [] }
        : result
    } catch (error) {
      return {
        isValid: false,
        errors: [`Validation error: ${error.message}`]
      }
    }
  }

  /**
   * Merge states using provided merge strategy
   * @param {*} currentState - Current state
   * @param {*} incomingState - Incoming state
   * @returns {*} Merged state
   */
  function mergeStates(currentState, incomingState) {
    if (!mergeStrategy) {
      // Default merge strategy: last-write-wins
      return incomingState
    }

    try {
      return mergeStrategy(currentState, incomingState)
    } catch (error) {
      console.error('❌ Error in merge strategy:', error)
      return incomingState // Fallback to last-write-wins
    }
  }

  /**
   * Handle remote state synchronization
   * @param {*} remoteState - Remote state data
   * @param {String} timestamp - Sync timestamp
   */
  function handleRemoteStateSync(remoteState, timestamp) {
    const validation = validateStateData(remoteState)

    if (!validation.isValid) {
      console.warn('⚠️ Invalid remote state received:', validation.errors)
      return
    }

    emitStateChangeEvent('REMOTE_SYNC_RECEIVED', {
      remoteState,
      timestamp,
      validation
    })
  }

  /**
   * Handle remote state change
   * @param {*} changeData - State change data
   * @param {String} timestamp - Change timestamp
   */
  function handleRemoteStateChange(changeData, timestamp) {
    emitStateChangeEvent('REMOTE_CHANGE_RECEIVED', {
      changeData,
      timestamp
    })
  }

  /**
   * Handle remote conflict resolution
   * @param {*} resolutionData - Conflict resolution data
   * @param {String} timestamp - Resolution timestamp
   */
  function handleRemoteConflictResolution(resolutionData, timestamp) {
    emitStateChangeEvent('CONFLICT_RESOLVED', {
      resolutionData,
      timestamp
    })
  }

  /**
   * Handle sync request from other tab
   * @param {String} _requestingTabId - ID of requesting tab
   */
  function handleSyncRequest(_requestingTabId) {
    // Respond with current state if we have it
    if (persistedState.value) {
      broadcastToOtherTabs('STATE_SYNC', persistedState.value)
    }
  }

  /**
   * Handle cache invalidation from WebSocket messages
   * Vietnamese Business Context: Xử lý vô hiệu hóa cache từ tin nhắn WebSocket
   * @param {Object} message - WebSocket message with cache invalidation data
   */
  function handleCacheInvalidationFromWebSocket(message) {
    const invalidationData = message.payload || message.data || message
    handleCacheInvalidationSignal(invalidationData)

    // Broadcast to other tabs for coordination
    broadcastToOtherTabs('WEBSOCKET_UPDATE', {
      type: 'CACHE_INVALIDATION',
      data: invalidationData,
      source: 'websocket'
    })
  }

  /**
   * Handle state update from WebSocket messages
   * Vietnamese Business Context: Xử lý cập nhật trạng thái từ tin nhắn WebSocket
   * @param {Object} message - WebSocket message with state update
   */
  function handleStateUpdateFromWebSocket(message) {
    const stateData = message.payload || message.data
    if (!stateData) return

    // Apply optimistic update if enabled
    if (enableOptimisticUpdates) {
      applyOptimisticUpdate(stateData, message)
    }

    // Emit state change event
    emitStateChangeEvent('WEBSOCKET_STATE_UPDATE', {
      stateData,
      message,
      timestamp: new Date().toISOString()
    })

    // Broadcast to other tabs
    broadcastToOtherTabs('WEBSOCKET_UPDATE', {
      type: 'STATE_UPDATE',
      data: stateData,
      source: 'websocket'
    })
  }

  /**
   * Handle price update from WebSocket messages
   * Vietnamese Business Context: Xử lý cập nhật giá từ tin nhắn WebSocket
   * @param {Object} message - WebSocket message with price update
   */
  function handlePriceUpdateFromWebSocket(message) {
    if (!isRelevantTopic(message.topic, 'gia-san-pham')) return

    const priceData = message.payload || message.data

    // Emit price update event
    emitStateChangeEvent('WEBSOCKET_PRICE_UPDATE', {
      priceData,
      topic: message.topic,
      timestamp: new Date().toISOString()
    })

    // Trigger cache invalidation for pricing data
    handleCacheInvalidationSignal({
      scope: 'PRICING_DATA',
      entityId: priceData?.variantId || priceData?.sanPhamChiTietId,
      requiresRefresh: true,
      timestamp: new Date().toISOString()
    })
  }

  /**
   * Handle voucher update from WebSocket messages
   * Vietnamese Business Context: Xử lý cập nhật voucher từ tin nhắn WebSocket
   * @param {Object} message - WebSocket message with voucher update
   */
  function handleVoucherUpdateFromWebSocket(message) {
    if (!isRelevantTopic(message.topic, 'phieu-giam-gia') &&
        !isRelevantTopic(message.topic, 'dot-giam-gia')) return

    const voucherData = message.payload || message.data

    // Emit voucher update event
    emitStateChangeEvent('WEBSOCKET_VOUCHER_UPDATE', {
      voucherData,
      topic: message.topic,
      timestamp: new Date().toISOString()
    })

    // Trigger cache invalidation for voucher data
    handleCacheInvalidationSignal({
      scope: 'VOUCHER_DATA',
      entityId: voucherData?.id || voucherData?.voucherId,
      requiresRefresh: true,
      timestamp: new Date().toISOString()
    })
  }

  /**
   * Handle order update from WebSocket messages
   * Vietnamese Business Context: Xử lý cập nhật đơn hàng từ tin nhắn WebSocket
   * @param {Object} message - WebSocket message with order update
   */
  function handleOrderUpdateFromWebSocket(message) {
    if (!isRelevantTopic(message.topic, 'hoa-don')) return

    const orderData = message.payload || message.data

    // Emit order update event
    emitStateChangeEvent('WEBSOCKET_ORDER_UPDATE', {
      orderData,
      topic: message.topic,
      timestamp: new Date().toISOString()
    })

    // Trigger cache invalidation for order data
    handleCacheInvalidationSignal({
      scope: 'ORDER_DATA',
      entityId: orderData?.id || orderData?.hoaDonId,
      requiresRefresh: true,
      timestamp: new Date().toISOString()
    })
  }

  /**
   * Handle generic WebSocket update for relevant topics
   * Vietnamese Business Context: Xử lý cập nhật WebSocket chung cho các chủ đề liên quan
   * @param {Object} message - WebSocket message
   */
  function handleGenericWebSocketUpdate(message) {
    // Emit generic update event
    emitStateChangeEvent('WEBSOCKET_GENERIC_UPDATE', {
      message,
      timestamp: new Date().toISOString()
    })

    // Trigger cache invalidation based on topic
    const scope = getDataScopeFromTopic(message.topic)
    if (scope) {
      handleCacheInvalidationSignal({
        scope,
        requiresRefresh: true,
        timestamp: new Date().toISOString()
      })
    }
  }

  /**
   * Handle WebSocket update from cross-tab synchronization
   * Vietnamese Business Context: Xử lý cập nhật WebSocket từ đồng bộ đa tab
   * @param {Object} data - Update data from other tab
   * @param {String} timestamp - Update timestamp
   */
  function handleWebSocketUpdateFromCrossTab(data, timestamp) {
    // Process the update without re-broadcasting to avoid loops
    emitStateChangeEvent('CROSS_TAB_WEBSOCKET_UPDATE', {
      data,
      timestamp,
      source: 'cross_tab'
    })
  }

  /**
   * Handle cache invalidation signal from WebSocket
   * Vietnamese Business Context: Xử lý tín hiệu vô hiệu hóa cache từ WebSocket
   * @param {Object} invalidationData - Cache invalidation data
   */
  function handleCacheInvalidationSignal(invalidationData) {
    try {
      console.log('🗑️ Received cache invalidation signal:', invalidationData)

      // Update cache invalidation state
      cacheInvalidationState.value.lastInvalidationTime = new Date().toISOString()
      cacheInvalidationState.value.pendingRefresh = true

      // Track invalidated scopes
      if (invalidationData.scope) {
        cacheInvalidationState.value.invalidatedScopes.add(invalidationData.scope)
      }

      // Update cache version if provided
      if (invalidationData.version) {
        cacheInvalidationState.value.cacheVersion = invalidationData.version
      }

      // Update metrics
      syncMetrics.value.cacheInvalidations++

      // Emit cache invalidation event for components to handle
      emitStateChangeEvent('CACHE_INVALIDATED', {
        scope: invalidationData.scope,
        invalidatedCaches: invalidationData.invalidatedCaches,
        timestamp: invalidationData.timestamp,
        requiresRefresh: invalidationData.requiresRefresh,
        eventType: invalidationData.eventType,
        entityId: invalidationData.entityId
      })

      // Auto-refresh if enabled and scope matches entity
      if (invalidationData.requiresRefresh && shouldAutoRefresh(invalidationData.scope)) {
        setTimeout(() => {
          performCacheRefresh(invalidationData)
        }, autoRefreshDelay) // Use configurable delay
      }

      console.log(`✅ Cache invalidation processed for scope: ${invalidationData.scope}`)

    } catch (error) {
      console.error('❌ Error handling cache invalidation signal:', error)
    }
  }

  /**
   * Check if a topic is relevant to the current entity
   * Vietnamese Business Context: Kiểm tra xem chủ đề có liên quan đến thực thể hiện tại không
   * @param {String} topic - WebSocket topic
   * @param {String} specificType - Specific type to check for
   * @returns {Boolean} Whether topic is relevant
   */
  function isRelevantTopic(topic, specificType = null) {
    if (!topic) return false

    if (specificType) {
      return topic.includes(specificType)
    }

    // Check if topic is relevant to current entity
    const relevantTopics = {
      'hoaDon': ['hoa-don', 'order'],
      'sanPham': ['gia-san-pham', 'san-pham', 'product'],
      'phieuGiamGia': ['phieu-giam-gia', 'voucher'],
      'dotGiamGia': ['dot-giam-gia', 'campaign'],
      'tonKho': ['ton-kho', 'inventory'],
      'gia': ['gia-san-pham', 'price']
    }

    const entityTopics = relevantTopics[entityName] || []
    return entityTopics.some(entityTopic => topic.includes(entityTopic))
  }

  /**
   * Get data scope from WebSocket topic
   * Vietnamese Business Context: Lấy phạm vi dữ liệu từ chủ đề WebSocket
   * @param {String} topic - WebSocket topic
   * @returns {String|null} Data scope
   */
  function getDataScopeFromTopic(topic) {
    if (!topic) return null

    const topicScopeMap = {
      'gia-san-pham': 'PRICING_DATA',
      'san-pham': 'PRODUCT_DATA',
      'phieu-giam-gia': 'VOUCHER_DATA',
      'dot-giam-gia': 'VOUCHER_DATA',
      'hoa-don': 'ORDER_DATA',
      'ton-kho': 'INVENTORY_DATA'
    }

    for (const [topicKey, scope] of Object.entries(topicScopeMap)) {
      if (topic.includes(topicKey)) {
        return scope
      }
    }

    return 'GENERAL_DATA'
  }

  /**
   * Apply optimistic update to state
   * Vietnamese Business Context: Áp dụng cập nhật lạc quan cho trạng thái
   * @param {Object} stateData - State data to apply
   * @param {Object} message - Original WebSocket message
   */
  function applyOptimisticUpdate(stateData, message) {
    if (!enableOptimisticUpdates) return

    const updateId = `${Date.now()}_${Math.random().toString(36).substring(2, 9)}`

    // Store optimistic update
    syncState.value.optimisticUpdates.set(updateId, {
      stateData,
      message,
      timestamp: new Date().toISOString(),
      applied: true
    })

    // Emit optimistic update event
    emitStateChangeEvent('OPTIMISTIC_UPDATE_APPLIED', {
      updateId,
      stateData,
      message
    })

    // Schedule rollback check
    setTimeout(() => {
      checkOptimisticUpdateConfirmation(updateId)
    }, 5000) // 5 second timeout for confirmation
  }

  /**
   * Check optimistic update confirmation
   * Vietnamese Business Context: Kiểm tra xác nhận cập nhật lạc quan
   * @param {String} updateId - Update ID to check
   */
  function checkOptimisticUpdateConfirmation(updateId) {
    const update = syncState.value.optimisticUpdates.get(updateId)
    if (!update) return

    // If update is still pending, consider it failed and rollback
    if (update.applied && !update.confirmed) {
      rollbackOptimisticUpdate(updateId)
    }

    // Clean up old updates
    syncState.value.optimisticUpdates.delete(updateId)
  }

  /**
   * Rollback optimistic update
   * Vietnamese Business Context: Hoàn tác cập nhật lạc quan
   * @param {String} updateId - Update ID to rollback
   */
  function rollbackOptimisticUpdate(updateId) {
    const update = syncState.value.optimisticUpdates.get(updateId)
    if (!update) return

    // Add to rollback queue
    syncState.value.rollbackQueue.push({
      updateId,
      update,
      rollbackTime: new Date().toISOString()
    })

    // Emit rollback event
    emitStateChangeEvent('OPTIMISTIC_UPDATE_ROLLBACK', {
      updateId,
      update
    })

    console.warn('🔄 Rolling back optimistic update:', updateId)
  }

  /**
   * Resolve state conflicts using configured strategy
   * Vietnamese Business Context: Giải quyết xung đột trạng thái bằng chiến lược đã cấu hình
   * @param {Object} currentState - Current state
   * @param {Object} incomingState - Incoming state
   * @returns {Object} Resolved state
   */
  function resolveStateConflicts(currentState, incomingState) {
    try {
      const resolution = resolveStateConflict(
        currentState,
        incomingState,
        conflictResolutionStrategy,
        { entityName, storeKey }
      )

      // Track conflict resolution
      syncState.value.conflictQueue.push({
        currentState,
        incomingState,
        resolvedState: resolution.resolvedState,
        strategy: conflictResolutionStrategy,
        timestamp: new Date().toISOString()
      })

      // Emit conflict resolution event
      emitStateChangeEvent('STATE_CONFLICT_RESOLVED', {
        resolution,
        strategy: conflictResolutionStrategy
      })

      return resolution.resolvedState
    } catch (error) {
      console.error('❌ Error resolving state conflict:', error)
      // Fallback to last-write-wins
      return incomingState
    }
  }

  /**
   * Check if auto-refresh should be performed for the given scope
   * @param {String} scope - Cache scope
   * @returns {Boolean} Whether to auto-refresh
   */
  function shouldAutoRefresh(scope) {
    // Auto-refresh for scopes that match the current entity
    const entityScopes = {
      'hoaDon': 'ORDER_DATA',
      'sanPham': 'PRODUCT_DATA',
      'phieuGiamGia': 'VOUCHER_DATA',
      'dotGiamGia': 'VOUCHER_DATA',
      'tonKho': 'INVENTORY_DATA',
      'gia': 'PRICING_DATA'
    }

    return entityScopes[entityName] === scope || scope === 'GENERAL_DATA'
  }

  /**
   * Perform cache refresh after invalidation
   * Vietnamese Business Context: Thực hiện làm mới cache sau khi vô hiệu hóa
   * @param {Object} invalidationData - Cache invalidation data
   */
  function performCacheRefresh(invalidationData) {
    try {
      console.log('🔄 Performing cache refresh for scope:', invalidationData.scope)

      // Update metrics
      syncMetrics.value.cacheRefreshes++

      // Clear pending refresh flag
      cacheInvalidationState.value.pendingRefresh = false

      // Emit cache refresh event
      emitStateChangeEvent('CACHE_REFRESHED', {
        scope: invalidationData.scope,
        timestamp: new Date().toISOString(),
        refreshReason: 'CACHE_INVALIDATION'
      })

      // Request fresh data sync if state exists
      if (persistedState.value) {
        requestSyncFromOtherTabs()
      }

      console.log(`✅ Cache refresh completed for scope: ${invalidationData.scope}`)

    } catch (error) {
      console.error('❌ Error performing cache refresh:', error)
    }
  }

  /**
   * Persist state to localStorage
   * @param {*} state - State to persist
   */
  function persistState(state) {
    if (!enablePersistence) return

    try {
      const persistData = {
        state,
        timestamp: new Date().toISOString(),
        version: syncState.value.syncVersion,
        tabId: tabId.value
      }

      localStorage.setItem(persistenceKey, JSON.stringify(persistData))
      persistedState.value = state
    } catch (error) {
      console.error('❌ Failed to persist state:', error)
    }
  }

  /**
   * Load persisted state from localStorage
   * @returns {*} Persisted state or null
   */
  function loadPersistedState() {
    if (!enablePersistence) return null

    try {
      const stored = localStorage.getItem(persistenceKey)
      if (!stored) return null

      const persistData = JSON.parse(stored)

      // Validate persisted data
      const validation = validateStateData(persistData.state)
      if (!validation.isValid) {
        console.warn('⚠️ Invalid persisted state, clearing:', validation.errors)
        localStorage.removeItem(persistenceKey)
        return null
      }

      persistedState.value = persistData.state
      return persistData
    } catch (error) {
      console.error('❌ Failed to load persisted state:', error)
      localStorage.removeItem(persistenceKey)
      return null
    }
  }

  /**
   * Request sync from other tabs
   */
  function requestSyncFromOtherTabs() {
    broadcastToOtherTabs('SYNC_REQUEST', { requestingTab: tabId.value })
  }

  /**
   * Synchronize state with remote source
   * @param {*} newState - New state to sync
   * @param {Object} options - Sync options
   * @returns {Promise} Promise resolving to sync result
   */
  async function syncStateData(newState, options = {}) {
    const startTime = Date.now()
    syncMetrics.value.totalSyncs++

    try {
      // Validate new state
      const validation = validateStateData(newState)
      if (!validation.isValid) {
        throw new Error(`State validation failed: ${validation.errors.join(', ')}`)
      }

      // Merge with current state if needed
      const finalState = options.merge && persistedState.value
        ? mergeStates(persistedState.value, newState)
        : newState

      // Persist state
      persistState(finalState)

      // Update sync state
      syncState.value.lastSyncTime = new Date().toISOString()
      syncState.value.syncVersion++
      syncState.value.isConnected = true

      // Emit sync event
      emitStateChangeEvent('STATE_SYNCED', {
        state: finalState,
        options,
        syncVersion: syncState.value.syncVersion
      })

      // Update metrics
      const duration = Date.now() - startTime
      syncMetrics.value.successfulSyncs++
      syncMetrics.value.lastSyncDuration = duration
      syncMetrics.value.averageSyncTime =
        (syncMetrics.value.averageSyncTime * (syncMetrics.value.successfulSyncs - 1) + duration) /
        syncMetrics.value.successfulSyncs

      return { success: true, state: finalState, duration }
    } catch (error) {
      syncMetrics.value.failedSyncs++
      syncState.value.syncErrors.push({
        error: error.message,
        timestamp: new Date().toISOString(),
        state: newState
      })

      console.error(`❌ Sync failed for ${entityName}:`, error)

      toast.add({
        severity: 'error',
        summary: 'Lỗi đồng bộ',
        detail: `Không thể đồng bộ ${entityName}: ${error.message}`,
        life: 5000
      })

      return { success: false, error: error.message }
    }
  }

  // Computed properties
  const isHealthy = computed(() => {
    return syncState.value.isConnected &&
           syncState.value.syncErrors.length < 5 &&
           syncMetrics.value.failedSyncs / Math.max(syncMetrics.value.totalSyncs, 1) < 0.1
  })

  const syncSuccessRate = computed(() => {
    const total = syncMetrics.value.totalSyncs
    return total > 0 ? (syncMetrics.value.successfulSyncs / total * 100).toFixed(2) : 0
  })

  /**
   * Initialize all synchronization systems
   * Vietnamese Business Context: Khởi tạo tất cả hệ thống đồng bộ hóa
   */
  function initializeSync() {
    initializeCrossTabSync()
    initializeWebSocketIntegration()
    loadPersistedState()

    console.log(`🚀 Real-time sync initialized for ${entityName} (${storeKey})`)
  }

  // Initialize on creation
  onMounted(() => {
    initializeSync()
  })

  // Cleanup on unmount
  onUnmounted(() => {
    cleanup()
  })

  function cleanup() {
    // Close cross-tab channel
    if (crossTabChannel.value) {
      crossTabChannel.value.removeEventListener('message', handleCrossTabMessage)
      crossTabChannel.value.close()
      crossTabChannel.value = null
    }

    // Clear WebSocket integration
    if (webSocketMessageHandler.value) {
      webSocketMessageHandler.value = null
    }

    // Clear event listeners
    eventListeners.value.clear()

    // Clear state change events
    stateChangeEvents.value = []

    // Clear optimistic updates
    syncState.value.optimisticUpdates.clear()
    syncState.value.rollbackQueue = []

    console.log(`🧹 Cleanup completed for ${entityName} (${storeKey})`)
  }

  // Additional computed properties
  const isWebSocketConnected = computed(() => {
    return syncState.value.webSocketConnected
  })

  const hasOptimisticUpdates = computed(() => {
    return syncState.value.optimisticUpdates.size > 0
  })

  const hasPendingConflicts = computed(() => {
    return syncState.value.conflictQueue.length > 0
  })

  return {
    // State
    syncState: computed(() => syncState.value),
    persistedState: computed(() => persistedState.value),
    stateChangeEvents: computed(() => stateChangeEvents.value),
    syncMetrics: computed(() => syncMetrics.value),
    tabId: computed(() => tabId.value),
    cacheInvalidationState: computed(() => cacheInvalidationState.value),

    // Computed
    isHealthy,
    syncSuccessRate,
    isWebSocketConnected,
    hasOptimisticUpdates,
    hasPendingConflicts,

    // Core Methods
    syncStateData,
    addEventListener,
    emitStateChangeEvent,
    validateStateData,
    mergeStates,
    persistState,
    loadPersistedState,
    requestSyncFromOtherTabs,
    broadcastToOtherTabs,
    handleCacheInvalidationSignal,
    performCacheRefresh,
    cleanup,

    // Enhanced WebSocket Methods
    handleWebSocketMessage,
    isRelevantTopic,
    getDataScopeFromTopic,
    applyOptimisticUpdate,
    rollbackOptimisticUpdate,
    resolveStateConflicts,

    // Initialization
    initializeSync
  }
}
