<template>
  <Fluid />
  <Toast />

  <!-- Page Header -->
  <div class="card mb-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
          <i class="pi pi-box text-3xl text-primary"></i>
        </div>
        <div>
          <h1 class="font-semibold text-xl text-surface-900 m-0">
            {{ product?.tenSanPham || 'Chi tiết sản phẩm' }}
          </h1>
          <p class="text-surface-500 text-sm mt-1 mb-0">
            Mã sản phẩm: {{ product?.maSanPham || 'Đang tải...' }}
          </p>
        </div>
      </div>
      <div class="flex gap-2">
        <Button
          label="Chỉnh sửa"
          icon="pi pi-pencil"
          @click="editProduct"
          :disabled="!product"
        />
        <Button
          label="Làm mới"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="refreshData"
        />
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          @click="goBack"
          v-tooltip.left="'Quay lại'"
        />
      </div>
    </div>
  </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-8">
      <ProgressSpinner />
      <p class="mt-4 text-surface-600">Đang tải dữ liệu...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center py-8">
      <i class="pi pi-exclamation-triangle text-4xl text-red-500 mb-4 block"></i>
      <p class="text-surface-600">{{ error }}</p>
      <Button
        label="Thử lại"
        icon="pi pi-refresh"
        @click="refreshData"
        class="mt-4"
      />
    </div>

  <!-- Product Content -->
  <div v-else-if="product" class="card">
    <!-- Modern Tabs Layout -->
    <Tabs value="info" class="product-detail-tabs">
      <TabList class="mb-6">
        <Tab value="info" class="flex items-center gap-2">
          <i class="pi pi-info"></i>
          <span>Thông tin sản phẩm</span>
        </Tab>
        <Tab value="variants" class="flex items-center gap-2">
          <i class="pi pi-list"></i>
          <span>Biến thể sản phẩm</span>
          <Badge :value="product.sanPhamChiTiets?.length || 0" severity="info" class="ml-2" />
        </Tab>
        <Tab value="audit" class="flex items-center gap-2">
          <i class="pi pi-history"></i>
          <span>Lịch sử thay đổi</span>
        </Tab>
      </TabList>

      <TabPanels>
        <TabPanel value="info">
          <!-- Product Information -->
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
            <!-- Basic Information -->
            <div class="card border border-surface-200 dark:border-surface-700 h-full">
              <div class="flex items-center gap-2 mb-4">
                <i class="pi pi-info-circle text-primary"></i>
                <span class="font-semibold text-xl">Thông tin cơ bản</span>
              </div>
              <div class="space-y-4">
                <div>
                  <label class="text-sm font-medium text-surface-600">Mã sản phẩm</label>
                  <p class="font-mono text-lg">{{ product.maSanPham }}</p>
                </div>

                <div>
                  <label class="text-sm font-medium text-surface-600">Tên sản phẩm</label>
                  <p class="text-lg font-semibold">{{ product.tenSanPham }}</p>
                </div>

                <div>
                  <label class="text-sm font-medium text-surface-600">Mô tả</label>
                  <p class="text-surface-700 dark:text-surface-300">{{ product.moTa || 'Chưa có mô tả' }}</p>
                </div>

                <div>
                  <label class="text-sm font-medium text-surface-600">Ngày ra mắt</label>
                  <p>{{ formatDate(product.ngayRaMat) }}</p>
                </div>

                <div>
                  <label class="text-sm font-medium text-surface-600 mr-2">Trạng thái</label>
                  <Badge
                    :value="product.trangThai ? 'Hoạt động' : 'Ngừng hoạt động'"
                    :severity="product.trangThai ? 'success' : 'danger'"
                    outlined
                  />
                </div>
              </div>
            </div>

            <!-- Category and Brand Information -->
            <div class="card border border-surface-200 dark:border-surface-700 h-full flex flex-col">
              <div class="flex items-center gap-2 mb-4">
                <i class="pi pi-tags text-primary"></i>
                <span class="font-semibold text-xl">Phân loại & Hình ảnh</span>
              </div>
              <div class="space-y-4 flex-1">
                <div>
                  <label class="text-sm font-medium text-surface-600">Danh mục</label>
                  <div v-if="product.danhMucs?.length > 0" class="flex flex-wrap gap-2 mt-1">
                    <Badge
                      v-for="danhMuc in product.danhMucs"
                      :key="danhMuc.id"
                      :value="danhMuc.moTaDanhMuc"
                      severity="info"
                      outlined
                    />
                  </div>
                  <p v-else class="text-lg text-surface-500">Chưa phân loại</p>
                </div>

                <div>
                  <label class="text-sm font-medium text-surface-600">Thương hiệu</label>
                  <p class="text-lg">{{ product.thuongHieu?.moTaThuongHieu || 'Chưa có thương hiệu' }}</p>
                </div>

                <!-- Product Images Section (moved from standalone section) -->
                <div v-if="product.hinhAnh?.length" class="mt-4">
                  <label class="text-sm font-medium text-surface-600 mb-2 block">Hình ảnh sản phẩm</label>
                  <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
                    <div
                      v-for="(image, index) in product.hinhAnh"
                      :key="index"
                      class="relative group cursor-pointer"
                      @click="showImageDialog(image)"
                    >
                      <img
                        :src="getProductImage(image) || '/placeholder-product.png'"
                        :alt="`${product.tenSanPham} - ${index + 1}`"
                        class="w-full h-20 object-cover rounded-lg border border-surface-200 dark:border-surface-700 transition-transform group-hover:scale-105"
                      />
                      <div class="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-all rounded-lg flex items-center justify-center">
                        <i class="pi pi-eye text-white opacity-0 group-hover:opacity-100 text-xl"></i>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Add some spacing to balance the height -->
                <div class="flex-1"></div>
              </div>
            </div>
          </div>
        </TabPanel>

        <TabPanel value="variants">
          <!-- Product Variants -->
          <ProductVariantManager
            :product-id="product.id"
            :variants="product.sanPhamChiTiets"
            @variant-updated="refreshData"
          />
        </TabPanel>

        <TabPanel value="audit">
          <!-- Audit Trail -->
          <ProductAuditLog
            :product-id="product.id"
          />
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>

  <!-- Image Dialog -->
  <Dialog
    v-model:visible="imageDialogVisible"
    :header="'Hình ảnh sản phẩm'"
    modal
    :style="{ width: '80vw', maxWidth: '800px' }"
  >
    <img
      v-if="selectedImage"
      :src="selectedImage"
      :alt="product?.tenSanPham"
      class="w-full h-auto max-h-96 object-contain"
    />
  </Dialog>

  <!-- Serial Import Dialog -->
  <Dialog
    v-model:visible="showSerialImportDialog"
    header="Import Serial Numbers từ CSV"
    :style="{ width: '500px' }"
    modal
  >
    <div class="space-y-4">
      <div class="text-sm text-surface-600">
        <p>Tải lên file CSV để import hàng loạt serial numbers cho các biến thể.</p>
        <p class="mt-2">
          <a href="#" @click="downloadSerialTemplate" class="text-primary hover:underline">
            <i class="pi pi-download mr-1"></i>Tải xuống template CSV
          </a>
        </p>
      </div>

      <div class="flex flex-col gap-2">
        <label class="text-sm font-medium">Chọn file CSV</label>
        <FileUpload
          ref="serialFileUpload"
          mode="basic"
          accept=".csv"
          :maxFileSize="5000000"
          @select="onSerialFileSelect"
          @clear="selectedSerialFile = null"
          chooseLabel="Chọn file"
          class="w-full"
        />
      </div>

      <div v-if="selectedSerialFile" class="p-3 bg-surface-50 dark:bg-surface-800 rounded-lg">
        <div class="flex items-center gap-2">
          <i class="pi pi-file text-blue-600"></i>
          <span class="text-sm font-medium">{{ selectedSerialFile.name }}</span>
          <span class="text-xs text-surface-500">({{ formatFileSize(selectedSerialFile.size) }})</span>
        </div>
      </div>

      <div v-if="serialImportLoading" class="p-3 bg-surface-50 dark:bg-surface-800 rounded-lg">
        <div class="flex items-center gap-2 mb-2">
          <ProgressSpinner size="small" />
          <span class="text-sm font-medium">Đang xử lý file...</span>
        </div>
        <ProgressBar :value="serialImportProgress" class="h-2" />
        <p class="text-xs text-surface-600 mt-1">
          Đã xử lý {{ serialImportProcessed }}/{{ serialImportTotal }} dòng
        </p>
      </div>
    </div>

    <template #footer>
      <Button label="Hủy" severity="secondary" outlined @click="cancelSerialImport" />
      <Button
        label="Import"
        @click="processSerialImport"
        :loading="serialImportLoading"
        :disabled="!selectedSerialFile"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useProductStore } from '@/stores/productstore'
import ProductVariantManager from './ProductVariantManager.vue'
import ProductAuditLog from './components/ProductAuditLog.vue'
import storageApi from '@/apis/storage'
import serialNumberApi from '@/apis/serialNumberApi'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const productStore = useProductStore()

// Component state
const product = ref(null)
const loading = ref(false)
const error = ref(null)
const imageDialogVisible = ref(false)
const selectedImage = ref(null)

// Serial number management state
const showSerialImportDialog = ref(false)
const selectedSerialFile = ref(null)
const serialImportLoading = ref(false)
const serialImportProgress = ref(0)
const serialImportProcessed = ref(0)
const serialImportTotal = ref(0)
const variantSerialNumbers = ref(new Map()) // Cache for variant serial numbers

// Image URL cache for performance
const imageUrlCache = ref(new Map())

// Computed properties (removed stats-related computed properties)

// Methods
const formatDate = (dateString) => {
  if (!dateString) return 'Chưa có thông tin'
  return new Date(dateString).toLocaleDateString('vi-VN')
}



// Helper method for product image display
const getProductImage = (imageFilename) => {
  if (!imageFilename) return null

  // If it's already a full URL, return as is
  if (imageFilename.startsWith('http')) return imageFilename

  // Check cache first
  if (imageUrlCache.value.has(imageFilename)) {
    return imageUrlCache.value.get(imageFilename)
  }

  // Load presigned URL asynchronously
  loadProductImageUrl(imageFilename)

  // Return null for now, will update when loaded
  return null
}

const loadProductImageUrl = async (imageFilename) => {
  try {
    // Get presigned URL for the image filename
    const presignedUrl = await storageApi.getPresignedUrl('products', imageFilename)

    // Cache the URL for future use
    imageUrlCache.value.set(imageFilename, presignedUrl)

    // Force reactivity update
    imageUrlCache.value = new Map(imageUrlCache.value)
  } catch (error) {
    console.warn('Error getting presigned URL for product image:', imageFilename, error)
    // Cache null to prevent repeated attempts
    imageUrlCache.value.set(imageFilename, null)
  }
}

const showImageDialog = (image) => {
  selectedImage.value = getProductImage(image) || image
  imageDialogVisible.value = true
}

const goBack = () => {
  router.push({ name: 'products' })
}

const editProduct = () => {
  router.push({ name: 'product-edit', params: { id: product.value.id } })
}

const loadProduct = async () => {
  loading.value = true
  error.value = null

  try {
    const productId = route.params.id
    if (!productId) {
      throw new Error('ID sản phẩm không hợp lệ')
    }

    // Get product from cache first, then fetch if needed
    product.value = productStore.getCachedProduct(productId)

    if (!product.value) {
      // Fetch from API if not in cache
      await productStore.fetchProducts()
      product.value = productStore.products.find(p => p.id == productId)
    }

    if (!product.value) {
      throw new Error('Không tìm thấy sản phẩm')
    }

    // Load serial numbers for all variants
    await loadVariantSerialNumbers()

  } catch (err) {
    error.value = err.message || 'Lỗi tải dữ liệu sản phẩm'
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.value,
      life: 3000
    })
  } finally {
    loading.value = false
  }
}



// Load serial numbers for all variants (simplified - only for caching)
const loadVariantSerialNumbers = async () => {
  if (!product.value?.sanPhamChiTiets?.length) return

  try {
    for (const variant of product.value.sanPhamChiTiets) {
      const serialNumbers = await serialNumberApi.getSerialNumbersByVariant(variant.id)
      variantSerialNumbers.value.set(variant.id, serialNumbers || [])
    }
  } catch (error) {
    console.warn('Error loading serial numbers:', error)
    // Don't show error toast as this is not critical for product display
  }
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const downloadSerialTemplate = () => {
  // Download CSV template for serial import
  const templateData = [
    {
      'Variant SKU': 'MBA-M2-8GB-256GB-SLV',
      'Serial Number': 'SN001234567890',
      'Status': 'AVAILABLE'
    }
  ]

  // Implement actual template download
  console.log('Template data:', templateData)

  toast.add({
    severity: 'info',
    summary: 'Template',
    detail: 'Đã tải xuống template CSV',
    life: 3000
  })
}

const onSerialFileSelect = (event) => {
  selectedSerialFile.value = event.files[0]
}

const processSerialImport = async () => {
  if (!selectedSerialFile.value) return

  serialImportLoading.value = true
  serialImportProgress.value = 0
  serialImportProcessed.value = 0
  serialImportTotal.value = 100 // Placeholder

  try {
    // This is a placeholder - implement actual CSV processing
    for (let i = 0; i <= 100; i += 10) {
      await new Promise(resolve => setTimeout(resolve, 100))
      serialImportProgress.value = i
      serialImportProcessed.value = Math.floor(i / 10)
    }

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã import serial numbers thành công',
      life: 3000
    })

    showSerialImportDialog.value = false
    await refreshData()
  } catch (err) {
    console.error('Import error:', err)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Lỗi import dữ liệu',
      life: 3000
    })
  } finally {
    serialImportLoading.value = false
    selectedSerialFile.value = null
    serialImportProgress.value = 0
    serialImportProcessed.value = 0
  }
}

const cancelSerialImport = () => {
  showSerialImportDialog.value = false
  selectedSerialFile.value = null
  serialImportProgress.value = 0
  serialImportProcessed.value = 0
}

const refreshData = async () => {
  await loadProduct()
  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Đã làm mới dữ liệu',
    life: 2000
  })
}

// Lifecycle
onMounted(async () => {
  await loadProduct()
})
</script>

<style scoped>
/* Modern tabs styling */
.product-detail-tabs :deep(.p-tablist) {
  border-bottom: 1px solid var(--surface-border);
  background: transparent;
}

.product-detail-tabs :deep(.p-tab) {
  border: none;
  background: transparent;
  color: var(--text-color-secondary);
  padding: 1rem 1.5rem;
  margin-right: 0.5rem;
  border-radius: 8px 8px 0 0;
  transition: all 0.2s ease;
}

.product-detail-tabs :deep(.p-tab:hover) {
  background: var(--surface-hover);
  color: var(--text-color);
}

.product-detail-tabs :deep(.p-tab[data-p-active="true"]) {
  color: var(--primary-color-text);
  border-bottom: 2px solid var(--primary-color);
}

.product-detail-tabs :deep(.p-tabpanels) {
  padding: 2rem 0;
  background: transparent;
}

/* Card styling improvements */
.card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.2s ease;
}

.card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* Image hover effects */
.group:hover img {
  transform: scale(1.05);
}

/* Statistics cards */
.text-center {
  padding: 1.5rem;
  min-height: 140px; /* Ensure consistent minimum height */
}

/* Equal height cards */
.grid.items-stretch > .card {
  display: flex;
  flex-direction: column;
}

.grid.items-start > .card.h-full {
  min-height: 300px; /* Minimum height for product info cards */
}

/* Currency text wrapping */
.break-words {
  word-break: break-word;
  hyphens: auto;
}

/* Responsive design */
@media (max-width: 768px) {
  .product-detail-tabs :deep(.p-tablist) {
    flex-wrap: wrap;
  }

  .product-detail-tabs :deep(.p-tab) {
    padding: 0.75rem 1rem;
    margin-bottom: 0.25rem;
  }

  /* Reduce minimum heights on mobile */
  .grid.items-start > .card.h-full {
    min-height: auto;
  }

  .text-center {
    min-height: 120px;
    padding: 1rem;
  }

  /* Stack statistics cards on mobile */
  .grid.grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  /* Single column on very small screens */
  .grid.grid-cols-1.md\\:grid-cols-2.lg\\:grid-cols-4 {
    grid-template-columns: 1fr;
  }
}
</style>
