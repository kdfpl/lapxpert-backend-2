import { ref, computed, watch } from 'vue'
import { createEntityAdapter } from '@/utils/EntityAdapter'
import { useToast } from 'primevue/usetoast'

/**
 * useEntityStore - Consistent Entity State Operations Composable
 * Provides standardized entity management with normalized state structure
 * Follows LapXpert patterns and Vietnamese business terminology
 */

/**
 * Create entity store composable with normalized state management
 * @param {Object} options - Configuration options
 * @param {String} options.entityName - Vietnamese entity name (e.g., 'hoaDon', 'sanPham')
 * @param {Function} options.selectId - Function to extract entity ID
 * @param {Function} options.sortComparer - Function to sort entities
 * @param {Boolean} options.enableCrossTab - Enable cross-tab synchronization
 * @param {Boolean} options.enableOptimistic - Enable optimistic updates
 * @returns {Object} Entity store composable
 */
export function useEntityStore(options = {}) {
  const {
    entityName = 'entity',
    selectId = (entity) => entity.id,
    sortComparer = null,
    enableCrossTab = true,
    enableOptimistic = true
  } = options

  const toast = useToast()

  // Create entity adapter
  const adapter = createEntityAdapter({ selectId, sortComparer })

  // Normalized state
  const entityState = ref(adapter.getInitialState())
  
  // Additional state
  const loading = ref(false)
  const error = ref(null)
  const lastUpdated = ref(null)
  const optimisticOperations = ref(new Map()) // Track optimistic operations

  // Cross-tab synchronization
  const crossTabChannel = ref(null)
  const tabId = ref(`tab_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`)

  /**
   * Initialize cross-tab synchronization
   */
  function initializeCrossTabSync() {
    if (!enableCrossTab || typeof BroadcastChannel === 'undefined') return

    try {
      crossTabChannel.value = new BroadcastChannel(`lapxpert_${entityName}_sync`)
      
      crossTabChannel.value.onmessage = (event) => {
        const { type, data, sourceTabId, timestamp } = event.data
        
        // Ignore messages from same tab
        if (sourceTabId === tabId.value) return

        handleCrossTabMessage(type, data, timestamp)
      }

      console.log(`✅ Cross-tab sync initialized for ${entityName}`)
    } catch (error) {
      console.warn(`⚠️ Failed to initialize cross-tab sync for ${entityName}:`, error)
    }
  }

  /**
   * Handle cross-tab messages
   * @param {String} type - Message type
   * @param {*} data - Message data
   * @param {String} timestamp - Message timestamp
   */
  function handleCrossTabMessage(type, data, timestamp) {
    try {
      switch (type) {
        case 'ENTITY_ADDED':
          entityState.value = adapter.addOne(entityState.value, data)
          break
        case 'ENTITY_UPDATED':
          entityState.value = adapter.updateOne(entityState.value, data)
          break
        case 'ENTITY_REMOVED':
          entityState.value = adapter.removeOne(entityState.value, data)
          break
        case 'ENTITIES_SET':
          entityState.value = adapter.setAll(entityState.value, data)
          break
        case 'STATE_SYNC':
          // Full state synchronization
          entityState.value = data
          break
      }
      
      lastUpdated.value = timestamp
    } catch (error) {
      console.error(`❌ Error handling cross-tab message for ${entityName}:`, error)
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
        entityName
      })
    } catch (error) {
      console.error(`❌ Failed to broadcast ${type} for ${entityName}:`, error)
    }
  }

  /**
   * Generate optimistic operation ID
   * @returns {String} Unique operation ID
   */
  function generateOptimisticId() {
    return `opt_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
  }

  /**
   * Start optimistic operation
   * @param {String} type - Operation type
   * @param {*} data - Operation data
   * @returns {String} Operation ID
   */
  function startOptimisticOperation(type, data) {
    if (!enableOptimistic) return null

    const operationId = generateOptimisticId()
    const previousState = { ...entityState.value }

    optimisticOperations.value.set(operationId, {
      type,
      data,
      previousState,
      timestamp: Date.now()
    })

    return operationId
  }

  /**
   * Commit optimistic operation
   * @param {String} operationId - Operation ID
   * @param {*} actualData - Actual data from server (optional)
   */
  function commitOptimisticOperation(operationId, actualData = null) {
    if (!operationId || !optimisticOperations.value.has(operationId)) return

    const operation = optimisticOperations.value.get(operationId)
    
    // If actual data provided, update with server response
    if (actualData) {
      switch (operation.type) {
        case 'add':
          entityState.value = adapter.upsertOne(entityState.value, actualData)
          break
        case 'update':
          entityState.value = adapter.updateOne(entityState.value, {
            id: selectId(actualData),
            changes: actualData
          })
          break
      }
    }

    optimisticOperations.value.delete(operationId)
  }

  /**
   * Rollback optimistic operation
   * @param {String} operationId - Operation ID
   */
  function rollbackOptimisticOperation(operationId) {
    if (!operationId || !optimisticOperations.value.has(operationId)) return

    const operation = optimisticOperations.value.get(operationId)
    entityState.value = operation.previousState
    optimisticOperations.value.delete(operationId)

    toast.add({
      severity: 'warn',
      summary: 'Thao tác không thành công',
      detail: `Đã hoàn tác thay đổi cho ${entityName}`,
      life: 3000
    })
  }

  // Computed selectors
  const allEntities = computed(() => adapter.selectors.selectAll(entityState.value))
  const totalCount = computed(() => adapter.selectors.selectTotal(entityState.value))
  const entityIds = computed(() => adapter.selectors.selectIds(entityState.value))
  const entitiesMap = computed(() => adapter.selectors.selectEntities(entityState.value))

  /**
   * Get entity by ID
   * @param {String|Number} id - Entity ID
   * @returns {Object|undefined} Entity or undefined
   */
  const getEntityById = (id) => adapter.selectors.selectById(entityState.value, id)

  /**
   * Get multiple entities by IDs
   * @param {Array} ids - Array of entity IDs
   * @returns {Array} Array of entities
   */
  const getEntitiesByIds = (ids) => adapter.selectors.selectByIds(entityState.value, ids)

  /**
   * Add entity with optimistic update
   * @param {Object} entity - Entity to add
   * @param {Function} apiCall - API call function
   * @returns {Promise} Promise resolving to operation result
   */
  async function addEntity(entity, apiCall = null) {
    const operationId = startOptimisticOperation('add', entity)
    
    // Optimistic update
    entityState.value = adapter.addOne(entityState.value, entity)
    broadcastToOtherTabs('ENTITY_ADDED', entity)

    if (!apiCall) {
      commitOptimisticOperation(operationId)
      return { success: true, data: entity }
    }

    try {
      const result = await apiCall(entity)
      
      if (result.success) {
        commitOptimisticOperation(operationId, result.data)
        lastUpdated.value = new Date().toISOString()
        return result
      } else {
        rollbackOptimisticOperation(operationId)
        return result
      }
    } catch (error) {
      rollbackOptimisticOperation(operationId)
      throw error
    }
  }

  /**
   * Update entity with optimistic update
   * @param {String|Number} id - Entity ID
   * @param {Object} changes - Changes to apply
   * @param {Function} apiCall - API call function
   * @returns {Promise} Promise resolving to operation result
   */
  async function updateEntity(id, changes, apiCall = null) {
    const operationId = startOptimisticOperation('update', { id, changes })
    
    // Optimistic update
    entityState.value = adapter.updateOne(entityState.value, { id, changes })
    broadcastToOtherTabs('ENTITY_UPDATED', { id, changes })

    if (!apiCall) {
      commitOptimisticOperation(operationId)
      return { success: true, data: getEntityById(id) }
    }

    try {
      const result = await apiCall(id, changes)
      
      if (result.success) {
        commitOptimisticOperation(operationId, result.data)
        lastUpdated.value = new Date().toISOString()
        return result
      } else {
        rollbackOptimisticOperation(operationId)
        return result
      }
    } catch (error) {
      rollbackOptimisticOperation(operationId)
      throw error
    }
  }

  /**
   * Remove entity with optimistic update
   * @param {String|Number} id - Entity ID
   * @param {Function} apiCall - API call function
   * @returns {Promise} Promise resolving to operation result
   */
  async function removeEntity(id, apiCall = null) {
    const operationId = startOptimisticOperation('remove', id)
    
    // Optimistic update
    entityState.value = adapter.removeOne(entityState.value, id)
    broadcastToOtherTabs('ENTITY_REMOVED', id)

    if (!apiCall) {
      commitOptimisticOperation(operationId)
      return { success: true }
    }

    try {
      const result = await apiCall(id)
      
      if (result.success) {
        commitOptimisticOperation(operationId)
        lastUpdated.value = new Date().toISOString()
        return result
      } else {
        rollbackOptimisticOperation(operationId)
        return result
      }
    } catch (error) {
      rollbackOptimisticOperation(operationId)
      throw error
    }
  }

  /**
   * Set all entities (replace current state)
   * @param {Array} entities - New entities
   */
  function setAllEntities(entities) {
    entityState.value = adapter.setAll(entityState.value, entities)
    broadcastToOtherTabs('ENTITIES_SET', entities)
    lastUpdated.value = new Date().toISOString()
  }

  /**
   * Upsert entity (update if exists, add if not)
   * @param {Object} entity - Entity to upsert
   */
  function upsertEntity(entity) {
    entityState.value = adapter.upsertOne(entityState.value, entity)
    broadcastToOtherTabs('ENTITY_UPDATED', entity)
    lastUpdated.value = new Date().toISOString()
  }

  /**
   * Clear all entities
   */
  function clearAllEntities() {
    entityState.value = adapter.getInitialState()
    broadcastToOtherTabs('ENTITIES_SET', [])
    lastUpdated.value = new Date().toISOString()
  }

  /**
   * Sync state with other tabs
   */
  function syncStateWithOtherTabs() {
    broadcastToOtherTabs('STATE_SYNC', entityState.value)
  }

  // Initialize cross-tab sync
  initializeCrossTabSync()

  return {
    // State
    entityState,
    loading,
    error,
    lastUpdated,
    tabId,

    // Computed
    allEntities,
    totalCount,
    entityIds,
    entitiesMap,

    // Selectors
    getEntityById,
    getEntitiesByIds,

    // CRUD operations
    addEntity,
    updateEntity,
    removeEntity,
    setAllEntities,
    upsertEntity,
    clearAllEntities,

    // Cross-tab sync
    syncStateWithOtherTabs,
    broadcastToOtherTabs,

    // Optimistic operations
    optimisticOperations: computed(() => optimisticOperations.value),

    // Adapter access (for advanced usage)
    adapter
  }
}
