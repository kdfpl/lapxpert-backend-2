import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import orderApi from '@/apis/orderApi'
import { useToast } from 'primevue/usetoast'
import { useOrderCache } from '@/composables/useOrderCache'
import { usePerformanceOptimization } from '@/composables/usePerformanceOptimization'

export const useOrderStore = defineStore('order', () => {
  const toast = useToast()

  // Performance optimization composables
  const orderCache = useOrderCache()
  const { debounce, deduplicateRequest, measurePerformance } = usePerformanceOptimization()

  // State
  const orders = ref([])
  const currentOrder = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const totalRecords = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(20)
  const auditHistory = ref({})

  // Multi-tab order management state
  const orderTabs = ref([])
  const activeTabId = ref(null)
  const tabCounter = ref(1)
  const reservedInventory = ref(new Map()) // Track reserved items per order

  // Filters
  const filters = ref({
    status: 'all',
    type: 'all', // ONLINE, TAI_QUAY, all
    dateRange: null,
    customer: null,
    staff: null,
    search: ''
  })

  // Order statuses from backend
  const orderStatuses = ref([
    { value: 'CHO_XAC_NHAN', label: 'Chờ xác nhận', severity: 'warning', icon: 'pi pi-clock' },
    { value: 'DA_XAC_NHAN', label: 'Đã xác nhận', severity: 'info', icon: 'pi pi-check' },
    { value: 'DANG_XU_LY', label: 'Đang chuẩn bị hàng', severity: 'info', icon: 'pi pi-cog' },
    { value: 'CHO_GIAO_HANG', label: 'Chờ giao hàng', severity: 'warning', icon: 'pi pi-package' },
    { value: 'DANG_GIAO_HANG', label: 'Đang giao hàng', severity: 'info', icon: 'pi pi-truck' },
    { value: 'DA_GIAO_HANG', label: 'Đã giao hàng', severity: 'success', icon: 'pi pi-check-circle' },
    { value: 'HOAN_THANH', label: 'Hoàn thành', severity: 'success', icon: 'pi pi-verified' },
    { value: 'DA_HUY', label: 'Đã hủy', severity: 'danger', icon: 'pi pi-times-circle' },
    { value: 'YEU_CAU_TRA_HANG', label: 'Yêu cầu trả hàng', severity: 'warning', icon: 'pi pi-undo' },
    { value: 'DA_TRA_HANG', label: 'Đã trả hàng', severity: 'warning', icon: 'pi pi-undo' },
    { value: 'THAT_BAI', label: 'Thất bại', severity: 'danger', icon: 'pi pi-exclamation-triangle' }
  ])

  const orderTypes = ref([
    { value: 'ONLINE', label: 'Đơn hàng online', icon: 'pi pi-globe' },
    { value: 'TAI_QUAY', label: 'Bán tại quầy', icon: 'pi pi-shop' }
  ])

  const paymentStatuses = ref([
    { value: 'CHUA_THANH_TOAN', label: 'Chưa thanh toán', severity: 'warn' },
    { value: 'DA_THANH_TOAN', label: 'Đã thanh toán', severity: 'success' },
    { value: 'THANH_TOAN_MOT_PHAN', label: 'Thanh toán một phần', severity: 'info' },
    { value: 'HOAN_TIEN', label: 'Hoàn tiền', severity: 'info' }
  ])

  // Cache management
  const lastFetchTime = ref(null)
  const cacheTimeout = 5 * 60 * 1000 // 5 minutes

  // Cache validation
  const isCacheValid = () => {
    if (!lastFetchTime.value) return false
    return Date.now() - lastFetchTime.value < cacheTimeout
  }

  // Computed
  const filteredOrders = computed(() => {
    if (!orders.value) return []

    return orders.value.filter(order => {
      // Status filter
      if (filters.value.status !== 'all' && order.trangThaiDonHang !== filters.value.status) {
        return false
      }

      // Type filter
      if (filters.value.type !== 'all' && order.loaiHoaDon !== filters.value.type) {
        return false
      }

      // Search filter
      if (filters.value.search) {
        const searchTerm = filters.value.search.toLowerCase()
        const searchableText = `${order.maHoaDon} ${order.khachHang?.hoTen || ''} ${order.nhanVien?.hoTen || ''}`.toLowerCase()
        if (!searchableText.includes(searchTerm)) {
          return false
        }
      }

      // Date range filter
      if (filters.value.dateRange && filters.value.dateRange.length === 2) {
        const orderDate = new Date(order.ngayTao)
        const [startDate, endDate] = filters.value.dateRange
        if (orderDate < startDate || orderDate > endDate) {
          return false
        }
      }

      return true
    })
  })

  const orderStatusMap = computed(() => {
    const map = {}
    orderStatuses.value.forEach(status => {
      map[status.value] = status
    })
    return map
  })

  const orderTypeMap = computed(() => {
    const map = {}
    orderTypes.value.forEach(type => {
      map[type.value] = type
    })
    return map
  })

  const paymentStatusMap = computed(() => {
    const map = {}
    paymentStatuses.value.forEach(status => {
      map[status.value] = status
    })
    return map
  })

  // Multi-tab computed properties
  const activeTab = computed(() => {
    return orderTabs.value.find(tab => tab.id === activeTabId.value)
  })

  const hasActiveTabs = computed(() => {
    return orderTabs.value.length > 0
  })

  const canCreateNewTab = computed(() => {
    return orderTabs.value.length < 5 // Limit to 5 concurrent orders
  })

  // Multi-tab order management actions
  const generateOrderCode = () => {
    const timestamp = Date.now().toString().slice(-6)
    const counter = tabCounter.value.toString().padStart(3, '0')
    return `HD${timestamp}${counter}`
  }

  const createNewOrderTab = () => {
    if (!canCreateNewTab.value) {
      toast.add({
        severity: 'warn',
        summary: 'Cảnh báo',
        detail: 'Không thể tạo thêm đơn hàng. Tối đa 5 đơn hàng cùng lúc.',
        life: 3000
      })
      return null
    }

    const newTab = {
      id: `tab_${Date.now()}_${tabCounter.value}`,
      maHoaDon: generateOrderCode(),
      loaiHoaDon: 'TAI_QUAY',
      khachHang: null,
      diaChiGiaoHang: null,
      giaohang: false,
      sanPhamList: [],
      voucherList: [],
      phuongThucThanhToan: null,
      tongTienHang: 0,
      giaTriGiamGiaVoucher: 0,
      phiVanChuyen: 0,
      tongThanhToan: 0,
      trangThaiDonHang: 'CHO_XAC_NHAN',
      trangThaiThanhToan: 'CHUA_THANH_TOAN',
      createdAt: new Date(),
      isModified: false
    }

    orderTabs.value.push(newTab)
    activeTabId.value = newTab.id
    tabCounter.value++

    return newTab
  }

  const closeOrderTab = (tabId) => {
    const tabIndex = orderTabs.value.findIndex(tab => tab.id === tabId)
    if (tabIndex === -1) return

    // const tab = orderTabs.value[tabIndex] // Currently unused

    // Release any reserved inventory for this tab
    if (reservedInventory.value.has(tabId)) {
      reservedInventory.value.delete(tabId)
    }

    // Remove the tab
    orderTabs.value.splice(tabIndex, 1)

    // Update active tab if necessary
    if (activeTabId.value === tabId) {
      if (orderTabs.value.length > 0) {
        // Switch to the previous tab or first available
        const newActiveIndex = Math.max(0, tabIndex - 1)
        activeTabId.value = orderTabs.value[newActiveIndex]?.id || null
      } else {
        activeTabId.value = null
      }
    }
  }

  const switchToTab = (tabId) => {
    const tab = orderTabs.value.find(t => t.id === tabId)
    if (tab) {
      activeTabId.value = tabId
    }
  }

  const updateActiveTabData = (updates) => {
    if (!activeTab.value) return

    const tabIndex = orderTabs.value.findIndex(tab => tab.id === activeTabId.value)
    if (tabIndex !== -1) {
      orderTabs.value[tabIndex] = { ...orderTabs.value[tabIndex], ...updates, isModified: true }
      calculateTabTotals(activeTabId.value)
    }
  }

  const calculateTabTotals = (tabId) => {
    const tab = orderTabs.value.find(t => t.id === tabId)
    if (!tab) return

    // Calculate subtotal from products
    const tongTienHang = tab.sanPhamList.reduce((total, item) => {
      return total + (item.donGia * item.soLuong)
    }, 0)

    // Calculate voucher discount
    const giaTriGiamGiaVoucher = tab.voucherList.reduce((total, voucher) => {
      return total + voucher.giaTriGiam
    }, 0)

    // Calculate shipping fee (only for delivery orders)
    const phiVanChuyen = tab.giaohang ? 30000 : 0

    // Calculate final total
    const tongThanhToan = Math.max(0, tongTienHang - giaTriGiamGiaVoucher + phiVanChuyen)

    // Update tab data
    const tabIndex = orderTabs.value.findIndex(t => t.id === tabId)
    if (tabIndex !== -1) {
      orderTabs.value[tabIndex] = {
        ...orderTabs.value[tabIndex],
        tongTienHang,
        giaTriGiamGiaVoucher,
        phiVanChuyen,
        tongThanhToan
      }
    }
  }

  // Actions
  const fetchOrders = async (page = 0, size = 20, filterParams = {}) => {
    const endMeasure = measurePerformance('fetchOrders')

    const params = {
      page,
      size,
      ...filterParams,
      ...filters.value
    }

    // Check cache first
    const cachedData = orderCache.getCachedOrderList(params)
    if (cachedData) {
      orders.value = cachedData.content || cachedData
      totalRecords.value = cachedData.totalElements || cachedData.length
      currentPage.value = page
      pageSize.value = size
      endMeasure()
      return
    }

    // Use request deduplication to prevent multiple identical requests
    const cacheKey = orderCache.generateCacheKey('orderList', params)

    try {
      loading.value = true
      error.value = null

      const response = await deduplicateRequest(cacheKey, async () => {
        return await orderApi.getAllOrders(params)
      })

      if (response.success) {
        const data = response.data
        orders.value = data.content || data
        totalRecords.value = data.totalElements || data.length
        currentPage.value = page
        pageSize.value = size

        // Cache the response for 2 minutes
        orderCache.setCachedOrderList(params, data, 2 * 60 * 1000)
      } else {
        throw new Error(response.message || 'Failed to fetch orders')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tải danh sách đơn hàng: ${err.message}`,
        life: 5000
      })
    } finally {
      loading.value = false
      endMeasure()
    }
  }

  const fetchOrderById = async (id) => {
    const endMeasure = measurePerformance('fetchOrderById')

    // Check cache first
    const cachedOrder = orderCache.getCachedOrderDetail(id)
    if (cachedOrder) {
      currentOrder.value = cachedOrder
      endMeasure()
      return cachedOrder
    }

    try {
      loading.value = true
      error.value = null

      const response = await deduplicateRequest(`orderDetail:${id}`, async () => {
        return await orderApi.getOrderById(id)
      })

      if (response.success) {
        currentOrder.value = response.data

        // Cache the order detail for 5 minutes
        orderCache.setCachedOrderDetail(id, response.data, 5 * 60 * 1000)

        endMeasure()
        return response.data
      } else {
        throw new Error(response.message || 'Failed to fetch order')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tải thông tin đơn hàng: ${err.message}`,
        life: 5000
      })
      endMeasure()
      return null
    } finally {
      loading.value = false
    }
  }

  const createOrder = async (orderData) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.createOrder(orderData)

      if (response.success) {
        // Add new order to the beginning of the list
        orders.value.unshift(response.data)
        currentOrder.value = response.data

        // Invalidate order list cache since we added a new order
        orderCache.invalidateByPattern('^orderList:')

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đơn hàng ${response.data.maHoaDon} đã được tạo thành công`,
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to create order')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tạo đơn hàng: ${err.message}`,
        life: 5000
      })
      return null
    } finally {
      loading.value = false
    }
  }

  // Create order from active tab with enhanced audit
  const createOrderFromTab = async (tabId = null) => {
    const targetTabId = tabId || activeTabId.value
    const tab = orderTabs.value.find(t => t.id === targetTabId)

    if (!tab) {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không tìm thấy đơn hàng để tạo',
        life: 3000
      })
      return null
    }

    // Validate required fields
    if (!tab.sanPhamList.length) {
      toast.add({
        severity: 'warn',
        summary: 'Cảnh báo',
        detail: 'Vui lòng thêm ít nhất một sản phẩm',
        life: 3000
      })
      return null
    }

    if (!tab.phuongThucThanhToan) {
      toast.add({
        severity: 'warn',
        summary: 'Cảnh báo',
        detail: 'Vui lòng chọn phương thức thanh toán',
        life: 3000
      })
      return null
    }

    // Prepare order data for API (match backend DTO structure)
    const orderData = {
      maHoaDon: tab.maHoaDon,
      loaiHoaDon: tab.loaiHoaDon,

      // Send only IDs to avoid transient entity issues
      khachHangId: tab.khachHang?.id || null,
      diaChiGiaoHangId: tab.diaChiGiaoHang?.id || null,

      // Delivery contact information
      nguoiNhanTen: tab.khachHang?.hoTen || null,
      nguoiNhanSdt: tab.khachHang?.soDienThoai || null,

      chiTiet: tab.sanPhamList.map(item => ({
        sanPhamChiTietId: item.sanPhamChiTiet?.id || item.sanPham?.id,
        soLuong: item.soLuong,
        giaBan: item.donGia
      })),
      voucherCodes: tab.voucherList.map(voucher => voucher.maPhieuGiamGia),
      phuongThucThanhToan: tab.phuongThucThanhToan,
      tongTienHang: tab.tongTienHang,
      giaTriGiamGiaVoucher: tab.giaTriGiamGiaVoucher,
      phiVanChuyen: tab.phiVanChuyen,
      tongThanhToan: tab.tongThanhToan,
      trangThaiDonHang: tab.giaohang ? 'CHO_XAC_NHAN' : 'HOAN_THANH',
      trangThaiThanhToan: tab.phuongThucThanhToan === 'TIEN_MAT' ? 'DA_THANH_TOAN' : 'CHUA_THANH_TOAN'
    }

    // Prepare enhanced audit context
    const auditContext = {
      userAgent: navigator.userAgent,
      sessionId: sessionStorage.getItem('sessionId') || `session_${Date.now()}`,
      ipAddress: 'client-side' // This would be set by the backend
    }

    try {
      // Use enhanced audit API for order creation
      const result = await orderApi.createOrderWithEnhancedAudit(orderData, auditContext)

      if (result.success) {
        const createdOrder = result.data

        // Add new order to the beginning of the list
        orders.value.unshift(createdOrder)
        currentOrder.value = createdOrder

        // Invalidate order list cache since we added a new order
        orderCache.invalidateByPattern('^orderList:')

        // Close the tab after successful creation
        closeOrderTab(targetTabId)

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đơn hàng ${createdOrder.maHoaDon} đã được tạo thành công`,
          life: 5000
        })

        return createdOrder
      } else {
        throw new Error(result.message || 'Failed to create order')
      }
    } catch (error) {
      console.error('Error creating order from tab:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tạo đơn hàng: ${error.message}`,
        life: 5000
      })
      return null
    }
  }

  const updateOrderStatus = async (orderId, newStatus, reason = '') => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.updateOrderStatus(orderId, newStatus, reason)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        const statusLabel = orderStatusMap.value[newStatus]?.label || newStatus
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đơn hàng đã được cập nhật trạng thái: ${statusLabel}`,
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to update order status')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể cập nhật trạng thái đơn hàng: ${err.message}`,
        life: 5000
      })
      return null
    } finally {
      loading.value = false
    }
  }

  const updateOrder = async (id, updateData) => {
    loading.value = true
    error.value = null

    try {
      console.log('OrderStore: updateOrder called with id:', id)
      console.log('OrderStore: updateData:', updateData)

      const response = await orderApi.updateOrder(id, updateData)
      console.log('OrderStore: API response:', response)

      if (response.success) {
        // Update the order in the local state
        const orderIndex = orders.value.findIndex(order => order.id === id)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = response.data
        }

        // Update current order if it's the same
        if (currentOrder.value?.id === id) {
          currentOrder.value = response.data
        }

        // Invalidate order caches since we updated an order
        orderCache.invalidateByPattern('^orderList:')
        orderCache.invalidateByPattern(`^orderDetail:${id}`)

        console.log('OrderStore: Order updated successfully:', response.data)
        return response.data
      } else {
        throw new Error(response.message || 'Failed to update order')
      }
    } catch (err) {
      console.error('OrderStore: Error updating order:', err)
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const cancelOrder = async (orderId, reason) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.cancelOrder(orderId, reason)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Đơn hàng đã được hủy thành công',
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to cancel order')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể hủy đơn hàng: ${err.message}`,
        life: 5000
      })
      return null
    } finally {
      loading.value = false
    }
  }

  // Payment operations
  const confirmPayment = async (orderId, paymentMethod) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.confirmPayment(orderId, paymentMethod)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Xác nhận thanh toán thành công',
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to confirm payment')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể xác nhận thanh toán: ${err.message}`,
        life: 5000
      })
      throw err
    } finally {
      loading.value = false
    }
  }

  const processRefund = async (orderId, refundAmount, reason) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.processRefund(orderId, refundAmount, reason)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Hoàn tiền thành công',
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to process refund')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể hoàn tiền: ${err.message}`,
        life: 5000
      })
      throw err
    } finally {
      loading.value = false
    }
  }

  const updatePaymentStatus = async (orderId, paymentStatus, note = '') => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.updatePaymentStatus(orderId, paymentStatus, note)

      if (response.success) {
        // Update order in the list
        const orderIndex = orders.value.findIndex(order => order.id === orderId)
        if (orderIndex !== -1) {
          orders.value[orderIndex] = { ...orders.value[orderIndex], ...response.data }
        }

        // Update current order if it's the same
        if (currentOrder.value && currentOrder.value.id === orderId) {
          currentOrder.value = { ...currentOrder.value, ...response.data }
        }

        // Invalidate cache for this order
        orderCache.invalidateByPattern(`^order:${orderId}`)

        const statusLabel = paymentStatusMap.value[paymentStatus]?.label || paymentStatus
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Trạng thái thanh toán đã được cập nhật: ${statusLabel}`,
          life: 5000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to update payment status')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể cập nhật trạng thái thanh toán: ${err.message}`,
        life: 5000
      })
      throw err
    } finally {
      loading.value = false
    }
  }

  const printOrderReceipt = async (orderId) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.printOrderReceipt(orderId)

      if (response.success) {
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Tạo hóa đơn thành công',
          life: 3000
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to generate receipt')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tạo hóa đơn: ${err.message}`,
        life: 5000
      })
      throw err
    } finally {
      loading.value = false
    }
  }

  // Batch operations
  const cancelMultipleOrders = async (orderIds, reason) => {
    loading.value = true
    error.value = null

    try {
      const response = await orderApi.cancelMultipleOrders(orderIds, reason)

      if (response.success) {
        // Update orders in the list
        orderIds.forEach(orderId => {
          const orderIndex = orders.value.findIndex(order => order.id === orderId)
          if (orderIndex !== -1) {
            orders.value[orderIndex] = {
              ...orders.value[orderIndex],
              trangThaiDonHang: 'DA_HUY',
              lyDoHuy: reason
            }
          }
        })

        return response.data
      } else {
        throw new Error(response.message || 'Failed to cancel multiple orders')
      }
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // Audit history functionality
  const fetchOrderAuditHistory = async (orderId) => {
    try {
      const response = await orderApi.getOrderHistory(orderId)

      if (response.success) {
        auditHistory.value[orderId] = response.data
        return response.data
      } else {
        throw new Error(response.message || 'Failed to fetch order audit history')
      }
    } catch (err) {
      console.error('Error fetching order audit history:', err)
      auditHistory.value[orderId] = []
      return []
    }
  }

  // Export functionality
  const exportOrders = async (ordersData) => {
    try {
      // Create CSV content
      const headers = [
        'Mã đơn hàng',
        'Loại đơn hàng',
        'Khách hàng',
        'Tổng tiền',
        'Trạng thái đơn hàng',
        'Trạng thái thanh toán',
        'Ngày tạo'
      ]

      const csvContent = [
        headers.join(','),
        ...ordersData.map(order => [
          order.maHoaDon,
          getOrderTypeInfo(order.loaiHoaDon).label,
          order.khachHang?.hoTen || 'Khách lẻ',
          order.tongThanhToan,
          getOrderStatusInfo(order.trangThaiDonHang).label,
          getPaymentStatusInfo(order.trangThaiThanhToan).label,
          new Date(order.ngayTao).toLocaleDateString('vi-VN')
        ].join(','))
      ].join('\n')

      // Create and download file
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', `danh-sach-don-hang-${new Date().toISOString().split('T')[0]}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      return true
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const setFilters = (newFilters) => {
    filters.value = { ...filters.value, ...newFilters }
  }

  const clearFilters = () => {
    filters.value = {
      status: 'all',
      type: 'all',
      dateRange: null,
      customer: null,
      staff: null,
      search: ''
    }
  }

  const setCurrentOrder = (order) => {
    currentOrder.value = order
  }

  const clearCurrentOrder = () => {
    currentOrder.value = null
  }

  const clearError = () => {
    error.value = null
  }

  // Utility functions
  const getOrderStatusInfo = (status) => {
    return orderStatusMap.value[status] || { label: status, severity: 'secondary', icon: 'pi pi-question' }
  }

  const getOrderTypeInfo = (type) => {
    return orderTypeMap.value[type] || { label: type, icon: 'pi pi-question' }
  }

  const getPaymentStatusInfo = (status) => {
    return paymentStatusMap.value[status] || { label: status, severity: 'secondary' }
  }

  // Enhanced audit history with granular tracking
  const fetchEnhancedAuditHistory = async (orderId) => {
    try {
      loading.value = true
      error.value = null

      const response = await orderApi.getEnhancedAuditHistory(orderId)

      if (response.success) {
        auditHistory.value[orderId] = response.data
        return response.data
      } else {
        throw new Error(response.message || 'Failed to fetch enhanced audit history')
      }
    } catch (err) {
      error.value = err.message
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: `Không thể tải lịch sử kiểm toán: ${err.message}`,
        life: 5000
      })
      return []
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    orders,
    currentOrder,
    loading,
    error,
    totalRecords,
    currentPage,
    pageSize,
    filters,
    orderStatuses,
    orderTypes,
    paymentStatuses,
    auditHistory,

    // Multi-tab state
    orderTabs,
    activeTabId,
    tabCounter,
    reservedInventory,

    // Computed
    filteredOrders,
    orderStatusMap,
    orderTypeMap,
    paymentStatusMap,
    activeTab,
    hasActiveTabs,
    canCreateNewTab,

    // Actions
    fetchOrders,
    fetchOrderById,
    createOrder,
    updateOrder,
    createOrderFromTab,
    updateOrderStatus,
    cancelOrder,
    cancelMultipleOrders,
    confirmPayment,
    processRefund,
    updatePaymentStatus,
    printOrderReceipt,
    fetchOrderAuditHistory,
    fetchEnhancedAuditHistory,
    exportOrders,
    setFilters,
    clearFilters,
    setCurrentOrder,
    clearCurrentOrder,
    clearError,

    // Multi-tab actions
    generateOrderCode,
    createNewOrderTab,
    closeOrderTab,
    switchToTab,
    updateActiveTabData,
    calculateTabTotals,

    // Utilities
    getOrderStatusInfo,
    getOrderTypeInfo,
    getPaymentStatusInfo,
    isCacheValid,

    // Performance optimization
    orderCache,
    debounce
  }
})
