import { defineStore } from 'pinia'
import userApi from '@/apis/user'

export const useStaffStore = defineStore('staff', {
  state: () => ({
    staff: [],
    loading: false,
    error: null,
  }),
  actions: {
    async fetchStaff() {
      this.loading = true
      try {
        const response = await userApi.getStaff()
        this.staff = response.data || [] // Ensure we always have an array
      } catch (error) {
        this.error = error.response?.data?.message || error.message
      } finally {
        this.loading = false
      }
    },
    async fetchStaffById(id) {
      this.loading = true
      try {
        const response = await userApi.getStaffById(id)
        this.currentstaff = response.data // Store the current customer separately
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async createStaff(staffData) {
      this.loading = true
      try {
        const response = await userApi.createStaff(staffData)
        this.staff.push(response.data)
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async updateStaff(id, staffData) {
      this.loading = true
      try {
        const response = await userApi.updateStaff(id, staffData)
        const index = this.staff.findIndex((s) => s.id === id)
        if (index !== -1) {
          this.staff[index] = response.data
        }
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async deleteStaff(id) {
      this.loading = true
      try {
        await userApi.deleteStaff(id)
        const index = this.staff.findIndex((s) => s.id === id)
        if (index !== -1) {
          this.staff[index].trangThai = false
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    async restoreStaff(id) {
      this.loading = true
      try {
        await userApi.restoreStaff(id)
        const index = this.staff.findIndex((s) => s.id === id)
        if (index !== -1) {
          this.staff[index].trangThai = true
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
    activeStaff: (state) => state.staff.filter((s) => s.trangThai === 'HOAT_DONG' || s.trangThai === true),
    inactiveStaff: (state) => state.staff.filter((s) => s.trangThai === 'KHONG_HOAT_DONG' || s.trangThai === false),
  },
})
