import { computed } from 'vue'

/**
 * Composable for calculating severity levels and icons for ThongKe dashboard cards
 * Supports different calculation types: percentage, growth, inventory, completion
 */
export function useThongKeCardSeverity(value, type = 'percentage', options = {}) {
  const {
    successThreshold = 80,
    warningThreshold = 60,
    dangerThreshold = 20,
    invertLogic = false
  } = options

  const severity = computed(() => {
    const numValue = typeof value.value === 'number' ? value.value : 0

    switch (type) {
      case 'growth':
        // For growth metrics (can be positive or negative)
        if (numValue > 0) return 'success'
        if (numValue < 0) return 'danger'
        return 'info'

      case 'inventory': {
        // For inventory metrics (lower percentage is better)
        const inventoryPercentage = numValue
        if (inventoryPercentage > dangerThreshold) return 'danger'
        if (inventoryPercentage > warningThreshold / 3) return 'warning' // Adjusted for inventory
        return 'success'
      }

      case 'completion':
      case 'percentage':
      default:
        // For completion rates and general percentages (higher is better)
        if (invertLogic) {
          // Inverted logic: lower values are better
          if (numValue <= dangerThreshold) return 'success'
          if (numValue <= warningThreshold) return 'warning'
          return 'danger'
        } else {
          // Normal logic: higher values are better
          if (numValue >= successThreshold) return 'success'
          if (numValue >= warningThreshold) return 'warning'
          return 'danger'
        }
    }
  })

  const icon = computed(() => {
    switch (type) {
      case 'growth': {
        const numValue = typeof value.value === 'number' ? value.value : 0
        if (numValue > 0) return 'pi pi-arrow-up'
        if (numValue < 0) return 'pi pi-arrow-down'
        return 'pi pi-minus'
      }

      case 'inventory':
        switch (severity.value) {
          case 'success': return 'pi pi-check-circle'
          case 'warning': return 'pi pi-exclamation-triangle'
          case 'danger': return 'pi pi-times-circle'
          default: return 'pi pi-info-circle'
        }

      case 'completion':
      case 'percentage':
      default:
        switch (severity.value) {
          case 'success': return 'pi pi-check-circle'
          case 'warning': return 'pi pi-exclamation-triangle'
          case 'danger': return 'pi pi-times-circle'
          default: return 'pi pi-info-circle'
        }
    }
  })

  return {
    severity,
    icon
  }
}

/**
 * Specific composable for revenue growth severity calculation
 */
export function useRevenueGrowthSeverity(growthValue) {
  return useThongKeCardSeverity(growthValue, 'growth')
}

/**
 * Specific composable for completion rate severity calculation
 */
export function useCompletionRateSeverity(completionRate) {
  return useThongKeCardSeverity(completionRate, 'completion')
}

/**
 * Specific composable for inventory severity calculation
 */
export function useInventorySeverity(inventoryData) {
  const inventoryPercentage = computed(() => {
    if (!inventoryData.value || inventoryData.value.tongSo === 0) return 0
    return (inventoryData.value.sapHetHang / inventoryData.value.tongSo) * 100
  })

  return useThongKeCardSeverity(inventoryPercentage, 'inventory')
}

/**
 * Specific composable for customer retention severity calculation
 */
export function useRetentionSeverity(retentionRate) {
  return useThongKeCardSeverity(retentionRate, 'percentage')
}
