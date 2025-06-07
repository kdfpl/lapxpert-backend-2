<template>
  <Dialog
    v-model:visible="dialogVisible"
    modal
    :header="`Hủy đơn hàng ${order?.maHoaDon || ''}`"
    :style="{ width: '450px' }"
    class="order-cancel-dialog"
  >
    <div v-if="order" class="dialog-content">
      <!-- Warning Message -->
      <Message severity="warn" :closable="false" class="mb-4">
        <template #messageicon>
          <i class="pi pi-exclamation-triangle"></i>
        </template>
        <div>
          <strong>Cảnh báo:</strong> Hành động này sẽ hủy đơn hàng và không thể hoàn tác.
          <ul class="mt-2 ml-4 list-disc text-sm">
            <li>Tồn kho sản phẩm sẽ được giải phóng</li>
            <li>Voucher đã áp dụng sẽ được hoàn lại</li>
            <li>Thông báo sẽ được gửi đến khách hàng</li>
          </ul>
        </div>
      </Message>

      <!-- Order Information -->
      <div class="mb-4 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
        <h4 class="font-medium mb-2">Thông tin đơn hàng</h4>
        <div class="space-y-2 text-sm">
          <div class="flex justify-between">
            <span class="text-gray-600 dark:text-gray-400">Khách hàng:</span>
            <span class="font-medium">{{ order.khachHang?.hoTen || 'Khách lẻ' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600 dark:text-gray-400">Tổng tiền:</span>
            <span class="font-medium">{{ formatCurrency(order.tongThanhToan) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600 dark:text-gray-400">Trạng thái:</span>
            <OrderStatusBadge :status="order.trangThaiDonHang" size="small" />
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600 dark:text-gray-400">Ngày tạo:</span>
            <span>{{ formatDate(order.ngayTao) }}</span>
          </div>
        </div>
      </div>

      <!-- Cancellation Reason -->
      <div class="mb-4">
        <label class="block text-sm font-medium mb-2">
          Lý do hủy đơn hàng <span class="text-red-500">*</span>
        </label>
        <Select
          v-model="selectedReason"
          :options="cancellationReasons"
          option-label="label"
          option-value="value"
          placeholder="Chọn lý do hủy"
          class="w-full mb-3"
          :class="{ 'p-invalid': submitted && !selectedReason }"
        />
        <Textarea
          v-model="customReason"
          rows="3"
          placeholder="Nhập chi tiết lý do hủy (tùy chọn)..."
          class="w-full"
          :disabled="!selectedReason"
        />
        <small v-if="submitted && !selectedReason" class="p-error">
          Vui lòng chọn lý do hủy đơn hàng
        </small>
      </div>

      <!-- Refund Information -->
      <div v-if="order.trangThaiThanhToan === 'DA_THANH_TOAN'" class="mb-4">
        <div class="p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
          <div class="flex items-start gap-2">
            <i class="pi pi-info-circle text-blue-500 mt-0.5"></i>
            <div>
              <h5 class="font-medium text-blue-900 dark:text-blue-100 mb-1">
                Thông tin hoàn tiền
              </h5>
              <p class="text-sm text-blue-700 dark:text-blue-300">
                Đơn hàng đã được thanh toán. Sau khi hủy, tiền sẽ được hoàn lại theo phương thức thanh toán ban đầu trong vòng 3-5 ngày làm việc.
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Confirmation Checkbox -->
      <div class="mb-4">
        <div class="flex items-start gap-2">
          <Checkbox
            v-model="confirmed"
            :binary="true"
            input-id="confirm-cancel"
            :class="{ 'p-invalid': submitted && !confirmed }"
          />
          <label for="confirm-cancel" class="text-sm">
            Tôi xác nhận muốn hủy đơn hàng này và hiểu rằng hành động này không thể hoàn tác.
          </label>
        </div>
        <small v-if="submitted && !confirmed" class="p-error block mt-1">
          Vui lòng xác nhận để tiếp tục
        </small>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <Button
          label="Không hủy"
          icon="pi pi-times"
          @click="closeDialog"
          class="p-button-outlined"
          :disabled="loading"
        />
        <Button
          label="Xác nhận hủy"
          icon="pi pi-trash"
          @click="cancelOrder"
          severity="danger"
          :loading="loading"
          :disabled="!selectedReason || !confirmed"
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

const emit = defineEmits(['update:visible', 'cancelled'])

const orderStore = useOrderStore()
const toast = useToast()

// Reactive data
const dialogVisible = ref(false)
const selectedReason = ref('')
const customReason = ref('')
const confirmed = ref(false)
const loading = ref(false)
const submitted = ref(false)

// Cancellation reasons
const cancellationReasons = ref([
  { label: 'Khách hàng yêu cầu hủy', value: 'CUSTOMER_REQUEST' },
  { label: 'Hết hàng trong kho', value: 'OUT_OF_STOCK' },
  { label: 'Thông tin đơn hàng sai', value: 'WRONG_ORDER_INFO' },
  { label: 'Lỗi hệ thống', value: 'SYSTEM_ERROR' },
  { label: 'Không liên lạc được khách hàng', value: 'CUSTOMER_UNREACHABLE' },
  { label: 'Vấn đề về thanh toán', value: 'PAYMENT_ISSUE' },
  { label: 'Lý do khác', value: 'OTHER' }
])

// Computed properties
const finalReason = computed(() => {
  const reasonLabel = cancellationReasons.value.find(r => r.value === selectedReason.value)?.label || ''
  return customReason.value ? `${reasonLabel}: ${customReason.value}` : reasonLabel
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
  selectedReason.value = ''
  customReason.value = ''
  confirmed.value = false
  submitted.value = false
}

const closeDialog = () => {
  dialogVisible.value = false
}

const cancelOrder = async () => {
  submitted.value = true

  if (!selectedReason.value || !confirmed.value) {
    return
  }

  loading.value = true

  try {
    const result = await orderStore.cancelOrder(props.order.id, finalReason.value)

    if (result) {
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Đơn hàng đã được hủy thành công',
        life: 3000
      })

      emit('cancelled', result)
      closeDialog()
    }
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể hủy đơn hàng',
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
</script>

<style scoped>
.order-cancel-dialog {
  font-family: inherit;
}

.dialog-content {
  padding: 0;
}

:deep(.p-dialog-header) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
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

:deep(.p-select.p-invalid) {
  border-color: #e24c4c;
}

:deep(.p-checkbox.p-invalid .p-checkbox-box) {
  border-color: #e24c4c;
}

.p-error {
  color: #e24c4c;
  font-size: 0.875rem;
  margin-top: 0.25rem;
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

:deep(.dark .bg-gray-50) {
  background-color: #374151 !important;
}

/* Focus states for accessibility */
:deep(.p-select:focus) {
  box-shadow: 0 0 0 2px #3b82f6;
}

:deep(.p-textarea:focus) {
  box-shadow: 0 0 0 2px #3b82f6;
}

:deep(.p-checkbox:focus .p-checkbox-box) {
  box-shadow: 0 0 0 2px #3b82f6;
}
</style>
