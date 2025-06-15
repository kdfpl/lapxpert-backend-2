<template>
  <div v-if="priceChanges.length > 0" class="price-change-warnings mb-4">
    <div
      v-for="(change, index) in priceChanges"
      :key="index"
      class="price-change-warning flex items-center gap-3 p-3 mb-2 bg-red-50 border border-red-200 rounded-lg"
    >
      <i class="pi pi-exclamation-triangle text-red-500"></i>
      <div class="flex-1">
        <div class="text-red-700 font-medium text-sm">
          {{ change.productName }} - {{ change.variantInfo }}
        </div>
        <div class="text-red-600 italic text-xs mt-1">
          <span v-if="change.changeType === 'SKU_PRICE_DIFFERENCE'">
            Giá sản phẩm đã thay đổi từ {{ formatCurrency(change.oldPrice) }} thành {{ formatCurrency(change.newPrice) }}
          </span>
          <span v-else>
            Giá đã thay đổi từ {{ formatCurrency(change.oldPrice) }} thành {{ formatCurrency(change.newPrice) }}
          </span>
          <span v-if="change.changeType === 'INCREASE' || (change.changeType === 'SKU_PRICE_DIFFERENCE' && change.newPrice > change.oldPrice)" class="text-red-700 font-semibold"> (Tăng {{ formatCurrency(change.newPrice - change.oldPrice) }})</span>
          <span v-else-if="change.changeType === 'DECREASE' || (change.changeType === 'SKU_PRICE_DIFFERENCE' && change.newPrice < change.oldPrice)" class="text-green-600 font-semibold"> (Giảm {{ formatCurrency(change.oldPrice - change.newPrice) }})</span>
        </div>
      </div>
      <Button
        icon="pi pi-check"
        text
        rounded
        size="small"
        severity="secondary"
        @click="acknowledgeChange(index)"
        v-tooltip.top="'Đã hiểu'"
        class="text-red-500 hover:text-red-700"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'

const props = defineProps({
  priceChanges: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['acknowledge-change'])

/**
 * Format currency for display
 */
const formatCurrency = (amount) => {
  if (amount == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

/**
 * Acknowledge a price change
 */
const acknowledgeChange = (index) => {
  emit('acknowledge-change', index)
}
</script>

<style scoped>
.price-change-warning {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.price-change-warning:hover {
  background-color: rgb(254 242 242);
  border-color: rgb(252 165 165);
}
</style>
