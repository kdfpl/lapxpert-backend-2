import { defineStore } from 'pinia'
import productService from '@/apis/product'
import productDetailService from '@/apis/productdetail'
import { useRealTimeSync } from '@/composables/useRealTimeSync'
import { useToast } from 'primevue/usetoast'

export const useProductStore = defineStore('product', {
  state: () => ({
    products: [],
    activeProducts: [],
    activeProductsDetailed: [],
    productAuditHistory: {},
    productDetailAuditHistory: {},
    productCache: new Map(),
    lastFetch: null,
    cacheTimeout: 5 * 60 * 1000, // 5 minutes
    loading: false,
    error: null,
    filters: {
      tenSanPham: '',
      maSanPham: '',
      danhMuc: null,
      mauSac: null,
      thuongHieu: null,
      cpu: null,
      ram: null,
      gpu: null,
      priceRange: [0, 50000000], // Will be updated dynamically
      trangThai: null
    },
    serialNumbers: {},
    variantSerialNumbers: {},

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
      const { useRealTimeSync } = require('@/composables/useRealTimeSync')
      const { useToast } = require('primevue/usetoast')

      const toast = useToast()

      store.realTimeState.realTimeSync = useRealTimeSync({
        entityName: 'sanPham',
        storeKey: 'productStore',
        enablePersistence: true,
        enableCrossTab: true,
        validateState: (state) => {
          if (!state || typeof state !== 'object') return false
          if (state.tenSanPham && typeof state.tenSanPham !== 'string') return false
          if (state.gia && typeof state.gia !== 'number') return false
          return true
        }
      })

      // Setup real-time sync event listeners
      store.realTimeState.realTimeSync.addEventListener('CACHE_INVALIDATION', (event) => {
        const { scope, entityId, requiresRefresh } = event.data

        if (scope === 'PRICING_DATA' || scope === 'PRODUCT_DATA') {
          store.handleCacheInvalidation(scope, entityId, requiresRefresh)
        }
      })

      store.realTimeState.realTimeSync.addEventListener('WEBSOCKET_PRICE_UPDATE', (event) => {
        const { priceData } = event.data
        store.handlePriceUpdate(priceData)
      })

      store.realTimeState.realTimeSync.addEventListener('WEBSOCKET_STATE_UPDATE', (event) => {
        const { stateData } = event.data
        if (stateData && (stateData.tenSanPham || stateData.maSanPham)) {
          store.handleProductUpdate(stateData)
        }
      })
    }
  },

  getters: {
    // Status enum options for variants (Boolean)
    variantStatusOptions: () => [
      { label: 'Hoạt động', value: true, severity: 'success' },
      { label: 'Không hoạt động', value: false, severity: 'danger' }
    ],

    // Status enum options for serial numbers (String enum)
    serialStatusOptions: () => [
      { label: 'Có sẵn', value: 'AVAILABLE', severity: 'success' },
      { label: 'Đã đặt trước', value: 'RESERVED', severity: 'warning' },
      { label: 'Đã bán', value: 'SOLD', severity: 'info' },
      { label: 'Không có sẵn', value: 'UNAVAILABLE', severity: 'danger' }
    ],

    // Cache validation
    isCacheValid: (state) => {
      return state.lastFetch &&(Date.now() - state.lastFetch) < state.cacheTimeout
    },

    // Filtered products based on current filters
    filteredProducts: (state) => {
      return state.products.filter(product => {
        if (state.filters.tenSanPham &&
            !product.tenSanPham.toLowerCase().includes(state.filters.tenSanPham.toLowerCase())) {
          return false
        }

        if (state.filters.danhMuc && product.danhMuc?.id !== state.filters.danhMuc) {
          return false
        }

        if (state.filters.mauSac &&
            !product.sanPhamChiTiets?.some(detail => detail.mauSac?.id === state.filters.mauSac)) {
          return false
        }

        if (state.filters.thuongHieu && product.thuongHieu?.id !== state.filters.thuongHieu) {
          return false
        }

        if (state.filters.trangThai !== null && product.trangThai !== state.filters.trangThai) {
          return false
        }

        return true
      })
    }
  },
  actions: {
    async fetchProducts(forceRefresh = false) {
      if (!forceRefresh && this.isCacheValid && this.products.length > 0) {
        return this.products
      }

      this.loading = true
      try {
        // Use getActiveProducts instead of getAllProducts to get promotional pricing
        this.products = await productService.getActiveProducts()
        this.lastFetch = Date.now()

        // Cache individual products
        this.products.forEach(product => {
          this.productCache.set(product.id, product)
        })

        return this.products
      } catch (error) {
        this.error = error
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchActiveProducts() {
      this.loading = true
      try {
        this.activeProducts = await productService.getActiveProducts()
      } catch (error) {
        this.error = error
      } finally {
        this.loading = false
      }
    },

    async fetchActiveProductsDetailed() {
      this.loading = true
      try {
        this.activeProductsDetailed = await productDetailService.getActiveProductsDetailed()
      } catch (error) {
        this.error = error
      } finally {
        this.loading = false
      }
    },

    async fetchProductById(id) {
      this.loading = true
      try {
        const product = await productService.getProductById(id)
        this.productCache.set(id, product)
        return product
      } catch (error) {
        this.error = error
        throw error
      } finally {
        this.loading = false
      }
    },

    // NEW: Audit trail methods
    async fetchProductAuditHistory(productId) {
      try {
        const history = await productService.getProductAuditHistory(productId)
        this.productAuditHistory[productId] = history
        return history
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async fetchProductDetailAuditHistory(productDetailId) {
      try {
        const history = await productService.getProductDetailAuditHistory(productDetailId)
        this.productDetailAuditHistory[productDetailId] = history
        return history
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // NEW: Status management methods
    async updateProductStatus(id, status, reason) {
      try {
        const updatedProduct = await productService.updateProductStatus(id, status, reason)
        // Update local state
        const index = this.products.findIndex(p => p.id === id)
        if (index !== -1) {
          this.products[index] = updatedProduct
        }
        this.productCache.set(id, updatedProduct)
        return updatedProduct
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async updateProductDetailStatus(id, status, reason) {
      try {
        const updatedDetail = await productService.updateProductDetailStatus(id, status, reason)

        // Update activeProductsDetailed array
        const index = this.activeProductsDetailed.findIndex(pd => pd.id === id)
        if (index !== -1) {
          this.activeProductsDetailed[index] = updatedDetail
        }

        // CRITICAL FIX: Also update main products array and cache for status changes
        const parentProductId = updatedDetail.sanPham?.id
        if (parentProductId) {
          // Update the variant in the main products array
          const productIndex = this.products.findIndex(p => p.id === parentProductId)
          if (productIndex !== -1) {
            const variantIndex = this.products[productIndex].sanPhamChiTiets?.findIndex(v => v.id === id)
            if (variantIndex !== -1) {
              this.products[productIndex].sanPhamChiTiets[variantIndex] = updatedDetail
            }
          }

          // Update the cached product
          const cachedProduct = this.productCache.get(parentProductId)
          if (cachedProduct) {
            const cachedVariantIndex = cachedProduct.sanPhamChiTiets?.findIndex(v => v.id === id)
            if (cachedVariantIndex !== -1) {
              cachedProduct.sanPhamChiTiets[cachedVariantIndex] = updatedDetail
              this.productCache.set(parentProductId, cachedProduct)
            }
          }
        }

        return updatedDetail
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // NEW: Batch operations
    async updateMultipleProductStatus(productIds, status, reason) {
      try {
        const result = await productService.updateMultipleProductStatus(productIds, status, reason)
        // Don't refresh automatically - let the component handle it to avoid UI flicker
        return result
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async updateMultipleProductDetailStatus(productDetailIds, status, reason) {
      try {
        const result = await productService.updateMultipleProductDetailStatus(productDetailIds, status, reason)
        // Refresh product details to get updated data
        await this.fetchActiveProductsDetailed()
        return result
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // NEW: CRUD operations
    async createProduct(productData) {
      try {
        const newProduct = await productService.addProduct(productData)
        this.products.unshift(newProduct)
        this.productCache.set(newProduct.id, newProduct)
        return newProduct
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async createProductWithVariants(productData) {
      try {
        const result = await productService.addProduct(productData)
        await this.fetchProducts(true) // Refresh to get complete data
        return result
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async updateProduct(id, productData) {
      try {
        const updatedProduct = await productService.updateProduct(id, productData)
        const index = this.products.findIndex(p => p.id === id)
        if (index !== -1) {
          this.products[index] = updatedProduct
        }
        this.productCache.set(id, updatedProduct)
        return updatedProduct
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async updateProductWithVariants(id, productData) {
      try {
        const updatedProduct = await productService.updateProductWithVariants(id, productData)
        const index = this.products.findIndex(p => p.id === id)
        if (index !== -1) {
          this.products[index] = updatedProduct
        }
        this.productCache.set(id, updatedProduct)
        // Refresh to get complete data including variants
        await this.fetchProducts(true)
        return updatedProduct
      } catch (error) {
        this.error = error
        throw error
      }
    },



    async deleteProduct(id) {
      try {
        await productService.softDeleteProduct(id)
        // Remove from local state
        this.products = this.products.filter(p => p.id !== id)
        this.productCache.delete(id)
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // NEW: Product detail CRUD
    async createProductDetail(productDetailData) {
      try {
        const newDetail = await productDetailService.addProductDetailed(productDetailData)
        this.activeProductsDetailed.unshift(newDetail)

        // CONSISTENCY FIX: Also update main products array and cache when creating new variants
        const parentProductId = newDetail.sanPham?.id
        if (parentProductId) {
          // Add the new variant to the main products array
          const productIndex = this.products.findIndex(p => p.id === parentProductId)
          if (productIndex !== -1) {
            if (!this.products[productIndex].sanPhamChiTiets) {
              this.products[productIndex].sanPhamChiTiets = []
            }
            this.products[productIndex].sanPhamChiTiets.push(newDetail)
          }

          // Add the new variant to the cached product
          const cachedProduct = this.productCache.get(parentProductId)
          if (cachedProduct) {
            if (!cachedProduct.sanPhamChiTiets) {
              cachedProduct.sanPhamChiTiets = []
            }
            cachedProduct.sanPhamChiTiets.push(newDetail)
            this.productCache.set(parentProductId, cachedProduct)
          }
        }

        return newDetail
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async updateProductDetail(id, productDetailData) {
      try {
        const updatedDetail = await productDetailService.updateProductDetailed(id, productDetailData)

        // Update activeProductsDetailed array
        const index = this.activeProductsDetailed.findIndex(pd => pd.id === id)
        if (index !== -1) {
          this.activeProductsDetailed[index] = updatedDetail
        }

        // CRITICAL FIX: Update the main products array and cache
        // Find the parent product that contains this variant
        const parentProductId = updatedDetail.sanPham?.id
        if (parentProductId) {
          console.log(`ProductStore: Updating variant ${id} for product ${parentProductId}`)

          // Update the variant in the main products array
          const productIndex = this.products.findIndex(p => p.id === parentProductId)
          if (productIndex !== -1) {
            const variantIndex = this.products[productIndex].sanPhamChiTiets?.findIndex(v => v.id === id)
            if (variantIndex !== -1) {
              console.log(`ProductStore: Updated variant in products array at index ${variantIndex}`)
              this.products[productIndex].sanPhamChiTiets[variantIndex] = updatedDetail
            }
          }

          // Update the cached product
          const cachedProduct = this.productCache.get(parentProductId)
          if (cachedProduct) {
            const cachedVariantIndex = cachedProduct.sanPhamChiTiets?.findIndex(v => v.id === id)
            if (cachedVariantIndex !== -1) {
              console.log(`ProductStore: Updated variant in cache at index ${cachedVariantIndex}`)
              cachedProduct.sanPhamChiTiets[cachedVariantIndex] = updatedDetail
              this.productCache.set(parentProductId, cachedProduct)
            }
          }
        } else {
          console.warn(`ProductStore: No parent product ID found for variant ${id}`)
        }

        return updatedDetail
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async deleteProductDetail(id) {
      try {
        await productDetailService.softDeleteProductDetailed(id)
        this.activeProductsDetailed = this.activeProductsDetailed.filter(pd => pd.id !== id)
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // NEW: Cache management
    getCachedProduct(id) {
      return this.productCache.get(id)
    },

    setCachedProduct(id, product) {
      this.productCache.set(id, product)
    },

    clearCache() {
      this.productCache.clear()
      this.lastFetch = null
    },

    // NEW: Force refresh a specific product from API
    async forceRefreshProduct(id) {
      try {
        const freshProduct = await productService.getProductById(id)

        // Update the main products array
        const index = this.products.findIndex(p => p.id == id)
        if (index !== -1) {
          this.products[index] = freshProduct
        }

        // Update the cache
        this.productCache.set(id, freshProduct)

        return freshProduct
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // NEW: Filter management
    updateFilters(newFilters) {
      this.filters = { ...this.filters, ...newFilters }
    },

    clearFilters() {
      // Note: This method is kept for compatibility but components should use
      // the useDynamicPricing composable for dynamic price ranges
      this.filters = {
        tenSanPham: '',
        maSanPham: '',
        danhMuc: null,
        mauSac: null,
        thuongHieu: null,
        cpu: null,
        ram: null,
        gpu: null,
        priceRange: [0, 50000000], // Components using useDynamicPricing will override this
        trangThai: null
      }
    },

    // NEW: Search functionality
    async searchProducts(searchFilters) {
      try {
        return await productService.searchProducts(searchFilters)
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // NEW: Serial number management
    async fetchSerialNumbers(variantId) {
      try {
        const serialNumbers = await productService.getSerialNumbers(variantId)
        this.variantSerialNumbers[variantId] = serialNumbers
        return serialNumbers
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async createSerialNumber(variantId, serialData) {
      try {
        const newSerial = await productService.createSerialNumber(variantId, serialData)
        if (!this.variantSerialNumbers[variantId]) {
          this.variantSerialNumbers[variantId] = []
        }
        this.variantSerialNumbers[variantId].push(newSerial)
        return newSerial
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async updateSerialNumber(serialId, serialData) {
      try {
        const updatedSerial = await productService.updateSerialNumber(serialId, serialData)
        // Update in local state
        Object.keys(this.variantSerialNumbers).forEach(variantId => {
          const index = this.variantSerialNumbers[variantId].findIndex(s => s.id === serialId)
          if (index !== -1) {
            this.variantSerialNumbers[variantId][index] = updatedSerial
          }
        })
        return updatedSerial
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async deleteSerialNumber(serialId) {
      try {
        await productService.deleteSerialNumber(serialId)
        // Remove from local state
        Object.keys(this.variantSerialNumbers).forEach(variantId => {
          this.variantSerialNumbers[variantId] = this.variantSerialNumbers[variantId].filter(s => s.id !== serialId)
        })
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async bulkCreateSerialNumbers(variantId, serialNumbers) {
      try {
        const result = await productService.bulkCreateSerialNumbers(variantId, serialNumbers)
        // Refresh serial numbers for this variant
        await this.fetchSerialNumbers(variantId)
        return result
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async updateSerialNumberStatus(serialId, status, reason) {
      try {
        const updatedSerial = await productService.updateSerialNumberStatus(serialId, status, reason)
        // Update in local state
        Object.keys(this.variantSerialNumbers).forEach(variantId => {
          const index = this.variantSerialNumbers[variantId].findIndex(s => s.id === serialId)
          if (index !== -1) {
            this.variantSerialNumbers[variantId][index] = updatedSerial
          }
        })
        return updatedSerial
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // NEW: Import/Export functionality
    async importProducts(file, options = {}) {
      try {
        const result = await productService.importProducts(file, options)
        // Refresh products after import
        await this.fetchProducts(true)
        return result
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async exportProducts(filters = {}) {
      try {
        return await productService.exportProducts(filters)
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async importSerialNumbers(variantId, file, options = {}) {
      try {
        const result = await productService.importSerialNumbers(variantId, file, options)
        // Refresh serial numbers for this variant
        await this.fetchSerialNumbers(variantId)
        return result
      } catch (error) {
        this.error = error
        throw error
      }
    },

    async exportSerialNumbers(variantId) {
      try {
        return await productService.exportSerialNumbers(variantId)
      } catch (error) {
        this.error = error
        throw error
      }
    },

    // Real-time update handlers
    handleCacheInvalidation(scope, entityId, requiresRefresh) {
      console.log(`🔄 ProductStore: Cache invalidation received - scope: ${scope}, entityId: ${entityId}, requiresRefresh: ${requiresRefresh}`)

      this.realTimeState.cacheInvalidationCount++
      this.realTimeState.lastCacheInvalidation = new Date().toISOString()

      if (requiresRefresh) {
        // Invalidate relevant cache entries
        if (scope === 'PRODUCT_DATA') {
          if (entityId) {
            this.productCache.delete(entityId)
          } else {
            // Clear all product cache
            this.productCache.clear()
            this.lastFetch = null
          }
        } else if (scope === 'PRICING_DATA') {
          // For pricing updates, we might need to refresh specific products
          if (entityId) {
            // Find and refresh the product containing this variant
            const product = this.products.find(p =>
              p.sanPhamChiTiets?.some(variant => variant.id === entityId)
            )
            if (product) {
              this.productCache.delete(product.id)
            }
          }
        }
      }
    },

    handlePriceUpdate(priceData) {
      console.log(`💰 ProductStore: Price update received:`, priceData)

      const variantId = priceData?.variantId || priceData?.sanPhamChiTietId
      if (!variantId) return

      // Update price in products array
      this.products.forEach(product => {
        const variantIndex = product.sanPhamChiTiets?.findIndex(v => v.id === variantId)
        if (variantIndex !== -1) {
          product.sanPhamChiTiets[variantIndex].gia = priceData.newPrice || priceData.gia
          product.sanPhamChiTiets[variantIndex].giaKhuyenMai = priceData.promotionalPrice || priceData.giaKhuyenMai
        }
      })

      // Update price in activeProductsDetailed array
      const detailIndex = this.activeProductsDetailed.findIndex(detail => detail.id === variantId)
      if (detailIndex !== -1) {
        this.activeProductsDetailed[detailIndex].gia = priceData.newPrice || priceData.gia
        this.activeProductsDetailed[detailIndex].giaKhuyenMai = priceData.promotionalPrice || priceData.giaKhuyenMai
      }

      // Update cached products
      this.productCache.forEach((product, productId) => {
        const variantIndex = product.sanPhamChiTiets?.findIndex(v => v.id === variantId)
        if (variantIndex !== -1) {
          product.sanPhamChiTiets[variantIndex].gia = priceData.newPrice || priceData.gia
          product.sanPhamChiTiets[variantIndex].giaKhuyenMai = priceData.promotionalPrice || priceData.giaKhuyenMai
          this.productCache.set(productId, product)
        }
      })
    },

    handleProductUpdate(stateData) {
      console.log(`📦 ProductStore: Product update received:`, stateData)

      const productId = stateData.id
      if (!productId) return

      // Update product in products array
      const productIndex = this.products.findIndex(p => p.id === productId)
      if (productIndex !== -1) {
        this.products[productIndex] = { ...this.products[productIndex], ...stateData }
      }

      // Update cached product
      if (this.productCache.has(productId)) {
        const cachedProduct = this.productCache.get(productId)
        this.productCache.set(productId, { ...cachedProduct, ...stateData })
      }

      // Sync with real-time system
      if (this.realTimeState.realTimeSync) {
        this.realTimeState.realTimeSync.syncStateData(stateData, { merge: true })
      }
    },

    // Force refresh products from API (for DataTable integration)
    async forceRefreshProducts() {
      console.log('🔄 ProductStore: Force refreshing products for real-time update')
      return await this.fetchProducts(true)
    }
  }
})
