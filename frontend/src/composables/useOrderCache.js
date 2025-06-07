import { ref, computed } from 'vue'

/**
 * Intelligent caching composable for order data
 * Provides caching with TTL, cache invalidation, and memory management
 */

// Global cache state
const orderCache = ref(new Map())
const cacheMetadata = ref(new Map())
const maxCacheSize = 100 // Maximum number of cached orders
const defaultTTL = 5 * 60 * 1000 // 5 minutes in milliseconds

export function useOrderCache() {
  // Cache statistics
  const cacheStats = ref({
    hits: 0,
    misses: 0,
    evictions: 0,
    size: 0
  })

  // Computed cache hit ratio
  const hitRatio = computed(() => {
    const total = cacheStats.value.hits + cacheStats.value.misses
    return total > 0 ? (cacheStats.value.hits / total * 100).toFixed(2) : 0
  })

  /**
   * Generate cache key for order data
   */
  const generateCacheKey = (type, params = {}) => {
    const sortedParams = Object.keys(params)
      .sort()
      .reduce((result, key) => {
        result[key] = params[key]
        return result
      }, {})
    
    return `${type}:${JSON.stringify(sortedParams)}`
  }

  /**
   * Check if cache entry is valid (not expired)
   */
  const isCacheValid = (key) => {
    const metadata = cacheMetadata.value.get(key)
    if (!metadata) return false
    
    const now = Date.now()
    return now < metadata.expiresAt
  }

  /**
   * Evict expired entries from cache
   */
  const evictExpiredEntries = () => {
    const now = Date.now()
    const expiredKeys = []
    
    for (const [key, metadata] of cacheMetadata.value.entries()) {
      if (now >= metadata.expiresAt) {
        expiredKeys.push(key)
      }
    }
    
    expiredKeys.forEach(key => {
      orderCache.value.delete(key)
      cacheMetadata.value.delete(key)
      cacheStats.value.evictions++
    })
    
    updateCacheSize()
  }

  /**
   * Evict least recently used entries if cache is full
   */
  const evictLRU = () => {
    if (orderCache.value.size <= maxCacheSize) return
    
    // Sort by last accessed time
    const entries = Array.from(cacheMetadata.value.entries())
      .sort((a, b) => a[1].lastAccessed - b[1].lastAccessed)
    
    // Remove oldest entries
    const toRemove = entries.slice(0, orderCache.value.size - maxCacheSize)
    toRemove.forEach(([key]) => {
      orderCache.value.delete(key)
      cacheMetadata.value.delete(key)
      cacheStats.value.evictions++
    })
    
    updateCacheSize()
  }

  /**
   * Update cache size statistics
   */
  const updateCacheSize = () => {
    cacheStats.value.size = orderCache.value.size
  }

  /**
   * Get data from cache
   */
  const get = (key) => {
    // Clean up expired entries periodically
    if (Math.random() < 0.1) { // 10% chance
      evictExpiredEntries()
    }
    
    if (!orderCache.value.has(key) || !isCacheValid(key)) {
      cacheStats.value.misses++
      return null
    }
    
    // Update last accessed time
    const metadata = cacheMetadata.value.get(key)
    metadata.lastAccessed = Date.now()
    cacheMetadata.value.set(key, metadata)
    
    cacheStats.value.hits++
    return orderCache.value.get(key)
  }

  /**
   * Set data in cache
   */
  const set = (key, data, ttl = defaultTTL) => {
    // Evict LRU entries if cache is full
    evictLRU()
    
    const now = Date.now()
    const metadata = {
      createdAt: now,
      lastAccessed: now,
      expiresAt: now + ttl,
      size: JSON.stringify(data).length // Approximate size
    }
    
    orderCache.value.set(key, data)
    cacheMetadata.value.set(key, metadata)
    updateCacheSize()
  }

  /**
   * Remove specific entry from cache
   */
  const remove = (key) => {
    const removed = orderCache.value.delete(key)
    cacheMetadata.value.delete(key)
    if (removed) {
      updateCacheSize()
    }
    return removed
  }

  /**
   * Clear all cache entries
   */
  const clear = () => {
    orderCache.value.clear()
    cacheMetadata.value.clear()
    cacheStats.value.evictions += cacheStats.value.size
    updateCacheSize()
  }

  /**
   * Invalidate cache entries by pattern
   */
  const invalidateByPattern = (pattern) => {
    const regex = new RegExp(pattern)
    const keysToRemove = []
    
    for (const key of orderCache.value.keys()) {
      if (regex.test(key)) {
        keysToRemove.push(key)
      }
    }
    
    keysToRemove.forEach(key => remove(key))
    return keysToRemove.length
  }

  /**
   * Get cache information for debugging
   */
  const getCacheInfo = () => {
    const entries = Array.from(cacheMetadata.value.entries()).map(([key, metadata]) => ({
      key,
      ...metadata,
      isExpired: Date.now() >= metadata.expiresAt
    }))
    
    return {
      stats: cacheStats.value,
      hitRatio: hitRatio.value,
      entries,
      totalSize: entries.reduce((sum, entry) => sum + entry.size, 0)
    }
  }

  /**
   * Cached order list getter
   */
  const getCachedOrderList = (params = {}) => {
    const key = generateCacheKey('orderList', params)
    return get(key)
  }

  /**
   * Cached order list setter
   */
  const setCachedOrderList = (params = {}, data, ttl) => {
    const key = generateCacheKey('orderList', params)
    set(key, data, ttl)
  }

  /**
   * Cached order detail getter
   */
  const getCachedOrderDetail = (orderId) => {
    const key = generateCacheKey('orderDetail', { id: orderId })
    return get(key)
  }

  /**
   * Cached order detail setter
   */
  const setCachedOrderDetail = (orderId, data, ttl) => {
    const key = generateCacheKey('orderDetail', { id: orderId })
    set(key, data, ttl)
  }

  /**
   * Invalidate order-related cache when order is updated
   */
  const invalidateOrderCache = (orderId) => {
    // Remove specific order detail
    remove(generateCacheKey('orderDetail', { id: orderId }))
    
    // Invalidate all order lists (they might contain the updated order)
    return invalidateByPattern('^orderList:')
  }

  /**
   * Preload frequently accessed orders
   */
  const preloadOrders = async (orderIds, fetchFunction) => {
    const uncachedIds = orderIds.filter(id => !getCachedOrderDetail(id))
    
    if (uncachedIds.length === 0) return
    
    try {
      // Fetch uncached orders in batches
      const batchSize = 10
      for (let i = 0; i < uncachedIds.length; i += batchSize) {
        const batch = uncachedIds.slice(i, i + batchSize)
        const promises = batch.map(id => fetchFunction(id))
        const results = await Promise.allSettled(promises)
        
        results.forEach((result, index) => {
          if (result.status === 'fulfilled' && result.value.success) {
            setCachedOrderDetail(batch[index], result.value.data)
          }
        })
      }
    } catch (error) {
      console.warn('Error preloading orders:', error)
    }
  }

  return {
    // Cache operations
    get,
    set,
    remove,
    clear,
    invalidateByPattern,
    
    // Order-specific cache operations
    getCachedOrderList,
    setCachedOrderList,
    getCachedOrderDetail,
    setCachedOrderDetail,
    invalidateOrderCache,
    preloadOrders,
    
    // Cache management
    generateCacheKey,
    evictExpiredEntries,
    getCacheInfo,
    
    // Statistics
    cacheStats: computed(() => cacheStats.value),
    hitRatio
  }
}
