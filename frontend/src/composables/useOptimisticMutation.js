import { ref, computed, nextTick } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useRealTimeSync } from './useRealTimeSync'
import { createConflictResolver, RESOLUTION_STRATEGIES } from '@/utils/StateConflictResolver'

/**
 * useOptimisticMutation - Enhanced Optimistic UI Updates Composable
 * Provides granular control over optimistic mutations with real-time sync integration,
 * advanced rollback mechanisms, and conflict resolution
 * Follows LapXpert patterns and Vietnamese business terminology
 */

/**
 * Mutation types
 */
export const MUTATION_TYPES = {
  CREATE: 'create',
  UPDATE: 'update',
  DELETE: 'delete',
  BATCH: 'batch',
  CUSTOM: 'custom'
}

/**
 * Mutation states
 */
export const MUTATION_STATES = {
  PENDING: 'pending',
  OPTIMISTIC: 'optimistic',
  CONFIRMED: 'confirmed',
  FAILED: 'failed',
  ROLLED_BACK: 'rolled_back'
}

/**
 * Create optimistic mutation composable
 * @param {Object} options - Configuration options
 * @param {String} options.entityName - Vietnamese entity name
 * @param {String} options.storeKey - Store identifier for real-time sync
 * @param {Number} options.timeoutMs - Mutation timeout
 * @param {Boolean} options.enableRealTimeSync - Enable real-time synchronization
 * @param {Boolean} options.enableConflictResolution - Enable automatic conflict resolution
 * @param {String} options.conflictStrategy - Default conflict resolution strategy
 * @returns {Object} Optimistic mutation composable
 */
export function useOptimisticMutation(options = {}) {
  const {
    entityName = 'dữ liệu',
    storeKey = 'default',
    timeoutMs = 15000,
    enableRealTimeSync = true,
    enableConflictResolution = true,
    conflictStrategy = RESOLUTION_STRATEGIES.FIELD_LEVEL
  } = options

  const toast = useToast()

  // Initialize real-time sync if enabled
  const realTimeSync = enableRealTimeSync ? useRealTimeSync({
    entityName,
    storeKey,
    enablePersistence: true,
    enableCrossTab: true
  }) : null

  // Initialize conflict resolver if enabled
  const conflictResolver = enableConflictResolution ? createConflictResolver({
    entityName,
    defaultStrategy: conflictStrategy
  }) : null

  // Mutation state management
  const activeMutations = ref(new Map())
  const mutationHistory = ref([])
  const maxHistorySize = 50

  // Performance metrics
  const mutationMetrics = ref({
    totalMutations: 0,
    successfulMutations: 0,
    failedMutations: 0,
    rolledBackMutations: 0,
    averageResponseTime: 0,
    conflictsResolved: 0
  })

  // UI state tracking
  const pendingOperations = ref(new Set())
  const optimisticUpdates = ref(new Map())

  /**
   * Generate unique mutation ID
   * @returns {String} Unique mutation ID
   */
  function generateMutationId() {
    return `mut_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
  }

  /**
   * Create mutation context
   * @param {String} type - Mutation type
   * @param {*} data - Mutation data
   * @param {Object} options - Mutation options
   * @returns {Object} Mutation context
   */
  function createMutationContext(type, data, options = {}) {
    const mutationId = generateMutationId()

    return {
      id: mutationId,
      type,
      data,
      state: MUTATION_STATES.PENDING,
      startTime: Date.now(),
      endTime: null,
      error: null,
      rollbackData: null,
      optimisticState: null,
      confirmedState: null,
      retryCount: 0,
      maxRetries: options.maxRetries || 3,
      timeout: options.timeout || timeoutMs,
      enableRollback: options.enableRollback !== false,
      metadata: {
        entityName,
        storeKey,
        timestamp: new Date().toISOString(),
        ...options.metadata
      }
    }
  }

  /**
   * Execute optimistic mutation
   * @param {String} type - Mutation type
   * @param {*} data - Mutation data
   * @param {Function} optimisticUpdate - Optimistic update function
   * @param {Function} apiCall - API call function
   * @param {Function} rollbackUpdate - Rollback function
   * @param {Object} options - Mutation options
   * @returns {Promise} Promise resolving to mutation result
   */
  async function executeMutation(type, data, optimisticUpdate, apiCall, rollbackUpdate, options = {}) {
    const context = createMutationContext(type, data, options)
    const { id } = context

    // Track mutation
    activeMutations.value.set(id, context)
    pendingOperations.value.add(id)
    mutationMetrics.value.totalMutations++

    try {
      // Apply optimistic update immediately
      if (optimisticUpdate) {
        context.state = MUTATION_STATES.OPTIMISTIC
        context.rollbackData = await captureRollbackData(data, options)

        const optimisticResult = await optimisticUpdate(data)
        context.optimisticState = optimisticResult
        optimisticUpdates.value.set(id, optimisticResult)

        // Sync optimistic state if real-time sync enabled
        if (realTimeSync) {
          await realTimeSync.syncStateData(optimisticResult, {
            merge: true,
            optimistic: true,
            mutationId: id
          })
        }

        // Trigger UI update
        await nextTick()
      }

      // Set timeout for API call
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Mutation timeout')), context.timeout)
      })

      // Execute API call with timeout
      const apiResult = await Promise.race([
        apiCall(data, context),
        timeoutPromise
      ])

      // Handle API response
      if (apiResult && apiResult.success) {
        context.state = MUTATION_STATES.CONFIRMED
        context.confirmedState = apiResult.data
        context.endTime = Date.now()

        // Update optimistic state with confirmed data
        if (context.confirmedState && optimisticUpdates.value.has(id)) {
          optimisticUpdates.value.set(id, context.confirmedState)

          // Handle potential conflicts with real-time sync
          if (realTimeSync && enableConflictResolution) {
            await handleStateConflict(context)
          }
        }

        // Update metrics
        mutationMetrics.value.successfulMutations++
        updateAverageResponseTime(context.endTime - context.startTime)

        // Add to history
        addToHistory(context)

        return {
          success: true,
          data: context.confirmedState || apiResult.data,
          mutationId: id,
          responseTime: context.endTime - context.startTime
        }

      } else {
        throw new Error(apiResult?.message || 'API call failed')
      }

    } catch (error) {
      context.error = error.message
      context.endTime = Date.now()

      // Check if retry is possible
      if (context.retryCount < context.maxRetries && shouldRetry(error)) {
        context.retryCount++

        toast.add({
          severity: 'warn',
          summary: 'Đang thử lại',
          detail: `Thử lại thao tác ${entityName} (lần ${context.retryCount}/${context.maxRetries})`,
          life: 2000
        })

        // Retry with exponential backoff
        const delay = Math.min(1000 * Math.pow(2, context.retryCount - 1), 5000)
        await new Promise(resolve => setTimeout(resolve, delay))

        return executeMutation(type, data, optimisticUpdate, apiCall, rollbackUpdate, options)
      }

      // Rollback optimistic update
      if (context.enableRollback && rollbackUpdate && context.rollbackData) {
        try {
          context.state = MUTATION_STATES.ROLLED_BACK
          await rollbackUpdate(context.rollbackData, context)
          optimisticUpdates.value.delete(id)
          mutationMetrics.value.rolledBackMutations++

          // Sync rollback state
          if (realTimeSync) {
            await realTimeSync.syncStateData(context.rollbackData, {
              merge: false,
              rollback: true,
              mutationId: id
            })
          }

        } catch (rollbackError) {
          console.error('❌ Failed to rollback optimistic update:', rollbackError)
        }
      }

      context.state = MUTATION_STATES.FAILED
      mutationMetrics.value.failedMutations++

      // Add to history
      addToHistory(context)

      // Show error toast
      toast.add({
        severity: 'error',
        summary: 'Lỗi thao tác',
        detail: `Không thể thực hiện thao tác ${entityName}: ${error.message}`,
        life: 5000
      })

      return {
        success: false,
        error: error.message,
        mutationId: id,
        responseTime: context.endTime - context.startTime
      }

    } finally {
      // Cleanup
      activeMutations.value.delete(id)
      pendingOperations.value.delete(id)
    }
  }

  /**
   * Handle state conflicts with real-time sync
   * @param {Object} context - Mutation context
   */
  async function handleStateConflict(context) {
    if (!conflictResolver || !realTimeSync) return

    try {
      const currentState = realTimeSync.persistedState.value
      const incomingState = context.confirmedState

      if (!currentState || !incomingState) return

      const resolution = conflictResolver.resolveConflicts(
        currentState,
        incomingState,
        conflictStrategy
      )

      if (resolution.success && resolution.conflicts.length > 0) {
        // Update with resolved state
        await realTimeSync.syncStateData(resolution.resolvedState, {
          merge: false,
          conflictResolved: true,
          mutationId: context.id
        })

        mutationMetrics.value.conflictsResolved++

        // Notify about conflict resolution
        toast.add({
          severity: 'info',
          summary: 'Xung đột đã được giải quyết',
          detail: `Đã tự động giải quyết ${resolution.conflicts.length} xung đột cho ${entityName}`,
          life: 3000
        })
      }

    } catch (error) {
      console.error('❌ Failed to handle state conflict:', error)
    }
  }

  /**
   * Capture rollback data
   * @param {*} data - Mutation data
   * @param {Object} options - Options
   * @returns {*} Rollback data
   */
  async function captureRollbackData(data, options) {
    if (options.captureRollback && typeof options.captureRollback === 'function') {
      return await options.captureRollback(data)
    }

    // Default rollback data capture
    if (realTimeSync && realTimeSync.persistedState.value) {
      return { ...realTimeSync.persistedState.value }
    }

    return null
  }

  /**
   * Check if error should trigger retry
   * @param {Error} error - Error object
   * @returns {Boolean} Should retry
   */
  function shouldRetry(error) {
    const retryableErrors = [
      'network error',
      'timeout',
      'connection failed',
      'server error'
    ]

    const errorMessage = error.message.toLowerCase()
    return retryableErrors.some(retryable => errorMessage.includes(retryable))
  }

  /**
   * Update average response time metric
   * @param {Number} responseTime - Response time in ms
   */
  function updateAverageResponseTime(responseTime) {
    const total = mutationMetrics.value.successfulMutations
    const current = mutationMetrics.value.averageResponseTime

    mutationMetrics.value.averageResponseTime =
      (current * (total - 1) + responseTime) / total
  }

  /**
   * Add mutation to history
   * @param {Object} context - Mutation context
   */
  function addToHistory(context) {
    mutationHistory.value.unshift({
      ...context,
      // Remove large objects to save memory
      optimisticState: null,
      confirmedState: null,
      rollbackData: null
    })

    if (mutationHistory.value.length > maxHistorySize) {
      mutationHistory.value = mutationHistory.value.slice(0, maxHistorySize)
    }
  }

  /**
   * Create optimistic create mutation
   * @param {*} data - Data to create
   * @param {Function} optimisticUpdate - Optimistic update function
   * @param {Function} apiCall - API call function
   * @param {Function} rollbackUpdate - Rollback function
   * @param {Object} options - Options
   * @returns {Function} Mutation function
   */
  function createOptimisticCreate(data, optimisticUpdate, apiCall, rollbackUpdate, options = {}) {
    return () => executeMutation(
      MUTATION_TYPES.CREATE,
      data,
      optimisticUpdate,
      apiCall,
      rollbackUpdate,
      options
    )
  }

  /**
   * Create optimistic update mutation
   * @param {*} data - Data to update
   * @param {Function} optimisticUpdate - Optimistic update function
   * @param {Function} apiCall - API call function
   * @param {Function} rollbackUpdate - Rollback function
   * @param {Object} options - Options
   * @returns {Function} Mutation function
   */
  function createOptimisticUpdate(data, optimisticUpdate, apiCall, rollbackUpdate, options = {}) {
    return () => executeMutation(
      MUTATION_TYPES.UPDATE,
      data,
      optimisticUpdate,
      apiCall,
      rollbackUpdate,
      options
    )
  }

  /**
   * Create optimistic delete mutation
   * @param {*} data - Data to delete
   * @param {Function} optimisticUpdate - Optimistic update function
   * @param {Function} apiCall - API call function
   * @param {Function} rollbackUpdate - Rollback function
   * @param {Object} options - Options
   * @returns {Function} Mutation function
   */
  function createOptimisticDelete(data, optimisticUpdate, apiCall, rollbackUpdate, options = {}) {
    return () => executeMutation(
      MUTATION_TYPES.DELETE,
      data,
      optimisticUpdate,
      apiCall,
      rollbackUpdate,
      options
    )
  }

  /**
   * Execute batch mutations
   * @param {Array} mutations - Array of mutation functions
   * @param {Object} options - Batch options
   * @returns {Promise} Promise resolving to batch result
   */
  async function executeBatchMutations(mutations, options = {}) {
    const batchId = generateMutationId()
    const results = []

    try {
      if (options.sequential) {
        // Execute mutations sequentially
        for (const mutation of mutations) {
          const result = await mutation()
          results.push(result)

          if (!result.success && options.stopOnError) {
            break
          }
        }
      } else {
        // Execute mutations in parallel
        const promises = mutations.map(mutation => mutation())
        const batchResults = await Promise.allSettled(promises)

        results.push(...batchResults.map(result =>
          result.status === 'fulfilled' ? result.value : { success: false, error: result.reason.message }
        ))
      }

      const successCount = results.filter(r => r.success).length
      const failureCount = results.length - successCount

      return {
        success: failureCount === 0,
        batchId,
        results,
        summary: {
          total: results.length,
          successful: successCount,
          failed: failureCount
        }
      }

    } catch (error) {
      return {
        success: false,
        batchId,
        error: error.message,
        results
      }
    }
  }

  // Computed properties
  const hasPendingMutations = computed(() => pendingOperations.value.size > 0)
  const mutationSuccessRate = computed(() => {
    const total = mutationMetrics.value.totalMutations
    return total > 0 ? (mutationMetrics.value.successfulMutations / total * 100).toFixed(2) : 0
  })

  const isHealthy = computed(() => {
    const successRate = parseFloat(mutationSuccessRate.value)
    return successRate >= 90 && mutationMetrics.value.averageResponseTime < 5000
  })

  return {
    // State
    activeMutations: computed(() => activeMutations.value),
    mutationHistory: computed(() => mutationHistory.value),
    mutationMetrics: computed(() => mutationMetrics.value),
    pendingOperations: computed(() => Array.from(pendingOperations.value)),
    optimisticUpdates: computed(() => optimisticUpdates.value),

    // Computed
    hasPendingMutations,
    mutationSuccessRate,
    isHealthy,

    // Methods
    executeMutation,
    createOptimisticCreate,
    createOptimisticUpdate,
    createOptimisticDelete,
    executeBatchMutations,

    // Real-time sync integration
    realTimeSync,
    conflictResolver,

    // Constants
    MUTATION_TYPES,
    MUTATION_STATES
  }
}
