import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import voucherApi from '@/apis/voucherApi'
import { useToast } from 'primevue/usetoast'

export const useVoucherStore = defineStore('voucher', () => {
  const toast = useToast()

  // State
  const vouchers = ref([])
  const currentVoucher = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const validationResults = ref({}) // Cache validation results by voucher code
  const appliedVouchers = ref([]) // Currently applied vouchers in order creation

  // Voucher search and selection
  const searchQuery = ref('')
  const searchResults = ref([])
  const searchLoading = ref(false)

  // Filters
  const filters = ref({
    status: 'all',
    type: 'all', // public/private
    dateRange: null,
    search: ''
  })

  // Voucher statuses
  const voucherStatuses = ref([
    { value: 'CHUA_DIEN_RA', label: 'Chưa diễn ra', severity: 'secondary', icon: 'pi pi-clock' },
    { value: 'DA_DIEN_RA', label: 'Đang diễn ra', severity: 'success', icon: 'pi pi-check-circle' },
    { value: 'KET_THUC', label: 'Kết thúc', severity: 'danger', icon: 'pi pi-times-circle' },
    { value: 'BI_HUY', label: 'Đã hủy', severity: 'secondary', icon: 'pi pi-ban' }
  ])

  // Computed
  const filteredVouchers = computed(() => {
    if (!vouchers.value) return []

    return vouchers.value.filter(voucher => {
      // Status filter
      if (filters.value.status !== 'all' && voucher.trangThai !== filters.value.status) {
        return false
      }

      // Type filter (public/private)
      if (filters.value.type !== 'all') {
        const isPrivate = voucher.phieuRiengTu === true
        if (filters.value.type === 'private' && !isPrivate) return false
        if (filters.value.type === 'public' && isPrivate) return false
      }

      // Search filter
      if (filters.value.search) {
        const searchTerm = filters.value.search.toLowerCase()
        const searchableText = `${voucher.maPhieuGiamGia} ${voucher.tenPhieuGiamGia || ''}`.toLowerCase()
        if (!searchableText.includes(searchTerm)) {
          return false
        }
      }

      return true
    })
  })

  const voucherStatusMap = computed(() => {
    const map = {}
    voucherStatuses.value.forEach(status => {
      map[status.value] = status
    })
    return map
  })

  // Actions
  const fetchVouchers = async (params = {}) => {
    loading.value = true
    error.value = null

    try {
      const response = await voucherApi.getAllVouchers(params)

      if (response.success) {
        vouchers.value = response.data
      } else {
        throw new Error(response.message || 'Failed to fetch vouchers')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tải danh sách voucher: ${err.message}`,
        life: 5000
      })
    } finally {
      loading.value = false
    }
  }

  const searchVouchers = async (query) => {
    if (!query || query.length < 2) {
      searchResults.value = []
      return
    }

    searchLoading.value = true

    try {
      const response = await voucherApi.searchVouchers(query)

      if (response.success) {
        searchResults.value = response.data
      } else {
        searchResults.value = []
      }
    } catch (err) {
      console.error('Error searching vouchers:', err)
      searchResults.value = []
    } finally {
      searchLoading.value = false
    }
  }

  const validateVoucher = async (voucherCode, customerId, orderTotal) => {
    if (!voucherCode || !customerId || !orderTotal) {
      return {
        valid: false,
        error: 'Missing required parameters for voucher validation'
      }
    }

    // Check cache first
    const cacheKey = `${voucherCode}_${customerId}_${orderTotal}`
    if (validationResults.value[cacheKey]) {
      return validationResults.value[cacheKey]
    }

    try {
      const response = await voucherApi.validateVoucher(voucherCode, customerId, orderTotal)

      const result = {
        valid: response.success && response.data.valid,
        voucher: response.data.voucher,
        discountAmount: response.data.discountAmount,
        error: response.data.error || (!response.success ? response.message : null)
      }

      // Cache the result
      validationResults.value[cacheKey] = result

      return result
    } catch (err) {
      const result = {
        valid: false,
        error: err.message || 'Voucher validation failed'
      }

      validationResults.value[cacheKey] = result
      return result
    }
  }

  const checkCustomerEligibility = async (voucherCode, customerId) => {
    try {
      const response = await voucherApi.checkEligibility(voucherCode, customerId)
      return response.success && response.data.eligible
    } catch (err) {
      console.error('Error checking voucher eligibility:', err)
      return false
    }
  }

  const addVoucherToOrder = (voucher, discountAmount) => {
    // Check if voucher is already applied
    const existingIndex = appliedVouchers.value.findIndex(v => v.voucher.maPhieuGiamGia === voucher.maPhieuGiamGia)

    if (existingIndex !== -1) {
      // Update existing voucher
      appliedVouchers.value[existingIndex] = { voucher, discountAmount }
    } else {
      // Add new voucher
      appliedVouchers.value.push({ voucher, discountAmount })
    }

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Voucher ${voucher.maPhieuGiamGia} đã được áp dụng`,
      life: 3000
    })
  }

  const removeVoucherFromOrder = (voucherCode) => {
    const index = appliedVouchers.value.findIndex(v => v.voucher.maPhieuGiamGia === voucherCode)

    if (index !== -1) {
      appliedVouchers.value.splice(index, 1)

      toast.add({
        severity: 'info',
        summary: 'Thông báo',
        detail: `Voucher ${voucherCode} đã được gỡ bỏ`,
        life: 3000
      })
    }
  }

  const clearAppliedVouchers = () => {
    appliedVouchers.value = []
  }

  const getTotalDiscount = computed(() => {
    return appliedVouchers.value.reduce((total, item) => {
      return total + (item.discountAmount || 0)
    }, 0)
  })

  const getAppliedVoucherCodes = computed(() => {
    return appliedVouchers.value.map(item => item.voucher.maPhieuGiamGia)
  })

  const setFilters = (newFilters) => {
    filters.value = { ...filters.value, ...newFilters }
  }

  const clearFilters = () => {
    filters.value = {
      status: 'all',
      type: 'all',
      dateRange: null,
      search: ''
    }
  }

  const clearValidationCache = () => {
    validationResults.value = {}
  }

  const clearError = () => {
    error.value = null
  }

  const setSearchQuery = (query) => {
    searchQuery.value = query
    if (query) {
      searchVouchers(query)
    } else {
      searchResults.value = []
    }
  }

  // Utility functions
  const getVoucherStatusInfo = (status) => {
    return voucherStatusMap.value[status] || { label: status, severity: 'secondary', icon: 'pi pi-question' }
  }

  const formatDiscountValue = (voucher) => {
    if (!voucher) return ''

    if (voucher.loaiPhieuGiamGia) {
      // Percentage discount
      return `${voucher.giaTriGiam}%`
    } else {
      // Fixed amount discount
      return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
      }).format(voucher.giaTriGiam)
    }
  }

  const calculateDiscountAmount = (voucher, orderTotal) => {
    if (!voucher || !orderTotal) return 0

    if (voucher.loaiPhieuGiamGia) {
      // Percentage discount
      return (orderTotal * voucher.giaTriGiam) / 100
    } else {
      // Fixed amount discount
      return Math.min(voucher.giaTriGiam, orderTotal)
    }
  }

  const isVoucherApplied = (voucherCode) => {
    return appliedVouchers.value.some(item => item.voucher.maPhieuGiamGia === voucherCode)
  }

  const getVoucherByCode = (voucherCode) => {
    return vouchers.value.find(voucher => voucher.maPhieuGiamGia === voucherCode)
  }

  return {
    // State
    vouchers,
    currentVoucher,
    loading,
    error,
    validationResults,
    appliedVouchers,
    searchQuery,
    searchResults,
    searchLoading,
    filters,
    voucherStatuses,

    // Computed
    filteredVouchers,
    voucherStatusMap,
    getTotalDiscount,
    getAppliedVoucherCodes,

    // Actions
    fetchVouchers,
    searchVouchers,
    validateVoucher,
    checkCustomerEligibility,
    addVoucherToOrder,
    removeVoucherFromOrder,
    clearAppliedVouchers,
    setFilters,
    clearFilters,
    clearValidationCache,
    clearError,
    setSearchQuery,

    // Utilities
    getVoucherStatusInfo,
    formatDiscountValue,
    calculateDiscountAmount,
    isVoucherApplied,
    getVoucherByCode
  }
})
