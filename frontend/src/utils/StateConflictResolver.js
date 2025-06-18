/**
 * StateConflictResolver - Utility for resolving state conflicts
 * Handles concurrent user actions and provides various merge strategies
 * for different data types in the LapXpert system
 * Follows Vietnamese business terminology and patterns
 */

/**
 * Conflict resolution strategies
 */
export const RESOLUTION_STRATEGIES = {
  LAST_WRITE_WINS: 'last_write_wins',
  FIRST_WRITE_WINS: 'first_write_wins',
  MERGE_DEEP: 'merge_deep',
  MERGE_SHALLOW: 'merge_shallow',
  USER_CHOICE: 'user_choice',
  FIELD_LEVEL: 'field_level',
  BUSINESS_RULES: 'business_rules'
}

/**
 * Conflict types
 */
export const CONFLICT_TYPES = {
  CONCURRENT_UPDATE: 'concurrent_update',
  VERSION_MISMATCH: 'version_mismatch',
  FIELD_CONFLICT: 'field_conflict',
  BUSINESS_RULE_VIOLATION: 'business_rule_violation',
  CROSS_TAB_CONFLICT: 'cross_tab_conflict'
}

/**
 * Vietnamese business entity field priorities
 * Higher priority fields take precedence in conflicts
 */
const FIELD_PRIORITIES = {
  // Order fields (Hóa đơn)
  'maHoaDon': 100,
  'trangThaiDonHang': 90,
  'trangThaiThanhToan': 90,
  'tongThanhToan': 85,
  'ngayCapNhat': 80,
  'sanPhamList': 75,
  'khachHang': 70,
  'giaohang': 65,
  'ghiChu': 50,

  // Product fields (Sản phẩm)
  'maSanPham': 100,
  'tenSanPham': 90,
  'giaBan': 85,
  'soLuongTon': 80,
  'trangThai': 75,
  'moTa': 50,

  // Customer fields (Khách hàng)
  'maKhachHang': 100,
  'tenKhachHang': 90,
  'soDienThoai': 85,
  'email': 80,
  'diaChi': 75,

  // Default priority
  'default': 60
}

/**
 * Create state conflict resolver
 */
export class StateConflictResolver {
  constructor(options = {}) {
    this.entityName = options.entityName || 'dữ liệu'
    this.defaultStrategy = options.defaultStrategy || RESOLUTION_STRATEGIES.LAST_WRITE_WINS
    this.businessRules = options.businessRules || {}
    this.fieldPriorities = { ...FIELD_PRIORITIES, ...options.fieldPriorities }
    this.conflictHistory = []
    this.maxHistorySize = options.maxHistorySize || 100
  }

  /**
   * Detect conflicts between two states
   * @param {Object} currentState - Current state
   * @param {Object} incomingState - Incoming state
   * @param {Object} options - Detection options
   * @returns {Object} Conflict detection result
   */
  detectConflicts(currentState, incomingState, options = {}) {
    const conflicts = []
    const metadata = {
      detectionTime: new Date().toISOString(),
      entityName: this.entityName,
      conflictCount: 0
    }

    // Check for version conflicts
    if (currentState.version && incomingState.version) {
      if (currentState.version !== incomingState.version) {
        conflicts.push({
          type: CONFLICT_TYPES.VERSION_MISMATCH,
          field: 'version',
          currentValue: currentState.version,
          incomingValue: incomingState.version,
          severity: 'high'
        })
      }
    }

    // Check for timestamp conflicts
    if (currentState.ngayCapNhat && incomingState.ngayCapNhat) {
      const currentTime = new Date(currentState.ngayCapNhat)
      const incomingTime = new Date(incomingState.ngayCapNhat)
      
      if (Math.abs(currentTime - incomingTime) < 1000) { // Within 1 second
        conflicts.push({
          type: CONFLICT_TYPES.CONCURRENT_UPDATE,
          field: 'ngayCapNhat',
          currentValue: currentState.ngayCapNhat,
          incomingValue: incomingState.ngayCapNhat,
          severity: 'medium'
        })
      }
    }

    // Check field-level conflicts
    const allFields = new Set([
      ...Object.keys(currentState),
      ...Object.keys(incomingState)
    ])

    for (const field of allFields) {
      if (this.isSystemField(field)) continue

      const currentValue = currentState[field]
      const incomingValue = incomingState[field]

      if (this.hasFieldConflict(currentValue, incomingValue)) {
        conflicts.push({
          type: CONFLICT_TYPES.FIELD_CONFLICT,
          field,
          currentValue,
          incomingValue,
          severity: this.getFieldConflictSeverity(field, currentValue, incomingValue),
          priority: this.fieldPriorities[field] || this.fieldPriorities.default
        })
      }
    }

    // Check business rule violations
    const businessRuleConflicts = this.checkBusinessRules(currentState, incomingState)
    conflicts.push(...businessRuleConflicts)

    metadata.conflictCount = conflicts.length
    
    return {
      hasConflicts: conflicts.length > 0,
      conflicts,
      metadata
    }
  }

  /**
   * Resolve conflicts using specified strategy
   * @param {Object} currentState - Current state
   * @param {Object} incomingState - Incoming state
   * @param {String} strategy - Resolution strategy
   * @param {Object} options - Resolution options
   * @returns {Object} Resolution result
   */
  resolveConflicts(currentState, incomingState, strategy = null, options = {}) {
    const resolveStrategy = strategy || this.defaultStrategy
    const startTime = Date.now()

    try {
      let resolvedState
      let resolutionDetails = {
        strategy: resolveStrategy,
        timestamp: new Date().toISOString(),
        entityName: this.entityName,
        conflictsResolved: 0,
        fieldsModified: []
      }

      // Detect conflicts first
      const conflictResult = this.detectConflicts(currentState, incomingState, options)
      
      if (!conflictResult.hasConflicts) {
        return {
          success: true,
          resolvedState: incomingState,
          conflicts: [],
          resolutionDetails: {
            ...resolutionDetails,
            message: 'No conflicts detected'
          }
        }
      }

      // Apply resolution strategy
      switch (resolveStrategy) {
        case RESOLUTION_STRATEGIES.LAST_WRITE_WINS:
          resolvedState = this.resolveLastWriteWins(currentState, incomingState, conflictResult)
          break

        case RESOLUTION_STRATEGIES.FIRST_WRITE_WINS:
          resolvedState = this.resolveFirstWriteWins(currentState, incomingState, conflictResult)
          break

        case RESOLUTION_STRATEGIES.MERGE_DEEP:
          resolvedState = this.resolveMergeDeep(currentState, incomingState, conflictResult)
          break

        case RESOLUTION_STRATEGIES.MERGE_SHALLOW:
          resolvedState = this.resolveMergeShallow(currentState, incomingState, conflictResult)
          break

        case RESOLUTION_STRATEGIES.FIELD_LEVEL:
          resolvedState = this.resolveFieldLevel(currentState, incomingState, conflictResult)
          break

        case RESOLUTION_STRATEGIES.BUSINESS_RULES:
          resolvedState = this.resolveBusinessRules(currentState, incomingState, conflictResult)
          break

        default:
          throw new Error(`Unknown resolution strategy: ${resolveStrategy}`)
      }

      // Update resolution details
      resolutionDetails.conflictsResolved = conflictResult.conflicts.length
      resolutionDetails.resolutionTime = Date.now() - startTime
      resolutionDetails.fieldsModified = this.getModifiedFields(currentState, resolvedState)

      // Add to conflict history
      this.addToHistory({
        currentState,
        incomingState,
        resolvedState,
        conflicts: conflictResult.conflicts,
        resolutionDetails
      })

      return {
        success: true,
        resolvedState,
        conflicts: conflictResult.conflicts,
        resolutionDetails
      }

    } catch (error) {
      return {
        success: false,
        error: error.message,
        conflicts: [],
        resolutionDetails: {
          ...resolutionDetails,
          error: error.message,
          resolutionTime: Date.now() - startTime
        }
      }
    }
  }

  /**
   * Last write wins resolution strategy
   */
  resolveLastWriteWins(currentState, incomingState, conflictResult) {
    // Simply return incoming state with updated timestamp
    return {
      ...incomingState,
      ngayCapNhat: new Date().toISOString(),
      version: (currentState.version || 0) + 1
    }
  }

  /**
   * First write wins resolution strategy
   */
  resolveFirstWriteWins(currentState, incomingState, conflictResult) {
    // Keep current state but update non-conflicting fields
    const resolvedState = { ...currentState }
    
    for (const field in incomingState) {
      const hasConflict = conflictResult.conflicts.some(c => c.field === field)
      if (!hasConflict) {
        resolvedState[field] = incomingState[field]
      }
    }

    return {
      ...resolvedState,
      ngayCapNhat: new Date().toISOString()
    }
  }

  /**
   * Deep merge resolution strategy
   */
  resolveMergeDeep(currentState, incomingState, conflictResult) {
    return this.deepMerge(currentState, incomingState)
  }

  /**
   * Shallow merge resolution strategy
   */
  resolveMergeShallow(currentState, incomingState, conflictResult) {
    return {
      ...currentState,
      ...incomingState,
      ngayCapNhat: new Date().toISOString(),
      version: (currentState.version || 0) + 1
    }
  }

  /**
   * Field-level priority resolution strategy
   */
  resolveFieldLevel(currentState, incomingState, conflictResult) {
    const resolvedState = { ...currentState }

    for (const conflict of conflictResult.conflicts) {
      if (conflict.type === CONFLICT_TYPES.FIELD_CONFLICT) {
        const currentPriority = this.getFieldPriority(conflict.field, conflict.currentValue)
        const incomingPriority = this.getFieldPriority(conflict.field, conflict.incomingValue)

        // Higher priority wins
        if (incomingPriority >= currentPriority) {
          resolvedState[conflict.field] = conflict.incomingValue
        }
      }
    }

    return {
      ...resolvedState,
      ngayCapNhat: new Date().toISOString(),
      version: (currentState.version || 0) + 1
    }
  }

  /**
   * Business rules resolution strategy
   */
  resolveBusinessRules(currentState, incomingState, conflictResult) {
    let resolvedState = { ...currentState }

    // Apply business-specific resolution rules
    for (const [field, rule] of Object.entries(this.businessRules)) {
      if (typeof rule === 'function') {
        try {
          const ruleResult = rule(currentState[field], incomingState[field], {
            currentState,
            incomingState,
            conflicts: conflictResult.conflicts
          })
          
          if (ruleResult !== undefined) {
            resolvedState[field] = ruleResult
          }
        } catch (error) {
          console.warn(`Business rule failed for field ${field}:`, error)
        }
      }
    }

    return {
      ...resolvedState,
      ngayCapNhat: new Date().toISOString(),
      version: (currentState.version || 0) + 1
    }
  }

  /**
   * Check if field has conflict
   */
  hasFieldConflict(currentValue, incomingValue) {
    if (currentValue === incomingValue) return false
    if (currentValue == null && incomingValue == null) return false
    if (currentValue == null || incomingValue == null) return true

    // Deep comparison for objects and arrays
    if (typeof currentValue === 'object' && typeof incomingValue === 'object') {
      return JSON.stringify(currentValue) !== JSON.stringify(incomingValue)
    }

    return currentValue !== incomingValue
  }

  /**
   * Get field conflict severity
   */
  getFieldConflictSeverity(field, currentValue, incomingValue) {
    const priority = this.fieldPriorities[field] || this.fieldPriorities.default
    
    if (priority >= 90) return 'critical'
    if (priority >= 75) return 'high'
    if (priority >= 60) return 'medium'
    return 'low'
  }

  /**
   * Get field priority for resolution
   */
  getFieldPriority(field, value) {
    let basePriority = this.fieldPriorities[field] || this.fieldPriorities.default
    
    // Boost priority for non-empty values
    if (value != null && value !== '' && value !== 0) {
      basePriority += 5
    }

    return basePriority
  }

  /**
   * Check if field is system field (should be ignored in conflicts)
   */
  isSystemField(field) {
    return ['id', 'createdAt', 'updatedAt', '__typename'].includes(field)
  }

  /**
   * Check business rule violations
   */
  checkBusinessRules(currentState, incomingState) {
    const violations = []

    // Example: Order status transition rules
    if (currentState.trangThaiDonHang && incomingState.trangThaiDonHang) {
      const validTransitions = {
        'CHO_XAC_NHAN': ['XAC_NHAN', 'HUY'],
        'XAC_NHAN': ['DANG_GIAO', 'HUY'],
        'DANG_GIAO': ['HOAN_THANH', 'TRA_HANG'],
        'HOAN_THANH': ['TRA_HANG'],
        'HUY': [],
        'TRA_HANG': []
      }

      const currentStatus = currentState.trangThaiDonHang
      const incomingStatus = incomingState.trangThaiDonHang
      
      if (currentStatus !== incomingStatus) {
        const allowedTransitions = validTransitions[currentStatus] || []
        if (!allowedTransitions.includes(incomingStatus)) {
          violations.push({
            type: CONFLICT_TYPES.BUSINESS_RULE_VIOLATION,
            field: 'trangThaiDonHang',
            currentValue: currentStatus,
            incomingValue: incomingStatus,
            severity: 'critical',
            rule: 'invalid_status_transition'
          })
        }
      }
    }

    return violations
  }

  /**
   * Deep merge two objects
   */
  deepMerge(target, source) {
    const result = { ...target }

    for (const key in source) {
      if (source[key] != null && typeof source[key] === 'object' && !Array.isArray(source[key])) {
        result[key] = this.deepMerge(result[key] || {}, source[key])
      } else {
        result[key] = source[key]
      }
    }

    return result
  }

  /**
   * Get modified fields between two states
   */
  getModifiedFields(oldState, newState) {
    const modified = []
    
    for (const field in newState) {
      if (oldState[field] !== newState[field]) {
        modified.push(field)
      }
    }

    return modified
  }

  /**
   * Add resolution to history
   */
  addToHistory(resolutionRecord) {
    this.conflictHistory.unshift(resolutionRecord)
    
    if (this.conflictHistory.length > this.maxHistorySize) {
      this.conflictHistory = this.conflictHistory.slice(0, this.maxHistorySize)
    }
  }

  /**
   * Get conflict resolution statistics
   */
  getStatistics() {
    const total = this.conflictHistory.length
    if (total === 0) return null

    const strategies = {}
    const conflictTypes = {}
    let totalResolutionTime = 0

    for (const record of this.conflictHistory) {
      const strategy = record.resolutionDetails.strategy
      strategies[strategy] = (strategies[strategy] || 0) + 1
      
      totalResolutionTime += record.resolutionDetails.resolutionTime || 0

      for (const conflict of record.conflicts) {
        conflictTypes[conflict.type] = (conflictTypes[conflict.type] || 0) + 1
      }
    }

    return {
      totalResolutions: total,
      averageResolutionTime: totalResolutionTime / total,
      strategiesUsed: strategies,
      conflictTypesEncountered: conflictTypes,
      entityName: this.entityName
    }
  }

  /**
   * Clear conflict history
   */
  clearHistory() {
    this.conflictHistory = []
  }
}

/**
 * Create conflict resolver instance
 * @param {Object} options - Configuration options
 * @returns {StateConflictResolver} Conflict resolver instance
 */
export function createConflictResolver(options = {}) {
  return new StateConflictResolver(options)
}

/**
 * Quick conflict resolution function
 * @param {Object} currentState - Current state
 * @param {Object} incomingState - Incoming state
 * @param {String} strategy - Resolution strategy
 * @param {Object} options - Options
 * @returns {Object} Resolution result
 */
export function resolveStateConflict(currentState, incomingState, strategy = RESOLUTION_STRATEGIES.LAST_WRITE_WINS, options = {}) {
  const resolver = new StateConflictResolver(options)
  return resolver.resolveConflicts(currentState, incomingState, strategy, options)
}
