import { ref, computed } from 'vue'

/**
 * Composable for standardized DataTable auto-sorting functionality
 * Provides consistent newest-first sorting across all DataTable components
 */
export function useDataTableSorting(options = {}) {
  const {
    defaultSortField = 'ngayTao',
    defaultSortOrder = -1, // -1 for descending (newest first), 1 for ascending
    enableUserOverride = true
  } = options

  // Reactive sorting state
  const sortField = ref(defaultSortField)
  const sortOrder = ref(defaultSortOrder)
  const multiSortMeta = ref([
    {
      field: defaultSortField,
      order: defaultSortOrder
    }
  ])

  /**
   * Get DataTable props for auto-sorting
   * @returns {Object} Props to be spread into DataTable component
   */
  const getDataTableSortProps = () => {
    const props = {
      sortField: sortField.value,
      sortOrder: sortOrder.value,
      multiSortMeta: multiSortMeta.value
    }

    // Add removableSort only if user override is enabled
    if (enableUserOverride) {
      props.removableSort = true
    }

    return props
  }

  /**
   * Handle sort change events from DataTable
   * @param {Object} event - Sort event from DataTable
   */
  const onSort = (event) => {
    if (!enableUserOverride) return

    sortField.value = event.sortField
    sortOrder.value = event.sortOrder
    
    if (event.multiSortMeta) {
      multiSortMeta.value = event.multiSortMeta
    }
  }

  /**
   * Reset sorting to default values
   */
  const resetSort = () => {
    sortField.value = defaultSortField
    sortOrder.value = defaultSortOrder
    multiSortMeta.value = [
      {
        field: defaultSortField,
        order: defaultSortOrder
      }
    ]
  }

  /**
   * Apply sorting to a data array (for client-side sorting)
   * @param {Array} data - Array of data to sort
   * @returns {Array} Sorted array
   */
  const applySorting = (data) => {
    if (!data || !Array.isArray(data)) return []

    return [...data].sort((a, b) => {
      const field = sortField.value
      const order = sortOrder.value

      let aVal = getNestedValue(a, field)
      let bVal = getNestedValue(b, field)

      // Handle date fields specifically
      if (field === 'ngayTao' || field === 'ngayCapNhat' || field.includes('ngay')) {
        aVal = aVal ? new Date(aVal) : new Date(0)
        bVal = bVal ? new Date(bVal) : new Date(0)
      }

      // Handle null/undefined values
      if (aVal == null && bVal == null) return 0
      if (aVal == null) return order
      if (bVal == null) return -order

      // Compare values
      if (aVal < bVal) return -order
      if (aVal > bVal) return order
      return 0
    })
  }

  /**
   * Get nested object value by dot notation
   * @param {Object} obj - Object to get value from
   * @param {String} path - Dot notation path (e.g., 'user.name')
   * @returns {*} Value at path
   */
  const getNestedValue = (obj, path) => {
    return path.split('.').reduce((current, key) => current?.[key], obj)
  }

  /**
   * Check if current sorting is the default
   * @returns {Boolean} True if using default sorting
   */
  const isDefaultSort = computed(() => {
    return sortField.value === defaultSortField && sortOrder.value === defaultSortOrder
  })

  /**
   * Get sort indicator for UI display
   * @returns {Object} Sort indicator info
   */
  const getSortIndicator = computed(() => {
    if (isDefaultSort.value) {
      return {
        field: defaultSortField,
        order: defaultSortOrder,
        label: defaultSortOrder === -1 ? 'Mới nhất trước' : 'Cũ nhất trước',
        icon: defaultSortOrder === -1 ? 'pi pi-sort-amount-down' : 'pi pi-sort-amount-up'
      }
    }

    return {
      field: sortField.value,
      order: sortOrder.value,
      label: sortOrder.value === -1 ? 'Giảm dần' : 'Tăng dần',
      icon: sortOrder.value === -1 ? 'pi pi-sort-amount-down' : 'pi pi-sort-amount-up'
    }
  })

  return {
    // State
    sortField,
    sortOrder,
    multiSortMeta,
    
    // Computed
    isDefaultSort,
    getSortIndicator,
    
    // Methods
    getDataTableSortProps,
    onSort,
    resetSort,
    applySorting,
    
    // Utilities
    getNestedValue
  }
}
