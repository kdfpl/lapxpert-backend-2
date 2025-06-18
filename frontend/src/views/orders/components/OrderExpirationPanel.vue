<template>
  <div class="order-expiration-panel">
    <!-- Panel Header -->
    <div class="flex items-center justify-between mb-4">
      <h4 class="font-semibold text-lg flex items-center gap-2">
        <i class="pi pi-clock text-orange-600"></i>
        <span>Theo dõi hết hạn đơn hàng</span>
      </h4>
      <div class="flex items-center gap-2">
        <Badge 
          v-if="hasExpirationUpdates" 
          :value="totalUpdatesCount" 
          severity="warn" 
          size="small" 
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

    <!-- Settings Panel -->
    <div v-if="showSettings" class="mb-4 p-3 border rounded-lg bg-surface-50">
      <h5 class="font-medium mb-3">Cài đặt thông báo hết hạn</h5>
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <label class="text-sm">Thông báo hết hạn đơn hàng</label>
          <ToggleSwitch v-model="showExpirationNotifications" @change="toggleExpirationNotifications" />
        </div>
        <div class="flex items-center justify-between">
          <label class="text-sm">Cảnh báo trước (phút)</label>
          <InputNumber 
            v-model="expirationWarningThreshold" 
            :min="5" 
            :max="120" 
            size="small"
            class="w-20"
          />
        </div>
      </div>
    </div>

    <!-- Critical Expiring Orders -->
    <div v-if="criticalExpiringOrders.length > 0" class="mb-4 p-3 border rounded-lg bg-red-50 border-red-200">
      <h5 class="font-medium text-red-800 mb-2 flex items-center gap-2">
        <i class="pi pi-exclamation-triangle"></i>
        Đơn hàng sắp hết hạn ({{ criticalExpiringOrders.length }})
      </h5>
      <div class="space-y-2">
        <div v-for="order in criticalExpiringOrders.slice(0, 3)" :key="order.id" 
             class="text-sm text-red-700 p-2 bg-red-100 rounded">
          <div class="flex items-center justify-between">
            <div class="font-medium">{{ order.orderCode }}</div>
            <div class="text-xs font-bold text-red-600">
              {{ formatRemainingTime(getRemainingTimeForOrder(order.id)) }}
            </div>
          </div>
          <div class="text-xs">
            {{ order.customerName }} - {{ formatCurrency(order.totalAmount) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Expiring Orders -->
    <div v-if="expiringOrders.length > 0" class="mb-4 p-3 border rounded-lg bg-orange-50 border-orange-200">
      <h5 class="font-medium text-orange-800 mb-2 flex items-center gap-2">
        <i class="pi pi-clock"></i>
        Đơn hàng chờ thanh toán ({{ expiringOrders.length }})
      </h5>
      <div class="space-y-2">
        <div v-for="order in expiringOrders.slice(0, 5)" :key="order.id" 
             class="text-sm text-orange-700">
          <div class="flex items-center justify-between">
            <div class="font-medium">{{ order.orderCode }}</div>
            <div class="text-xs" :class="{
              'text-red-600 font-bold': order.remainingMinutes <= 30,
              'text-orange-600': order.remainingMinutes > 30
            }">
              {{ formatRemainingTime(getRemainingTimeForOrder(order.id)) }}
            </div>
          </div>
          <div class="text-xs">
            {{ order.customerName }} - {{ formatCurrency(order.totalAmount) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Expired Orders -->
    <div v-if="expiredOrders.length > 0" class="mb-4 p-3 border rounded-lg bg-gray-50 border-gray-200">
      <h5 class="font-medium text-gray-800 mb-2 flex items-center gap-2">
        <i class="pi pi-times-circle"></i>
        Đơn hàng đã hết hạn ({{ expiredOrders.length }})
      </h5>
      <div class="space-y-2">
        <div v-for="order in expiredOrders.slice(0, 3)" :key="order.id" 
             class="text-sm text-gray-700">
          <div class="font-medium">{{ order.orderCode }}</div>
          <div class="text-xs">
            {{ order.customerName }} - {{ formatTime(order.expiredAt) }}
          </div>
          <div class="text-xs text-gray-500">{{ order.reason }}</div>
        </div>
      </div>
    </div>

    <!-- Inventory Release Notifications -->
    <div v-if="inventoryReleaseNotifications.length > 0" class="mb-4 p-3 border rounded-lg bg-blue-50 border-blue-200">
      <h5 class="font-medium text-blue-800 mb-2 flex items-center gap-2">
        <i class="pi pi-box"></i>
        Giải phóng tồn kho ({{ inventoryReleaseNotifications.length }})
      </h5>
      <div class="space-y-2">
        <div v-for="release in inventoryReleaseNotifications.slice(0, 3)" :key="release.id" 
             class="text-sm text-blue-700">
          <div class="font-medium">{{ release.orderCode }}</div>
          <div class="text-xs">
            Đã giải phóng {{ release.totalItemsReleased }} sản phẩm - {{ formatTime(release.timestamp) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Order Status Changes -->
    <div v-if="orderStatusChanges.length > 0" class="mb-4 p-3 border rounded-lg bg-green-50 border-green-200">
      <h5 class="font-medium text-green-800 mb-2 flex items-center gap-2">
        <i class="pi pi-refresh"></i>
        Thay đổi trạng thái ({{ orderStatusChanges.length }})
      </h5>
      <div class="space-y-2">
        <div v-for="change in orderStatusChanges.slice(0, 3)" :key="change.id" 
             class="text-sm text-green-700">
          <div class="font-medium">{{ change.orderCode }}</div>
          <div class="text-xs">
            {{ change.oldStatus }} → {{ change.newStatus }} - {{ formatTime(change.timestamp) }}
          </div>
          <div class="text-xs text-green-600">{{ change.reason }}</div>
        </div>
      </div>
    </div>

    <!-- No Updates Message -->
    <div v-if="!hasExpirationUpdates" class="text-center text-surface-500 text-sm py-6">
      <i class="pi pi-check-circle text-green-500 mb-2 block text-lg"></i>
      Không có đơn hàng nào sắp hết hạn
    </div>

    <!-- Action Buttons -->
    <div v-if="hasExpirationUpdates" class="flex gap-2 pt-3 border-t">
      <Button 
        label="Xóa lịch sử" 
        icon="pi pi-trash" 
        size="small" 
        text 
        severity="secondary"
        @click="clearExpirationHistory"
      />
      <Button 
        label="Làm mới" 
        icon="pi pi-refresh" 
        size="small" 
        text 
        severity="info"
        @click="subscribeToOrderExpiration"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import ToggleSwitch from 'primevue/toggleswitch'
import InputNumber from 'primevue/inputnumber'
import { useOrderExpiration } from '@/composables/useOrderExpiration'

// Order expiration composable
const {
  expiringOrders,
  expiredOrders,
  inventoryReleaseNotifications,
  orderStatusChanges,
  showExpirationNotifications,
  expirationWarningThreshold,
  hasExpirationUpdates,
  criticalExpiringOrders,
  toggleExpirationNotifications,
  clearExpirationHistory,
  subscribeToOrderExpiration,
  formatRemainingTime,
  getRemainingTimeForOrder,
  formatCurrency
} = useOrderExpiration()

// Local state
const showSettings = ref(false)

// Computed properties
const totalUpdatesCount = computed(() => {
  return expiringOrders.value.length + 
         expiredOrders.value.length + 
         inventoryReleaseNotifications.value.length + 
         orderStatusChanges.value.length
})

// Utility functions
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('vi-VN', { 
    hour: '2-digit', 
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit'
  })
}
</script>

<style scoped>
.order-expiration-panel {
  min-width: 350px;
  max-width: 450px;
}

.order-expiration-panel .p-badge {
  font-size: 0.75rem;
}

.order-expiration-panel .text-xs {
  font-size: 0.75rem;
  line-height: 1rem;
}
</style>
