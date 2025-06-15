<template>
  <div class="variant-manager">
    <!-- Header with Actions -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h3 class="text-xl font-semibold">Quản lý biến thể sản phẩm</h3>
      </div>
      <div class="flex gap-2">
        <Button label="Thêm biến thể" icon="pi pi-plus" @click="openVariantDialog()" />
        <Button
          label="Làm mới"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="refreshVariants"
        />
      </div>
    </div>

    <!-- Filter Section -->
    <div class="card mb-6">
      <div class="font-semibold text-xl mb-4">Bộ lọc biến thể</div>

      <Button
        type="button"
        icon="pi pi-filter-slash"
        label="Xóa toàn bộ bộ lọc"
        outlined
        class="mb-4"
        @click="clearAllFilters()"
      />

      <div class="mb-6 grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 border p-4 rounded-lg">
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
          <label class="block mb-2 font-medium">Bộ nhớ</label>
          <InputGroup>
            <Select
              v-model="filters.storage"
              :options="storages"
              optionLabel="moTaBoNho"
              optionValue="id"
              placeholder="Chọn bộ nhớ"
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
      </div>
    </div>

    <!-- Variants DataTable -->
    <DataTable
      v-model:selection="selectedVariants"
      :value="sortedFilteredVariants"
      :loading="loading"
      paginator
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20]"
      dataKey="id"
      selectionMode="multiple"
      class="p-datatable-sm"
      showGridlines
      v-bind="getDataTableSortProps()"
      @sort="onSort"
    >
      <template #header>
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-2">
            <span class="text-lg font-semibold">Danh sách biến thể</span>
            <Badge :value="sortedFilteredVariants?.length || 0" severity="info" />
          </div>

          <div class="flex gap-2">
            <Button
              v-if="selectedVariants.length > 0"
              label="Thao tác hàng loạt"
              icon="pi pi-cog"
              severity="secondary"
              @click="showBatchDialog = true"
            />
            <InputGroup>
              <InputText
                v-model="globalFilter"
                placeholder="Tìm kiếm biến thể..."
                @input="onGlobalFilter"
              />
              <Button
                v-if="globalFilter"
                icon="pi pi-times"
                severity="secondary"
                text
                @click="globalFilter = ''"
              />
            </InputGroup>
          </div>
        </div>
      </template>

      <template #empty>
        <div class="text-center py-8">
          <i class="pi pi-inbox text-4xl text-surface-400 mb-4 block"></i>
          <p class="text-surface-600">Chưa có biến thể nào</p>
          <Button
            label="Thêm biến thể đầu tiên"
            icon="pi pi-plus"
            @click="openVariantDialog()"
            class="mt-4"
          />
        </div>
      </template>

      <template #loading>
        <div class="text-center py-8">
          <ProgressSpinner />
          <p class="mt-4 text-surface-600">Đang tải dữ liệu...</p>
        </div>
      </template>

      <Column selectionMode="multiple" headerStyle="width: 3rem"></Column>

      <Column field="sku" header="SKU" sortable>
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <span v-if="data.sku" class="font-mono text-sm bg-surface-100 px-2 py-1 rounded">
              {{ data.sku }}
            </span>
            <span v-else class="text-surface-400 text-sm italic"> Auto-generated </span>
            <i
              v-if="!data.sku"
              class="pi pi-info-circle text-surface-400"
              v-tooltip="'SKU sẽ được tự động tạo khi lưu'"
            ></i>
          </div>
        </template>
      </Column>

      <Column field="mauSac.moTaMauSac" header="Màu sắc" sortable>
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

      <Column field="cpu.moTaCpu" header="CPU" sortable>
        <template #body="{ data }">
          <span>{{ data.cpu?.moTaCpu || 'N/A' }}</span>
        </template>
      </Column>

      <Column field="ram.moTaRam" header="RAM" sortable>
        <template #body="{ data }">
          <span>{{ data.ram?.moTaRam || 'N/A' }}</span>
        </template>
      </Column>

      <Column field="gpu.moTaGpu" header="GPU" sortable>
        <template #body="{ data }">
          <span>{{ data.gpu?.moTaGpu || 'N/A' }}</span>
        </template>
      </Column>

      <Column field="boNho.moTaBoNho" header="Storage" sortable>
        <template #body="{ data }">
          <span>{{ (data.boNho?.moTaBoNho || data.bonho?.moTaBoNho || data.oCung?.moTaOCung || data.ocung?.moTaOCung) || 'N/A' }}</span>
        </template>
      </Column>

      <Column field="manHinh.moTaManHinh" header="Screen" sortable>
        <template #body="{ data }">
          <span>{{ data.manHinh?.moTaManHinh || 'N/A' }}</span>
        </template>
      </Column>

      <Column field="giaBan" header="Giá bán" sortable>
        <template #body="{ data }">
          <span class="font-semibold">{{ formatCurrency(data.giaBan) }}</span>
        </template>
      </Column>

      <Column header="Tồn kho" sortable>
        <template #body="{ data }">
          <div class="text-center">
            <div class="font-semibold text-lg">{{ getVariantInventory(data) }}</div>
            <div class="text-xs text-surface-500">
              <span class="text-green-600">{{ getAvailableCount(data) }} có sẵn</span>
              <span v-if="getReservedCount(data) > 0" class="text-orange-600 ml-1">
                • {{ getReservedCount(data) }} đặt trước
              </span>
            </div>
          </div>
        </template>
      </Column>

      <Column field="trangThai" header="Trạng thái" sortable>
        <template #body="{ data }">
          <Badge
            :value="data.trangThai ? 'Hoạt động' : 'Không hoạt động'"
            :severity="data.trangThai ? 'success' : 'danger'"
          />
        </template>
      </Column>

      <Column field="ngayTao" header="Ngày tạo" sortable>
        <template #body="{ data }">
          {{ formatDateTime(data.ngayTao) }}
        </template>
      </Column>

      <Column field="ngayCapNhat" header="Ngày cập nhật" sortable>
        <template #body="{ data }">
          {{ formatDateTime(data.ngayCapNhat) }}
        </template>
      </Column>

      <Column header="Thao tác" style="width: 15rem">
        <template #body="{ data }">
          <div class="flex gap-1">
            <Button
              icon="pi pi-pencil"
              severity="warning"
              text
              rounded
              @click="openVariantDialog(data)"
              v-tooltip="'Chỉnh sửa'"
            />
            <Button
              icon="pi pi-cog"
              severity="info"
              text
              rounded
              @click="openStatusDialog(data)"
              v-tooltip="'Thay đổi trạng thái'"
            />
            <Button
              icon="pi pi-barcode"
              severity="secondary"
              text
              rounded
              @click="manageSerialNumbers(data)"
              v-tooltip="'Quản lý serial numbers'"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              @click="confirmDeleteVariant(data)"
              v-tooltip="'Xóa'"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Variant Form Dialog -->
    <Dialog
      v-model:visible="variantDialogVisible"
      :header="editingVariant ? 'Chỉnh sửa biến thể' : 'Thêm biến thể mới'"
      modal
      :style="{ width: '800px' }"
    >
      <div class="space-y-4">
        <!-- SKU Information -->
        <div class="p-3 bg-surface-50 dark:bg-surface-800 rounded-lg">
          <div class="flex items-center gap-2 mb-2">
            <i class="pi pi-info-circle text-primary"></i>
            <span class="font-medium">Thông tin SKU</span>
          </div>
          <p class="text-sm text-surface-600 mb-2">
            SKU sẽ được tự động tạo dựa trên các thuộc tính của biến thể.
          </p>
          <div class="flex items-center gap-2">
            <span class="text-sm font-medium">Preview:</span>
            <span class="font-mono text-sm bg-surface-100 dark:bg-surface-700 px-2 py-1 rounded">
              {{ skuPreview }}
            </span>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Color -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">Màu sắc *</label>
            <Select
              v-model="variantForm.mauSac"
              :options="colors"
              optionLabel="moTaMauSac"
              placeholder="Chọn màu sắc"
              :class="{ 'p-invalid': errors.mauSac }"
            />
            <small v-if="errors.mauSac" class="p-error">{{ errors.mauSac }}</small>
          </div>

          <!-- CPU -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">CPU</label>
            <Select
              v-model="variantForm.cpu"
              :options="cpus"
              optionLabel="moTaCpu"
              placeholder="Chọn CPU"
            />
          </div>

          <!-- RAM -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">RAM</label>
            <Select
              v-model="variantForm.ram"
              :options="rams"
              optionLabel="moTaRam"
              placeholder="Chọn RAM"
            />
          </div>

          <!-- Storage -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">Bộ nhớ</label>
            <Select
              v-model="variantForm.boNho"
              :options="storages"
              optionLabel="moTaBoNho"
              placeholder="Chọn bộ nhớ"
            />
          </div>

          <!-- GPU -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">GPU</label>
            <Select
              v-model="variantForm.gpu"
              :options="gpus"
              optionLabel="moTaGpu"
              placeholder="Chọn GPU"
            />
          </div>

          <!-- Screen -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">Màn hình</label>
            <Select
              v-model="variantForm.manHinh"
              :options="screens"
              optionLabel="moTaManHinh"
              placeholder="Chọn màn hình"
            />
          </div>

          <!-- Price -->
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">Giá bán *</label>
            <InputNumber
              v-model="variantForm.giaBan"
              mode="currency"
              currency="VND"
              locale="vi-VN"
              placeholder="Nhập giá bán"
              :class="{ 'p-invalid': errors.giaBan }"
            />
            <small v-if="errors.giaBan" class="p-error">{{ errors.giaBan }}</small>
          </div>

        </div>

        <!-- Image Upload Section -->
        <div class="flex flex-col gap-2 mt-4">
          <label class="text-sm font-medium">Hình ảnh biến thể</label>
          <div class="flex items-center gap-4">
            <!-- Image Preview -->
            <div class="relative">
              <div
                class="w-20 h-20 rounded-lg overflow-hidden border-2 transition-all duration-200"
                :class="variantImagePreview
                  ? 'border-primary-200 shadow-sm'
                  : 'border-dashed border-surface-300 bg-surface-50'"
              >
                <img
                  v-if="variantImagePreview"
                  :src="variantImagePreview"
                  alt="Variant image preview"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center">
                  <i class="pi pi-image text-2xl text-surface-400"></i>
                </div>
              </div>

              <!-- Remove button -->
              <Button
                v-if="variantImagePreview"
                icon="pi pi-times"
                severity="danger"
                text
                rounded
                size="small"
                class="absolute -top-2 -right-2"
                @click="removeVariantImage"
              />
            </div>

            <!-- Upload Controls -->
            <div class="flex flex-col gap-2">
              <input
                ref="variantImageInput"
                type="file"
                accept="image/*"
                style="display: none"
                @change="onVariantImageSelect"
              />
              <Button
                :label="variantImagePreview ? 'Thay đổi ảnh' : 'Tải ảnh lên'"
                :icon="variantImagePreview ? 'pi pi-refresh' : 'pi pi-upload'"
                severity="secondary"
                outlined
                size="small"
                @click="$refs.variantImageInput?.click()"
                :loading="uploadingVariantImage"
              />
              <small class="text-surface-500">
                Định dạng: JPG, PNG. Tối đa 10MB
              </small>
            </div>
          </div>
        </div>

        <!-- Attributes validation error -->
        <div v-if="errors.attributes" class="col-span-2">
          <small class="p-error">{{ errors.attributes }}</small>
        </div>
      </div>

      <!-- Status -->
      <div class="flex flex-col gap-2 mt-4">
        <label class="text-sm font-medium">Trạng thái</label>
        <Select
          v-model="variantForm.trangThai"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Chọn trạng thái"
        />
      </div>

      <template #footer>
        <Button label="Hủy" severity="secondary" outlined @click="variantDialogVisible = false" />
        <Button
          :label="editingVariant ? 'Cập nhật' : 'Thêm'"
          @click="saveVariant"
          :loading="saving"
        />
      </template>
    </Dialog>

    <!-- Status Change Dialog -->
    <Dialog
      v-model:visible="statusDialogVisible"
      header="Thay đổi trạng thái biến thể"
      modal
      :style="{ width: '400px' }"
    >
      <div class="space-y-4">
        <p
          >Thay đổi trạng thái cho biến thể:
          <strong>{{ getVariantDisplayName(selectedVariantForStatus) }}</strong></p
        >

        <div class="flex flex-col gap-2">
          <label class="text-sm font-medium">Trạng thái mới</label>
          <Select
            v-model="newStatus"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Chọn trạng thái"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label class="text-sm font-medium">Lý do thay đổi</label>
          <Textarea v-model="statusReason" placeholder="Nhập lý do thay đổi..." rows="3" />
        </div>
      </div>

      <template #footer>
        <Button label="Hủy" severity="secondary" outlined @click="statusDialogVisible = false" />
        <Button label="Cập nhật" @click="updateVariantStatus" :loading="updatingStatus" />
      </template>
    </Dialog>

    <!-- Batch Actions Dialog -->
    <Dialog
      v-model:visible="showBatchDialog"
      header="Thao tác hàng loạt"
      modal
      :style="{ width: '450px' }"
    >
      <div class="space-y-4">
        <p>Đã chọn {{ selectedVariants.length }} biến thể</p>

        <div class="flex flex-col gap-2">
          <label class="text-sm font-medium">Thay đổi trạng thái</label>
          <Select
            v-model="batchStatus"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Chọn trạng thái"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label class="text-sm font-medium">Lý do thay đổi</label>
          <Textarea v-model="batchReason" placeholder="Nhập lý do thay đổi..." rows="3" />
        </div>
      </div>

      <template #footer>
        <Button label="Hủy" severity="secondary" outlined @click="showBatchDialog = false" />
        <Button label="Áp dụng" @click="applyBatchActions" :loading="batchLoading" />
      </template>
    </Dialog>

    <!-- Serial Number Management Dialog -->
    <Dialog
      v-model:visible="showSerialManagementDialog"
      modal
      header="Quản lý Serial Numbers"
      :style="{ width: '60rem' }"
      :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
    >
      <div v-if="selectedVariantForSerial" class="space-y-4">
        <!-- Variant Information -->
        <div class="p-3 bg-surface-50 dark:bg-surface-800 rounded-lg">
          <div class="flex items-center gap-2 mb-2">
            <i class="pi pi-info-circle text-primary"></i>
            <span class="font-medium">Thông tin biến thể</span>
          </div>
          <p class="text-sm">
            <strong>Biến thể:</strong> {{ getVariantDisplayName(selectedVariantForSerial) }}
          </p>
          <p class="text-sm">
            <strong>SKU:</strong> {{ selectedVariantForSerial.sku || 'Auto-generated' }}
          </p>
        </div>

        <!-- Add Serial Number -->
        <div class="space-y-2">
          <label class="font-semibold">Thêm Serial Number:</label>
          <div class="flex gap-2">
            <InputText
              v-model="newSerialNumber"
              placeholder="Nhập serial number (có thể nhập nhiều, cách nhau bằng dấu phẩy)"
              class="flex-1"
              @keyup.enter="addSerialNumber"
            />
            <Button
              label="Thêm"
              icon="pi pi-plus"
              @click="addSerialNumber"
              :disabled="!newSerialNumber.trim()"
            />
          </div>
          <small class="text-surface-500">
            Có thể nhập nhiều serial number cùng lúc, cách nhau bằng dấu phẩy (,) hoặc dấu chấm phẩy (;)
          </small>
        </div>

        <!-- Serial Numbers List -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="font-semibold">Danh sách Serial Numbers:</label>
            <Badge :value="variantSerialNumbersForDialog.length" severity="info" />
          </div>

          <DataTable
            :value="variantSerialNumbersForDialog"
            :paginator="variantSerialNumbersForDialog.length > 10"
            :rows="10"
            class="p-datatable-sm"
            showGridlines
          >
            <template #empty>
              <div class="text-center py-4">
                <i class="pi pi-inbox text-4xl text-surface-400 mb-2"></i>
                <p class="text-surface-500">Chưa có serial number nào</p>
              </div>
            </template>

            <Column field="serialNumberValue" header="Serial Number" sortable>
              <template #body="{ data }">
                <span class="font-mono">{{ data.serialNumberValue || data.serialNumber }}</span>
                <Badge v-if="data.isNew" value="Mới" severity="success" class="ml-2" />
              </template>
            </Column>

            <Column field="trangThai" header="Trạng thái" sortable>
              <template #body="{ data }">
                <Badge
                  :value="data.trangThaiDisplay || data.trangThai"
                  :severity="data.trangThai === 'AVAILABLE' ? 'success' :
                           data.trangThai === 'RESERVED' ? 'warning' :
                           data.trangThai === 'SOLD' ? 'info' : 'danger'"
                />
              </template>
            </Column>

            <Column header="Thao tác" style="width: 8rem">
              <template #body="{ index }">
                <div class="flex gap-1">
                  <Button
                    icon="pi pi-trash"
                    severity="danger"
                    text
                    rounded
                    size="small"
                    @click="removeSerialNumber(index)"
                    v-tooltip="'Xóa'"
                  />
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>

      <template #footer>
        <Button
          label="Hủy"
          severity="secondary"
          outlined
          @click="closeSerialManagementDialog"
        />
        <Button
          label="Lưu"
          icon="pi pi-check"
          @click="saveSerialNumbers"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useAttributeStore } from '@/stores/attributestore'
import { useProductStore } from '@/stores/productstore'
import { useDynamicPricing } from '@/composables/useDynamicPricing'
import { useDataTableSorting } from '@/composables/useDataTableSorting'
import { debounce } from 'lodash-es'
import serialNumberApi from '@/apis/serialNumberApi'
import storageApi from '@/apis/storage'

const props = defineProps({
  productId: {
    type: [String, Number],
    required: true,
  },
  variants: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['variant-updated'])

const toast = useToast()
const confirm = useConfirm()
const attributeStore = useAttributeStore()
const productStore = useProductStore()
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

// Component state
const loading = ref(false)
const saving = ref(false)
const updatingStatus = ref(false)
const batchLoading = ref(false)
const globalFilter = ref('')
const selectedVariants = ref([])
const variantSerialNumbers = ref(new Map()) // Cache for variant serial numbers

// Filter state
const filters = ref({
  cpu: null,
  ram: null,
  gpu: null,
  colors: null,
  storage: null,
  screen: null,
  priceRange: [0, 50000000] // Will be updated dynamically
})

// Watch for price range changes and update filter default
watch(() => dynamicPricing.defaultPriceRange.value, (newRange) => {
  // Only update if current range is at the old default
  if (filters.value.priceRange[0] === 0 && filters.value.priceRange[1] === 50000000) {
    filters.value.priceRange = [...newRange]
  }
}, { immediate: true })

// Product information for SKU generation
const productInfo = ref(null)

// Dialog states
const variantDialogVisible = ref(false)
const statusDialogVisible = ref(false)
const showBatchDialog = ref(false)

// Form states
const editingVariant = ref(null)
const selectedVariantForStatus = ref(null)
const newStatus = ref(null)
const statusReason = ref('')
const batchStatus = ref(null)
const batchReason = ref('')
const errors = ref({})

const variantForm = ref({
  mauSac: null,
  cpu: null,
  ram: null,
  boNho: null,
  gpu: null,
  manHinh: null,
  giaBan: null,
  giaKhuyenMai: null,
  trangThai: true,
})

// Computed properties
const colors = computed(() => attributeStore.colors)
const cpus = computed(() => attributeStore.cpu)
const rams = computed(() => attributeStore.ram)
const storages = computed(() => attributeStore.storage)
const gpus = computed(() => attributeStore.gpu)
const screens = computed(() => attributeStore.screen)

const statusOptions = computed(() => [
  { label: 'Hoạt động', value: true, severity: 'success' },
  { label: 'Không hoạt động', value: false, severity: 'danger' },
])

// Filtered variants computed property
const filteredVariants = computed(() => {
  let filtered = props.variants || []

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
      const price = variant.giaBan
      return price >= minPrice && price <= maxPrice
    })
  }

  // Apply global filter if present
  if (globalFilter.value) {
    const searchTerm = globalFilter.value.toLowerCase()
    filtered = filtered.filter(variant => {
      return (
        variant.sku?.toLowerCase().includes(searchTerm) ||
        variant.mauSac?.moTaMauSac?.toLowerCase().includes(searchTerm) ||
        variant.cpu?.moTaCpu?.toLowerCase().includes(searchTerm) ||
        variant.ram?.moTaRam?.toLowerCase().includes(searchTerm) ||
        variant.gpu?.moTaGpu?.toLowerCase().includes(searchTerm) ||
        (variant.boNho?.moTaBoNho || variant.bonho?.moTaBoNho || variant.oCung?.moTaOCung || variant.ocung?.moTaOCung)?.toLowerCase().includes(searchTerm) ||
        variant.manHinh?.moTaManHinh?.toLowerCase().includes(searchTerm)
      )
    })
  }

  return filtered
})

// Apply auto-sorting to filtered variants
const sortedFilteredVariants = computed(() => {
  return applySorting(filteredVariants.value)
})

// Computed property for SKU preview
const skuPreview = computed(() => {
  if (!variantForm.value.mauSac && !variantForm.value.cpu && !variantForm.value.ram) {
    return 'SKU sẽ được tự động tạo'
  }

  try {
    return generateBaseSku(variantForm.value)
  } catch {
    return 'SKU sẽ được tự động tạo'
  }
})

// Methods
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount)
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

const getColorValue = (colorName) => {
  const colorMap = {
    Đỏ: '#ef4444',
    Xanh: '#3b82f6',
    Vàng: '#eab308',
    Đen: '#000000',
    Trắng: '#ffffff',
    Xám: '#6b7280',
  }
  return colorMap[colorName] || '#6b7280'
}

const onGlobalFilter = debounce(() => {
  // Global filter implementation
}, 300)

// Filter methods
const clearAllFilters = () => {
  filters.value = {
    cpu: null,
    ram: null,
    gpu: null,
    colors: null,
    storage: null,
    screen: null,
    priceRange: [...dynamicPricing.defaultPriceRange.value]
  }
  globalFilter.value = ''

  toast.add({
    severity: 'info',
    summary: 'Bộ lọc',
    detail: 'Đã xóa tất cả bộ lọc',
    life: 2000,
  })
}

// SKU Generation Logic (matching ProductForm.vue patterns)
const generateBaseSku = (variant) => {
  const productCode = productInfo.value?.maSanPham || 'SP'
  const parts = [productCode]

  // Add core attributes to SKU
  if (variant.cpu?.moTaCpu) {
    parts.push(variant.cpu.moTaCpu.replace(/\s+/g, '').toUpperCase().substring(0, 8))
  }
  if (variant.ram?.moTaRam) {
    parts.push(variant.ram.moTaRam.replace(/\s+/g, '').toUpperCase().substring(0, 6))
  }
  if (variant.mauSac?.moTaMauSac) {
    parts.push(variant.mauSac.moTaMauSac.replace(/\s+/g, '').toUpperCase().substring(0, 4))
  }
  // Add storage to SKU for better uniqueness
  if (variant.boNho?.moTaBoNho) {
    parts.push(variant.boNho.moTaBoNho.replace(/\s+/g, '').toUpperCase().substring(0, 6))
  }

  return parts.join('-')
}

const checkSkuExists = async (sku) => {
  try {
    // Check against existing products in the store
    const allProducts = productStore.products || []
    for (const product of allProducts) {
      if (product.sanPhamChiTiets) {
        for (const variant of product.sanPhamChiTiets) {
          if (variant.sku === sku) {
            return true
          }
        }
      }
    }

    // Check against current variants
    if (props.variants) {
      for (const variant of props.variants) {
        if (variant.sku === sku) {
          return true
        }
      }
    }

    return false
  } catch (error) {
    console.warn('Error checking SKU existence:', error)
    return false
  }
}

const generateUniqueSku = async (baseSku) => {
  let candidateSku = baseSku
  let counter = 1

  // Check if base SKU is available
  if (!(await checkSkuExists(candidateSku))) {
    return candidateSku
  }

  // Generate numbered variants until we find a unique one
  while (counter <= 999) {
    candidateSku = `${baseSku}-${String(counter).padStart(3, '0')}`
    if (!(await checkSkuExists(candidateSku))) {
      return candidateSku
    }
    counter++
  }

  // If we can't find a unique SKU after 999 attempts, throw an error
  throw new Error(`Không thể tạo SKU duy nhất cho: ${baseSku}`)
}

const openVariantDialog = async (variant = null) => {
  editingVariant.value = variant
  if (variant) {
    // Deep copy for editing to avoid modifying original data
    // Handle both boNho (camelCase) and bonho (lowercase) field names from backend, with backward compatibility
    const storageField = variant.boNho || variant.bonho || variant.oCung || variant.ocung
    variantForm.value = {
      ...variant,
      mauSac: variant.mauSac ? { ...variant.mauSac } : null,
      cpu: variant.cpu ? { ...variant.cpu } : null,
      ram: variant.ram ? { ...variant.ram } : null,
      boNho: storageField ? { ...storageField } : null,
      gpu: variant.gpu ? { ...variant.gpu } : null,
      manHinh: variant.manHinh ? { ...variant.manHinh } : null,
    }

    // Load existing image preview if available
    if (variant.hinhAnh && variant.hinhAnh.length > 0) {
      try {
        const presignedUrl = await storageApi.getPresignedUrl('products', variant.hinhAnh[0])
        variantImagePreview.value = presignedUrl
      } catch (error) {
        console.warn('Could not load variant image preview:', error)
        variantImagePreview.value = null
      }
    } else {
      variantImagePreview.value = null
    }
  } else {
    resetVariantForm()
  }
  errors.value = {}
  variantDialogVisible.value = true
}

const resetVariantForm = () => {
  variantForm.value = {
    mauSac: null,
    cpu: null,
    ram: null,
    boNho: null,
    gpu: null,
    manHinh: null,
    giaBan: null,
    trangThai: true,
    hinhAnh: [],
    serialNumbers: []
  }
  // Reset image preview
  variantImagePreview.value = null
}

const validateVariantForm = () => {
  errors.value = {}

  // Color validation (required)
  if (!variantForm.value.mauSac) {
    errors.value.mauSac = 'Vui lòng chọn màu sắc'
  }

  // Price validation (required and must be positive)
  if (!variantForm.value.giaBan || variantForm.value.giaBan <= 0) {
    errors.value.giaBan = 'Giá bán phải lớn hơn 0'
  }



  // Validate that at least one technical attribute is selected (besides color which is required)
  const hasAttributes =
    variantForm.value.cpu ||
    variantForm.value.ram ||
    variantForm.value.gpu ||
    variantForm.value.boNho ||
    variantForm.value.manHinh

  if (!hasAttributes) {
    errors.value.attributes =
      'Vui lòng chọn ít nhất một thuộc tính kỹ thuật (CPU, RAM, GPU, Storage, hoặc Screen)'
  }

  return Object.keys(errors.value).length === 0
}

const saveVariant = async () => {
  if (!validateVariantForm()) {
    return
  }

  saving.value = true
  try {
    const variantData = {
      ...variantForm.value,
      sanPham: { id: props.productId },
    }

    // Log the variant data being sent for debugging
    console.log('Saving variant data:', JSON.stringify(variantData, null, 2))

    // Note: Backend issue - SanPhamChiTietController returns entity instead of DTO
    // This causes Jackson to serialize entity fields, potentially changing field names
    // Proper fix: Update backend controller to return SanPhamChiTietDto instead of SanPhamChiTiet

    // Generate SKU for new variants if not editing
    if (!editingVariant.value) {
      try {
        const baseSku = generateBaseSku(variantForm.value)
        const uniqueSku = await generateUniqueSku(baseSku)
        variantData.sku = uniqueSku

      } catch (skuError) {
        console.warn('SKU generation failed, backend will handle:', skuError)
        // Continue without SKU - backend will generate one
      }
    }

    if (editingVariant.value) {
      // Update existing variant
      await productStore.updateProductDetail(editingVariant.value.id, variantData)
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Cập nhật biến thể thành công',
        life: 3000,
      })
    } else {
      // Create new variant
      await productStore.createProductDetail(variantData)
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Thêm biến thể thành công',
        life: 3000,
      })
    }

    variantDialogVisible.value = false
    emit('variant-updated')
  } catch (error) {
    console.error('Error saving variant:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.response?.data?.message || error.message || 'Lỗi lưu biến thể',
      life: 3000,
    })
  } finally {
    saving.value = false
  }
}

const openStatusDialog = (variant) => {
  selectedVariantForStatus.value = variant
  newStatus.value = variant.trangThai
  statusReason.value = ''
  statusDialogVisible.value = true
}

const updateVariantStatus = async () => {
  if (!newStatus.value || !statusReason.value.trim()) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng chọn trạng thái và nhập lý do',
      life: 3000,
    })
    return
  }

  updatingStatus.value = true
  try {
    await productStore.updateProductDetailStatus(
      selectedVariantForStatus.value.id,
      newStatus.value,
      statusReason.value,
    )

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Cập nhật trạng thái thành công',
      life: 3000,
    })

    statusDialogVisible.value = false
    emit('variant-updated')
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Lỗi cập nhật trạng thái',
      life: 3000,
    })
  } finally {
    updatingStatus.value = false
  }
}

const confirmDeleteVariant = (variant) => {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa biến thể "${getVariantDisplayName(variant)}"?`,
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
    accept: () => deleteVariant(variant),
  })
}

const deleteVariant = async (variant) => {
  try {
    await productStore.deleteProductDetail(variant.id)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Xóa biến thể thành công',
      life: 3000,
    })
    emit('variant-updated')
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Lỗi xóa biến thể',
      life: 3000,
    })
  }
}

const applyBatchActions = async () => {
  if (!batchStatus.value || !batchReason.value.trim()) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng chọn trạng thái và nhập lý do',
      life: 3000,
    })
    return
  }

  batchLoading.value = true
  try {
    const variantIds = selectedVariants.value.map((v) => v.id)
    await productStore.updateMultipleProductDetailStatus(
      variantIds,
      batchStatus.value,
      batchReason.value,
    )

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã cập nhật ${selectedVariants.value.length} biến thể`,
      life: 3000,
    })

    showBatchDialog.value = false
    selectedVariants.value = []
    batchStatus.value = null
    batchReason.value = ''
    emit('variant-updated')
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

// Load product information for SKU generation
const loadProductInfo = async () => {
  if (!props.productId) return

  try {
    productInfo.value = await productStore.fetchProductById(props.productId)
  } catch (error) {
    console.warn('Error loading product info:', error)
    // Don't show error toast as this is not critical - SKU generation will use fallback
  }
}

// Load serial numbers for all variants
const loadVariantSerialNumbers = async () => {
  if (!props.variants?.length) return

  try {
    for (const variant of props.variants) {
      const serialNumbers = await serialNumberApi.getSerialNumbersByVariant(variant.id)
      variantSerialNumbers.value.set(variant.id, serialNumbers || [])
    }
  } catch (error) {
    console.warn('Error loading serial numbers:', error)
    // Don't show error toast as this is not critical for variant management
  }
}

// Inventory management methods
const getVariantInventory = (variant) => {
  const cachedSerials = variantSerialNumbers.value.get(variant.id)
  return cachedSerials?.length || 0
}

const getAvailableCount = (variant) => {
  const cachedSerials = variantSerialNumbers.value.get(variant.id)
  if (!cachedSerials) return 0
  return cachedSerials.filter((serial) => serial.trangThai === 'AVAILABLE').length
}

const getReservedCount = (variant) => {
  const cachedSerials = variantSerialNumbers.value.get(variant.id)
  if (!cachedSerials) return 0
  return cachedSerials.filter((serial) => serial.trangThai === 'RESERVED').length
}

// Serial number management state
const showSerialManagementDialog = ref(false)
const selectedVariantForSerial = ref(null)
const newSerialNumber = ref('')
const variantSerialNumbersForDialog = ref([])

// Image upload state for variant dialog
const variantImagePreview = ref(null)
const uploadingVariantImage = ref(false)
const variantImageInput = ref(null)



// Serial number changes tracking for backend persistence
const serialNumberChanges = ref({
  toCreate: [], // New serial numbers to create
  toUpdate: [], // Existing serial numbers to update
  toDelete: []  // Existing serial numbers to delete
})

const manageSerialNumbers = async (variant) => {
  selectedVariantForSerial.value = variant
  newSerialNumber.value = ''
  showSerialManagementDialog.value = true

  // Clear previous changes tracking
  serialNumberChanges.value = {
    toCreate: [],
    toUpdate: [],
    toDelete: []
  }

  // Use cached serial numbers
  const cachedSerials = variantSerialNumbers.value.get(variant.id)
  variantSerialNumbersForDialog.value = [...(cachedSerials || [])]

  console.log(`Opening serial management for variant ${variant.id} with ${variantSerialNumbersForDialog.value.length} serial numbers`)
}

const addSerialNumber = () => {
  if (!newSerialNumber.value.trim()) return

  // Split by comma and semicolon for batch input
  const serialNumbers = newSerialNumber.value
    .split(/[,;]/)
    .map(s => s.trim())
    .filter(s => s.length > 0)

  if (serialNumbers.length === 0) return

  const newSerials = []
  const duplicates = []

  // Process each serial number
  for (const serialNumber of serialNumbers) {
    // Check for duplicates in existing list
    const existsInCurrent = variantSerialNumbersForDialog.value.some(
      serial => (serial.serialNumberValue || serial.serialNumber) === serialNumber
    )

    // Check for duplicates in the batch being added
    const existsInBatch = newSerials.some(
      serial => (serial.serialNumberValue || serial.serialNumber) === serialNumber
    )

    if (existsInCurrent || existsInBatch) {
      duplicates.push(serialNumber)
    } else {
      newSerials.push({
        serialNumberValue: serialNumber,
        serialNumber: serialNumber, // For backward compatibility
        trangThai: 'AVAILABLE',
        trangThaiDisplay: 'Có sẵn',
        isNew: true
      })
    }
  }

  // Add valid serial numbers to the list
  if (newSerials.length > 0) {
    variantSerialNumbersForDialog.value.push(...newSerials)

    // Track for backend creation if variant has ID
    if (selectedVariantForSerial.value?.id) {
      newSerials.forEach(serial => {
        serialNumberChanges.value.toCreate.push({
          serialNumberValue: serial.serialNumberValue,
          sanPhamChiTietId: selectedVariantForSerial.value.id,
          trangThai: serial.trangThai
        })
      })
    }
  }

  // Clear input
  newSerialNumber.value = ''

  // Show feedback
  if (newSerials.length > 0) {
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã thêm ${newSerials.length} serial number${newSerials.length > 1 ? 's' : ''}`,
      life: 2000
    })
  }

  if (duplicates.length > 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: `${duplicates.length} serial number bị trùng: ${duplicates.join(', ')}`,
      life: 3000
    })
  }
}

const removeSerialNumber = (index) => {
  const serial = variantSerialNumbersForDialog.value[index]

  // Track for backend deletion if this is an existing serial number
  if (serial.id && selectedVariantForSerial.value?.id) {
    serialNumberChanges.value.toDelete.push({
      id: serial.id,
      serialNumberValue: serial.serialNumberValue || serial.serialNumber
    })
  }

  variantSerialNumbersForDialog.value.splice(index, 1)
  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Đã xóa serial number',
    life: 2000
  })
}

const closeSerialManagementDialog = () => {
  showSerialManagementDialog.value = false
  selectedVariantForSerial.value = null
  variantSerialNumbersForDialog.value = []
  newSerialNumber.value = ''
}

const saveSerialNumbers = async () => {
  if (selectedVariantForSerial.value) {
    try {
      // Persist changes to backend if variant exists
      const result = await persistSerialNumberChanges()

      // Update the cached serial numbers
      variantSerialNumbers.value.set(selectedVariantForSerial.value.id, [...variantSerialNumbersForDialog.value])

      // Show appropriate toast message
      toast.add({
        severity: result.success ? 'success' : 'warn',
        summary: result.success ? 'Thành công' : 'Cảnh báo',
        detail: result.message,
        life: 3000
      })

      // Close dialog
      closeSerialManagementDialog()

      // Emit update event to refresh parent component
      emit('variant-updated')
    } catch (error) {
      console.error('Error saving serial numbers:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Lỗi lưu serial numbers',
        life: 3000
      })
    }
  }
}

// Persist serial number changes to backend
const persistSerialNumberChanges = async () => {
  if (!selectedVariantForSerial.value?.id) {
    return { success: true, message: 'Serial numbers saved locally' }
  }

  let successCount = 0
  let errorCount = 0
  const errors = []

  try {
    // Process deletions first
    for (const deleteItem of serialNumberChanges.value.toDelete) {
      try {
        await serialNumberApi.deleteSerialNumber(deleteItem.id, 'Xóa từ quản lý biến thể')
        successCount++
        console.log(`Deleted serial number: ${deleteItem.serialNumberValue}`)
      } catch (error) {
        errorCount++
        const errorMessage = error.response?.status === 403
          ? `Không có quyền xóa serial number: ${deleteItem.serialNumberValue}`
          : `Lỗi xóa serial number ${deleteItem.serialNumberValue}: ${error.message}`
        errors.push(errorMessage)
        console.error('Delete error:', error)
      }
    }

    // Process creations
    for (const createItem of serialNumberChanges.value.toCreate) {
      try {
        const result = await serialNumberApi.createSerialNumber(createItem)
        successCount++
        console.log(`Created serial number: ${createItem.serialNumberValue}`, result)
      } catch (error) {
        errorCount++
        const errorMessage = error.response?.status === 409
          ? `Serial number đã tồn tại: ${createItem.serialNumberValue}`
          : `Lỗi tạo serial number ${createItem.serialNumberValue}: ${error.message}`
        errors.push(errorMessage)
        console.error('Create error:', error)
      }
    }

    // Determine result
    const totalOperations = serialNumberChanges.value.toDelete.length + serialNumberChanges.value.toCreate.length

    if (totalOperations === 0) {
      return { success: true, message: 'Không có thay đổi nào cần lưu' }
    } else if (errorCount === 0) {
      return { success: true, message: `Đã lưu thành công ${successCount} thay đổi` }
    } else if (successCount > 0) {
      return {
        success: false,
        message: `Lưu một phần: ${successCount} thành công, ${errorCount} lỗi. Chi tiết: ${errors.join('; ')}`
      }
    } else {
      return {
        success: false,
        message: `Lưu thất bại: ${errors.join('; ')}`
      }
    }
  } catch (error) {
    console.error('Unexpected error during persistence:', error)
    return {
      success: false,
      message: `Lỗi không mong muốn: ${error.message}`
    }
  }
}

const getVariantDisplayName = (variant) => {
  const attributes = []
  if (variant.cpu?.moTaCpu) attributes.push(variant.cpu.moTaCpu)
  if (variant.ram?.moTaRam) attributes.push(variant.ram.moTaRam)
  if (variant.gpu?.moTaGpu) attributes.push(variant.gpu.moTaGpu)
  if (variant.mauSac?.moTaMauSac) attributes.push(variant.mauSac.moTaMauSac)
  // Handle storage field (boNho)
  if (variant.boNho?.moTaBoNho) {
    attributes.push(variant.boNho.moTaBoNho)
  }
  if (variant.manHinh?.moTaManHinh) attributes.push(variant.manHinh.moTaManHinh)

  return attributes.length > 0 ? attributes.join(' • ') : 'Biến thể cơ bản'
}

const refreshVariants = async () => {
  loading.value = true
  try {
    // Refresh product store data for accurate SKU checking
    await productStore.fetchProducts(true)
    emit('variant-updated')
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã làm mới dữ liệu',
      life: 2000,
    })
  } catch (error) {
    console.error('Error refreshing data:', error)
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Làm mới dữ liệu không hoàn toàn thành công',
      life: 3000,
    })
  } finally {
    loading.value = false
  }
}

// Image upload methods for variant dialog
const onVariantImageSelect = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // Validate file type
  if (!file.type.startsWith('image/')) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Vui lòng chọn file hình ảnh',
      life: 3000
    })
    return
  }

  // Validate file size (10MB limit)
  if (file.size > 10 * 1024 * 1024) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Kích thước file không được vượt quá 10MB',
      life: 3000
    })
    return
  }

  uploadingVariantImage.value = true
  try {
    // Create immediate preview using FileReader
    const reader = new FileReader()
    reader.onload = (e) => {
      variantImagePreview.value = e.target.result
    }
    reader.readAsDataURL(file)

    // Upload to MinIO
    const uploadedFilenames = await storageApi.uploadFiles([file], 'products')

    if (uploadedFilenames && uploadedFilenames.length > 0) {
      // Initialize hinhAnh array if needed
      if (!variantForm.value.hinhAnh) {
        variantForm.value.hinhAnh = []
      }

      // Set as the primary image
      variantForm.value.hinhAnh = [uploadedFilenames[0]]

      // Get presigned URL for the uploaded image and update preview
      try {
        const presignedUrl = await storageApi.getPresignedUrl('products', uploadedFilenames[0])
        variantImagePreview.value = presignedUrl
      } catch (error) {
        console.warn('Could not get presigned URL for variant preview:', error)
        // Keep the FileReader preview
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Tải ảnh biến thể thành công',
        life: 3000
      })
    }
  } catch (error) {
    console.error('Error uploading variant image:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Lỗi tải ảnh lên',
      life: 3000
    })
  } finally {
    uploadingVariantImage.value = false
    // Clear the input
    event.target.value = ''
  }
}

const removeVariantImage = () => {
  variantImagePreview.value = null
  if (variantForm.value.hinhAnh) {
    variantForm.value.hinhAnh = []
  }

  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Đã xóa ảnh biến thể',
    life: 2000
  })
}

// Watch for variants changes to reload serial numbers
watch(() => props.variants, async (newVariants) => {
  if (newVariants?.length) {
    await loadVariantSerialNumbers()
  }
}, { immediate: true })

// Lifecycle
onMounted(async () => {
  await Promise.all([
    attributeStore.fetchAllAttributes(),
    loadProductInfo(),
    loadVariantSerialNumbers()
  ])

  // Debug: Log storage data after loading
  console.log('Storage data loaded:', attributeStore.storage)
  console.log('All attribute store data:', {
    cpu: attributeStore.cpu,
    ram: attributeStore.ram,
    gpu: attributeStore.gpu,
    colors: attributeStore.colors,
    storage: attributeStore.storage,
    screen: attributeStore.screen
  })
})
</script>

<style scoped>
.variant-manager {
  padding: 1rem;
}
</style>
