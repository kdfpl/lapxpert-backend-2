<script setup>
import { ref, computed, onBeforeMount } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useRoute, useRouter } from 'vue-router'
import { useDiscountStore } from '@/stores/discountstore'
import { useProductStore } from '@/stores/productstore'
import discountService from '@/apis/discount'

// PrimeVue Components
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import ProgressSpinner from 'primevue/progressspinner'
import Tag from 'primevue/tag'
import DatePicker from 'primevue/datepicker'
import Badge from 'primevue/badge'
import Textarea from 'primevue/textarea'
import ConfirmDialog from 'primevue/confirmdialog'


// --- Store Access ---
const discountStore = useDiscountStore()
const productStore = useProductStore()

// --- Router & Route ---
const route = useRoute()
const router = useRouter()

// --- State ---
const discount = ref({}) // Holds the discount being edited or created
const products = computed(() => productStore.activeProducts)
const selectedProductDetails = ref(new Map()) // Map<productId, SanPhamChiTiet[]> for selections in the dialog table
const originalSelectedProductDetailIds = ref(new Set()) // Tracks original product detail IDs for diffing on update
const submitted = ref(false) // Form submission status for validation
const expandedRows = ref([]) // Controls expanded rows in the product table (dialog)
const isLoading = ref(false)
const pageTitle = ref('Thêm đợt giảm giá mới')
const auditHistory = ref([]) // Audit history for the discount
const isLoadingAudit = ref(false)
const auditReason = ref('') // Audit reason for updates

// --- PrimeVue Utilities ---
const toast = useToast()
const confirm = useConfirm()

// --- Enhanced Store Integration ---
// Using the enhanced discountStore for comprehensive state management

// --- Lifecycle Hooks ---
onBeforeMount(async () => {
  isLoading.value = true
  try {
    // Always fetch active products with details for selection
    await productStore.fetchActiveProducts()
    console.log('Loaded products:', products.value.length)
    console.log('Sample product structure:', products.value[0])

    const discountId = route.params.id
    if (discountId) {
      pageTitle.value = 'Chỉnh sửa đợt giảm giá'
      // Fetch existing discount data
      const existingDiscount = await discountService.getDiscountById(discountId)
      if (existingDiscount) {
        // Convert ISO string dates to Date objects for DatePicker compatibility
        discount.value = {
          ...existingDiscount,
          ngayBatDau: existingDiscount.ngayBatDau ? new Date(existingDiscount.ngayBatDau) : null,
          ngayKetThuc: existingDiscount.ngayKetThuc ? new Date(existingDiscount.ngayKetThuc) : null
        }

        // Fetch and pre-select products associated with this discount
        const discountProducts = await discountService.getDiscountProducts(discount.value.id)
        console.log('Discount products:', discountProducts)

        const discountProductIds = new Set(discountProducts.map((p) => p.id))
        originalSelectedProductDetailIds.value = new Set(discountProductIds)

        // Pre-select associated product details
        products.value.forEach((product) => {
          if (product.sanPhamChiTiets && product.sanPhamChiTiets.length > 0) {
            const selectedDetailsForProduct = product.sanPhamChiTiets.filter((detail) =>
              discountProductIds.has(detail.id),
            )
            if (selectedDetailsForProduct.length > 0) {
              selectedProductDetails.value.set(product.id, selectedDetailsForProduct)
            }
          }
        })

        // Load audit history for existing discount
        await loadAuditHistory(discount.value.id)
      } else {
        toast.add({ severity: 'error', summary: 'Lỗi', detail: 'Không tìm thấy đợt giảm giá.', life: 3000 })
        router.push({ name: 'DiscountList' })
      }
    } else {
      // New discount - status will be automatically computed by backend
      discount.value = {
        ngayBatDau: null,
        ngayKetThuc: null
      }
      selectedProductDetails.value.clear()
      originalSelectedProductDetailIds.value.clear()
    }
  } catch (error) {
    console.error('Error loading discount form data:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải dữ liệu cho form.',
      life: 3000,
    })
    router.push({ name: 'DiscountList' })
  } finally {
    isLoading.value = false
  }
})

// --- Utility Functions ---
const formatCurrency = (value) => {
  if (typeof value !== 'number') return ''
  return value.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })
}

// --- Audit History Functions ---
async function loadAuditHistory(discountId) {
  if (!discountId) return

  isLoadingAudit.value = true
  try {
    console.log('Loading audit history for discount ID:', discountId)
    const response = await discountService.getDiscountAuditHistory(discountId)
    auditHistory.value = response || []
    console.log('Loaded audit history:', auditHistory.value)
    console.log('Audit history length:', auditHistory.value.length)

    // Debug: Log the structure of each audit entry
    auditHistory.value.forEach((entry, index) => {
      console.log(`Audit entry ${index}:`, {
        id: entry.id,
        hanhDong: entry.hanhDong,
        thoiGianThayDoi: entry.thoiGianThayDoi,
        nguoiThucHien: entry.nguoiThucHien,
        lyDoThayDoi: entry.lyDoThayDoi,
        giaTriCu: entry.giaTriCu,
        giaTriMoi: entry.giaTriMoi,
        hasOldValues: !!entry.giaTriCu,
        hasNewValues: !!entry.giaTriMoi,
        oldValuesLength: entry.giaTriCu ? entry.giaTriCu.length : 0,
        newValuesLength: entry.giaTriMoi ? entry.giaTriMoi.length : 0
      })
    })
  } catch (error) {
    console.error('Error loading audit history:', error)
    auditHistory.value = []
    // Don't show error toast for audit history as it's not critical
  } finally {
    isLoadingAudit.value = false
  }
}

const getActionDisplayName = (action) => {
  const actionMap = {
    'CREATE': 'Tạo mới',
    'UPDATE': 'Cập nhật',
    'DELETE': 'Xóa',
    'STATUS_CHANGE': 'Thay đổi trạng thái',
    'PRODUCT_ASSIGNMENT': 'Gán sản phẩm'
  }
  return actionMap[action] || action
}

const formatAuditDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  // Format in Vietnam timezone following PhieuGiamGia pattern
  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'Asia/Ho_Chi_Minh'
  })
}



const getFieldDisplayName = (field) => {
  const fieldNames = {
    maDotGiamGia: 'Mã đợt giảm giá',
    tenDotGiamGia: 'Tên đợt giảm giá',
    phanTramGiam: 'Phần trăm giảm',
    ngayBatDau: 'Ngày bắt đầu',
    ngayKetThuc: 'Ngày kết thúc',
    trangThai: 'Trạng thái'
  }
  return fieldNames[field] || field
}



// Enhanced audit trail functions following StaffForm/CustomerForm patterns
const formatAuditValue = (field, value) => {
  if (value === null || value === undefined) return 'Không có'

  const statusMap = {
    'CHUA_DIEN_RA': 'Chưa diễn ra',
    'DA_DIEN_RA': 'Đã diễn ra',
    'KET_THUC': 'Kết thúc',
    'BI_HUY': 'Bị hủy'
  }

  switch (field) {
    case 'trangThai':
      return statusMap[value] || value
    case 'ngayBatDau':
    case 'ngayKetThuc':
      return value ? new Date(value).toLocaleString('vi-VN') : 'Không có'
    case 'phanTramGiam':
      return value ? `${value}%` : 'Không có'
    default:
      return String(value)
  }
}

const getAuditBorderColor = (action) => {
  const colorMap = {
    'CREATE': 'border-green-400 bg-green-50',
    'UPDATE': 'border-blue-400 bg-blue-50',
    'DELETE': 'border-red-400 bg-red-50',
    'STATUS_CHANGE': 'border-orange-400 bg-orange-50',
    'PRODUCT_ASSIGNMENT': 'border-purple-400 bg-purple-50'
  }
  return colorMap[action] || 'border-surface-400 bg-surface-50'
}

const formatJsonData = (jsonString) => {
  if (!jsonString) return ''
  try {
    const data = JSON.parse(jsonString)
    return Object.entries(data)
      .map(([key, value]) => `${key}: ${value}`)
      .join(', ')
  } catch {
    return jsonString
  }
}

// Enhanced parseAuditChanges following CrudCoupons pattern - only show changed fields
const parseAuditChangesEnhanced = (oldValuesJson, newValuesJson) => {
  if (!oldValuesJson && !newValuesJson) return []

  try {
    // Handle different audit scenarios
    if (oldValuesJson && !newValuesJson) {
      // DELETE operation - show all old values
      const oldValues = JSON.parse(oldValuesJson)
      const changes = []
      for (const [field, value] of Object.entries(oldValues)) {
        changes.push({
          field: field,
          fieldName: getFieldDisplayName(field),
          oldValue: formatAuditValue(field, value),
          newValue: 'Đã xóa',
          isChanged: true
        })
      }
      return changes
    }

    if (!oldValuesJson && newValuesJson) {
      // CREATE operation - show all new values
      const newValues = JSON.parse(newValuesJson)
      const changes = []
      for (const [field, value] of Object.entries(newValues)) {
        changes.push({
          field: field,
          fieldName: getFieldDisplayName(field),
          oldValue: 'Không có',
          newValue: formatAuditValue(field, value),
          isChanged: true
        })
      }
      return changes
    }

    // UPDATE operation - only show changed fields (following CrudCoupons pattern)
    const oldValues = JSON.parse(oldValuesJson)
    const newValues = JSON.parse(newValuesJson)
    const changes = []

    // Compare values and only include changed fields
    for (const [field, newValue] of Object.entries(newValues)) {
      const oldValue = oldValues[field]

      // Only include fields that actually changed
      if (JSON.stringify(oldValue) !== JSON.stringify(newValue)) {
        changes.push({
          field: field,
          fieldName: getFieldDisplayName(field),
          oldValue: formatAuditValue(field, oldValue),
          newValue: formatAuditValue(field, newValue),
          isChanged: true
        })
      }
    }

    return changes
  } catch (error) {
    console.error('Error parsing audit changes:', error)
    return []
  }
}

// Enhanced parseCreateAuditValues following StaffForm/CustomerForm patterns
const parseCreateAuditValuesEnhanced = (newValuesJson) => {
  if (!newValuesJson) return []

  try {
    const newValues = JSON.parse(newValuesJson)
    const fields = []

    // Process each field
    for (const [field, value] of Object.entries(newValues)) {
      if (value !== null && value !== undefined) {
        fields.push({
          field: field,
          fieldName: getFieldDisplayName(field),
          value: formatAuditValue(field, value)
        })
      }
    }

    return fields
  } catch (error) {
    console.error('Error parsing create audit values:', error)
    return []
  }
}

const getAuditIcon = (action) => {
  const iconMap = {
    'CREATE': 'pi pi-plus-circle',
    'UPDATE': 'pi pi-pencil',
    'DELETE': 'pi pi-trash',
    'STATUS_CHANGE': 'pi pi-refresh',
    'PRODUCT_ASSIGNMENT': 'pi pi-link'
  }
  return iconMap[action] || 'pi pi-info-circle'
}

const getAuditIconColor = (action) => {
  const colorMap = {
    'CREATE': 'text-green-600',
    'UPDATE': 'text-blue-600',
    'DELETE': 'text-red-600',
    'STATUS_CHANGE': 'text-orange-600',
    'PRODUCT_ASSIGNMENT': 'text-purple-600'
  }
  return colorMap[action] || 'text-gray-600'
}

// Product status helper functions (for SanPhamChiTiet table)
const getProductStatusLabel = (status) => {
  const statusMap = {
    'AVAILABLE': 'Có sẵn',
    'RESERVED': 'Đã đặt trước',
    'SOLD': 'Đã bán',
    'DAMAGED': 'Hỏng',
    'MAINTENANCE': 'Bảo trì'
  }
  return statusMap[status] || status
}

const getProductStatusSeverity = (status) => {
  const severityMap = {
    'AVAILABLE': 'success',
    'RESERVED': 'warning',
    'SOLD': 'info',
    'DAMAGED': 'danger',
    'MAINTENANCE': 'secondary'
  }
  return severityMap[status] || 'secondary'
}

// Campaign status helper functions (for DotGiamGia)
const getCampaignStatusLabel = (trangThai) => {
  const labelMap = {
    CHUA_DIEN_RA: 'Chưa diễn ra',
    DA_DIEN_RA: 'Đã diễn ra',
    KET_THUC: 'Kết thúc',
    BI_HUY: 'Bị hủy',
  }
  return labelMap[trangThai] || 'Không xác định'
}

const getCampaignStatusSeverity = (trangThai) => {
  const severityMap = {
    CHUA_DIEN_RA: 'warn',
    DA_DIEN_RA: 'success',
    KET_THUC: 'danger',
    BI_HUY: 'secondary',
  }
  return severityMap[trangThai] || null
}

// Color mapping function following ProductVariantManager.vue patterns
const getColorValue = (colorName) => {
  const colorMap = {
    'Đỏ': '#ef4444',
    'Xanh': '#3b82f6',
    'Vàng': '#eab308',
    'Đen': '#000000',
    'Trắng': '#ffffff',
    'Xám': '#6b7280',
    'Hồng': '#ec4899',
    'Tím': '#8b5cf6',
    'Cam': '#f97316',
    'Xanh lá': '#22c55e'
  }
  return colorMap[colorName] || '#6b7280'
}

// --- CRUD Action Methods ---
async function saveDiscount() {
  submitted.value = true

  if (
    !discount.value.maDotGiamGia?.trim() ||
    !discount.value.tenDotGiamGia?.trim() ||
    discount.value.phanTramGiam == null ||
    !discount.value.ngayBatDau ||
    !discount.value.ngayKetThuc
  ) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng điền đầy đủ thông tin bắt buộc!',
      life: 3000,
    })
    return
  }

  // Validate audit reason for updates
  if (discount.value.id && !auditReason.value?.trim()) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng nhập lý do thay đổi!',
      life: 3000,
    })
    return
  }

  if (
    discount.value.ngayBatDau &&
    discount.value.ngayKetThuc &&
    new Date(discount.value.ngayKetThuc) < new Date(discount.value.ngayBatDau)
  ) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Ngày kết thúc không được trước ngày bắt đầu!',
      life: 3000,
    })
    return
  }

  isLoading.value = true;
  try {
    const isUpdating = !!discount.value.id
    let savedDiscountData

    // Prepare data for backend - convert Date objects to ISO strings
    const discountData = {
      ...discount.value,
      ngayBatDau: discount.value.ngayBatDau ? discount.value.ngayBatDau.toISOString() : null,
      ngayKetThuc: discount.value.ngayKetThuc ? discount.value.ngayKetThuc.toISOString() : null
    }

    if (isUpdating) {
      // Use audit-aware method for updates
      try {
        savedDiscountData = await discountService.saveDiscountWithAudit(discountData, auditReason.value.trim())
      } catch (error) {
        console.warn('Audit save method failed, using standard method:', error)
        savedDiscountData = await discountService.saveDiscount(discountData)
      }
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Cập nhật thông tin đợt giảm giá thành công',
        life: 2000,
      })
    } else {
      savedDiscountData = await discountService.saveDiscount(discountData)
      discount.value.id = savedDiscountData.id // Get the new ID for product association
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Tạo đợt giảm giá thành công',
        life: 3000,
      })
    }

    const discountId = discount.value.id
    const currentSelectedIds = new Set(
      Array.from(selectedProductDetails.value.values())
        .flat()
        .map((item) => item.id)
        .filter(id => id != null), // Filter out any null/undefined IDs
    )

    console.log('Current selected product detail IDs:', Array.from(currentSelectedIds))

    if (isUpdating) {
      const idsToAdd = [...currentSelectedIds].filter(
        (id) => !originalSelectedProductDetailIds.value.has(id),
      )
      const idsToRemove = [...originalSelectedProductDetailIds.value].filter(
        (id) => !currentSelectedIds.has(id),
      )

      const associationPromises = []
      if (idsToRemove.length > 0) {
        associationPromises.push(
          discountService.removeDiscountFromProducts(discountId, idsToRemove),
        )
      }
      if (idsToAdd.length > 0) {
        associationPromises.push(discountService.addDiscountToProducts(discountId, idsToAdd))
      }

      if (associationPromises.length > 0) {
        await Promise.all(associationPromises)
        toast.add({
          severity: 'info',
          summary: 'Thông tin',
          detail: 'Cập nhật liên kết sản phẩm thành công',
          life: 3000,
        })
      }
    } else {
      const allSelectedIdsForNew = Array.from(currentSelectedIds)
      if (allSelectedIdsForNew.length > 0) {
        await discountService.addDiscountToProducts(discountId, allSelectedIdsForNew)
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đã thêm ${allSelectedIdsForNew.length} sản phẩm vào đợt giảm giá mới`,
          life: 3000,
        })
      }
    }

    await discountStore.fetchDiscounts() // Refresh the main list in the store

    // Reload audit history if this was an update
    if (isUpdating) {
      await loadAuditHistory(discount.value.id)
      auditReason.value = '' // Clear the audit reason after successful save
    }

    router.push({ name: 'DiscountList' }) // Or your discount list route name
  } catch (error) {
    console.error('Error saving discount:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Lưu thất bại: ${error.response?.data?.message || error.message || 'Lỗi không xác định'}`,
      life: 3000,
    })
  } finally {
    isLoading.value = false;
    submitted.value = false
  }
}

// --- Product Selection Methods ---
function handleProductDetailSelection(productId, selection) {
  console.log('Product selection changed:', productId, selection)
  if (selection && selection.length > 0) {
    selectedProductDetails.value.set(productId, selection)
  } else {
    selectedProductDetails.value.delete(productId)
  }
}

// --- Table Interaction Methods ---
function expandAll() {
  expandedRows.value = products.value.reduce((acc, product) => {
    acc[product.id] = true
    return acc
  }, {})
}

function collapseAll() {
  expandedRows.value = []
}

function goBack() {
  router.push({ name: 'DiscountList' })
}

// Delete functionality
function confirmDeleteDiscount() {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa đợt giảm giá "${discount.value.tenDotGiamGia}"?`,
    header: 'Xác nhận xóa',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {
      label: 'Hủy',
      severity: 'secondary',
      outlined: true
    },
    acceptProps: {
      label: 'Xóa',
      severity: 'danger'
    },
    accept: () => {
      deleteDiscount()
    }
  })
}

async function deleteDiscount() {
  if (!discount.value.id) return

  try {
    isLoading.value = true

    // Call the delete API (sets status to BI_HUY)
    await discountService.deleteDiscount(discount.value.id)

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã xóa đợt giảm giá thành công',
      life: 3000
    })

    // Navigate back to list
    router.push({ name: 'DiscountList' })
  } catch (error) {
    console.error('Error deleting discount:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.response?.data?.message || 'Có lỗi xảy ra khi xóa đợt giảm giá',
      life: 5000
    })
  } finally {
    isLoading.value = false
  }
}

// Status options and helper functions removed - now using discountStore methods for consistency


</script>

<template>
  <div class="min-h-screen bg-surface-50">
    <Toast />
    <ConfirmDialog />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-percentage text-lg text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">
              {{ discount.id ? 'Cập nhật đợt giảm giá' : 'Thêm đợt giảm giá mới' }}
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              {{ discount.id ? 'Chỉnh sửa thông tin đợt giảm giá' : 'Tạo đợt giảm giá mới trong hệ thống' }}
            </p>
          </div>
        </div>
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          size="small"
          @click="goBack"
          v-tooltip.left="'Quay lại'"
          :disabled="isLoading"
        />
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="card">
      <div class="text-center py-8">
        <ProgressSpinner />
        <p class="text-surface-500 mt-4">Đang tải dữ liệu...</p>
      </div>
    </div>

    <!-- Main Form -->
    <form v-else @submit.prevent="saveDiscount" class="space-y-6">

      <!-- Basic Information Section -->
      <div class="card">
        <div class="flex items-center gap-2 mb-6">
          <i class="pi pi-info-circle text-primary"></i>
          <h2 class="text-lg font-semibold text-surface-900 m-0">Thông tin cơ bản</h2>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- Mã đợt giảm giá -->
          <div class="flex flex-col gap-2">
            <label for="maDotGiamGia" class="font-semibold">
              Mã đợt giảm giá <span class="text-red-500">*</span>
            </label>
            <InputText
              id="maDotGiamGia"
              v-model.trim="discount.maDotGiamGia"
              placeholder="Nhập mã đợt giảm giá"
              :invalid="submitted && !discount.maDotGiamGia"
            />
            <small v-if="submitted && !discount.maDotGiamGia" class="text-red-500">
              Mã đợt giảm giá là bắt buộc
            </small>
          </div>

          <!-- Tên đợt giảm giá -->
          <div class="flex flex-col gap-2">
            <label for="tenDotGiamGia" class="font-semibold">
              Tên đợt giảm giá <span class="text-red-500">*</span>
            </label>
            <InputText
              id="tenDotGiamGia"
              v-model.trim="discount.tenDotGiamGia"
              placeholder="Nhập tên đợt giảm giá"
              :invalid="submitted && !discount.tenDotGiamGia"
            />
            <small v-if="submitted && !discount.tenDotGiamGia" class="text-red-500">
              Tên đợt giảm giá là bắt buộc
            </small>
          </div>

          <!-- Phần trăm giảm -->
          <div class="flex flex-col gap-2">
            <label for="phanTramGiam" class="font-semibold">
              Phần trăm giảm <span class="text-red-500">*</span>
            </label>
            <InputNumber
              id="phanTramGiam"
              v-model="discount.phanTramGiam"
              mode="decimal"
              suffix="%"
              showButtons
              :min="0"
              :max="100"
              :minFractionDigits="0"
              :maxFractionDigits="2"
              :invalid="submitted && discount.phanTramGiam == null"
              placeholder="0.00"
            />
            <small v-if="submitted && discount.phanTramGiam == null" class="text-red-500">
              Phần trăm giảm là bắt buộc
            </small>
          </div>

          <!-- Trạng thái (Tự động tính toán) -->
          <div class="flex flex-col gap-2">
            <div class="flex items-center justify-between">
              <label class="font-semibold">
                Trạng thái (Tự động)
              </label>
            </div>
            <div class="p-3 bg-surface-50 border border-surface-200 rounded-lg">
              <div class="flex items-center gap-2">
                <Tag
                  :value="discountStore.getStatusLabel(discount.trangThai || 'CHUA_DIEN_RA')"
                  :severity="discountStore.getStatusSeverity(discount.trangThai || 'CHUA_DIEN_RA')"
                />
                <span class="text-sm text-surface-600">
                  Trạng thái được tính tự động dựa trên thời gian (GMT+7)
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Date Section -->
      <div class="card">
        <div class="flex items-center gap-2 mb-6">
          <i class="pi pi-calendar text-primary"></i>
          <h2 class="text-lg font-semibold text-surface-900 m-0">Thời gian áp dụng</h2>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- Ngày bắt đầu -->
          <div class="flex flex-col gap-2">
            <label for="ngayBatDau" class="font-semibold">
              Ngày bắt đầu <span class="text-red-500">*</span>
            </label>
            <DatePicker
              id="ngayBatDau"
              v-model="discount.ngayBatDau"
              showTime
              hourFormat="24"
              dateFormat="dd/mm/yy"
              placeholder="Chọn ngày và giờ bắt đầu"
              showButtonBar
              showIcon
              iconDisplay="input"
              :invalid="submitted && !discount.ngayBatDau"
            />
            <small v-if="submitted && !discount.ngayBatDau" class="text-red-500">
              Ngày bắt đầu là bắt buộc
            </small>
          </div>

          <!-- Ngày kết thúc -->
          <div class="flex flex-col gap-2">
            <label for="ngayKetThuc" class="font-semibold">
              Ngày kết thúc <span class="text-red-500">*</span>
            </label>
            <DatePicker
              id="ngayKetThuc"
              v-model="discount.ngayKetThuc"
              showTime
              hourFormat="24"
              dateFormat="dd/mm/yy"
              placeholder="Chọn ngày và giờ kết thúc"
              showButtonBar
              showIcon
              iconDisplay="input"
              :minDate="discount.ngayBatDau instanceof Date ? discount.ngayBatDau : (discount.ngayBatDau ? new Date(discount.ngayBatDau) : null)"
              :invalid="submitted && !discount.ngayKetThuc"
            />
            <small v-if="submitted && !discount.ngayKetThuc" class="text-red-500">
              Ngày kết thúc là bắt buộc
            </small>
          </div>
        </div>
      </div>

      <!-- Audit Reason Section (Edit Mode Only) -->
      <div v-if="discount.id" class="card">
        <div class="flex flex-col gap-2">
          <label for="auditReason" class="font-semibold">
            Lý do thay đổi <span class="text-red-500">*</span>
          </label>
          <Textarea
            id="auditReason"
            v-model="auditReason"
            placeholder="Nhập lý do thay đổi thông tin đợt giảm giá..."
            rows="3"
            :invalid="submitted && discount.id && !auditReason?.trim()"
          />
          <small v-if="submitted && discount.id && !auditReason?.trim()" class="text-red-500">
            Lý do thay đổi là bắt buộc khi cập nhật
          </small>
          <small class="text-surface-500">
            Lý do này sẽ được ghi lại trong lịch sử thay đổi để theo dõi audit trail
          </small>
        </div>
      </div>

      <!-- Product Management Section -->
      <div class="card">
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-2">
            <i class="pi pi-shopping-cart text-primary"></i>
            <h2 class="text-lg font-semibold text-surface-900 m-0">Sản phẩm áp dụng</h2>
          </div>
          <div v-if="selectedProductDetails.size > 0" class="flex items-center gap-2">
            <Badge
              :value="Array.from(selectedProductDetails.values()).flat().length"
              severity="success"
            />
            <span class="text-sm text-surface-600">sản phẩm đã chọn</span>
          </div>
        </div>
        <DataTable
          v-model:expanded-rows="expandedRows"
          :value="products"
          dataKey="id"
          scrollable
          scrollHeight="500px"
          showGridlines
          :rowHover="true"
          class="p-datatable-sm"
        >
          <template #header>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="font-medium">Danh sách sản phẩm</span>
                <Badge v-if="products.length > 0" :value="products.length" severity="info" />
              </div>
              <div class="flex items-center gap-2">
                <Button
                  icon="pi pi-plus"
                  label="Mở rộng tất cả"
                  outlined
                  size="small"
                  @click="expandAll"
                />
                <Button
                  icon="pi pi-minus"
                  label="Thu gọn tất cả"
                  outlined
                  size="small"
                  @click="collapseAll"
                />
              </div>
            </div>
          </template>

          <!-- Expander Column -->
          <Column expander style="width: 3rem" />

          <!-- Mã sản phẩm -->
          <Column field="maSanPham" header="Mã sản phẩm" sortable style="min-width: 8rem">
            <template #body="{ data }">
              <span class="font-medium text-primary">{{ data.maSanPham }}</span>
            </template>
          </Column>

          <!-- Danh mục -->
          <Column header="Danh mục" style="min-width: 10rem">
            <template #body="{ data }">
              <div class="flex flex-wrap gap-1">
                <Tag
                  v-for="danhMuc in data.danhMucs"
                  :key="danhMuc.id"
                  :value="danhMuc.moTaDanhMuc"
                  severity="secondary"
                />
                <span v-if="!data.danhMucs || data.danhMucs.length === 0" class="text-surface-400 text-sm">
                  Chưa phân loại
                </span>
              </div>
            </template>
          </Column>

          <!-- Tên sản phẩm -->
          <Column field="tenSanPham" header="Tên sản phẩm" sortable style="min-width: 12rem">
            <template #body="{ data }">
              <span class="font-medium">{{ data.tenSanPham }}</span>
            </template>
          </Column>

          <!-- Thương hiệu -->
          <Column field="thuongHieu.moTaThuongHieu" header="Thương hiệu" sortable style="min-width: 8rem">
            <template #body="{ data }">
              <Tag v-if="data.thuongHieu" :value="data.thuongHieu.moTaThuongHieu" severity="info" size="small" />
              <span v-else class="text-surface-400">N/A</span>
            </template>
          </Column>

          <!-- Số lượng chi tiết -->
          <Column header="Số lượng" style="width: 6rem">
            <template #body="{ data }">
              <Badge :value="data.sanPhamChiTiets?.length || 0" severity="secondary" />
            </template>
          </Column>
          <!-- Product Details Expansion -->
          <template #expansion="slotProps">
            <div class="bg-surface-50 p-4 border-round">
              <div class="flex items-center justify-between mb-4">
                <div class="flex items-center gap-2">
                  <i class="pi pi-list text-primary"></i>
                  <span class="font-semibold">Chi tiết sản phẩm: {{ slotProps.data.tenSanPham }}</span>
                  <Badge
                    v-if="slotProps.data.sanPhamChiTiets?.length"
                    :value="slotProps.data.sanPhamChiTiets.length"
                    severity="info"
                  />
                </div>
                <div v-if="selectedProductDetails.get(slotProps.data.id)?.length" class="flex items-center gap-2">
                  <Badge
                    :value="selectedProductDetails.get(slotProps.data.id).length"
                    severity="success"
                  />
                  <span class="text-sm text-surface-600">đã chọn</span>
                </div>
              </div>

              <DataTable
                :value="slotProps.data.sanPhamChiTiets || []"
                :selection="selectedProductDetails.get(slotProps.data.id) || []"
                @update:selection="(selection) => handleProductDetailSelection(slotProps.data.id, selection)"
                dataKey="id"
                selectionMode="multiple"
                class="p-datatable-sm"
                showGridlines
                :rowHover="true"
              >
                <!-- Selection Column -->
                <Column selectionMode="multiple" style="width: 3rem" :exportable="false"></Column>

                <!-- Serial Number -->
                <Column field="serialNumber" header="Serial Number" sortable style="min-width: 10rem">
                  <template #body="{ data: detailData }">
                    <span class="font-mono text-sm">{{ detailData.serialNumber }}</span>
                  </template>
                </Column>

                <!-- Giá bán -->
                <Column field="giaBan" header="Giá bán" sortable style="min-width: 8rem">
                  <template #body="{ data: detailData }">
                    <span class="font-semibold text-green-600">
                      {{ formatCurrency(detailData.giaBan) }}
                    </span>
                  </template>
                </Column>

                <!-- Màu sắc -->
                <Column field="mauSac.moTaMauSac" header="Màu sắc" sortable style="min-width: 6rem">
                  <template #body="{ data: detailData }">
                    <div v-if="detailData.mauSac" class="flex items-center gap-2">
                      <div
                        class="w-4 h-4 rounded-full border border-surface-300"
                        :style="{ backgroundColor: getColorValue(detailData.mauSac.moTaMauSac) }"
                      ></div>
                      <span class="text-sm">{{ detailData.mauSac.moTaMauSac }}</span>
                    </div>
                    <span v-else class="text-surface-400 text-sm">N/A</span>
                  </template>
                </Column>

                <!-- Trạng thái -->
                <Column field="trangThai" header="Trạng thái" sortable style="min-width: 8rem">
                  <template #body="{ data: detailData }">
                    <Tag
                      :value="getProductStatusLabel(detailData.trangThai)"
                      :severity="getProductStatusSeverity(detailData.trangThai)"
                      size="small"
                    />
                  </template>
                </Column>

                <!-- Thông số kỹ thuật -->
                <Column header="Thông số kỹ thuật" style="min-width: 12rem">
                  <template #body="{ data: detailData }">
                    <div class="space-y-1">
                      <div v-if="detailData.cpu" class="flex items-center gap-2 text-sm">
                        <i class="pi pi-microchip text-blue-600"></i>
                        <span class="font-medium">CPU:</span>
                        <span>{{ detailData.cpu.moTaCpu }}</span>
                      </div>
                      <div v-if="detailData.ram" class="flex items-center gap-2 text-sm">
                        <i class="pi pi-server text-green-600"></i>
                        <span class="font-medium">RAM:</span>
                        <span>{{ detailData.ram.moTaRam }}</span>
                      </div>
                      <div v-if="detailData.boNho || detailData.bonho || detailData.oCung || detailData.ocung" class="flex items-center gap-2 text-sm">
                        <i class="pi pi-database text-orange-600"></i>
                        <span class="font-medium">Ổ cứng:</span>
                        <span>{{ (detailData.boNho?.moTaBoNho || detailData.bonho?.moTaBoNho || detailData.oCung?.moTaOCung || detailData.ocung?.moTaOCung) }}</span>
                      </div>
                      <div v-if="detailData.gpu" class="flex items-center gap-2 text-sm">
                        <i class="pi pi-desktop text-purple-600"></i>
                        <span class="font-medium">GPU:</span>
                        <span>{{ detailData.gpu.moTaGpu }}</span>
                      </div>
                      <div v-if="detailData.manHinh" class="flex items-center gap-2 text-sm">
                        <i class="pi pi-tablet text-indigo-600"></i>
                        <span class="font-medium">Màn hình:</span>
                        <span>{{ detailData.manHinh.moTaManHinh }}</span>
                      </div>
                      <div v-if="!detailData.cpu && !detailData.ram && !detailData.boNho && !detailData.bonho && !detailData.oCung && !detailData.ocung && !detailData.gpu && !detailData.manHinh" class="text-surface-400 text-sm">
                        Chưa có thông số
                      </div>
                    </div>
                  </template>
                </Column>

                <template #empty>
                  <div class="text-center py-4 text-surface-500">
                    <i class="pi pi-info-circle text-2xl mb-2"></i>
                    <p>Không có chi tiết sản phẩm</p>
                  </div>
                </template>
              </DataTable>
            </div>
          </template>
          <template #empty>
            <div class="text-center py-8">
              <i class="pi pi-shopping-cart text-4xl text-surface-400 mb-4"></i>
              <p class="text-surface-500 text-lg mb-2">Không có sản phẩm nào</p>
              <p class="text-surface-400 text-sm">
                {{ products.length === 0 ? 'Chưa có sản phẩm nào trong hệ thống' : 'Đang tải dữ liệu sản phẩm...' }}
              </p>
            </div>
          </template>
        </DataTable>
      </div>

      <!-- Form Actions -->
      <div class="card">
        <div class="flex items-center justify-between">
          <div class="text-sm text-surface-500">
            <i class="pi pi-info-circle mr-2"></i>
            Vui lòng kiểm tra thông tin trước khi lưu
          </div>
          <div class="flex items-center gap-3">
            <Button
              label="Hủy"
              icon="pi pi-times"
              outlined
              @click="goBack"
              :disabled="isLoading"
            />
            <Button
              v-if="discount.id && discount.trangThai !== 'BI_HUY'"
              label="Xóa đợt giảm giá"
              icon="pi pi-trash"
              severity="danger"
              outlined
              @click="confirmDeleteDiscount"
              :disabled="isLoading"
            />
            <Button
              type="submit"
              :label="discount.id ? 'Cập nhật' : 'Tạo mới'"
              :icon="discount.id ? 'pi pi-save' : 'pi pi-plus'"
              :loading="isLoading"
            />
          </div>
        </div>
      </div>
    </form>

    <!-- Audit Log Section (Edit Mode Only) -->
    <div v-if="discount.id" class="card">
      <div class="flex items-center gap-2 mb-4">
        <i class="pi pi-history text-primary"></i>
        <span class="font-semibold text-xl">Lịch sử thay đổi</span>
        <div class="flex items-center gap-2 text-sm text-surface-500 ml-auto">
          <i class="pi pi-clock"></i>
          <span>{{ auditHistory.length }} mục</span>
        </div>
      </div>

      <div class="space-y-4 max-h-96 overflow-y-auto">
        <div
          v-for="(entry, index) in auditHistory"
          :key="entry.id || index"
          class="border-l-4 pl-4 py-3 rounded-r-lg"
          :class="getAuditBorderColor(entry.hanhDong)"
        >
          <!-- Header with action and timestamp -->
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-3">
              <i :class="[getAuditIcon(entry.hanhDong), getAuditIconColor(entry.hanhDong), 'text-lg']"></i>
              <span class="font-medium text-base">{{ getActionDisplayName(entry.hanhDong) }}</span>
              <span class="text-sm text-surface-500">{{ formatAuditDate(entry.thoiGianThayDoi) }}</span>
            </div>
          </div>

          <!-- User and Reason Information -->
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
            <div class="space-y-2">
              <div class="text-sm text-surface-700">
                <strong class="text-surface-900">Người thực hiện:</strong>
                <span class="font-medium ml-2">{{ entry.nguoiThucHien || 'Hệ thống' }}</span>
              </div>

              <div class="text-sm text-surface-700">
                <strong class="text-surface-900">Lý do:</strong>
                <span class="italic ml-2">{{ entry.lyDoThayDoi || 'Không có lý do được ghi nhận' }}</span>
                <span v-if="!entry.lyDoThayDoi" class="text-orange-600 text-xs ml-2">(Thiếu thông tin)</span>
              </div>
            </div>
          </div>

          <!-- Change Details Section -->
          <div v-if="entry.giaTriCu || entry.giaTriMoi" class="bg-surface-50 rounded-lg p-4">
            <strong class="text-surface-900 text-base block mb-3">Chi tiết thay đổi:</strong>

            <!-- Parse and display only changed fields (following CrudCoupons pattern) -->
            <div class="space-y-3">
              <div v-for="change in parseAuditChangesEnhanced(entry.giaTriCu, entry.giaTriMoi)" :key="change.field" class="border-b border-surface-200 pb-3 last:border-b-0 last:pb-0">
                <div class="font-medium text-surface-700 mb-2 text-sm flex items-center gap-2">
                  {{ change.fieldName }}
                  <span class="text-xs bg-orange-100 text-orange-700 px-2 py-1 rounded">Đã thay đổi</span>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <div class="text-red-600 bg-red-50 p-2 rounded text-sm">{{ change.oldValue }}</div>
                  </div>
                  <div>
                    <div class="text-green-600 bg-green-50 p-2 rounded text-sm">{{ change.newValue }}</div>
                  </div>
                </div>
              </div>

              <!-- Show message when no changes detected -->
              <div v-if="parseAuditChangesEnhanced(entry.giaTriCu, entry.giaTriMoi).length === 0" class="text-center text-surface-500 py-4">
                <i class="pi pi-info-circle mr-2"></i>
                Không có thay đổi nào được ghi nhận
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Loading state for audit history -->
      <div v-if="isLoadingAudit" class="text-center py-8">
        <ProgressSpinner />
        <p class="text-surface-500 mt-4">Đang tải lịch sử thay đổi...</p>
      </div>

      <!-- Empty state -->
      <div v-else-if="auditHistory.length === 0" class="text-center py-8 text-surface-500">
        <i class="pi pi-history text-2xl mb-2"></i>
        <p class="text-base">Chưa có lịch sử thay đổi</p>
        <p class="text-sm text-surface-400 mt-2">
          Lịch sử sẽ được ghi lại khi có thay đổi dữ liệu
        </p>
      </div>
    </div>
  </div>
</template>
