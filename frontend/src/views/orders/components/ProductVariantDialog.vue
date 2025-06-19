<template>
  <Dialog
    :visible="visible"
    modal
    :header="dialogTitle"
    :style="{ width: '95vw', maxWidth: '1400px' }"
    class="product-variant-dialog"
    @update:visible="$emit('update:visible', $event)"
    @hide="onDialogHide"
  >
    <!-- Product Catalog Header -->
    <div class="mb-6 p-4 border rounded-lg bg-surface-50 dark:bg-surface-800">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <i class="pi pi-shopping-bag text-primary text-2xl"></i>
          <div>
            <h3 class="text-xl font-semibold text-surface-900 dark:text-surface-0 mb-1">
              Danh mục sản phẩm
            </h3>
            <div class="text-sm text-surface-600 dark:text-surface-400">
              Chọn serial number để thêm sản phẩm vào giỏ hàng
            </div>
          </div>
        </div>
        <div class="flex items-center gap-4 text-sm">
          <div class="flex items-center gap-1">
            <i class="pi pi-tag text-primary"></i>
            <span>{{ availableVariants.length }} phiên bản</span>
          </div>
          <div class="flex items-center gap-1">
            <i class="pi pi-filter text-primary"></i>
            <span>{{ filteredVariants.length }} hiển thị</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Variant Selection DataTable -->
    <div class="mb-6">
      <h4 class="text-lg font-semibold mb-4 text-surface-900 dark:text-surface-0">
        Chọn phiên bản sản phẩm
      </h4>

      <!-- Filters Section -->
      <div class="mb-6 border p-4 rounded-lg">
        <!-- Search Input -->
        <div class="mb-4">
          <label class="block mb-2 font-medium">Tìm kiếm sản phẩm</label>
          <InputText
            v-model="filters.searchQuery"
            placeholder="Tìm theo tên sản phẩm, mã sản phẩm, SKU, CPU, RAM, GPU..."
            fluid
            class="w-full"
          />
        </div>

        <!-- Attribute Filters Grid -->
        <div class="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          <!-- CPU Filter -->
        <div>
          <label class="block mb-2 font-medium">CPU</label>
          <InputGroup>
            <Select
              v-model="filters.cpu"
              :options="cpus"
              optionLabel="moTaCpu"
              optionValue="id"
              placeholder="Chọn CPU"
              fluid
            />
            <Button
              v-if="filters.cpu"
              icon="pi pi-times"
              outlined
              @click="filters.cpu = null"
            />
          </InputGroup>
        </div>

        <!-- RAM Filter -->
        <div>
          <label class="block mb-2 font-medium">RAM</label>
          <InputGroup>
            <Select
              v-model="filters.ram"
              :options="rams"
              optionLabel="moTaRam"
              optionValue="id"
              placeholder="Chọn RAM"
              fluid
            />
            <Button
              v-if="filters.ram"
              icon="pi pi-times"
              outlined
              @click="filters.ram = null"
            />
          </InputGroup>
        </div>

        <!-- GPU Filter -->
        <div>
          <label class="block mb-2 font-medium">GPU</label>
          <InputGroup>
            <Select
              v-model="filters.gpu"
              :options="gpus"
              optionLabel="moTaGpu"
              optionValue="id"
              placeholder="Chọn GPU"
              fluid
            />
            <Button
              v-if="filters.gpu"
              icon="pi pi-times"
              outlined
              @click="filters.gpu = null"
            />
          </InputGroup>
        </div>

        <!-- Colors Filter -->
        <div>
          <label class="block mb-2 font-medium">Màu sắc</label>
          <InputGroup>
            <Select
              v-model="filters.colors"
              :options="colors"
              optionLabel="moTaMauSac"
              optionValue="id"
              placeholder="Chọn màu sắc"
              fluid
            />
            <Button
              v-if="filters.colors"
              icon="pi pi-times"
              outlined
              @click="filters.colors = null"
            />
          </InputGroup>
        </div>

        <!-- Storage Filter -->
        <div>
          <label class="block mb-2 font-medium">Dung lượng</label>
          <InputGroup>
            <Select
              v-model="filters.storage"
              :options="storages"
              optionLabel="moTaOCung"
              optionValue="id"
              placeholder="Chọn dung lượng"
              fluid
            />
            <Button
              v-if="filters.storage"
              icon="pi pi-times"
              outlined
              @click="filters.storage = null"
            />
          </InputGroup>
        </div>

        <!-- Screen Filter -->
        <div>
          <label class="block mb-2 font-medium">Màn hình</label>
          <InputGroup>
            <Select
              v-model="filters.screen"
              :options="screens"
              optionLabel="moTaManHinh"
              optionValue="id"
              placeholder="Chọn màn hình"
              fluid
            />
            <Button
              v-if="filters.screen"
              icon="pi pi-times"
              outlined
              @click="filters.screen = null"
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

          <!-- Clear Filters Button -->
          <div class="col-span-2 lg:col-span-3 xl:col-span-4 flex justify-end">
            <Button
              label="Xóa bộ lọc"
              icon="pi pi-filter-slash"
              outlined
              @click="clearAllFilters"
              :disabled="!hasActiveFilters"
            />
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="text-center py-8">
        <i class="pi pi-spin pi-spinner text-2xl mb-2 text-primary"></i>
        <p class="text-surface-500">Đang tải các phiên bản...</p>
      </div>

      <!-- No Variants Available -->
      <div v-else-if="!filteredVariants.length" class="text-center py-8">
        <i class="pi pi-exclamation-triangle text-2xl mb-2 text-orange-500"></i>
        <p class="text-surface-500">
          {{ availableVariants.length === 0 ? 'Không có phiên bản nào có sẵn' : 'Không có phiên bản nào phù hợp với bộ lọc' }}
        </p>
        <Button
          v-if="availableVariants.length > 0 && hasActiveFilters"
          label="Xóa bộ lọc"
          icon="pi pi-filter-slash"
          outlined
          @click="clearAllFilters"
          class="mt-3"
        />
      </div>

      <!-- Variants DataTable -->
      <div v-else>
        <DataTable
          :value="filteredVariants"
          dataKey="id"
          :paginator="filteredVariants.length > 10"
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20]"
          class="p-datatable-sm"
          :scrollable="true"
          scrollHeight="400px"
          v-bind="getDataTableSortProps()"
          @sort="onSort"
        >
          <template #header>
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-2">
                <span class="font-semibold">Danh sách phiên bản</span>
                <Badge :value="filteredVariants.length" severity="info" />
                <span v-if="hasActiveFilters" class="text-xs text-surface-500">
                  ({{ availableVariants.length }} tổng cộng)
                </span>
              </div>
              <div class="flex items-center gap-2">
                <Button
                  icon="pi pi-refresh"
                  size="small"
                  outlined
                  @click="refreshProductData"
                  v-tooltip.top="'Làm mới giá sản phẩm'"
                  :loading="loading"
                />
              </div>
            </div>
          </template>

          <Column field="sanPham.tenSanPham" header="Sản phẩm" sortable style="min-width: 200px">
            <template #body="{ data }">
              <div>
                <div class="font-medium text-sm">{{ data.sanPham?.tenSanPham || 'N/A' }}</div>
                <div class="text-xs text-surface-500">{{ data.sanPham?.maSanPham || 'N/A' }}</div>
              </div>
            </template>
          </Column>

          <Column field="sku" header="SKU" sortable style="min-width: 150px">
            <template #body="{ data }">
              <span v-if="data.sku" class="font-mono text-sm bg-surface-100 px-2 py-1 rounded">
                {{ data.sku }}
              </span>
              <span v-else class="text-surface-400 text-sm italic">Auto-generated</span>
            </template>
          </Column>

          <Column field="mauSac.moTaMauSac" header="Màu sắc" sortable style="min-width: 100px">
            <template #body="{ data }">
              <div class="flex items-center gap-2">
                <div
                  class="w-4 h-4 rounded-full border border-surface-300"
                  :style="{ backgroundColor: getColorValue(data.mauSac?.moTaMauSac) }"
                ></div>
                <span>{{ data.mauSac?.moTaMauSac || 'N/A' }}</span>
              </div>
            </template>
          </Column>

          <Column field="cpu.moTaCpu" header="CPU" sortable style="min-width: 120px">
            <template #body="{ data }">
              <span>{{ data.cpu?.moTaCpu || 'N/A' }}</span>
            </template>
          </Column>

          <Column field="ram.moTaRam" header="RAM" sortable style="min-width: 100px">
            <template #body="{ data }">
              <span>{{ data.ram?.moTaRam || 'N/A' }}</span>
            </template>
          </Column>

          <Column field="gpu.moTaGpu" header="GPU" sortable style="min-width: 120px">
            <template #body="{ data }">
              <span>{{ data.gpu?.moTaGpu || 'N/A' }}</span>
            </template>
          </Column>

          <Column field="boNho.moTaBoNho" header="Dung lượng" sortable style="min-width: 100px">
            <template #body="{ data }">
              <span>{{ data.boNho?.moTaBoNho || 'N/A' }}</span>
            </template>
          </Column>

          <Column field="giaBan" header="Giá bán" sortable style="min-width: 140px">
            <template #body="{ data }">
              <div class="relative">
                <!-- Price change indicator -->
                <div v-if="hasRecentPriceChange(data.id)" class="absolute -top-1 -right-1">
                  <i class="pi pi-exclamation-circle text-orange-500 text-xs"
                     v-tooltip.top="'Giá đã thay đổi gần đây'"></i>
                </div>

                <!-- Current price display -->
                <div class="space-y-1">
                  <div class="font-semibold text-primary flex items-center gap-1">
                    {{ formatCurrency(getVariantPrice(data)) }}
                    <span v-if="getLatestPriceForVariant(data.id) !== null"
                          class="text-xs bg-orange-100 text-orange-700 px-1 rounded">
                      Cập nhật
                    </span>
                  </div>

                  <!-- Show original price if different from current -->
                  <div v-if="getLatestPriceForVariant(data.id) !== null &&
                           getLatestPriceForVariant(data.id) !== (data.giaKhuyenMai || data.giaBan)"
                       class="text-xs text-surface-500 line-through">
                    {{ formatCurrency(data.giaKhuyenMai || data.giaBan) }}
                  </div>
                </div>
              </div>
            </template>
          </Column>

          <Column header="Có sẵn" style="min-width: 100px">
            <template #body="{ data }">
              <div class="text-center">
                <div class="font-semibold text-lg text-green-600">{{ getAvailableSerialCount(data) }}</div>
                <div class="text-xs text-surface-500">sản phẩm</div>
              </div>
            </template>
          </Column>

          <Column field="ngayCapNhat" header="Ngày cập nhật" sortable style="min-width: 140px">
            <template #body="{ data }">
              {{ formatDateTime(data.ngayCapNhat) }}
            </template>
          </Column>

          <Column header="Thao tác" style="width: 140px">
            <template #body="{ data }">
              <Button
                label="Chọn & Thêm"
                icon="pi pi-shopping-cart"
                size="small"
                severity="primary"
                @click="selectSerialNumbers(data)"
                :disabled="getAvailableSerialCount(data) === 0"
                v-tooltip.top="'Chọn serial number và thêm vào giỏ hàng'"
              />
            </template>
          </Column>
        </DataTable>
      </div>
    </div>



    <!-- Dialog Actions -->
    <template #footer>
      <div class="flex justify-between items-center w-full">
        <div class="text-sm text-surface-500">
          Sử dụng nút "Chọn & Thêm" để chọn serial number và thêm vào giỏ hàng
        </div>
        <div class="flex gap-2">
          <Button
            label="Đóng"
            icon="pi pi-times"
            text
            @click="closeDialog"
          />
        </div>
      </div>
    </template>
  </Dialog>

  <!-- Serial Number Selection Dialog -->
  <Dialog
    v-model:visible="serialDialogVisible"
    modal
    :header="`Chọn Serial Numbers - ${selectedVariantForSerial?.sku || 'N/A'}`"
    :style="{ width: '800px' }"
    class="serial-selection-dialog"
  >
    <div v-if="selectedVariantForSerial" class="space-y-4">
      <!-- Variant Information -->
      <div class="p-3 bg-surface-50 dark:bg-surface-800 rounded-lg">
        <div class="flex items-center gap-2 mb-2">
          <i class="pi pi-info-circle text-primary"></i>
          <span class="font-medium">Thông tin phiên bản</span>
        </div>
        <p class="text-sm">
          <strong>Phiên bản:</strong> {{ getVariantDisplayName(selectedVariantForSerial) }}
        </p>
        <p class="text-sm">
          <strong>SKU:</strong> {{ selectedVariantForSerial.sku || 'Auto-generated' }}
        </p>
        <p class="text-sm">
          <strong>Giá:</strong> {{ formatCurrency(getVariantPrice(selectedVariantForSerial)) }}
        </p>
      </div>

      <!-- Available Serial Numbers -->
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <label class="font-semibold">Serial Numbers có sẵn:</label>
          <Badge :value="filteredSerialNumbers.length" severity="info" />
        </div>

        <!-- Serial Number Search -->
        <div class="mb-3">
          <InputText
            v-model="serialFilterQuery"
            placeholder="Tìm kiếm serial number..."
            fluid
            class="w-full"
          />
        </div>

        <div v-if="filteredSerialNumbers.length === 0" class="text-center py-8 text-surface-500">
          <i class="pi pi-exclamation-triangle text-2xl mb-2"></i>
          <p v-if="availableSerialNumbers.length === 0">Không có serial number nào có sẵn cho phiên bản này</p>
          <p v-else>Không tìm thấy serial number nào phù hợp với từ khóa tìm kiếm</p>
        </div>

        <DataTable
          v-else
          v-model:selection="selectedSerialNumbers"
          :value="filteredSerialNumbers"
          selectionMode="multiple"
          dataKey="id"
          :paginator="filteredSerialNumbers.length > 10"
          :rows="10"
          class="p-datatable-sm"
          showGridlines
        >
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-semibold">Chọn Serial Numbers</span>
              <div v-if="selectedSerialNumbers.length > 0" class="text-sm text-surface-600">
                Đã chọn: {{ selectedSerialNumbers.length }}
              </div>
            </div>
          </template>

          <Column selectionMode="multiple" headerStyle="width: 3rem"></Column>

          <Column field="serialNumberValue" header="Serial Number" sortable>
            <template #body="{ data }">
              <span class="font-mono">{{ data.serialNumberValue || data.serialNumber }}</span>
            </template>
          </Column>

          <Column field="trangThai" header="Trạng thái" sortable>
            <template #body>
              <Badge
                value="Có sẵn"
                severity="success"
              />
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-between items-center w-full">
        <div v-if="selectedSerialNumbers.length > 0" class="text-sm text-surface-600">
          Đã chọn {{ selectedSerialNumbers.length }} serial numbers
        </div>
        <div v-else-if="serialFilterQuery" class="text-sm text-surface-600">
          Hiển thị {{ filteredSerialNumbers.length }} / {{ availableSerialNumbers.length }} serial numbers
        </div>
        <div class="flex gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            text
            @click="serialDialogVisible = false"
          />
          <Button
            label="Thêm vào giỏ hàng"
            icon="pi pi-shopping-cart"
            @click="confirmSerialSelection"
            :disabled="selectedSerialNumbers.length === 0"
          />
        </div>
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useAttributeStore } from '@/stores/attributestore'
import { useProductStore } from '@/stores/productstore'
import { useDynamicPricing } from '@/composables/useDynamicPricing'
import { useRealTimePricing } from '@/composables/useRealTimePricing'
import { useDataTableSorting } from '@/composables/useDataTableSorting'
// REMOVED: Complex cache-related imports - using simple approach now
import { storeToRefs } from 'pinia'

import serialNumberApi from '@/apis/serialNumberApi'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Badge from 'primevue/badge'
import Select from 'primevue/select'
import InputGroup from 'primevue/inputgroup'
import InputText from 'primevue/inputtext'
import Slider from 'primevue/slider'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  product: {
    type: Object,
    default: null
  }
})

// Emits
const emit = defineEmits(['update:visible', 'variant-selected', 'request-cart-sync'])

// Composables
const toast = useToast()
const attributeStore = useAttributeStore()
const productStore = useProductStore()
const dynamicPricing = useDynamicPricing()

// Real-time pricing integration
const {
  priceUpdates,
  subscribeToPriceUpdates,
  getLatestPriceForVariant,
  hasRecentPriceChange
} = useRealTimePricing()

// DataTable sorting integration
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

// SIMPLIFIED: Removed complex cache system, using direct API + WebSocket pub/sub
// The backend handles inventory correctly via WebSocket notifications

// Destructure attribute store
const {
  cpu: cpus,
  ram: rams,
  gpu: gpus,
  colors,
  storage: storages,
  screen: screens
} = storeToRefs(attributeStore)

// Destructure product store
const { products } = storeToRefs(productStore)

// Local state
const loading = ref(false)
const variantSerialNumbers = ref(new Map()) // Cache for variant serial numbers
const usedSerialNumbers = ref(new Set()) // Track serial numbers that have been added to cart
const preventSyncOverride = ref(false) // Flag to prevent race conditions during immediate updates

// Serial number selection dialog state
const serialDialogVisible = ref(false)
const selectedVariantForSerial = ref(null)
const availableSerialNumbers = ref([])
const selectedSerialNumbers = ref([])
const serialFilterQuery = ref('')



// Filter state
const filters = ref({
  cpu: null,
  ram: null,
  gpu: null,
  colors: null,
  storage: null,
  screen: null,
  priceRange: [0, 50000000], // Will be updated dynamically
  searchQuery: ''
})

// Watch for price range changes and update filter default
watch(() => dynamicPricing.defaultPriceRange.value, (newRange) => {
  // Only update if current range is at the old default
  if (filters.value.priceRange[0] === 0 && filters.value.priceRange[1] === 50000000) {
    filters.value.priceRange = [...newRange]
  }
}, { immediate: true })

// Computed properties
const dialogTitle = computed(() => {
  return 'Chọn sản phẩm'
})

const availableVariants = computed(() => {
  if (!products.value?.length) return []

  // Collect all variants from all products
  const allVariants = []

  for (const product of products.value) {
    if (product.sanPhamChiTiets && Array.isArray(product.sanPhamChiTiets)) {
      // Filter only active variants and add product reference
      const activeVariants = product.sanPhamChiTiets
        .filter(variant => variant.active === true)
        .map(variant => ({
          ...variant,
          sanPham: product // Add product reference for display
        }))

      allVariants.push(...activeVariants)
    }
  }

  return allVariants
})

// Filtered variants computed property
const filteredVariants = computed(() => {
  let filtered = availableVariants.value

  // Apply search filter first
  if (filters.value.searchQuery && filters.value.searchQuery.trim()) {
    const query = filters.value.searchQuery.toLowerCase().trim()
    filtered = filtered.filter(variant => {
      // Search in product name, product code, variant SKU, and variant attributes
      const productName = variant.sanPham?.tenSanPham?.toLowerCase() || ''
      const productCode = variant.sanPham?.maSanPham?.toLowerCase() || ''
      const variantSku = variant.sku?.toLowerCase() || ''
      const variantCode = variant.maSanPhamChiTiet?.toLowerCase() || ''

      // Search in attributes
      const cpu = variant.cpu?.moTaCpu?.toLowerCase() || ''
      const ram = variant.ram?.moTaRam?.toLowerCase() || ''
      const gpu = variant.gpu?.moTaGpu?.toLowerCase() || ''
      const color = variant.mauSac?.moTaMauSac?.toLowerCase() || ''
      const storage = (variant.oCung || variant.ocung)?.moTaOCung?.toLowerCase() || ''
      const screen = variant.manHinh?.moTaManHinh?.toLowerCase() || ''

      // Search in serial numbers (check cached serial numbers for this variant)
      const cachedSerials = variantSerialNumbers.value.get(variant.id) || []
      const hasMatchingSerial = cachedSerials.some(serial => {
        const serialValue = (serial.serialNumberValue || serial.serialNumber || '').toLowerCase()
        return serialValue.includes(query)
      })

      return productName.includes(query) ||
             productCode.includes(query) ||
             variantSku.includes(query) ||
             variantCode.includes(query) ||
             cpu.includes(query) ||
             ram.includes(query) ||
             gpu.includes(query) ||
             color.includes(query) ||
             storage.includes(query) ||
             screen.includes(query) ||
             hasMatchingSerial
    })
  }

  // Apply CPU filter
  if (filters.value.cpu) {
    filtered = filtered.filter(variant => variant.cpu?.id === filters.value.cpu)
  }

  // Apply RAM filter
  if (filters.value.ram) {
    filtered = filtered.filter(variant => variant.ram?.id === filters.value.ram)
  }

  // Apply GPU filter
  if (filters.value.gpu) {
    filtered = filtered.filter(variant => variant.gpu?.id === filters.value.gpu)
  }

  // Apply Colors filter
  if (filters.value.colors) {
    filtered = filtered.filter(variant => variant.mauSac?.id === filters.value.colors)
  }

  // Apply Storage filter
  if (filters.value.storage) {
    filtered = filtered.filter(variant => {
      // Handle both boNho (camelCase) and bonho (lowercase) field names for backward compatibility
      const storage = variant.boNho || variant.bonho || variant.oCung || variant.ocung
      return storage?.id === filters.value.storage
    })
  }

  // Apply Screen filter
  if (filters.value.screen) {
    filtered = filtered.filter(variant => variant.manHinh?.id === filters.value.screen)
  }

  // Apply price range filter
  if (filters.value.priceRange && filters.value.priceRange.length === 2) {
    const [minPrice, maxPrice] = filters.value.priceRange
    filtered = filtered.filter(variant => {
      const price = variant.giaKhuyenMai && variant.giaKhuyenMai < variant.giaBan
        ? variant.giaKhuyenMai
        : variant.giaBan
      return price >= minPrice && price <= maxPrice
    })
  }

  // Apply sorting (auto-sort by ngayCapNhat, newest first)
  return applySorting(filtered)
})

// Check if any filters are active
const hasActiveFilters = computed(() => {
  const defaultRange = dynamicPricing.defaultPriceRange.value
  return filters.value.cpu !== null ||
         filters.value.ram !== null ||
         filters.value.gpu !== null ||
         filters.value.colors !== null ||
         filters.value.storage !== null ||
         filters.value.screen !== null ||
         (filters.value.priceRange[0] !== defaultRange[0] || filters.value.priceRange[1] !== defaultRange[1]) ||
         (filters.value.searchQuery && filters.value.searchQuery.trim())
})

// Filtered serial numbers for the dialog
const filteredSerialNumbers = computed(() => {
  if (!serialFilterQuery.value?.trim()) {
    return availableSerialNumbers.value
  }

  const query = serialFilterQuery.value.toLowerCase().trim()
  return availableSerialNumbers.value.filter(serial => {
    const serialValue = (serial.serialNumberValue || serial.serialNumber || '').toLowerCase()
    return serialValue.includes(query)
  })
})

// Methods
const getColorValue = (colorName) => {
  const colorMap = {
    Đỏ: '#ef4444',
    Xanh: '#3b82f6',
    Vàng: '#eab308',
    Đen: '#000000',
    Trắng: '#ffffff',
    Xám: '#6b7280',
    Bạc: '#9ca3af'
  }
  return colorMap[colorName] || '#6b7280'
}

// Simplified method to get available serial count - NO CACHE, direct API approach
const getAvailableSerialCount = (variant) => {
  // Get fresh serial numbers from API for this variant
  const cachedSerials = variantSerialNumbers.value.get(variant.id)
  if (!cachedSerials) {
    console.log(`🔍 [INVENTORY DEBUG] Variant ${variant.id}: Loading serials...`)
    // Load serials asynchronously and return 0 for now
    loadSerialNumbersForVariant(variant.id)
    return 0
  }

  // Filter out serials that are available AND not already used in cart
  const availableSerials = cachedSerials.filter((serial) => {
    const serialValue = serial.serialNumberValue || serial.serialNumber
    const isAvailable = serial.trangThai === 'AVAILABLE'
    const isNotUsedInCart = !usedSerialNumbers.value.has(serialValue)
    return isAvailable && isNotUsedInCart
  })

  const availableCount = availableSerials.length

  console.log(`🔍 [INVENTORY DEBUG] Variant ${variant.id}: ${availableCount} available (${cachedSerials.length} total, ${usedSerialNumbers.value.size} used in cart)`)

  return availableCount
}

// Simple function to load serial numbers for a specific variant (no cache invalidation)
const loadSerialNumbersForVariant = async (variantId) => {
  try {
    console.log(`📦 [SIMPLE LOAD] Loading serials for variant ${variantId}...`)
    const serialNumbers = await serialNumberApi.getSerialNumbersByVariant(variantId)
    variantSerialNumbers.value.set(variantId, serialNumbers || [])
    console.log(`📦 [SIMPLE LOAD] Variant ${variantId}: Loaded ${serialNumbers?.length || 0} serials`)
  } catch (error) {
    console.error(`❌ [SIMPLE LOAD] Error loading serials for variant ${variantId}:`, error)
  }
}

const getVariantPrice = (variant) => {
  // Check for real-time price updates first
  const latestPrice = getLatestPriceForVariant(variant.id)
  if (latestPrice !== null) {
    return latestPrice
  }

  // Fall back to cached price data
  if (variant.giaKhuyenMai && variant.giaKhuyenMai < variant.giaBan) {
    return variant.giaKhuyenMai
  }
  return variant.giaBan
}

const getVariantDisplayName = (variant) => {
  const parts = []
  if (variant.mauSac) parts.push(variant.mauSac.moTaMauSac)
  if (variant.cpu) parts.push(variant.cpu.moTaCpu)
  if (variant.ram) parts.push(variant.ram.moTaRam)
  if (variant.gpu) parts.push(variant.gpu.moTaGpu)
  // Storage field reference (boNho)
  if (variant.boNho) parts.push(variant.boNho.moTaBoNho)
  return parts.length > 0 ? parts.join(' - ') : 'Phiên bản cơ bản'
}



const selectSerialNumbers = async (variant) => {
  selectedVariantForSerial.value = variant

  // Load fresh serial numbers for this variant to get current availability
  try {
    const serialNumbers = await serialNumberApi.getSerialNumbersByVariant(variant.id)

    // Filter out serials that are available AND not already used in cart
    availableSerialNumbers.value = serialNumbers.filter(serial => {
      const serialValue = serial.serialNumberValue || serial.serialNumber
      return serial.trangThai === 'AVAILABLE' && !usedSerialNumbers.value.has(serialValue)
    })
    selectedSerialNumbers.value = []
    serialFilterQuery.value = '' // Clear search filter
    serialDialogVisible.value = true
  } catch (error) {
    console.error('Error loading serial numbers:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải danh sách serial numbers',
      life: 3000
    })
  }
}

const confirmSerialSelection = async () => {
  if (!selectedSerialNumbers.value.length || !selectedVariantForSerial.value) {
    return
  }

  const selectedCount = selectedSerialNumbers.value.length
  const variantId = selectedVariantForSerial.value.id

  console.log(`🛒 [CART ADD DEBUG] Starting cart addition for variant ${variantId}:`, {
    selectedCount,
    selectedSerials: selectedSerialNumbers.value.map(s => s.serialNumberValue || s.serialNumber),
    currentUsedSerials: Array.from(usedSerialNumbers.value)
  })

  try {
    // Add each selected serial number as a separate variant directly to the cart
    for (const [index, serialNumber] of selectedSerialNumbers.value.entries()) {
      const serialValue = serialNumber.serialNumberValue || serialNumber.serialNumber

      console.log(`🛒 [CART ADD DEBUG] Adding serial ${index + 1}/${selectedCount}: ${serialValue}`)

      // Create a variant copy with the specific serial number
      const variantWithSerial = {
        ...selectedVariantForSerial.value,
        serialNumber: serialValue,
        serialNumberId: serialNumber.id
      }

      // Emit variant-selected event to add to cart immediately
      emit('variant-selected', {
        sanPhamChiTiet: variantWithSerial,
        soLuong: 1, // Each variant is individual
        donGia: getVariantPrice(variantWithSerial),
        thanhTien: getVariantPrice(variantWithSerial),
        groupInfo: {
          displayName: getVariantDisplayName(variantWithSerial),
          isFromGroup: false
        }
      })

      // Track this serial number as used to update available count
      usedSerialNumbers.value.add(serialValue)
      console.log(`🛒 [CART ADD DEBUG] Added ${serialValue} to used serials. New count: ${usedSerialNumbers.value.size}`)

      // Set flag to prevent sync override for a short period
      preventSyncOverride.value = true
      setTimeout(() => {
        preventSyncOverride.value = false
      }, 200)
    }

    // Close the serial selection dialog
    serialDialogVisible.value = false
    selectedSerialNumbers.value = []
    selectedVariantForSerial.value = null

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã thêm ${selectedCount} sản phẩm vào giỏ hàng`,
      life: 3000
    })

  } catch (error) {
    console.error('Error adding variants to cart:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể thêm sản phẩm vào giỏ hàng',
      life: 3000
    })
  }
}









const formatCurrency = (amount) => {
  if (!amount) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return 'N/A'
  try {
    const date = new Date(dateTime)
    return new Intl.DateTimeFormat('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).format(date)
  } catch (_error) {
    return 'N/A'
  }
}



const closeDialog = () => {
  emit('update:visible', false)
}

const onDialogHide = () => {
  // Don't clear usedSerialNumbers - keep cart tracking persistent
  serialDialogVisible.value = false
  // Reset filters
  clearAllFilters()
}

// Filter methods
const clearAllFilters = () => {
  filters.value = {
    cpu: null,
    ram: null,
    gpu: null,
    colors: null,
    storage: null,
    screen: null,
    priceRange: [...dynamicPricing.defaultPriceRange.value],
    searchQuery: ''
  }
}

// Simple cross-tab sync: refresh serial numbers when needed
const refreshSerialNumbersForCrossTabs = async () => {
  // Clear the cache to force fresh data from API
  variantSerialNumbers.value.clear()

  // Reload all serial numbers to get current availability
  await loadAllSerialNumbers()
}

// Refresh product data and prices to get latest information
const refreshProductData = async () => {
  try {
    console.log('ProductVariantDialog: Starting refresh of product data...')

    // Force refresh product store data to get latest prices
    await productStore.fetchProducts(true)

    // Clear any cached price updates to force fresh data
    // This ensures we get the most current prices from the database
    console.log('ProductVariantDialog: Cleared price update cache and fetched fresh product data')

    // Show success message to user
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã làm mới giá sản phẩm',
      life: 2000
    })

    console.log('ProductVariantDialog: Refreshed product data successfully')
  } catch (error) {
    console.error('Error refreshing product data:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể làm mới giá sản phẩm',
      life: 3000
    })
  }
}

// Sync used serial numbers with current cart items (legacy method for compatibility)
const syncUsedSerialNumbersWithCart = () => {
  // Request parent to send current cart data for serial number tracking
  emit('request-cart-sync')

  // Refresh serial numbers to get current cross-tab state
  refreshSerialNumbersForCrossTabs()
}

// Simplified method to receive cart data from parent - NO CACHE INVALIDATION
const updateUsedSerialNumbers = (cartItems = []) => {
  console.log(`🔄 [SIMPLE SYNC] updateUsedSerialNumbers called with ${cartItems?.length || 0} items`)

  // Skip update if we're in the middle of an immediate update to prevent race conditions
  if (preventSyncOverride.value) {
    console.log(`⏸️ [SIMPLE SYNC] Skipping update due to preventSyncOverride flag`)
    return
  }

  // Simply update tracking to match current cart items
  usedSerialNumbers.value.clear()

  if (Array.isArray(cartItems)) {
    cartItems.forEach((item, index) => {
      // Check for serial number in the correct location: item.sanPhamChiTiet.serialNumber
      if (item.sanPhamChiTiet?.serialNumber) {
        usedSerialNumbers.value.add(item.sanPhamChiTiet.serialNumber)
        console.log(`📦 [SIMPLE SYNC] Item ${index}: Added serial ${item.sanPhamChiTiet.serialNumber} to used list`)
      }
    })
  }

  console.log(`🔄 [SIMPLE SYNC] Updated used serials: ${usedSerialNumbers.value.size} items in cart`)

  // NO CACHE INVALIDATION - just let WebSocket handle real-time updates
  // The backend will send WebSocket messages when inventory changes
}

// Expose method for parent to call
defineExpose({
  updateUsedSerialNumbers
})

// Watch for dialog visibility to load data
watch(() => props.visible, async (isVisible) => {
  if (isVisible) {
    // Sync used serial numbers with current cart items when dialog opens
    syncUsedSerialNumbersWithCart()

    // Load all products if not already loaded or refresh to get latest prices
    if (!products.value?.length) {
      loading.value = true
      try {
        await productStore.fetchProducts()
      } catch (error) {
        console.error('Error loading products:', error)
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: 'Không thể tải danh sách sản phẩm',
          life: 3000
        })
      } finally {
        loading.value = false
      }
    } else {
      // Refresh product data to get latest prices
      await refreshProductData()
    }

    // Load serial numbers for all variants
    await loadAllSerialNumbers()

    // Subscribe to price updates for all available variants
    const variantIds = availableVariants.value.map(variant => variant.id).filter(Boolean)
    if (variantIds.length > 0) {
      subscribeToPriceUpdates(variantIds)
    }
  }
}, { immediate: true })

// Watch for price updates to refresh variant display
watch(() => priceUpdates.value, (newUpdates) => {
  if (newUpdates && newUpdates.length > 0 && props.visible) {
    // Force reactivity update for price display
    // The getVariantPrice function will automatically use the latest prices
    console.log('ProductVariantDialog: Price updates received, display will refresh automatically')
  }
}, { deep: true })

// Simplified load serial numbers - just load what we need, when we need it
const loadAllSerialNumbers = async (forceRefresh = false) => {
  if (!products.value?.length) {
    console.log(`📦 [SIMPLE LOAD] No products available`)
    return
  }

  console.log(`📦 [SIMPLE LOAD] Loading serial numbers for all variants...`)

  try {
    for (const product of products.value) {
      if (product.sanPhamChiTiets && Array.isArray(product.sanPhamChiTiets)) {
        for (const variant of product.sanPhamChiTiets) {
          if (variant.active && (forceRefresh || !variantSerialNumbers.value.has(variant.id))) {
            await loadSerialNumbersForVariant(variant.id)
          }
        }
      }
    }

    console.log(`📦 [SIMPLE LOAD] Completed loading for ${variantSerialNumbers.value.size} variants`)
  } catch (error) {
    console.warn('❌ [SIMPLE LOAD] Error loading serial numbers:', error)
  }
}

// REMOVED: Complex cache invalidation system
// Now using simple WebSocket pub/sub for real-time updates

// Load attributes and products on component mount
onMounted(async () => {
  try {
    // Load attributes for filtering
    await attributeStore.fetchAllAttributes()

    // Load products if dialog is initially visible
    if (props.visible && !products.value?.length) {
      loading.value = true
      try {
        await productStore.fetchProducts()
        await loadAllSerialNumbers()
      } catch (error) {
        console.error('Error loading products:', error)
      } finally {
        loading.value = false
      }
    }
  } catch (error) {
    console.error('Error loading attributes:', error)
  }
})
</script>

<style scoped>
.product-variant-dialog {
  max-height: 90vh;
  overflow-y: auto;
}

.serial-selection-dialog {
  max-height: 80vh;
  overflow-y: auto;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .product-variant-dialog {
    margin: 1rem;
  }
}
</style>
