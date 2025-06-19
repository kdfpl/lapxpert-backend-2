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
          <!-- Payment Method Specific Branding -->
          <div class="mb-4">
            <div v-if="selectedPaymentMethod === 'MOMO'" class="flex items-center justify-center gap-3 mb-4">
              <div class="w-12 h-12 bg-pink-100 rounded-full flex items-center justify-center">
                <i class="pi pi-wallet text-pink-600 text-xl"></i>
              </div>
              <div class="text-left">
                <h4 class="text-lg font-semibold text-pink-600">MoMo</h4>
                <p class="text-sm text-surface-600">Ví điện tử MoMo</p>
              </div>
            </div>
            <div v-else-if="selectedPaymentMethod === 'VNPAY'" class="flex items-center justify-center gap-3 mb-4">
              <div class="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                <i class="pi pi-credit-card text-blue-600 text-xl"></i>
              </div>
              <div class="text-left">
                <h4 class="text-lg font-semibold text-blue-600">VNPay</h4>
                <p class="text-sm text-surface-600">Cổng thanh toán VNPay</p>
              </div>
            </div>
            <div v-else class="flex items-center justify-center gap-3 mb-4">
              <div class="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
                <i class="pi pi-money-bill text-green-600 text-xl"></i>
              </div>
              <div class="text-left">
                <h4 class="text-lg font-semibold text-green-600">Tiền mặt</h4>
                <p class="text-sm text-surface-600">Thanh toán trực tiếp</p>
              </div>
            </div>
          </div>

          <ProgressSpinner />
          <h4 class="text-lg font-semibold mt-4 mb-2">
            {{ getProcessingTitle() }}
          </h4>
          <p class="text-surface-600 dark:text-surface-400">
            {{ getProcessingDescription() }}
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

          <!-- Payment Method Specific Instructions -->
          <div v-if="selectedPaymentMethod === 'MOMO'" class="mt-6 p-4 bg-pink-50 border border-pink-200 rounded-lg max-w-md mx-auto">
            <div class="flex items-start gap-3">
              <i class="pi pi-info-circle text-pink-600 mt-0.5"></i>
              <div class="text-left">
                <h5 class="font-semibold text-pink-800 mb-2">Hướng dẫn thanh toán MoMo</h5>
                <ul class="text-sm text-pink-700 space-y-1">
                  <li>• Bạn sẽ được chuyển đến ứng dụng MoMo</li>
                  <li>• Xác nhận thông tin đơn hàng</li>
                  <li>• Nhập mã PIN hoặc xác thực sinh trắc học</li>
                  <li>• Hoàn tất thanh toán</li>
                </ul>
              </div>
            </div>
          </div>

          <div v-else-if="selectedPaymentMethod === 'VNPAY'" class="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg max-w-md mx-auto">
            <div class="flex items-start gap-3">
              <i class="pi pi-info-circle text-blue-600 mt-0.5"></i>
              <div class="text-left">
                <h5 class="font-semibold text-blue-800 mb-2">Hướng dẫn thanh toán VNPay</h5>
                <ul class="text-sm text-blue-700 space-y-1">
                  <li>• Bạn sẽ được chuyển đến trang VNPay</li>
                  <li>• Chọn ngân hàng hoặc ví điện tử</li>
                  <li>• Nhập thông tin thanh toán</li>
                  <li>• Xác nhận giao dịch</li>
                </ul>
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
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import PaymentSummary from './PaymentSummary.vue'
import PaymentMethod from './PaymentMethod.vue'
import PaymentStatus from './PaymentStatus.vue'
import orderApi from '@/apis/orderApi'
import { useRealTimeOrderManagement } from '@/composables/useRealTimeOrderManagement'

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
const {
  isConnected,
  connectionStatus,
  sendMessage,
  processIncomingMessage,
  setIntegrationCallback
} = useRealTimeOrderManagement()

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

// WebSocket subscription for real-time payment updates
let orderSubscription = null

// WebSocket message handler for payment notifications
const handlePaymentNotification = (message) => {
  try {
    console.log('📨 Received payment notification:', message)

    // Check if this message is for our current order
    if (message.topic && message.topic.includes(`/topic/hoa-don/${props.orderData.orderId}`)) {
      const { status, data } = message

      // Handle different payment notification types
      switch (status) {
        case 'PAYMENT_INITIATED':
          handlePaymentInitiated(data)
          break
        case 'PAYMENT_SUCCESS':
          handlePaymentSuccess(data)
          break
        case 'PAYMENT_FAILED':
          handlePaymentFailed(data)
          break
        case 'PAYMENT_STATUS_CHECKED':
          handlePaymentStatusChecked(data)
          break
        default:
          console.log('📨 Unknown payment notification status:', status)
      }
    }
  } catch (error) {
    console.error('Error handling payment notification:', error)
  }
}

// Payment notification handlers
const handlePaymentInitiated = (data) => {
  console.log('💳 Payment initiated:', data)

  const paymentMethodName = getPaymentMethodLabel(selectedPaymentMethod.value)

  toast.add({
    severity: 'info',
    summary: `Thanh toán ${paymentMethodName} đã khởi tạo`,
    detail: `Đang chuyển hướng đến ${paymentMethodName}...`,
    life: 3000
  })
}

const handlePaymentSuccess = (data) => {
  console.log('✅ Payment successful:', data)

  // Update payment result with real-time data
  paymentResult.value = {
    status: 'DA_THANH_TOAN',
    paidAmount: props.orderData.totalAmount,
    transactionId: data.transactionId || data.transactionRef || `TXN${Date.now()}`,
    paymentDate: new Date(),
    history: [
      ...paymentResult.value.history,
      {
        action: 'Thanh toán thành công (Real-time)',
        description: `Thanh toán qua ${getPaymentMethodLabel(selectedPaymentMethod.value)} đã được xác nhận`,
        amount: props.orderData.totalAmount,
        timestamp: new Date(),
        transactionId: data.transactionId || data.transactionRef,
        status: 'success'
      }
    ]
  }

  // Complete processing steps
  processingDetails.value.forEach(detail => detail.completed = true)

  // Move to status step if still processing
  if (currentStep.value === 'processing') {
    currentStep.value = 'status'
  }

  toast.add({
    severity: 'success',
    summary: 'Thanh toán thành công!',
    detail: 'Giao dịch đã được xử lý thành công',
    life: 5000
  })

  // Auto-refresh order data after successful payment
  setTimeout(() => {
    refreshOrderData()
  }, 2000)
}

const handlePaymentFailed = (data) => {
  console.log('❌ Payment failed:', data)

  // Get payment method specific error message
  const errorMessage = getPaymentErrorMessage(data, selectedPaymentMethod.value)
  const paymentMethodName = getPaymentMethodLabel(selectedPaymentMethod.value)

  // Update payment result with failure data
  paymentResult.value = {
    status: 'CHUA_THANH_TOAN',
    paidAmount: 0,
    transactionId: data.transactionId || data.transactionRef || null,
    paymentDate: null,
    history: [
      ...paymentResult.value.history,
      {
        action: `Thanh toán ${paymentMethodName} thất bại (Real-time)`,
        description: errorMessage,
        amount: 0,
        timestamp: new Date(),
        transactionId: data.transactionId || data.transactionRef,
        status: 'error'
      }
    ]
  }

  // Move to status step if still processing
  if (currentStep.value === 'processing') {
    currentStep.value = 'status'
  }

  toast.add({
    severity: 'error',
    summary: `Thanh toán ${paymentMethodName} thất bại`,
    detail: errorMessage,
    life: 5000
  })
}

const handlePaymentStatusChecked = (data) => {
  console.log('🔍 Payment status checked:', data)

  toast.add({
    severity: 'info',
    summary: 'Kiểm tra trạng thái',
    detail: 'Đã cập nhật trạng thái thanh toán',
    life: 3000
  })
}

// Auto-refresh order data function
const refreshOrderData = async () => {
  try {
    console.log('🔄 Refreshing order data after payment...')

    // Emit event to parent component to refresh order data
    emit('payment-completed', {
      paymentMethod: selectedPaymentMethod.value,
      paymentResult: paymentResult.value,
      orderData: props.orderData,
      shouldRefresh: true
    })
  } catch (error) {
    console.error('Error refreshing order data:', error)
  }
}

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
    } else if (selectedPaymentMethod.value === 'MOMO') {
      // For MoMo, we need to handle redirect to payment gateway
      paymentResponse = await orderApi.processMoMoPayment(props.orderData.orderId, {
        amount: props.orderData.totalAmount,
        orderInfo: `Thanh toán đơn hàng ${props.orderData.orderCode}`,
        returnUrl: window.location.origin + '/orders/payment-return'
      })

      if (paymentResponse.success && paymentResponse.data.paymentUrl) {
        // Redirect to MoMo payment page
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

// Helper function to get processing title based on payment method
const getProcessingTitle = () => {
  const titleMap = {
    'MOMO': 'Đang xử lý thanh toán MoMo',
    'VNPAY': 'Đang xử lý thanh toán VNPay',
    'VIETQR': 'Đang xử lý thanh toán VietQR',
    'TIEN_MAT': 'Đang xử lý thanh toán tiền mặt'
  }
  return titleMap[selectedPaymentMethod.value] || 'Đang xử lý thanh toán'
}

// Helper function to get processing description based on payment method
const getProcessingDescription = () => {
  const descriptionMap = {
    'MOMO': 'Đang kết nối với ví MoMo. Vui lòng đợi trong giây lát...',
    'VNPAY': 'Đang kết nối với cổng thanh toán VNPay. Vui lòng đợi trong giây lát...',
    'VIETQR': 'Đang tạo mã QR thanh toán. Vui lòng đợi trong giây lát...',
    'TIEN_MAT': 'Đang xác nhận thanh toán tiền mặt. Vui lòng đợi trong giây lát...'
  }
  return descriptionMap[selectedPaymentMethod.value] || 'Vui lòng đợi trong giây lát...'
}

// Helper function to get payment-specific error messages in Vietnamese
const getPaymentErrorMessage = (data, paymentMethod) => {
  // If there's a specific error message from the backend, use it
  if (data.errorMessage) {
    return data.errorMessage
  }

  // MoMo specific error handling
  if (paymentMethod === 'MOMO') {
    const resultCode = data.resultCode || data.errorCode

    const momoErrors = {
      '1': 'Giao dịch MoMo thất bại. Vui lòng kiểm tra lại thông tin.',
      '2': 'Giao dịch MoMo bị từ chối. Vui lòng thử lại sau.',
      '3': 'Giao dịch MoMo đã bị hủy bởi người dùng.',
      '1000': 'Giao dịch MoMo đang chờ xác nhận. Vui lòng kiểm tra ứng dụng MoMo.',
      '1001': 'Tài khoản MoMo chưa được kích hoạt. Vui lòng kích hoạt tài khoản.',
      '1002': 'Tài khoản MoMo đã bị khóa. Vui lòng liên hệ MoMo để được hỗ trợ.',
      '1003': 'Tài khoản MoMo chưa đăng ký dịch vụ thanh toán.',
      '1004': 'Số tiền vượt quá hạn mức thanh toán MoMo.',
      '1005': 'Liên kết thanh toán MoMo đã hết hạn. Vui lòng tạo giao dịch mới.',
      '1006': 'Người dùng từ chối xác nhận thanh toán trên ứng dụng MoMo.',
      '1007': 'Ứng dụng MoMo cần được cập nhật lên phiên bản mới nhất.'
    }

    if (momoErrors[resultCode]) {
      return momoErrors[resultCode]
    }

    return 'Thanh toán MoMo không thành công. Vui lòng kiểm tra ứng dụng MoMo và thử lại.'
  }

  // VNPay specific error handling
  if (paymentMethod === 'VNPAY') {
    const responseCode = data.responseCode || data.errorCode

    const vnpayErrors = {
      '01': 'Giao dịch VNPay chưa hoàn tất. Vui lòng thử lại.',
      '02': 'Giao dịch VNPay bị lỗi. Vui lòng kiểm tra thông tin và thử lại.',
      '04': 'Giao dịch VNPay bị đảo. Vui lòng liên hệ ngân hàng để được hỗ trợ.',
      '05': 'VNPay đang xử lý giao dịch hoàn tiền.',
      '06': 'VNPay đã gửi yêu cầu hoàn tiền đến ngân hàng.',
      '07': 'Giao dịch VNPay bị nghi ngờ gian lận.',
      '09': 'Giao dịch hoàn trả VNPay bị từ chối.'
    }

    if (vnpayErrors[responseCode]) {
      return vnpayErrors[responseCode]
    }

    return 'Thanh toán VNPay không thành công. Vui lòng kiểm tra thông tin và thử lại.'
  }

  // Generic error message
  return 'Giao dịch không thành công. Vui lòng thử lại sau hoặc chọn phương thức thanh toán khác.'
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

// Lifecycle hooks for WebSocket integration
onMounted(() => {
  // Set up WebSocket message callback for payment notifications
  setIntegrationCallback('onMessage', handlePaymentNotification)

  console.log('🔌 PaymentWorkflow WebSocket integration initialized for order:', props.orderData.orderId)

  // Show connection status if not connected
  if (!isConnected.value) {
    console.log('⚠️ WebSocket not connected - real-time payment updates may not work')
  }
})

onUnmounted(() => {
  // Clean up WebSocket subscription
  if (orderSubscription) {
    orderSubscription = null
  }

  // Remove integration callback
  setIntegrationCallback('onMessage', null)

  console.log('🔌 PaymentWorkflow WebSocket integration cleaned up')
})
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
