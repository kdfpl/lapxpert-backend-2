<template>
  <Fluid />

  <!-- Page Header -->
  <div class="card mb-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
          <i class="pi pi-box text-3xl text-primary"></i>
        </div>
        <div>
          <h1 class="font-semibold text-xl text-surface-900 m-0">Quản lý sản phẩm</h1>
          <p class="text-surface-500 text-sm mt-1 mb-0">
            Quản lý danh sách sản phẩm với hệ thống 8-core attributes và SKU-based variants
          </p>
        </div>
      </div>
      <div class="flex gap-2">
        <Button label="Thêm sản phẩm" icon="pi pi-plus" @click="navigateToAdd" />
        <Button
          label="Làm mới"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="refreshData"
        />
      </div>
    </div>
  </div>

  <!-- Filter Section -->
  <div class="card mb-6">
    <div class="font-semibold text-xl mb-4">Bộ lọc</div>

    <Button
      type="button"
      icon="pi pi-filter-slash"
      label="Xóa toàn bộ bộ lọc"
      outlined
      class="mb-4"
      @click="clearFilters()"
    />

    <div class="mb-6 grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 border p-4 rounded-lg">
      <!-- Product Name Filter -->
      <div>
        <label class="block mb-2 font-medium">Tên sản phẩm</label>
        <InputGroup>
          <InputText
            v-model="filters.tenSanPham"
            placeholder="Nhập tên sản phẩm"
            @input="debouncedSearch"
            fluid
          />
          <Button
            v-if="filters.tenSanPham"
            icon="pi pi-times"
            outlined
            @click="filters.tenSanPham = ''"
          />
        </InputGroup>
      </div>

      <!-- Product Code Filter -->
      <div>
        <label class="block mb-2 font-medium">Mã sản phẩm</label>
        <InputGroup>
          <InputText
            v-model="filters.maSanPham"
            placeholder="Nhập mã sản phẩm"
            @input="debouncedSearch"
            fluid
          />
          <Button
            v-if="filters.maSanPham"
            icon="pi pi-times"
            outlined
            @click="filters.maSanPham = ''"
          />
        </InputGroup>
      </div>

      <!-- Category Filter -->
      <div>
        <label class="block mb-2 font-medium">Danh mục</label>
        <InputGroup>
          <Select
            v-model="filters.danhMuc"
            :options="categories"
            optionLabel="moTaDanhMuc"
            optionValue="id"
            placeholder="Chọn danh mục"
            fluid
          />
          <Button
            v-if="filters.danhMuc"
            icon="pi pi-times"
            outlined
            @click="filters.danhMuc = null"
          />
        </InputGroup>
      </div>

      <!-- Brand Filter -->
      <div>
        <label class="block mb-2 font-medium">Thương hiệu</label>
        <InputGroup>
          <Select
            v-model="filters.thuongHieu"
            :options="brands"
            optionLabel="moTaThuongHieu"
            optionValue="id"
            placeholder="Chọn thương hiệu"
            fluid
          />
          <Button
            v-if="filters.thuongHieu"
            icon="pi pi-times"
            outlined
            @click="filters.thuongHieu = null"
          />
        </InputGroup>
      </div>



      <!-- Status Filter -->
      <div>
        <label class="block mb-2 font-medium">Trạng thái</label>
        <InputGroup>
          <Select
            v-model="filters.trangThai"
            :options="[
              { label: 'Hoạt động', value: true },
              { label: 'Ngừng hoạt động', value: false },
            ]"
            optionLabel="label"
            optionValue="value"
            placeholder="Chọn trạng thái"
            fluid
          />
          <Button
            v-if="filters.trangThai !== null"
            icon="pi pi-times"
            outlined
            @click="filters.trangThai = null"
          />
        </InputGroup>
      </div>

      <!-- Price Range Filter -->
      <div class="col-span-2 lg:col-span-3 xl:col-span-4">
        <label class="block mb-2 font-medium">Khoảng giá</label>
        <Slider
          v-model="filters.priceRange"
          range
          :min="dynamicPricing.minPrice.value"
          :max="dynamicPricing.maxPrice.value"
          :step="dynamicPricing.priceStep.value"
          class="w-full"
        />
        <div class="flex justify-between text-xs text-surface-600 mt-1">
          <span>{{ formatCurrency(filters.priceRange[0]) }}</span>
          <span>{{ formatCurrency(filters.priceRange[1]) }}</span>
        </div>
      </div>
    </div>
  </div>

  <!-- Product DataTable -->
  <div class="card">
    <DataTable
      v-model:selection="selectedProducts"
      :value="sortedProductsWithPriceFields"
      :loading="loading"
      paginator
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20, 50]"
      dataKey="id"
      selectionMode="multiple"
      :globalFilterFields="['tenSanPham', 'maSanPham']"
      class="p-datatable-sm"
      showGridlines
      :rowHover="true"
      v-bind="getDataTableSortProps()"
      @sort="onSort"
    >
      <template #header>
        <div class="space-y-4">
          <!-- Main header row -->
          <div class="flex justify-between items-center">
            <div class="flex items-center gap-3">
              <span class="text-lg font-semibold">Danh sách sản phẩm</span>
              <Badge :value="filteredProducts.length" severity="info" outlined />

              <!-- Sort Indicator -->
              <div class="flex items-center gap-2 text-sm text-surface-600">
                <i :class="getSortIndicator.icon"></i>
                <span>{{ getSortIndicator.label }}</span>
              </div>
            </div>

            <div class="flex gap-2">
              <InputGroup>
                <InputText
                  v-model="globalFilter"
                  placeholder="Tìm kiếm..."
                  @input="onGlobalFilter"
                />
                <Button
                  v-if="globalFilter"
                  icon="pi pi-times"
                  outlined
                  @click="globalFilter = ''"
                />
              </InputGroup>
            </div>
          </div>

          <!-- Batch actions row - only shows when items are selected -->
          <div
            v-if="selectedProducts.length > 0"
            class="flex justify-between items-center p-3 bg-primary-50 dark:bg-primary-900/20 rounded-lg border border-primary-200 dark:border-primary-700"
          >
            <div class="flex items-center gap-3">
              <i class="pi pi-check-circle text-primary text-lg"></i>
              <span class="font-medium text-primary">
                Đã chọn {{ selectedProducts.length }} sản phẩm
              </span>
            </div>

            <div class="flex gap-2">
              <Button
                label="Bỏ chọn tất cả"
                icon="pi pi-times"
                severity="secondary"
                outlined
                size="small"
                @click="selectedProducts = []"
              />
              <Button
                label="Thao tác hàng loạt"
                icon="pi pi-cog"
                severity="primary"
                size="small"
                @click="showBatchActions = true"
              />
            </div>
          </div>
        </div>
      </template>

      <template #empty>
        <div class="text-center py-8">
          <i class="pi pi-inbox text-4xl text-surface-400 mb-4 block"></i>
          <p class="text-surface-600">Không tìm thấy sản phẩm nào</p>
        </div>
      </template>

      <template #loading>
        <div class="text-center py-8">
          <ProgressSpinner />
          <p class="mt-4 text-surface-600">Đang tải dữ liệu...</p>
        </div>
      </template>

      <Column selectionMode="multiple" headerStyle="width: 3rem"></Column>

      <!-- STT Column -->
      <Column header="STT" style="width: 4rem">
        <template #body="{ index }">
          <span class="font-medium">{{ index + 1 }}</span>
        </template>
      </Column>

      <!-- Product Code Column -->
      <Column field="maSanPham" header="Mã sản phẩm" sortable style="width: 10rem">
        <template #body="{ data }">
          <span class="font-mono text-sm bg-surface-100 px-2 py-1 rounded">{{ data.maSanPham }}</span>
        </template>
      </Column>

      <Column field="tenSanPham" header="Tên sản phẩm" sortable>
        <template #body="{ data }">
          <div class="flex items-center gap-3">
            <div class="relative w-12 h-12">
              <img
                v-if="getProductImageUrl(data)"
                :src="getProductImageUrl(data)"
                :alt="data.tenSanPham"
                class="w-12 h-12 object-cover rounded-lg border"
                @error="onImageError"
              />
              <div
                v-else
                class="w-12 h-12 bg-surface-100 dark:bg-surface-800 rounded-lg flex items-center justify-center border"
              >
                <i class="pi pi-image text-surface-400"></i>
              </div>
            </div>
            <div>
              <p class="font-medium">{{ data.tenSanPham }}</p>
            </div>
          </div>
        </template>
      </Column>

      <Column field="danhMucs" header="Danh mục" sortable>
        <template #body="{ data }">
          <div v-if="data.danhMucs && data.danhMucs.length > 0" class="flex flex-wrap gap-1">
            <Badge
              v-for="danhMuc in data.danhMucs"
              :key="danhMuc.id"
              :value="danhMuc.moTaDanhMuc || danhMuc.tenDanhMuc || 'Danh mục'"
              severity="info"
              outlined
            />
          </div>
          <span v-else class="text-surface-400">Chưa phân loại</span>
        </template>
      </Column>

      <Column field="thuongHieu.moTaThuongHieu" header="Thương hiệu" sortable>
        <template #body="{ data }">
          <span v-if="data.thuongHieu">{{ data.thuongHieu.moTaThuongHieu }}</span>
          <span v-else class="text-surface-400">Chưa có thương hiệu</span>
        </template>
      </Column>



      <Column field="minPrice" header="Giá thấp nhất" sortable>
        <template #body="{ data }">
          <div v-if="data.sanPhamChiTiets?.length" class="text-center">
            <div class="font-semibold">{{
              formatNumber(getMinPrice(data.sanPhamChiTiets))
            }}</div>
            <div v-if="hasPromotionalPrice(data.sanPhamChiTiets)" class="text-xs text-green-600 mt-1">
              <i class="pi pi-tag mr-1"></i>Có khuyến mãi
            </div>
          </div>
          <span v-else class="text-surface-400">Chưa có giá</span>
        </template>
      </Column>

      <Column field="maxPrice" header="Giá cao nhất" sortable>
        <template #body="{ data }">
          <div v-if="data.sanPhamChiTiets?.length" class="text-center">
            <div class="font-semibold">{{
              formatNumber(getMaxPrice(data.sanPhamChiTiets))
            }}</div>
          </div>
          <span v-else class="text-surface-400">Chưa có giá</span>
        </template>
      </Column>

      <Column header="Tồn kho" sortable>
        <template #body="{ data }">
          <div v-if="data.sanPhamChiTiets?.length" class="text-center">
            <div class="font-semibold text-lg">{{ getTotalInventory(data.sanPhamChiTiets) }}</div>
            <div class="text-xs text-surface-500">
              <span class="text-green-600">{{ getAvailableInventory(data.sanPhamChiTiets) }} có sẵn</span>
              <span v-if="getReservedInventory(data.sanPhamChiTiets) > 0" class="text-orange-600 ml-1">
                • {{ getReservedInventory(data.sanPhamChiTiets) }} đặt trước
              </span>
            </div>
          </div>
          <span v-else class="text-surface-400">0</span>
        </template>
      </Column>

      <Column field="trangThai" header="Trạng thái" sortable>
        <template #body="{ data }">
          <div class="flex items-center justify-center">
            <div v-if="data.trangThai" class="flex items-center gap-2 px-3 py-1 bg-green-50 dark:bg-green-900/20 rounded-full border border-green-200 dark:border-green-700">
              <i class="pi pi-check-circle text-green-600 dark:text-green-400 text-sm"></i>
              <span class="text-green-700 dark:text-green-300 text-sm font-medium">Hoạt động</span>
            </div>
            <div v-else class="flex items-center gap-2 px-3 py-1 bg-red-50 dark:bg-red-900/20 rounded-full border border-red-200 dark:border-red-700">
              <i class="pi pi-times-circle text-red-600 dark:text-red-400 text-sm"></i>
              <span class="text-red-700 dark:text-red-300 text-sm font-medium">Ngừng hoạt động</span>
            </div>
          </div>
        </template>
      </Column>

      <Column
        field="ngayTao"
        header="Ngày tạo"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDateTime(data.ngayTao) }}
        </template>
      </Column>

      <Column
        field="ngayCapNhat"
        header="Ngày cập nhật"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDateTime(data.ngayCapNhat) }}
        </template>
      </Column>

      <Column header="Thao tác" style="width: 12rem">
        <template #body="{ data }">
          <div class="flex gap-2">
            <Button
              icon="pi pi-eye"
              severity="info"
              text
              rounded
              @click="viewProduct(data)"
              v-tooltip="'Xem chi tiết'"
            />
            <Button
              icon="pi pi-pencil"
              severity="warning"
              text
              rounded
              @click="editProduct(data)"
              v-tooltip="'Chỉnh sửa'"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              @click.stop="confirmDelete(data)"
              v-tooltip="'Xóa'"
              :disabled="loading"
            />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>

  <!-- Batch Actions Dialog -->
  <Dialog
    v-model:visible="showBatchActions"
    header="Thao tác hàng loạt"
    :style="{ width: '450px' }"
    modal
  >
    <div class="space-y-4">
      <p>Đã chọn {{ selectedProducts.length }} sản phẩm</p>

      <div class="flex flex-col gap-2">
        <label class="text-sm font-medium">Thay đổi trạng thái</label>
        <Select
          v-model="batchStatus"
          :options="[
            { label: 'Kích hoạt', value: true },
            { label: 'Vô hiệu hóa', value: false },
          ]"
          optionLabel="label"
          optionValue="value"
          placeholder="Chọn trạng thái"
        />
      </div>

      <div class="flex flex-col gap-2">
        <label class="text-sm font-medium">Lý do thay đổi</label>
        <Textarea v-model="batchReason" placeholder="Nhập lý do thay đổi..." rows="3" />
      </div>

      <!-- Progress indicator during batch operation -->
      <div v-if="batchLoading" class="mt-4 p-3 bg-surface-50 dark:bg-surface-800 rounded-lg">
        <div class="flex items-center gap-2 mb-2">
          <ProgressSpinner size="small" />
          <span class="text-sm font-medium">Đang cập nhật sản phẩm...</span>
        </div>
        <ProgressBar :value="batchProgress" class="h-2" />
        <p class="text-xs text-surface-600 mt-1">
          Đã xử lý {{ batchProcessed }}/{{ selectedProducts.length }} sản phẩm
        </p>
      </div>
    </div>

    <template #footer>
      <Button label="Hủy" severity="secondary" outlined @click="cancelBatchActions" />
      <Button label="Áp dụng" @click="applyBatchActions" :loading="batchLoading" />
    </template>
  </Dialog>




</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useProductStore } from '@/stores/productstore'
import { useAttributeStore } from '@/stores/attributestore'
import { useProductFilters } from '@/composables/useProductFilters'
import { useDynamicPricing } from '@/composables/useDynamicPricing'
import { useDataTableSorting } from '@/composables/useDataTableSorting'
import { useDataTableRealTime } from '@/composables/useDataTableRealTime'
import { debounce } from 'lodash-es'
import storageApi from '@/apis/storage'
import inventoryApi from '@/apis/inventoryApi'
import serialNumberApi from '@/apis/serialNumberApi'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const productStore = useProductStore()
const attributeStore = useAttributeStore()

// Use composables for filters and dynamic pricing
const { filters, filteredProducts, clearFilters: clearFiltersComposable } = useProductFilters()
const dynamicPricing = useDynamicPricing()

// Auto-Sorting Composable
const {
  getDataTableSortProps,
  onSort,
  applySorting,
  getSortIndicator
} = useDataTableSorting({
  defaultSortField: 'ngayCapNhat',
  defaultSortOrder: -1, // Newest first
  enableUserOverride: true
})

// Real-time DataTable integration
const realTimeDataTable = useDataTableRealTime({
  entityType: 'sanPham',
  storeKey: 'productList',
  refreshCallback: async (refreshInfo) => {
    console.log('🔄 ProductList: Real-time refresh triggered:', refreshInfo)

    // Refresh product data from store
    await productStore.forceRefreshProducts()

    // Refresh dynamic pricing if needed
    if (refreshInfo.source === 'WEBSOCKET' && refreshInfo.topic?.includes('gia-san-pham')) {
      await dynamicPricing.refreshPricing()
    }
  },
  debounceDelay: 200, // Faster refresh for product updates
  enableSelectiveUpdates: true,
  topicFilters: ['san-pham', 'gia-san-pham', 'ton-kho', 'product']
})

// Component state
const loading = ref(false)
const globalFilter = ref('')
const selectedProducts = ref([])
const showBatchActions = ref(false)
const batchStatus = ref(null)
const batchReason = ref('')
const batchLoading = ref(false)
const batchProgress = ref(0)
const batchProcessed = ref(0)
const imageUrlCache = ref(new Map()) // Cache for presigned URLs
const inventoryData = ref(new Map()) // Cache for inventory data by variant ID





// Computed properties
const categories = computed(() => attributeStore.category)
const brands = computed(() => attributeStore.brand)

// Methods
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount)
}

const formatNumber = (amount) => {
  return new Intl.NumberFormat('vi-VN').format(amount)
}

const formatDateTime = (date) => {
  if (!date) return ''
  return new Intl.DateTimeFormat('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(date))
}

const getMinPrice = (variants) => {
  return Math.min(...variants.map((v) => {
    // Use promotional price if available and lower than regular price
    const regularPrice = v.giaBan
    const promoPrice = v.giaKhuyenMai
    return (promoPrice && promoPrice < regularPrice) ? promoPrice : regularPrice
  }))
}

const getMaxPrice = (variants) => {
  return Math.max(...variants.map((v) => {
    // Use promotional price if available and lower than regular price
    const regularPrice = v.giaBan
    const promoPrice = v.giaKhuyenMai
    return (promoPrice && promoPrice < regularPrice) ? promoPrice : regularPrice
  }))
}

// New utility methods for enhanced functionality

const hasPromotionalPrice = (variants) => {
  return variants.some(v => v.giaKhuyenMai && v.giaKhuyenMai < v.giaBan)
}

const getTotalInventory = (variants) => {
  // Calculate total inventory from all variants using cached inventory data
  return variants.reduce((total, variant) => {
    const inventory = inventoryData.value.get(variant.id)
    if (!inventory) return total
    return total + inventory.available + inventory.reserved
  }, 0)
}

const getAvailableInventory = (variants) => {
  // Calculate available inventory from all variants using cached inventory data
  return variants.reduce((total, variant) => {
    const inventory = inventoryData.value.get(variant.id)
    return total + (inventory?.available || 0)
  }, 0)
}

const getReservedInventory = (variants) => {
  // Calculate reserved inventory from all variants using cached inventory data
  return variants.reduce((total, variant) => {
    const inventory = inventoryData.value.get(variant.id)
    return total + (inventory?.reserved || 0)
  }, 0)
}







// Image URL utility functions
const getProductImageUrl = (product) => {
  if (!product?.hinhAnh || !product.hinhAnh.length) return null

  const firstImage = product.hinhAnh[0]
  if (!firstImage) return null

  // If it's already a full URL, return as is
  if (firstImage.startsWith('http')) return firstImage

  // Check cache first
  if (imageUrlCache.value.has(firstImage)) {
    return imageUrlCache.value.get(firstImage)
  }

  // Load presigned URL asynchronously
  loadImageUrl(firstImage)

  // Return null for now, will update when loaded
  return null
}

const loadImageUrl = async (imageFilename) => {
  try {
    // Get presigned URL for the image filename
    const presignedUrl = await storageApi.getPresignedUrl('products', imageFilename)

    // Cache the URL for future use
    imageUrlCache.value.set(imageFilename, presignedUrl)

    // Force reactivity update
    imageUrlCache.value = new Map(imageUrlCache.value)
  } catch (error) {
    console.warn('Error getting presigned URL for image:', imageFilename, error)
    // Cache null to prevent repeated attempts
    imageUrlCache.value.set(imageFilename, null)
  }
}

const onImageError = (event) => {
  // Hide broken image and show placeholder
  event.target.style.display = 'none'
}

const debouncedSearch = debounce(() => {
  // Trigger search
}, 300)

const onGlobalFilter = debounce(() => {
  // Apply global filter
}, 300)

const navigateToAdd = () => {
  router.push({ name: 'product-add' })
}

const viewProduct = (product) => {
  router.push({ name: 'product-detail', params: { id: product.id } })
}

const editProduct = (product) => {
  router.push({ name: 'product-edit', params: { id: product.id } })
}



const confirmDelete = (product) => {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa sản phẩm "${product.tenSanPham}"?`,
    header: 'Xác nhận xóa',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: {
      label: 'Hủy',
      severity: 'secondary',
      outlined: true,
    },
    acceptProps: {
      label: 'Xóa',
      severity: 'danger',
    },
    accept: () => deleteProduct(product),
  })
}

const deleteProduct = async (product) => {
  try {
    await productStore.deleteProduct(product.id)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Xóa sản phẩm thành công',
      life: 3000,
    })
    await refreshData()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Lỗi xóa sản phẩm',
      life: 3000,
    })
  }
}

const clearFilters = () => {
  clearFiltersComposable()
  globalFilter.value = ''
  toast.add({
    severity: 'info',
    summary: 'Bộ lọc',
    detail: 'Đã xóa tất cả bộ lọc',
    life: 2000,
  })
}

const cancelBatchActions = () => {
  showBatchActions.value = false
  batchStatus.value = null
  batchReason.value = ''
  batchProgress.value = 0
  batchProcessed.value = 0
}

const applyBatchActions = async () => {
  // Fix validation: batchStatus.value can be false (boolean), so check for null/undefined instead
  if (batchStatus.value === null || batchStatus.value === undefined || !batchReason.value.trim()) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng chọn trạng thái và nhập lý do',
      life: 3000,
    })
    return
  }

  batchLoading.value = true
  batchProgress.value = 0
  batchProcessed.value = 0

  try {
    const productIds = selectedProducts.value.map((p) => p.id)
    const totalProducts = selectedProducts.value.length

    // Use the proper batch endpoint
    const result = await productStore.updateMultipleProductStatus(productIds, batchStatus.value, batchReason.value)

    // Update progress to 100%
    batchProcessed.value = totalProducts
    batchProgress.value = 100

    // Use the result from the batch operation
    const successCount = result.successCount || 0
    const errorCount = result.failureCount || 0

    if (successCount > 0) {
      toast.add({
        severity: successCount === productIds.length ? 'success' : 'warn',
        summary: successCount === productIds.length ? 'Thành công' : 'Hoàn thành một phần',
        detail: `Đã cập nhật ${successCount}/${productIds.length} sản phẩm${errorCount > 0 ? `, ${errorCount} sản phẩm lỗi` : ''}`,
        life: 3000,
      })
    } else {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không thể cập nhật sản phẩm nào',
        life: 3000,
      })
    }

    showBatchActions.value = false
    selectedProducts.value = []
    batchStatus.value = null
    batchReason.value = ''
    batchProgress.value = 0
    batchProcessed.value = 0

    // Refresh data to ensure consistency (after a small delay to let local updates take effect)
    setTimeout(async () => {
      await refreshData()
    }, 100)
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Lỗi cập nhật hàng loạt',
      life: 3000,
    })
  } finally {
    batchLoading.value = false
  }
}





// Function to fetch inventory data for all variants
const fetchInventoryData = async () => {
  try {
    // Get all variant IDs from current products
    const variantIds = []
    productStore.products.forEach(product => {
      if (product.sanPhamChiTiets) {
        product.sanPhamChiTiets.forEach(variant => {
          variantIds.push(variant.id)
        })
      }
    })

    if (variantIds.length === 0) return

    // Use batch availability API for better performance
    const batchResult = await inventoryApi.getBatchAvailability(variantIds)

    if (batchResult.success) {
      // Update inventory cache with available quantities
      const newInventoryData = new Map()

      // For each variant, get available quantity and fetch reserved count separately
      const reservedPromises = variantIds.map(async (variantId) => {
        try {
          const serialNumbers = await serialNumberApi.getSerialNumbersByVariant(variantId)
          const reserved = serialNumbers.filter(serial => serial.trangThai === 'RESERVED').length
          return { variantId, reserved }
        } catch (error) {
          console.warn(`Error fetching reserved count for variant ${variantId}:`, error)
          return { variantId, reserved: 0 }
        }
      })

      const reservedResults = await Promise.all(reservedPromises)
      const reservedMap = new Map()
      reservedResults.forEach(result => {
        reservedMap.set(result.variantId, result.reserved)
      })

      // Combine available and reserved data
      variantIds.forEach(variantId => {
        const available = batchResult.data[variantId] || 0
        const reserved = reservedMap.get(variantId) || 0
        newInventoryData.set(variantId, {
          available,
          reserved,
          total: available + reserved
        })
      })

      inventoryData.value = newInventoryData
    }

  } catch (error) {
    console.error('Error fetching inventory data:', error)
  }
}

// Computed property to add price fields to products
const productsWithPriceFields = computed(() => {
  return filteredProducts.value.map(product => ({
    ...product,
    minPrice: product.sanPhamChiTiets?.length ? getMinPrice(product.sanPhamChiTiets) : 0,
    maxPrice: product.sanPhamChiTiets?.length ? getMaxPrice(product.sanPhamChiTiets) : 0
  }))
})

// Apply auto-sorting to products with price fields
const sortedProductsWithPriceFields = computed(() => {
  return applySorting(productsWithPriceFields.value)
})

const refreshData = async () => {
  loading.value = true
  try {
    // First load products and attributes
    await Promise.all([productStore.fetchProducts(), attributeStore.fetchAllAttributes()])

    // Then load inventory data for all variants
    await fetchInventoryData()
  } catch (err) {
    console.error('Error loading data:', err)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Lỗi tải dữ liệu',
      life: 3000,
    })
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(async () => {
  await refreshData()
})
</script>

<style scoped>
.product-list-container {
  padding: 1.5rem;
}

.page-header {
  background: linear-gradient(135deg, var(--primary-50) 0%, var(--primary-100) 100%);
  border-radius: 12px;
  padding: 2rem;
  margin-bottom: 2rem;
}

.dark .page-header {
  background: linear-gradient(135deg, var(--primary-900) 0%, var(--primary-800) 100%);
}
</style>
