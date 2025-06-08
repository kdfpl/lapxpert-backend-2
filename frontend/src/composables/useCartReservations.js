import { ref } from 'vue'
import { privateApi } from '@/apis/axiosAPI'

/**
 * Composable for managing cart-level inventory reservations
 * Provides backend integration for serial number reservations during cart operations
 * Fixed import error - now uses direct privateApi import
 */
export function useCartReservations() {
  const loading = ref(false)
  const error = ref(null)

  /**
   * Make API call with consistent error handling
   */
  const apiCall = async (url, options = {}) => {
    try {
      const response = await privateApi({
        url,
        method: options.method || 'GET',
        data: options.body ? JSON.parse(options.body) : options.data,
        headers: options.headers,
        ...options
      })

      return {
        success: true,
        data: response.data,
        message: 'Request successful'
      }
    } catch (error) {
      console.error('API call failed:', error)
      throw {
        success: false,
        data: null,
        message: error.response?.data?.message || error.message || 'Request failed'
      }
    }
  }

  /**
   * Reserve serial numbers for cart
   */
  const reserveForCart = async (request) => {
    loading.value = true
    error.value = null

    try {
      const response = await apiCall('/cart/reservations/reserve', {
        method: 'POST',
        data: request,
        headers: {
          'Content-Type': 'application/json'
        }
      })

      if (response.success) {
        return response.data
      } else {
        error.value = response.message || 'Không thể đặt trước sản phẩm'
        throw new Error(error.value)
      }
    } catch (err) {
      error.value = err.message || 'Lỗi kết nối'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Release all cart reservations for a tab
   */
  const releaseCartReservations = async (tabId) => {
    loading.value = true
    error.value = null

    try {
      const response = await apiCall(`/cart/reservations/release/${tabId}`, {
        method: 'DELETE'
      })

      if (response.success) {
        return response.data
      } else {
        error.value = response.message || 'Không thể hủy đặt trước'
        throw new Error(error.value)
      }
    } catch (err) {
      error.value = err.message || 'Lỗi kết nối'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Release specific items from cart
   */
  const releaseSpecificItems = async (tabId, variantId, quantity) => {
    loading.value = true
    error.value = null

    try {
      const response = await apiCall(`/cart/reservations/release/${tabId}/variant/${variantId}?quantity=${quantity}`, {
        method: 'DELETE'
      })

      if (response.success) {
        return response.data
      } else {
        error.value = response.message || 'Không thể hủy đặt trước'
        throw new Error(error.value)
      }
    } catch (err) {
      error.value = err.message || 'Lỗi kết nối'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Get real-time inventory availability
   */
  const getInventoryAvailability = async (variantId) => {
    loading.value = true
    error.value = null

    try {
      const response = await apiCall(`/cart/reservations/availability/${variantId}`)

      if (response.success) {
        return response.data
      } else {
        error.value = response.message || 'Không thể lấy thông tin tồn kho'
        throw new Error(error.value)
      }
    } catch (err) {
      error.value = err.message || 'Lỗi kết nối'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    reserveForCart,
    releaseCartReservations,
    releaseSpecificItems,
    getInventoryAvailability
  }
}
