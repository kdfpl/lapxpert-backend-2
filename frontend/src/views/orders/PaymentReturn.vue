<template>
  <Fluid />
  <Toast />

  <!-- Page Header -->
  <div class="card mb-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
          <i class="pi pi-credit-card text-lg text-primary"></i>
        </div>
        <div>
          <h1 class="font-semibold text-xl text-surface-900 m-0">
            Kết quả thanh toán
          </h1>
          <p class="text-surface-500 text-sm mt-1 mb-0">
            Thông tin kết quả thanh toán từ cổng thanh toán
          </p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          size="small"
          @click="goToOrderList"
          v-tooltip.left="'Về danh sách đơn hàng'"
        />
      </div>
    </div>
  </div>

  <!-- Loading State -->
  <div v-if="loading" class="text-center py-12">
    <ProgressSpinner />
    <p class="mt-4 text-surface-600">Đang xử lý kết quả thanh toán...</p>
  </div>

  <!-- Payment Result Content -->
  <div v-else class="card">
    <!-- Payment Status Card -->
    <div class="text-center mb-6">
      <div class="mb-4">
        <div 
          :class="[
            'w-20 h-20 mx-auto rounded-full flex items-center justify-center mb-4',
            paymentResult.success ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'
          ]"
        >
          <i 
            :class="[
              'text-3xl',
              paymentResult.success ? 'pi pi-check' : 'pi pi-times'
            ]"
          ></i>
        </div>
        <h2 
          :class="[
            'text-2xl font-bold mb-2',
            paymentResult.success ? 'text-green-600' : 'text-red-600'
          ]"
        >
          {{ paymentResult.title }}
        </h2>
        <p class="text-surface-600 text-lg">
          {{ paymentResult.message }}
        </p>
      </div>
    </div>

    <!-- Payment Details -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
      <!-- Transaction Information -->
      <div class="card border border-surface-200 dark:border-surface-700">
        <div class="flex items-center gap-2 mb-4">
          <i class="pi pi-info-circle text-primary"></i>
          <span class="font-semibold text-lg">Thông tin giao dịch</span>
        </div>
        <div class="space-y-3">
          <div v-if="paymentParams.orderId">
            <label class="text-sm font-medium text-surface-600">Mã đơn hàng</label>
            <p class="font-mono text-lg">{{ paymentParams.orderId }}</p>
          </div>
          <div v-if="paymentParams.transactionId">
            <label class="text-sm font-medium text-surface-600">Mã giao dịch</label>
            <p class="font-mono text-lg">{{ paymentParams.transactionId }}</p>
          </div>
          <div v-if="paymentParams.amount">
            <label class="text-sm font-medium text-surface-600">Số tiền</label>
            <p class="text-lg font-semibold text-primary">{{ formatCurrency(paymentParams.amount) }}</p>
          </div>
          <div v-if="paymentParams.paymentMethod">
            <label class="text-sm font-medium text-surface-600">Phương thức thanh toán</label>
            <p class="text-lg">{{ getPaymentMethodLabel(paymentParams.paymentMethod) }}</p>
          </div>
          <div>
            <label class="text-sm font-medium text-surface-600">Thời gian xử lý</label>
            <p class="text-lg">{{ formatDateTime(new Date()) }}</p>
          </div>
        </div>
      </div>

      <!-- Next Steps -->
      <div class="card border border-surface-200 dark:border-surface-700">
        <div class="flex items-center gap-2 mb-4">
          <i class="pi pi-arrow-right text-primary"></i>
          <span class="font-semibold text-lg">Bước tiếp theo</span>
        </div>
        <div class="space-y-3">
          <div v-if="paymentResult.success">
            <p class="text-surface-600 mb-3">
              Thanh toán đã được xử lý thành công. Bạn có thể:
            </p>
            <div class="space-y-2">
              <Button
                v-if="paymentParams.orderId"
                label="Xem chi tiết đơn hàng"
                icon="pi pi-eye"
                class="w-full"
                @click="goToOrderDetail"
              />
              <Button
                label="Về danh sách đơn hàng"
                icon="pi pi-list"
                severity="secondary"
                outlined
                class="w-full"
                @click="goToOrderList"
              />
            </div>
          </div>
          <div v-else>
            <p class="text-surface-600 mb-3">
              Thanh toán không thành công. Bạn có thể:
            </p>
            <div class="space-y-2">
              <Button
                v-if="paymentParams.orderId"
                label="Thử thanh toán lại"
                icon="pi pi-refresh"
                class="w-full"
                @click="retryPayment"
              />
              <Button
                label="Về danh sách đơn hàng"
                icon="pi pi-list"
                severity="secondary"
                outlined
                class="w-full"
                @click="goToOrderList"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Additional Information -->
    <div v-if="paymentParams.responseCode || paymentParams.errorCode" class="card border border-surface-200 dark:border-surface-700">
      <div class="flex items-center gap-2 mb-4">
        <i class="pi pi-code text-primary"></i>
        <span class="font-semibold text-lg">Thông tin kỹ thuật</span>
      </div>
      <div class="space-y-2 text-sm">
        <div v-if="paymentParams.responseCode">
          <span class="font-medium">Mã phản hồi:</span> {{ paymentParams.responseCode }}
        </div>
        <div v-if="paymentParams.errorCode">
          <span class="font-medium">Mã lỗi:</span> {{ paymentParams.errorCode }}
        </div>
        <div v-if="paymentParams.rawMessage">
          <span class="font-medium">Thông báo gốc:</span> {{ paymentParams.rawMessage }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import Toast from 'primevue/toast'
import Fluid from 'primevue/fluid'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const loading = ref(true)
const paymentParams = ref({})
const paymentResult = ref({
  success: false,
  title: '',
  message: ''
})

onMounted(async () => {
  try {
    // Extract payment parameters from URL
    extractPaymentParameters()
    
    // Process payment result
    processPaymentResult()
    
    // Auto-redirect after delay if successful
    if (paymentResult.value.success && paymentParams.value.orderId) {
      setTimeout(() => {
        goToOrderDetail()
      }, 5000) // Redirect after 5 seconds
    }
  } catch (error) {
    console.error('Error processing payment return:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Có lỗi xảy ra khi xử lý kết quả thanh toán',
      life: 5000
    })
  } finally {
    loading.value = false
  }
})

const extractPaymentParameters = () => {
  const query = route.query
  
  paymentParams.value = {
    // Common parameters
    orderId: query.orderId || query.vnp_TxnRef || query.orderInfo,
    transactionId: query.transactionId || query.vnp_TransactionNo || query.transId,
    amount: query.amount || query.vnp_Amount,
    paymentMethod: query.paymentMethod || (query.vnp_BankCode ? 'VNPAY' : 'MOMO'),
    
    // Status indicators
    status: query.status,
    responseCode: query.responseCode || query.vnp_ResponseCode || query.resultCode,
    errorCode: query.errorCode || query.vnp_TransactionStatus,
    
    // Messages
    message: query.message || query.vnp_OrderInfo || query.orderInfo,
    rawMessage: query.rawMessage || query.vnp_OrderInfo
  }
}

const processPaymentResult = () => {
  const { status, responseCode, errorCode } = paymentParams.value
  
  // Determine success based on various payment gateway responses
  let isSuccess = false
  let title = ''
  let message = ''
  
  // VNPay success codes
  if (responseCode === '00' || status === 'success' || status === 'SUCCESS') {
    isSuccess = true
    title = 'Thanh toán thành công!'
    message = 'Giao dịch của bạn đã được xử lý thành công.'
  }
  // MoMo success codes
  else if (responseCode === '0' || errorCode === '0') {
    isSuccess = true
    title = 'Thanh toán thành công!'
    message = 'Giao dịch MoMo của bạn đã được xử lý thành công.'
  }
  // Failure cases
  else {
    isSuccess = false
    title = 'Thanh toán không thành công'
    message = getErrorMessage(responseCode, errorCode, status)
  }
  
  paymentResult.value = {
    success: isSuccess,
    title,
    message
  }
}

const getErrorMessage = (responseCode, errorCode, status) => {
  // VNPay error codes
  const vnpayErrors = {
    '01': 'Giao dịch chưa hoàn tất',
    '02': 'Giao dịch bị lỗi',
    '04': 'Giao dịch đảo (Khách hàng đã bị trừ tiền tại Ngân hàng nhưng GD chưa thành công ở VNPAY)',
    '05': 'VNPAY đang xử lý giao dịch này (GD hoàn tiền)',
    '06': 'VNPAY đã gửi yêu cầu hoàn tiền sang Ngân hàng (GD hoàn tiền)',
    '07': 'Giao dịch bị nghi ngờ gian lận',
    '09': 'GD Hoàn trả bị từ chối'
  }
  
  // MoMo error codes
  const momoErrors = {
    '1': 'Giao dịch thất bại',
    '2': 'Giao dịch bị từ chối',
    '3': 'Giao dịch bị hủy',
    '1000': 'Giao dịch được khởi tạo, chờ người dùng xác nhận thanh toán',
    '1001': 'Giao dịch thất bại do tài khoản người dùng chưa được kích hoạt',
    '1002': 'Giao dịch thất bại do tài khoản người dùng bị khóa',
    '1003': 'Giao dịch thất bại do tài khoản người dùng chưa đăng ký dịch vụ',
    '1004': 'Giao dịch thất bại do số tiền vượt quá hạn mức thanh toán',
    '1005': 'Giao dịch thất bại do url hoặc QR code đã hết hạn',
    '1006': 'Giao dịch thất bại do người dùng từ chối xác nhận thanh toán',
    '1007': 'Giao dịch thất bại do app MoMo của người dùng chưa được cập nhật lên phiên bản mới nhất'
  }
  
  if (vnpayErrors[responseCode]) {
    return vnpayErrors[responseCode]
  }
  
  if (momoErrors[errorCode] || momoErrors[responseCode]) {
    return momoErrors[errorCode] || momoErrors[responseCode]
  }
  
  // Generic error messages
  if (status === 'cancelled' || status === 'CANCELLED') {
    return 'Giao dịch đã bị hủy bởi người dùng.'
  }
  
  if (status === 'failed' || status === 'FAILED') {
    return 'Giao dịch thất bại. Vui lòng thử lại sau.'
  }
  
  return 'Giao dịch không thành công. Vui lòng liên hệ hỗ trợ nếu cần thiết.'
}

const goToOrderDetail = () => {
  if (paymentParams.value.orderId) {
    router.push(`/orders/${paymentParams.value.orderId}`)
  } else {
    goToOrderList()
  }
}

const goToOrderList = () => {
  router.push('/orders')
}

const retryPayment = () => {
  if (paymentParams.value.orderId) {
    router.push(`/orders/${paymentParams.value.orderId}/edit`)
  } else {
    goToOrderList()
  }
}

const getPaymentMethodLabel = (method) => {
  const labelMap = {
    'VNPAY': 'VNPay',
    'MOMO': 'MoMo',
    'VIETQR': 'VietQR',
    'TIEN_MAT': 'Tiền mặt'
  }
  return labelMap[method] || method || 'Không xác định'
}

const formatCurrency = (amount) => {
  if (!amount) return '0 ₫'
  
  // VNPay amounts are in cents, convert to VND
  const actualAmount = amount > 100000 ? amount / 100 : amount
  
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(actualAmount)
}

const formatDateTime = (date) => {
  return new Intl.DateTimeFormat('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(date))
}
</script>

<style scoped>
.card {
  @apply bg-surface-0 dark:bg-surface-900 border border-surface-200 dark:border-surface-700 rounded-lg p-6;
}
</style>
