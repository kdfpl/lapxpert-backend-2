import { ref, onUnmounted } from 'vue'

/**
 * Performance optimization composable
 * Provides debouncing, throttling, and request deduplication
 */

export function usePerformanceOptimization() {
  // Active timeouts for cleanup
  const activeTimeouts = ref(new Set())
  
  // Active requests for deduplication
  const activeRequests = ref(new Map())

  /**
   * Debounce function calls
   * @param {Function} func - Function to debounce
   * @param {number} delay - Delay in milliseconds
   * @param {boolean} immediate - Execute immediately on first call
   * @returns {Function} Debounced function
   */
  const debounce = (func, delay = 300, immediate = false) => {
    let timeoutId = null
    
    return function debounced(...args) {
      const callNow = immediate && !timeoutId
      
      if (timeoutId) {
        clearTimeout(timeoutId)
        activeTimeouts.value.delete(timeoutId)
      }
      
      timeoutId = setTimeout(() => {
        timeoutId = null
        activeTimeouts.value.delete(timeoutId)
        if (!immediate) func.apply(this, args)
      }, delay)
      
      activeTimeouts.value.add(timeoutId)
      
      if (callNow) func.apply(this, args)
    }
  }

  /**
   * Throttle function calls
   * @param {Function} func - Function to throttle
   * @param {number} limit - Time limit in milliseconds
   * @returns {Function} Throttled function
   */
  const throttle = (func, limit = 300) => {
    let inThrottle = false
    
    return function throttled(...args) {
      if (!inThrottle) {
        func.apply(this, args)
        inThrottle = true
        
        const timeoutId = setTimeout(() => {
          inThrottle = false
          activeTimeouts.value.delete(timeoutId)
        }, limit)
        
        activeTimeouts.value.add(timeoutId)
      }
    }
  }

  /**
   * Create a debounced search function
   * @param {Function} searchFunction - Search function to debounce
   * @param {number} delay - Debounce delay
   * @returns {Function} Debounced search function
   */
  const createDebouncedSearch = (searchFunction, delay = 300) => {
    return debounce(async (query, ...args) => {
      if (!query || query.trim().length === 0) {
        return { success: true, data: [] }
      }
      
      if (query.trim().length < 2) {
        return { success: true, data: [] }
      }
      
      return await searchFunction(query.trim(), ...args)
    }, delay)
  }

  /**
   * Request deduplication
   * Prevents multiple identical requests from being made simultaneously
   * @param {string} key - Unique key for the request
   * @param {Function} requestFunction - Function that returns a Promise
   * @returns {Promise} The request promise
   */
  const deduplicateRequest = async (key, requestFunction) => {
    // If request is already in progress, return the existing promise
    if (activeRequests.value.has(key)) {
      return activeRequests.value.get(key)
    }
    
    // Create new request promise
    const requestPromise = requestFunction()
      .finally(() => {
        // Clean up after request completes
        activeRequests.value.delete(key)
      })
    
    // Store the promise for deduplication
    activeRequests.value.set(key, requestPromise)
    
    return requestPromise
  }

  /**
   * Batch multiple operations together
   * @param {Array} operations - Array of operation functions
   * @param {number} batchSize - Size of each batch
   * @param {number} delay - Delay between batches
   * @returns {Promise} Promise that resolves when all operations complete
   */
  const batchOperations = async (operations, batchSize = 5, delay = 100) => {
    const results = []
    
    for (let i = 0; i < operations.length; i += batchSize) {
      const batch = operations.slice(i, i + batchSize)
      const batchPromises = batch.map(op => 
        typeof op === 'function' ? op() : op
      )
      
      const batchResults = await Promise.allSettled(batchPromises)
      results.push(...batchResults)
      
      // Add delay between batches to prevent overwhelming the server
      if (i + batchSize < operations.length && delay > 0) {
        await new Promise(resolve => {
          const timeoutId = setTimeout(() => {
            activeTimeouts.value.delete(timeoutId)
            resolve()
          }, delay)
          activeTimeouts.value.add(timeoutId)
        })
      }
    }
    
    return results
  }

  /**
   * Lazy loading with intersection observer
   * @param {Function} loadFunction - Function to call when element is visible
   * @param {Object} options - Intersection observer options
   * @returns {Object} Observer and cleanup function
   */
  const createLazyLoader = (loadFunction, options = {}) => {
    const defaultOptions = {
      root: null,
      rootMargin: '50px',
      threshold: 0.1,
      ...options
    }
    
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          loadFunction(entry.target)
          observer.unobserve(entry.target)
        }
      })
    }, defaultOptions)
    
    return {
      observer,
      observe: (element) => observer.observe(element),
      unobserve: (element) => observer.unobserve(element),
      disconnect: () => observer.disconnect()
    }
  }

  /**
   * Memory-efficient data processing
   * Process large datasets in chunks to prevent memory issues
   * @param {Array} data - Data to process
   * @param {Function} processor - Processing function
   * @param {number} chunkSize - Size of each chunk
   * @returns {Promise<Array>} Processed results
   */
  const processInChunks = async (data, processor, chunkSize = 100) => {
    const results = []
    
    for (let i = 0; i < data.length; i += chunkSize) {
      const chunk = data.slice(i, i + chunkSize)
      const chunkResults = await processor(chunk)
      results.push(...chunkResults)
      
      // Allow other tasks to run
      await new Promise(resolve => {
        const timeoutId = setTimeout(() => {
          activeTimeouts.value.delete(timeoutId)
          resolve()
        }, 0)
        activeTimeouts.value.add(timeoutId)
      })
    }
    
    return results
  }

  /**
   * Optimized filter function for large datasets
   * @param {Array} data - Data to filter
   * @param {Function} filterFn - Filter function
   * @param {number} chunkSize - Processing chunk size
   * @returns {Promise<Array>} Filtered results
   */
  const optimizedFilter = async (data, filterFn, chunkSize = 1000) => {
    return processInChunks(data, (chunk) => {
      return Promise.resolve(chunk.filter(filterFn))
    }, chunkSize)
  }

  /**
   * Virtual scrolling helper
   * Calculate visible items for virtual scrolling
   * @param {number} scrollTop - Current scroll position
   * @param {number} itemHeight - Height of each item
   * @param {number} containerHeight - Height of container
   * @param {number} totalItems - Total number of items
   * @param {number} buffer - Buffer items to render outside viewport
   * @returns {Object} Visible range and offset
   */
  const calculateVirtualScrolling = (
    scrollTop, 
    itemHeight, 
    containerHeight, 
    totalItems, 
    buffer = 5
  ) => {
    const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - buffer)
    const endIndex = Math.min(
      totalItems - 1,
      Math.ceil((scrollTop + containerHeight) / itemHeight) + buffer
    )
    
    const visibleItems = endIndex - startIndex + 1
    const offsetY = startIndex * itemHeight
    const totalHeight = totalItems * itemHeight
    
    return {
      startIndex,
      endIndex,
      visibleItems,
      offsetY,
      totalHeight
    }
  }

  /**
   * Performance monitoring
   * @param {string} name - Performance mark name
   * @returns {Function} Function to end measurement
   */
  const measurePerformance = (name) => {
    const startMark = `${name}-start`
    const endMark = `${name}-end`
    const measureName = `${name}-duration`
    
    performance.mark(startMark)
    
    return () => {
      performance.mark(endMark)
      performance.measure(measureName, startMark, endMark)
      
      const measure = performance.getEntriesByName(measureName)[0]
      console.log(`Performance: ${name} took ${measure.duration.toFixed(2)}ms`)
      
      // Clean up marks
      performance.clearMarks(startMark)
      performance.clearMarks(endMark)
      performance.clearMeasures(measureName)
      
      return measure.duration
    }
  }

  /**
   * Cleanup function
   */
  const cleanup = () => {
    // Clear all active timeouts
    activeTimeouts.value.forEach(timeoutId => {
      clearTimeout(timeoutId)
    })
    activeTimeouts.value.clear()
    
    // Clear active requests
    activeRequests.value.clear()
  }

  // Cleanup on unmount
  onUnmounted(cleanup)

  return {
    // Core optimization functions
    debounce,
    throttle,
    createDebouncedSearch,
    deduplicateRequest,
    batchOperations,
    
    // Data processing
    processInChunks,
    optimizedFilter,
    
    // UI optimization
    createLazyLoader,
    calculateVirtualScrolling,
    
    // Performance monitoring
    measurePerformance,
    
    // Cleanup
    cleanup,
    
    // State
    activeRequests: activeRequests.value,
    activeTimeouts: activeTimeouts.value
  }
}
