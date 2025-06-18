import { ref, computed, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'

/**
 * useOptimisticUpdates - Enhanced Optimistic Update Composable
 * Provides optimistic update patterns with rollback mechanisms for better UX
 * Follows LapXpert patterns and Vietnamese business terminology
 */

/**
 * Create optimistic updates composable
 * @param {Object} options - Configuration options
 * @param {String} options.entityName - Vietnamese entity name for error messages
 * @param {Number} options.timeoutMs - Timeout for operations (default: 10000ms)
 * @param {Boolean} options.enableRetry - Enable automatic retry on failure
 * @param {Number} options.maxRetries - Maximum retry attempts (default: 3)
 * @returns {Object} Optimistic updates composable
 */
export function useOptimisticUpdates(options = {}) {
  const {
    entityName = 'dữ liệu',
    timeoutMs = 10000,
    enableRetry = true,
    maxRetries = 3
  } = options

  const toast = useToast()

  // Operation tracking
  const pendingOperations = ref(new Map())
  const operationHistory = ref([])
  const maxHistorySize = 50

  // Statistics
  const stats = ref({
    totalOperations: 0,
    successfulOperations: 0,
    failedOperations: 0,
    rolledBackOperations: 0,
    averageResponseTime: 0
  })

  /**
   * Generate unique operation ID
   * @returns {String} Unique operation ID
   */
  function generateOperationId() {
    return `opt_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
  }

  /**
   * Create operation context
   * @param {String} type - Operation type
   * @param {*} data - Operation data
   * @param {Function} optimisticUpdate - Function to apply optimistic update
   * @param {Function} rollbackUpdate - Function to rollback optimistic update
   * @returns {Object} Operation context
   */
  function createOperationContext(type, data, optimisticUpdate, rollbackUpdate) {
    return {
      id: generateOperationId(),
      type,
      data,
      optimisticUpdate,
      rollbackUpdate,
      startTime: Date.now(),
      retryCount: 0,
      status: 'pending'
    }
  }

  /**
   * Execute optimistic operation
   * @param {Object} context - Operation context
   * @param {Function} apiCall - API call function
   * @returns {Promise} Promise resolving to operation result
   */
  async function executeOptimisticOperation(context, apiCall) {
    const { id, optimisticUpdate, rollbackUpdate } = context
    
    // Track operation
    pendingOperations.value.set(id, context)
    stats.value.totalOperations++

    try {
      // Apply optimistic update immediately
      if (optimisticUpdate) {
        await optimisticUpdate()
      }

      // Set timeout for operation
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Operation timeout')), timeoutMs)
      })

      // Execute API call with timeout
      const result = await Promise.race([apiCall(), timeoutPromise])

      if (result.success) {
        // Operation successful
        context.status = 'success'
        context.endTime = Date.now()
        stats.value.successfulOperations++
        
        // Update average response time
        updateAverageResponseTime(context.endTime - context.startTime)

        // Show success notification
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đã cập nhật ${entityName} thành công`,
          life: 3000
        })

        return result
      } else {
        throw new Error(result.message || 'Operation failed')
      }
    } catch (error) {
      // Operation failed - handle retry or rollback
      return handleOperationFailure(context, apiCall, error)
    } finally {
      // Clean up operation tracking
      pendingOperations.value.delete(id)
      addToHistory(context)
    }
  }

  /**
   * Handle operation failure with retry logic
   * @param {Object} context - Operation context
   * @param {Function} apiCall - API call function
   * @param {Error} error - Error that occurred
   * @returns {Promise} Promise resolving to operation result
   */
  async function handleOperationFailure(context, apiCall, error) {
    const { id, rollbackUpdate, retryCount } = context

    // Check if retry is possible
    if (enableRetry && retryCount < maxRetries && !isNetworkError(error)) {
      context.retryCount++
      
      toast.add({
        severity: 'warn',
        summary: 'Đang thử lại',
        detail: `Thử lại thao tác ${entityName} (lần ${retryCount + 1}/${maxRetries})`,
        life: 2000
      })

      // Retry with exponential backoff
      const delay = Math.min(1000 * Math.pow(2, retryCount), 5000)
      await new Promise(resolve => setTimeout(resolve, delay))

      return executeOptimisticOperation(context, apiCall)
    }

    // Rollback optimistic update
    if (rollbackUpdate) {
      try {
        await rollbackUpdate()
        stats.value.rolledBackOperations++
      } catch (rollbackError) {
        console.error('❌ Failed to rollback optimistic update:', rollbackError)
      }
    }

    context.status = 'failed'
    context.error = error.message
    context.endTime = Date.now()
    stats.value.failedOperations++

    // Show error notification
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể cập nhật ${entityName}: ${error.message}`,
      life: 5000
    })

    throw error
  }

  /**
   * Check if error is a network error
   * @param {Error} error - Error to check
   * @returns {Boolean} True if network error
   */
  function isNetworkError(error) {
    return error.message.includes('timeout') ||
           error.message.includes('network') ||
           error.message.includes('fetch')
  }

  /**
   * Update average response time
   * @param {Number} responseTime - Response time in milliseconds
   */
  function updateAverageResponseTime(responseTime) {
    const currentAvg = stats.value.averageResponseTime
    const totalOps = stats.value.successfulOperations
    
    stats.value.averageResponseTime = totalOps === 1 
      ? responseTime 
      : (currentAvg * (totalOps - 1) + responseTime) / totalOps
  }

  /**
   * Add operation to history
   * @param {Object} context - Operation context
   */
  function addToHistory(context) {
    operationHistory.value.unshift({
      ...context,
      timestamp: new Date().toISOString()
    })

    // Limit history size
    if (operationHistory.value.length > maxHistorySize) {
      operationHistory.value = operationHistory.value.slice(0, maxHistorySize)
    }
  }

  /**
   * Create optimistic update function
   * @param {String} type - Operation type ('create', 'update', 'delete')
   * @param {*} data - Operation data
   * @param {Function} optimisticUpdate - Function to apply optimistic update
   * @param {Function} rollbackUpdate - Function to rollback optimistic update
   * @param {Function} apiCall - API call function
   * @returns {Function} Optimistic update function
   */
  function createOptimisticUpdate(type, data, optimisticUpdate, rollbackUpdate, apiCall) {
    return async () => {
      const context = createOperationContext(type, data, optimisticUpdate, rollbackUpdate)
      return executeOptimisticOperation(context, apiCall)
    }
  }

  /**
   * Batch optimistic operations
   * @param {Array} operations - Array of operation functions
   * @param {Object} options - Batch options
   * @returns {Promise} Promise resolving to batch results
   */
  async function batchOptimisticOperations(operations, batchOptions = {}) {
    const {
      concurrency = 3,
      stopOnFirstError = false
    } = batchOptions

    const results = []
    const errors = []

    // Execute operations in batches
    for (let i = 0; i < operations.length; i += concurrency) {
      const batch = operations.slice(i, i + concurrency)
      
      try {
        const batchResults = await Promise.allSettled(
          batch.map(operation => operation())
        )

        batchResults.forEach((result, index) => {
          if (result.status === 'fulfilled') {
            results.push(result.value)
          } else {
            errors.push({
              index: i + index,
              error: result.reason
            })
            
            if (stopOnFirstError) {
              throw result.reason
            }
          }
        })
      } catch (error) {
        if (stopOnFirstError) {
          throw error
        }
      }
    }

    return {
      results,
      errors,
      success: errors.length === 0
    }
  }

  /**
   * Cancel pending operation
   * @param {String} operationId - Operation ID to cancel
   * @returns {Boolean} True if operation was cancelled
   */
  function cancelOperation(operationId) {
    const operation = pendingOperations.value.get(operationId)
    
    if (operation) {
      operation.status = 'cancelled'
      
      // Rollback if possible
      if (operation.rollbackUpdate) {
        operation.rollbackUpdate()
        stats.value.rolledBackOperations++
      }
      
      pendingOperations.value.delete(operationId)
      addToHistory(operation)
      
      toast.add({
        severity: 'info',
        summary: 'Đã hủy',
        detail: `Đã hủy thao tác ${entityName}`,
        life: 3000
      })
      
      return true
    }
    
    return false
  }

  /**
   * Cancel all pending operations
   */
  function cancelAllOperations() {
    const operationIds = Array.from(pendingOperations.value.keys())
    operationIds.forEach(id => cancelOperation(id))
  }

  /**
   * Clear operation history
   */
  function clearHistory() {
    operationHistory.value = []
  }

  /**
   * Reset statistics
   */
  function resetStats() {
    stats.value = {
      totalOperations: 0,
      successfulOperations: 0,
      failedOperations: 0,
      rolledBackOperations: 0,
      averageResponseTime: 0
    }
  }

  // Computed properties
  const hasPendingOperations = computed(() => pendingOperations.value.size > 0)
  const pendingOperationsList = computed(() => Array.from(pendingOperations.value.values()))
  const successRate = computed(() => {
    const total = stats.value.totalOperations
    return total > 0 ? (stats.value.successfulOperations / total * 100).toFixed(2) : 0
  })

  // Cleanup on unmount
  onUnmounted(() => {
    cancelAllOperations()
  })

  return {
    // Core functions
    createOptimisticUpdate,
    executeOptimisticOperation,
    batchOptimisticOperations,

    // Operation management
    cancelOperation,
    cancelAllOperations,
    
    // State
    pendingOperations: pendingOperationsList,
    operationHistory: computed(() => operationHistory.value),
    stats: computed(() => stats.value),
    
    // Computed
    hasPendingOperations,
    successRate,
    
    // Utilities
    clearHistory,
    resetStats
  }
}
