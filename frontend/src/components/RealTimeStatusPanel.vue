<template>
  <div class="real-time-status-panel">
    <!-- Connection Status -->
    <div class="mb-4 p-3 border rounded-lg" 
         :class="{
           'bg-green-50 border-green-200': isConnected,
           'bg-red-50 border-red-200': !isConnected
         }">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <i class="text-sm" :class="{
            'pi pi-circle-fill text-green-500': isConnected,
            'pi pi-circle text-red-500': !isConnected
          }"></i>
          <span class="font-medium text-sm">{{ connectionStatus?.text || 'Không xác định' }}</span>
        </div>
        <div class="flex gap-2">
          <Button 
            v-if="!isConnected"
            icon="pi pi-refresh" 
            size="small" 
            text 
            @click="reconnect"
            v-tooltip.top="'Kết nối lại'"
          />
          <Button 
            icon="pi pi-cog" 
            size="small" 
            text 
            @click="showSettings = !showSettings"
            v-tooltip.top="'Cài đặt'"
          />
        </div>
      </div>
    </div>

    <!-- Settings Panel -->
    <div v-if="showSettings" class="mb-4 p-3 border rounded-lg bg-surface-50">
      <h5 class="font-medium mb-3">Cài đặt thông báo</h5>
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <label class="text-sm">Thông báo thay đổi giá</label>
          <ToggleSwitch v-model="showPriceWarnings" @change="togglePriceWarnings" />
        </div>
        <div class="flex items-center justify-between">
          <label class="text-sm">Thông báo voucher</label>
          <ToggleSwitch v-model="showVoucherNotifications" @change="toggleVoucherNotifications" />
        </div>
      </div>
    </div>

    <!-- Recent Updates -->
    <div v-if="hasUpdates" class="space-y-3">
      <!-- Price Updates -->
      <div v-if="recentPriceUpdates.length > 0" class="p-3 border rounded-lg bg-blue-50 border-blue-200">
        <h5 class="font-medium text-blue-800 mb-2 flex items-center gap-2">
          <i class="pi pi-chart-line"></i>
          Cập nhật giá gần đây ({{ recentPriceUpdates.length }})
        </h5>
        <div class="space-y-2">
          <div v-for="update in recentPriceUpdates.slice(0, 3)" :key="update.id" 
               class="text-sm text-blue-700">
            <div class="font-medium">{{ update.productName || 'Sản phẩm' }}</div>
            <div class="text-xs">
              {{ formatCurrency(update.oldPrice) }} → {{ formatCurrency(update.newPrice) }}
              <span class="ml-2 text-blue-600">{{ formatTime(update.timestamp) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Voucher Updates -->
      <div v-if="recentExpiredVouchers.length > 0" class="p-3 border rounded-lg bg-orange-50 border-orange-200">
        <h5 class="font-medium text-orange-800 mb-2 flex items-center gap-2">
          <i class="pi pi-exclamation-triangle"></i>
          Voucher hết hạn ({{ recentExpiredVouchers.length }})
        </h5>
        <div class="space-y-2">
          <div v-for="voucher in recentExpiredVouchers.slice(0, 2)" :key="voucher.id" 
               class="text-sm text-orange-700">
            <div class="font-medium">{{ voucher.code }}</div>
            <div class="text-xs">{{ formatTime(voucher.timestamp) }}</div>
          </div>
        </div>
      </div>

      <!-- New Vouchers -->
      <div v-if="recentNewVouchers.length > 0" class="p-3 border rounded-lg bg-green-50 border-green-200">
        <h5 class="font-medium text-green-800 mb-2 flex items-center gap-2">
          <i class="pi pi-check-circle"></i>
          Voucher mới ({{ recentNewVouchers.length }})
        </h5>
        <div class="space-y-2">
          <div v-for="voucher in recentNewVouchers.slice(0, 2)" :key="voucher.id" 
               class="text-sm text-green-700">
            <div class="font-medium">{{ voucher.code }}</div>
            <div class="text-xs">
              Giảm {{ formatCurrency(voucher.discountValue) }} - {{ formatTime(voucher.timestamp) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- No Updates Message -->
    <div v-else-if="isConnected" class="text-center text-surface-500 text-sm py-4">
      <i class="pi pi-check-circle text-green-500 mb-2 block text-lg"></i>
      Đang theo dõi cập nhật real-time
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Button from 'primevue/button'
import ToggleSwitch from 'primevue/toggleswitch'
import { useRealTimeOrderManagement } from '@/composables/useRealTimeOrderManagement'
import { useRealTimePricing } from '@/composables/useRealTimePricing'
import { useVoucherMonitoring } from '@/composables/useVoucherMonitoring'

// Real-time composables
const {
  isConnected,
  connectionStatus,
  reconnect
} = useRealTimeOrderManagement()

const {
  recentPriceUpdates,
  showPriceWarnings,
  togglePriceWarnings
} = useRealTimePricing()

const {
  recentExpiredVouchers,
  recentNewVouchers,
  showVoucherNotifications,
  toggleVoucherNotifications
} = useVoucherMonitoring()

// Local state
const showSettings = ref(false)

// Computed properties
const hasUpdates = computed(() => {
  return recentPriceUpdates.value.length > 0 || 
         recentExpiredVouchers.value.length > 0 || 
         recentNewVouchers.value.length > 0
})

// Utility functions
const formatCurrency = (amount) => {
  if (amount == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('vi-VN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}
</script>

<style scoped>
.real-time-status-panel {
  min-width: 300px;
  max-width: 400px;
}
</style>
