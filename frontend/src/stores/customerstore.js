import { defineStore } from 'pinia'
import userApi from '@/apis/user'

export const useCustomerStore = defineStore('customer', {
  state: () => ({
    customers: [], // Ensure this is always initialized as an array
    loading: false,
    error: null,
    currentCustomer: null, // Add this to store the currently edited customer
  }),
  actions: {
    async fetchCustomers(params = {}) {
      this.loading = true
      try {
        console.log('CustomerStore: fetchCustomers called with params:', params)
        const response = await userApi.getCustomers(params)
        console.log('CustomerStore: API response:', response)

        // If this is a search request, return the filtered results directly
        if (params.search) {
          console.log('CustomerStore: Returning search results:', response.data)
          return response.data || []
        }

        // Otherwise, update the store's customers array
        this.customers = response.data || [] // Ensure we always have an array
        return this.customers
      } catch (error) {
        console.error('CustomerStore: Error in fetchCustomers:', error)
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchCustomerById(id) {
      this.loading = true
      try {
        const response = await userApi.getCustomerById(id)
        this.currentCustomer = response.data // Store the current customer separately
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async createCustomer(customerData) {
      this.loading = true
      try {
        const response = await userApi.createCustomer(customerData)
        this.customers.push(response.data)
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async updateCustomer(id, customerData) {
      this.loading = true
      try {
        const response = await userApi.updateCustomer(id, customerData)
        const index = this.customers.findIndex((c) => c.id === id)
        if (index !== -1) {
          this.customers[index] = response.data
        }
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async deleteCustomer(id) {
      this.loading = true
      try {
        await userApi.deleteCustomer(id)
        const index = this.customers.findIndex((c) => c.id === id)
        if (index !== -1) {
          this.customers[index].trangThai = false
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async restoreCustomer(id) {
      this.loading = true
      try {
        await userApi.restoreCustomer(id)
        const index = this.customers.findIndex((c) => c.id === id)
        if (index !== -1) {
          this.customers[index].trangThai = true
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
  },
  getters: {
    activeCustomers: (state) => {
      if (!Array.isArray(state.customers)) return [] // Safety check
      return state.customers.filter((c) => c.trangThai === 'HOAT_DONG' || c.trangThai === true)
    },
    inactiveCustomers: (state) => {
      if (!Array.isArray(state.customers)) return [] // Safety check
      return state.customers.filter((c) => c.trangThai === 'KHONG_HOAT_DONG' || c.trangThai === false)
    },
  },
})
