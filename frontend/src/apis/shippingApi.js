import { privateApi } from './axiosAPI'

// Shipping API base URL - privateApi already includes /api/v1
const SHIPPING_BASE_URL = '/shipping'

/**
 * Shipping API service for GHTK integration
 * Handles shipping fee calculation and delivery management
 */
const shippingApi = {
  /**
   * Calculate shipping fee using GHTK API
   * @param {Object} shippingRequest - Shipping calculation request
   * @param {string} shippingRequest.pickProvince - Pickup province
   * @param {string} shippingRequest.pickDistrict - Pickup district
   * @param {string} shippingRequest.pickWard - Pickup ward
   * @param {string} shippingRequest.pickAddress - Pickup address
   * @param {string} shippingRequest.province - Delivery province
   * @param {string} shippingRequest.district - Delivery district
   * @param {string} shippingRequest.ward - Delivery ward
   * @param {string} shippingRequest.address - Delivery address
   * @param {number} shippingRequest.weight - Package weight in grams
   * @param {number} shippingRequest.value - Package value in VND
   * @param {string} shippingRequest.transport - Transport type (road/fly)
   * @param {Array} shippingRequest.tags - Additional service tags
   * @returns {Promise<Object>} API response with shipping fee data
   */
  async calculateShippingFee(shippingRequest) {
    try {
      const response = await privateApi.post(`${SHIPPING_BASE_URL}/calculate`, shippingRequest)

      return {
        success: true,
        data: response.data,
        message: 'Shipping fee calculated successfully'
      }
    } catch (error) {
      console.error('Error calculating shipping fee:', error)
      return {
        success: false,
        data: {
          fee: 0,
          isManualOverride: true,
          errorMessage: error.response?.data?.message || error.message || 'Failed to calculate shipping fee'
        },
        message: error.response?.data?.message || error.message || 'Failed to calculate shipping fee'
      }
    }
  },

  /**
   * Get available shipping services for a route
   * @param {Object} routeRequest - Route information
   * @param {string} routeRequest.pickProvince - Pickup province
   * @param {string} routeRequest.pickDistrict - Pickup district
   * @param {string} routeRequest.province - Delivery province
   * @param {string} routeRequest.district - Delivery district
   * @returns {Promise<Object>} API response with available services
   */
  async getAvailableServices(routeRequest) {
    try {
      const response = await privateApi.post(`${SHIPPING_BASE_URL}/services`, routeRequest)

      return {
        success: true,
        data: response.data,
        message: 'Available services fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching available services:', error)
      return {
        success: false,
        data: [],
        message: error.response?.data?.message || error.message || 'Failed to fetch available services'
      }
    }
  },

  /**
   * Validate delivery address
   * @param {Object} addressRequest - Address validation request
   * @param {string} addressRequest.province - Province name
   * @param {string} addressRequest.district - District name
   * @param {string} addressRequest.ward - Ward name
   * @param {string} addressRequest.address - Full address
   * @returns {Promise<Object>} API response with address validation result
   */
  async validateAddress(addressRequest) {
    try {
      const response = await privateApi.post(`${SHIPPING_BASE_URL}/validate-address`, addressRequest)

      return {
        success: true,
        data: response.data,
        message: 'Address validated successfully'
      }
    } catch (error) {
      console.error('Error validating address:', error)
      return {
        success: false,
        data: {
          isValid: false,
          suggestions: [],
          errorMessage: error.response?.data?.message || error.message
        },
        message: error.response?.data?.message || error.message || 'Failed to validate address'
      }
    }
  },

  /**
   * Get estimated delivery time
   * @param {Object} deliveryRequest - Delivery time estimation request
   * @param {string} deliveryRequest.pickProvince - Pickup province
   * @param {string} deliveryRequest.pickDistrict - Pickup district
   * @param {string} deliveryRequest.province - Delivery province
   * @param {string} deliveryRequest.district - Delivery district
   * @param {string} deliveryRequest.transport - Transport type
   * @returns {Promise<Object>} API response with delivery time estimation
   */
  async getEstimatedDeliveryTime(deliveryRequest) {
    try {
      const response = await privateApi.post(`${SHIPPING_BASE_URL}/delivery-time`, deliveryRequest)

      return {
        success: true,
        data: response.data,
        message: 'Delivery time estimated successfully'
      }
    } catch (error) {
      console.error('Error estimating delivery time:', error)
      return {
        success: false,
        data: {
          estimatedDays: 0,
          estimatedTime: 'Không xác định',
          errorMessage: error.response?.data?.message || error.message
        },
        message: error.response?.data?.message || error.message || 'Failed to estimate delivery time'
      }
    }
  },

  /**
   * Create shipping order (for future use)
   * @param {Object} orderRequest - Shipping order creation request
   * @returns {Promise<Object>} API response with shipping order data
   */
  async createShippingOrder(orderRequest) {
    try {
      const response = await privateApi.post(`${SHIPPING_BASE_URL}/create-order`, orderRequest)

      return {
        success: true,
        data: response.data,
        message: 'Shipping order created successfully'
      }
    } catch (error) {
      console.error('Error creating shipping order:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to create shipping order'
      }
    }
  },

  /**
   * Track shipping order (for future use)
   * @param {string} trackingNumber - Tracking number
   * @returns {Promise<Object>} API response with tracking data
   */
  async trackShippingOrder(trackingNumber) {
    try {
      const response = await privateApi.get(`${SHIPPING_BASE_URL}/track/${trackingNumber}`)

      return {
        success: true,
        data: response.data,
        message: 'Tracking information fetched successfully'
      }
    } catch (error) {
      console.error('Error tracking shipping order:', error)
      return {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Failed to track shipping order'
      }
    }
  },

  /**
   * Get shipping configuration and settings
   * @returns {Promise<Object>} API response with shipping configuration
   */
  async getShippingConfig() {
    try {
      const response = await privateApi.get(`${SHIPPING_BASE_URL}/config`)

      return {
        success: true,
        data: response.data,
        message: 'Shipping configuration fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching shipping configuration:', error)
      return {
        success: false,
        data: {
          defaultWeight: 500, // 500g default
          defaultTransport: 'road',
          pickupAddress: {
            province: 'Hà Nội',
            district: 'Cầu Giấy',
            ward: 'Dịch Vọng',
            address: 'Số 1 Đại Cồ Việt'
          }
        },
        message: error.response?.data?.message || error.message || 'Failed to fetch shipping configuration'
      }
    }
  },

  /**
   * Calculate shipping fee with fallback to manual entry
   * This is a convenience method that handles errors gracefully
   * @param {Object} shippingRequest - Shipping calculation request
   * @returns {Promise<Object>} Always returns a valid response with fallback data
   */
  async calculateShippingFeeWithFallback(shippingRequest) {
    try {
      const result = await this.calculateShippingFee(shippingRequest)

      if (result.success) {
        return result
      } else {
        // Return fallback data for manual entry
        return {
          success: true,
          data: {
            fee: 0,
            isManualOverride: true,
            isAutoCalculated: false,
            errorMessage: result.message,
            fallbackReason: 'API calculation failed, manual entry required'
          },
          message: 'Shipping fee calculation failed, please enter manually'
        }
      }
    } catch (error) {
      console.error('Error in shipping fee calculation with fallback:', error)
      return {
        success: true,
        data: {
          fee: 0,
          isManualOverride: true,
          isAutoCalculated: false,
          errorMessage: error.message,
          fallbackReason: 'Network error, manual entry required'
        },
        message: 'Network error, please enter shipping fee manually'
      }
    }
  }
}

export default shippingApi
