import { privateApi } from './axiosAPI'

/**
 * Cart API Service
 * Integrates with backend GioHangController at /api/v1/cart
 * Provides comprehensive cart management functionality
 */
const CART_BASE_URL = '/cart'

const cartApi = {
  /**
   * Get current user's cart
   * @returns {Promise<Object>} API response with cart data
   */
  async getCurrentUserCart() {
    try {
      const response = await privateApi.get(CART_BASE_URL)
      return {
        success: true,
        data: response.data,
        message: 'Cart fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching cart:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Add product to cart
   * @param {Object} request - Add to cart request
   * @param {number} request.sanPhamChiTietId - Product variant ID
   * @param {number} request.soLuong - Quantity to add
   * @returns {Promise<Object>} API response
   */
  async addToCart(request) {
    try {
      const response = await privateApi.post(`${CART_BASE_URL}/add`, request)
      return {
        success: true,
        data: response.data,
        message: 'Product added to cart successfully'
      }
    } catch (error) {
      console.error('Error adding to cart:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Update item quantity in cart
   * @param {Object} request - Update quantity request
   * @param {number} request.gioHangChiTietId - Cart item ID
   * @param {number} request.soLuongMoi - New quantity
   * @returns {Promise<Object>} API response
   */
  async updateQuantity(request) {
    try {
      const response = await privateApi.put(`${CART_BASE_URL}/update-quantity`, request)
      return {
        success: true,
        data: response.data,
        message: 'Cart quantity updated successfully'
      }
    } catch (error) {
      console.error('Error updating cart quantity:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Remove item from cart
   * @param {number} cartItemId - Cart item ID to remove
   * @returns {Promise<Object>} API response
   */
  async removeFromCart(cartItemId) {
    try {
      const response = await privateApi.delete(`${CART_BASE_URL}/remove/${cartItemId}`)
      return {
        success: true,
        data: response.data,
        message: 'Item removed from cart successfully'
      }
    } catch (error) {
      console.error('Error removing from cart:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Clear entire cart
   * @returns {Promise<Object>} API response
   */
  async clearCart() {
    try {
      const response = await privateApi.delete(`${CART_BASE_URL}/clear`)
      return {
        success: true,
        data: response.data,
        message: 'Cart cleared successfully'
      }
    } catch (error) {
      console.error('Error clearing cart:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Validate cart for order conversion
   * @param {Object} request - Cart validation request
   * @param {number} request.expectedTotal - Expected total amount
   * @param {Array} request.items - Cart items to validate
   * @returns {Promise<Object>} Validation response
   */
  async validateCartForOrder(request) {
    try {
      const response = await privateApi.post(`${CART_BASE_URL}/validate-for-order`, request)
      return {
        success: true,
        data: response.data,
        message: 'Cart validation completed'
      }
    } catch (error) {
      console.error('Error validating cart:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Get cart conversion preview
   * @param {Object} request - Conversion preview request
   * @returns {Promise<Object>} Conversion preview response
   */
  async getConversionPreview(request) {
    try {
      const response = await privateApi.post(`${CART_BASE_URL}/conversion-preview`, request)
      return {
        success: true,
        data: response.data,
        message: 'Conversion preview generated successfully'
      }
    } catch (error) {
      console.error('Error getting conversion preview:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Convert cart to order
   * @param {Object} request - Cart to order conversion request
   * @returns {Promise<Object>} Order creation response
   */
  async convertToOrder(request) {
    try {
      const response = await privateApi.post(`${CART_BASE_URL}/convert-to-order`, request)
      return {
        success: true,
        data: response.data,
        message: 'Cart converted to order successfully'
      }
    } catch (error) {
      console.error('Error converting cart to order:', error.response?.data || error.message)
      throw error
    }
  },

  /**
   * Get cart statistics
   * @returns {Promise<Object>} Cart statistics
   */
  async getCartStats() {
    try {
      const response = await privateApi.get(`${CART_BASE_URL}/stats`)
      return {
        success: true,
        data: response.data,
        message: 'Cart statistics fetched successfully'
      }
    } catch (error) {
      console.error('Error fetching cart stats:', error.response?.data || error.message)
      throw error
    }
  }
}

export default cartApi
