<template>
  <div class="payment-summary-container">
    <!-- Header -->
    <div class="flex items-center gap-2 mb-6">
      <i class="pi pi-calculator text-primary text-xl"></i>
      <h3 class="text-xl font-semibold text-surface-900 dark:text-surface-0">
        Tổng kết thanh toán
      </h3>
    </div>

    <!-- Order Summary Card -->
    <div class="card border border-surface-200 dark:border-surface-700 mb-6">
      <div class="space-y-4">
        <!-- Subtotal -->
        <div class="flex justify-between items-center py-2">
          <span class="text-surface-600 dark:text-surface-400">
            Tổng tiền hàng ({{ totalItems }} sản phẩm):
          </span>
          <span class="font-semibold text-lg">
            {{ formatCurrency(subtotal) }}
          </span>
        </div>

        <!-- Shipping Fee -->
        <div v-if="shippingFee > 0" class="flex justify-between items-center py-2">
          <span class="text-surface-600 dark:text-surface-400">
            Phí vận chuyển:
          </span>
          <span class="font-semibold">
            {{ formatCurrency(shippingFee) }}
          </span>
        </div>

        <!-- Voucher Discounts -->
        <div v-if="voucherDiscount > 0" class="flex justify-between items-center py-2">
          <span class="text-surface-600 dark:text-surface-400 flex items-center gap-2">
            <i class="pi pi-ticket text-green-600"></i>
            Giảm giá voucher:
          </span>
          <span class="font-semibold text-green-600">
            -{{ formatCurrency(voucherDiscount) }}
          </span>
        </div>

        <!-- Campaign Discounts -->
        <div v-if="campaignDiscount > 0" class="flex justify-between items-center py-2">
          <span class="text-surface-600 dark:text-surface-400 flex items-center gap-2">
            <i class="pi pi-percentage text-orange-600"></i>
            Giảm giá khuyến mãi:
          </span>
          <span class="font-semibold text-orange-600">
            -{{ formatCurrency(campaignDiscount) }}
          </span>
        </div>

        <!-- Divider -->
        <Divider />

        <!-- Total Amount -->
        <div class="flex justify-between items-center py-3 bg-primary-50 dark:bg-primary-900/20 px-4 rounded-lg">
          <span class="text-lg font-bold text-surface-900 dark:text-surface-0">
            Tổng thanh toán:
          </span>
          <span class="text-2xl font-bold text-primary">
            {{ formatCurrency(totalAmount) }}
          </span>
        </div>

        <!-- Payment Status -->
        <div v-if="showPaymentStatus" class="space-y-3 pt-4">
          <div class="flex justify-between items-center">
            <span class="text-surface-600 dark:text-surface-400">
              Đã thanh toán:
            </span>
            <span class="font-semibold text-green-600">
              {{ formatCurrency(paidAmount) }}
            </span>
          </div>

          <div v-if="remainingAmount > 0" class="flex justify-between items-center">
            <span class="text-surface-600 dark:text-surface-400">
              Còn lại:
            </span>
            <span class="font-semibold text-orange-600">
              {{ formatCurrency(remainingAmount) }}
            </span>
          </div>

          <!-- Payment Status Badge -->
          <div class="flex justify-between items-center">
            <span class="text-surface-600 dark:text-surface-400">
              Trạng thái:
            </span>
            <Badge 
              :value="paymentStatusInfo.label" 
              :severity="paymentStatusInfo.severity"
              class="text-sm"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Payment Method Display -->
    <div v-if="paymentMethod" class="card border border-surface-200 dark:border-surface-700 mb-6">
      <div class="flex items-center gap-2 mb-4">
        <i class="pi pi-credit-card text-primary"></i>
        <span class="font-semibold">Phương thức thanh toán</span>
      </div>
      
      <div class="flex items-center gap-3">
        <i :class="paymentMethodInfo.icon" class="text-xl text-primary"></i>
        <div>
          <div class="font-semibold">{{ paymentMethodInfo.label }}</div>
          <div class="text-sm text-surface-600 dark:text-surface-400">
            {{ paymentMethodInfo.description }}
          </div>
        </div>
      </div>
    </div>

    <!-- Applied Vouchers List -->
    <div v-if="appliedVouchers?.length > 0" class="card border border-surface-200 dark:border-surface-700">
      <div class="flex items-center gap-2 mb-4">
        <i class="pi pi-ticket text-primary"></i>
        <span class="font-semibold">Voucher đã áp dụng</span>
      </div>
      
      <div class="space-y-3">
        <div 
          v-for="voucher in appliedVouchers" 
          :key="voucher.id"
          class="flex justify-between items-center p-3 bg-surface-50 dark:bg-surface-800 rounded-lg"
        >
          <div class="flex items-center gap-3">
            <i class="pi pi-ticket text-green-600"></i>
            <div>
              <div class="font-medium">{{ voucher.maPhieuGiamGia }}</div>
              <div class="text-sm text-surface-600 dark:text-surface-400">
                {{ voucher.tenPhieuGiamGia }}
              </div>
            </div>
          </div>
          <div class="text-right">
            <div class="font-semibold text-green-600">
              -{{ formatCurrency(voucher.giaTriGiam) }}
            </div>
            <div class="text-xs text-surface-500">
              {{ voucher.loaiGiamGia === 'PHAN_TRAM' ? `${voucher.giaTriGiam}%` : 'Số tiền cố định' }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// Props
const props = defineProps({
  // Order totals
  subtotal: {
    type: Number,
    default: 0
  },
  shippingFee: {
    type: Number,
    default: 0
  },
  voucherDiscount: {
    type: Number,
    default: 0
  },
  campaignDiscount: {
    type: Number,
    default: 0
  },
  totalAmount: {
    type: Number,
    default: 0
  },
  totalItems: {
    type: Number,
    default: 0
  },
  
  // Payment information
  paymentMethod: {
    type: String,
    default: null
  },
  paymentStatus: {
    type: String,
    default: null
  },
  paidAmount: {
    type: Number,
    default: 0
  },
  
  // Applied vouchers
  appliedVouchers: {
    type: Array,
    default: () => []
  },
  
  // Display options
  showPaymentStatus: {
    type: Boolean,
    default: false
  }
})

// Computed properties
const remainingAmount = computed(() => {
  return Math.max(0, props.totalAmount - props.paidAmount)
})

const paymentStatusInfo = computed(() => {
  const statusMap = {
    'CHUA_THANH_TOAN': {
      label: 'Chưa thanh toán',
      severity: 'warn'
    },
    'DA_THANH_TOAN': {
      label: 'Đã thanh toán',
      severity: 'success'
    },
    'THANH_TOAN_MOT_PHAN': {
      label: 'Thanh toán một phần',
      severity: 'info'
    },
    'HOAN_TIEN': {
      label: 'Hoàn tiền',
      severity: 'secondary'
    }
  }
  
  return statusMap[props.paymentStatus] || { label: 'Không xác định', severity: 'secondary' }
})

const paymentMethodInfo = computed(() => {
  const methodMap = {
    'TIEN_MAT': {
      label: 'Tiền mặt',
      description: 'Thanh toán bằng tiền mặt tại quầy',
      icon: 'pi pi-wallet'
    },
    'COD': {
      label: 'Thanh toán khi nhận hàng',
      description: 'Thanh toán khi giao hàng',
      icon: 'pi pi-money-bill'
    },
    'VNPAY': {
      label: 'VNPay',
      description: 'Thanh toán qua ví điện tử VNPay',
      icon: 'pi pi-credit-card'
    }
  }
  
  return methodMap[props.paymentMethod] || { 
    label: 'Không xác định', 
    description: '', 
    icon: 'pi pi-question-circle' 
  }
})

// Utility functions
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount || 0)
}
</script>

<style scoped>
.payment-summary-container {
  @apply space-y-4;
}

.card {
  @apply p-6 bg-surface-0 dark:bg-surface-900 rounded-lg shadow-sm;
}
</style>
