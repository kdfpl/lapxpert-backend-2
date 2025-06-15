<template>
  <div class="payment-workflow-container">
    <!-- Header -->
    <div class="flex items-center gap-2 mb-6">
      <i class="pi pi-credit-card text-primary text-xl"></i>
      <h3 class="text-xl font-semibold text-surface-900 dark:text-surface-0">
        Quy trình thanh toán
      </h3>
    </div>

    <!-- Workflow Steps -->
    <div class="workflow-steps mb-6">
      <div class="flex items-center justify-between">
        <div
          v-for="(step, index) in workflowSteps"
          :key="step.key"
          class="flex items-center"
          :class="{ 'flex-1': index < workflowSteps.length - 1 }"
        >
          <!-- Step Circle -->
          <div class="step-circle" :class="getStepClass(step.key)">
            <i :class="step.icon" class="text-sm"></i>
          </div>

          <!-- Step Label -->
          <div class="ml-3">
            <div class="text-sm font-medium" :class="getStepTextClass(step.key)">
              {{ step.label }}
            </div>
            <div class="text-xs text-surface-500">
              {{ step.description }}
            </div>
          </div>

          <!-- Connector Line -->
          <div
            v-if="index < workflowSteps.length - 1"
            class="flex-1 h-px mx-4"
            :class="getConnectorClass(step.key)"
          ></div>
        </div>
      </div>
    </div>

    <!-- Current Step Content -->
    <div class="workflow-content">
      <!-- Step 1: Payment Summary -->
      <div v-if="currentStep === 'summary'" class="step-content">
        <PaymentSummary
          :subtotal="orderData.subtotal"
          :shipping-fee="orderData.shippingFee"
          :voucher-discount="orderData.voucherDiscount"
          :campaign-discount="orderData.campaignDiscount"
          :total-amount="orderData.totalAmount"
          :total-items="orderData.totalItems"
          :applied-vouchers="orderData.appliedVouchers"
          :show-payment-status="false"
        />
      </div>

      <!-- Step 2: Payment Method Selection -->
      <div v-if="currentStep === 'method'" class="step-content">
        <PaymentMethod
          :order-type="orderData.orderType"
          :has-delivery="orderData.hasDelivery"
          :total-amount="orderData.totalAmount"
          v-model:selected-payment-method="selectedPaymentMethod"
          :processing="processing"
          @confirm="processPayment"
          @back="goToPreviousStep"
        />
      </div>

      <!-- Step 3: Payment Processing -->
      <div v-if="currentStep === 'processing'" class="step-content">
        <div class="text-center py-8">
          <ProgressSpinner />
          <h4 class="text-lg font-semibold mt-4 mb-2">Đang xử lý thanh toán</h4>
          <p class="text-surface-600 dark:text-surface-400">
            Vui lòng đợi trong giây lát...
          </p>

          <!-- Processing Details -->
          <div v-if="processingDetails" class="mt-6 max-w-md mx-auto">
            <div class="text-left space-y-2">
              <div v-for="detail in processingDetails" :key="detail.step" class="flex items-center gap-2">
                <i
                  :class="detail.completed ? 'pi pi-check text-green-600' : 'pi pi-spin pi-spinner text-blue-600'"
                  class="text-sm"
                ></i>
                <span class="text-sm" :class="detail.completed ? 'text-green-600' : 'text-surface-600'">
                  {{ detail.label }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 4: Payment Status -->
      <div v-if="currentStep === 'status'" class="step-content">
        <PaymentStatus
          :payment-status="paymentResult.status"
          :total-amount="orderData.totalAmount"
          :paid-amount="paymentResult.paidAmount"
          :payment-method="selectedPaymentMethod"
          :transaction-id="paymentResult.transactionId"
          :payment-date="paymentResult.paymentDate"
          :payment-history="paymentResult.history"
          :processing="processing"
          @confirm-payment="handleConfirmPayment"
          @process-refund="handleProcessRefund"
          @update-status="handleUpdateStatus"
          @view-receipt="handleViewReceipt"
        />
      </div>
    </div>

    <!-- Navigation Controls -->
    <div class="flex justify-between items-center mt-8">
      <Button
        v-if="canGoBack"
        label="Quay lại"
        icon="pi pi-arrow-left"
        severity="secondary"
        outlined
        :disabled="processing"
        @click="goToPreviousStep"
      />

      <div class="flex gap-3">
        <Button
          v-if="canSkip"
          label="Bỏ qua"
          severity="secondary"
          outlined
          :disabled="processing"
          @click="skipStep"
        />

        <Button
          v-if="canContinue"
          :label="getNextButtonLabel"
          icon="pi pi-arrow-right"
          :disabled="!canProceed || processing"
          :loading="processing"
          @click="goToNextStep"
        />

        <Button
          v-if="canComplete"
          label="Hoàn thành"
          icon="pi pi-check"
          severity="success"
          :disabled="processing"
          @click="completeWorkflow"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import PaymentSummary from './PaymentSummary.vue'
import PaymentMethod from './PaymentMethod.vue'
import PaymentStatus from './PaymentStatus.vue'
import orderApi from '@/apis/orderApi'

// Props
const props = defineProps({
  orderData: {
    type: Object,
    required: true
  },
  initialStep: {
    type: String,
    default: 'summary'
  }
})

// Emits
const emit = defineEmits(['payment-completed', 'workflow-cancelled', 'step-changed'])

// Composables
const toast = useToast()

// Reactive data
const currentStep = ref(props.initialStep)
const selectedPaymentMethod = ref('')
const processing = ref(false)
const paymentResult = ref({
  status: 'CHUA_THANH_TOAN',
  paidAmount: 0,
  transactionId: null,
  paymentDate: null,
  history: []
})

const processingDetails = ref([
  { step: 1, label: 'Xác thực thông tin thanh toán', completed: false },
  { step: 2, label: 'Xử lý giao dịch', completed: false },
  { step: 3, label: 'Cập nhật trạng thái đơn hàng', completed: false },
  { step: 4, label: 'Hoàn tất thanh toán', completed: false }
])

// Computed properties
const workflowSteps = computed(() => [
  {
    key: 'summary',
    label: 'Tổng kết',
    description: 'Xem chi tiết',
    icon: 'pi pi-calculator'
  },
  {
    key: 'method',
    label: 'Phương thức',
    description: 'Chọn thanh toán',
    icon: 'pi pi-credit-card'
  },
  {
    key: 'processing',
    label: 'Xử lý',
    description: 'Đang thanh toán',
    icon: 'pi pi-spin pi-spinner'
  },
  {
    key: 'status',
    label: 'Kết quả',
    description: 'Hoàn thành',
    icon: 'pi pi-check'
  }
])

const canGoBack = computed(() => {
  return ['method', 'status'].includes(currentStep.value) && !processing.value
})

const canContinue = computed(() => {
  return ['summary', 'method'].includes(currentStep.value)
})

const canSkip = computed(() => {
  return currentStep.value === 'summary'
})

const canComplete = computed(() => {
  return currentStep.value === 'status' && paymentResult.value.status === 'DA_THANH_TOAN'
})

const canProceed = computed(() => {
  if (currentStep.value === 'summary') return true
  if (currentStep.value === 'method') return !!selectedPaymentMethod.value
  return false
})

const getNextButtonLabel = computed(() => {
  const labelMap = {
    'summary': 'Chọn thanh toán',
    'method': 'Xác nhận thanh toán'
  }
  return labelMap[currentStep.value] || 'Tiếp tục'
})

// Watchers
watch(currentStep, (newStep) => {
  emit('step-changed', newStep)
})

// Methods
const getStepClass = (stepKey) => {
  const stepIndex = workflowSteps.value.findIndex(s => s.key === stepKey)
  const currentIndex = workflowSteps.value.findIndex(s => s.key === currentStep.value)

  if (stepIndex < currentIndex) {
    return 'bg-green-100 border-green-300 text-green-600' // Completed
  } else if (stepIndex === currentIndex) {
    return 'bg-primary-100 border-primary-300 text-primary-600' // Current
  } else {
    return 'bg-surface-100 border-surface-300 text-surface-400' // Pending
  }
}

const getStepTextClass = (stepKey) => {
  const stepIndex = workflowSteps.value.findIndex(s => s.key === stepKey)
  const currentIndex = workflowSteps.value.findIndex(s => s.key === currentStep.value)

  if (stepIndex <= currentIndex) {
    return 'text-surface-900 dark:text-surface-0'
  } else {
    return 'text-surface-400'
  }
}

const getConnectorClass = (stepKey) => {
  const stepIndex = workflowSteps.value.findIndex(s => s.key === stepKey)
  const currentIndex = workflowSteps.value.findIndex(s => s.key === currentStep.value)

  if (stepIndex < currentIndex) {
    return 'bg-green-300' // Completed
  } else {
    return 'bg-surface-200 dark:bg-surface-700' // Pending
  }
}

const goToNextStep = () => {
  const stepOrder = ['summary', 'method', 'processing', 'status']
  const currentIndex = stepOrder.indexOf(currentStep.value)

  if (currentIndex < stepOrder.length - 1) {
    if (currentStep.value === 'method') {
      // Start payment processing
      processPayment()
    } else {
      currentStep.value = stepOrder[currentIndex + 1]
    }
  }
}

const goToPreviousStep = () => {
  const stepOrder = ['summary', 'method', 'processing', 'status']
  const currentIndex = stepOrder.indexOf(currentStep.value)

  if (currentIndex > 0) {
    currentStep.value = stepOrder[currentIndex - 1]
  }
}

const skipStep = () => {
  if (currentStep.value === 'summary') {
    currentStep.value = 'method'
  }
}

const processPayment = async () => {
  if (!selectedPaymentMethod.value) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng chọn phương thức thanh toán',
      life: 3000
    })
    return
  }

  if (!props.orderData.orderId) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không tìm thấy ID đơn hàng để xử lý thanh toán',
      life: 3000
    })
    return
  }

  try {
    processing.value = true
    currentStep.value = 'processing'

    // Step 1: Validate payment information
    processingDetails.value[0].completed = true
    await new Promise(resolve => setTimeout(resolve, 500))

    // Step 2: Process payment based on method
    let paymentResponse

    if (selectedPaymentMethod.value === 'VNPAY') {
      // For VNPAY, we need to handle redirect to payment gateway
      paymentResponse = await orderApi.processVNPayPayment(props.orderData.orderId, {
        amount: props.orderData.totalAmount,
        orderInfo: `Thanh toán đơn hàng ${props.orderData.orderCode}`,
        returnUrl: window.location.origin + '/orders/payment-return'
      })

      if (paymentResponse.success && paymentResponse.data.paymentUrl) {
        // Redirect to VNPAY payment page
        window.location.href = paymentResponse.data.paymentUrl
        return
      }
    } else {
      // For TIEN_MAT (including cash on delivery), confirm payment directly
      paymentResponse = await orderApi.confirmPayment(
        props.orderData.orderId,
        selectedPaymentMethod.value,
        {
          transactionId: `TXN${Date.now()}`,
          paymentDate: new Date().toISOString()
        }
      )
    }

    processingDetails.value[1].completed = true
    await new Promise(resolve => setTimeout(resolve, 500))

    if (paymentResponse.success) {
      // Step 3: Update order status
      processingDetails.value[2].completed = true
      await new Promise(resolve => setTimeout(resolve, 500))

      // Step 4: Complete payment
      processingDetails.value[3].completed = true

      // Set successful payment result
      paymentResult.value = {
        status: 'DA_THANH_TOAN',
        paidAmount: props.orderData.totalAmount,
        transactionId: paymentResponse.data.transactionId || `TXN${Date.now()}`,
        paymentDate: new Date(),
        history: [
          {
            action: 'Thanh toán thành công',
            description: `Thanh toán qua ${getPaymentMethodLabel(selectedPaymentMethod.value)}`,
            amount: props.orderData.totalAmount,
            timestamp: new Date(),
            transactionId: paymentResponse.data.transactionId || `TXN${Date.now()}`,
            status: 'success'
          }
        ]
      }

      currentStep.value = 'status'

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Thanh toán đã được xử lý thành công',
        life: 3000
      })

    } else {
      throw new Error(paymentResponse.message || 'Lỗi xử lý thanh toán')
    }

  } catch (error) {
    console.error('Payment processing error:', error)

    paymentResult.value = {
      status: 'CHUA_THANH_TOAN',
      paidAmount: 0,
      transactionId: null,
      paymentDate: null,
      history: [
        {
          action: 'Thanh toán thất bại',
          description: error.message || 'Lỗi xử lý thanh toán',
          amount: 0,
          timestamp: new Date(),
          status: 'error'
        }
      ]
    }

    currentStep.value = 'status'

    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể xử lý thanh toán: ${error.message}`,
      life: 5000
    })
  } finally {
    processing.value = false
  }
}

const completeWorkflow = () => {
  emit('payment-completed', {
    paymentMethod: selectedPaymentMethod.value,
    paymentResult: paymentResult.value,
    orderData: props.orderData
  })
}

// Helper function to get payment method label
const getPaymentMethodLabel = (method) => {
  const labelMap = {
    'TIEN_MAT': 'Tiền mặt',
    'VNPAY': 'VNPay',
    'MOMO': 'MoMo',
    'VIETQR': 'VietQR'
  }
  return labelMap[method] || method
}

const handleConfirmPayment = async () => {
  if (!props.orderData.orderId) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không tìm thấy ID đơn hàng',
      life: 3000
    })
    return
  }

  try {
    const response = await orderApi.confirmPayment(
      props.orderData.orderId,
      selectedPaymentMethod.value
    )

    if (response.success) {
      // Update payment result with confirmed data
      paymentResult.value = {
        ...paymentResult.value,
        status: 'DA_THANH_TOAN',
        paidAmount: props.orderData.totalAmount,
        paymentDate: new Date()
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Xác nhận thanh toán thành công',
        life: 3000
      })
    } else {
      throw new Error(response.message)
    }
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể xác nhận thanh toán: ${error.message}`,
      life: 3000
    })
  }
}

const handleProcessRefund = async () => {
  if (!props.orderData.orderId) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không tìm thấy ID đơn hàng',
      life: 3000
    })
    return
  }

  try {
    const response = await orderApi.processRefund(
      props.orderData.orderId,
      paymentResult.value.paidAmount,
      'Hoàn tiền theo yêu cầu khách hàng'
    )

    if (response.success) {
      // Update payment result with refund data
      paymentResult.value = {
        ...paymentResult.value,
        status: 'HOAN_TIEN',
        history: [
          ...paymentResult.value.history,
          {
            action: 'Hoàn tiền thành công',
            description: `Hoàn tiền ${formatCurrency(paymentResult.value.paidAmount)}`,
            amount: paymentResult.value.paidAmount,
            timestamp: new Date(),
            status: 'success'
          }
        ]
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Hoàn tiền thành công',
        life: 3000
      })
    } else {
      throw new Error(response.message)
    }
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể hoàn tiền: ${error.message}`,
      life: 3000
    })
  }
}

const handleUpdateStatus = async (statusUpdate) => {
  if (!props.orderData.orderId) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không tìm thấy ID đơn hàng',
      life: 3000
    })
    return
  }

  try {
    const response = await orderApi.updatePaymentStatus(
      props.orderData.orderId,
      statusUpdate.status,
      statusUpdate.note
    )

    if (response.success) {
      // Update payment result with new status
      paymentResult.value = {
        ...paymentResult.value,
        status: statusUpdate.status,
        history: [
          ...paymentResult.value.history,
          {
            action: 'Cập nhật trạng thái',
            description: `Trạng thái thanh toán: ${statusUpdate.status}`,
            amount: 0,
            timestamp: new Date(),
            status: 'info'
          }
        ]
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Cập nhật trạng thái thanh toán thành công',
        life: 3000
      })
    } else {
      throw new Error(response.message)
    }
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể cập nhật trạng thái: ${error.message}`,
      life: 3000
    })
  }
}

const handleViewReceipt = async () => {
  if (!props.orderData.orderId) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không tìm thấy ID đơn hàng',
      life: 3000
    })
    return
  }

  try {
    const response = await orderApi.printOrderReceipt(props.orderData.orderId)

    if (response.success) {
      // Create blob URL and download
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `receipt-${props.orderData.orderCode || props.orderData.orderId}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Hóa đơn đã được tải xuống',
        life: 3000
      })
    } else {
      throw new Error(response.message)
    }
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể tải hóa đơn: ${error.message}`,
      life: 3000
    })
  }
}

// Helper function to format currency
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount || 0)
}
</script>

<style scoped>
.payment-workflow-container {
  @apply space-y-6;
}

.workflow-steps {
  @apply p-6 bg-surface-50 dark:bg-surface-800 rounded-lg;
}

.step-circle {
  @apply w-10 h-10 flex items-center justify-center rounded-full border-2 flex-shrink-0;
}

.step-content {
  @apply min-h-96;
}
</style>
