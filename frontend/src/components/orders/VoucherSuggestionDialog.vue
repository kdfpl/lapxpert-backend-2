<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="dialogTitle"
    :style="{ width: '500px' }"
    :closable="true"
    @hide="onDialogHide"
  >
    <div v-if="suggestion" class="space-y-4">
      <!-- Suggestion Header -->
      <div class="text-center mb-4">
        <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
          <i class="pi pi-star-fill text-2xl text-green-600"></i>
        </div>
        <h3 class="text-lg font-semibold text-surface-900 mb-2">
          Tìm thấy voucher tốt hơn!
        </h3>
        <p class="text-sm text-surface-600">
          Chúng tôi tìm thấy một voucher giúp bạn tiết kiệm thêm
        </p>
      </div>

      <!-- Savings Highlight -->
      <div class="bg-green-50 border border-green-200 rounded-lg p-4 text-center">
        <div class="text-2xl font-bold text-green-700 mb-1">
          +{{ formatCurrency(suggestion.savingsAmount) }}
        </div>
        <div class="text-sm text-green-600">
          Tiết kiệm thêm so với voucher hiện tại
        </div>
      </div>

      <!-- Voucher Comparison -->
      <div class="grid grid-cols-2 gap-4">
        <!-- Current Voucher -->
        <div class="border border-surface-200 rounded-lg p-3">
          <div class="text-xs text-surface-500 uppercase tracking-wide font-medium mb-2">
            Voucher hiện tại
          </div>
          <div class="font-semibold text-sm text-surface-900 mb-1">
            {{ suggestion.currentVoucherCode }}
          </div>
          <div class="text-sm text-surface-600">
            Giảm {{ formatCurrency(suggestion.currentDiscount) }}
          </div>
        </div>

        <!-- Better Voucher -->
        <div class="border border-green-300 bg-green-50 rounded-lg p-3">
          <div class="text-xs text-green-600 uppercase tracking-wide font-medium mb-2">
            Voucher được đề xuất
          </div>
          <div class="font-semibold text-sm text-green-800 mb-1">
            {{ suggestion.betterVoucher?.maPhieuGiamGia || suggestion.betterVoucher?.code }}
          </div>
          <div class="text-sm text-green-700">
            Giảm {{ formatCurrency(suggestion.betterDiscount) }}
          </div>
        </div>
      </div>

      <!-- Voucher Details -->
      <div v-if="suggestion.betterVoucher" class="border-t pt-4">
        <div class="text-sm font-medium text-surface-900 mb-2">
          Chi tiết voucher được đề xuất:
        </div>
        <div class="text-sm text-surface-600 space-y-1">
          <div v-if="suggestion.betterVoucher.tenPhieuGiamGia">
            <strong>Tên:</strong> {{ suggestion.betterVoucher.tenPhieuGiamGia }}
          </div>
          <div v-if="suggestion.betterVoucher.moTa">
            <strong>Mô tả:</strong> {{ suggestion.betterVoucher.moTa }}
          </div>
          <div v-if="suggestion.betterVoucher.giaTriDonHangToiThieu">
            <strong>Đơn tối thiểu:</strong> {{ formatCurrency(suggestion.betterVoucher.giaTriDonHangToiThieu) }}
          </div>
          <div v-if="suggestion.betterVoucher.ngayKetThuc">
            <strong>Hết hạn:</strong> {{ formatDate(suggestion.betterVoucher.ngayKetThuc) }}
          </div>
        </div>
      </div>

      <!-- Message -->
      <div v-if="suggestion.message" class="text-sm text-surface-600 text-center italic">
        {{ suggestion.message }}
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button
          label="Không, cảm ơn"
          outlined
          @click="onReject"
          :disabled="processing"
        />
        <Button
          label="Áp dụng voucher tốt hơn"
          @click="onAccept"
          :loading="processing"
          icon="pi pi-check"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  suggestion: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'accept', 'reject'])

const processing = ref(false)

const dialogTitle = computed(() => {
  if (!props.suggestion) return 'Gợi ý voucher'
  return `Tiết kiệm thêm ${formatCurrency(props.suggestion.savingsAmount)}`
})

const onDialogHide = () => {
  emit('update:visible', false)
}

const onAccept = async () => {
  if (!props.suggestion) return
  
  processing.value = true
  try {
    emit('accept', props.suggestion)
  } finally {
    processing.value = false
  }
}

const onReject = () => {
  if (!props.suggestion) return
  
  emit('reject', props.suggestion)
  emit('update:visible', false)
}

const formatCurrency = (amount) => {
  if (amount == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}
</script>

<style scoped>
/* Custom styles for the voucher suggestion dialog */
.space-y-4 > * + * {
  margin-top: 1rem;
}

.space-y-1 > * + * {
  margin-top: 0.25rem;
}

.grid {
  display: grid;
}

.grid-cols-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.gap-4 {
  gap: 1rem;
}

.gap-2 {
  gap: 0.5rem;
}
</style>
