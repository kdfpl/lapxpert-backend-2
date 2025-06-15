import { privateApi } from './axiosAPI'

// Note: SerialNumberController uses /api/v1/serial-numbers
// privateApi base URL is already /api/v1, so we just need /serial-numbers
const SERIAL_NUMBER_BASE_URL = '/serial-numbers'

/**
 * Serial Number API service for managing product serial numbers
 * Handles creation, updates, and bulk operations for serial numbers
 */
const serialNumberApi = {
  /**
   * Create a new serial number
   * @param {Object} serialNumberData - Serial number data
   * @param {string} serialNumberData.serialNumberValue - The serial number value
   * @param {number} serialNumberData.sanPhamChiTietId - Product variant ID
   * @param {string} serialNumberData.trangThai - Status (AVAILABLE, RESERVED, SOLD, etc.)
   * @returns {Promise<Object>} Created serial number
   */
  async createSerialNumber(serialNumberData) {
    try {
      const response = await privateApi.post(SERIAL_NUMBER_BASE_URL, serialNumberData)
      return response.data
    } catch (error) {
      console.error('Error creating serial number:', error.response?.data || error.message)
      throw new Error(error.response?.data?.message || 'Không thể tạo serial number')
    }
  },

  /**
   * Create multiple serial numbers for a variant
   * @param {number} variantId - Product variant ID
   * @param {Array<Object>} serialNumbers - Array of serial number data
   * @returns {Promise<Array>} Created serial numbers
   */
  async createMultipleSerialNumbers(variantId, serialNumbers) {
    try {
      const promises = serialNumbers.map(serialData =>
        this.createSerialNumber({
          serialNumberValue: serialData.serialNumber || serialData.serialNumberValue,
          sanPhamChiTietId: variantId,
          trangThai: serialData.trangThai || 'AVAILABLE'
        })
      )

      const results = await Promise.allSettled(promises)

      const successful = []
      const failed = []

      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          successful.push(result.value)
        } else {
          failed.push({
            serialNumber: serialNumbers[index].serialNumber || serialNumbers[index].serialNumberValue,
            error: result.reason.message
          })
        }
      })

      return { successful, failed }
    } catch (error) {
      console.error('Error creating multiple serial numbers:', error)
      throw error
    }
  },

  /**
   * Generate serial numbers for a product variant
   * @param {number} variantId - Product variant ID
   * @param {number} quantity - Number of serial numbers to generate
   * @param {string} pattern - Pattern for serial number generation
   * @returns {Promise<Array>} Generated serial numbers
   */
  async generateSerialNumbers(variantId, quantity, pattern) {
    try {
      const response = await privateApi.post(`${SERIAL_NUMBER_BASE_URL}/generate`, null, {
        params: { variantId, quantity, pattern }
      })
      return response.data
    } catch (error) {
      console.error('Error generating serial numbers:', error.response?.data || error.message)
      throw new Error(error.response?.data?.message || 'Không thể tạo serial numbers tự động')
    }
  },

  /**
   * Get serial numbers for a product variant
   * @param {number} variantId - Product variant ID
   * @returns {Promise<Array>} Serial numbers for the variant
   */
  async getSerialNumbersByVariant(variantId) {
    try {
      // Use the new dedicated endpoint for getting all serial numbers by variant
      const response = await privateApi.get(`${SERIAL_NUMBER_BASE_URL}/variant/${variantId}`)
      // Extract the data from the API response
      return response.data?.data || []
    } catch (error) {
      console.error('Error fetching serial numbers:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Get serial numbers for a specific order
   * @param {string} orderId - Order ID
   * @returns {Promise<Array>} Serial numbers for the order
   */
  async getSerialNumbersByOrder(orderId) {
    try {
      const response = await privateApi.get(`${SERIAL_NUMBER_BASE_URL}/order/${orderId}`)
      return response.data?.data || []
    } catch (error) {
      console.error('Error fetching serial numbers for order:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Update serial number (including value and metadata)
   * @param {number} serialNumberId - Serial number ID
   * @param {Object} serialNumberData - Updated serial number data
   * @returns {Promise<Object>} Updated serial number
   */
  async updateSerialNumber(serialNumberId, serialNumberData) {
    try {
      const response = await privateApi.put(`${SERIAL_NUMBER_BASE_URL}/${serialNumberId}`, serialNumberData)
      return response.data?.data || response.data
    } catch (error) {
      console.error('Error updating serial number:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Update serial number status
   * @param {number} serialNumberId - Serial number ID
   * @param {string} newStatus - New status
   * @param {string} reason - Reason for status change
   * @returns {Promise<Object>} Updated serial number
   */
  async updateSerialNumberStatus(serialNumberId, newStatus, reason) {
    try {
      const response = await privateApi.patch(`${SERIAL_NUMBER_BASE_URL}/${serialNumberId}/status`, null, {
        params: { newStatus, reason }
      })
      return response.data
    } catch (error) {
      console.error('Error updating serial number status:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Delete a serial number
   * @param {number} serialNumberId - Serial number ID
   * @param {string} reason - Reason for deletion
   * @returns {Promise<void>}
   */
  async deleteSerialNumber(serialNumberId, reason) {
    try {
      const params = reason ? { reason } : {}
      await privateApi.delete(`${SERIAL_NUMBER_BASE_URL}/${serialNumberId}`, {
        params
      })
    } catch (error) {
      console.error('Error deleting serial number:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Get available quantity for a product variant
   * @param {number} variantId - Product variant ID
   * @returns {Promise<number>} Available quantity
   */
  async getAvailableQuantity(variantId) {
    try {
      const response = await privateApi.get(`${SERIAL_NUMBER_BASE_URL}/variant/${variantId}/available-quantity`)
      return response.data
    } catch (error) {
      console.error('Error fetching available quantity:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Get serial number by its value
   * @param {string} serialNumberValue - Serial number value to search for
   * @returns {Promise<Object>} Serial number data
   */
  async getBySerialNumber(serialNumberValue) {
    try {
      const response = await privateApi.get(`${SERIAL_NUMBER_BASE_URL}/by-value/${encodeURIComponent(serialNumberValue)}`)
      return response.data?.data || response.data
    } catch (error) {
      console.error('Error fetching serial number by value:', error.response?.data || error.message)
      // Return null if not found instead of throwing error
      if (error.response?.status === 404) {
        return null
      }
      throw error
    }
  }
}

export default serialNumberApi
