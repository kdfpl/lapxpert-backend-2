<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useToast } from 'primevue/usetoast'
import { useOrderStore } from '@/stores/orderStore'
import { useCustomerStore } from '@/stores/customerstore'
import { useProductStore } from '@/stores/productstore'
import { useStaffStore } from '@/stores/staffstore'
import voucherApi from '@/apis/voucherApi'
import orderApi from '@/apis/orderApi'
import { useOrderAudit } from '@/composables/useOrderAudit'
import { useOrderValidation } from '@/composables/useOrderValidation'
import storageApi from '@/apis/storage'
import serialNumberApi from '@/apis/serialNumberApi'

// PrimeVue Components
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import InputText from 'primevue/inputtext'
import AutoComplete from 'primevue/autocomplete'
import Avatar from 'primevue/avatar'
import ToggleButton from 'primevue/togglebutton'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

// Custom Components
import ProductVariantDialog from '@/components/orders/ProductVariantDialog.vue'
import FastCustomerCreate from '@/components/orders/FastCustomerCreate.vue'
import FastAddressCreate from '@/components/orders/FastAddressCreate.vue'

// QR Scanner
import { QrcodeStream } from 'vue-qrcode-reader'

// Store access
const toast = useToast()
const orderStore = useOrderStore()
const customerStore = useCustomerStore()
const productStore = useProductStore()
const staffStore = useStaffStore()

// Destructure store state and actions using storeToRefs for reactive properties
const {
  orderTabs,
  activeTabId,
  activeTab,
  hasActiveTabs,
  canCreateNewTab
} = storeToRefs(orderStore)

const {
  createNewOrderTab,
  closeOrderTab,
  switchToTab,
  updateActiveTabData,
  calculateTabTotals,
  createOrderFromTab
} = orderStore

// Local state
const creating = ref(false)
const productSearchQuery = ref('')
const availableProducts = ref([])
const loadingProducts = ref(false)
const selectedCustomer = ref(null)
const customerSuggestions = ref([])

// Staff member state
const currentStaffMember = ref(null)
const selectedStaffMember = ref(null)
const staffSuggestions = ref([])

const voucherCode = ref('')
const availableVouchers = ref([])
const applyingVoucher = ref(false)
const searchTimeout = ref(null)

// New state for enhanced features
const bestVoucherResult = ref(null)
const loadingBestVoucher = ref(false)
const topVouchers = ref([])
const loadingTopVouchers = ref(false)

// Image URL cache for performance
const imageUrlCache = ref(new Map())

// Product variant dialog state
const variantDialogVisible = ref(false)
const selectedProductForVariants = ref(null)

// Product recommendations state
const showAllRecommendations = ref(false)
const allRecommendedProducts = ref([])

// Fast customer creation dialog state
const fastCustomerDialogVisible = ref(false)

// Fast address creation dialog state
const fastAddressDialogVisible = ref(false)

// QR Scanner state
const showQRScanner = ref(false)
const qrScanResult = ref(null)
const qrProcessingResult = ref(null)
const cameraError = ref(null)

// Local state
const hasUnsavedChanges = ref(false)



// Customer payment state
const customerPayment = ref(0)
const changeAmount = ref(0)

// Computed properties
const canCreateActiveOrder = computed(() => {
  if (!activeTab.value) return false

  // Basic requirements
  const hasProducts = activeTab.value.sanPhamList.length > 0
  const hasPaymentMethod = activeTab.value.phuongThucThanhToan

  // Delivery validation
  const deliveryValid = !activeTab.value.giaohang ||
    (activeTab.value.giaohang && activeTab.value.khachHang && activeTab.value.diaChiGiaoHang)

  return hasProducts && hasPaymentMethod && deliveryValid
})

const paymentMethods = computed(() => {
  if (!activeTab.value) return []

  const methods = []

  // TIEN_MAT - Only for TAI_QUAY orders (POS only)
  if (activeTab.value.loaiHoaDon === 'TAI_QUAY') {
    methods.push({
      value: 'TIEN_MAT',
      label: 'Tiền mặt',
      description: 'Thanh toán bằng tiền mặt tại quầy',
      icon: 'pi pi-wallet',
      available: true
    })
  }

  // COD - Available for both order types, but only when delivery is enabled
  if (activeTab.value.giaohang) {
    methods.push({
      value: 'COD',
      label: 'Thanh toán khi nhận hàng',
      description: 'Thanh toán khi giao hàng',
      icon: 'pi pi-money-bill',
      available: true
    })
  }

  // VNPAY - Available for both order types
  methods.push({
    value: 'VNPAY',
    label: 'VNPay',
    description: 'Thanh toán qua ví điện tử VNPay',
    icon: 'pi pi-credit-card',
    available: true
  })

  return methods
})

// Product recommendations computed property
const recommendedProducts = computed(() => {
  if (showAllRecommendations.value) {
    return allRecommendedProducts.value
  }
  return allRecommendedProducts.value.slice(0, 6)
})

// Methods
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

// Calculate actual discount amount for a voucher based on current order total
const calculateVoucherDiscount = (voucher, orderTotal = null) => {
  if (!voucher || !voucher.giaTriGiam) return 0

  const total = orderTotal || activeTab.value?.tongTienHang || 0

  if (voucher.loaiGiamGia === 'PHAN_TRAM') {
    // Percentage discount
    const discountAmount = (total * voucher.giaTriGiam) / 100
    // Apply maximum discount limit if specified
    if (voucher.giaTriGiamToiDa && discountAmount > voucher.giaTriGiamToiDa) {
      return voucher.giaTriGiamToiDa
    }
    return discountAmount
  } else {
    // Fixed amount discount (SO_TIEN_CO_DINH)
    return Math.min(voucher.giaTriGiam, total)
  }
}

// Get the effective price of the first available variant (promotional price if available, otherwise regular price)
const getProductPrice = (product) => {
  if (!product.sanPhamChiTiets || product.sanPhamChiTiets.length === 0) {
    return 0
  }
  const availableVariant = product.sanPhamChiTiets.find(variant => variant.active === true)
  const variant = availableVariant || product.sanPhamChiTiets[0]

  // Check for promotional price first (from DotGiamGia campaigns)
  if (variant.giaKhuyenMai && variant.giaKhuyenMai > 0 && variant.giaKhuyenMai < variant.giaBan) {
    return variant.giaKhuyenMai
  }
  return variant.giaBan
}

// Get the original price (before any discounts)
const getOriginalPrice = (product) => {
  if (!product.sanPhamChiTiets || product.sanPhamChiTiets.length === 0) {
    return 0
  }
  const availableVariant = product.sanPhamChiTiets.find(variant => variant.active === true)
  const variant = availableVariant || product.sanPhamChiTiets[0]
  return variant.giaBan
}

// Check if product has promotional pricing from DotGiamGia campaigns
const hasPromotionalPrice = (product) => {
  if (!product.sanPhamChiTiets) return false
  return product.sanPhamChiTiets.some(variant =>
    variant.giaKhuyenMai && variant.giaKhuyenMai > 0 && variant.giaKhuyenMai < variant.giaBan
  )
}

// Calculate discount percentage
const getDiscountPercentage = (product) => {
  if (!hasPromotionalPrice(product)) return 0
  const originalPrice = getOriginalPrice(product)
  const promotionalPrice = getProductPrice(product)
  if (originalPrice <= 0) return 0
  return Math.round(((originalPrice - promotionalPrice) / originalPrice) * 100)
}

// Get count of available variants
const getAvailableVariantsCount = (product) => {
  if (!product.sanPhamChiTiets || product.sanPhamChiTiets.length === 0) {
    return 0
  }
  return product.sanPhamChiTiets.filter(variant => variant.active === true).length
}

// Product prefetching functionality
const prefetchProducts = async () => {
  try {
    loadingProducts.value = true
    // Load initial set of active products (first 50-100 most popular/recent)
    await productStore.fetchActiveProducts()
    console.log('Raw products from store:', productStore.activeProducts)

    // Filter products that have available variants (sanPhamChiTiets with available status)
    const filteredProducts = productStore.activeProducts.filter(product => {
      // Check if product has variants
      if (!product.sanPhamChiTiets || product.sanPhamChiTiets.length === 0) {
        console.log(`Product ${product.tenSanPham} has no variants`)
        return false
      }

      // Check if any variant is available
      const hasAvailableVariants = product.sanPhamChiTiets.some(variant => variant.active === true)
      if (!hasAvailableVariants) {
        console.log(`Product ${product.tenSanPham} has no available variants`)
      }

      return hasAvailableVariants
    }).slice(0, 100)

    console.log('Filtered available products:', filteredProducts)
    console.log(`Found ${filteredProducts.length} products with available variants`)
    availableProducts.value = filteredProducts
  } catch (error) {
    console.error('Error prefetching products:', error)
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Không thể tải danh sách sản phẩm ban đầu',
      life: 3000
    })
  } finally {
    loadingProducts.value = false
  }
}

const searchProducts = async (immediate = false) => {
  // Clear existing timeout
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value)
  }

  if (!productSearchQuery.value.trim()) {
    // If no search query, show prefetched products
    await prefetchProducts()
    return
  }

  const performSearch = async () => {
    try {
      loadingProducts.value = true
      // Format search query as object for backend
      const searchFilters = {
        tenSanPham: productSearchQuery.value
      }
      const products = await productStore.searchProducts(searchFilters)
      console.log('Search results:', products)

      // Filter products that have available variants (sanPhamChiTiets with available status)
      const filteredSearchResults = products.filter(product => {
        // Check if product has variants
        if (!product.sanPhamChiTiets || product.sanPhamChiTiets.length === 0) {
          console.log(`Search result ${product.tenSanPham} has no variants`)
          return false
        }

        // Check if any variant is available
        const hasAvailableVariants = product.sanPhamChiTiets.some(variant => variant.active === true)
        if (!hasAvailableVariants) {
          console.log(`Search result ${product.tenSanPham} has no available variants`)
        }

        return hasAvailableVariants
      })

      console.log('Filtered search results:', filteredSearchResults)
      console.log(`Found ${filteredSearchResults.length} search results with available variants`)
      availableProducts.value = filteredSearchResults
    } catch (error) {
      console.error('Error searching products:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không thể tìm kiếm sản phẩm',
        life: 3000
      })
    } finally {
      loadingProducts.value = false
    }
  }

  if (immediate) {
    await performSearch()
  } else {
    // Debounce search with 300ms delay
    searchTimeout.value = setTimeout(performSearch, 300)
  }
}

// Show variant selection dialog for a product
const showVariantSelectionDialog = (product) => {
  if (!product.sanPhamChiTiets || product.sanPhamChiTiets.length === 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Sản phẩm này không có phiên bản nào có sẵn',
      life: 3000
    })
    return
  }

  selectedProductForVariants.value = product
  variantDialogVisible.value = true
}

// Legacy method for backward compatibility
const showVariantDialog = (product) => {
  showVariantSelectionDialog(product)
}

// Add selected variant to active tab (handles frontend-selected variants)
const addVariantToActiveTab = (variantData) => {
  if (!activeTab.value) return

  const { sanPhamChiTiet, soLuong, donGia, thanhTien, groupInfo } = variantData

  // Check if this specific variant with the same serial number already exists in cart
  // For variants with serial numbers, we need to check both variant ID and serial number
  const existingIndex = activeTab.value.sanPhamList.findIndex(item => {
    if (item.sanPhamChiTiet?.id !== sanPhamChiTiet.id) {
      return false
    }

    // If both items have serial numbers, compare them
    if (sanPhamChiTiet.serialNumber && item.sanPhamChiTiet?.serialNumber) {
      return item.sanPhamChiTiet.serialNumber === sanPhamChiTiet.serialNumber
    }

    // If both items have serial number IDs, compare them
    if (sanPhamChiTiet.serialNumberId && item.sanPhamChiTiet?.serialNumberId) {
      return item.sanPhamChiTiet.serialNumberId === sanPhamChiTiet.serialNumberId
    }

    // If neither has serial numbers, then it's a duplicate variant
    if (!sanPhamChiTiet.serialNumber && !sanPhamChiTiet.serialNumberId &&
        !item.sanPhamChiTiet?.serialNumber && !item.sanPhamChiTiet?.serialNumberId) {
      return true
    }

    // Different serial numbers or one has serial and other doesn't = not duplicate
    return false
  })

  if (existingIndex !== -1) {
    const serialInfo = sanPhamChiTiet.serialNumber ? ` (Serial: ${sanPhamChiTiet.serialNumber})` : ''
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: `Phiên bản này${serialInfo} đã có trong giỏ hàng`,
      life: 3000
    })
    return
  }

  // Add new variant to cart
  activeTab.value.sanPhamList.push({
    sanPhamChiTiet: sanPhamChiTiet,
    soLuong: soLuong,
    donGia: donGia,
    thanhTien: thanhTien,
    groupInfo: groupInfo // Store group info for display purposes
  })

  calculateTabTotals(activeTabId.value)

  // Don't show individual success messages when adding from groups
  // The ProductVariantDialog will show the group success message
  if (!groupInfo?.isFromGroup) {
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã thêm sản phẩm vào giỏ hàng',
      life: 3000
    })
  }
}

// Helper methods for cart item display
const getCartItemImage = (item) => {
  let imageFilename = null

  if (item.sanPhamChiTiet) {
    // Get first image from variant's image array
    if (item.sanPhamChiTiet.hinhAnh && Array.isArray(item.sanPhamChiTiet.hinhAnh) && item.sanPhamChiTiet.hinhAnh.length > 0) {
      imageFilename = item.sanPhamChiTiet.hinhAnh[0]
    } else {
      // Fallback to product image if variant has no image
      const productImages = item.sanPhamChiTiet.sanPham?.hinhAnh
      if (productImages && Array.isArray(productImages) && productImages.length > 0) {
        imageFilename = productImages[0]
      } else if (typeof productImages === 'string') {
        imageFilename = productImages
      }
    }
  } else {
    // Legacy support for old product-based items
    const productImages = item.sanPham?.hinhAnh
    if (productImages && Array.isArray(productImages) && productImages.length > 0) {
      imageFilename = productImages[0]
    } else if (typeof productImages === 'string') {
      imageFilename = productImages
    }
  }

  if (!imageFilename) return null

  // If it's already a full URL, return as is
  if (imageFilename.startsWith('http')) return imageFilename

  // Check cache first
  if (imageUrlCache.value.has(imageFilename)) {
    return imageUrlCache.value.get(imageFilename)
  }

  // Load presigned URL asynchronously
  loadCartImageUrl(imageFilename)

  // Return null for now, will update when loaded
  return null
}

const loadCartImageUrl = async (imageFilename) => {
  try {
    // Get presigned URL for the image filename
    const presignedUrl = await storageApi.getPresignedUrl('products', imageFilename)

    // Cache the URL for future use
    imageUrlCache.value.set(imageFilename, presignedUrl)

    // Force reactivity update
    imageUrlCache.value = new Map(imageUrlCache.value)
  } catch (error) {
    console.warn('Error getting presigned URL for cart image:', imageFilename, error)
    // Cache null to prevent repeated attempts
    imageUrlCache.value.set(imageFilename, null)
  }
}

// Helper method for product selection image display
const getProductImage = (product) => {
  let imageFilename = null

  // Get first image from product's image array
  if (product.hinhAnh && Array.isArray(product.hinhAnh) && product.hinhAnh.length > 0) {
    imageFilename = product.hinhAnh[0]
  } else if (typeof product.hinhAnh === 'string') {
    // Handle single image as string
    imageFilename = product.hinhAnh
  }

  if (!imageFilename) return null

  // If it's already a full URL, return as is
  if (imageFilename.startsWith('http')) return imageFilename

  // Check cache first
  if (imageUrlCache.value.has(imageFilename)) {
    return imageUrlCache.value.get(imageFilename)
  }

  // Load presigned URL asynchronously
  loadCartImageUrl(imageFilename)

  // Return null for now, will update when loaded
  return null
}

const getCartItemName = (item) => {
  if (item.sanPhamChiTiet) {
    return item.sanPhamChiTiet.sanPham?.tenSanPham || 'Sản phẩm'
  }
  // Legacy support for old product-based items
  return item.sanPham?.tenSanPham || 'Sản phẩm'
}

const getCartItemCode = (item) => {
  if (item.sanPhamChiTiet) {
    // Show product code instead of serial number to avoid duplication
    return item.sanPhamChiTiet.sanPham?.maSanPham || item.sanPhamChiTiet.maSanPhamChiTiet || ''
  }
  // Legacy support for old product-based items
  return item.sanPham?.maSanPham || ''
}

const getVariantDisplayInfo = (item) => {
  if (item.sanPhamChiTiet) {
    const parts = []

    // Add hardware specifications
    if (item.sanPhamChiTiet.cpu) parts.push(item.sanPhamChiTiet.cpu.moTaCpu)
    if (item.sanPhamChiTiet.ram) parts.push(item.sanPhamChiTiet.ram.moTaRam)
    if (item.sanPhamChiTiet.gpu) parts.push(item.sanPhamChiTiet.gpu.moTaGpu)
    if (item.sanPhamChiTiet.mauSac) parts.push(item.sanPhamChiTiet.mauSac.moTaMauSac)

    // Fix storage field reference - handle both oCung and ocung
    const storage = item.sanPhamChiTiet.oCung || item.sanPhamChiTiet.ocung
    if (storage) parts.push(storage.moTaOCung)

    if (item.sanPhamChiTiet.manHinh) parts.push(item.sanPhamChiTiet.manHinh.moTaManHinh)

    // Add serial number if available
    if (item.sanPhamChiTiet.serialNumber) {
      parts.push(`Serial: ${item.sanPhamChiTiet.serialNumber}`)
    }

    const displayInfo = parts.join(' • ')

    // Add group info if this variant was selected from a group
    if (item.groupInfo?.isFromGroup) {
      return `${displayInfo} • ${item.groupInfo.displayName}`
    }

    return displayInfo
  }
  return ''
}

const canIncreaseQuantity = (item) => {
  // For individual item tracking, each variant represents one item
  // So we don't allow quantity increase for variants
  if (item.sanPhamChiTiet) {
    return false
  }
  // Legacy support for old product-based items
  return item.soLuong < (item.sanPham?.soLuongTon || 0)
}

const increaseQuantity = (index) => {
  const item = activeTab.value.sanPhamList[index]

  // For variant-based items, we don't allow quantity increase
  if (item.sanPhamChiTiet) {
    toast.add({
      severity: 'info',
      summary: 'Thông báo',
      detail: 'Không thể tăng số lượng cho sản phẩm có serial number. Vui lòng thêm sản phẩm khác từ cùng nhóm.',
      life: 3000
    })
    return
  }

  // Legacy support for old product-based items
  if (item.soLuong < (item.sanPham?.soLuongTon || 0)) {
    item.soLuong++
    item.thanhTien = item.soLuong * item.donGia
    calculateTabTotals(activeTabId.value)
  }
}

const decreaseQuantity = (index) => {
  const item = activeTab.value.sanPhamList[index]
  if (item.soLuong > 1) {
    item.soLuong--
    item.thanhTien = item.soLuong * item.donGia
    calculateTabTotals(activeTabId.value)
  }
}

const removeFromActiveTab = (index) => {
  activeTab.value.sanPhamList.splice(index, 1)
  calculateTabTotals(activeTabId.value)
}

// Product recommendation methods
const toggleRecommendations = () => {
  showAllRecommendations.value = !showAllRecommendations.value
}

// QR Scanner Methods
const onQRDetect = async (detectedCodes) => {
  if (detectedCodes && detectedCodes.length > 0) {
    const scannedValue = detectedCodes[0].rawValue
    console.log('QR Code detected:', scannedValue)

    // Set the scanned result
    qrScanResult.value = scannedValue

    // Process the scanned serial number
    await processScannedSerialNumber(scannedValue)
  }
}

const onQRInit = async (promise) => {
  try {
    await promise
    cameraError.value = null

    toast.add({
      severity: 'info',
      summary: 'Camera',
      detail: 'Camera đã được kích hoạt thành công',
      life: 2000,
    })
  } catch (error) {
    console.error('QR Scanner initialization error:', error)

    if (error.name === 'NotAllowedError') {
      cameraError.value = 'Quyền truy cập camera bị từ chối. Vui lòng cấp quyền và thử lại.'
    } else if (error.name === 'NotFoundError') {
      cameraError.value = 'Không tìm thấy camera. Vui lòng kiểm tra thiết bị.'
    } else if (error.name === 'NotSupportedError') {
      cameraError.value = 'Trình duyệt không hỗ trợ camera.'
    } else if (error.name === 'NotReadableError') {
      cameraError.value = 'Camera đang được sử dụng bởi ứng dụng khác.'
    } else if (error.name === 'OverconstrainedError') {
      cameraError.value = 'Camera không đáp ứng yêu cầu.'
    } else {
      cameraError.value = 'Lỗi không xác định khi truy cập camera.'
    }
  }
}

const paintBoundingBox = (detectedCodes, ctx) => {
  for (const detectedCode of detectedCodes) {
    const { boundingBox: { x, y, width, height } } = detectedCode

    ctx.lineWidth = 2
    ctx.strokeStyle = '#007bff'
    ctx.strokeRect(x, y, width, height)
  }
}

const requestCameraPermission = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    stream.getTracks().forEach((track) => track.stop())

    showQRScanner.value = false
    setTimeout(() => {
      showQRScanner.value = true
      qrScanResult.value = null
      cameraError.value = null
    }, 500)

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã được cấp quyền truy cập camera',
      life: 3000,
    })
  } catch (error) {
    console.error('Permission request error:', error)
    cameraError.value = 'Không thể truy cập camera. Vui lòng kiểm tra cài đặt quyền của trình duyệt.'
    toast.add({
      severity: 'error',
      summary: 'Lỗi Camera',
      detail: cameraError.value,
      life: 5000,
    })
  }
}

const stopQRScanner = () => {
  qrScanResult.value = null
  qrProcessingResult.value = null
  cameraError.value = null
}

const resetQRScanner = () => {
  qrScanResult.value = null
  qrProcessingResult.value = null
}

const processScannedSerialNumber = async (serialNumber) => {
  try {
    console.log('Processing scanned serial number:', serialNumber)

    // Find the serial number in the database
    const serialData = await serialNumberApi.getBySerialNumber(serialNumber)

    if (!serialData) {
      qrProcessingResult.value = {
        success: false,
        message: `Serial number "${serialNumber}" không tồn tại trong hệ thống`
      }
      return
    }

    // Check if serial number is available
    if (serialData.trangThai !== 'AVAILABLE') {
      qrProcessingResult.value = {
        success: false,
        message: `Serial number "${serialNumber}" không khả dụng (Trạng thái: ${serialData.trangThai})`
      }
      return
    }

    // Get the product variant information
    const variantId = serialData.sanPhamChiTietId
    if (!variantId) {
      qrProcessingResult.value = {
        success: false,
        message: `Serial number "${serialNumber}" không liên kết với sản phẩm nào`
      }
      return
    }

    // Find variant in current products or search for it
    let variant = null

    // First, try to find in currently loaded products
    for (const product of availableProducts.value) {
      if (product.sanPhamChiTiets) {
        variant = product.sanPhamChiTiets.find(v => v.id === variantId)
        if (variant) {
          // Add product reference to variant for display
          variant.sanPham = product
          break
        }
      }
    }

    // If not found in current products, search all products
    if (!variant) {
      try {
        await productStore.fetchProducts()
        for (const product of productStore.products) {
          if (product.sanPhamChiTiets) {
            variant = product.sanPhamChiTiets.find(v => v.id === variantId)
            if (variant) {
              // Add product reference to variant for display
              variant.sanPham = product
              break
            }
          }
        }
      } catch (error) {
        console.error('Error fetching products for variant lookup:', error)
      }
    }

    if (!variant) {
      qrProcessingResult.value = {
        success: false,
        message: `Không tìm thấy thông tin sản phẩm cho serial number "${serialNumber}"`
      }
      return
    }

    // Check if this serial number is already in cart
    const existingInCart = activeTab.value?.sanPhamList?.find(item =>
      item.sanPhamChiTiet?.serialNumberId === serialData.id ||
      item.sanPhamChiTiet?.serialNumber === serialNumber
    )

    if (existingInCart) {
      qrProcessingResult.value = {
        success: false,
        message: `Serial number "${serialNumber}" đã có trong giỏ hàng`
      }
      return
    }

    // Create variant data with serial number
    const variantWithSerial = {
      ...variant,
      serialNumber: serialNumber,
      serialNumberId: serialData.id
    }

    // Add to cart
    const variantData = {
      sanPhamChiTiet: variantWithSerial,
      soLuong: 1,
      donGia: variant.giaKhuyenMai && variant.giaKhuyenMai > 0 ? variant.giaKhuyenMai : variant.giaBan,
      thanhTien: variant.giaKhuyenMai && variant.giaKhuyenMai > 0 ? variant.giaKhuyenMai : variant.giaBan
    }

    addVariantToActiveTab(variantData)

    qrProcessingResult.value = {
      success: true,
      message: `Đã thêm sản phẩm với serial "${serialNumber}" vào giỏ hàng`
    }

    // Auto-close scanner after successful scan (optional)
    setTimeout(() => {
      if (qrProcessingResult.value?.success) {
        resetQRScanner()
      }
    }, 2000)

  } catch (error) {
    console.error('Error processing scanned serial number:', error)
    qrProcessingResult.value = {
      success: false,
      message: `Lỗi khi xử lý serial number: ${error.message}`
    }
  }
}

const generateRecommendations = () => {
  if (!productStore.activeProducts || productStore.activeProducts.length === 0) {
    allRecommendedProducts.value = []
    return
  }

  // Get products already in cart to exclude them
  const cartProductIds = new Set()
  if (activeTab.value?.sanPhamList) {
    activeTab.value.sanPhamList.forEach(item => {
      if (item.sanPhamChiTiet?.sanPham?.id) {
        cartProductIds.add(item.sanPhamChiTiet.sanPham.id)
      }
    })
  }

  // Get categories and brands from cart items for similarity matching
  const cartCategories = new Set()
  const cartBrands = new Set()
  if (activeTab.value?.sanPhamList) {
    activeTab.value.sanPhamList.forEach(item => {
      if (item.sanPhamChiTiet?.sanPham) {
        const product = item.sanPhamChiTiet.sanPham
        if (product.danhMucs) {
          product.danhMucs.forEach(category => cartCategories.add(category.id))
        }
        if (product.thuongHieu) {
          cartBrands.add(product.thuongHieu.id)
        }
      }
    })
  }

  // Filter and score products for recommendations
  const candidates = productStore.activeProducts
    .filter(product => {
      // Exclude products already in cart
      if (cartProductIds.has(product.id)) return false

      // Only include products with available variants
      return getAvailableVariantsCount(product) > 0
    })
    .map(product => {
      let score = 0

      // Base score for having promotional pricing
      if (hasPromotionalPrice(product)) {
        score += 10
      }

      // Score for category similarity
      if (product.danhMucs && cartCategories.size > 0) {
        const hasMatchingCategory = product.danhMucs.some(category =>
          cartCategories.has(category.id)
        )
        if (hasMatchingCategory) score += 20
      }

      // Score for brand similarity
      if (product.thuongHieu && cartBrands.has(product.thuongHieu.id)) {
        score += 15
      }

      // Score for price range (prefer mid-range products)
      const price = getProductPrice(product)
      if (price >= 5000000 && price <= 20000000) {
        score += 5
      }

      // Random factor for variety
      score += Math.random() * 5

      return { product, score }
    })
    .sort((a, b) => b.score - a.score)
    .slice(0, 12) // Get top 12 candidates
    .map(item => item.product)

  allRecommendedProducts.value = candidates
}

const getRecommendationProductImage = (product) => {
  let imageFilename = null

  // Get first image from product's image array (same logic as getProductImage)
  if (product.hinhAnh && Array.isArray(product.hinhAnh) && product.hinhAnh.length > 0) {
    imageFilename = product.hinhAnh[0]
  } else if (product.hinhAnhs && Array.isArray(product.hinhAnhs) && product.hinhAnhs.length > 0) {
    // Fallback to hinhAnhs if hinhAnh doesn't exist
    const firstImage = product.hinhAnhs[0]
    imageFilename = firstImage?.duongDanHinh || firstImage
  } else if (typeof product.hinhAnh === 'string') {
    // Single image as string
    imageFilename = product.hinhAnh
  }

  if (!imageFilename) return null

  // If it's already a full URL, return as is
  if (imageFilename.startsWith('http')) return imageFilename

  // Check if we have a cached URL
  const cached = imageUrlCache.value.get(imageFilename)
  if (cached) return cached

  // Load the image URL asynchronously
  loadRecommendationImageUrl(imageFilename)

  // Return null for now, will update when loaded
  return null
}

const loadRecommendationImageUrl = async (imageFilename) => {
  try {
    // Get presigned URL for the image filename
    const presignedUrl = await storageApi.getPresignedUrl('products', imageFilename)

    // Cache the URL for future use
    imageUrlCache.value.set(imageFilename, presignedUrl)

    // Force reactivity update
    imageUrlCache.value = new Map(imageUrlCache.value)
  } catch (error) {
    console.warn('Failed to load recommendation image:', error)
    // Cache a null value to avoid repeated attempts
    imageUrlCache.value.set(imageFilename, null)
  }
}

const onRecommendationImageError = (event) => {
  // Hide broken image and show placeholder
  event.target.style.display = 'none'
}

const searchCustomers = async (event) => {
  try {
    console.log('Searching customers with query:', event.query)

    // Try backend search first
    try {
      const customers = await customerStore.fetchCustomers({ search: event.query })
      console.log('Customer search results from backend:', customers)
      customerSuggestions.value = customers
      console.log('Updated customerSuggestions:', customerSuggestions.value)
      return
    } catch (backendError) {
      console.warn('Backend search failed, falling back to frontend filtering:', backendError)
    }

    // Fallback: Load all customers and filter on frontend
    const allCustomers = await customerStore.fetchCustomers()
    console.log('All customers loaded:', allCustomers)

    if (!event.query || event.query.trim() === '') {
      customerSuggestions.value = allCustomers.slice(0, 10) // Limit to first 10
      return
    }

    const query = event.query.toLowerCase().trim()
    const filteredCustomers = allCustomers.filter(customer => {
      return (
        customer.hoTen?.toLowerCase().includes(query) ||
        customer.soDienThoai?.includes(query) ||
        customer.email?.toLowerCase().includes(query) ||
        customer.maNguoiDung?.toLowerCase().includes(query)
      )
    }).slice(0, 10) // Limit to first 10 results

    console.log('Filtered customers:', filteredCustomers)
    customerSuggestions.value = filteredCustomers

  } catch (error) {
    console.error('Error searching customers:', error)
    customerSuggestions.value = []
  }
}

const onCustomerSelect = async (event) => {
  try {
    console.log('Customer selected from search:', event.value)

    // Fetch complete customer data with addresses to ensure we have all necessary information
    const customerWithAddresses = await customerStore.fetchCustomerById(event.value.id)
    console.log('Customer data with addresses loaded:', customerWithAddresses)

    updateActiveTabData({
      khachHang: customerWithAddresses,
      diaChiGiaoHang: null // Clear any previously selected address
    })
    selectedCustomer.value = customerWithAddresses

    // Load available vouchers for the selected customer
    await loadAvailableVouchers()
  } catch (error) {
    console.error('Error loading customer details:', error)
    // Fallback to the basic customer data from search
    console.log('Using fallback customer data:', event.value)
    updateActiveTabData({
      khachHang: event.value,
      diaChiGiaoHang: null // Clear any previously selected address
    })
    selectedCustomer.value = event.value
    await loadAvailableVouchers()
  }
}

const clearCustomerFromTab = () => {
  updateActiveTabData({ khachHang: null, diaChiGiaoHang: null })
  selectedCustomer.value = null
  // Clear available vouchers when customer is removed
  availableVouchers.value = []
  // Reset customer payment fields
  customerPayment.value = 0
  changeAmount.value = 0
}

// Staff member methods
const searchStaffMembers = async (event) => {
  try {
    console.log('Searching staff members with query:', event.query)

    // Ensure staff data is loaded
    if (!staffStore.staff || staffStore.staff.length === 0) {
      await staffStore.fetchStaff()
    }

    // Filter staff members locally based on search query
    const query = event.query.toLowerCase()
    const filteredStaff = staffStore.activeStaff.filter(staff => {
      return staff.hoTen?.toLowerCase().includes(query) ||
             staff.soDienThoai?.includes(query) ||
             staff.email?.toLowerCase().includes(query)
    })

    console.log('Staff search results:', filteredStaff)
    staffSuggestions.value = filteredStaff || []
  } catch (error) {
    console.error('Error searching staff members:', error)
    staffSuggestions.value = []
  }
}

const onStaffMemberSelect = (event) => {
  try {
    console.log('Staff member selected from search:', event.value)

    // Update the active tab with selected staff member
    updateActiveTabData({ nhanVien: event.value })
    selectedStaffMember.value = event.value

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã chọn nhân viên: ${event.value.hoTen}`,
      life: 3000
    })
  } catch (error) {
    console.error('Error selecting staff member:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể chọn nhân viên',
      life: 3000
    })
  }
}

const clearStaffMemberFromTab = () => {
  updateActiveTabData({ nhanVien: null })
  selectedStaffMember.value = null
}

// Fast customer creation methods
const showFastCustomerDialog = () => {
  fastCustomerDialogVisible.value = true
}

const onCustomerCreated = async (newCustomer) => {
  try {
    console.log('New customer created:', newCustomer)

    // Select the newly created customer
    updateActiveTabData({
      khachHang: newCustomer,
      diaChiGiaoHang: null // Clear any previously selected address
    })
    selectedCustomer.value = newCustomer

    // Load available vouchers for the new customer
    await loadAvailableVouchers()

    // Automatically find and apply best voucher for new customer
    if (activeTab.value.tongTienHang > 0) {
      await findAndApplyBestVoucher()
    }

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã chọn khách hàng ${newCustomer.hoTen} cho đơn hàng`,
      life: 3000
    })
  } catch (error) {
    console.error('Error selecting newly created customer:', error)
  }
}

// Fast address creation methods
const showFastAddressDialog = () => {
  if (!activeTab.value.khachHang) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng chọn khách hàng trước khi thêm địa chỉ',
      life: 3000
    })
    return
  }
  fastAddressDialogVisible.value = true
}

const onAddressCreated = async (newAddress) => {
  try {
    console.log('New address created:', newAddress)

    if (!activeTab.value.khachHang) {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không tìm thấy thông tin khách hàng',
        life: 3000
      })
      return
    }

    // Add the new address to the customer's address list
    if (!activeTab.value.khachHang.diaChis) {
      activeTab.value.khachHang.diaChis = []
    }

    // Create a temporary address object with ID for UI purposes
    const tempAddress = {
      ...newAddress,
      id: Date.now(), // Temporary ID for UI
      nguoiDungId: activeTab.value.khachHang.id
    }

    activeTab.value.khachHang.diaChis.push(tempAddress)

    // Automatically select the new address for delivery
    updateActiveTabData({ diaChiGiaoHang: tempAddress })

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã thêm địa chỉ giao hàng và chọn làm địa chỉ giao hàng cho đơn hàng này',
      life: 4000
    })
  } catch (error) {
    console.error('Error handling newly created address:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Có lỗi xảy ra khi xử lý địa chỉ mới',
      life: 3000
    })
  }
}

const loadAvailableVouchers = async () => {
  if (!activeTab.value) return

  try {
    const customerId = activeTab.value.khachHang?.id || null
    const orderTotal = activeTab.value.tongTienHang || 0

    const response = await voucherApi.getAvailableVouchers(customerId, orderTotal)

    if (response.success) {
      // Filter out already applied vouchers
      const appliedVoucherCodes = activeTab.value.voucherList.map(v => v.maPhieuGiamGia)
      availableVouchers.value = response.data.filter(
        voucher => !appliedVoucherCodes.includes(voucher.maPhieuGiamGia)
      )
    } else {
      availableVouchers.value = []
    }
  } catch (error) {
    console.error('Error loading available vouchers:', error)
    availableVouchers.value = []
  }
}

const selectDeliveryAddress = (address) => {
  // Validate that the address belongs to the current customer
  if (!activeTab.value.khachHang) {
    console.warn('Cannot select address: No customer selected')
    return
  }

  // Check if address has nguoiDungId field (from DTO mapping)
  const addressOwnerId = address.nguoiDungId || address.nguoiDung?.id

  if (addressOwnerId && addressOwnerId !== activeTab.value.khachHang.id) {
    console.error('Address validation failed: Address does not belong to selected customer', {
      addressId: address.id,
      addressOwnerId: addressOwnerId,
      selectedCustomerId: activeTab.value.khachHang.id
    })
    return
  }

  updateActiveTabData({ diaChiGiaoHang: address })
}

const onDeliveryToggle = () => {
  if (!activeTab.value.giaohang) {
    // Clear address when delivery is turned off
    updateActiveTabData({ diaChiGiaoHang: null })
  } else {
    // When delivery is turned on, validate current address selection
    if (activeTab.value.diaChiGiaoHang && activeTab.value.khachHang) {
      const currentAddress = activeTab.value.diaChiGiaoHang
      if (currentAddress.nguoiDungId && currentAddress.nguoiDungId !== activeTab.value.khachHang.id) {
        console.warn('Clearing invalid address selection: Address does not belong to current customer')
        updateActiveTabData({ diaChiGiaoHang: null })
      }
    }
  }

  // Validate current payment method when delivery option changes
  const currentPaymentMethod = activeTab.value.phuongThucThanhToan
  const availablePaymentMethods = paymentMethods.value.map(m => m.value)

  if (currentPaymentMethod && !availablePaymentMethods.includes(currentPaymentMethod)) {
    // Clear invalid payment method
    updateActiveTabData({ phuongThucThanhToan: null })
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Phương thức thanh toán đã chọn không khả dụng với tùy chọn giao hàng hiện tại',
      life: 3000
    })
  }

  calculateTabTotals(activeTabId.value)
}

const applyVoucher = async () => {
  if (!voucherCode.value.trim()) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng nhập mã voucher',
      life: 3000
    })
    return
  }

  if (!activeTab.value) return

  applyingVoucher.value = true
  try {
    // Validate voucher with backend
    const customerId = activeTab.value.khachHang?.id || null
    const orderTotal = activeTab.value.tongTienHang || 0

    const response = await voucherApi.validateVoucher(voucherCode.value, customerId, orderTotal)

    if (response.success && response.data.valid) {
      // Check if voucher is already applied
      const existingVoucher = activeTab.value.voucherList.find(
        v => v.maPhieuGiamGia === response.data.voucher.maPhieuGiamGia
      )

      if (existingVoucher) {
        toast.add({
          severity: 'warn',
          summary: 'Cảnh báo',
          detail: 'Voucher này đã được áp dụng',
          life: 3000
        })
        return
      }

      // Add voucher to active tab
      const voucherData = {
        ...response.data.voucher,
        giaTriGiam: response.data.discountAmount
      }

      activeTab.value.voucherList.push(voucherData)
      calculateTabTotals(activeTabId.value)

      // Clear input and show success
      voucherCode.value = ''
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `Áp dụng voucher ${response.data.voucher.maPhieuGiamGia} thành công`,
        life: 3000
      })
    } else {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: response.data.error || 'Voucher không hợp lệ',
        life: 3000
      })
    }
  } catch (error) {
    console.error('Error applying voucher:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể áp dụng voucher. Vui lòng thử lại.',
      life: 3000
    })
  } finally {
    applyingVoucher.value = false
  }
}

const removeVoucherFromTab = async (index) => {
  if (!activeTab.value) return

  const removedVoucher = activeTab.value.voucherList[index]

  // Remove voucher directly
  activeTab.value.voucherList.splice(index, 1)
  calculateTabTotals(activeTabId.value)

  // Reload available vouchers to include the removed voucher
  await loadAvailableVouchers()

  toast.add({
    severity: 'info',
    summary: 'Thông báo',
    detail: `Đã gỡ voucher ${removedVoucher.maPhieuGiamGia}`,
    life: 3000
  })
}

const selectVoucher = async (voucher) => {
  if (!activeTab.value) return

  try {
    // Validate voucher before applying
    const customerId = activeTab.value.khachHang?.id || null
    const orderTotal = activeTab.value.tongTienHang || 0

    const response = await voucherApi.validateVoucher(voucher.maPhieuGiamGia, customerId, orderTotal)

    if (response.success && response.data.valid) {
      // Check if voucher is already applied
      const existingVoucher = activeTab.value.voucherList.find(
        v => v.maPhieuGiamGia === voucher.maPhieuGiamGia
      )

      if (existingVoucher) {
        toast.add({
          severity: 'warn',
          summary: 'Cảnh báo',
          detail: 'Voucher này đã được áp dụng',
          life: 3000
        })
        return
      }

      // Add voucher to active tab with validated discount amount
      const voucherData = {
        ...response.data.voucher,
        giaTriGiam: response.data.discountAmount
      }

      activeTab.value.voucherList.push(voucherData)
      calculateTabTotals(activeTabId.value)

      // Remove from available vouchers list
      availableVouchers.value = availableVouchers.value.filter(
        v => v.maPhieuGiamGia !== voucher.maPhieuGiamGia
      )

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `Áp dụng voucher ${voucher.maPhieuGiamGia} thành công`,
        life: 3000
      })
    } else {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: response.data.error || 'Voucher không hợp lệ',
        life: 3000
      })
    }
  } catch (error) {
    console.error('Error applying voucher:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể áp dụng voucher. Vui lòng thử lại.',
      life: 3000
    })
  }
}

const selectPaymentMethod = (method) => {
  updateActiveTabData({ phuongThucThanhToan: method })

  // Reset customer payment when changing payment method
  if (method !== 'TIEN_MAT') {
    customerPayment.value = 0
    changeAmount.value = 0
  }
}

// Calculate change amount for cash payments
const calculateChange = () => {
  if (!activeTab.value || activeTab.value.phuongThucThanhToan !== 'TIEN_MAT') {
    changeAmount.value = 0
    return
  }

  const payment = customerPayment.value || 0
  const total = activeTab.value.tongThanhToan || 0
  changeAmount.value = Math.max(0, payment - total)
}



// Automatic voucher selection methods
const findAndApplyBestVoucher = async () => {
  if (!activeTab.value || !activeTab.value.khachHang || activeTab.value.tongTienHang <= 0) return

  try {
    loadingBestVoucher.value = true
    const customerId = activeTab.value.khachHang.id
    const orderTotal = activeTab.value.tongTienHang

    const response = await voucherApi.getBestVoucher(customerId, orderTotal)

    if (response.success && response.data.found) {
      bestVoucherResult.value = response.data

      // Check if this voucher is already applied
      const existingVoucher = activeTab.value.voucherList.find(
        v => v.maPhieuGiamGia === response.data.voucher.maPhieuGiamGia
      )

      if (!existingVoucher) {
        // Automatically apply the best voucher
        await selectVoucher(response.data.voucher)

        toast.add({
          severity: 'success',
          summary: 'Tự động áp dụng voucher',
          detail: `Đã áp dụng voucher tốt nhất: ${response.data.voucher.maPhieuGiamGia} (Giảm ${formatCurrency(response.data.discountAmount)})`,
          life: 4000
        })
      }
    }
  } catch (error) {
    console.error('Error finding best voucher:', error)
    // Don't show error toast for automatic voucher application to avoid annoying users
  } finally {
    loadingBestVoucher.value = false
  }
}

// Legacy method for backward compatibility
const findBestVoucher = findAndApplyBestVoucher

const loadTopVouchers = async () => {
  if (!activeTab.value) return

  try {
    loadingTopVouchers.value = true
    const customerId = activeTab.value.khachHang?.id || null
    const orderTotal = activeTab.value.tongTienHang || 0

    const response = await voucherApi.getTopVouchers(customerId, orderTotal, 3)

    if (response.success) {
      topVouchers.value = response.data
    }
  } catch (error) {
    console.error('Error loading top vouchers:', error)
    topVouchers.value = []
  } finally {
    loadingTopVouchers.value = false
  }
}

// Enhanced audit trail methods
const createEnhancedAuditEntry = async (entryType, auditData) => {
  if (!activeTab.value) return

  try {
    // Create audit entry using enhanced API
    const response = await orderApi.createAuditEntry(
      activeTab.value.id, // This would be the order ID after creation
      entryType,
      auditData
    )

    if (response.success) {
      console.log('Enhanced audit entry created:', response.data)
    }
  } catch (error) {
    console.error('Error creating enhanced audit entry:', error)
  }
}

const createOrderFromActiveTab = async () => {
  if (!activeTab.value) return

  // Prevent multiple simultaneous order creation attempts
  if (creating.value) {
    console.log('Order creation already in progress, ignoring duplicate request')
    return
  }

  // Perform comprehensive validation using Bean Validation patterns
  const validationErrors = validateActiveTab()

  if (Object.keys(validationErrors).length > 0) {
    // Display validation errors
    const errorMessages = []
    Object.entries(validationErrors).forEach(([, errors]) => {
      errors.forEach(error => errorMessages.push(`- ${error}`))
    })

    toast.add({
      severity: 'warn',
      summary: 'Dữ liệu không hợp lệ',
      detail: `Vui lòng kiểm tra lại:\n${errorMessages.join('\n')}`,
      life: 7000
    })
    return
  }

  // Proceed directly with order creation
  await performOrderCreation()
}

const performOrderCreation = async () => {
  creating.value = true
  try {
    // Map frontend data to HoaDonDto structure for validation and logging
    const orderData = mapTabToHoaDonDto(activeTab.value)
    console.log('Creating order with data:', orderData)

    // Create order using orderStore (which handles the actual API call)
    const result = await createOrderFromTab()

    if (result) {
      // Create audit trail entry for order creation
      await auditOrderCreation(result.id, result)

      // Clear unsaved changes flag
      hasUnsavedChanges.value = false

      // Note: Success toast is handled by orderStore.createOrderFromTab()
    }
  } catch (error) {
    console.error('Error creating order:', error)

    // Handle specific API validation errors
    if (error.response && error.response.data && error.response.data.errors) {
      const apiErrors = error.response.data.errors
      const errorMessages = Object.values(apiErrors).flat()

      toast.add({
        severity: 'error',
        summary: 'Lỗi xác thực',
        detail: `Dữ liệu không hợp lệ:\n${errorMessages.join('\n')}`,
        life: 7000
      })
    } else {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không thể tạo đơn hàng. Vui lòng thử lại.',
        life: 5000
      })
    }
  } finally {
    creating.value = false
  }
}

// Enhanced tab closure with unsaved changes warning
const closeTabWithConfirmation = (tabId) => {
  const tab = orderTabs.value.find(t => t.id === tabId)

  if (!tab) {
    closeOrderTab(tabId)
    return
  }

  // Close tab directly (no confirmation)
  closeOrderTab(tabId)
  hasUnsavedChanges.value = false
}

// Map frontend tab data to backend HoaDonDto structure
const mapTabToHoaDonDto = (tab) => {
  // Validate address ownership before sending
  let validatedAddressId = null

  if (tab.diaChiGiaoHang && tab.khachHang) {
    const addressOwnerId = tab.diaChiGiaoHang.nguoiDungId || tab.diaChiGiaoHang.nguoiDung?.id
    if (addressOwnerId === tab.khachHang.id) {
      validatedAddressId = tab.diaChiGiaoHang.id
    } else {
      console.error('Address validation failed - not sending address ID', {
        addressId: tab.diaChiGiaoHang.id,
        addressOwnerId: addressOwnerId,
        customerId: tab.khachHang.id
      })
    }
  }

  const dto = {
    // Basic order information
    maHoaDon: tab.maHoaDon,
    loaiHoaDon: tab.loaiHoaDon,

    // Customer information - send only ID to avoid transient entity issues
    khachHangId: tab.khachHang?.id || null,

    // Staff member information - prioritize selected staff, fallback to current user
    nhanVienId: tab.nhanVien?.id || currentStaffMember.value?.id || null,

    // Delivery information - send only validated address ID
    diaChiGiaoHangId: validatedAddressId,
    nguoiNhanTen: tab.khachHang?.hoTen || null,
    nguoiNhanSdt: tab.khachHang?.soDienThoai || null,

    // Financial information
    tongTienHang: tab.tongTienHang || 0,
    giaTriGiamGiaVoucher: tab.giaTriGiamGiaVoucher || 0,
    phiVanChuyen: tab.phiVanChuyen || 0,
    tongThanhToan: tab.tongThanhToan || 0,

    // Status information
    trangThaiDonHang: tab.giaohang ? 'CHO_XAC_NHAN' : 'HOAN_THANH',
    trangThaiThanhToan: tab.phuongThucThanhToan === 'TIEN_MAT' ? 'DA_THANH_TOAN' : 'CHUA_THANH_TOAN',

    // Order details - send individual variants (frontend-selected from groups)
    chiTiet: tab.sanPhamList.map(item => ({
      sanPhamChiTietId: item.sanPhamChiTiet?.id,
      soLuong: item.soLuong,
      donGia: item.donGia,
      thanhTien: item.donGia * item.soLuong,
      // Include serial number information if available
      serialNumberId: item.sanPhamChiTiet?.serialNumberId,
      serialNumber: item.sanPhamChiTiet?.serialNumber
    })),

    // Voucher information
    voucherCodes: tab.voucherList.map(voucher => voucher.maPhieuGiamGia)
  }

  console.log('Generated HoaDonDto:', dto)
  return dto
}

// Use shared audit composable
const { auditOrderCreation } = useOrderAudit()

// Use shared validation composable
const {
  validateTabData,
  clearValidationErrors
} = useOrderValidation()

const validateActiveTab = () => {
  if (!activeTab.value) return {}
  return validateTabData(activeTab.value)
}

// Watchers with proper null checks
watch(
  () => activeTab.value?.tongTienHang,
  async (newTotal, oldTotal) => {
    // Only proceed if activeTab exists and has required data
    if (activeTab.value && newTotal !== oldTotal && activeTab.value.khachHang) {
      // Reload available vouchers when order total changes
      await loadAvailableVouchers()

      // Automatically find and apply best voucher when order total changes
      if (newTotal > 0) {
        await findAndApplyBestVoucher()
      }
    }
  },
  { immediate: false } // Don't run immediately to avoid undefined access
)

// Watch for customer changes to automatically apply vouchers
watch(
  () => activeTab.value?.khachHang,
  async (newCustomer, oldCustomer) => {
    if (activeTab.value && newCustomer && newCustomer !== oldCustomer) {
      // Clear existing vouchers when customer changes
      if (oldCustomer && newCustomer.id !== oldCustomer.id) {
        activeTab.value.voucherList = []
        calculateTabTotals(activeTabId.value)
      }

      // Load available vouchers for new customer
      await loadAvailableVouchers()

      // Automatically find and apply best voucher for new customer
      if (activeTab.value.tongTienHang > 0) {
        await findAndApplyBestVoucher()
      }
    }
  },
  { immediate: false }
)

watch(
  () => activeTab.value,
  (newTab, oldTab) => {
    // Only proceed if we have valid tab data
    if (newTab && newTab !== oldTab) {
      // Clear validation errors when switching tabs
      clearValidationErrors()
      hasUnsavedChanges.value = false
    }
  },
  { deep: true, immediate: false } // Don't run immediately to avoid undefined access
)

watch(
  () => activeTab.value?.isModified,
  (isModified) => {
    // Only update if activeTab exists
    if (activeTab.value) {
      hasUnsavedChanges.value = isModified || false
    }
  },
  { immediate: false } // Don't run immediately to avoid undefined access
)

// Watch for new tabs to automatically set current staff member
watch(
  () => activeTab.value?.id,
  (newTabId, oldTabId) => {
    // When switching to a new tab that doesn't have a staff member assigned
    if (newTabId && newTabId !== oldTabId && activeTab.value && !activeTab.value.nhanVien && currentStaffMember.value) {
      // Auto-assign current staff member to new tabs
      updateActiveTabData({ nhanVien: currentStaffMember.value })
      console.log('Auto-assigned current staff member to new tab:', currentStaffMember.value.hoTen)
    }
  },
  { immediate: false }
)

// Initialize
onMounted(async () => {
  // Initialize current staff member from localStorage
  try {
    const storedUser = localStorage.getItem('nguoiDung')
    if (storedUser) {
      const user = JSON.parse(storedUser)
      if (user && (user.vaiTro === 'STAFF' || user.vaiTro === 'ADMIN')) {
        currentStaffMember.value = user
        console.log('Current staff member loaded:', user.hoTen)
      }
    }
  } catch (error) {
    console.error('Error loading current user:', error)
  }

  // Create first tab if none exist
  if (!hasActiveTabs.value) {
    createNewOrderTab()
  }

  // Ensure we have an active tab after initialization
  if (!activeTab.value && orderTabs.value.length > 0) {
    switchToTab(orderTabs.value[0].id)
  }

  // Prefetch products for better user experience
  await prefetchProducts()

  // Generate initial recommendations
  generateRecommendations()

  // Test customer search with sample data
  console.log('Testing customer search functionality...')
  try {
    const testCustomers = await customerStore.fetchCustomers()
    console.log('Loaded customers for testing:', testCustomers)
  } catch (error) {
    console.error('Failed to load customers:', error)
  }

  // Preload staff data for search functionality
  try {
    await staffStore.fetchStaff()
    console.log('Staff data preloaded for search functionality')
  } catch (error) {
    console.error('Failed to preload staff data:', error)
  }

})

// Watch for cart changes to update recommendations
watch(() => activeTab.value?.sanPhamList, () => {
  generateRecommendations()
}, { deep: true })

// Watch for product store changes to update recommendations
watch(() => productStore.activeProducts, () => {
  generateRecommendations()
}, { deep: true })
</script>

<template>
  <div class="order-create-container">
    <Toast />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-shop text-lg text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">
              Bán hàng tại quầy
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              Quản lý nhiều đơn hàng đồng thời với giao diện tab
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <Button
            label="Quay lại"
            icon="pi pi-arrow-left"
            outlined
            @click="$router.push('/orders')"
          />
        </div>
      </div>
    </div>

    <!-- Order Tabs Navigation -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2 flex-1 overflow-x-auto">
          <!-- Order Tabs -->
          <div
            v-for="tab in orderTabs"
            :key="tab.id"
            class="flex items-center gap-2 px-4 py-2 rounded-lg cursor-pointer transition-all min-w-fit"
            :class="{
              'bg-primary text-white': activeTabId === tab.id,
              'bg-surface-100 hover:bg-surface-200 text-surface-700': activeTabId !== tab.id
            }"
            @click="switchToTab(tab.id)"
          >
            <i class="pi pi-file text-sm"></i>
            <span class="font-medium text-sm">{{ tab.maHoaDon }}</span>
            <Badge
              v-if="tab.sanPhamList.length > 0"
              :value="tab.sanPhamList.length"
              severity="info"
              size="small"
            />
            <Button
              icon="pi pi-times"
              text
              rounded
              size="small"
              class="w-5 h-5 ml-1"
              :class="activeTabId === tab.id ? 'text-white hover:bg-white/20' : 'text-surface-500 hover:bg-surface-300'"
              @click.stop="closeTabWithConfirmation(tab.id)"
            />
          </div>

          <!-- Add New Tab Button -->
          <Button
            v-if="canCreateNewTab"
            icon="pi pi-plus"
            outlined
            rounded
            size="small"
            class="min-w-fit"
            @click="createNewOrderTab"
            v-tooltip.top="'Tạo đơn hàng mới'"
          />
        </div>

        <!-- Tab Actions -->
        <div v-if="hasActiveTabs" class="flex items-center gap-2 ml-4">
          <Button
            icon="pi pi-refresh"
            outlined
            rounded
            size="small"
            @click="calculateTabTotals(activeTabId)"
            v-tooltip.top="'Tính lại tổng tiền'"
          />
          <Button
            icon="pi pi-trash"
            outlined
            rounded
            size="small"
            severity="danger"
            @click="closeTabWithConfirmation(activeTabId)"
            v-tooltip.top="'Đóng tab hiện tại'"
          />
        </div>
      </div>
    </div>



    <!-- Main Order Creation Interface -->
    <div v-if="!hasActiveTabs" class="card">
      <div class="text-center py-12">
        <i class="pi pi-shopping-cart text-6xl text-surface-300 mb-4"></i>
        <h3 class="text-xl font-semibold text-surface-600 mb-2">Chưa có đơn hàng nào</h3>
        <p class="text-surface-500 mb-6">Nhấn nút "+" để tạo đơn hàng mới</p>
        <Button
          label="Tạo đơn hàng đầu tiên"
          icon="pi pi-plus"
          @click="createNewOrderTab"
          size="large"
        />
      </div>
    </div>

    <!-- Active Order Tab Content -->
    <div v-else-if="activeTab" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Left Column: Product Selection & Order Items -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Product Search -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-search text-primary"></i>
            Tìm kiếm sản phẩm
          </div>

          <div class="mb-4">
            <InputGroup>
              <InputGroupAddon v-if="productSearchQuery">
                <Button
                  icon="pi pi-times"
                  text
                  size="small"
                  @click="productSearchQuery = ''; availableProducts = []"
                  class="text-surface-400 hover:text-red-500"
                />
              </InputGroupAddon>
              <InputText
                v-model="productSearchQuery"
                placeholder="Nhập tên sản phẩm, mã sản phẩm..."
                @input="searchProducts"
              />
              <Button
                icon="pi pi-search"
                @click="() => searchProducts(true)"
                outlined
              />
            </InputGroup>
          </div>

          <!-- Loading State -->
          <div v-if="loadingProducts" class="text-center py-8 text-surface-500">
            <i class="pi pi-spin pi-spinner text-2xl mb-2"></i>
            <p>Đang tải sản phẩm...</p>
          </div>

          <!-- Product DataTable -->
          <div v-else class="max-h-96 overflow-y-auto">
            <DataTable
              :value="availableProducts"
              :paginator="false"
              :rows="10"
              dataKey="id"
              class="p-datatable-sm"
              :scrollable="true"
              scrollHeight="350px"
            >
              <Column field="hinhAnh" header="Ảnh" style="width: 80px">
                <template #body="{ data }">
                  <img
                    :src="getProductImage(data) || '/placeholder-product.png'"
                    :alt="data.tenSanPham"
                    class="w-12 h-12 object-cover rounded"
                  />
                </template>
              </Column>

              <Column field="maSanPham" header="Mã sản phẩm" style="width: 120px">
                <template #body="{ data }">
                  <span class="font-mono text-sm bg-surface-100 px-2 py-1 rounded">
                    {{ data.maSanPham }}
                  </span>
                </template>
              </Column>

              <Column field="tenSanPham" header="Tên sản phẩm" style="min-width: 200px">
                <template #body="{ data }">
                  <div class="font-medium text-sm">{{ data.tenSanPham }}</div>
                </template>
              </Column>

              <Column field="thuongHieu" header="Thương hiệu" style="width: 120px">
                <template #body="{ data }">
                  <span v-if="data.thuongHieu" class="text-sm">
                    {{ data.thuongHieu.moTaThuongHieu }}
                  </span>
                  <span v-else class="text-xs text-surface-400 italic">N/A</span>
                </template>
              </Column>

              <Column field="price" header="Giá" style="width: 120px">
                <template #body="{ data }">
                  <div class="text-sm">
                    <div v-if="hasPromotionalPrice(data)" class="space-y-1">
                      <div class="text-red-500 font-semibold">{{ formatCurrency(getProductPrice(data)) }}</div>
                      <div class="text-xs text-surface-500 line-through">{{ formatCurrency(getOriginalPrice(data)) }}</div>
                      <Badge :value="`-${getDiscountPercentage(data)}%`" severity="danger" size="small" />
                    </div>
                    <div v-else class="text-primary font-semibold">
                      {{ formatCurrency(getProductPrice(data)) }}
                    </div>
                  </div>
                </template>
              </Column>

              <Column field="actions" header="Thao tác" style="width: 100px">
                <template #body="{ data }">
                  <Button
                    label="Chọn"
                    icon="pi pi-plus"
                    size="small"
                    @click="showVariantSelectionDialog(data)"
                    :disabled="getAvailableVariantsCount(data) === 0"
                  />
                </template>
              </Column>
            </DataTable>
          </div>

          <!-- Empty State -->
          <div v-if="!loadingProducts && !availableProducts.length" class="text-center py-8 text-surface-500">
            <i class="pi pi-search text-2xl mb-2"></i>
            <p>Không tìm thấy sản phẩm nào</p>
          </div>
        </div>

        <!-- Order Items -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <i class="pi pi-shopping-cart text-primary"></i>
              Sản phẩm trong đơn hàng
            </div>
            <div class="flex items-center gap-2">
              <Button
                label="Quét QR Serial"
                icon="pi pi-qrcode"
                severity="info"
                outlined
                size="small"
                @click="showQRScanner = true"
                v-tooltip.top="'Quét mã QR để thêm serial number vào giỏ hàng'"
              />
              <Badge
                v-if="activeTab?.sanPhamList?.length > 0"
                :value="activeTab.sanPhamList.length"
                severity="info"
              />
            </div>
          </div>

          <!-- Order Items List -->
          <div v-if="activeTab?.sanPhamList?.length" class="space-y-3 mb-4">
            <div
              v-for="(item, index) in activeTab.sanPhamList"
              :key="index"
              class="flex items-center gap-3 p-3 border rounded-lg"
            >
              <img
                :src="getCartItemImage(item) || '/placeholder-product.png'"
                :alt="getCartItemName(item)"
                class="w-12 h-12 object-cover rounded"
              />
              <div class="flex-1">
                <div class="font-medium text-sm">{{ getCartItemName(item) }}</div>
                <div class="text-xs text-surface-500">{{ getCartItemCode(item) }}</div>
                <div class="text-xs text-surface-600 mb-1">
                  {{ getVariantDisplayInfo(item) }}
                </div>
                <div class="text-sm text-primary font-semibold">{{ formatCurrency(item.donGia) }}</div>
              </div>
              <div class="flex items-center gap-2">
                <Button
                  icon="pi pi-minus"
                  text
                  rounded
                  size="small"
                  @click="decreaseQuantity(index)"
                  :disabled="item.soLuong <= 1"
                />
                <span class="text-sm font-medium w-8 text-center">{{ item.soLuong }}</span>
                <Button
                  icon="pi pi-plus"
                  text
                  rounded
                  size="small"
                  @click="increaseQuantity(index)"
                  :disabled="!canIncreaseQuantity(item)"
                />
              </div>
              <div class="text-right">
                <div class="font-semibold text-primary">{{ formatCurrency(item.thanhTien) }}</div>
              </div>
              <Button
                icon="pi pi-trash"
                text
                rounded
                size="small"
                severity="danger"
                @click="removeFromActiveTab(index)"
              />
            </div>
          </div>

          <!-- Empty Cart -->
          <div v-else class="text-center py-8 text-surface-500">
            <i class="pi pi-shopping-cart text-2xl mb-2"></i>
            <p class="text-sm">Chưa có sản phẩm nào trong đơn hàng</p>
            <p class="text-xs">Tìm kiếm và thêm sản phẩm ở phía trên</p>
          </div>
        </div>

        <!-- Product Recommendations Section -->
        <div v-if="recommendedProducts.length > 0" class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-heart text-primary"></i>
            Sản phẩm liên quan
          </div>

          <!-- Horizontal Product Cards -->
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <div
              v-for="product in recommendedProducts.slice(0, 6)"
              :key="product.id"
              class="border rounded-lg p-3 hover:shadow-md transition-shadow cursor-pointer"
              @click="showVariantSelectionDialog(product)"
            >
              <!-- Product Image -->
              <div class="aspect-square mb-3 bg-surface-100 rounded-lg overflow-hidden">
                <img
                  :src="getRecommendationProductImage(product) || '/placeholder-product.png'"
                  :alt="product.tenSanPham"
                  class="w-full h-full object-cover"
                  @error="onRecommendationImageError"
                />
              </div>

              <!-- Product Info -->
              <div class="space-y-2">
                <h4 class="font-medium text-sm line-clamp-2">{{ product.tenSanPham }}</h4>

                <div class="text-xs text-surface-500">
                  {{ product.maSanPham }}
                </div>

                <div v-if="product.thuongHieu" class="text-xs text-surface-600">
                  {{ product.thuongHieu.moTaThuongHieu }}
                </div>

                <!-- Price -->
                <div class="text-sm">
                  <div v-if="hasPromotionalPrice(product)" class="space-y-1">
                    <div class="text-red-500 font-semibold">{{ formatCurrency(getProductPrice(product)) }}</div>
                    <div class="text-xs text-surface-500 line-through">{{ formatCurrency(getOriginalPrice(product)) }}</div>
                  </div>
                  <div v-else class="text-primary font-semibold">
                    {{ formatCurrency(getProductPrice(product)) }}
                  </div>
                </div>

                <!-- Available Variants -->
                <div class="flex items-center justify-between">
                  <div class="text-xs text-surface-500">
                    {{ getAvailableVariantsCount(product) }} phiên bản
                  </div>
                  <Button
                    icon="pi pi-plus"
                    size="small"
                    rounded
                    :disabled="getAvailableVariantsCount(product) === 0"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Show More Button -->
          <div v-if="allRecommendedProducts.length > 6" class="text-center mt-4">
            <Button
              :label="showAllRecommendations ? 'Thu gọn' : `Xem thêm ${allRecommendedProducts.length - 6} sản phẩm`"
              icon="pi pi-angle-down"
              :iconPos="showAllRecommendations ? 'right' : 'right'"
              text
              @click="toggleRecommendations"
            />
          </div>
        </div>
      </div>

      <!-- Right Column: Order Summary & Actions -->
      <div class="lg:col-span-1 space-y-6">
        <!-- Customer Selection -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <i class="pi pi-user text-primary"></i>
              Khách hàng
            </div>
            <Button
              label="Thêm nhanh"
              icon="pi pi-user-plus"
              size="small"
              severity="success"
              outlined
              @click="showFastCustomerDialog"
            />
          </div>

          <!-- Customer Search -->
          <div class="mb-4">
            <AutoComplete
              v-model="selectedCustomer"
              :suggestions="customerSuggestions"
              @complete="searchCustomers"
              @item-select="onCustomerSelect"
              optionLabel="hoTen"
              placeholder="Tìm kiếm khách hàng..."
              fluid
            >
              <template #item="{ item }">
                <div class="flex items-center gap-2 p-2">
                  <Avatar :label="item.hoTen?.charAt(0)" size="small" />
                  <div>
                    <div class="font-medium">{{ item.hoTen }}</div>
                    <div class="text-sm text-surface-500">{{ item.soDienThoai }}</div>
                  </div>
                </div>
              </template>
            </AutoComplete>
          </div>

          <!-- Selected Customer Display -->
          <div v-if="activeTab?.khachHang" class="p-3 border rounded-lg bg-surface-50">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <Avatar :label="activeTab.khachHang.hoTen?.charAt(0)" size="small" />
                <div>
                  <div class="font-semibold text-sm">{{ activeTab.khachHang.hoTen }}</div>
                  <div class="text-xs text-surface-500">{{ activeTab.khachHang.soDienThoai }}</div>
                </div>
              </div>
              <Button
                icon="pi pi-times"
                text
                rounded
                size="small"
                @click="clearCustomerFromTab"
                class="text-surface-400 hover:text-red-500"
              />
            </div>
          </div>

          <!-- Walk-in Customer Note -->
          <div v-else class="text-center py-3 text-surface-500">
            <i class="pi pi-user-plus text-lg mb-1"></i>
            <p class="text-xs">Khách hàng vãng lai</p>
          </div>
        </div>

        <!-- Staff Member Section -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-user-check text-primary"></i>
            Nhân viên phụ trách
          </div>

          <!-- Staff Member Search -->
          <div class="mb-4">
            <AutoComplete
              v-model="selectedStaffMember"
              :suggestions="staffSuggestions"
              @complete="searchStaffMembers"
              @item-select="onStaffMemberSelect"
              optionLabel="hoTen"
              placeholder="Tìm kiếm nhân viên..."
              fluid
            >
              <template #item="{ item }">
                <div class="flex items-center gap-2 p-2">
                  <Avatar :label="item.hoTen?.charAt(0)" size="small" />
                  <div>
                    <div class="font-medium">{{ item.hoTen }}</div>
                    <div class="text-sm text-surface-500">{{ item.soDienThoai }}</div>
                  </div>
                </div>
              </template>
            </AutoComplete>
          </div>

          <!-- Selected Staff Member Display -->
          <div v-if="activeTab?.nhanVien" class="p-3 border rounded-lg ">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <Avatar :label="activeTab.nhanVien.hoTen?.charAt(0)" size="small" />
                <div>
                  <div class="font-semibold text-sm">{{ activeTab.nhanVien.hoTen }}</div>
                  <div class="text-xs ">{{ activeTab.nhanVien.soDienThoai }}</div>
                </div>
              </div>
              <Button
                icon="pi pi-times"
                text
                rounded
                size="small"
                @click="clearStaffMemberFromTab"
                class="text-surface-400 hover:text-red-500"
              />
            </div>
          </div>

          <!-- Current User Note -->
          <div v-else-if="currentStaffMember" class="text-center py-3 text-surface-500">
            <i class="pi pi-user text-lg mb-1"></i>
            <p class="text-xs">Sử dụng nhân viên hiện tại: {{ currentStaffMember.hoTen }}</p>
          </div>

          <!-- No Staff Member Note -->
          <div v-else class="text-center py-3 text-surface-500">
            <i class="pi pi-exclamation-triangle text-lg mb-1"></i>
            <p class="text-xs">Chưa có nhân viên phụ trách</p>
          </div>
        </div>

        <!-- Delivery Options -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-truck text-primary"></i>
            Giao hàng
          </div>

          <div class="flex items-center justify-between mb-4">
            <label class="font-medium">Giao hàng tận nơi</label>
            <ToggleButton
              v-model="activeTab.giaohang"
              onLabel="Có"
              offLabel="Không"
              @change="onDeliveryToggle"
              :disabled="!activeTab"
            />
          </div>

          <!-- Delivery Address (when delivery is enabled) -->
          <div v-if="activeTab?.giaohang" class="space-y-3">
            <div v-if="activeTab?.khachHang?.diaChis?.length" class="space-y-2">
              <label class="text-sm font-medium">Chọn địa chỉ giao hàng:</label>
              <div
                v-for="address in activeTab.khachHang.diaChis"
                :key="address.id"
                class="border rounded-lg p-2 cursor-pointer transition-all"
                :class="{
                  'border-primary bg-primary/5': activeTab?.diaChiGiaoHang?.id === address.id,
                  'border-surface-200 hover:border-primary/50': activeTab?.diaChiGiaoHang?.id !== address.id
                }"
                @click="selectDeliveryAddress(address)"
              >
                <div class="text-sm font-medium">{{ address.loaiDiaChi }}</div>
                <div class="text-xs text-surface-500">
                  {{ address.duong }}, {{ address.phuongXa }}, {{ address.quanHuyen }}
                </div>
                <div class="text-xs text-surface-500">{{ address.tinhThanh }}</div>
              </div>
            </div>
            <div v-else-if="activeTab?.khachHang" class="text-center py-4 text-surface-500">
              <i class="pi pi-map-marker text-lg mb-2"></i>
              <p class="text-xs mb-3">Khách hàng chưa có địa chỉ giao hàng</p>
              <Button
                label="Thêm địa chỉ giao hàng"
                icon="pi pi-plus"
                size="small"
                severity="info"
                outlined
                @click="showFastAddressDialog"
              />
            </div>
            <div v-else class="text-center py-3 text-surface-500">
              <p class="text-xs">Vui lòng chọn khách hàng trước</p>
            </div>
          </div>
        </div>

        <!-- Voucher Section -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-tag text-primary"></i>
            Voucher giảm giá
          </div>

          <!-- Voucher Input -->
          <div class="mb-4">
            <InputGroup>
              <InputGroupAddon v-if="voucherCode">
                <Button
                  icon="pi pi-times"
                  text
                  size="small"
                  @click="voucherCode = ''"
                  class="text-surface-400 hover:text-red-500"
                />
              </InputGroupAddon>
              <InputText
                v-model="voucherCode"
                placeholder="Nhập mã voucher..."
                @keyup.enter="applyVoucher"
              />
              <Button
                label="Áp dụng"
                @click="applyVoucher"
                :loading="applyingVoucher"
                :disabled="!voucherCode.trim()"
              />
            </InputGroup>
          </div>





          <!-- Applied Vouchers -->
          <div v-if="activeTab?.voucherList?.length" class="space-y-2 mb-4">
            <div
              v-for="(voucher, index) in activeTab.voucherList"
              :key="index"
              class="flex items-center justify-between p-3 border rounded-lg bg-green-50"
            >
              <div>
                <div class="font-medium text-green-800 text-sm">{{ voucher.maPhieuGiamGia }}</div>
                <div class="text-xs text-green-600">
                  Giảm {{ formatCurrency(voucher.giaTriGiam) }}
                </div>
              </div>
              <Button
                icon="pi pi-times"
                text
                rounded
                size="small"
                severity="danger"
                @click="removeVoucherFromTab(index)"
              />
            </div>
          </div>

          <!-- Available Vouchers -->
          <div v-if="availableVouchers.length" class="mb-4">
            <div class="font-medium mb-2 text-sm">Voucher khả dụng</div>
            <div class="space-y-2 max-h-32 overflow-y-auto">
              <div
                v-for="voucher in availableVouchers"
                :key="voucher.id"
                class="flex items-center justify-between p-2 border rounded-lg hover:bg-surface-50 cursor-pointer"
                @click="selectVoucher(voucher)"
              >
                <div>
                  <div class="font-medium text-sm">{{ voucher.tenPhieuGiamGia }}</div>
                  <div class="text-xs text-surface-500">{{ voucher.maPhieuGiamGia }}</div>
                  <div class="text-xs text-primary">
                    Giảm {{ formatCurrency(calculateVoucherDiscount(voucher)) }}
                  </div>
                </div>
                <Button
                  icon="pi pi-plus"
                  text
                  rounded
                  size="small"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Payment Section -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-credit-card text-primary"></i>
            Thanh toán
          </div>

          <!-- Payment Methods -->
          <div v-if="paymentMethods.length === 0" class="text-center py-4 text-surface-500 mb-4">
            <i class="pi pi-info-circle text-2xl mb-2"></i>
            <p>Không có phương thức thanh toán khả dụng</p>
            <p class="text-sm">Vui lòng kiểm tra lại tùy chọn giao hàng</p>
          </div>
          <div v-else class="space-y-3 mb-4">
            <div
              v-for="method in paymentMethods"
              :key="method.value"
              class="border rounded-lg p-3 cursor-pointer transition-all"
              :class="{
                'border-primary bg-primary/5': activeTab?.phuongThucThanhToan === method.value,
                'border-surface-200 hover:border-primary/50': activeTab?.phuongThucThanhToan !== method.value,
                'opacity-50 cursor-not-allowed': !method.available
              }"
              @click="method.available && selectPaymentMethod(method.value)"
            >
              <div class="flex items-center gap-3">
                <i :class="method.icon" class="text-lg text-primary"></i>
                <div>
                  <div class="font-semibold text-sm">{{ method.label }}</div>
                  <div class="text-xs text-surface-500">{{ method.description }}</div>
                  <div v-if="!method.available" class="text-xs text-red-500 mt-1">
                    Không khả dụng
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Order Summary -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-calculator text-primary"></i>
            Tổng kết đơn hàng
          </div>

          <div v-if="activeTab" class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span>Tạm tính:</span>
              <span>{{ formatCurrency(activeTab.tongTienHang || 0) }}</span>
            </div>
            <div v-if="activeTab.giaTriGiamGiaVoucher > 0" class="flex justify-between text-green-600">
              <span>Giảm giá voucher:</span>
              <span>-{{ formatCurrency(activeTab.giaTriGiamGiaVoucher) }}</span>
            </div>
            <div v-if="activeTab.giaohang" class="flex justify-between">
              <span>Phí giao hàng:</span>
              <span>{{ formatCurrency(activeTab.phiVanChuyen || 0) }}</span>
            </div>
            <hr class="my-2">
            <div class="flex justify-between font-semibold text-lg">
              <span>Tổng cộng:</span>
              <span class="text-primary">{{ formatCurrency(activeTab.tongThanhToan || 0) }}</span>
            </div>

            <!-- Customer Payment Section (for cash payments) -->
            <div v-if="activeTab?.phuongThucThanhToan === 'TIEN_MAT'" class="mt-4 pt-4 border-t border-surface-200">
              <div class="space-y-3">
                <div>
                  <label class="block text-sm font-medium mb-1">Khách hàng đưa:</label>
                  <InputText
                    v-model.number="customerPayment"
                    type="number"
                    placeholder="Nhập số tiền khách đưa..."
                    class="w-full"
                    :min="activeTab?.tongThanhToan || 0"
                    @input="calculateChange"
                  />
                </div>
                <div v-if="customerPayment >= (activeTab?.tongThanhToan || 0)" class="flex justify-between font-semibold text-lg">
                  <span>Tiền trả lại:</span>
                  <span class="text-green-600">{{ formatCurrency(changeAmount) }}</span>
                </div>
                <div v-else-if="customerPayment > 0" class="text-red-500 text-sm">
                  Số tiền không đủ (thiếu {{ formatCurrency((activeTab?.tongThanhToan || 0) - customerPayment) }})
                </div>
              </div>
            </div>
          </div>

          <!-- Create Order Button -->
          <div class="mt-6 pt-4 border-t border-surface-200">
            <Button
              label="Tạo đơn hàng"
              icon="pi pi-check"
              severity="success"
              size="large"
              class="w-full"
              @click="createOrderFromActiveTab"
              :loading="creating"
              :disabled="!canCreateActiveOrder || creating"
            />
            <div v-if="!canCreateActiveOrder" class="text-center mt-2">
              <small class="text-surface-500">
                <span v-if="!activeTab?.sanPhamList?.length">Vui lòng thêm sản phẩm vào đơn hàng</span>
                <span v-else-if="!activeTab?.phuongThucThanhToan">Vui lòng chọn phương thức thanh toán</span>
                <span v-else-if="activeTab?.giaohang && (!activeTab?.khachHang || !activeTab?.diaChiGiaoHang)">
                  Vui lòng chọn khách hàng và địa chỉ giao hàng
                </span>
              </small>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>



  <!-- Product Variant Selection Dialog -->
  <ProductVariantDialog
    v-model:visible="variantDialogVisible"
    :product="selectedProductForVariants"
    @variant-selected="addVariantToActiveTab"
  />

  <!-- Fast Customer Creation Dialog -->
  <FastCustomerCreate
    v-model:visible="fastCustomerDialogVisible"
    @customer-created="onCustomerCreated"
  />

  <!-- Fast Address Creation Dialog -->
  <FastAddressCreate
    v-model:visible="fastAddressDialogVisible"
    :customer="activeTab?.khachHang"
    @address-created="onAddressCreated"
  />

  <!-- QR Scanner Dialog -->
  <Dialog
    v-model:visible="showQRScanner"
    modal
    header="Quét QR Serial Number"
    :style="{ width: '500px' }"
    @hide="stopQRScanner"
    :closable="true"
  >
    <div class="text-center">
      <p class="mb-4">Quét mã QR chứa serial number để tự động thêm vào giỏ hàng</p>
      <div class="border-2 border-primary border-dashed rounded-lg p-4 mb-4">
        <!-- QR Scanner component -->
        <div class="flex flex-col items-center justify-center">
          <div v-if="!qrScanResult" class="w-full">
            <div v-if="!cameraError">
              <qrcode-stream
                @detect="onQRDetect"
                @init="onQRInit"
                :track="paintBoundingBox"
                class="w-full h-64 rounded-lg overflow-hidden"
              />
              <p class="text-surface-600 mt-2">Đưa mã QR chứa serial number vào khung hình</p>
            </div>
            <div v-else class="p-4 bg-red-50 rounded-lg">
              <i class="pi pi-exclamation-triangle text-red-500 text-xl"></i>
              <p class="text-red-700 font-medium mt-2">Lỗi khi truy cập camera</p>
              <p class="mt-2">{{ cameraError }}</p>
              <Button
                label="Cấp quyền camera"
                icon="pi pi-camera"
                severity="info"
                @click="requestCameraPermission"
                class="mt-3"
              />
            </div>
          </div>
          <div v-else class="p-4 bg-green-50 rounded-lg w-full">
            <p class="text-green-700 font-medium">Quét thành công!</p>
            <p class="mt-2 font-mono">{{ qrScanResult }}</p>
            <div v-if="qrProcessingResult" class="mt-3">
              <div v-if="qrProcessingResult.success" class="text-green-600">
                <i class="pi pi-check-circle mr-2"></i>
                {{ qrProcessingResult.message }}
              </div>
              <div v-else class="text-red-600">
                <i class="pi pi-times-circle mr-2"></i>
                {{ qrProcessingResult.message }}
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="flex justify-center gap-2">
        <Button
          label="Đóng"
          icon="pi pi-times"
          severity="secondary"
          @click="showQRScanner = false"
        />
        <Button
          v-if="qrScanResult && qrProcessingResult?.success"
          label="Quét tiếp"
          icon="pi pi-refresh"
          severity="info"
          @click="resetQRScanner"
        />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>


