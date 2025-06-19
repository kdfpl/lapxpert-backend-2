import { defineStore } from 'pinia'
import userApi from '@/apis/user'
import { useRealTimeSync } from '@/composables/useRealTimeSync'
import { useToast } from 'primevue/usetoast'

export const useCustomerStore = defineStore('customer', {
  state: () => ({
    customers: [], // Ensure this is always initialized as an array
    loading: false,
    error: null,
    currentCustomer: null, // Add this to store the currently edited customer

    // Real-time state management
    realTimeState: {
      isConnected: false,
      lastSyncTime: null,
      cacheInvalidationCount: 0,
      lastCacheInvalidation: null,
      realTimeSync: null
    },

    // Cache management
    customerCache: new Map(),
    lastFetch: null,
    cacheTimeout: 5 * 60 * 1000 // 5 minutes
  }),

  // Initialize real-time sync when store is created
  $onAction({ name, store, args, after, onError }) {
    // Initialize real-time sync on first action
    if (!store.realTimeState.realTimeSync) {
      const toast = useToast()

      store.realTimeState.realTimeSync = useRealTimeSync({
        entityName: 'nguoiDung',
        storeKey: 'customerStore',
        enablePersistence: true,
        enableCrossTab: true,
        validateState: (state) => {
          if (!state || typeof state !== 'object') return false
          if (state.hoTen && typeof state.hoTen !== 'string') return false
          if (state.email && typeof state.email !== 'string') return false
          return true
        }
      })

      // Setup real-time sync event listeners
      store.realTimeState.realTimeSync.addEventListener('CACHE_INVALIDATION', (event) => {
        const { scope, entityId, requiresRefresh } = event.data

        if (scope === 'USER_DATA' || scope === 'CUSTOMER_DATA') {
          store.handleCacheInvalidation(scope, entityId, requiresRefresh)
        }
      })

      store.realTimeState.realTimeSync.addEventListener('WEBSOCKET_STATE_UPDATE', (event) => {
        const { stateData } = event.data
        if (stateData && (stateData.hoTen || stateData.email)) {
          store.handleCustomerUpdate(stateData)
        }
      })
    }
  },
  actions: {
    async fetchCustomers(params = {}) {
      this.loading = true
      try {
        console.log('CustomerStore: fetchCustomers called with params:', params)
        const response = await userApi.getCustomers(params)
        console.log('CustomerStore: API response:', response)

        // If this is a search request, return the filtered results directly
        if (params.search) {
          console.log('CustomerStore: Returning search results:', response.data)
          return response.data || []
        }

        // Otherwise, update the store's customers array
        this.customers = response.data || [] // Ensure we always have an array
        return this.customers
      } catch (error) {
        console.error('CustomerStore: Error in fetchCustomers:', error)
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchCustomerById(id) {
      this.loading = true
      try {
        const response = await userApi.getCustomerById(id)
        this.currentCustomer = response.data // Store the current customer separately
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async createCustomer(customerData) {
      this.loading = true
      try {
        const response = await userApi.createCustomer(customerData)
        this.customers.push(response.data)
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async updateCustomer(id, customerData) {
      this.loading = true
      try {
        const response = await userApi.updateCustomer(id, customerData)
        const index = this.customers.findIndex((c) => c.id === id)
        if (index !== -1) {
          this.customers[index] = response.data
        }
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async deleteCustomer(id) {
      this.loading = true
      try {
        await userApi.deleteCustomer(id)
        const index = this.customers.findIndex((c) => c.id === id)
        if (index !== -1) {
          this.customers[index].trangThai = false
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async restoreCustomer(id) {
      this.loading = true
      try {
        await userApi.restoreCustomer(id)
        const index = this.customers.findIndex((c) => c.id === id)
        if (index !== -1) {
          this.customers[index].trangThai = true
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    // Real-time update handlers
    handleCacheInvalidation(scope, entityId, requiresRefresh) {
      console.log(`🔄 CustomerStore: Cache invalidation received - scope: ${scope}, entityId: ${entityId}, requiresRefresh: ${requiresRefresh}`)

      this.realTimeState.cacheInvalidationCount++
      this.realTimeState.lastCacheInvalidation = new Date().toISOString()

      if (requiresRefresh) {
        if (scope === 'USER_DATA' || scope === 'CUSTOMER_DATA') {
          if (entityId) {
            this.customerCache.delete(entityId)
          } else {
            // Clear all customer cache
            this.customerCache.clear()
            this.lastFetch = null
          }
        }
      }
    },

    handleCustomerUpdate(stateData) {
      console.log(`👤 CustomerStore: Customer update received:`, stateData)

      const customerId = stateData.id
      if (!customerId) return

      // Update customer in customers array
      const customerIndex = this.customers.findIndex(c => c.id === customerId)
      if (customerIndex !== -1) {
        this.customers[customerIndex] = { ...this.customers[customerIndex], ...stateData }
      }

      // Update cached customer
      if (this.customerCache.has(customerId)) {
        const cachedCustomer = this.customerCache.get(customerId)
        this.customerCache.set(customerId, { ...cachedCustomer, ...stateData })
      }

      // Update current customer if it's the same
      if (this.currentCustomer && this.currentCustomer.id === customerId) {
        this.currentCustomer = { ...this.currentCustomer, ...stateData }
      }

      // Sync with real-time system
      if (this.realTimeState.realTimeSync) {
        this.realTimeState.realTimeSync.syncStateData(stateData, { merge: true })
      }
    },

    // Force refresh customers from API (for DataTable integration)
    async forceRefreshCustomers() {
      console.log('🔄 CustomerStore: Force refreshing customers for real-time update')
      return await this.fetchCustomers()
    },
  },
  getters: {
    activeCustomers: (state) => {
      if (!Array.isArray(state.customers)) return [] // Safety check
      return state.customers.filter((c) => c.trangThai === 'HOAT_DONG' || c.trangThai === true)
    },
    inactiveCustomers: (state) => {
      if (!Array.isArray(state.customers)) return [] // Safety check
      return state.customers.filter((c) => c.trangThai === 'KHONG_HOAT_DONG' || c.trangThai === false)
    },
  },
})
