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
            :min="0"
            :max="50000000"
            :step="500000"
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

          <Column field="oCung.moTaOCung" header="Dung lượng" sortable style="min-width: 100px">
            <template #body="{ data }">
              <span>{{ (data.oCung || data.ocung)?.moTaOCung || 'N/A' }}</span>
            </template>
          </Column>

          <Column field="giaBan" header="Giá bán" sortable style="min-width: 120px">
            <template #body="{ data }">
              <div>
                <div v-if="data.giaKhuyenMai && data.giaKhuyenMai < data.giaBan" class="space-y-1">
                  <div class="text-red-500 font-semibold">{{ formatCurrency(data.giaKhuyenMai) }}</div>
                  <div class="text-xs text-surface-500 line-through">{{ formatCurrency(data.giaBan) }}</div>
                </div>
                <div v-else class="font-semibold text-primary">
                  {{ formatCurrency(data.giaBan) }}
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
          <Badge :value="availableSerialNumbers.length" severity="info" />
        </div>

        <div v-if="availableSerialNumbers.length === 0" class="text-center py-8 text-surface-500">
          <i class="pi pi-exclamation-triangle text-2xl mb-2"></i>
          <p>Không có serial number nào có sẵn cho phiên bản này</p>
        </div>

        <DataTable
          v-else
          v-model:selection="selectedSerialNumbers"
          :value="availableSerialNumbers"
          selectionMode="multiple"
          dataKey="id"
          :paginator="availableSerialNumbers.length > 10"
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

// Serial number selection dialog state
const serialDialogVisible = ref(false)
const selectedVariantForSerial = ref(null)
const availableSerialNumbers = ref([])
const selectedSerialNumbers = ref([])



// Filter state
const filters = ref({
  cpu: null,
  ram: null,
  gpu: null,
  colors: null,
  storage: null,
  screen: null,
  priceRange: [0, 50000000],
  searchQuery: ''
})

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

      return productName.includes(query) ||
             productCode.includes(query) ||
             variantSku.includes(query) ||
             variantCode.includes(query) ||
             cpu.includes(query) ||
             ram.includes(query) ||
             gpu.includes(query) ||
             color.includes(query) ||
             storage.includes(query) ||
             screen.includes(query)
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
      // Handle both oCung (camelCase) and ocung (lowercase) field names
      const storage = variant.oCung || variant.ocung
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

  return filtered
})

// Check if any filters are active
const hasActiveFilters = computed(() => {
  return filters.value.cpu !== null ||
         filters.value.ram !== null ||
         filters.value.gpu !== null ||
         filters.value.colors !== null ||
         filters.value.storage !== null ||
         filters.value.screen !== null ||
         (filters.value.priceRange[0] !== 0 || filters.value.priceRange[1] !== 50000000) ||
         (filters.value.searchQuery && filters.value.searchQuery.trim())
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

const getAvailableSerialCount = (variant) => {
  const cachedSerials = variantSerialNumbers.value.get(variant.id)
  if (!cachedSerials) return 0

  // Filter out serials that are available AND not already used in cart
  return cachedSerials.filter((serial) => {
    const serialValue = serial.serialNumberValue || serial.serialNumber
    return serial.trangThai === 'AVAILABLE' && !usedSerialNumbers.value.has(serialValue)
  }).length
}

const getVariantPrice = (variant) => {
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
  // Fix storage field reference - handle both oCung and ocung
  const storage = variant.oCung || variant.ocung
  if (storage) parts.push(storage.moTaOCung)
  return parts.length > 0 ? parts.join(' - ') : 'Phiên bản cơ bản'
}



const selectSerialNumbers = async (variant) => {
  selectedVariantForSerial.value = variant

  // Load serial numbers for this variant
  try {
    const serialNumbers = await serialNumberApi.getSerialNumbersByVariant(variant.id)
    // Filter out serials that are available AND not already used in cart
    availableSerialNumbers.value = serialNumbers.filter(serial => {
      const serialValue = serial.serialNumberValue || serial.serialNumber
      return serial.trangThai === 'AVAILABLE' && !usedSerialNumbers.value.has(serialValue)
    })
    selectedSerialNumbers.value = []
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

  try {
    // Add each selected serial number as a separate variant directly to the cart
    for (const serialNumber of selectedSerialNumbers.value) {
      const serialValue = serialNumber.serialNumberValue || serialNumber.serialNumber

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
    priceRange: [0, 50000000],
    searchQuery: ''
  }
}

// Sync used serial numbers with current cart items (legacy method for compatibility)
const syncUsedSerialNumbersWithCart = () => {
  // This method is now deprecated in favor of backend reservations
  // Keep for compatibility but functionality moved to backend
  emit('request-cart-sync')
}

// Method to receive cart data from parent (legacy for compatibility)
const updateUsedSerialNumbers = () => {
  // This method is now deprecated in favor of backend reservations
  // Keep for compatibility but clear the tracking since backend handles it
  usedSerialNumbers.value.clear()
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

    // Load all products if not already loaded
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
    }

    // Load serial numbers for all variants
    await loadAllSerialNumbers()
  }
}, { immediate: true })

// Load serial numbers for all variants
const loadAllSerialNumbers = async () => {
  if (!products.value?.length) return

  try {
    for (const product of products.value) {
      if (product.sanPhamChiTiets && Array.isArray(product.sanPhamChiTiets)) {
        for (const variant of product.sanPhamChiTiets) {
          if (variant.active && !variantSerialNumbers.value.has(variant.id)) {
            try {
              const serialNumbers = await serialNumberApi.getSerialNumbersByVariant(variant.id)
              variantSerialNumbers.value.set(variant.id, serialNumbers || [])
            } catch (error) {
              console.warn(`Error loading serial numbers for variant ${variant.id}:`, error)
            }
          }
        }
      }
    }
  } catch (error) {
    console.warn('Error loading serial numbers:', error)
  }
}

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
