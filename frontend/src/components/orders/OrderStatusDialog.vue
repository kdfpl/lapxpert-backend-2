<template>
  <Dialog
    v-model:visible="dialogVisible"
    modal
    :header="`Cập nhật trạng thái đơn hàng ${order?.maHoaDon || ''}`"
    :style="{ width: '500px' }"
    class="order-status-dialog"
  >
    <div v-if="order" class="dialog-content">
      <!-- Current Status -->
      <div class="mb-4">
        <label class="block text-sm font-medium mb-2">Trạng thái hiện tại</label>
        <OrderStatusBadge :status="order.trangThaiDonHang" size="large" />
      </div>

      <!-- New Status Selection -->
      <div class="mb-4">
        <label class="block text-sm font-medium mb-2">Trạng thái mới</label>
        <Dropdown
          v-model="selectedStatus"
          :options="availableStatuses"
          option-label="label"
          option-value="value"
          placeholder="Chọn trạng thái mới"
          class="w-full"
          :class="{ 'p-invalid': submitted && !selectedStatus }"
        />
        <small v-if="submitted && !selectedStatus" class="p-error">
          Vui lòng chọn trạng thái mới
        </small>
      </div>

      <!-- Status Preview -->
      <div v-if="selectedStatus" class="mb-4">
        <label class="block text-sm font-medium mb-2">Xem trước</label>
        <OrderStatusBadge :status="selectedStatus" size="large" show-progress />
      </div>

      <!-- Reason Input -->
      <div class="mb-4">
        <label class="block text-sm font-medium mb-2">
          Lý do thay đổi
          <span v-if="requiresReason" class="text-red-500">*</span>
        </label>
        <Textarea
          v-model="reason"
          rows="3"
          placeholder="Nhập lý do thay đổi trạng thái..."
          class="w-full"
          :class="{ 'p-invalid': submitted && requiresReason && !reason }"
        />
        <small v-if="submitted && requiresReason && !reason" class="p-error">
          Vui lòng nhập lý do thay đổi
        </small>
      </div>

      <!-- Warning Messages -->
      <div v-if="statusWarning" class="mb-4">
        <Message :severity="statusWarning.severity" :closable="false">
          <template #messageicon>
            <i :class="statusWarning.icon"></i>
          </template>
          {{ statusWarning.message }}
        </Message>
      </div>

      <!-- Order Information -->
      <div class="mb-4 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
        <h4 class="font-medium mb-2">Thông tin đơn hàng</h4>
        <div class="grid grid-cols-2 gap-2 text-sm">
          <div>
            <span class="text-gray-600 dark:text-gray-400">Khách hàng:</span>
            <span class="ml-2 font-medium">{{ order.khachHang?.hoTen || 'Khách lẻ' }}</span>
          </div>
          <div>
            <span class="text-gray-600 dark:text-gray-400">Tổng tiền:</span>
            <span class="ml-2 font-medium">{{ formatCurrency(order.tongThanhToan) }}</span>
          </div>
          <div>
            <span class="text-gray-600 dark:text-gray-400">Ngày tạo:</span>
            <span class="ml-2">{{ formatDate(order.ngayTao) }}</span>
          </div>
          <div>
            <span class="text-gray-600 dark:text-gray-400">Loại:</span>
            <span class="ml-2">{{ getOrderTypeLabel(order.loaiHoaDon) }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <Button
          label="Hủy"
          icon="pi pi-times"
          @click="closeDialog"
          class="p-button-outlined"
          :disabled="loading"
        />
        <Button
          label="Cập nhật"
          icon="pi pi-check"
          @click="updateStatus"
          :loading="loading"
          :disabled="!selectedStatus || (requiresReason && !reason)"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useOrderStore } from '@/stores/orderStore'
import { useToast } from 'primevue/usetoast'
import OrderStatusBadge from './OrderStatusBadge.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  order: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'updated'])

const orderStore = useOrderStore()
const toast = useToast()

// Reactive data
const dialogVisible = ref(false)
const selectedStatus = ref('')
const reason = ref('')
const loading = ref(false)
const submitted = ref(false)

// Computed properties
const availableStatuses = computed(() => {
  if (!props.order) return []
  
  const currentStatus = props.order.trangThaiDonHang
  const allStatuses = orderStore.orderStatuses
  
  // Define status flow rules
  const statusFlow = {
    'CHO_XAC_NHAN': ['DA_XAC_NHAN', 'DA_HUY'],
    'DA_XAC_NHAN': ['DANG_CHUAN_BI', 'DA_HUY'],
    'DANG_CHUAN_BI': ['DANG_GIAO_HANG', 'DA_HUY'],
    'DANG_GIAO_HANG': ['DA_GIAO_HANG', 'THAT_BAI'],
    'DA_GIAO_HANG': ['HOAN_THANH', 'TRA_HANG'],
    'HOAN_THANH': ['TRA_HANG'],
    'TRA_HANG': ['HOAN_TIEN'],
    'DA_HUY': [],
    'THAT_BAI': ['CHO_XAC_NHAN'],
    'HOAN_TIEN': []
  }
  
  const allowedStatuses = statusFlow[currentStatus] || []
  
  return allStatuses.filter(status => 
    allowedStatuses.includes(status.value)
  )
})

const requiresReason = computed(() => {
  return ['DA_HUY', 'THAT_BAI', 'TRA_HANG', 'HOAN_TIEN'].includes(selectedStatus.value)
})

const statusWarning = computed(() => {
  if (!selectedStatus.value) return null
  
  const warnings = {
    'DA_HUY': {
      severity: 'warn',
      icon: 'pi pi-exclamation-triangle',
      message: 'Hủy đơn hàng sẽ giải phóng tồn kho và không thể hoàn tác.'
    },
    'THAT_BAI': {
      severity: 'error',
      icon: 'pi pi-times-circle',
      message: 'Đánh dấu đơn hàng thất bại sẽ ảnh hưởng đến thống kê.'
    },
    'TRA_HANG': {
      severity: 'warn',
      icon: 'pi pi-undo',
      message: 'Xử lý trả hàng cần kiểm tra tình trạng sản phẩm.'
    },
    'HOAN_TIEN': {
      severity: 'info',
      icon: 'pi pi-money-bill',
      message: 'Hoàn tiền cần xác nhận từ bộ phận tài chính.'
    }
  }
  
  return warnings[selectedStatus.value] || null
})

// Watchers
watch(() => props.visible, (newValue) => {
  dialogVisible.value = newValue
  if (newValue) {
    resetForm()
  }
})

watch(dialogVisible, (newValue) => {
  emit('update:visible', newValue)
})

// Methods
const resetForm = () => {
  selectedStatus.value = ''
  reason.value = ''
  submitted.value = false
}

const closeDialog = () => {
  dialogVisible.value = false
}

const updateStatus = async () => {
  submitted.value = true
  
  if (!selectedStatus.value || (requiresReason.value && !reason.value)) {
    return
  }
  
  loading.value = true
  
  try {
    const result = await orderStore.updateOrderStatus(
      props.order.id,
      selectedStatus.value,
      reason.value
    )
    
    if (result) {
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Cập nhật trạng thái đơn hàng thành công',
        life: 3000
      })
      
      emit('updated', result)
      closeDialog()
    }
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể cập nhật trạng thái đơn hàng',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

// Utility functions
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount || 0)
}

const formatDate = (date) => {
  return new Intl.DateTimeFormat('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(date))
}

const getOrderTypeLabel = (type) => {
  const typeMap = {
    'ONLINE': 'Đơn hàng online',
    'TAI_QUAY': 'Bán tại quầy'
  }
  return typeMap[type] || type
}
</script>

<style scoped>
.order-status-dialog {
  font-family: inherit;
}

.dialog-content {
  padding: 0;
}

:deep(.p-dialog-header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 6px 6px 0 0;
}

:deep(.p-dialog-header .p-dialog-title) {
  font-weight: 600;
}

:deep(.p-dialog-header .p-dialog-header-icon) {
  color: white;
}

:deep(.p-dialog-header .p-dialog-header-icon:hover) {
  background-color: rgba(255, 255, 255, 0.1);
}

:deep(.p-dropdown.p-invalid) {
  border-color: #e24c4c;
}

:deep(.p-textarea.p-invalid) {
  border-color: #e24c4c;
}

.p-error {
  color: #e24c4c;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

/* Animation for status preview */
.status-preview {
  transition: all 0.3s ease;
  transform: scale(1);
}

.status-preview:hover {
  transform: scale(1.05);
}

/* Loading state */
:deep(.p-button[disabled]) {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Responsive design */
@media (max-width: 640px) {
  :deep(.p-dialog) {
    width: 95vw !important;
    margin: 1rem;
  }
  
  .dialog-content {
    padding: 0.5rem;
  }
  
  :deep(.p-dialog-footer) {
    padding: 1rem 0.5rem;
  }
}

/* Dark mode support */
:deep(.dark .p-dialog) {
  background-color: #1f2937;
  border-color: #374151;
}

:deep(.dark .p-dialog-header) {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
}

:deep(.dark .bg-gray-50) {
  background-color: #374151 !important;
}

/* Focus states for accessibility */
:deep(.p-dropdown:focus) {
  box-shadow: 0 0 0 2px #3b82f6;
}

:deep(.p-textarea:focus) {
  box-shadow: 0 0 0 2px #3b82f6;
}

/* Custom scrollbar for textarea */
:deep(.p-textarea) {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 #f1f5f9;
}

:deep(.p-textarea::-webkit-scrollbar) {
  width: 6px;
}

:deep(.p-textarea::-webkit-scrollbar-track) {
  background: #f1f5f9;
  border-radius: 3px;
}

:deep(.p-textarea::-webkit-scrollbar-thumb) {
  background: #cbd5e1;
  border-radius: 3px;
}

:deep(.p-textarea::-webkit-scrollbar-thumb:hover) {
  background: #94a3b8;
}
</style>
