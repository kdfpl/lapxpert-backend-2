import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import orderApi from '@/apis/orderApi'
import { useToast } from 'primevue/usetoast'
import { useOrderCache } from '@/composables/useOrderCache'
import { usePerformanceOptimization } from '@/composables/usePerformanceOptimization'
import { useEntityStore } from '@/composables/useEntityStore'
import { useOptimisticUpdates } from '@/composables/useOptimisticUpdates'
import { useRealTimeSync } from '@/composables/useRealTimeSync'
import { useOptimisticMutation } from '@/composables/useOptimisticMutation'
import { createConflictResolver, RESOLUTION_STRATEGIES } from '@/utils/StateConflictResolver'
import { createVietnameseEntityAdapter } from '@/utils/EntityAdapter'

export const useOrderStore = defineStore('order', () => {
  const toast = useToast()

  // Performance optimization composables
  const orderCache = useOrderCache()
  const { debounce, deduplicateRequest, measurePerformance } = usePerformanceOptimization()

  // Real-time synchronization layer
  const realTimeSync = useRealTimeSync({
    entityName: 'hoaDon',
    storeKey: 'orderStore',
    enablePersistence: true,
    enableCrossTab: true,
    validateState: (state) => {
      // Validate order state structure
      if (!state || typeof state !== 'object') return false
      if (state.maHoaDon && typeof state.maHoaDon !== 'string') return false
      if (state.tongThanhToan && typeof state.tongThanhToan !== 'number') return false
      return true
    },
    mergeStrategy: (currentState, incomingState) => {
      // Custom merge strategy for orders
      return {
        ...currentState,
        ...incomingState,
        // Preserve critical fields from current state if they exist
        maHoaDon: currentState.maHoaDon || incomingState.maHoaDon,
        ngayCapNhat: new Date().toISOString(),
        version: (currentState.version || 0) + 1
      }
    }
  })

  // Enhanced optimistic mutations
  const optimisticMutation = useOptimisticMutation({
    entityName: 'đơn hàng',
    storeKey: 'orderStore',
    timeoutMs: 20000, // Orders may take longer
    enableRealTimeSync: true,
    enableConflictResolution: true,
    conflictStrategy: RESOLUTION_STRATEGIES.BUSINESS_RULES
  })

  // Conflict resolver for business rules
  const conflictResolver = createConflictResolver({
    entityName: 'đơn hàng',
    defaultStrategy: RESOLUTION_STRATEGIES.BUSINESS_RULES,
    businessRules: {
      // Order status transition rules
      trangThaiDonHang: (currentStatus, incomingStatus, context) => {
        const validTransitions = {
          'CHO_XAC_NHAN': ['XAC_NHAN', 'HUY'],
          'XAC_NHAN': ['DANG_GIAO', 'HUY'],
          'DANG_GIAO': ['HOAN_THANH', 'TRA_HANG'],
          'HOAN_THANH': ['TRA_HANG'],
          'HUY': [],
          'TRA_HANG': []
        }

        const allowedTransitions = validTransitions[currentStatus] || []
        return allowedTransitions.includes(incomingStatus) ? incomingStatus : currentStatus
      },

      // Payment status rules
      trangThaiThanhToan: (currentStatus, incomingStatus, context) => {
        // Payment status can only progress forward
        const statusOrder = ['CHUA_THANH_TOAN', 'DA_THANH_TOAN_MOT_PHAN', 'DA_THANH_TOAN', 'HOAN_TIEN']
        const currentIndex = statusOrder.indexOf(currentStatus)
        const incomingIndex = statusOrder.indexOf(incomingStatus)

        return incomingIndex >= currentIndex ? incomingStatus : currentStatus
      },

      // Total amount validation
      tongThanhToan: (currentAmount, incomingAmount, context) => {
        // Use higher amount if both are valid
        if (typeof currentAmount === 'number' && typeof incomingAmount === 'number') {
          return Math.max(currentAmount, incomingAmount)
        }
        return incomingAmount || currentAmount
      }
    }
  })

  // Enhanced entity store with normalized state management
  const {
    allEntities: orders,
    totalCount: totalRecords,
    getEntityById: getOrderById,
    addEntity: addOrderEntity,
    updateEntity: updateOrderEntity,
    removeEntity: removeOrderEntity,
    setAllEntities: setAllOrders,
    upsertEntity: upsertOrder,
    clearAllEntities: clearAllOrders,
    entityState: normalizedOrderState,
    loading: entityLoading,
    error: entityError,
    lastUpdated: lastOrderUpdate
  } = useEntityStore({
    entityName: 'hoaDon',
    selectId: (order) => order.id || order.maHoaDon,
    sortComparer: (a, b) => {
      // Sort by ngayCapNhat (newest first) - Vietnamese pattern
      const dateA = new Date(a.ngayCapNhat || a.createdAt || 0)
      const dateB = new Date(b.ngayCapNhat || b.createdAt || 0)
      return dateB - dateA
    },
    enableCrossTab: true,
    enableOptimistic: true
  })

  // Optimistic updates composable
  const {
    createOptimisticUpdate,
    batchOptimisticOperations,
    hasPendingOperations,
    successRate
  } = useOptimisticUpdates({
    entityName: 'đơn hàng',
    timeoutMs: 15000, // Orders may take longer
    enableRetry: true,
    maxRetries: 2
  })

  // Additional state (preserved from original)
  const currentOrder = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const currentPage = ref(0)
  const pageSize = ref(20)
  const auditHistory = ref({})

  // Real-time state synchronization
  const realTimeState = ref({
    isConnected: false,
    lastSyncTime: null,
    syncErrors: [],
    conflictCount: 0,
    debugMode: false
  })

  // State persistence and migration
  const stateMigration = ref({
    currentVersion: 1,
    migrationHistory: [],
    pendingMigrations: []
  })

  // Debugging and monitoring
  const debugInfo = ref({
    stateChanges: [],
    performanceMetrics: {},
    errorLog: [],
    maxLogSize: 100
  })

  // Multi-tab order management state
  const orderTabs = ref([])
  const activeTabId = ref(null)
  const tabCounter = ref(1)
  const reservedInventory = ref(new Map()) // Track reserved items per order

  // Initialize real-time sync event listeners
  if (realTimeSync) {
    // Listen for remote state changes
    realTimeSync.addEventListener('REMOTE_SYNC_RECEIVED', (event) => {
      handleRemoteStateSync(event.data)
    })

    realTimeSync.addEventListener('REMOTE_CHANGE_RECEIVED', (event) => {
      handleRemoteStateChange(event.data)
    })

    realTimeSync.addEventListener('CONFLICT_RESOLVED', (event) => {
      handleConflictResolution(event.data)
    })

    realTimeSync.addEventListener('STATE_SYNCED', (event) => {
      realTimeState.value.lastSyncTime = event.data.timestamp
      realTimeState.value.isConnected = true
    })
  }

  // Filters
  const filters = ref({
    status: 'all',
    type: 'all', // ONLINE, TAI_QUAY, all
    dateRange: null,
    customer: null,
    staff: null,
    search: ''
  })

  /**
   * Handle remote state synchronization
   * @param {Object} data - Remote sync data
   */
  function handleRemoteStateSync(data) {
    try {
      const { remoteState, timestamp, validation } = data

      if (!validation.isValid) {
        logDebugInfo('SYNC_ERROR', 'Invalid remote state received', { validation })
        return
      }

      // Merge remote state with current state
      if (remoteState && Array.isArray(remoteState)) {
        setAllOrders(remoteState)
        logDebugInfo('SYNC_SUCCESS', 'Remote state synchronized', {
          orderCount: remoteState.length,
          timestamp
        })
      }

      realTimeState.value.lastSyncTime = timestamp
      realTimeState.value.isConnected = true

    } catch (error) {
      logDebugInfo('SYNC_ERROR', 'Failed to handle remote sync', { error: error.message })
      realTimeState.value.syncErrors.push({
        type: 'REMOTE_SYNC_ERROR',
        message: error.message,
        timestamp: new Date().toISOString()
      })
    }
  }

  /**
   * Handle remote state changes
   * @param {Object} data - Remote change data
   */
  function handleRemoteStateChange(data) {
    try {
      const { changeData, timestamp } = data

      logDebugInfo('REMOTE_CHANGE', 'Remote state change received', {
        changeData,
        timestamp
      })

      // Apply remote changes if they don't conflict with local pending operations
      if (!optimisticMutation.hasPendingMutations.value) {
        // Safe to apply remote changes
        if (changeData.type === 'ORDER_UPDATED' && changeData.order) {
          upsertOrder(changeData.order)
        } else if (changeData.type === 'ORDER_CREATED' && changeData.order) {
          addOrderEntity(changeData.order)
        } else if (changeData.type === 'ORDER_DELETED' && changeData.orderId) {
          removeOrderEntity(changeData.orderId)
        }
      }

    } catch (error) {
      logDebugInfo('CHANGE_ERROR', 'Failed to handle remote change', { error: error.message })
    }
  }

  /**
   * Handle conflict resolution
   * @param {Object} data - Conflict resolution data
   */
  function handleConflictResolution(data) {
    try {
      const { resolutionData, timestamp } = data

      realTimeState.value.conflictCount++
      logDebugInfo('CONFLICT_RESOLVED', 'State conflict resolved', {
        resolutionData,
        timestamp
      })

      toast.add({
        severity: 'info',
        summary: 'Xung đột đã được giải quyết',
        detail: 'Dữ liệu đơn hàng đã được đồng bộ tự động',
        life: 3000
      })

    } catch (error) {
      logDebugInfo('CONFLICT_ERROR', 'Failed to handle conflict resolution', { error: error.message })
    }
  }

  /**
   * Log debug information
   * @param {String} type - Log type
   * @param {String} message - Log message
   * @param {Object} data - Additional data
   */
  function logDebugInfo(type, message, data = {}) {
    if (!realTimeState.value.debugMode) return

    const logEntry = {
      type,
      message,
      data,
      timestamp: new Date().toISOString(),
      tabId: realTimeSync?.tabId?.value
    }

    debugInfo.value.stateChanges.unshift(logEntry)

    // Keep log size manageable
    if (debugInfo.value.stateChanges.length > debugInfo.value.maxLogSize) {
      debugInfo.value.stateChanges = debugInfo.value.stateChanges.slice(0, debugInfo.value.maxLogSize)
    }

    // Also log to console in debug mode
    console.log(`🔍 [OrderStore Debug] ${type}: ${message}`, data)
  }

  // Order statuses from backend
  const orderStatuses = ref([
    { value: 'CHO_XAC_NHAN', label: 'Chờ xác nhận', severity: 'warning', icon: 'pi pi-clock' },
    { value: 'DA_XAC_NHAN', label: 'Đã xác nhận', severity: 'info', icon: 'pi pi-check' },
    { value: 'DANG_XU_LY', label: 'Đang chuẩn bị hàng', severity: 'info', icon: 'pi pi-cog' },
    { value: 'CHO_GIAO_HANG', label: 'Chờ giao hàng', severity: 'warning', icon: 'pi pi-package' },
    { value: 'DANG_GIAO_HANG', label: 'Đang giao hàng', severity: 'info', icon: 'pi pi-truck' },
    { value: 'DA_GIAO_HANG', label: 'Đã giao hàng', severity: 'success', icon: 'pi pi-check-circle' },
    { value: 'HOAN_THANH', label: 'Hoàn thành', severity: 'success', icon: 'pi pi-verified' },
    { value: 'DA_HUY', label: 'Đã hủy', severity: 'danger', icon: 'pi pi-times-circle' },
    { value: 'YEU_CAU_TRA_HANG', label: 'Yêu cầu trả hàng', severity: 'warning', icon: 'pi pi-undo' },
    { value: 'DA_TRA_HANG', label: 'Đã trả hàng', severity: 'warning', icon: 'pi pi-undo' },
    { value: 'THAT_BAI', label: 'Thất bại', severity: 'danger', icon: 'pi pi-exclamation-triangle' }
  ])

  const orderTypes = ref([
    { value: 'ONLINE', label: 'Đơn hàng online', icon: 'pi pi-globe' },
    { value: 'TAI_QUAY', label: 'Bán tại quầy', icon: 'pi pi-shop' }
  ])

  const paymentStatuses = ref([
    { value: 'CHUA_THANH_TOAN', label: 'Chưa thanh toán', severity: 'warn' },
    { value: 'DA_THANH_TOAN', label: 'Đã thanh toán', severity: 'success' },
    { value: 'THANH_TOAN_MOT_PHAN', label: 'Thanh toán một phần', severity: 'info' },
    { value: 'HOAN_TIEN', label: 'Hoàn tiền', severity: 'info' }
  ])

  // Cache management
  const lastFetchTime = ref(null)
  const cacheTimeout = 5 * 60 * 1000 // 5 minutes

  // Cache validation
  const isCacheValid = () => {
    if (!lastFetchTime.value) return false
    return Date.now() - lastFetchTime.value < cacheTimeout
  }

  // Computed (enhanced with normalized state)
  const filteredOrders = computed(() => {
    if (!orders.value || orders.value.length === 0) return []

    return orders.value.filter(order => {
      // Status filter
      if (filters.value.status !== 'all' && order.trangThaiDonHang !== filters.value.status) {
        return false
      }

      // Type filter
      if (filters.value.type !== 'all' && order.loaiHoaDon !== filters.value.type) {
        return false
      }

      // Search filter
      if (filters.value.search) {
        const searchTerm = filters.value.search.toLowerCase()
        const searchableText = `${order.maHoaDon} ${order.khachHang?.hoTen || ''} ${order.nhanVien?.hoTen || ''}`.toLowerCase()
        if (!searchableText.includes(searchTerm)) {
          return false
        }
      }

      // Date range filter
      if (filters.value.dateRange && filters.value.dateRange.length === 2) {
        const orderDate = new Date(order.ngayTao)
        const [startDate, endDate] = filters.value.dateRange
        if (orderDate < startDate || orderDate > endDate) {
          return false
        }
      }

      return true
    })
  })

  const orderStatusMap = computed(() => {
    const map = {}
    orderStatuses.value.forEach(status => {
      map[status.value] = status
    })
    return map
  })

  const orderTypeMap = computed(() => {
    const map = {}
    orderTypes.value.forEach(type => {
      map[type.value] = type
    })
    return map
  })

  const paymentStatusMap = computed(() => {
    const map = {}
    paymentStatuses.value.forEach(status => {
      map[status.value] = status
    })
    return map
  })

  // Multi-tab computed properties
  const activeTab = computed(() => {
    return orderTabs.value.find(tab => tab.id === activeTabId.value)
  })

  const hasActiveTabs = computed(() => {
    return orderTabs.value.length > 0
  })

  const canCreateNewTab = computed(() => {
    return orderTabs.value.length < 5 // Limit to 5 concurrent orders
  })

  // Multi-tab order management actions
  const generateOrderCode = () => {
    const timestamp = Date.now().toString().slice(-6)
    const counter = tabCounter.value.toString().padStart(3, '0')
    return `HD${timestamp}${counter}`
  }

  const createNewOrderTab = () => {
    if (!canCreateNewTab.value) {
      toast.add({
        severity: 'warn',
        summary: 'Cảnh báo',
        detail: 'Không thể tạo thêm đơn hàng. Tối đa 5 đơn hàng cùng lúc.',
        life: 3000
      })
      return null
    }

    const newTab = {
      id: `tab_${Date.now()}_${tabCounter.value}`,
      maHoaDon: generateOrderCode(),
      loaiHoaDon: 'TAI_QUAY',
      khachHang: null,
      diaChiGiaoHang: null,
      giaohang: false,
      sanPhamList: [],
      voucherList: [],
      phuongThucThanhToan: null,
      tongTienHang: 0,
      giaTriGiamGiaVoucher: 0,
      phiVanChuyen: 0,
      tongThanhToan: 0,
      trangThaiDonHang: 'CHO_XAC_NHAN',
      trangThaiThanhToan: 'CHUA_THANH_TOAN',
      createdAt: new Date(),
      isModified: false
    }

    orderTabs.value.push(newTab)
    activeTabId.value = newTab.id
    tabCounter.value++

    return newTab
  }

  const closeOrderTab = (tabId) => {
    const tabIndex = orderTabs.value.findIndex(tab => tab.id === tabId)
    if (tabIndex === -1) return

    // const tab = orderTabs.value[tabIndex] // Currently unused

    // Release any reserved inventory for this tab
    if (reservedInventory.value.has(tabId)) {
      reservedInventory.value.delete(tabId)
    }

    // Remove the tab
    orderTabs.value.splice(tabIndex, 1)

    // Update active tab if necessary
    if (activeTabId.value === tabId) {
      if (orderTabs.value.length > 0) {
        // Switch to the previous tab or first available
        const newActiveIndex = Math.max(0, tabIndex - 1)
        activeTabId.value = orderTabs.value[newActiveIndex]?.id || null
      } else {
        activeTabId.value = null
      }
    }
  }

  const switchToTab = (tabId) => {
    const tab = orderTabs.value.find(t => t.id === tabId)
    if (tab) {
      activeTabId.value = tabId
    }
  }

  const updateActiveTabData = (updates) => {
    if (!activeTab.value) return

    const tabIndex = orderTabs.value.findIndex(tab => tab.id === activeTabId.value)
    if (tabIndex !== -1) {
      orderTabs.value[tabIndex] = { ...orderTabs.value[tabIndex], ...updates, isModified: true }
      calculateTabTotals(activeTabId.value)
    }
  }

  const calculateTabTotals = (tabId) => {
    const tab = orderTabs.value.find(t => t.id === tabId)
    if (!tab) return

    // Calculate subtotal from products
    const tongTienHang = tab.sanPhamList.reduce((total, item) => {
      return total + (item.donGia * item.soLuong)
    }, 0)

    // Calculate voucher discount
    const giaTriGiamGiaVoucher = tab.voucherList.reduce((total, voucher) => {
      return total + voucher.giaTriGiam
    }, 0)

    // Calculate shipping fee (only for delivery orders)
    const phiVanChuyen = tab.giaohang ? 30000 : 0

    // Calculate final total
    const tongThanhToan = Math.max(0, tongTienHang - giaTriGiamGiaVoucher + phiVanChuyen)

    // Update tab data
    const tabIndex = orderTabs.value.findIndex(t => t.id === tabId)
    if (tabIndex !== -1) {
      orderTabs.value[tabIndex] = {
        ...orderTabs.value[tabIndex],
        tongTienHang,
        giaTriGiamGiaVoucher,
        phiVanChuyen,
        tongThanhToan
      }
    }
  }

  /**
   * Enhanced state persistence with migration support
   */
  function persistOrderState() {
    try {
      const stateToSave = {
        orders: orders.value,
        currentOrder: currentOrder.value,
        filters: filters.value,
        orderTabs: orderTabs.value,
        activeTabId: activeTabId.value,
        version: stateMigration.value.currentVersion,
        timestamp: new Date().toISOString()
      }

      if (realTimeSync) {
        realTimeSync.persistState(stateToSave)
      }

      logDebugInfo('STATE_PERSISTED', 'Order state persisted', {
        orderCount: orders.value.length,
        tabCount: orderTabs.value.length
      })

    } catch (error) {
      logDebugInfo('PERSIST_ERROR', 'Failed to persist state', { error: error.message })
    }
  }

  /**
   * Load persisted state with migration
   */
  function loadPersistedOrderState() {
    try {
      if (!realTimeSync) return null

      const persistedData = realTimeSync.loadPersistedState()
      if (!persistedData) return null

      // Check if migration is needed
      const currentVersion = stateMigration.value.currentVersion
      const persistedVersion = persistedData.version || 0

      if (persistedVersion < currentVersion) {
        return migrateOrderState(persistedData, persistedVersion, currentVersion)
      }

      return persistedData.state

    } catch (error) {
      logDebugInfo('LOAD_ERROR', 'Failed to load persisted state', { error: error.message })
      return null
    }
  }

  /**
   * Migrate order state between versions
   * @param {Object} oldState - Old state data
   * @param {Number} fromVersion - Source version
   * @param {Number} toVersion - Target version
   * @returns {Object} Migrated state
   */
  function migrateOrderState(oldState, fromVersion, toVersion) {
    try {
      let migratedState = { ...oldState }

      // Apply migrations sequentially
      for (let version = fromVersion; version < toVersion; version++) {
        migratedState = applyStateMigration(migratedState, version, version + 1)
      }

      // Record migration
      stateMigration.value.migrationHistory.push({
        fromVersion,
        toVersion,
        timestamp: new Date().toISOString(),
        success: true
      })

      logDebugInfo('STATE_MIGRATED', 'State migrated successfully', {
        fromVersion,
        toVersion
      })

      return migratedState

    } catch (error) {
      logDebugInfo('MIGRATION_ERROR', 'State migration failed', {
        error: error.message,
        fromVersion,
        toVersion
      })

      // Record failed migration
      stateMigration.value.migrationHistory.push({
        fromVersion,
        toVersion,
        timestamp: new Date().toISOString(),
        success: false,
        error: error.message
      })

      return null
    }
  }

  /**
   * Apply specific migration between versions
   * @param {Object} state - State to migrate
   * @param {Number} fromVersion - Source version
   * @param {Number} toVersion - Target version
   * @returns {Object} Migrated state
   */
  function applyStateMigration(state, fromVersion, toVersion) {
    // Example migration logic
    if (fromVersion === 0 && toVersion === 1) {
      // Migration from version 0 to 1: Add new fields
      return {
        ...state,
        orders: (state.orders || []).map(order => ({
          ...order,
          version: 1,
          ngayCapNhat: order.ngayCapNhat || new Date().toISOString()
        }))
      }
    }

    return state
  }

  // Actions (enhanced with normalized state management and real-time sync)
  const fetchOrders = async (page = 0, size = 20, filterParams = {}) => {
    const endMeasure = measurePerformance('fetchOrders')

    const params = {
      page,
      size,
      ...filterParams,
      ...filters.value
    }

    // Check cache first
    const cachedData = orderCache.getCachedOrderList(params)
    if (cachedData) {
      const ordersData = cachedData.content || cachedData
      setAllOrders(ordersData)
      currentPage.value = page
      pageSize.value = size

      // Sync with real-time system
      if (realTimeSync) {
        await realTimeSync.syncStateData(ordersData, { merge: false })
      }

      endMeasure()
      return
    }

    // Use request deduplication to prevent multiple identical requests
    const cacheKey = orderCache.generateCacheKey('orderList', params)

    try {
      loading.value = true
      error.value = null

      const response = await deduplicateRequest(cacheKey, async () => {
        return await orderApi.getAllOrders(params)
      })

      if (response.success) {
        const data = response.data
        const ordersData = data.content || data

        // Use normalized state management
        setAllOrders(ordersData)
        currentPage.value = page
        pageSize.value = size

        // Sync with real-time system
        if (realTimeSync) {
          await realTimeSync.syncStateData(ordersData, { merge: false })
        }

        // Cache the response for 2 minutes
        orderCache.setCachedOrderList(params, data, 2 * 60 * 1000)

        // Persist state
        persistOrderState()

        logDebugInfo('FETCH_SUCCESS', 'Orders fetched successfully', {
          count: ordersData.length,
          page,
          size
        })

      } else {
        throw new Error(response.message || 'Failed to fetch orders')
      }
    } catch (err) {
      error.value = err.message
      logDebugInfo('FETCH_ERROR', 'Failed to fetch orders', { error: err.message })

      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tải danh sách đơn hàng: ${err.message}`,
        life: 5000
      })
    } finally {
      loading.value = false
      endMeasure()
    }
  }

  const fetchOrderById = async (id) => {
    const endMeasure = measurePerformance('fetchOrderById')

    // Check normalized state first
    const existingOrder = getOrderById(id)
    if (existingOrder) {
      currentOrder.value = existingOrder
      endMeasure()
      return existingOrder
    }

    // Check cache
    const cachedOrder = orderCache.getCachedOrderDetail(id)
    if (cachedOrder) {
      currentOrder.value = cachedOrder
      // Add to normalized state
      upsertOrder(cachedOrder)
      endMeasure()
      return cachedOrder
    }

    try {
      loading.value = true
      error.value = null

      const response = await deduplicateRequest(`orderDetail:${id}`, async () => {
        return await orderApi.getOrderById(id)
      })

      if (response.success) {
        currentOrder.value = response.data

        // Add to normalized state
        upsertOrder(response.data)

        // Cache the order detail for 5 minutes
        orderCache.setCachedOrderDetail(id, response.data, 5 * 60 * 1000)

        endMeasure()
        return response.data
      } else {
        throw new Error(response.message || 'Failed to fetch order')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tải thông tin đơn hàng: ${err.message}`,
        life: 5000
      })
      endMeasure()
      return null
    } finally {
      loading.value = false
    }
  }

  const createOrder = async (orderData) => {
    // Create optimistic order with temporary ID
    const optimisticOrder = {
      ...orderData,
      id: `temp_${Date.now()}`,
      maHoaDon: orderData.maHoaDon || `HD${Date.now()}`,
      ngayCapNhat: new Date().toISOString(),
      trangThaiDonHang: orderData.trangThaiDonHang || 'CHO_XAC_NHAN',
      isOptimistic: true
    }

    // Use optimistic update
    const optimisticUpdate = createOptimisticUpdate(
      'create',
      optimisticOrder,
      // Optimistic update function
      async () => {
        currentOrder.value = optimisticOrder
        await addOrderEntity(optimisticOrder)
      },
      // Rollback function
      async () => {
        await removeOrderEntity(optimisticOrder.id)
        currentOrder.value = null
      },
      // API call function
      async () => {
        loading.value = true
        error.value = null

        try {
          const response = await orderApi.createOrder(orderData)

          if (response.success) {
            // Invalidate order list cache since we added a new order
            orderCache.invalidateByPattern('^orderList:')
            return response
          } else {
            throw new Error(response.message || 'Failed to create order')
          }
        } finally {
          loading.value = false
        }
      }
    )

    return optimisticUpdate()
  }

  // Create order from active tab with enhanced audit
  const createOrderFromTab = async (tabId = null) => {
    const targetTabId = tabId || activeTabId.value
    const tab = orderTabs.value.find(t => t.id === targetTabId)

    if (!tab) {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không tìm thấy đơn hàng để tạo',
        life: 3000
      })
      return null
    }

    // Validate required fields
    if (!tab.sanPhamList.length) {
      toast.add({
        severity: 'warn',
        summary: 'Cảnh báo',
        detail: 'Vui lòng thêm ít nhất một sản phẩm',
        life: 3000
      })
      return null
    }

    if (!tab.phuongThucThanhToan) {
      toast.add({
        severity: 'warn',
        summary: 'Cảnh báo',
        detail: 'Vui lòng chọn phương thức thanh toán',
        life: 3000
      })
      return null
    }

    // Prepare order data for API (match backend DTO structure)
    const orderData = {
      maHoaDon: tab.maHoaDon,
      loaiHoaDon: tab.loaiHoaDon,

      // Send only IDs to avoid transient entity issues
      khachHangId: tab.khachHang?.id || null,
      diaChiGiaoHangId: tab.diaChiGiaoHang?.id || null,

      // Delivery contact information
      nguoiNhanTen: tab.khachHang?.hoTen || null,
      nguoiNhanSdt: tab.khachHang?.soDienThoai || null,

      chiTiet: tab.sanPhamList.map(item => ({
        sanPhamChiTietId: item.sanPhamChiTiet?.id || item.sanPham?.id,
        soLuong: item.soLuong,
        giaBan: item.donGia
      })),
      voucherCodes: tab.voucherList.map(voucher => voucher.maPhieuGiamGia),
      phuongThucThanhToan: tab.phuongThucThanhToan,
      tongTienHang: tab.tongTienHang,
      giaTriGiamGiaVoucher: tab.giaTriGiamGiaVoucher,
      phiVanChuyen: tab.phiVanChuyen,
      tongThanhToan: tab.tongThanhToan,
      trangThaiDonHang: tab.giaohang ? 'CHO_XAC_NHAN' : 'HOAN_THANH',
      trangThaiThanhToan: tab.phuongThucThanhToan === 'TIEN_MAT' ? 'DA_THANH_TOAN' : 'CHUA_THANH_TOAN'
    }

    // Prepare enhanced audit context
    const auditContext = {
      userAgent: navigator.userAgent,
      sessionId: sessionStorage.getItem('sessionId') || `session_${Date.now()}`,
      ipAddress: 'client-side' // This would be set by the backend
    }

    try {
      // Use enhanced audit API for order creation
      const result = await orderApi.createOrderWithEnhancedAudit(orderData, auditContext)

      if (result.success) {
        const createdOrder = result.data

        // Add new order to normalized state
        await addOrderEntity(createdOrder)
        currentOrder.value = createdOrder

        // Invalidate order list cache since we added a new order
        orderCache.invalidateByPattern('^orderList:')

        // Close the tab after successful creation
        closeOrderTab(targetTabId)

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đơn hàng ${createdOrder.maHoaDon} đã được tạo thành công`,
          life: 5000
        })

        return createdOrder
      } else {
        throw new Error(result.message || 'Failed to create order')
      }
    } catch (error) {
      console.error('Error creating order from tab:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tạo đơn hàng: ${error.message}`,
        life: 5000
      })
      return null
    }
  }

  const updateOrderStatus = async (orderId, newStatus, reason = '') => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.updateOrderStatus(orderId, newStatus, reason)

      if (response.success) {
        // Update order in normalized state
        await updateOrderEntity(orderId, response.data)

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        const statusLabel = orderStatusMap.value[newStatus]?.label || newStatus
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đơn hàng đã được cập nhật trạng thái: ${statusLabel}`,
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to update order status')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể cập nhật trạng thái đơn hàng: ${err.message}`,
        life: 5000
      })
      return null
    } finally {
      loading.value = false
    }
  }

  const updateOrder = async (id, updateData) => {
    loading.value = true
    error.value = null

    try {
      console.log('OrderStore: updateOrder called with id:', id)
      console.log('OrderStore: updateData:', updateData)

      const response = await orderApi.updateOrder(id, updateData)
      console.log('OrderStore: API response:', response)

      if (response.success) {
        // Update the order in normalized state
        await updateOrderEntity(id, response.data)

        // Update current order if it's the same
        if (currentOrder.value?.id === id) {
          currentOrder.value = response.data
        }

        // Invalidate order caches since we updated an order
        orderCache.invalidateByPattern('^orderList:')
        orderCache.invalidateByPattern(`^orderDetail:${id}`)

        console.log('OrderStore: Order updated successfully:', response.data)
        return response.data
      } else {
        throw new Error(response.message || 'Failed to update order')
      }
    } catch (err) {
      console.error('OrderStore: Error updating order:', err)
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const cancelOrder = async (orderId, reason) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.cancelOrder(orderId, reason)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Đơn hàng đã được hủy thành công',
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to cancel order')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể hủy đơn hàng: ${err.message}`,
        life: 5000
      })
      return null
    } finally {
      loading.value = false
    }
  }

  // Payment operations
  const confirmPayment = async (orderId, paymentMethod) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.confirmPayment(orderId, paymentMethod)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Xác nhận thanh toán thành công',
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to confirm payment')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể xác nhận thanh toán: ${err.message}`,
        life: 5000
      })
      throw err
    } finally {
      loading.value = false
    }
  }

  const processRefund = async (orderId, refundAmount, reason) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.processRefund(orderId, refundAmount, reason)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Hoàn tiền thành công',
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to process refund')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể hoàn tiền: ${err.message}`,
        life: 5000
      })
      throw err
    } finally {
      loading.value = false
    }
  }

  const updatePaymentStatus = async (orderId, paymentStatus, note = '') => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.updatePaymentStatus(orderId, paymentStatus, note)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        // Invalidate cache for this order
        orderCache.invalidateByPattern(`^order:${orderId}`)

        const statusLabel = paymentStatusMap.value[paymentStatus]?.label || paymentStatus
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Trạng thái thanh toán đã được cập nhật: ${statusLabel}`,
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to update payment status')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể cập nhật trạng thái thanh toán: ${err.message}`,
        life: 5000
      })
      throw err
    } finally {
      loading.value = false
    }
  }

  const printOrderReceipt = async (orderId) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.printOrderReceipt(orderId)

      if (response.success) {
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Tạo hóa đơn thành công',
          life: 3000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to generate receipt')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tạo hóa đơn: ${err.message}`,
        life: 5000
      })
      throw err
    } finally {
      loading.value = false
    }
  }

  // Batch operations
  const cancelMultipleOrders = async (orderIds, reason) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.cancelMultipleOrders(orderIds, reason)

      if (response.success) {
        // Update orders in the list
        orderIds.forEach(orderId => {
          const orderIndex = orders.value.findIndex(order => order.id === orderId)
          if (orderIndex !== -1) {
            orders.value[orderIndex] = {
              ...orders.value[orderIndex],
              trangThaiDonHang: 'DA_HUY',
              lyDoHuy: reason
            }
          }
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to cancel multiple orders')
      }
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // Audit history functionality
  const fetchOrderAuditHistory = async (orderId) => {
    try {
      const response = await orderApi.getOrderHistory(orderId)

      if (response.success) {
        auditHistory.value[orderId] = response.data
        return response.data
      } else {
        throw new Error(response.message || 'Failed to fetch order audit history')
      }
    } catch (err) {
      console.error('Error fetching order audit history:', err)
      auditHistory.value[orderId] = []
      return []
    }
  }

  // Export functionality
  const exportOrders = async (ordersData) => {
    try {
      // Create CSV content
      const headers = [
        'Mã đơn hàng',
        'Loại đơn hàng',
        'Khách hàng',
        'Tổng tiền',
        'Trạng thái đơn hàng',
        'Trạng thái thanh toán',
        'Ngày tạo'
      ]

      const csvContent = [
        headers.join(','),
        ...ordersData.map(order => [
          order.maHoaDon,
          getOrderTypeInfo(order.loaiHoaDon).label,
          order.khachHang?.hoTen || 'Khách lẻ',
          order.tongThanhToan,
          getOrderStatusInfo(order.trangThaiDonHang).label,
          getPaymentStatusInfo(order.trangThaiThanhToan).label,
          new Date(order.ngayTao).toLocaleDateString('vi-VN')
        ].join(','))
      ].join('\n')

      // Create and download file
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', `danh-sach-don-hang-${new Date().toISOString().split('T')[0]}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      return true
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const setFilters = (newFilters) => {
    filters.value = { ...filters.value, ...newFilters }
  }

  const clearFilters = () => {
    filters.value = {
      status: 'all',
      type: 'all',
      dateRange: null,
      customer: null,
      staff: null,
      search: ''
    }
  }

  const setCurrentOrder = (order) => {
    currentOrder.value = order
  }

  const clearCurrentOrder = () => {
    currentOrder.value = null
  }

  const clearError = () => {
    error.value = null
  }

  // Utility functions
  const getOrderStatusInfo = (status) => {
    return orderStatusMap.value[status] || { label: status, severity: 'secondary', icon: 'pi pi-question' }
  }

  const getOrderTypeInfo = (type) => {
    return orderTypeMap.value[type] || { label: type, icon: 'pi pi-question' }
  }

  const getPaymentStatusInfo = (status) => {
    return paymentStatusMap.value[status] || { label: status, severity: 'secondary' }
  }

  // Enhanced audit history with granular tracking
  const fetchEnhancedAuditHistory = async (orderId) => {
    try {
      loading.value = true
      error.value = null

      const response = await orderApi.getEnhancedAuditHistory(orderId)

      if (response.success) {
        auditHistory.value[orderId] = response.data
        return response.data
      } else {
        throw new Error(response.message || 'Failed to fetch enhanced audit history')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tải lịch sử kiểm toán: ${err.message}`,
        life: 5000
      })
      return []
    } finally {
      loading.value = false
    }
  }

  /**
   * Toggle debug mode
   * @param {Boolean} enabled - Enable debug mode
   */
  function toggleDebugMode(enabled = true) {
    realTimeState.value.debugMode = enabled
    logDebugInfo('DEBUG_MODE', `Debug mode ${enabled ? 'enabled' : 'disabled'}`)
  }

  /**
   * Get comprehensive debug information
   * @returns {Object} Debug information
   */
  function getDebugInfo() {
    return {
      realTimeState: realTimeState.value,
      stateMigration: stateMigration.value,
      debugInfo: debugInfo.value,
      syncMetrics: realTimeSync?.syncMetrics?.value,
      mutationMetrics: optimisticMutation?.mutationMetrics?.value,
      conflictStats: conflictResolver?.getStatistics()
    }
  }

  /**
   * Clear debug logs
   */
  function clearDebugLogs() {
    debugInfo.value.stateChanges = []
    debugInfo.value.errorLog = []
    realTimeState.value.syncErrors = []
    logDebugInfo('DEBUG_CLEARED', 'Debug logs cleared')
  }

  /**
   * Export debug data for analysis
   * @returns {String} JSON string of debug data
   */
  function exportDebugData() {
    const debugData = {
      timestamp: new Date().toISOString(),
      storeState: {
        orderCount: orders.value.length,
        currentOrder: currentOrder.value?.maHoaDon,
        activeTabCount: orderTabs.value.length,
        filters: filters.value
      },
      ...getDebugInfo()
    }

    return JSON.stringify(debugData, null, 2)
  }

  // Initialize persisted state on store creation
  const persistedState = loadPersistedOrderState()
  if (persistedState) {
    // Restore state from persistence
    if (persistedState.orders) setAllOrders(persistedState.orders)
    if (persistedState.currentOrder) currentOrder.value = persistedState.currentOrder
    if (persistedState.filters) filters.value = { ...filters.value, ...persistedState.filters }
    if (persistedState.orderTabs) orderTabs.value = persistedState.orderTabs
    if (persistedState.activeTabId) activeTabId.value = persistedState.activeTabId

    logDebugInfo('STATE_RESTORED', 'Persisted state restored', {
      orderCount: persistedState.orders?.length || 0,
      tabCount: persistedState.orderTabs?.length || 0
    })
  }

  // Watch for state changes to trigger persistence
  watch([orders, currentOrder, filters, orderTabs, activeTabId], () => {
    persistOrderState()
  }, { deep: true, debounce: 1000 })

  return {
    // State
    orders,
    currentOrder,
    loading,
    error,
    totalRecords,
    currentPage,
    pageSize,
    filters,
    orderStatuses,
    orderTypes,
    paymentStatuses,
    auditHistory,

    // Multi-tab state
    orderTabs,
    activeTabId,
    tabCounter,
    reservedInventory,

    // Real-time state
    realTimeState: computed(() => realTimeState.value),
    stateMigration: computed(() => stateMigration.value),

    // Computed
    filteredOrders,
    orderStatusMap,
    orderTypeMap,
    paymentStatusMap,
    activeTab,
    hasActiveTabs,
    canCreateNewTab,

    // Actions
    fetchOrders,
    fetchOrderById,
    createOrder,
    updateOrder,
    createOrderFromTab,
    updateOrderStatus,
    cancelOrder,
    cancelMultipleOrders,
    confirmPayment,
    processRefund,
    updatePaymentStatus,
    printOrderReceipt,
    fetchOrderAuditHistory,
    fetchEnhancedAuditHistory,
    exportOrders,
    setFilters,
    clearFilters,
    setCurrentOrder,
    clearCurrentOrder,
    clearError,

    // Multi-tab actions
    generateOrderCode,
    createNewOrderTab,
    closeOrderTab,
    switchToTab,
    updateActiveTabData,
    calculateTabTotals,

    // Utilities
    getOrderStatusInfo,
    getOrderTypeInfo,
    getPaymentStatusInfo,
    isCacheValid,

    // Performance optimization
    orderCache,
    debounce,

    // Enhanced normalized state management
    normalizedOrderState,
    getOrderById,
    lastOrderUpdate,

    // Optimistic updates
    hasPendingOperations,
    successRate,

    // Entity operations (for advanced usage)
    addOrderEntity,
    updateOrderEntity,
    removeOrderEntity,
    setAllOrders,
    upsertOrder,
    clearAllOrders,

    // Real-time synchronization
    realTimeSync,
    optimisticMutation,
    conflictResolver,

    // State management
    persistOrderState,
    loadPersistedOrderState,
    migrateOrderState,

    // Debug and monitoring
    toggleDebugMode,
    getDebugInfo,
    clearDebugLogs,
    exportDebugData,
    logDebugInfo
  }
})
