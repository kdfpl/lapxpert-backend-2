import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useRealTimeSync } from '@/composables/useRealTimeSync'
import { useRealTimeOrderManagement } from '@/composables/useRealTimeOrderManagement'

/**
 * Unified DataTable Real-Time Integration Composable
 * 
 * Provides comprehensive real-time data synchronization for ALL DataTable components
 * across the LapXpert frontend. Ensures automatic display of newest data from 
 * database/cache with immediate refresh when database changes occur via WebSocket notifications.
 * 
 * Features:
 * - Automatic refresh when relevant database changes occur
 * - Integration with enhanced WebSocket infrastructure
 * - Intelligent refresh strategies with debouncing
 * - Selective row updates for performance optimization
 * - Cross-tab synchronization support
 * - Vietnamese business terminology preservation
 * - 100% backward compatibility with existing DataTable functionality
 * 
 * Vietnamese Business Context: Tích hợp thời gian thực cho bảng dữ liệu
 */
export function useDataTableRealTime(options = {}) {
  const {
    entityType = 'dữ liệu', // Vietnamese entity name
    storeKey = 'default',
    refreshCallback = null, // Function to call when data needs refresh
    debounceDelay = 300, // Debounce delay for refresh operations
    enableSelectiveUpdates = true, // Enable selective row updates
    enableCrossTab = true, // Enable cross-tab synchronization
    topicFilters = [], // WebSocket topic filters for this DataTable
    enablePerformanceOptimization = true
  } = options

  const toast = useToast()

  // WebSocket integration
  const webSocketManager = useRealTimeOrderManagement()
  
  // Real-time sync integration
  const realTimeSync = useRealTimeSync({
    entityName: entityType,
    storeKey: `datatable_${storeKey}`,
    enableWebSocketIntegration: true,
    enableCrossTab,
    enableOptimisticUpdates: false, // DataTables don't need optimistic updates
    autoRefreshDelay: debounceDelay
  })

  // DataTable real-time state
  const realTimeState = ref({
    isActive: false,
    lastRefreshTime: null,
    pendingRefresh: false,
    refreshCount: 0,
    errorCount: 0,
    lastUpdateSource: null,
    affectedRows: new Set(),
    debounceTimer: null
  })

  // Performance metrics
  const performanceMetrics = ref({
    totalRefreshes: 0,
    averageRefreshTime: 0,
    lastRefreshDuration: 0,
    skippedRefreshes: 0,
    selectiveUpdates: 0
  })

  // Topic subscription management
  const subscribedTopics = ref(new Set())
  const topicHandlers = ref(new Map())

  /**
   * Initialize DataTable real-time integration
   * Vietnamese Business Context: Khởi tạo tích hợp thời gian thực cho bảng dữ liệu
   */
  function initializeRealTimeIntegration() {
    console.log(`🚀 Initializing DataTable real-time integration for ${entityType}`)
    
    realTimeState.value.isActive = true
    
    // Setup WebSocket message handling
    setupWebSocketHandlers()
    
    // Setup real-time sync event listeners
    setupRealTimeSyncHandlers()
    
    // Setup cross-tab synchronization
    if (enableCrossTab) {
      setupCrossTabHandlers()
    }
    
    console.log(`✅ DataTable real-time integration initialized for ${entityType}`)
  }

  /**
   * Setup WebSocket message handlers for DataTable updates
   * Vietnamese Business Context: Thiết lập xử lý tin nhắn WebSocket cho cập nhật bảng dữ liệu
   */
  function setupWebSocketHandlers() {
    // Watch for WebSocket connection status
    watch(() => webSocketManager.isConnected.value, (connected) => {
      if (connected) {
        console.log(`🔌 WebSocket connected - DataTable ${entityType} ready for real-time updates`)
        subscribeToRelevantTopics()
      } else {
        console.log(`🔌 WebSocket disconnected - DataTable ${entityType} real-time updates paused`)
      }
    })

    // Watch for incoming WebSocket messages
    watch(() => webSocketManager.messageHistory.value, (newHistory, oldHistory) => {
      if (!newHistory || newHistory.length === 0) return
      
      // Process new messages
      const newMessages = newHistory.slice(0, newHistory.length - (oldHistory?.length || 0))
      newMessages.forEach(handleWebSocketMessage)
    }, { deep: true })
  }

  /**
   * Setup real-time sync event handlers
   * Vietnamese Business Context: Thiết lập xử lý sự kiện đồng bộ thời gian thực
   */
  function setupRealTimeSyncHandlers() {
    // Listen for cache invalidation signals
    realTimeSync.addEventListener('CACHE_INVALIDATION', (event) => {
      const { scope, entityId, requiresRefresh } = event.data
      
      if (requiresRefresh && isRelevantToDataTable(scope, entityId)) {
        triggerIntelligentRefresh({
          source: 'CACHE_INVALIDATION',
          scope,
          entityId,
          selective: enableSelectiveUpdates && entityId
        })
      }
    })

    // Listen for state updates
    realTimeSync.addEventListener('WEBSOCKET_STATE_UPDATE', (event) => {
      const { stateData } = event.data
      
      if (isRelevantToDataTable('STATE_UPDATE', stateData?.id)) {
        triggerIntelligentRefresh({
          source: 'STATE_UPDATE',
          data: stateData,
          selective: enableSelectiveUpdates && stateData?.id
        })
      }
    })
  }

  /**
   * Setup cross-tab synchronization handlers
   * Vietnamese Business Context: Thiết lập xử lý đồng bộ giữa các tab
   */
  function setupCrossTabHandlers() {
    realTimeSync.addEventListener('CROSS_TAB_UPDATE', (event) => {
      const { type, data } = event.data
      
      if (type === 'DATATABLE_REFRESH' && data.entityType === entityType) {
        triggerIntelligentRefresh({
          source: 'CROSS_TAB',
          ...data,
          selective: false // Cross-tab updates usually require full refresh
        })
      }
    })
  }

  /**
   * Handle incoming WebSocket messages for DataTable updates
   * Vietnamese Business Context: Xử lý tin nhắn WebSocket đến cho cập nhật bảng dữ liệu
   */
  function handleWebSocketMessage(message) {
    if (!message || !message.topic) return

    const { topic, payload, timestamp } = message
    
    // Check if this message is relevant to our DataTable
    if (!isRelevantTopic(topic)) return

    console.log(`📨 DataTable ${entityType} received relevant WebSocket message:`, {
      topic,
      timestamp,
      payloadType: typeof payload
    })

    // Determine update type and trigger appropriate refresh
    const updateInfo = analyzeWebSocketMessage(message)
    if (updateInfo.shouldRefresh) {
      triggerIntelligentRefresh({
        source: 'WEBSOCKET',
        topic,
        ...updateInfo
      })
    }
  }

  /**
   * Analyze WebSocket message to determine update requirements
   * Vietnamese Business Context: Phân tích tin nhắn WebSocket để xác định yêu cầu cập nhật
   */
  function analyzeWebSocketMessage(message) {
    const { topic, payload } = message
    
    // Default analysis
    let shouldRefresh = true
    let selective = false
    let entityId = null

    // Topic-specific analysis
    if (topic.includes('gia-san-pham')) {
      // Product price updates
      entityId = payload?.variantId || payload?.sanPhamChiTietId
      selective = enableSelectiveUpdates && entityId
    } else if (topic.includes('phieu-giam-gia')) {
      // Voucher updates
      entityId = payload?.id || payload?.phieuGiamGiaId
      selective = enableSelectiveUpdates && entityId
    } else if (topic.includes('hoa-don')) {
      // Order updates
      entityId = payload?.id || payload?.hoaDonId
      selective = enableSelectiveUpdates && entityId
    } else if (topic.includes('nguoi-dung')) {
      // User updates
      entityId = payload?.id || payload?.nguoiDungId
      selective = enableSelectiveUpdates && entityId
    } else if (topic.includes('ton-kho')) {
      // Inventory updates
      entityId = payload?.sanPhamChiTietId
      selective = enableSelectiveUpdates && entityId
    }

    return {
      shouldRefresh,
      selective,
      entityId,
      updateType: extractUpdateType(topic)
    }
  }

  /**
   * Extract update type from WebSocket topic
   * Vietnamese Business Context: Trích xuất loại cập nhật từ chủ đề WebSocket
   */
  function extractUpdateType(topic) {
    if (topic.includes('/new')) return 'CREATE'
    if (topic.includes('/updated')) return 'UPDATE'
    if (topic.includes('/deleted')) return 'DELETE'
    if (topic.includes('/expired')) return 'EXPIRE'
    return 'GENERAL'
  }

  /**
   * Check if WebSocket topic is relevant to this DataTable
   * Vietnamese Business Context: Kiểm tra chủ đề WebSocket có liên quan đến bảng dữ liệu này không
   */
  function isRelevantTopic(topic) {
    if (!topic) return false
    
    // If specific topic filters are provided, use them
    if (topicFilters.length > 0) {
      return topicFilters.some(filter => topic.includes(filter))
    }
    
    // Default relevance check based on entity type
    const entityTopicMap = {
      'hoaDon': ['hoa-don', 'order'],
      'sanPham': ['gia-san-pham', 'san-pham', 'product', 'ton-kho'],
      'phieuGiamGia': ['phieu-giam-gia', 'voucher'],
      'dotGiamGia': ['dot-giam-gia', 'discount'],
      'nguoiDung': ['nguoi-dung', 'user'],
      'thongKe': ['thong-ke', 'statistics', 'dashboard']
    }
    
    const relevantTopics = entityTopicMap[entityType] || []
    return relevantTopics.some(topicPart => topic.includes(topicPart))
  }

  /**
   * Check if update is relevant to this DataTable
   * Vietnamese Business Context: Kiểm tra cập nhật có liên quan đến bảng dữ liệu này không
   */
  function isRelevantToDataTable(scope, entityId) {
    // Always relevant if no specific entity ID
    if (!entityId) return true
    
    // Check scope relevance
    const scopeRelevanceMap = {
      'ORDER_DATA': entityType === 'hoaDon',
      'PRICING_DATA': entityType === 'sanPham',
      'VOUCHER_DATA': entityType === 'phieuGiamGia',
      'DISCOUNT_DATA': entityType === 'dotGiamGia',
      'USER_DATA': entityType === 'nguoiDung',
      'INVENTORY_DATA': entityType === 'sanPham',
      'STATISTICS_DATA': entityType === 'thongKe'
    }
    
    return scopeRelevanceMap[scope] || true
  }

  /**
   * Trigger intelligent refresh with debouncing and performance optimization
   * Vietnamese Business Context: Kích hoạt làm mới thông minh với debouncing và tối ưu hóa hiệu suất
   */
  function triggerIntelligentRefresh(options = {}) {
    const {
      source = 'UNKNOWN',
      selective = false,
      entityId = null,
      debounce = true
    } = options

    // Clear existing debounce timer if debouncing is enabled
    if (debounce && realTimeState.value.debounceTimer) {
      clearTimeout(realTimeState.value.debounceTimer)
    }

    const executeRefresh = () => {
      if (realTimeState.value.pendingRefresh) {
        performanceMetrics.value.skippedRefreshes++
        return
      }

      performRefresh({
        source,
        selective,
        entityId,
        timestamp: new Date().toISOString()
      })
    }

    if (debounce) {
      realTimeState.value.debounceTimer = setTimeout(executeRefresh, debounceDelay)
    } else {
      executeRefresh()
    }
  }

  /**
   * Perform actual data refresh
   * Vietnamese Business Context: Thực hiện làm mới dữ liệu thực tế
   */
  async function performRefresh(refreshInfo) {
    if (!refreshCallback || typeof refreshCallback !== 'function') {
      console.warn(`⚠️ No refresh callback provided for DataTable ${entityType}`)
      return
    }

    const startTime = performance.now()
    realTimeState.value.pendingRefresh = true
    realTimeState.value.lastUpdateSource = refreshInfo.source

    try {
      console.log(`🔄 Refreshing DataTable ${entityType}:`, refreshInfo)

      // Call the provided refresh callback
      await refreshCallback(refreshInfo)

      // Update metrics
      const duration = performance.now() - startTime
      performanceMetrics.value.lastRefreshDuration = duration
      performanceMetrics.value.totalRefreshes++
      performanceMetrics.value.averageRefreshTime = 
        (performanceMetrics.value.averageRefreshTime * (performanceMetrics.value.totalRefreshes - 1) + duration) / 
        performanceMetrics.value.totalRefreshes

      if (refreshInfo.selective) {
        performanceMetrics.value.selectiveUpdates++
      }

      // Update state
      realTimeState.value.lastRefreshTime = new Date().toISOString()
      realTimeState.value.refreshCount++
      realTimeState.value.errorCount = 0

      // Broadcast to other tabs
      if (enableCrossTab) {
        realTimeSync.broadcastToOtherTabs('DATATABLE_REFRESH', {
          entityType,
          source: refreshInfo.source,
          timestamp: realTimeState.value.lastRefreshTime
        })
      }

      console.log(`✅ DataTable ${entityType} refresh completed in ${duration.toFixed(2)}ms`)

    } catch (error) {
      console.error(`❌ DataTable ${entityType} refresh failed:`, error)
      realTimeState.value.errorCount++
      
      // Show user notification for critical errors
      if (realTimeState.value.errorCount >= 3) {
        toast.add({
          severity: 'warn',
          summary: 'Cập nhật dữ liệu',
          detail: `Có lỗi khi cập nhật dữ liệu ${entityType}. Vui lòng tải lại trang.`,
          life: 5000
        })
      }
    } finally {
      realTimeState.value.pendingRefresh = false
    }
  }

  /**
   * Subscribe to relevant WebSocket topics
   * Vietnamese Business Context: Đăng ký các chủ đề WebSocket liên quan
   */
  function subscribeToRelevantTopics() {
    // This will be handled by the WebSocket manager
    // We just track which topics we're interested in
    const relevantTopics = getRelevantTopicsForEntity(entityType)
    relevantTopics.forEach(topic => {
      subscribedTopics.value.add(topic)
    })
    
    console.log(`📡 DataTable ${entityType} subscribed to topics:`, Array.from(subscribedTopics.value))
  }

  /**
   * Get relevant WebSocket topics for entity type
   * Vietnamese Business Context: Lấy các chủ đề WebSocket liên quan cho loại thực thể
   */
  function getRelevantTopicsForEntity(entityType) {
    const topicMap = {
      'hoaDon': [
        '/topic/hoa-don/new',
        '/topic/hoa-don/updated',
        '/topic/hoa-don/status-changed'
      ],
      'sanPham': [
        '/topic/gia-san-pham/updates',
        '/topic/san-pham/new',
        '/topic/san-pham/updated',
        '/topic/ton-kho/updates'
      ],
      'phieuGiamGia': [
        '/topic/phieu-giam-gia/new',
        '/topic/phieu-giam-gia/expired',
        '/topic/phieu-giam-gia/alternatives'
      ],
      'dotGiamGia': [
        '/topic/dot-giam-gia/new',
        '/topic/dot-giam-gia/updated',
        '/topic/dot-giam-gia/status-changed'
      ],
      'nguoiDung': [
        '/topic/nguoi-dung/new',
        '/topic/nguoi-dung/updated'
      ],
      'thongKe': [
        '/topic/thong-ke/updated',
        '/topic/dashboard/refresh'
      ]
    }
    
    return topicMap[entityType] || []
  }

  // Computed properties
  const isRealTimeActive = computed(() => realTimeState.value.isActive)
  const isWebSocketConnected = computed(() => webSocketManager.isConnected.value)
  const hasRecentErrors = computed(() => realTimeState.value.errorCount > 0)
  const refreshRate = computed(() => {
    const total = performanceMetrics.value.totalRefreshes
    return total > 0 ? (total / (Date.now() - (realTimeState.value.lastRefreshTime ? new Date(realTimeState.value.lastRefreshTime).getTime() : Date.now())) * 60000).toFixed(2) : 0
  })

  // Lifecycle management
  onMounted(() => {
    initializeRealTimeIntegration()
  })

  onUnmounted(() => {
    // Cleanup
    if (realTimeState.value.debounceTimer) {
      clearTimeout(realTimeState.value.debounceTimer)
    }
    
    realTimeState.value.isActive = false
    subscribedTopics.value.clear()
    topicHandlers.value.clear()
    
    console.log(`🧹 DataTable ${entityType} real-time integration cleaned up`)
  })

  return {
    // State
    realTimeState: computed(() => realTimeState.value),
    performanceMetrics: computed(() => performanceMetrics.value),
    subscribedTopics: computed(() => Array.from(subscribedTopics.value)),
    
    // Computed
    isRealTimeActive,
    isWebSocketConnected,
    hasRecentErrors,
    refreshRate,
    
    // Methods
    triggerIntelligentRefresh,
    performRefresh,
    
    // Integration with real-time sync
    realTimeSync
  }
}
