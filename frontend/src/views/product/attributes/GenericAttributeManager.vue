<template>
    <Toast />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
            <i :class="config.icon" class="text-3xl text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">{{ config.title }}</h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              {{ config.description }}
            </p>
          </div>
        </div>
        <div class="flex gap-2">
          <Button :label="`Thêm ${config.label}`" icon="pi pi-plus" @click="openNew" />
          <Button
            label="Làm mới"
            icon="pi pi-refresh"
            severity="secondary"
            outlined
            @click="refreshData"
            :loading="loading"
          />
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <div class="card">
      <DataTable
        v-model:selection="selectedItems"
        :value="items"
        dataKey="id"
        paginator
        :rows="10"
        :filters="filters"
        filterDisplay="menu"
        :loading="loading"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink"
        :rowHover="true"
        responsiveLayout="scroll"
        class="p-datatable-sm"
      >
        <template #header>
          <div class="flex justify-between items-center py-2">
            <div class="flex items-center gap-2">
              <i class="pi pi-table text-primary"></i>
              <span class="font-semibold text-xl">{{ config.tableTitle }}</span>
            </div>
            <div class="flex gap-2">
              <IconField>
                <InputIcon>
                  <i class="pi pi-search" />
                </InputIcon>
                <InputText
                  v-model="filters['global'].value"
                  :placeholder="`Tìm kiếm ${config.label.toLowerCase()}...`"
                  class="w-80"
                />
              </IconField>
              <Button
                icon="pi pi-trash"
                label="Xóa đã chọn"
                severity="danger"
                outlined
                @click="confirmDeleteSelected"
                :disabled="!hasSelection"
              />
            </div>
          </div>
        </template>

        <Column selectionMode="multiple" headerStyle="width: 3rem"></Column>
        <Column header="STT" sortable style="width: 80px">
          <template #body="{ index }">
            <span class="font-medium">{{ index + 1 }}</span>
          </template>
        </Column>
        <Column :field="config.codeFieldName" header="Mã" sortable style="width: 120px">
          <template #body="{ data }">
            <span class="font-medium text-primary">{{ data[config.codeFieldName] }}</span>
          </template>
        </Column>
        <Column :field="config.fieldName" header="Tên" sortable>
          <template #body="{ data }">
            <span class="font-medium">{{ data[config.fieldName] }}</span>
          </template>
        </Column>
        <Column header="Thao tác" style="width: 150px">
          <template #body="{ data }">
            <div class="flex gap-2">
              <Button
                icon="pi pi-pencil"
                size="small"
                severity="info"
                outlined
                @click="editItem(data)"
                v-tooltip.top="'Chỉnh sửa'"
              />
              <Button
                icon="pi pi-trash"
                size="small"
                severity="danger"
                outlined
                @click="confirmDelete(data)"
                v-tooltip.top="'Xóa'"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Add/Edit Dialog -->
    <Dialog
      v-model:visible="itemDialog"
      :style="{ width: '600px' }"
      :header="isEditing ? `Chỉnh sửa ${config.label}` : `Thêm ${config.label} mới`"
      :modal="true"
      class="p-fluid"
    >
      <div class="space-y-6">
        <!-- Header Section -->
        <div class="flex items-center gap-3 pb-4 border-b border-surface-200">
          <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
            <i :class="config.icon" class="text-2xl text-primary"></i>
          </div>
          <div>
            <h3 class="font-semibold text-lg text-surface-900 m-0">{{ config.dialogTitle }}</h3>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              {{ isEditing ? 'Cập nhật thông tin' : 'Thêm mới vào hệ thống' }}
            </p>
          </div>
        </div>

        <!-- Input Section -->
        <div class="space-y-4">
          <div class="field">
            <label :for="config.fieldName" class="block font-medium text-surface-900 mb-2">
              {{ config.fieldLabel }} *
            </label>
            <Textarea
              :id="config.fieldName"
              v-model="currentItem[config.fieldName]"
              :class="{ 'p-invalid': submitted && !currentItem[config.fieldName] }"
              :placeholder="isEditing ? `Nhập ${config.fieldLabel.toLowerCase()}...` : `Nhập ${config.fieldLabel.toLowerCase()}... (có thể nhập nhiều giá trị cách nhau bằng dấu phẩy)`"
              rows="4"
              :autoResize="true"
              class="w-full"
            />
            <small v-if="submitted && !currentItem[config.fieldName]" class="p-error block mt-1">
              {{ config.fieldLabel }} là bắt buộc.
            </small>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3 pt-4">
          <Button
            label="Hủy"
            icon="pi pi-times"
            severity="secondary"
            outlined
            @click="hideDialog"
            :disabled="saving"
            class="px-4 py-2"
          />
          <Button
            :label="isEditing ? 'Cập nhật' : 'Lưu'"
            icon="pi pi-check"
            @click="saveItem"
            :loading="saving"
            class="px-4 py-2"
          />
        </div>
      </template>
    </Dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { FilterMatchMode } from '@primevue/core/api'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import attributeService from '@/apis/attribute'

// Props
const props = defineProps({
  config: {
    type: Object,
    required: true,
    validator: (config) => {
      return config.type && config.label && config.fieldName && config.fieldLabel && config.codeFieldName
    }
  }
})

// Store and utilities
const toast = useToast()
const confirm = useConfirm()

// Component state
const items = ref([])
const currentItem = ref({})
const selectedItems = ref([])
const itemDialog = ref(false)
const submitted = ref(false)
const loading = ref(false)
const saving = ref(false)
const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS },
})

// Computed properties
const hasSelection = computed(() => selectedItems.value && selectedItems.value.length > 0)
const isEditing = computed(() => currentItem.value.id != null)

// API method getters
const getApiMethod = (action) => {
  const methodName = `${action}${props.config.type.charAt(0).toUpperCase() + props.config.type.slice(1)}`
  return attributeService[methodName]
}

// Methods
const refreshData = async () => {
  loading.value = true
  try {
    const getAllMethod = getApiMethod('getAll')
    const response = await getAllMethod()
    items.value = response.data || []
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể tải danh sách ${props.config.label}`,
      life: 3000,
    })
  } finally {
    loading.value = false
  }
}

const openNew = () => {
  currentItem.value = { [props.config.fieldName]: '' }
  submitted.value = false
  itemDialog.value = true
}

const editItem = (item) => {
  currentItem.value = { ...item }
  submitted.value = false
  itemDialog.value = true
}

const hideDialog = () => {
  itemDialog.value = false
  submitted.value = false
}

const saveItem = async () => {
  submitted.value = true

  if (!currentItem.value[props.config.fieldName]?.trim()) {
    return
  }

  saving.value = true
  try {
    if (isEditing.value) {
      const updateMethod = getApiMethod('update')
      await updateMethod(currentItem.value.id, currentItem.value)
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `${props.config.label} đã được cập nhật`,
        life: 3000,
      })
    } else {
      // Handle bulk creation for comma-separated input
      const inputValue = currentItem.value[props.config.fieldName].trim()
      const values = inputValue.split(',').map(v => v.trim()).filter(v => v.length > 0)

      if (values.length > 1) {
        // Bulk creation
        const items = values.map(value => ({
          [props.config.fieldName]: value
        }))
        const createMultipleMethod = getApiMethod('createMultiple')
        await createMultipleMethod(items)
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đã thêm ${values.length} ${props.config.label}`,
          life: 3000,
        })
      } else {
        // Single creation
        const createMethod = getApiMethod('create')
        await createMethod(currentItem.value)
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `${props.config.label} đã được thêm mới`,
          life: 3000,
        })
      }
    }

    hideDialog()
    await refreshData()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: isEditing.value ? `Không thể cập nhật ${props.config.label}` : `Không thể thêm ${props.config.label}`,
      life: 3000,
    })
  } finally {
    saving.value = false
  }
}

const confirmDelete = (item) => {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa ${props.config.label} "${item[props.config.fieldName]}"?`,
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
    accept: () => deleteItem(item),
  })
}

const deleteItem = async (item) => {
  try {
    const deleteMethod = getApiMethod('delete')
    await deleteMethod(item.id)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `${props.config.label} đã được xóa`,
      life: 3000,
    })
    await refreshData()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể xóa ${props.config.label}`,
      life: 3000,
    })
  }
}

const confirmDeleteSelected = () => {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa ${selectedItems.value.length} ${props.config.label} đã chọn?`,
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
    accept: () => deleteSelectedItems(),
  })
}

const deleteSelectedItems = async () => {
  try {
    const ids = selectedItems.value.map(item => item.id)
    const deleteMultipleMethod = getApiMethod('deleteMultiple')
    await deleteMultipleMethod(ids)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã xóa ${selectedItems.value.length} ${props.config.label}`,
      life: 3000,
    })
    selectedItems.value = []
    await refreshData()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể xóa các ${props.config.label} đã chọn`,
      life: 3000,
    })
  }
}

// Lifecycle
onMounted(() => {
  refreshData()
})
</script>
