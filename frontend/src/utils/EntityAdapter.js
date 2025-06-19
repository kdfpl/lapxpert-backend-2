/**
 * EntityAdapter - Normalized State Management Utility
 * Provides efficient entity management with normalized state structure
 * Follows LapXpert patterns and Vietnamese business terminology
 */

/**
 * Create an entity adapter for normalized state management
 * @param {Object} options - Configuration options
 * @param {Function} options.selectId - Function to extract entity ID (default: entity => entity.id)
 * @param {Function} options.sortComparer - Function to sort entities (optional)
 * @returns {Object} Entity adapter with CRUD operations
 */
export function createEntityAdapter(options = {}) {
  const {
    selectId = (entity) => entity.id,
    sortComparer = null
  } = options

  /**
   * Create initial normalized state
   * @returns {Object} Initial state with ids array and entities object
   */
  function getInitialState() {
    return {
      ids: [],
      entities: {}
    }
  }

  /**
   * Add one entity to the state
   * @param {Object} state - Current normalized state
   * @param {Object} entity - Entity to add
   * @returns {Object} New state with added entity
   */
  function addOne(state, entity) {
    const id = selectId(entity)
    
    // Avoid duplicates
    if (state.entities[id]) {
      return state
    }

    const newEntities = { ...state.entities, [id]: entity }
    const newIds = [...state.ids, id]

    return {
      ids: sortComparer ? sortIds(newIds, newEntities) : newIds,
      entities: newEntities
    }
  }

  /**
   * Add multiple entities to the state
   * @param {Object} state - Current normalized state
   * @param {Array} entities - Entities to add
   * @returns {Object} New state with added entities
   */
  function addMany(state, entities) {
    const newEntities = { ...state.entities }
    const newIds = [...state.ids]

    entities.forEach(entity => {
      const id = selectId(entity)
      if (!newEntities[id]) {
        newEntities[id] = entity
        newIds.push(id)
      }
    })

    return {
      ids: sortComparer ? sortIds(newIds, newEntities) : newIds,
      entities: newEntities
    }
  }

  /**
   * Set all entities (replace existing state)
   * @param {Object} state - Current normalized state
   * @param {Array} entities - New entities to set
   * @returns {Object} New state with set entities
   */
  function setAll(state, entities) {
    const newEntities = {}
    const newIds = []

    entities.forEach(entity => {
      const id = selectId(entity)
      newEntities[id] = entity
      newIds.push(id)
    })

    return {
      ids: sortComparer ? sortIds(newIds, newEntities) : newIds,
      entities: newEntities
    }
  }

  /**
   * Update one entity in the state
   * @param {Object} state - Current normalized state
   * @param {Object} update - Update object with id and changes
   * @returns {Object} New state with updated entity
   */
  function updateOne(state, update) {
    const { id, changes } = update
    
    if (!state.entities[id]) {
      return state
    }

    const updatedEntity = { ...state.entities[id], ...changes }
    const newEntities = { ...state.entities, [id]: updatedEntity }

    return {
      ids: sortComparer ? sortIds(state.ids, newEntities) : state.ids,
      entities: newEntities
    }
  }

  /**
   * Update multiple entities in the state
   * @param {Object} state - Current normalized state
   * @param {Array} updates - Array of update objects
   * @returns {Object} New state with updated entities
   */
  function updateMany(state, updates) {
    const newEntities = { ...state.entities }
    let hasChanges = false

    updates.forEach(update => {
      const { id, changes } = update
      if (newEntities[id]) {
        newEntities[id] = { ...newEntities[id], ...changes }
        hasChanges = true
      }
    })

    if (!hasChanges) {
      return state
    }

    return {
      ids: sortComparer ? sortIds(state.ids, newEntities) : state.ids,
      entities: newEntities
    }
  }

  /**
   * Upsert one entity (update if exists, add if not)
   * @param {Object} state - Current normalized state
   * @param {Object} entity - Entity to upsert
   * @returns {Object} New state with upserted entity
   */
  function upsertOne(state, entity) {
    const id = selectId(entity)
    
    if (state.entities[id]) {
      return updateOne(state, { id, changes: entity })
    } else {
      return addOne(state, entity)
    }
  }

  /**
   * Upsert multiple entities
   * @param {Object} state - Current normalized state
   * @param {Array} entities - Entities to upsert
   * @returns {Object} New state with upserted entities
   */
  function upsertMany(state, entities) {
    const newEntities = { ...state.entities }
    const newIds = [...state.ids]

    entities.forEach(entity => {
      const id = selectId(entity)
      if (newEntities[id]) {
        newEntities[id] = { ...newEntities[id], ...entity }
      } else {
        newEntities[id] = entity
        newIds.push(id)
      }
    })

    return {
      ids: sortComparer ? sortIds(newIds, newEntities) : newIds,
      entities: newEntities
    }
  }

  /**
   * Remove one entity from the state
   * @param {Object} state - Current normalized state
   * @param {String|Number} id - ID of entity to remove
   * @returns {Object} New state with removed entity
   */
  function removeOne(state, id) {
    if (!state.entities[id]) {
      return state
    }

    const newEntities = { ...state.entities }
    delete newEntities[id]

    return {
      ids: state.ids.filter(existingId => existingId !== id),
      entities: newEntities
    }
  }

  /**
   * Remove multiple entities from the state
   * @param {Object} state - Current normalized state
   * @param {Array} ids - IDs of entities to remove
   * @returns {Object} New state with removed entities
   */
  function removeMany(state, ids) {
    const idsToRemove = new Set(ids)
    const newEntities = { ...state.entities }
    let hasChanges = false

    idsToRemove.forEach(id => {
      if (newEntities[id]) {
        delete newEntities[id]
        hasChanges = true
      }
    })

    if (!hasChanges) {
      return state
    }

    return {
      ids: state.ids.filter(id => !idsToRemove.has(id)),
      entities: newEntities
    }
  }

  /**
   * Remove all entities from the state
   * @param {Object} state - Current normalized state
   * @returns {Object} Empty normalized state
   */
  function removeAll(state) {
    return getInitialState()
  }

  /**
   * Sort entity IDs based on the sort comparer
   * @param {Array} ids - Array of entity IDs
   * @param {Object} entities - Entities object
   * @returns {Array} Sorted array of IDs
   */
  function sortIds(ids, entities) {
    if (!sortComparer) return ids
    
    return [...ids].sort((a, b) => {
      return sortComparer(entities[a], entities[b])
    })
  }

  // Selectors
  const selectors = {
    /**
     * Get all entities as an array
     * @param {Object} state - Normalized state
     * @returns {Array} Array of entities
     */
    selectAll: (state) => state.ids.map(id => state.entities[id]),

    /**
     * Get entity by ID
     * @param {Object} state - Normalized state
     * @param {String|Number} id - Entity ID
     * @returns {Object|undefined} Entity or undefined
     */
    selectById: (state, id) => state.entities[id],

    /**
     * Get multiple entities by IDs
     * @param {Object} state - Normalized state
     * @param {Array} ids - Array of entity IDs
     * @returns {Array} Array of entities
     */
    selectByIds: (state, ids) => ids.map(id => state.entities[id]).filter(Boolean),

    /**
     * Get total count of entities
     * @param {Object} state - Normalized state
     * @returns {Number} Total count
     */
    selectTotal: (state) => state.ids.length,

    /**
     * Get all entity IDs
     * @param {Object} state - Normalized state
     * @returns {Array} Array of entity IDs
     */
    selectIds: (state) => state.ids,

    /**
     * Get entities object
     * @param {Object} state - Normalized state
     * @returns {Object} Entities object
     */
    selectEntities: (state) => state.entities
  }

  return {
    // State management
    getInitialState,
    
    // CRUD operations
    addOne,
    addMany,
    setAll,
    updateOne,
    updateMany,
    upsertOne,
    upsertMany,
    removeOne,
    removeMany,
    removeAll,
    
    // Selectors
    selectors
  }
}

/**
 * Create entity adapter with Vietnamese date sorting (newest first)
 * Common pattern for LapXpert entities with ngayCapNhat field
 * @param {Object} options - Additional options
 * @returns {Object} Entity adapter with Vietnamese date sorting
 */
export function createVietnameseEntityAdapter(options = {}) {
  return createEntityAdapter({
    ...options,
    sortComparer: (a, b) => {
      // Sort by ngayCapNhat (newest first) - common Vietnamese pattern
      const dateA = new Date(a.ngayCapNhat || a.createdAt || 0)
      const dateB = new Date(b.ngayCapNhat || b.createdAt || 0)
      return dateB - dateA
    }
  })
}
