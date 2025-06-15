<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    modal
    header="Thanh toán hỗn hợp"
    :style="{ width: '700px' }"
    :closable="!processing"
    :dismissableMask="!processing"
  >
    <div class="space-y-6">
      <!-- Order Summary -->
      <div class="border rounded-lg p-4 bg-surface-50">
        <h4 class="font-semibold text-lg mb-3 flex items-center gap-2">
          <i class="pi pi-calculator text-primary"></i>
          Tổng tiền cần thanh toán
        </h4>
        <div class="text-center">
          <div class="text-3xl font-bold text-primary">{{ formatCurrency(totalAmount) }}</div>
          <div class="text-sm text-surface-500 mt-1">Tổng cộng</div>
        </div>
      </div>

      <!-- Payment Methods -->
      <div class="space-y-4">
        <h4 class="font-semibold text-lg flex items-center gap-2">
          <i class="pi pi-credit-card text-primary"></i>
          Phương thức thanh toán
        </h4>

        <div
          v-for="(payment, index) in payments"
          :key="index"
          class="border rounded-lg p-4 bg-white"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-3">
              <span class="font-medium">Thanh toán {{ index + 1 }}</span>
            </div>
            <Button
              v-if="payments.length > 1"
              icon="pi pi-trash"
              text
              rounded
              size="small"
              severity="danger"
              @click="removePayment(index)"
              :disabled="processing"
            />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Payment Method Selection -->
            <div>
              <label class="block text-sm font-medium mb-2">Phương thức</label>
              <Dropdown
                v-model="payment.method"
                :options="availablePaymentMethods"
                optionLabel="label"
                optionValue="value"
                placeholder="Chọn phương thức"
                class="w-full"
                :disabled="processing"
                @change="onPaymentMethodChange(index)"
              >
                <template #option="{ option }">
                  <div class="flex items-center gap-2">
                    <i :class="option.icon" class="text-primary"></i>
                    <span>{{ option.label }}</span>
                  </div>
                </template>
              </Dropdown>
            </div>

            <!-- Payment Amount -->
            <div>
              <label class="block text-sm font-medium mb-2">Số tiền</label>
              <InputNumber
                v-model="payment.amount"
                :min="0"
                :max="remainingAmount + payment.amount"
                mode="currency"
                currency="VND"
                locale="vi-VN"
                class="w-full"
                :disabled="processing"
                @input="validatePaymentAmounts"
              />
            </div>
          </div>

          <!-- Payment Instructions -->
          <div v-if="payment.method && getPaymentInstructions(payment.method)" class="mt-3 p-3 bg-blue-50 rounded-lg">
            <div class="text-sm text-blue-800">
              <div class="font-medium mb-2">Hướng dẫn thanh toán:</div>
              <ul class="list-disc list-inside space-y-1">
                <li v-for="instruction in getPaymentInstructions(payment.method)" :key="instruction">
                  {{ instruction }}
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Add Payment Button -->
        <Button
          v-if="payments.length < maxPayments && remainingAmount > 0"
          label="Thêm phương thức thanh toán"
          icon="pi pi-plus"
          outlined
          @click="addPayment"
          :disabled="processing"
          class="w-full"
        />
      </div>

      <!-- Payment Summary -->
      <div class="border rounded-lg p-4 bg-primary/5">
        <h4 class="font-semibold text-lg mb-3">Tổng kết thanh toán</h4>
        <div class="space-y-2">
          <div class="flex justify-between">
            <span>Tổng tiền cần thanh toán:</span>
            <span class="font-medium">{{ formatCurrency(totalAmount) }}</span>
          </div>
          <div class="flex justify-between">
            <span>Đã phân bổ:</span>
            <span class="font-medium">{{ formatCurrency(allocatedAmount) }}</span>
          </div>
          <div class="flex justify-between" :class="remainingAmount > 0 ? 'text-red-600' : 'text-green-600'">
            <span>Còn lại:</span>
            <span class="font-medium">{{ formatCurrency(remainingAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- Validation Errors -->
      <div v-if="validationErrors.length > 0" class="space-y-2">
        <div
          v-for="error in validationErrors"
          :key="error"
          class="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm"
        >
          {{ error }}
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <Button
          label="Hủy"
          icon="pi pi-times"
          severity="secondary"
          outlined
          @click="$emit('update:visible', false)"
          :disabled="processing"
        />
        <Button
          label="Xác nhận thanh toán"
          icon="pi pi-check"
          severity="success"
          @click="confirmMixedPayment"
          :loading="processing"
          :disabled="!isValidPayment"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'

// PrimeVue Components
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import InputNumber from 'primevue/inputnumber'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  totalAmount: {
    type: Number,
    required: true
  },
  orderType: {
    type: String,
    required: true
  },
  hasDelivery: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['update:visible', 'confirm'])

// Composables
const toast = useToast()

// State
const processing = ref(false)
const maxPayments = ref(3)
const payments = ref([
  { method: null, amount: props.totalAmount }
])
const validationErrors = ref([])

// Available payment methods based on order type and delivery
const availablePaymentMethods = computed(() => {
  const methods = []

  // TIEN_MAT - Only for TAI_QUAY orders
  if (props.orderType === 'TAI_QUAY') {
    methods.push({
      value: 'TIEN_MAT',
      label: 'Tiền mặt',
      icon: 'pi pi-wallet'
    })
  }

  // TIEN_MAT for delivery - Only when delivery is enabled (former COD)
  if (props.hasDelivery && props.orderType === 'ONLINE') {
    methods.push({
      value: 'TIEN_MAT',
      label: 'Tiền mặt khi giao hàng',
      icon: 'pi pi-money-bill'
    })
  }

  // Digital payment methods - available for all
  methods.push(
    {
      value: 'VNPAY',
      label: 'VNPay',
      icon: 'pi pi-credit-card'
    },
    {
      value: 'MOMO',
      label: 'MoMo',
      icon: 'pi pi-mobile'
    },
    {
      value: 'VIETQR',
      label: 'VietQR',
      icon: 'pi pi-qrcode'
    }
  )

  return methods
})

// Computed properties
const allocatedAmount = computed(() => {
  return payments.value.reduce((sum, payment) => sum + (payment.amount || 0), 0)
})

const remainingAmount = computed(() => {
  return props.totalAmount - allocatedAmount.value
})

const isValidPayment = computed(() => {
  return validationErrors.value.length === 0 &&
         remainingAmount.value === 0 &&
         payments.value.every(p => p.method && p.amount > 0)
})

// Methods
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

const getPaymentMethodLabel = (method) => {
  const methodObj = availablePaymentMethods.value.find(m => m.value === method)
  return methodObj?.label || method
}

const getPaymentInstructions = (method) => {
  const instructions = {
    'TIEN_MAT': [
      'Nhân viên thu tiền mặt từ khách hàng (POS)',
      'Hoặc shipper thu tiền khi giao hàng (Online)',
      'Kiểm tra và đếm tiền cẩn thận'
    ],
    'VNPAY': [
      'Chuyển hướng đến cổng thanh toán VNPay',
      'Chọn phương thức và hoàn tất thanh toán'
    ],
    'MOMO': [
      'Chuyển hướng đến ứng dụng MoMo',
      'Xác nhận thanh toán trong ứng dụng'
    ],
    'VIETQR': [
      'Quét mã QR bằng ứng dụng ngân hàng',
      'Xác nhận chuyển khoản'
    ]
  }
  return instructions[method] || []
}

const addPayment = () => {
  if (payments.value.length < maxPayments.value) {
    payments.value.push({
      method: null,
      amount: remainingAmount.value
    })
  }
}

const removePayment = (index) => {
  if (payments.value.length > 1) {
    payments.value.splice(index, 1)
    validatePaymentAmounts()
  }
}

const onPaymentMethodChange = (index) => {
  validatePaymentAmounts()
}

const validatePaymentAmounts = () => {
  validationErrors.value = []

  // Check if all payments have methods
  const missingMethods = payments.value.some(p => !p.method)
  if (missingMethods) {
    validationErrors.value.push('Vui lòng chọn phương thức thanh toán cho tất cả các khoản')
  }

  // Check if all payments have amounts
  const missingAmounts = payments.value.some(p => !p.amount || p.amount <= 0)
  if (missingAmounts) {
    validationErrors.value.push('Vui lòng nhập số tiền cho tất cả các khoản thanh toán')
  }

  // Check if total matches
  if (remainingAmount.value !== 0) {
    if (remainingAmount.value > 0) {
      validationErrors.value.push(`Còn thiếu ${formatCurrency(remainingAmount.value)} chưa được phân bổ`)
    } else {
      validationErrors.value.push(`Đã phân bổ thừa ${formatCurrency(Math.abs(remainingAmount.value))}`)
    }
  }

  // Check for duplicate methods
  const methods = payments.value.map(p => p.method).filter(Boolean)
  const uniqueMethods = [...new Set(methods)]
  if (methods.length !== uniqueMethods.length) {
    validationErrors.value.push('Không thể sử dụng cùng một phương thức thanh toán nhiều lần')
  }
}

const confirmMixedPayment = async () => {
  if (!isValidPayment.value) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng kiểm tra lại thông tin thanh toán',
      life: 3000
    })
    return
  }

  processing.value = true
  try {
    // Emit the payment configuration
    emit('confirm', {
      payments: payments.value.map(p => ({
        method: p.method,
        amount: p.amount
      })),
      totalAmount: props.totalAmount
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Có lỗi xảy ra khi xử lý thanh toán',
      life: 3000
    })
  } finally {
    processing.value = false
  }
}

// Watchers
watch(() => props.visible, (newValue) => {
  if (newValue) {
    // Reset state when dialog opens
    payments.value = [{ method: null, amount: props.totalAmount }]
    validationErrors.value = []
  }
})

watch(() => payments.value, () => {
  validatePaymentAmounts()
}, { deep: true })
</script>

<style scoped>
.payment-method-option {
  @apply p-3 border border-surface-200 rounded-lg cursor-pointer transition-all;
  @apply hover:border-primary-300 hover:bg-primary-50;
}

.payment-method-option.selected {
  @apply border-primary-500 bg-primary-50;
}
</style>
