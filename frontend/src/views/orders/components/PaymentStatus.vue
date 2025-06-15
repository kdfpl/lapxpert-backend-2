<template>
  <div class="payment-status-container">
    <!-- Header -->
    <div class="flex items-center gap-2 mb-6">
      <i class="pi pi-credit-card text-primary text-xl"></i>
      <h3 class="text-xl font-semibold text-surface-900 dark:text-surface-0">
        Trạng thái thanh toán
      </h3>
    </div>

    <!-- Payment Status Card -->
    <div class="card border border-surface-200 dark:border-surface-700 mb-6">
      <!-- Current Status -->
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-3">
          <div class="status-icon" :class="statusIconClass">
            <i :class="statusInfo.icon" class="text-2xl"></i>
          </div>
          <div>
            <h4 class="text-lg font-semibold text-surface-900 dark:text-surface-0">
              {{ statusInfo.title }}
            </h4>
            <p class="text-surface-600 dark:text-surface-400">
              {{ statusInfo.description }}
            </p>
          </div>
        </div>
        <Badge :value="statusInfo.label" :severity="statusInfo.severity" class="text-sm" />
      </div>

      <!-- Payment Progress -->
      <div v-if="showProgress" class="mb-6">
        <div class="flex justify-between items-center mb-2">
          <span class="text-sm font-medium text-surface-700 dark:text-surface-300">
            Tiến độ thanh toán
          </span>
          <span class="text-sm text-surface-600 dark:text-surface-400">
            {{ Math.round(paymentProgress) }}%
          </span>
        </div>
        <ProgressBar :value="paymentProgress" :show-value="false" class="h-2" />
      </div>

      <!-- Payment Details -->
      <div class="space-y-4">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Total Amount -->
          <div class="payment-detail-item">
            <label class="payment-detail-label">Tổng thanh toán</label>
            <div class="payment-detail-value">
              {{ formatCurrency(totalAmount) }}
            </div>
          </div>

          <!-- Paid Amount -->
          <div class="payment-detail-item">
            <label class="payment-detail-label">Đã thanh toán</label>
            <div class="payment-detail-value text-green-600">
              {{ formatCurrency(paidAmount) }}
            </div>
          </div>

          <!-- Remaining Amount -->
          <div v-if="remainingAmount > 0" class="payment-detail-item">
            <label class="payment-detail-label">Còn lại</label>
            <div class="payment-detail-value text-orange-600">
              {{ formatCurrency(remainingAmount) }}
            </div>
          </div>

          <!-- Payment Method -->
          <div v-if="paymentMethod" class="payment-detail-item">
            <label class="payment-detail-label">Phương thức</label>
            <div class="payment-detail-value flex items-center gap-2">
              <i :class="paymentMethodInfo.icon" class="text-primary"></i>
              {{ paymentMethodInfo.label }}
            </div>
          </div>
        </div>

        <!-- Transaction ID -->
        <div v-if="transactionId" class="payment-detail-item">
          <label class="payment-detail-label">Mã giao dịch</label>
          <div class="payment-detail-value font-mono">
            {{ transactionId }}
            <Button
              icon="pi pi-copy"
              severity="secondary"
              text
              size="small"
              @click="copyTransactionId"
              v-tooltip="'Sao chép mã giao dịch'"
            />
          </div>
        </div>

        <!-- Payment Date -->
        <div v-if="paymentDate" class="payment-detail-item">
          <label class="payment-detail-label">Thời gian thanh toán</label>
          <div class="payment-detail-value">
            {{ formatDateTime(paymentDate) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Payment Actions -->
    <div v-if="showActions" class="card border border-surface-200 dark:border-surface-700 mb-6">
      <div class="flex items-center gap-2 mb-4">
        <i class="pi pi-cog text-primary"></i>
        <span class="font-semibold">Thao tác thanh toán</span>
      </div>

      <div class="flex flex-wrap gap-3">
        <!-- Confirm Payment -->
        <Button
          v-if="canConfirmPayment"
          label="Xác nhận thanh toán"
          icon="pi pi-check"
          severity="success"
          :loading="processing"
          @click="confirmPayment"
        />

        <!-- Process Refund -->
        <Button
          v-if="canProcessRefund"
          label="Hoàn tiền"
          icon="pi pi-undo"
          severity="warn"
          outlined
          :loading="processing"
          @click="processRefund"
        />

        <!-- Update Status -->
        <Button
          v-if="canUpdateStatus"
          label="Cập nhật trạng thái"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="showUpdateDialog = true"
        />

        <!-- View Receipt -->
        <Button
          v-if="canViewReceipt"
          label="Xem hóa đơn"
          icon="pi pi-file-pdf"
          severity="info"
          outlined
          @click="viewReceipt"
        />
      </div>
    </div>

    <!-- Payment History -->
    <div v-if="paymentHistory?.length > 0" class="card border border-surface-200 dark:border-surface-700">
      <div class="flex items-center gap-2 mb-4">
        <i class="pi pi-history text-primary"></i>
        <span class="font-semibold">Lịch sử thanh toán</span>
      </div>

      <Timeline :value="paymentHistory" class="w-full">
        <template #marker="{ item }">
          <div class="flex items-center justify-center w-8 h-8 rounded-full border-2"
               :class="getHistoryMarkerClass(item.status)">
            <i :class="getHistoryIcon(item.status)" class="text-sm"></i>
          </div>
        </template>

        <template #content="{ item }">
          <div class="space-y-2">
            <div class="flex justify-between items-start">
              <div>
                <div class="font-semibold">{{ item.action }}</div>
                <div class="text-sm text-surface-600 dark:text-surface-400">
                  {{ item.description }}
                </div>
              </div>
              <div class="text-right">
                <div class="font-semibold">{{ formatCurrency(item.amount) }}</div>
                <div class="text-xs text-surface-500">
                  {{ formatDateTime(item.timestamp) }}
                </div>
              </div>
            </div>
            <div v-if="item.transactionId" class="text-xs text-surface-500 font-mono">
              Mã GD: {{ item.transactionId }}
            </div>
          </div>
        </template>
      </Timeline>
    </div>

    <!-- Update Status Dialog -->
    <Dialog
      v-model:visible="showUpdateDialog"
      modal
      header="Cập nhật trạng thái thanh toán"
      class="w-full max-w-md"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium mb-2">Trạng thái mới</label>
          <Dropdown
            v-model="newStatus"
            :options="availableStatuses"
            option-label="label"
            option-value="value"
            placeholder="Chọn trạng thái"
            class="w-full"
          />
        </div>

        <div>
          <label class="block text-sm font-medium mb-2">Ghi chú</label>
          <Textarea
            v-model="statusNote"
            rows="3"
            placeholder="Nhập ghi chú (tùy chọn)"
            class="w-full"
          />
        </div>
      </div>

      <template #footer>
        <Button
          label="Hủy"
          severity="secondary"
          outlined
          @click="showUpdateDialog = false"
        />
        <Button
          label="Cập nhật"
          :disabled="!newStatus"
          :loading="processing"
          @click="updatePaymentStatus"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { format } from 'date-fns'
import { vi } from 'date-fns/locale'

// Props
const props = defineProps({
  paymentStatus: {
    type: String,
    required: true
  },
  totalAmount: {
    type: Number,
    required: true
  },
  paidAmount: {
    type: Number,
    default: 0
  },
  paymentMethod: {
    type: String,
    default: null
  },
  transactionId: {
    type: String,
    default: null
  },
  paymentDate: {
    type: [String, Date],
    default: null
  },
  paymentHistory: {
    type: Array,
    default: () => []
  },
  showActions: {
    type: Boolean,
    default: true
  },
  showProgress: {
    type: Boolean,
    default: true
  },
  processing: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['confirm-payment', 'process-refund', 'update-status', 'view-receipt'])

// Composables
const toast = useToast()

// Reactive data
const showUpdateDialog = ref(false)
const newStatus = ref('')
const statusNote = ref('')

// Computed properties
const remainingAmount = computed(() => {
  return Math.max(0, props.totalAmount - props.paidAmount)
})

const paymentProgress = computed(() => {
  if (props.totalAmount === 0) return 0
  return Math.min(100, (props.paidAmount / props.totalAmount) * 100)
})

const statusInfo = computed(() => {
  const statusMap = {
    'CHUA_THANH_TOAN': {
      title: 'Chưa thanh toán',
      description: 'Đơn hàng chưa được thanh toán',
      label: 'Chưa thanh toán',
      severity: 'warn',
      icon: 'pi pi-clock'
    },
    'DA_THANH_TOAN': {
      title: 'Đã thanh toán',
      description: 'Đơn hàng đã được thanh toán đầy đủ',
      label: 'Đã thanh toán',
      severity: 'success',
      icon: 'pi pi-check-circle'
    },
    'THANH_TOAN_MOT_PHAN': {
      title: 'Thanh toán một phần',
      description: 'Đơn hàng đã được thanh toán một phần',
      label: 'Thanh toán một phần',
      severity: 'info',
      icon: 'pi pi-info-circle'
    },
    'HOAN_TIEN': {
      title: 'Đã hoàn tiền',
      description: 'Đơn hàng đã được hoàn tiền',
      label: 'Đã hoàn tiền',
      severity: 'secondary',
      icon: 'pi pi-undo'
    }
  }

  return statusMap[props.paymentStatus] || {
    title: 'Không xác định',
    description: 'Trạng thái thanh toán không xác định',
    label: 'Không xác định',
    severity: 'secondary',
    icon: 'pi pi-question-circle'
  }
})

const statusIconClass = computed(() => {
  const classMap = {
    'CHUA_THANH_TOAN': 'bg-orange-100 text-orange-600 border-orange-300',
    'DA_THANH_TOAN': 'bg-green-100 text-green-600 border-green-300',
    'THANH_TOAN_MOT_PHAN': 'bg-blue-100 text-blue-600 border-blue-300',
    'HOAN_TIEN': 'bg-gray-100 text-gray-600 border-gray-300'
  }

  return classMap[props.paymentStatus] || 'bg-gray-100 text-gray-600 border-gray-300'
})

const paymentMethodInfo = computed(() => {
  const methodMap = {
    'TIEN_MAT': {
      label: 'Tiền mặt',
      icon: 'pi pi-wallet'
    },
    'VNPAY': {
      label: 'VNPay',
      icon: 'pi pi-credit-card'
    },
    'MOMO': {
      label: 'MoMo',
      icon: 'pi pi-mobile'
    },
    'VIETQR': {
      label: 'VietQR',
      icon: 'pi pi-qrcode'
    }
  }

  return methodMap[props.paymentMethod] || {
    label: 'Không xác định',
    icon: 'pi pi-question-circle'
  }
})

const canConfirmPayment = computed(() => {
  return props.paymentStatus === 'CHUA_THANH_TOAN' || props.paymentStatus === 'THANH_TOAN_MOT_PHAN'
})

const canProcessRefund = computed(() => {
  return props.paymentStatus === 'DA_THANH_TOAN' && props.paidAmount > 0
})

const canUpdateStatus = computed(() => {
  return true // Allow status updates for all states
})

const canViewReceipt = computed(() => {
  return props.paymentStatus === 'DA_THANH_TOAN' || props.paymentStatus === 'HOAN_TIEN'
})

const availableStatuses = computed(() => {
  return [
    { label: 'Chưa thanh toán', value: 'CHUA_THANH_TOAN' },
    { label: 'Đã thanh toán', value: 'DA_THANH_TOAN' },
    { label: 'Thanh toán một phần', value: 'THANH_TOAN_MOT_PHAN' },
    { label: 'Hoàn tiền', value: 'HOAN_TIEN' }
  ]
})

// Methods
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount || 0)
}

const formatDateTime = (date) => {
  if (!date) return ''
  const dateObj = typeof date === 'string' ? new Date(date) : date
  return format(dateObj, 'dd/MM/yyyy HH:mm', { locale: vi })
}

const copyTransactionId = async () => {
  try {
    await navigator.clipboard.writeText(props.transactionId)
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã sao chép mã giao dịch',
      life: 2000
    })
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể sao chép mã giao dịch',
      life: 3000
    })
  }
}

const confirmPayment = () => {
  emit('confirm-payment')
}

const processRefund = () => {
  emit('process-refund')
}

const updatePaymentStatus = () => {
  emit('update-status', {
    status: newStatus.value,
    note: statusNote.value
  })
  showUpdateDialog.value = false
  newStatus.value = ''
  statusNote.value = ''
}

const viewReceipt = () => {
  emit('view-receipt')
}

const getHistoryMarkerClass = (status) => {
  const classMap = {
    'success': 'bg-green-100 border-green-300 text-green-600',
    'warning': 'bg-orange-100 border-orange-300 text-orange-600',
    'error': 'bg-red-100 border-red-300 text-red-600',
    'info': 'bg-blue-100 border-blue-300 text-blue-600'
  }

  return classMap[status] || 'bg-gray-100 border-gray-300 text-gray-600'
}

const getHistoryIcon = (status) => {
  const iconMap = {
    'success': 'pi pi-check',
    'warning': 'pi pi-exclamation-triangle',
    'error': 'pi pi-times',
    'info': 'pi pi-info-circle'
  }

  return iconMap[status] || 'pi pi-circle'
}
</script>

<style scoped>
.payment-status-container {
  @apply space-y-4;
}

.card {
  @apply p-6 bg-surface-0 dark:bg-surface-900 rounded-lg shadow-sm;
}

.status-icon {
  @apply w-16 h-16 flex items-center justify-center rounded-full border-2;
}

.payment-detail-item {
  @apply space-y-1;
}

.payment-detail-label {
  @apply text-sm font-medium text-surface-600 dark:text-surface-400;
}

.payment-detail-value {
  @apply text-base font-semibold text-surface-900 dark:text-surface-0;
}
</style>
