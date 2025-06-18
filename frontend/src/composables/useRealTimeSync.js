import { ref, computed } from 'vue'
import { useToast } from 'primevue/usetoast'

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
    enableCrossTab = true
  } = options

  const toast = useToast()

  // Core state management
  const syncState = ref({
    isConnected: false,
    lastSyncTime: null,
    syncVersion: 0,
    pendingChanges: new Map(),
    conflictQueue: [],
    syncErrors: []
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

  // Performance monitoring
  const syncMetrics = ref({
    totalSyncs: 0,
    successfulSyncs: 0,
    failedSyncs: 0,
    averageSyncTime: 0,
    lastSyncDuration: 0
  })

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

  // Initialize
  initializeCrossTabSync()
  loadPersistedState()

  // Cleanup on unmount
  function cleanup() {
    if (crossTabChannel.value) {
      crossTabChannel.value.removeEventListener('message', handleCrossTabMessage)
      crossTabChannel.value.close()
    }
    eventListeners.value.clear()
  }

  return {
    // State
    syncState: computed(() => syncState.value),
    persistedState: computed(() => persistedState.value),
    stateChangeEvents: computed(() => stateChangeEvents.value),
    syncMetrics: computed(() => syncMetrics.value),
    tabId: computed(() => tabId.value),

    // Computed
    isHealthy,
    syncSuccessRate,

    // Methods
    syncStateData,
    addEventListener,
    emitStateChangeEvent,
    validateStateData,
    mergeStates,
    persistState,
    loadPersistedState,
    requestSyncFromOtherTabs,
    broadcastToOtherTabs,
    cleanup
  }
}
