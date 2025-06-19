import { defineStore } from 'pinia'
import { useToast } from 'primevue/usetoast'
import discountService from '@/apis/discount'
import { useRealTimeSync } from '@/composables/useRealTimeSync'

/**
 * Enhanced Discount Store
 * Provides comprehensive state management for DotGiamGia (Discount Campaign) module
 * Following PhieuGiamGia patterns with Vietnam timezone support and automatic status computation
 */
export const useDiscountStore = defineStore('discount', {
  state: () => ({
    // Core data
    discounts: [],
    currentDiscount: null,

    // Loading states
    loading: false,
    saving: false,
    deleting: false,

    // Error handling
    error: null,

    // Caching
    discountCache: new Map(),
    lastFetch: null,
    cacheTimeout: 5 * 60 * 1000, // 5 minutes

    // Filters and search
    filters: {
      search: '',
      status: null,
      dateRange: null,
      percentageRange: [0, 100]
    },

    // Statistics
    statistics: {
      total: 0,
      active: 0,
      upcoming: 0,
      expired: 0,
      cancelled: 0
    },

    // Audit history
    auditHistory: {},

    // Batch operations
    batchOperations: {
      inProgress: false,
      results: null
    },

    // Real-time state management
    realTimeState: {
      isConnected: false,
      lastSyncTime: null,
      cacheInvalidationCount: 0,
      lastCacheInvalidation: null,
      realTimeSync: null
    }
  }),

  // Initialize real-time sync when store is created
  $onAction({ name, store, args, after, onError }) {
    // Initialize real-time sync on first action
    if (!store.realTimeState.realTimeSync) {
      const toast = useToast()

      store.realTimeState.realTimeSync = useRealTimeSync({
        entityName: 'dotGiamGia',
        storeKey: 'discountStore',
        enablePersistence: true,
        enableCrossTab: true,
        validateState: (state) => {
          if (!state || typeof state !== 'object') return false
          if (state.tenDotGiamGia && typeof state.tenDotGiamGia !== 'string') return false
          if (state.phanTramGiam && typeof state.phanTramGiam !== 'number') return false
          return true
        }
      })

      // Setup real-time sync event listeners
      store.realTimeState.realTimeSync.addEventListener('CACHE_INVALIDATION', (event) => {
        const { scope, entityId, requiresRefresh } = event.data

        if (scope === 'DISCOUNT_DATA') {
          store.handleCacheInvalidation(scope, entityId, requiresRefresh)
        }
      })

      store.realTimeState.realTimeSync.addEventListener('WEBSOCKET_STATE_UPDATE', (event) => {
        const { stateData } = event.data
        if (stateData && (stateData.tenDotGiamGia || stateData.maDotGiamGia)) {
          store.handleDiscountUpdate(stateData)
        }
      })
    }
  },

  getters: {
    // Filter discounts based on current filters
    filteredDiscounts: (state) => {
      let filtered = [...state.discounts]

      if (state.filters.search) {
        const search = state.filters.search.toLowerCase()
        filtered = filtered.filter(discount =>
          discount.maDotGiamGia?.toLowerCase().includes(search) ||
          discount.tenDotGiamGia?.toLowerCase().includes(search)
        )
      }

      if (state.filters.status) {
        filtered = filtered.filter(discount => discount.trangThai === state.filters.status)
      }

      if (state.filters.percentageRange) {
        const [min, max] = state.filters.percentageRange
        filtered = filtered.filter(discount =>
          discount.phanTramGiam >= min && discount.phanTramGiam <= max
        )
      }

      return filtered
    },

    // Get discounts by status
    activeDiscounts: (state) => state.discounts.filter(d => d.trangThai === 'DA_DIEN_RA'),
    upcomingDiscounts: (state) => state.discounts.filter(d => d.trangThai === 'CHUA_DIEN_RA'),
    expiredDiscounts: (state) => state.discounts.filter(d => d.trangThai === 'KET_THUC'),
    cancelledDiscounts: (state) => state.discounts.filter(d => d.trangThai === 'BI_HUY'),

    // Check if cache is valid
    isCacheValid: (state) => {
      if (!state.lastFetch) return false
      return Date.now() - state.lastFetch < state.cacheTimeout
    },

    // Get discount by ID from cache
    getDiscountById: (state) => (id) => {
      return state.discounts.find(discount => discount.id === id) ||
             state.discountCache.get(id)
    },

    // Vietnam timezone support methods (following PhieuGiamGia pattern)
    getStatusLabel: () => (trangThai) => {
      const labelMap = {
        CHUA_DIEN_RA: 'Chưa diễn ra',
        DA_DIEN_RA: 'Đang diễn ra',
        KET_THUC: 'Kết thúc',
        BI_HUY: 'Đã hủy'
      }
      return labelMap[trangThai] || 'Không xác định'
    },

    getStatusSeverity: () => (trangThai) => {
      const severityMap = {
        CHUA_DIEN_RA: 'warn',
        DA_DIEN_RA: 'success',
        KET_THUC: 'danger',
        BI_HUY: 'secondary'
      }
      return severityMap[trangThai] || 'secondary'
    },

    // Format date in Vietnam timezone (following PhieuGiamGia pattern)
    formatVietnamDateTime: () => (dateString) => {
      if (!dateString) return ''
      try {
        const date = new Date(dateString)
        if (isNaN(date.getTime())) return ''

        const options = {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          timeZone: 'Asia/Ho_Chi_Minh',
          hour12: false
        }
        return new Intl.DateTimeFormat('vi-VN', options).format(date)
      } catch (error) {
        console.error('Error formatting Vietnam date-time:', error)
        return ''
      }
    }
  },

  actions: {
    // Initialize toast for notifications
    initToast() {
      if (!this.toast) {
        this.toast = useToast()
      }
    },

    // Fetch all discounts with caching
    async fetchDiscounts(forceRefresh = false) {
      // Use cache if valid and not forcing refresh
      if (!forceRefresh && this.isCacheValid && this.discounts.length > 0) {
        return this.discounts
      }

      this.loading = true
      this.error = null

      try {
        const response = await discountService.getAllDiscounts()
        this.discounts = response || []
        this.lastFetch = Date.now()
        this.updateStatistics()

        // Cache individual discounts
        this.discounts.forEach(discount => {
          this.discountCache.set(discount.id, discount)
        })

        return this.discounts
      } catch (error) {
        this.error = error.message || 'Không thể tải danh sách đợt giảm giá'
        this.initToast()
        this.toast?.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: this.error,
          life: 5000
        })
        throw error
      } finally {
        this.loading = false
      }
    },

    // Fetch single discount by ID
    async fetchDiscountById(id) {
      // Check cache first
      const cached = this.getDiscountById(id)
      if (cached) {
        this.currentDiscount = cached
        return cached
      }

      this.loading = true
      this.error = null

      try {
        const response = await discountService.getDiscountById(id)
        this.currentDiscount = response
        this.discountCache.set(id, response)
        return response
      } catch (error) {
        this.error = error.message || 'Không thể tải thông tin đợt giảm giá'
        this.initToast()
        this.toast?.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: this.error,
          life: 5000
        })
        throw error
      } finally {
        this.loading = false
      }
    },

    // Create or update discount
    async saveDiscount(discountData) {
      this.saving = true
      this.error = null

      try {
        const response = await discountService.saveDiscount(discountData)

        // Update local state
        if (discountData.id) {
          // Update existing
          const index = this.discounts.findIndex(d => d.id === discountData.id)
          if (index !== -1) {
            this.discounts[index] = response
          }
        } else {
          // Add new
          this.discounts.unshift(response)
        }

        // Update cache
        this.discountCache.set(response.id, response)
        this.updateStatistics()

        this.initToast()
        this.toast?.add({
          severity: 'success',
          summary: 'Thành công',
          detail: discountData.id ? 'Cập nhật đợt giảm giá thành công' : 'Tạo đợt giảm giá thành công',
          life: 3000
        })

        return response
      } catch (error) {
        this.error = error.message || 'Không thể lưu đợt giảm giá'
        this.initToast()
        this.toast?.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: this.error,
          life: 5000
        })
        throw error
      } finally {
        this.saving = false
      }
    },

    // Delete single discount
    async deleteDiscount(id) {
      this.deleting = true
      this.error = null

      try {
        await discountService.deleteDiscount(id)

        // Remove from local state
        this.discounts = this.discounts.filter(d => d.id !== id)
        this.discountCache.delete(id)
        this.updateStatistics()

        this.initToast()
        this.toast?.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Xóa đợt giảm giá thành công',
          life: 3000
        })
      } catch (error) {
        this.error = error.message || 'Không thể xóa đợt giảm giá'
        this.initToast()
        this.toast?.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: this.error,
          life: 5000
        })
        throw error
      } finally {
        this.deleting = false
      }
    },

    // Batch delete discounts
    async deleteMultipleDiscounts(ids) {
      this.batchOperations.inProgress = true
      this.error = null

      try {
        await discountService.deleteDiscounts(ids)

        // Remove from local state
        this.discounts = this.discounts.filter(d => !ids.includes(d.id))
        ids.forEach(id => this.discountCache.delete(id))
        this.updateStatistics()

        this.batchOperations.results = {
          success: true,
          count: ids.length,
          message: `Đã xóa ${ids.length} đợt giảm giá`
        }

        this.initToast()
        this.toast?.add({
          severity: 'success',
          summary: 'Thành công',
          detail: this.batchOperations.results.message,
          life: 3000
        })
      } catch (error) {
        this.error = error.message || 'Không thể xóa các đợt giảm giá'
        this.batchOperations.results = {
          success: false,
          error: this.error
        }

        this.initToast()
        this.toast?.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: this.error,
          life: 5000
        })
        throw error
      } finally {
        this.batchOperations.inProgress = false
      }
    },

    // Fetch audit history for a discount
    async fetchAuditHistory(discountId) {
      this.loading = true
      this.error = null

      try {
        const response = await discountService.getDiscountAuditHistory(discountId)
        this.auditHistory[discountId] = response || []
        return this.auditHistory[discountId]
      } catch (error) {
        this.error = error.message || 'Không thể tải lịch sử thay đổi'
        this.initToast()
        this.toast?.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: this.error,
          life: 5000
        })
        return []
      } finally {
        this.loading = false
      }
    },

    // Update statistics
    updateStatistics() {
      this.statistics.total = this.discounts.length
      this.statistics.active = this.activeDiscounts.length
      this.statistics.upcoming = this.upcomingDiscounts.length
      this.statistics.expired = this.expiredDiscounts.length
      this.statistics.cancelled = this.cancelledDiscounts.length
    },

    // Filter management
    setFilters(newFilters) {
      this.filters = { ...this.filters, ...newFilters }
    },

    clearFilters() {
      this.filters = {
        search: '',
        status: null,
        dateRange: null,
        percentageRange: [0, 100]
      }
    },

    // Clear error state
    clearError() {
      this.error = null
    },

    // Clear cache
    clearCache() {
      this.discountCache.clear()
      this.lastFetch = null
    },

    // Reset store state
    resetState() {
      this.discounts = []
      this.currentDiscount = null
      this.loading = false
      this.saving = false
      this.deleting = false
      this.error = null
      this.clearCache()
      this.clearFilters()
      this.auditHistory = {}
      this.batchOperations = {
        inProgress: false,
        results: null
      }
      this.updateStatistics()
    },

    // Real-time update handlers
    handleCacheInvalidation(scope, entityId, requiresRefresh) {
      console.log(`🔄 DiscountStore: Cache invalidation received - scope: ${scope}, entityId: ${entityId}, requiresRefresh: ${requiresRefresh}`)

      this.realTimeState.cacheInvalidationCount++
      this.realTimeState.lastCacheInvalidation = new Date().toISOString()

      if (requiresRefresh) {
        if (scope === 'DISCOUNT_DATA') {
          if (entityId) {
            this.discountCache.delete(entityId)
          } else {
            // Clear all discount cache
            this.discountCache.clear()
            this.lastFetch = null
          }
        }
      }
    },

    handleDiscountUpdate(stateData) {
      console.log(`🎯 DiscountStore: Discount update received:`, stateData)

      const discountId = stateData.id
      if (!discountId) return

      // Update discount in discounts array
      const discountIndex = this.discounts.findIndex(d => d.id === discountId)
      if (discountIndex !== -1) {
        this.discounts[discountIndex] = { ...this.discounts[discountIndex], ...stateData }
      }

      // Update cached discount
      if (this.discountCache.has(discountId)) {
        const cachedDiscount = this.discountCache.get(discountId)
        this.discountCache.set(discountId, { ...cachedDiscount, ...stateData })
      }

      // Update current discount if it's the same
      if (this.currentDiscount && this.currentDiscount.id === discountId) {
        this.currentDiscount = { ...this.currentDiscount, ...stateData }
      }

      // Update statistics
      this.updateStatistics()

      // Sync with real-time system
      if (this.realTimeState.realTimeSync) {
        this.realTimeState.realTimeSync.syncStateData(stateData, { merge: true })
      }
    },

    // Force refresh discounts from API (for DataTable integration)
    async forceRefreshDiscounts() {
      console.log('🔄 DiscountStore: Force refreshing discounts for real-time update')
      return await this.fetchDiscounts(true)
    }
  }
})
