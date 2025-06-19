<template>
  <div class="enhanced-realtime-status-panel">
    <!-- Connection Status Header -->
    <div class="status-header p-3 border-bottom-1 surface-border">
      <div class="flex align-items-center justify-content-between">
        <div class="flex align-items-center gap-2">
          <i :class="connectionIcon" :style="{ color: connectionColor }"></i>
          <span class="font-semibold">{{ connectionStatusText }}</span>
          <Badge v-if="crossTabSupported" value="Multi-tab" severity="info" size="small" />
        </div>
        
        <div class="flex align-items-center gap-2">
          <!-- Connection Quality Indicator -->
          <div class="flex align-items-center gap-1">
            <i class="pi pi-wifi text-xs"></i>
            <span class="text-xs">{{ unifiedConnectionQuality }}</span>
          </div>
          
          <!-- Message Queue Status -->
          <Badge 
            v-if="messageQueue.length > 0" 
            :value="messageQueue.length" 
            severity="warn" 
            size="small"
          />
          
          <!-- Settings Button -->
          <Button 
            icon="pi pi-cog" 
            size="small" 
            text 
            @click="showSettings = !showSettings"
          />
        </div>
      </div>
    </div>

    <!-- Settings Panel -->
    <div v-if="showSettings" class="settings-panel p-3 border-bottom-1 surface-border">
      <div class="grid">
        <div class="col-6">
          <div class="field-checkbox">
            <Checkbox 
              v-model="notificationSettings.showPriceChanges" 
              inputId="price-notifications" 
              binary 
            />
            <label for="price-notifications" class="ml-2 text-sm">Thông báo giá</label>
          </div>
        </div>
        <div class="col-6">
          <div class="field-checkbox">
            <Checkbox 
              v-model="notificationSettings.showVoucherUpdates" 
              inputId="voucher-notifications" 
              binary 
            />
            <label for="voucher-notifications" class="ml-2 text-sm">Thông báo voucher</label>
          </div>
        </div>
        <div class="col-6">
          <div class="field-checkbox">
            <Checkbox 
              v-model="notificationSettings.showOrderUpdates" 
              inputId="order-notifications" 
              binary 
            />
            <label for="order-notifications" class="ml-2 text-sm">Thông báo đơn hàng</label>
          </div>
        </div>
        <div class="col-6">
          <div class="field-checkbox">
            <Checkbox 
              v-model="crossTabEnabled" 
              inputId="cross-tab-sync" 
              binary 
              :disabled="!crossTabSupported"
            />
            <label for="cross-tab-sync" class="ml-2 text-sm">Đồng bộ đa tab</label>
          </div>
        </div>
      </div>
    </div>

    <!-- Performance Metrics -->
    <div v-if="showPerformanceMetrics" class="performance-panel p-3 border-bottom-1 surface-border">
      <div class="text-xs text-surface-600 mb-2">Hiệu suất Real-time</div>
      <div class="grid">
        <div class="col-3 text-center">
          <div class="text-lg font-bold text-primary">{{ performanceMetrics.messagesProcessed }}</div>
          <div class="text-xs text-surface-500">Tin nhắn</div>
        </div>
        <div class="col-3 text-center">
          <div class="text-lg font-bold text-green-500">{{ messageQueue.length }}</div>
          <div class="text-xs text-surface-500">Hàng đợi</div>
        </div>
        <div class="col-3 text-center">
          <div class="text-lg font-bold text-blue-500">{{ performanceMetrics.crossTabMessages }}</div>
          <div class="text-xs text-surface-500">Cross-tab</div>
        </div>
        <div class="col-3 text-center">
          <div class="text-lg font-bold text-orange-500">{{ Math.round(performanceMetrics.averageProcessingTime) }}ms</div>
          <div class="text-xs text-surface-500">Xử lý TB</div>
        </div>
      </div>
    </div>

    <!-- Recent Updates -->
    <div class="updates-panel p-3">
      <div class="flex align-items-center justify-content-between mb-3">
        <span class="text-sm font-semibold">Cập nhật gần đây</span>
        <Button 
          v-if="unifiedMessageHistory.length > 0"
          icon="pi pi-trash" 
          size="small" 
          text 
          severity="secondary"
          @click="clearAllHistory"
        />
      </div>

      <!-- Recent Messages -->
      <div v-if="recentMessages.length > 0" class="space-y-2">
        <div 
          v-for="message in recentMessages" 
          :key="message.id || message.timestamp"
          class="message-item p-2 border-round surface-50"
        >
          <div class="flex align-items-start justify-content-between">
            <div class="flex-1">
              <div class="flex align-items-center gap-2 mb-1">
                <i :class="getMessageIcon(message)" class="text-xs"></i>
                <span class="text-sm font-medium">{{ getMessageTitle(message) }}</span>
                <Badge 
                  v-if="message.crossTabSync" 
                  value="Sync" 
                  severity="info" 
                  size="small" 
                />
              </div>
              <div class="text-xs text-surface-600">{{ getMessageDescription(message) }}</div>
            </div>
            <div class="text-xs text-surface-500">
              {{ formatRelativeTime(message.timestamp || message.receivedAt) }}
            </div>
          </div>
        </div>
      </div>

      <!-- No Updates Message -->
      <div v-else-if="isUnifiedConnected" class="text-center text-surface-500 text-sm py-4">
        <i class="pi pi-check-circle text-green-500 mb-2 block text-lg"></i>
        Đang theo dõi cập nhật real-time
      </div>

      <!-- Offline Message -->
      <div v-else class="text-center text-surface-500 text-sm py-4">
        <i class="pi pi-wifi-slash text-red-500 mb-2 block text-lg"></i>
        Không có kết nối real-time
        <div class="mt-2">
          <Button 
            label="Kết nối lại" 
            size="small" 
            @click="reconnect"
          />
        </div>
      </div>
    </div>

    <!-- Queue Processing Status -->
    <div v-if="isProcessingQueue" class="queue-status p-3 border-top-1 surface-border">
      <div class="flex align-items-center gap-2">
        <ProgressSpinner size="1rem" />
        <span class="text-sm">Đang xử lý {{ messageQueue.length }} tin nhắn chờ...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import Checkbox from 'primevue/checkbox'
import ProgressSpinner from 'primevue/progressspinner'
import { useUnifiedRealTimeManager } from '@/composables/useUnifiedRealTimeManager'

// Enhanced real-time manager
const {
  isUnifiedConnected,
  unifiedConnectionQuality,
  unifiedMessageHistory,
  messageQueue,
  isProcessingQueue,
  crossTabSupported,
  crossTabEnabled,
  notificationSettings,
  performanceMetrics,
  orderManagement
} = useUnifiedRealTimeManager()

// Local state
const showSettings = ref(false)
const showPerformanceMetrics = ref(false)

// Computed properties
const connectionIcon = computed(() => {
  if (!isUnifiedConnected.value) return 'pi pi-wifi-slash'
  
  switch (unifiedConnectionQuality.value) {
    case 'EXCELLENT': return 'pi pi-wifi'
    case 'GOOD': return 'pi pi-wifi'
    case 'POOR': return 'pi pi-exclamation-triangle'
    case 'CRITICAL': return 'pi pi-times-circle'
    default: return 'pi pi-question-circle'
  }
})

const connectionColor = computed(() => {
  if (!isUnifiedConnected.value) return '#ef4444'
  
  switch (unifiedConnectionQuality.value) {
    case 'EXCELLENT': return '#22c55e'
    case 'GOOD': return '#84cc16'
    case 'POOR': return '#f59e0b'
    case 'CRITICAL': return '#ef4444'
    default: return '#6b7280'
  }
})

const connectionStatusText = computed(() => {
  if (!isUnifiedConnected.value) return 'Không kết nối'
  
  switch (unifiedConnectionQuality.value) {
    case 'EXCELLENT': return 'Kết nối tốt'
    case 'GOOD': return 'Kết nối ổn định'
    case 'POOR': return 'Kết nối yếu'
    case 'CRITICAL': return 'Kết nối có vấn đề'
    default: return 'Đang kết nối'
  }
})

const recentMessages = computed(() => {
  return unifiedMessageHistory.value.slice(0, 10)
})

// Methods
const getMessageIcon = (message) => {
  if (message.type?.includes('PRICE')) return 'pi pi-dollar'
  if (message.type?.includes('VOUCHER')) return 'pi pi-ticket'
  if (message.type?.includes('ORDER')) return 'pi pi-shopping-cart'
  if (message.topic?.includes('gia-san-pham')) return 'pi pi-dollar'
  if (message.topic?.includes('phieu-giam-gia')) return 'pi pi-ticket'
  if (message.topic?.includes('hoa-don')) return 'pi pi-shopping-cart'
  return 'pi pi-info-circle'
}

const getMessageTitle = (message) => {
  if (message.type?.includes('PRICE')) return 'Cập nhật giá'
  if (message.type?.includes('VOUCHER')) return 'Cập nhật voucher'
  if (message.type?.includes('ORDER')) return 'Cập nhật đơn hàng'
  if (message.topic?.includes('gia-san-pham')) return 'Thay đổi giá'
  if (message.topic?.includes('phieu-giam-gia')) return 'Voucher'
  if (message.topic?.includes('hoa-don')) return 'Đơn hàng'
  return 'Thông báo'
}

const getMessageDescription = (message) => {
  if (message.productName) return message.productName
  if (message.voucherCode) return `Voucher: ${message.voucherCode}`
  if (message.orderId) return `Đơn hàng: #${message.orderId}`
  if (message.message) return message.message
  return 'Cập nhật real-time'
}

const formatRelativeTime = (timestamp) => {
  if (!timestamp) return ''
  
  const now = new Date()
  const time = new Date(timestamp)
  const diffMs = now - time
  const diffMins = Math.floor(diffMs / 60000)
  
  if (diffMins < 1) return 'Vừa xong'
  if (diffMins < 60) return `${diffMins} phút trước`
  
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours} giờ trước`
  
  return time.toLocaleDateString('vi-VN')
}

const clearAllHistory = () => {
  unifiedMessageHistory.value = []
}

const reconnect = () => {
  orderManagement.reconnect()
}

// Toggle performance metrics on double click
const handleHeaderDoubleClick = () => {
  showPerformanceMetrics.value = !showPerformanceMetrics.value
}

onMounted(() => {
  // Auto-hide settings after 10 seconds
  setTimeout(() => {
    showSettings.value = false
  }, 10000)
})
</script>

<style scoped>
.enhanced-realtime-status-panel {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  max-height: 500px;
  overflow-y: auto;
}

.message-item {
  transition: all 0.2s ease;
}

.message-item:hover {
  background: var(--surface-100);
}

.status-header {
  cursor: pointer;
}

.space-y-2 > * + * {
  margin-top: 0.5rem;
}
</style>
