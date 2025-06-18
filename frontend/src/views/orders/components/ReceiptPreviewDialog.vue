<template>
  <Dialog
    v-model:visible="dialogVisible"
    modal
    :header="`Xem trước hóa đơn ${orderCode || ''}`"
    :style="{ width: '800px', maxHeight: '90vh' }"
    class="receipt-preview-dialog"
    :closable="!loading"
    :dismissableMask="!loading"
  >
    <!-- Loading State -->
    <div v-if="loading" class="text-center py-8">
      <ProgressSpinner />
      <p class="mt-4 text-surface-600">Đang tải dữ liệu hóa đơn...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center py-8">
      <i class="pi pi-exclamation-triangle text-4xl text-red-500 mb-4 block"></i>
      <h3 class="text-lg font-semibold mb-2">Có lỗi xảy ra</h3>
      <p class="text-surface-600 mb-4">{{ error }}</p>
      <Button
        label="Thử lại"
        icon="pi pi-refresh"
        @click="loadReceiptPreview"
      />
    </div>

    <!-- Receipt Preview Content -->
    <div v-else-if="receiptData" class="receipt-preview-content">
      <!-- Receipt Header -->
      <div class="text-center mb-6 pb-4 border-b border-surface-200 dark:border-surface-700">
        <h2 class="text-2xl font-bold text-primary mb-2">{{ receiptData.tenCuaHang || 'LapXpert Store' }}</h2>
        <p class="text-surface-600">{{ receiptData.diaChiCuaHang || 'Địa chỉ cửa hàng' }}</p>
        <p class="text-surface-600">{{ receiptData.soDienThoaiCuaHang || 'Số điện thoại cửa hàng' }}</p>
      </div>

      <!-- Order Information -->
      <div class="grid grid-cols-2 gap-4 mb-6">
        <div>
          <p class="text-sm text-surface-600">Mã hóa đơn:</p>
          <p class="font-semibold">{{ receiptData.maHoaDon }}</p>
        </div>
        <div>
          <p class="text-sm text-surface-600">Ngày tạo:</p>
          <p class="font-semibold">{{ formatDateTime(receiptData.ngayTao) }}</p>
        </div>
        <div>
          <p class="text-sm text-surface-600">Nhân viên:</p>
          <p class="font-semibold">{{ receiptData.tenNhanVien || 'Không có thông tin' }}</p>
        </div>
        <div>
          <p class="text-sm text-surface-600">Khách hàng:</p>
          <p class="font-semibold">{{ receiptData.tenKhachHang || 'Khách lẻ' }}</p>
        </div>
      </div>

      <!-- Order Items -->
      <div class="mb-6">
        <h3 class="text-lg font-semibold mb-3">Chi tiết sản phẩm</h3>
        <div class="border border-surface-200 dark:border-surface-700 rounded">
          <div class="bg-surface-50 dark:bg-surface-800 p-3 border-b border-surface-200 dark:border-surface-700">
            <div class="grid grid-cols-12 gap-2 text-sm font-semibold">
              <div class="col-span-6">Sản phẩm</div>
              <div class="col-span-2 text-center">Số lượng</div>
              <div class="col-span-2 text-right">Đơn giá</div>
              <div class="col-span-2 text-right">Thành tiền</div>
            </div>
          </div>
          <div class="divide-y divide-surface-200 dark:divide-surface-700">
            <div
              v-for="item in receiptData.items"
              :key="item.sku || item.tenSanPham"
              class="p-3"
            >
              <div class="grid grid-cols-12 gap-2 text-sm">
                <div class="col-span-6">
                  <p class="font-medium">{{ item.tenSanPham }}</p>
                  <p class="text-surface-600 text-xs">{{ item.sku }}</p>
                  <div v-if="item.mauSac || item.kichThuoc" class="text-xs text-surface-500 mt-1">
                    <span v-if="item.mauSac">Màu: {{ item.mauSac }}</span>
                    <span v-if="item.mauSac && item.kichThuoc"> | </span>
                    <span v-if="item.kichThuoc">Size: {{ item.kichThuoc }}</span>
                  </div>
                </div>
                <div class="col-span-2 text-center">{{ item.soLuong }}</div>
                <div class="col-span-2 text-right">{{ formatCurrency(item.giaBan) }}</div>
                <div class="col-span-2 text-right font-medium">{{ formatCurrency(item.thanhTien) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Order Summary -->
      <div class="border-t border-surface-200 dark:border-surface-700 pt-4">
        <div class="space-y-2">
          <div class="flex justify-between">
            <span>Tổng tiền hàng:</span>
            <span>{{ formatCurrency(receiptData.tongTienHang) }}</span>
          </div>
          <div v-if="receiptData.phiVanChuyen" class="flex justify-between">
            <span>Phí vận chuyển:</span>
            <span>{{ formatCurrency(receiptData.phiVanChuyen) }}</span>
          </div>
          <div v-if="receiptData.giaTriGiamGiaVoucher" class="flex justify-between text-green-600">
            <span>Giảm giá voucher:</span>
            <span>-{{ formatCurrency(receiptData.giaTriGiamGiaVoucher) }}</span>
          </div>
          <hr class="border-surface-200 dark:border-surface-700">
          <div class="flex justify-between text-lg font-bold">
            <span>Tổng thanh toán:</span>
            <span class="text-primary">{{ formatCurrency(receiptData.tongThanhToan) }}</span>
          </div>
        </div>
      </div>

      <!-- Payment Information -->
      <div v-if="receiptData.phuongThucThanhToan" class="mt-4 pt-4 border-t border-surface-200 dark:border-surface-700">
        <div class="flex justify-between">
          <span>Phương thức thanh toán:</span>
          <span class="font-medium">{{ getPaymentMethodLabel(receiptData.phuongThucThanhToan) }}</span>
        </div>
        <div v-if="receiptData.maGiaoDichThanhToan" class="flex justify-between mt-2">
          <span>Mã giao dịch:</span>
          <span class="font-mono text-sm">{{ receiptData.maGiaoDichThanhToan }}</span>
        </div>
      </div>

      <!-- Footer -->
      <div class="text-center mt-6 pt-4 border-t border-surface-200 dark:border-surface-700">
        <p class="text-sm text-surface-600">Cảm ơn quý khách đã mua hàng!</p>
        <p class="text-xs text-surface-500 mt-2">{{ formatDateTime(new Date()) }}</p>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <Button
          label="Đóng"
          icon="pi pi-times"
          severity="secondary"
          outlined
          @click="closeDialog"
          :disabled="loading"
        />
        <Button
          v-if="receiptData"
          label="In hóa đơn"
          icon="pi pi-print"
          severity="primary"
          @click="printReceipt"
          :loading="printing"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import orderApi from '@/apis/orderApi'
import { format } from 'date-fns'
import { vi } from 'date-fns/locale'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  orderId: {
    type: [String, Number],
    default: null
  },
  orderCode: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:visible'])

const toast = useToast()

// Reactive data
const dialogVisible = ref(false)
const receiptData = ref(null)
const loading = ref(false)
const printing = ref(false)
const error = ref(null)

// Computed properties
// isVisible removed - not used in template, dialogVisible is used instead

// Watchers
watch(() => props.visible, (newValue) => {
  dialogVisible.value = newValue
  if (newValue && props.orderId) {
    loadReceiptPreview()
  }
})

watch(dialogVisible, (newValue) => {
  emit('update:visible', newValue)
})

// Methods
const loadReceiptPreview = async () => {
  if (!props.orderId) return

  loading.value = true
  error.value = null

  try {
    const response = await orderApi.getReceiptPreview(props.orderId)

    if (response.success) {
      receiptData.value = response.data
    } else {
      throw new Error(response.message)
    }
  } catch (err) {
    console.error('Error loading receipt preview:', err)
    error.value = err.message || 'Không thể tải dữ liệu hóa đơn'
  } finally {
    loading.value = false
  }
}

const printReceipt = async () => {
  if (!props.orderId) return

  printing.value = true

  try {
    const response = await orderApi.printOrderReceipt(props.orderId)

    if (response.success) {
      // Create blob URL and download
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `hoa-don-${props.orderCode || props.orderId}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Tải hóa đơn thành công',
        life: 3000
      })
    } else {
      throw new Error(response.message)
    }
  } catch (err) {
    console.error('Error printing receipt:', err)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể in hóa đơn',
      life: 3000
    })
  } finally {
    printing.value = false
  }
}

const closeDialog = () => {
  dialogVisible.value = false
  receiptData.value = null
  error.value = null
}

const formatDateTime = (date) => {
  if (!date) return ''
  const dateObj = typeof date === 'string' ? new Date(date) : date
  return format(dateObj, 'dd/MM/yyyy HH:mm', { locale: vi })
}

const formatCurrency = (amount) => {
  if (amount === null || amount === undefined) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

const getPaymentMethodLabel = (method) => {
  const methodMap = {
    'TIEN_MAT': 'Tiền mặt',
    'VNPAY': 'VNPay',
    'MOMO': 'MoMo',
    'VIETQR': 'VietQR',
    'CHUYEN_KHOAN': 'Chuyển khoản'
  }
  return methodMap[method] || method
}
</script>

<style scoped>
.receipt-preview-dialog :deep(.p-dialog-content) {
  padding: 1.5rem;
}

.receipt-preview-content {
  max-height: 60vh;
  overflow-y: auto;
}

/* Print styles */
@media print {
  .receipt-preview-content {
    max-height: none;
    overflow: visible;
  }
}
</style>
