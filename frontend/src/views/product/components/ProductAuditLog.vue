<template>
  <div class="audit-log-container">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h3 class="text-xl font-semibold">Lịch sử thay đổi</h3>
        <p class="text-surface-600 dark:text-surface-400">
          Theo dõi tất cả các thay đổi của sản phẩm
        </p>
      </div>
      <Button 
        label="Làm mới" 
        icon="pi pi-refresh" 
        severity="secondary" 
        outlined 
        @click="$emit('refresh')"
      />
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-8">
      <ProgressSpinner />
      <p class="mt-4 text-surface-600">Đang tải lịch sử...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="!auditHistory?.length" class="text-center py-8">
      <i class="pi pi-history text-4xl text-surface-400 mb-4 block"></i>
      <p class="text-surface-600">Chưa có lịch sử thay đổi</p>
    </div>

    <!-- Audit Timeline -->
    <Timeline v-else :value="auditHistory" class="w-full">
      <template #marker="{ item }">
        <div class="flex items-center justify-center w-8 h-8 rounded-full border-2"
             :class="getAuditMarkerClass(item.hanhDong)">
          <i :class="getAuditIcon(item.hanhDong)" class="text-sm"></i>
        </div>
      </template>
      
      <template #content="{ item }">
        <Card class="border border-surface-200 dark:border-surface-700 mb-4">
          <template #content>
            <div class="space-y-3">
              <!-- Header with Action and Time -->
              <div class="flex justify-between items-start">
                <div class="flex items-center gap-3">
                  <Badge 
                    :value="item.hanhDongDisplay || item.hanhDong" 
                    :severity="getAuditSeverity(item.hanhDong)" 
                  />
                  <span class="font-semibold text-surface-900 dark:text-surface-0">
                    {{ item.nguoiThucHien || 'Hệ thống' }}
                  </span>
                </div>
                <span class="text-sm text-surface-600 dark:text-surface-400">
                  {{ formatFullTimestamp(item.thoiGianThayDoi) }}
                </span>
              </div>
              
              <!-- Reason -->
              <div v-if="item.lyDoThayDoi" class="bg-surface-50 dark:bg-surface-800 p-3 rounded-lg">
                <div class="flex items-start gap-2">
                  <i class="pi pi-info-circle text-blue-500 mt-0.5"></i>
                  <div>
                    <p class="font-medium text-sm text-surface-700 dark:text-surface-300">Lý do thay đổi:</p>
                    <p class="text-surface-600 dark:text-surface-400">{{ item.lyDoThayDoi }}</p>
                  </div>
                </div>
              </div>
              
              <!-- Change Details -->
              <div v-if="item.chiTietThayDoi?.length" class="space-y-3">
                <h4 class="font-medium text-surface-900 dark:text-surface-0 flex items-center gap-2">
                  <i class="pi pi-list text-primary"></i>
                  Chi tiết thay đổi:
                </h4>
                
                <div class="space-y-3">
                  <div 
                    v-for="change in item.chiTietThayDoi" 
                    :key="change.field"
                    class="bg-surface-50 dark:bg-surface-800 p-4 rounded-lg"
                  >
                    <div class="mb-2">
                      <span class="font-medium text-surface-900 dark:text-surface-0">
                        {{ change.fieldName }}:
                      </span>
                    </div>
                    
                    <!-- Old Value -->
                    <div class="mb-2">
                      <label class="text-xs font-medium text-surface-600 dark:text-surface-400 uppercase tracking-wide">
                        Giá trị cũ
                      </label>
                      <div class="bg-red-50 dark:bg-red-900/20 p-3 rounded border-l-4 border-red-400 mt-1">
                        <span class="text-surface-700 dark:text-surface-300">
                          {{ change.oldValue || 'Không có' }}
                        </span>
                      </div>
                    </div>
                    
                    <!-- New Value -->
                    <div>
                      <label class="text-xs font-medium text-surface-600 dark:text-surface-400 uppercase tracking-wide">
                        Giá trị mới
                      </label>
                      <div class="bg-green-50 dark:bg-green-900/20 p-3 rounded border-l-4 border-green-400 mt-1">
                        <span class="text-surface-700 dark:text-surface-300">
                          {{ change.newValue || 'Không có' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- Additional Info -->
              <div v-if="item.ipAddress || item.userAgent" class="border-t border-surface-200 dark:border-surface-700 pt-3">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-surface-600 dark:text-surface-400">
                  <div v-if="item.ipAddress" class="flex items-center gap-2">
                    <i class="pi pi-globe"></i>
                    <span>IP: {{ item.ipAddress }}</span>
                  </div>
                  <div v-if="item.userAgent" class="flex items-center gap-2">
                    <i class="pi pi-desktop"></i>
                    <span>{{ getUserAgentInfo(item.userAgent) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </Card>
      </template>
    </Timeline>

    <!-- Load More Button -->
    <div v-if="hasMore" class="text-center mt-6">
      <Button 
        label="Tải thêm" 
        icon="pi pi-chevron-down" 
        severity="secondary" 
        outlined 
        @click="loadMore"
        :loading="loadingMore"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  productId: {
    type: [String, Number],
    required: true
  },
  auditHistory: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['refresh', 'load-more'])

// Component state
const loadingMore = ref(false)
const hasMore = ref(false) // This would be determined by the API response

// Methods
const formatFullTimestamp = (timestamp) => {
  if (!timestamp) return 'Không có thông tin'
  
  const date = new Date(timestamp)
  const time = date.toLocaleTimeString('vi-VN', { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  })
  const dateStr = date.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
  
  return `${time} ${dateStr}`
}

const getAuditIcon = (action) => {
  const iconMap = {
    'CREATE': 'pi pi-plus',
    'UPDATE': 'pi pi-pencil',
    'DELETE': 'pi pi-trash',
    'STATUS_CHANGE': 'pi pi-refresh',
    'PRICE_CHANGE': 'pi pi-dollar',
    'RESTORE': 'pi pi-undo'
  }
  return iconMap[action] || 'pi pi-circle'
}

const getAuditMarkerClass = (action) => {
  const classMap = {
    'CREATE': 'border-green-500 bg-green-50 text-green-600 dark:bg-green-900/20',
    'UPDATE': 'border-blue-500 bg-blue-50 text-blue-600 dark:bg-blue-900/20',
    'DELETE': 'border-red-500 bg-red-50 text-red-600 dark:bg-red-900/20',
    'STATUS_CHANGE': 'border-yellow-500 bg-yellow-50 text-yellow-600 dark:bg-yellow-900/20',
    'PRICE_CHANGE': 'border-purple-500 bg-purple-50 text-purple-600 dark:bg-purple-900/20',
    'RESTORE': 'border-cyan-500 bg-cyan-50 text-cyan-600 dark:bg-cyan-900/20'
  }
  return classMap[action] || 'border-surface-300 bg-surface-50 text-surface-600 dark:bg-surface-800'
}

const getAuditSeverity = (action) => {
  const severityMap = {
    'CREATE': 'success',
    'UPDATE': 'info',
    'DELETE': 'danger',
    'STATUS_CHANGE': 'warning',
    'PRICE_CHANGE': 'secondary',
    'RESTORE': 'info'
  }
  return severityMap[action] || 'secondary'
}

const getUserAgentInfo = (userAgent) => {
  if (!userAgent) return 'Không có thông tin'
  
  // Simple user agent parsing
  if (userAgent.includes('Chrome')) return 'Chrome Browser'
  if (userAgent.includes('Firefox')) return 'Firefox Browser'
  if (userAgent.includes('Safari')) return 'Safari Browser'
  if (userAgent.includes('Edge')) return 'Edge Browser'
  
  return 'Unknown Browser'
}

const loadMore = async () => {
  loadingMore.value = true
  try {
    emit('load-more')
  } finally {
    loadingMore.value = false
  }
}
</script>

<style scoped>
.audit-log-container {
  padding: 1rem;
}

/* Timeline customization */
:deep(.p-timeline-event-marker) {
  border: none !important;
  background: transparent !important;
}

:deep(.p-timeline-event-connector) {
  background: var(--surface-300) !important;
}

.dark :deep(.p-timeline-event-connector) {
  background: var(--surface-600) !important;
}

/* Card hover effect */
.audit-log-container :deep(.p-card) {
  transition: all 0.2s ease;
}

.audit-log-container :deep(.p-card:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.dark .audit-log-container :deep(.p-card:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
</style>
