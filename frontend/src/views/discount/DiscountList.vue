<script setup>
import { ref, computed, onBeforeMount } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { FilterMatchMode, FilterOperator } from '@primevue/core/api'
import { useDiscountStore } from '@/stores/discountstore'
import discountService from '@/apis/discount'

// PrimeVue Components
import Toast from 'primevue/toast'
import Textarea from 'primevue/textarea'

// --- 1. Store Access ---
const discountStore = useDiscountStore()
// --- Router ---
const router = useRouter()

// --- 2. State ---

// Data from Stores
const discounts = computed(() => discountStore.discounts)

// Component State - Main Data & Selection
const discount = ref({}) // Holds the discount being edited or created
const selectedDiscounts = ref([]) // For multi-select in the main table

// Component State - UI Control (Dialogs, Table, Form)
const deleteDiscountDialog = ref(false)
const deleteDiscountsDialog = ref(false)
const restoreDiscountsDialog = ref(false)

// Audit reason state
const deleteReason = ref('')
const batchCloseReason = ref('')

// View mode state - show cancelled discounts
const showCancelledDiscounts = ref(false)

// Component State - PrimeVue Utilities
const toast = useToast()

// Performance optimization state
const isLoading = ref(false)

// --- 3. Filters ---
// Define the initial structure for filters
const initialFilters = {
  global: { value: null, matchMode: FilterMatchMode.CONTAINS },
  maDotGiamGia: {
    operator: FilterOperator.AND,
    constraints: [{ value: null, matchMode: FilterMatchMode.CONTAINS }],
  },
  tenDotGiamGia: {
    operator: FilterOperator.AND,
    constraints: [{ value: null, matchMode: FilterMatchMode.CONTAINS }],
  },
  phanTramGiam: { value: [0, 100], matchMode: FilterMatchMode.BETWEEN },
  ngayBatDau: {
    operator: FilterOperator.AND,
    constraints: [{ value: null, matchMode: FilterMatchMode.CUSTOM }],
  },
  ngayKetThuc: {
    operator: FilterOperator.AND,
    constraints: [{ value: null, matchMode: FilterMatchMode.CUSTOM }],
  },
  trangThai: { value: null, matchMode: FilterMatchMode.EQUALS },
}

const filters = ref(JSON.parse(JSON.stringify(initialFilters)))

const normalizeDateToStartOfDay = (dateInput) => {
  if (!dateInput) return null
  try {
    const date = dateInput instanceof Date ? dateInput : new Date(dateInput)
    if (isNaN(date.getTime())) return null
    const normalized = new Date(date.getFullYear(), date.getMonth(), date.getDate())
    return normalized
  } catch (e) {
    console.error('Error normalizing date:', dateInput, e)
    return null
  }
}

// Enhanced filter tracking following ProductList.vue patterns
const hasActiveFilters = computed(() => {
  return !!(
    filters.value.global.value ||
    filters.value.maDotGiamGia.constraints[0].value ||
    filters.value.tenDotGiamGia.constraints[0].value ||
    filters.value.trangThai.value ||
    filters.value.ngayBatDau.constraints[0].value ||
    filters.value.ngayKetThuc.constraints[0].value ||
    (filters.value.phanTramGiam.value &&
     (filters.value.phanTramGiam.value[0] !== 0 || filters.value.phanTramGiam.value[1] !== 100))
  )
})

const activeFilterCount = computed(() => {
  let count = 0
  if (filters.value.global.value) count++
  if (filters.value.maDotGiamGia.constraints[0].value) count++
  if (filters.value.tenDotGiamGia.constraints[0].value) count++
  if (filters.value.trangThai.value) count++
  if (filters.value.ngayBatDau.constraints[0].value) count++
  if (filters.value.ngayKetThuc.constraints[0].value) count++
  if (filters.value.phanTramGiam.value &&
      (filters.value.phanTramGiam.value[0] !== 0 || filters.value.phanTramGiam.value[1] !== 100)) count++
  return count
})

const filteredDiscounts = computed(() => {
  let data = [...discounts.value]
  const globalFilter = filters.value.global.value?.toLowerCase()
  const maFilter = filters.value.maDotGiamGia.constraints[0].value?.toLowerCase()
  const tenFilter = filters.value.tenDotGiamGia.constraints[0].value?.toLowerCase()
  const phanTramFilter = filters.value.phanTramGiam.value
  const trangThaiFilter = filters.value.trangThai.value
  const ngayBatDauFilter = normalizeDateToStartOfDay(filters.value.ngayBatDau.constraints[0].value)
  const ngayKetThucFilter = normalizeDateToStartOfDay(
    filters.value.ngayKetThuc.constraints[0].value,
  )

  if (globalFilter) {
    data = data.filter((item) =>
      Object.values(item).some((val) => String(val).toLowerCase().includes(globalFilter)),
    )
  }
  if (maFilter) {
    data = data.filter((item) => item.maDotGiamGia?.toLowerCase().includes(maFilter))
  }
  if (tenFilter) {
    data = data.filter((item) => item.tenDotGiamGia?.toLowerCase().includes(tenFilter))
  }
  if (phanTramFilter && phanTramFilter.length === 2) {
    data = data.filter(
      (item) => item.phanTramGiam >= phanTramFilter[0] && item.phanTramGiam <= phanTramFilter[1],
    )
  }
  if (trangThaiFilter) {
    data = data.filter((item) => item.trangThai === trangThaiFilter)
  }
  if (ngayBatDauFilter) {
    data = data.filter((item) => {
      const itemDate = normalizeDateToStartOfDay(item.ngayBatDau)
      return itemDate && itemDate.getTime() === ngayBatDauFilter.getTime()
    })
  }
  if (ngayKetThucFilter) {
    data = data.filter((item) => {
      const itemDate = normalizeDateToStartOfDay(item.ngayKetThuc)
      return itemDate && itemDate.getTime() === ngayKetThucFilter.getTime()
    })
  }
  return data
})

// Function to reset filters to their initial state
function clearFilter() {
  filters.value = JSON.parse(JSON.stringify(initialFilters))
}

function clearSpecificFilter(fieldName) {
  console.log(`Clearing filter for: ${fieldName}`)
  if (fieldName === 'global') {
    filters.value.global.value = null
  } else if (fieldName === 'phanTramGiam') {
    filters.value.phanTramGiam.value = [0, 100]
  } else if (filters.value[fieldName]?.constraints) {
    filters.value[fieldName].constraints[0].value = null
  } else if (fieldName === 'trangThai') {
    filters.value.trangThai.value = null
  } else {
    console.warn(`Unknown filter field name to clear: ${fieldName}`)
  }
}



// Enhanced data fetching with loading states
async function fetchDiscountsWithLoading() {
  isLoading.value = true
  try {
    if (showCancelledDiscounts.value) {
      // Fetch all discounts including cancelled ones
      const response = await discountService.getAllDiscountsIncludingHidden()
      discountStore.discounts = response || []
    } else {
      // Fetch only active discounts (normal behavior)
      await discountStore.fetchDiscounts()
    }
  } catch (error) {
    console.error('Error fetching discounts:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải dữ liệu đợt giảm giá',
      life: 3000,
    })
  } finally {
    isLoading.value = false
  }
}

// Toggle between showing all discounts and only active ones
async function toggleShowCancelled() {
  showCancelledDiscounts.value = !showCancelledDiscounts.value
  await fetchDiscountsWithLoading()

  toast.add({
    severity: 'info',
    summary: 'Thông báo',
    detail: showCancelledDiscounts.value
      ? 'Hiển thị tất cả đợt giảm giá (bao gồm đã hủy)'
      : 'Chỉ hiển thị đợt giảm giá đang hoạt động',
    life: 3000,
  })
}

// --- 6. Lifecycle Hooks ---
onBeforeMount(async () => {
  // Fetch initial data when the component is about to mount
  await fetchDiscountsWithLoading()
})

// --- 6. Utility Functions ---

/**
 * Formats an ISO date string into Vietnam timezone date and time string.
 * Following PhieuGiamGia pattern exactly for consistent timezone display
 * @param {string} dateString - The ISO date string (UTC).
 * @returns {string} Formatted date-time string in Vietnam timezone or empty string if input is invalid.
 */
const formatDateTime = (dateString) => {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) return '' // Check for invalid date

    // Format in Vietnam timezone (Asia/Ho_Chi_Minh)
    const options = {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'Asia/Ho_Chi_Minh',
      hour12: false // Use 24-hour format for business operations
    }
    return new Intl.DateTimeFormat('vi-VN', options).format(date)
  } catch (error) {
    console.error('Error formatting date-time:', error)
    return '' // Return empty on error
  }
}



/**
 * Gets the severity level for a discount status tag based on its state.
 * @param {string} trangThai - The status ('CHUA_DIEN_RA', 'DA_DIEN_RA', 'KET_THUC', 'BI_HUY').
 * @returns {string|null} PrimeVue severity ('warn', 'success', 'danger', 'secondary') or null.
 */
function getSeverity(trangThai) {
  const severityMap = {
    CHUA_DIEN_RA: 'warn',
    DA_DIEN_RA: 'success',
    KET_THUC: 'danger',
    BI_HUY: 'secondary',
  }
  return severityMap[trangThai] || null
}



// --- 6. Status Options (Following PhieuGiamGia pattern) ---
const statusOptions = ref([
  { label: 'Chưa diễn ra', value: 'CHUA_DIEN_RA' },
  { label: 'Đang diễn ra', value: 'DA_DIEN_RA' },
  { label: 'Kết thúc', value: 'KET_THUC' },
  { label: 'Đã hủy', value: 'BI_HUY' },
])

// --- 7. Data Refresh Method ---
async function refreshData() {
  await fetchDiscountsWithLoading()
  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Dữ liệu đã được làm mới',
    life: 3000,
  })
}

// --- 8. Dialog Control Methods ---

/** Navigates to the form for creating a new discount. */
function newDiscount() {
  router.push({ name: 'DiscountAdd' }) // Adjust 'DiscountCreate' to your route's name
}

/**
 * Navigates to the form for editing an existing discount.
 * @param {object} discountData - The discount data object from the table row.
 */
function editDiscount(discountData) {
  router.push({ name: 'DiscountEdit', params: { id: discountData.id } }) // Adjust 'DiscountEdit'
}

/**
 * Opens the confirmation dialog for deleting a single discount.
 * @param {object} discountData - The discount data object.
 */
function confirmDeleteDiscount(discountData) {
  discount.value = discountData // Store the discount to be deleted
  deleteReason.value = '' // Clear previous reason
  deleteDiscountDialog.value = true
}

/** Opens the confirmation dialog for deleting multiple selected discounts. */
function confirmDeleteDiscounts() {
  if (!selectedDiscounts.value || selectedDiscounts.value.length === 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Chưa chọn đợt giảm giá nào để xóa',
      life: 3000,
    })
    return
  }
  batchCloseReason.value = '' // Clear previous reason
  deleteDiscountsDialog.value = true
}



/** Opens the confirmation dialog for restoring multiple selected hidden discounts. */
function confirmRestoreDiscounts() {
  if (!selectedDiscounts.value || selectedDiscounts.value.length === 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Chưa chọn đợt giảm giá nào để khôi phục',
      life: 3000,
    })
    return
  }

  // Check if any selected discounts are cancelled (BI_HUY status)
  const cancelledDiscounts = selectedDiscounts.value.filter(discount => discount.trangThai === 'BI_HUY')

  if (cancelledDiscounts.length === 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Không có đợt giảm giá nào bị hủy trong danh sách đã chọn',
      life: 3000,
    })
    return
  }

  restoreDiscountsDialog.value = true
}

// --- 8. CRUD Action Methods ---

/** Handles the actual deletion of a single discount after confirmation. */
async function deleteSingleDiscount() {
  if (!discount.value || !discount.value.id) return

  // Validate delete reason
  if (!deleteReason.value?.trim()) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng nhập lý do xóa',
      life: 3000,
    })
    return
  }

  isLoading.value = true
  try {
    // Use audit-aware delete method with fallback
    try {
      await discountService.deleteDiscountWithAudit(discount.value.id, deleteReason.value.trim())
    } catch (error) {
      console.warn('Audit delete method failed, using standard method:', error)
      await discountService.deleteDiscount(discount.value.id)
    }

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Xóa đợt giảm giá thành công',
      life: 3000,
    })
    await fetchDiscountsWithLoading() // Refresh the list with loading state
    deleteDiscountDialog.value = false
    discount.value = {} // Clear the reference
    deleteReason.value = '' // Clear the reason
  } catch (error) {
    console.error('Error deleting discount:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Có lỗi xảy ra khi xóa: ${error.message || 'Lỗi không xác định'}`,
      life: 3000,
    })
  } finally {
    isLoading.value = false
  }
}

/** Handles the actual deletion of multiple selected discounts after confirmation. */
async function deleteMultipleDiscounts() {
  if (!selectedDiscounts.value || selectedDiscounts.value.length === 0) return

  // Validate batch delete reason
  if (!batchCloseReason.value?.trim()) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng nhập lý do xóa',
      life: 3000,
    })
    return
  }

  const selectedIds = selectedDiscounts.value.map((item) => item.id)
  isLoading.value = true
  try {
    // Use audit-aware batch delete method with fallback
    try {
      await discountService.deleteDiscountsWithAudit(selectedIds, batchCloseReason.value.trim())
    } catch (error) {
      console.warn('Audit batch delete method failed, using standard method:', error)
      await discountService.deleteDiscounts(selectedIds)
    }

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã xóa ${selectedIds.length} đợt giảm giá`,
      life: 3000,
    })
    await fetchDiscountsWithLoading() // Refresh the list with loading state
    deleteDiscountsDialog.value = false
    selectedDiscounts.value = [] // Clear selection
    batchCloseReason.value = '' // Clear the reason
  } catch (error) {
    console.error('Error deleting multiple discounts:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Có lỗi xảy ra khi xóa: ${error.message || 'Lỗi không xác định'}`,
      life: 3000,
    })
  } finally {
    isLoading.value = false
  }
}



/** Handles the actual restoration of multiple selected hidden discounts after confirmation. */
async function restoreMultipleDiscounts() {
  if (!selectedDiscounts.value || selectedDiscounts.value.length === 0) return

  // Filter only cancelled discounts
  const cancelledDiscounts = selectedDiscounts.value.filter(discount => discount.trangThai === 'BI_HUY')

  if (cancelledDiscounts.length === 0) return

  const selectedIds = cancelledDiscounts.map((item) => item.id)
  isLoading.value = true
  try {
    await discountService.restoreMultipleDiscounts(selectedIds)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã khôi phục ${selectedIds.length} đợt giảm giá`,
      life: 3000,
    })
    await fetchDiscountsWithLoading() // Refresh the list with loading state
    restoreDiscountsDialog.value = false
    selectedDiscounts.value = [] // Clear selection
  } catch (error) {
    console.error('Error restoring multiple discounts:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Có lỗi xảy ra khi khôi phục: ${error.message || 'Lỗi không xác định'}`,
      life: 3000,
    })
  } finally {
    isLoading.value = false
  }
}

/** Handles the restoration of a single cancelled discount. */
async function restoreDiscount(discountData) {
  if (!discountData || !discountData.id) return

  isLoading.value = true
  try {
    await discountService.restoreDiscount(discountData.id)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã khôi phục đợt giảm giá "${discountData.tenDotGiamGia}"`,
      life: 3000,
    })
    await fetchDiscountsWithLoading() // Refresh the list with loading state
  } catch (error) {
    console.error('Error restoring single discount:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Có lỗi xảy ra khi khôi phục: ${error.message || 'Lỗi không xác định'}`,
      life: 3000,
    })
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
    <Toast />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-percentage text-lg text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">
              Quản lý đợt giảm giá
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              Tạo và quản lý các đợt giảm giá cho sản phẩm
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <Button
            label="Làm mới"
            icon="pi pi-refresh"
            outlined
            @click="refreshData"
            :loading="isLoading"
          />
          <Button
            :label="showCancelledDiscounts ? 'Chỉ hiện hoạt động' : 'Hiện tất cả'"
            :icon="showCancelledDiscounts ? 'pi pi-eye-slash' : 'pi pi-eye'"
            outlined
            @click="toggleShowCancelled"
            :severity="showCancelledDiscounts ? 'warn' : 'info'"
          />
          <Button
            label="Thêm đợt giảm giá"
            icon="pi pi-plus"
            severity="success"
            @click="newDiscount"
          />
          <Button
            v-if="!showCancelledDiscounts"
            label="Xóa nhiều đợt"
            icon="pi pi-trash"
            severity="danger"
            outlined
            @click="confirmDeleteDiscounts"
            :disabled="!selectedDiscounts || !selectedDiscounts.length"
          />
          <Button
            v-if="showCancelledDiscounts"
            label="Khôi phục nhiều đợt"
            icon="pi pi-refresh"
            severity="success"
            outlined
            @click="confirmRestoreDiscounts"
            :disabled="!selectedDiscounts || !selectedDiscounts.length"
          />
        </div>
      </div>
    </div>

  <div class="card">

    <div class="font-semibold text-xl mb-4">Bộ lọc</div>

    <!-- Enhanced Filter Section Following ProductList.vue Patterns -->
    <div class="mb-6 border rounded-lg p-4">
      <!-- Filter Actions Row -->
      <div class="flex justify-between items-center mb-4">
        <div class="flex items-center gap-2">
          <i class="pi pi-filter text-primary"></i>
          <span class="text-sm text-surface-600">Sử dụng các bộ lọc dưới đây để tìm kiếm đợt giảm giá</span>
          <Badge v-if="hasActiveFilters" :value="activeFilterCount" severity="info" />
        </div>
        <Button
          type="button"
          icon="pi pi-filter-slash"
          label="Xóa toàn bộ bộ lọc"
          outlined
          size="small"
          @click="clearFilter()"
          :disabled="!hasActiveFilters"
        />
      </div>

      <!-- Filters Grid - Enhanced Responsive Layout -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <!-- Mã đợt giảm giá -->
        <div>
          <label class="block mb-2">Mã đợt giảm giá</label>
          <InputGroup>
            <Button
              v-if="filters['maDotGiamGia'].constraints[0].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('maDotGiamGia')"
            />
            <InputText
              v-model="filters['maDotGiamGia'].constraints[0].value"
              type="text"
              placeholder="Lọc mã"
              fluid
            />
          </InputGroup>
        </div>

        <!-- Tên đợt giảm giá -->
        <div>
          <label class="block mb-2">Tên đợt giảm giá</label>
          <InputGroup>
            <Button
              v-if="filters['tenDotGiamGia'].constraints[0].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('tenDotGiamGia')"
            />
            <InputText
              v-model="filters['tenDotGiamGia'].constraints[0].value"
              type="text"
              placeholder="Lọc tên"
              fluid
            />
          </InputGroup>
        </div>

        <!-- Trạng thái -->
        <div>
          <label class="block mb-2">Trạng thái</label>
          <InputGroup>
            <Button
              v-if="filters['trangThai'].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('trangThai')"
            />
            <Select
              v-model="filters['trangThai'].value"
              :options="statusOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Chọn trạng thái"
              fluid
            >
              <template #option="{ option }">
                <Tag :value="option.label" :severity="getSeverity(option.value)" />
              </template>
            </Select>
          </InputGroup>
        </div>

        <!-- Phần trăm giảm - Range Slider -->
        <div>
          <label class="block mb-4">Phần trăm giảm (%)</label>
          <div class="px-3">
            <Slider
              v-model="filters['phanTramGiam'].value"
              range
              class="mb-2"
              :max="100"
              fluid
            />
            <div class="flex items-center justify-between px-2">
              <span>{{ filters['phanTramGiam'].value ? filters['phanTramGiam'].value[0] : 0 }}%</span>
              <span>{{ filters['phanTramGiam'].value ? filters['phanTramGiam'].value[1] : 100 }}%</span>
            </div>
          </div>
        </div>

        <!-- Ngày bắt đầu -->
        <div>
          <label class="block mb-2">Ngày bắt đầu</label>
          <InputGroup>
            <Button
              v-if="filters['ngayBatDau'].constraints[0].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('ngayBatDau')"
            />
            <DatePicker
              v-model="filters['ngayBatDau'].constraints[0].value"
              dateFormat="dd/mm/yy"
              placeholder="dd/mm/yyyy"
              showButtonBar
              showIcon
              fluid
              iconDisplay="input"
            />
          </InputGroup>
        </div>

        <!-- Ngày kết thúc -->
        <div>
          <label class="block mb-2">Ngày kết thúc</label>
          <InputGroup>
            <Button
              v-if="filters['ngayKetThuc'].constraints[0].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('ngayKetThuc')"
            />
            <DatePicker
              v-model="filters['ngayKetThuc'].constraints[0].value"
              dateFormat="dd/mm/yy"
              placeholder="dd/mm/yyyy"
              showButtonBar
              showIcon
              fluid
              iconDisplay="input"
              :minDate="filters['ngayBatDau'].constraints[0].value"
            />
          </InputGroup>
        </div>
      </div>
    </div>

    <!-- Enhanced Discount DataTable with Performance Optimization -->
    <DataTable
      v-model:selection="selectedDiscounts"
      :value="filteredDiscounts"
      :loading="isLoading || discountStore.loading"
      paginator
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20, 50]"
      showGridlines
      dataKey="id"
      filterDisplay="menu"
      class="p-datatable-sm"
      currentPageReportTemplate="Hiển thị {first} đến {last} trong tổng số {totalRecords} đợt giảm giá"
      :globalFilterFields="['maDotGiamGia', 'tenDotGiamGia']"
      scrollable
      scrollHeight="600px"
    >
      <template #header>
        <div class="flex justify-between items-center">
          <div class="flex">
            <IconField>
              <InputIcon>
                <i class="pi pi-search" />
              </InputIcon>
              <InputText v-model="filters['global'].value" placeholder="Tìm kiếm..." />
            </IconField>
          </div>
        </div>
      </template>

      <!-- Empty State -->
      <template #empty>
        <div class="py-8 text-center">
          <i class="pi pi-search text-2xl mb-2" />
          <p>Không tìm thấy đợt giảm giá</p>
        </div>
      </template>

      <!-- Loading State -->
      <template #loading>
        <div class="py-8 text-center">
          <i class="pi pi-spinner pi-spin text-2xl mb-2" />
          <p>Đang tải dữ liệu...</p>
        </div>
      </template>

      <!-- Table Columns -->

      <!-- Selection Column with Checkboxes -->
      <Column selectionMode="multiple" headerStyle="width: 3rem" />

      <!-- STT Column -->
      <Column header="STT" style="width: 4rem">
        <template #body="{ index }">
          <span class="font-medium">{{ index + 1 }}</span>
        </template>
      </Column>

      <Column
        field="maDotGiamGia"
        header="Mã đợt"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      />

      <Column
        field="tenDotGiamGia"
        header="Tên đợt giảm giá"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      />

      <Column
        field="phanTramGiam"
        header="Phần trăm giảm"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          <Tag
            :value="`${data.phanTramGiam}%`"
            severity="success"
          />
        </template>
      </Column>

      <Column
        field="ngayBatDau"
        header="Bắt đầu"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDateTime(data.ngayBatDau) }}
        </template>
      </Column>

      <Column
        field="ngayKetThuc"
        header="Kết thúc"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDateTime(data.ngayKetThuc) }}
        </template>
      </Column>

      <Column
        field="trangThai"
        header="Trạng thái"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <Tag
              :value="discountStore.getStatusLabel(data.trangThai)"
              :severity="discountStore.getStatusSeverity(data.trangThai)"
            />
          </div>
        </template>
      </Column>

      <Column header="Hành động" headerClass="!text-md" class="!text-sm" style="width: 150px">
        <template #body="{ data }">
          <div class="flex gap-1">
            <Button
              v-if="data.trangThai !== 'BI_HUY'"
              icon="pi pi-pencil"
              text
              rounded
              size="small"
              @click="editDiscount(data)"
              class="!w-8 !h-8 !text-blue-500 hover:!bg-blue-50"
              v-tooltip.top="'Chỉnh sửa'"
            />
            <Button
              v-if="data.trangThai !== 'BI_HUY'"
              icon="pi pi-trash"
              text
              rounded
              size="small"
              severity="danger"
              @click="confirmDeleteDiscount(data)"
              class="!w-8 !h-8 !text-red-500 hover:!bg-red-50"
              v-tooltip.top="'Xóa đợt'"
            />
            <Button
              v-if="data.trangThai === 'BI_HUY'"
              icon="pi pi-refresh"
              text
              rounded
              size="small"
              severity="success"
              @click="restoreDiscount(data)"
              class="!w-8 !h-8 !text-green-500 hover:!bg-green-50"
              v-tooltip.top="'Khôi phục'"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Confirmation Dialogs -->

    <!-- Delete Single Discount Dialog -->
    <Dialog
      v-model:visible="deleteDiscountDialog"
      :style="{ width: '500px' }"
      header="Xác nhận xóa đợt giảm giá"
      :modal="true"
      class="p-fluid"
    >
      <div class="flex items-start gap-4 mb-4">
        <div class="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <i class="pi pi-exclamation-triangle text-red-600 text-xl"></i>
        </div>
        <div class="flex-1">
          <h4 class="text-lg font-semibold text-surface-900 mb-2">Xóa đợt giảm giá</h4>
          <p class="text-surface-600 mb-3">
            Bạn có chắc chắn muốn xóa đợt giảm giá
            <span class="font-semibold text-surface-900">{{ discount?.tenDotGiamGia }}</span>
            ({{ discount?.maDotGiamGia }})?
          </p>
          <div class="bg-red-50 border border-red-200 rounded-lg p-3">
            <p class="text-red-700 text-sm mb-0">
              <i class="pi pi-info-circle mr-2"></i>
              Hành động này không thể hoàn tác. Đợt giảm giá sẽ được chuyển sang trạng thái "Đã hủy".
            </p>
          </div>
        </div>
      </div>

      <!-- Audit Reason Input -->
      <div class="mt-4">
        <label for="deleteReason" class="block text-sm font-medium mb-2">
          Lý do xóa <span class="text-red-500">*</span>
        </label>
        <Textarea
          id="deleteReason"
          v-model="deleteReason"
          placeholder="Nhập lý do xóa đợt giảm giá..."
          rows="3"
          class="w-full"
        />
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            outlined
            @click="deleteDiscountDialog = false"
          />
          <Button
            label="Xóa đợt giảm giá"
            icon="pi pi-trash"
            severity="danger"
            @click="deleteSingleDiscount"
          />
        </div>
      </template>
    </Dialog>

    <!-- Delete Multiple Discounts Dialog -->
    <Dialog
      v-model:visible="deleteDiscountsDialog"
      :style="{ width: '500px' }"
      header="Xác nhận xóa nhiều đợt giảm giá"
      :modal="true"
      class="p-fluid"
    >
      <div class="flex items-start gap-4 mb-4">
        <div class="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <i class="pi pi-exclamation-triangle text-red-600 text-xl"></i>
        </div>
        <div class="flex-1">
          <h4 class="text-lg font-semibold text-surface-900 mb-2">Xóa nhiều đợt giảm giá</h4>
          <p class="text-surface-600 mb-3">
            Bạn có chắc chắn muốn xóa
            <span class="font-semibold text-surface-900">{{ selectedDiscounts?.length || 0 }}</span>
            đợt giảm giá đã chọn?
          </p>
          <div class="bg-red-50 border border-red-200 rounded-lg p-3">
            <p class="text-red-700 text-sm mb-0">
              <i class="pi pi-info-circle mr-2"></i>
              Hành động này không thể hoàn tác. Tất cả đợt giảm giá được chọn sẽ chuyển sang trạng thái "Đã hủy".
            </p>
          </div>
        </div>
      </div>

      <!-- Batch Delete Audit Reason Input -->
      <div class="mt-4">
        <label for="batchDeleteReason" class="block text-sm font-medium mb-2">
          Lý do xóa <span class="text-red-500">*</span>
        </label>
        <Textarea
          id="batchDeleteReason"
          v-model="batchCloseReason"
          placeholder="Nhập lý do xóa nhiều đợt giảm giá..."
          rows="3"
          class="w-full"
        />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            outlined
            @click="deleteDiscountsDialog = false"
          />
          <Button
            label="Xóa tất cả"
            icon="pi pi-trash"
            severity="danger"
            @click="deleteMultipleDiscounts"
          />
        </div>
      </template>
    </Dialog>



    <!-- Restore Multiple Discounts Dialog -->
    <Dialog
      v-model:visible="restoreDiscountsDialog"
      :style="{ width: '500px' }"
      header="Xác nhận khôi phục đợt giảm giá"
      :modal="true"
      class="p-fluid"
    >
      <div class="flex items-start gap-4 mb-4">
        <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <i class="pi pi-refresh text-green-600 text-xl"></i>
        </div>
        <div class="flex-1">
          <h4 class="text-lg font-semibold text-surface-900 mb-2">Khôi phục đợt giảm giá</h4>
          <p class="text-surface-600 mb-3">
            Bạn có chắc chắn muốn khôi phục
            <span class="font-semibold text-surface-900">{{ selectedDiscounts?.length || 0 }}</span>
            đợt giảm giá đã chọn?
          </p>
          <div class="bg-green-50 border border-green-200 rounded-lg p-3">
            <p class="text-green-700 text-sm mb-0">
              <i class="pi pi-info-circle mr-2"></i>
              Các đợt giảm giá sẽ được hiển thị trở lại trong danh sách và có thể được sử dụng.
            </p>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            outlined
            @click="restoreDiscountsDialog = false"
          />
          <Button
            label="Khôi phục đợt giảm giá"
            icon="pi pi-refresh"
            severity="success"
            @click="restoreMultipleDiscounts"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
