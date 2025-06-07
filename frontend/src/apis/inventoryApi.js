import { privateApi } from './axiosAPI'

const INVENTORY_BASE_URL = '/inventory'

/**
 * Inventory API service for inventory management and monitoring
 * Handles reservation statistics, availability checking, and cleanup operations
 */
const inventoryApi = {
  /**
   * Get real-time inventory reservation statistics
   * @returns {Promise<Object>} API response with reservation statistics
   */
  async getReservationStats() {
    try {
      const response = await privateApi.get(`${INVENTORY_BASE_URL}/stats`)

      return {
        success: true,
        data: {
          totalReserved: response.data.totalReserved || 0,
          posReserved: response.data.posReserved || 0,
          onlineReserved: response.data.onlineReserved || 0
        },
        message: 'Reservation statistics fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching reservation statistics:', error)
      return {
        success: false,
        data: {
          totalReserved: 0,
          posReserved: 0,
          onlineReserved: 0
        },
        message: error.response?.data?.message || error.message || 'Failed to fetch reservation statistics'
      }
    }
  },

  /**
   * Check available quantity for a specific product variant
   * @param {number} productVariantId - Product variant ID
   * @returns {Promise<Object>} API response with available quantity
   */
  async getAvailableQuantity(productVariantId) {
    try {
      const response = await privateApi.get(`${INVENTORY_BASE_URL}/available/${productVariantId}`)

      return {
        success: true,
        data: {
          availableQuantity: response.data || 0,
          productVariantId: productVariantId
        },
        message: 'Available quantity fetched successfully'
      }
    } catch (error) {
      console.error(`Error fetching available quantity for product variant ${productVariantId}:`, error)
      return {
        success: false,
        data: {
          availableQuantity: 0,
          productVariantId: productVariantId
        },
        message: error.response?.data?.message || error.message || 'Failed to fetch available quantity'
      }
    }
  },

  /**
   * Manually trigger cleanup of expired reservations (Admin only)
   * @returns {Promise<Object>} API response with cleanup result
   */
  async cleanupExpiredReservations() {
    try {
      const response = await privateApi.post(`${INVENTORY_BASE_URL}/cleanup-expired`)

      return {
        success: true,
        data: {
          message: response.data || 'Cleanup completed successfully'
        },
        message: 'Expired reservations cleanup completed successfully'
      }
    } catch (error) {
      console.error('Error during manual cleanup of expired reservations:', error)
      return {
        success: false,
        data: {
          message: 'Cleanup failed'
        },
        message: error.response?.data?.message || error.message || 'Failed to cleanup expired reservations'
      }
    }
  },

  /**
   * Check multiple product variants availability in batch
   * @param {Array<number>} productVariantIds - Array of product variant IDs
   * @returns {Promise<Object>} API response with availability map
   */
  async getBatchAvailability(productVariantIds) {
    try {
      const availabilityPromises = productVariantIds.map(id => this.getAvailableQuantity(id))
      const results = await Promise.all(availabilityPromises)

      const availabilityMap = {}
      results.forEach((result, index) => {
        const productVariantId = productVariantIds[index]
        availabilityMap[productVariantId] = result.success ? result.data.availableQuantity : 0
      })

      return {
        success: true,
        data: availabilityMap,
        message: 'Batch availability check completed successfully'
      }
    } catch (error) {
      console.error('Error during batch availability check:', error)
      return {
        success: false,
        data: {},
        message: error.message || 'Failed to check batch availability'
      }
    }
  },

  /**
   * Monitor inventory changes for real-time updates
   * @param {Array<number>} productVariantIds - Product variants to monitor
   * @param {Function} callback - Callback function for updates
   * @returns {Function} Cleanup function to stop monitoring
   */
  startInventoryMonitoring(productVariantIds, callback) {
    let isMonitoring = true
    let intervalId

    const checkInventory = async () => {
      if (!isMonitoring) return

      try {
        const result = await this.getBatchAvailability(productVariantIds)
        if (result.success && callback) {
          callback(result.data)
        }
      } catch (error) {
        console.error('Error during inventory monitoring:', error)
      }
    }

    // Initial check
    checkInventory()

    // Set up periodic checking (every 30 seconds)
    intervalId = setInterval(checkInventory, 30000)

    // Return cleanup function
    return () => {
      isMonitoring = false
      if (intervalId) {
        clearInterval(intervalId)
      }
    }
  }
}

export default inventoryApi
