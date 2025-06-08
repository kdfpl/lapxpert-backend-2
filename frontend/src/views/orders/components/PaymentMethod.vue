<template>
  <div class="payment-method-container">
    <!-- Header -->
    <div class="flex items-center gap-2 mb-6">
      <i class="pi pi-credit-card text-primary text-xl"></i>
      <h3 class="text-xl font-semibold text-surface-900 dark:text-surface-0">
        Phương thức thanh toán
      </h3>
    </div>

    <!-- Payment Method Selection -->
    <div class="space-y-4">
      <div
        v-for="method in availablePaymentMethods"
        :key="method.value"
        class="payment-method-option"
        :class="{
          'selected': selectedMethod === method.value,
          'disabled': !method.available
        }"
        @click="selectPaymentMethod(method)"
      >
        <div class="flex items-center gap-4">
          <!-- Radio Button -->
          <RadioButton
            v-model="selectedMethod"
            :value="method.value"
            :disabled="!method.available"
            :input-id="method.value"
          />

          <!-- Method Icon -->
          <div class="payment-icon">
            <i :class="method.icon" class="text-2xl"></i>
          </div>

          <!-- Method Details -->
          <div class="flex-1">
            <label :for="method.value" class="payment-label">
              {{ method.label }}
            </label>
            <p class="payment-description">
              {{ method.description }}
            </p>

            <!-- Additional Info -->
            <div v-if="method.additionalInfo" class="payment-additional-info">
              <i class="pi pi-info-circle text-blue-500 mr-1"></i>
              {{ method.additionalInfo }}
            </div>

            <!-- Unavailable Reason -->
            <div v-if="!method.available && method.unavailableReason" class="payment-unavailable">
              <i class="pi pi-exclamation-triangle text-orange-500 mr-1"></i>
              {{ method.unavailableReason }}
            </div>
          </div>

          <!-- Method Badge -->
          <div v-if="method.badge" class="payment-badge">
            <Badge :value="method.badge.text" :severity="method.badge.severity" />
          </div>
        </div>
      </div>
    </div>

    <!-- Payment Instructions -->
    <div v-if="selectedMethodInfo" class="card border border-surface-200 dark:border-surface-700 mt-6">
      <div class="flex items-center gap-2 mb-4">
        <i class="pi pi-info-circle text-primary"></i>
        <span class="font-semibold">Hướng dẫn thanh toán</span>
      </div>

      <div class="space-y-3">
        <div v-for="instruction in selectedMethodInfo.instructions" :key="instruction" class="flex items-start gap-2">
          <i class="pi pi-check-circle text-green-500 mt-1 text-sm"></i>
          <span class="text-sm text-surface-600 dark:text-surface-400">{{ instruction }}</span>
        </div>
      </div>

      <!-- Processing Time -->
      <div v-if="selectedMethodInfo.processingTime" class="mt-4 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
        <div class="flex items-center gap-2">
          <i class="pi pi-clock text-blue-600"></i>
          <span class="text-sm font-medium text-blue-800 dark:text-blue-200">
            Thời gian xử lý: {{ selectedMethodInfo.processingTime }}
          </span>
        </div>
      </div>
    </div>

    <!-- Validation Errors -->
    <div v-if="validationError" class="mt-4">
      <Message severity="error" :closable="false">
        {{ validationError }}
      </Message>
    </div>

    <!-- Action Buttons -->
    <div v-if="showActions" class="flex justify-between items-center mt-6">
      <Button
        label="Quay lại"
        icon="pi pi-arrow-left"
        severity="secondary"
        outlined
        @click="$emit('back')"
      />

      <Button
        label="Xác nhận thanh toán"
        icon="pi pi-check"
        :disabled="!selectedMethod || !isValidSelection"
        :loading="processing"
        @click="confirmPayment"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'

// Props
const props = defineProps({
  orderType: {
    type: String,
    required: true // 'TAI_QUAY' or 'ONLINE'
  },
  hasDelivery: {
    type: Boolean,
    default: false
  },
  totalAmount: {
    type: Number,
    required: true
  },
  selectedPaymentMethod: {
    type: String,
    default: null
  },
  showActions: {
    type: Boolean,
    default: true
  },
  processing: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['update:selectedPaymentMethod', 'confirm', 'back'])

// Composables
const toast = useToast()

// Reactive data
const selectedMethod = ref(props.selectedPaymentMethod)
const validationError = ref('')

// Computed properties
const availablePaymentMethods = computed(() => {
  const methods = []

  // TIEN_MAT - Only for TAI_QUAY orders
  if (props.orderType === 'TAI_QUAY') {
    methods.push({
      value: 'TIEN_MAT',
      label: 'Tiền mặt',
      description: 'Thanh toán bằng tiền mặt tại quầy',
      icon: 'pi pi-wallet',
      available: true,
      badge: { text: 'Tức thì', severity: 'success' },
      additionalInfo: 'Thanh toán ngay lập tức, đơn hàng hoàn thành'
    })
  }

  // COD - Available when delivery is enabled
  if (props.hasDelivery) {
    methods.push({
      value: 'COD',
      label: 'Thanh toán khi nhận hàng',
      description: 'Thanh toán khi giao hàng',
      icon: 'pi pi-money-bill',
      available: true,
      badge: { text: 'Khi giao', severity: 'info' },
      additionalInfo: 'Thanh toán khi nhận hàng từ shipper'
    })
  } else if (props.orderType === 'ONLINE') {
    methods.push({
      value: 'COD',
      label: 'Thanh toán khi nhận hàng',
      description: 'Thanh toán khi giao hàng',
      icon: 'pi pi-money-bill',
      available: false,
      unavailableReason: 'Cần bật giao hàng để sử dụng COD'
    })
  }

  // VNPAY - Available for both order types
  methods.push({
    value: 'VNPAY',
    label: 'Chuyển khoản',
    description: 'Thanh toán qua ví điện tử VNPay',
    icon: 'pi pi-credit-card',
    available: true,
    badge: { text: 'Trực tuyến', severity: 'primary' },
    additionalInfo: 'Hỗ trợ thẻ ATM, Internet Banking, QR Code'
  })

  return methods
})

const selectedMethodInfo = computed(() => {
  if (!selectedMethod.value) return null

  const methodDetails = {
    'TIEN_MAT': {
      instructions: [
        'Nhân viên thu tiền mặt từ khách hàng',
        'Kiểm tra và đếm tiền cẩn thận',
        'Đưa tiền thừa (nếu có) cho khách hàng',
        'In hóa đơn và giao cho khách hàng'
      ],
      processingTime: 'Ngay lập tức'
    },
    'COD': {
      instructions: [
        'Đơn hàng sẽ được chuẩn bị và đóng gói',
        'Shipper sẽ liên hệ trước khi giao hàng',
        'Khách hàng thanh toán khi nhận hàng',
        'Kiểm tra hàng hóa trước khi thanh toán'
      ],
      processingTime: '1-3 ngày làm việc'
    },
    'VNPAY': {
      instructions: [
        'Chuyển hướng đến cổng thanh toán VNPay',
        'Chọn phương thức thanh toán (ATM, Internet Banking, QR)',
        'Nhập thông tin thanh toán theo hướng dẫn',
        'Xác nhận giao dịch và hoàn tất thanh toán'
      ],
      processingTime: '1-5 phút'
    }
  }

  return methodDetails[selectedMethod.value]
})

const isValidSelection = computed(() => {
  return selectedMethod.value && availablePaymentMethods.value.find(m => m.value === selectedMethod.value)?.available
})

// Watchers
watch(selectedMethod, (newValue) => {
  emit('update:selectedPaymentMethod', newValue)
  validationError.value = ''
})

watch(() => props.selectedPaymentMethod, (newValue) => {
  selectedMethod.value = newValue
})

// Methods
const selectPaymentMethod = (method) => {
  if (!method.available) {
    toast.add({
      severity: 'warn',
      summary: 'Không khả dụng',
      detail: method.unavailableReason || 'Phương thức thanh toán này không khả dụng',
      life: 3000
    })
    return
  }

  selectedMethod.value = method.value
}

const confirmPayment = () => {
  if (!selectedMethod.value) {
    validationError.value = 'Vui lòng chọn phương thức thanh toán'
    return
  }

  if (!isValidSelection.value) {
    validationError.value = 'Phương thức thanh toán đã chọn không khả dụng'
    return
  }

  emit('confirm', selectedMethod.value)
}
</script>

<style scoped>
.payment-method-container {
  @apply space-y-4;
}

.payment-method-option {
  @apply p-4 border border-surface-200 dark:border-surface-700 rounded-lg cursor-pointer transition-all duration-200;
  @apply hover:border-primary-300 hover:bg-primary-50 dark:hover:bg-primary-900/10;
}

.payment-method-option.selected {
  @apply border-primary-500 bg-primary-50 dark:bg-primary-900/20;
}

.payment-method-option.disabled {
  @apply opacity-60 cursor-not-allowed;
  @apply hover:border-surface-200 hover:bg-transparent dark:hover:bg-transparent;
}

.payment-icon {
  @apply w-12 h-12 flex items-center justify-center bg-surface-100 dark:bg-surface-800 rounded-lg;
}

.payment-label {
  @apply block font-semibold text-surface-900 dark:text-surface-0 cursor-pointer;
}

.payment-description {
  @apply text-sm text-surface-600 dark:text-surface-400 mt-1;
}

.payment-additional-info {
  @apply text-xs text-blue-600 dark:text-blue-400 mt-2 flex items-center;
}

.payment-unavailable {
  @apply text-xs text-orange-600 dark:text-orange-400 mt-2 flex items-center;
}

.payment-badge {
  @apply flex-shrink-0;
}

.card {
  @apply p-4 bg-surface-0 dark:bg-surface-900 rounded-lg shadow-sm;
}
</style>
