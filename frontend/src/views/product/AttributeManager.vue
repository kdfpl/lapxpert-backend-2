<script setup>
import { ref, computed, defineProps } from 'vue'
import { FilterMatchMode } from '@primevue/core/api'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useAttributeStore } from '@/stores/attributestore'
import attributeService from '@/apis/attribute'

// Props
const props = defineProps({
  attributeType: {
    type: String,
    required: true,
  },
  attributeLabel: {
    type: String,
    required: true,
  },
  propertyName: {
    type: String,
    required: true,
  },
})

// Store and utilities
const attributeStore = useAttributeStore()
const toast = useToast()
const confirm = useConfirm()

// Component state
const items = computed(() => attributeStore[props.attributeType] || [])
const currentItem = ref({})
const selectedItems = ref([])
const itemDialog = ref(false)
const editItemDialog = ref(false)
const submitted = ref(false)
const loading = ref(false)
const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS },
})
const itemInputText = ref('')

// Statistics
const totalItems = computed(() => items.value.length)
const hasSelection = computed(() => selectedItems.value && selectedItems.value.length > 0)

// API Method Helpers
const getApiServiceMethod = (baseName) => {
  let typePart = props.attributeType
  if (typePart === 'brand') {
    typePart = 'Brand'
  } else if (typePart === 'category') {
    typePart = 'Category'
  } else if (typePart === 'colors') {
    typePart = 'Colors'
  } else {
    typePart = typePart.charAt(0).toUpperCase() + typePart.slice(1)
  }

  const methodName = `${baseName}${typePart}`

  if (typeof attributeService[methodName] === 'function') {
    return attributeService[methodName]
  } else {
    console.error(`API service method "${methodName}" not found for type "${props.attributeType}"`)
    return async () => {
      toast.add({
        severity: 'error',
        summary: 'Lỗi Cấu Hình',
        detail: `Phương thức API "${methodName}" không tồn tại.`,
        life: 5000,
      })
      throw new Error(`API service method "${methodName}" not found.`)
    }
  }
}

// API methods
const createSingleMethod = computed(() => getApiServiceMethod('create'))
const createMultipleMethod = computed(() => getApiServiceMethod('createMultiple'))
const deleteSingleMethod = computed(() => getApiServiceMethod('delete'))
const deleteMultipleMethod = computed(() => getApiServiceMethod('deleteMultiple'))

// Dialog Handlers
const openNew = () => {
  currentItem.value = {}
  itemInputText.value = ''
  submitted.value = false
  itemDialog.value = true
}

const hideDialog = () => {
  itemDialog.value = false
  submitted.value = false
}

const editItem = (itemData) => {
  if (!(props.propertyName in itemData)) {
    console.warn(`Property "${props.propertyName}" not found in item data:`, itemData)
  }
  currentItem.value = { ...itemData }
  submitted.value = false
  editItemDialog.value = true
}

const hideEditDialog = () => {
  editItemDialog.value = false
  submitted.value = false
  currentItem.value = {}
}

const confirmDeleteItem = (itemData) => {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa "${itemData[props.propertyName]}"?`,
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
    accept: () => deleteSingle(itemData),
  })
}

const confirmDeleteSelected = () => {
  if (!selectedItems.value?.length) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: `Chưa chọn ${props.attributeLabel} nào để xóa`,
      life: 3000,
    })
    return
  }

  confirm.require({
    message: `Bạn có muốn xóa ${selectedItems.value.length} ${props.attributeLabel} đã chọn?`,
    header: 'Xác nhận xóa nhiều',
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
    accept: () => deleteMultiple(),
  })
}

// Delete operations
const deleteSingle = async (itemData) => {
  if (!itemData?.id) return
  loading.value = true
  try {
    const deleteMethod = deleteSingleMethod.value
    await deleteMethod(itemData.id)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Xóa ${props.attributeLabel} thành công`,
      life: 3000,
    })
    await attributeStore.fetchAllAttributes()
  } catch (error) {
    console.error(`Error deleting ${props.attributeLabel}:`, error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail:
        error?.response?.data?.message ||
        error?.message ||
        `Lỗi không xác định khi xóa ${props.attributeLabel}`,
      life: 3000,
    })
  } finally {
    loading.value = false
  }
}

const deleteMultiple = async () => {
  if (!selectedItems.value?.length) return
  loading.value = true
  const selectedIds = selectedItems.value.map((item) => item.id)
  try {
    const deleteMethod = deleteMultipleMethod.value
    await deleteMethod(selectedIds)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã xóa ${selectedIds.length} ${props.attributeLabel}`,
      life: 3000,
    })
    await attributeStore.fetchAllAttributes()
    selectedItems.value = []
  } catch (error) {
    console.error(`Error deleting multiple ${props.attributeLabel}s:`, error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail:
        error?.response?.data?.message ||
        error?.message ||
        `Lỗi không xác định khi xóa nhiều ${props.attributeLabel}`,
      life: 3000,
    })
  } finally {
    loading.value = false
  }
}

// Save / Update operations
const saveItems = async () => {
  submitted.value = true
  const inputText = itemInputText.value.trim()
  if (!inputText) {
    submitted.value = false
    return
  }

  const itemNames = inputText
    .split(',')
    .map((name) => name.trim())
    .filter((name) => name.length > 0)

  if (itemNames.length === 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: `Không tìm thấy tên ${props.attributeLabel} hợp lệ.`,
      life: 3000,
    })
    submitted.value = false
    return
  }

  const payload = itemNames.map((name) => ({ [props.propertyName]: name }))

  try {
    const createMethod = createMultipleMethod.value
    const response = await createMethod(payload)
    const createdCount = response?.data?.length || itemNames.length
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã thêm thành công ${createdCount} ${props.attributeLabel}.`,
      life: 3000,
    })
    await attributeStore.fetchAllAttributes()
    itemDialog.value = false
    itemInputText.value = ''
  } catch (error) {
    console.error(`Error saving multiple ${props.attributeLabel}s:`, error)
    const detailMessage =
      error?.response?.data?.message ||
      error?.message ||
      `Có lỗi xảy ra khi lưu ${props.attributeLabel}.`
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: detailMessage,
      life: 5000,
    })
  } finally {
    submitted.value = false
  }
}

const updateItem = async () => {
  submitted.value = true
  if (!currentItem.value[props.propertyName]?.trim()) {
    submitted.value = false
    return
  }

  try {
    const updateMethod = createSingleMethod.value
    await updateMethod(currentItem.value)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Cập nhật ${props.attributeLabel} thành công`,
      life: 3000,
    })
    await attributeStore.fetchAllAttributes()
    editItemDialog.value = false
    currentItem.value = {}
  } catch (error) {
    console.error(`Error updating ${props.attributeLabel}:`, error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail:
        error?.response?.data?.message ||
        error?.message ||
        `Lỗi không xác định khi cập nhật ${props.attributeLabel}`,
      life: 3000,
    })
  } finally {
    submitted.value = false
  }
}
</script>

<template>
  <div class="flex flex-col gap-4">
    <!-- Header Section -->
    <div class="card">
      <div class="flex items-center justify-between mb-5">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-list text-lg text-primary"></i>
          </div>
          <div>
            <h2 class="font-semibold text-xl text-surface-900 m-0">{{ attributeLabel }}</h2>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              Quản lý {{ totalItems }} {{ attributeLabel.toLowerCase() }}
            </p>
          </div>
        </div>
        <div class="flex gap-2">
          <Button
            icon="pi pi-plus"
            label="Thêm mới"
            size="small"
            severity="primary"
            @click="openNew"
            v-tooltip.left="'Thêm mới'"
          />
          <Button
            icon="pi pi-trash"
            label="Xóa đã chọn"
            size="small"
            severity="danger"
            outlined
            @click="confirmDeleteSelected"
            :disabled="!hasSelection"
            v-tooltip.left="'Xóa các mục đã chọn'"
          />
        </div>
      </div>
      <!-- Data Table Section -->
      <DataTable
        v-model:selection="selectedItems"
        :value="items"
        dataKey="id"
        paginator
        :rows="10"
        :filters="filters"
        filterDisplay="menu"
        :loading="loading || (attributeStore.loading && items.length === 0)"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink"
        :rowHover="true"
        responsiveLayout="scroll"
        class="p-datatable-sm"
      >
        <template #header>
          <div class="flex justify-between items-center py-2">
            <div class="flex items-center gap-2">
              <i class="pi pi-table text-primary"></i>
              <span class="font-semibold text-xl">Danh sách {{ attributeLabel }}</span>
              <Badge :value="totalItems" severity="info" class="ml-2" />
            </div>
            <div class="flex items-center gap-2">
              <IconField>
                <InputIcon>
                  <i class="pi pi-search" />
                </InputIcon>
                <InputText
                  v-model="filters['global'].value"
                  :placeholder="`Tìm kiếm ${attributeLabel.toLowerCase()}...`"
                  class="w-64"
                />
              </IconField>
            </div>
          </div>
        </template>

        <template #empty>
          <div class="text-center py-6">
            <i class="pi pi-inbox text-3xl text-surface-400 mb-3"></i>
            <p class="text-surface-600 text-sm"
              >Không tìm thấy {{ attributeLabel.toLowerCase() }} nào.</p
            >
          </div>
        </template>

        <template #loading>
          <div class="text-center py-6">
            <ProgressSpinner style="width: 28px; height: 28px" strokeWidth="4" />
            <p class="text-surface-600 mt-2 text-sm"
              >Đang tải dữ liệu {{ attributeLabel.toLowerCase() }}...</p
            >
          </div>
        </template>

        <Column selectionMode="multiple" style="width: 3rem" :exportable="false" />

        <Column
          header="STT"
          headerStyle="width: 3rem; text-align: center"
          bodyStyle="text-align: center"
        >
          <template #body="slotProps">
            <span class="text-surface-600 font-medium text-sm">{{ slotProps.index + 1 }}</span>
          </template>
        </Column>

        <Column
          :field="propertyName"
          :header="`Tên ${attributeLabel}`"
          sortable
          style="min-width: 12rem"
        >
          <template #body="{ data }">
            <span class="font-medium text-surface-900 text-sm">{{ data[propertyName] }}</span>
          </template>
        </Column>

        <Column header="Thao tác" :exportable="false" style="width: 6rem">
          <template #body="{ data }">
            <div class="flex items-center gap-1">
              <Button
                icon="pi pi-pencil"
                outlined
                rounded
                @click="editItem(data)"
                v-tooltip.top="'Chỉnh sửa'"
              />
              <Button
                icon="pi pi-trash"
                outlined
                rounded
                severity="danger"
                @click="confirmDeleteItem(data)"
                v-tooltip.top="'Xóa'"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Dialog Thêm mới -->
    <Dialog
      v-model:visible="itemDialog"
      :style="{ width: '500px' }"
      :header="`Thêm mới ${attributeLabel}`"
      :modal="true"
      class="p-fluid"
    >
      <!-- Form Section -->
      <div class="border border-surface-200 rounded-lg p-4">
        <div class="flex items-center gap-2 mb-4">
          <i class="pi pi-plus text-primary"></i>
          <span class="font-semibold text-lg">Thông tin {{ attributeLabel }}</span>
        </div>

        <div class="space-y-4">
          <div class="field">
            <label for="itemInput" class="block text-surface-900 font-medium mb-2 text-sm">
              Tên {{ attributeLabel }} <span class="text-red-500">*</span>
            </label>
            <Textarea
              id="itemInput"
              v-model="itemInputText"
              :class="{ 'p-invalid': submitted && (!itemInputText || !itemInputText.trim()) }"
              :placeholder="`Nhập tên các ${attributeLabel.toLowerCase()}, cách nhau bởi dấu phẩy (,)`"
              autoResize
              rows="3"
              class="w-full"
            />
            <small v-if="submitted && (!itemInputText || !itemInputText.trim())" class="p-error">
              Tên {{ attributeLabel }} không được để trống.
            </small>
            <small class="text-surface-600 block mt-1 text-xs">
              <i class="pi pi-info-circle mr-1"></i>
              Ví dụ: {{ attributeLabel }} A, {{ attributeLabel }} B, {{ attributeLabel }} C
            </small>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            outlined
            size="small"
            @click="hideDialog"
            :disabled="submitted"
          />
          <Button
            label="Lưu"
            icon="pi pi-check"
            size="small"
            @click="saveItems"
            :loading="submitted"
          />
        </div>
      </template>
    </Dialog>

    <!-- Dialog Chỉnh sửa -->
    <Dialog
      v-model:visible="editItemDialog"
      :style="{ width: '450px' }"
      :header="`Chỉnh sửa ${attributeLabel}`"
      :modal="true"
      class="p-fluid"
    >
      <!-- Form Section -->
      <div class="border border-surface-200 rounded-lg p-4">
        <div class="flex items-center gap-2 mb-4">
          <i class="pi pi-pencil text-primary"></i>
          <span class="font-semibold text-lg">Thông tin {{ attributeLabel }}</span>
        </div>

        <div class="space-y-4">
          <div class="field">
            <label for="editId" class="block text-surface-900 font-medium mb-2 text-sm">ID</label>
            <InputText id="editId" v-model="currentItem.id" disabled size="small" class="w-full" />
          </div>

          <div class="field">
            <label
              :for="`editName-${attributeType}`"
              class="block text-surface-900 font-medium mb-2 text-sm"
            >
              Tên {{ attributeLabel }} <span class="text-red-500">*</span>
            </label>
            <InputText
              :id="`editName-${attributeType}`"
              v-model="currentItem[propertyName]"
              :class="{
                'p-invalid':
                  submitted && (!currentItem[propertyName] || !currentItem[propertyName].trim()),
              }"
              :placeholder="`Nhập tên ${attributeLabel.toLowerCase()}`"
              autofocus
              size="small"
              class="w-full"
            />
            <small
              v-if="submitted && (!currentItem[propertyName] || !currentItem[propertyName].trim())"
              class="p-error"
            >
              Tên {{ attributeLabel }} không được để trống.
            </small>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            outlined
            size="small"
            @click="hideEditDialog"
            :disabled="submitted"
          />
          <Button
            label="Cập nhật"
            icon="pi pi-check"
            size="small"
            @click="updateItem"
            :loading="submitted"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
