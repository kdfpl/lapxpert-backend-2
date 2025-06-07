<template>
  <Fluid>
    <Toast />
    <ConfirmDialog />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-microchip text-3xl text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">Quản lý CPU</h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              Quản lý danh sách CPU trong hệ thống
            </p>
          </div>
        </div>
        <div class="flex gap-2">
          <Button label="Thêm CPU" icon="pi pi-plus" @click="openNew" />
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
              <span class="font-semibold text-xl">Danh sách CPU</span>
            </div>
            <div class="flex gap-2">
              <IconField>
                <InputIcon>
                  <i class="pi pi-search" />
                </InputIcon>
                <InputText
                  v-model="filters['global'].value"
                  placeholder="Tìm kiếm CPU..."
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
        <Column field="id" header="ID" sortable style="width: 100px"></Column>
        <Column field="moTaCpu" header="Mô tả CPU" sortable>
          <template #body="{ data }">
            <span class="font-medium">{{ data.moTaCpu }}</span>
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
      :style="{ width: '450px' }"
      :header="isEditing ? 'Chỉnh sửa CPU' : 'Thêm CPU mới'"
      :modal="true"
      class="p-fluid"
    >
      <div class="border border-surface-200 rounded-lg p-4">
        <div class="flex items-center gap-2 mb-4">
          <i class="pi pi-microchip text-primary"></i>
          <span class="font-semibold text-lg">Thông tin CPU</span>
        </div>
        
        <div class="field">
          <label for="moTaCpu" class="font-medium">Mô tả CPU *</label>
          <InputText
            id="moTaCpu"
            v-model="currentItem.moTaCpu"
            :class="{ 'p-invalid': submitted && !currentItem.moTaCpu }"
            placeholder="Nhập mô tả CPU..."
          />
          <small v-if="submitted && !currentItem.moTaCpu" class="p-error">
            Mô tả CPU là bắt buộc.
          </small>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            outlined
            @click="hideDialog"
            :disabled="saving"
          />
          <Button
            :label="isEditing ? 'Cập nhật' : 'Lưu'"
            icon="pi pi-check"
            @click="saveItem"
            :loading="saving"
          />
        </div>
      </template>
    </Dialog>
  </Fluid>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { FilterMatchMode } from '@primevue/core/api'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import attributeService from '@/apis/attribute'

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

// Methods
const refreshData = async () => {
  loading.value = true
  try {
    const response = await attributeService.getAllCpu()
    items.value = response.data || []
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải danh sách CPU',
      life: 3000,
    })
  } finally {
    loading.value = false
  }
}

const openNew = () => {
  currentItem.value = { moTaCpu: '' }
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
  
  if (!currentItem.value.moTaCpu?.trim()) {
    return
  }

  saving.value = true
  try {
    if (isEditing.value) {
      await attributeService.updateCpu(currentItem.value.id, currentItem.value)
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'CPU đã được cập nhật',
        life: 3000,
      })
    } else {
      await attributeService.createCpu(currentItem.value)
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'CPU đã được thêm mới',
        life: 3000,
      })
    }
    
    hideDialog()
    await refreshData()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: isEditing.value ? 'Không thể cập nhật CPU' : 'Không thể thêm CPU',
      life: 3000,
    })
  } finally {
    saving.value = false
  }
}

const confirmDelete = (item) => {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa CPU "${item.moTaCpu}"?`,
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
    await attributeService.deleteCpu(item.id)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'CPU đã được xóa',
      life: 3000,
    })
    await refreshData()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể xóa CPU',
      life: 3000,
    })
  }
}

const confirmDeleteSelected = () => {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa ${selectedItems.value.length} CPU đã chọn?`,
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
    await attributeService.deleteMultipleCpu(ids)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã xóa ${selectedItems.value.length} CPU`,
      life: 3000,
    })
    selectedItems.value = []
    await refreshData()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể xóa các CPU đã chọn',
      life: 3000,
    })
  }
}

// Lifecycle
onMounted(() => {
  refreshData()
})
</script>
