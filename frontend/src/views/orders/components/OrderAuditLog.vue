<template>
  <div class="audit-log-container">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h3 class="text-xl font-semibold">Lịch sử thay đổi</h3>
        <p class="text-surface-600 dark:text-surface-400">
          Theo dõi tất cả các thay đổi của đơn hàng
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
              <div v-if="item.lyDoThayDoi" class="text-sm text-surface-700 dark:text-surface-300">
                <strong>Lý do:</strong> {{ item.lyDoThayDoi }}
              </div>

              <!-- Changes Details -->
              <div v-if="item.changes && item.changes.length > 0" class="space-y-2">
                <h4 class="text-sm font-medium text-surface-800 dark:text-surface-200">
                  Chi tiết thay đổi:
                </h4>
                <div class="space-y-1">
                  <div
                    v-for="change in item.changes"
                    :key="change.field"
                    class="text-sm bg-surface-50 dark:bg-surface-800 p-2 rounded"
                  >
                    <div class="font-medium text-surface-700 dark:text-surface-300">
                      {{ getFieldDisplayName(change.field) }}:
                    </div>
                    <div class="flex items-center gap-2 mt-1">
                      <span class="text-red-600 dark:text-red-400 line-through">
                        {{ formatFieldValue(change.field, change.oldValue) }}
                      </span>
                      <i class="pi pi-arrow-right text-xs text-surface-500"></i>
                      <span class="text-green-600 dark:text-green-400 font-medium">
                        {{ formatFieldValue(change.field, change.newValue) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Enhanced JSON parsing for audit values -->
              <div v-else-if="item.giaTriCu || item.giaTriMoi" class="space-y-2">
                <h4 class="text-sm font-medium text-surface-800 dark:text-surface-200">
                  {{ item.hanhDong === 'CREATE' ? 'Thông tin đơn hàng:' : 'Chi tiết thay đổi:' }}
                </h4>

                <!-- For CREATE action, show new values -->
                <div v-if="item.hanhDong === 'CREATE' && item.giaTriMoi" class="text-sm bg-surface-50 dark:bg-surface-800 p-2 rounded">
                  <div class="space-y-1">
                    <div v-for="(value, key) in parseJsonValues(item.giaTriMoi)" :key="key" class="flex justify-between">
                      <span class="text-surface-600 dark:text-surface-400">{{ getFieldDisplayName(key) }}:</span>
                      <span class="font-medium">{{ formatFieldValue(key, value) }}</span>
                    </div>
                  </div>
                </div>

                <!-- For UPDATE actions, show before/after comparison -->
                <div v-else-if="item.hanhDong !== 'CREATE'" class="space-y-2">
                  <div v-for="(newValue, key) in parseJsonValues(item.giaTriMoi)" :key="key" class="text-sm bg-surface-50 dark:bg-surface-800 p-2 rounded">
                    <div class="font-medium text-surface-700 dark:text-surface-300 mb-1">
                      {{ getFieldDisplayName(key) }}:
                    </div>
                    <div class="flex items-center gap-2">
                      <span class="text-red-600 dark:text-red-400 line-through">
                        {{ formatFieldValue(key, getOldValue(item.giaTriCu, key)) }}
                      </span>
                      <i class="pi pi-arrow-right text-xs text-surface-500"></i>
                      <span class="text-green-600 dark:text-green-400 font-medium">
                        {{ formatFieldValue(key, newValue) }}
                      </span>
                    </div>
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
        :loading="loadingMore"
        @click="$emit('load-more')"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// Props
// const props = defineProps({ // Currently unused
//   orderId: {
//     type: [String, Number],
//     required: true
//   },
//   auditHistory: {
//     type: Array,
//     default: () => []
//   },
//   loading: {
//     type: Boolean,
//     default: false
//   }
// })

// Emits
// const emit = defineEmits(['refresh', 'load-more']) // Currently unused

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
    'STATUS_CHANGE': 'pi pi-refresh',
    'PAYMENT_STATUS_CHANGE': 'pi pi-credit-card',
    'CANCEL': 'pi pi-times',
    'DELETE': 'pi pi-trash'
  }
  return iconMap[action] || 'pi pi-info'
}

const getAuditMarkerClass = (action) => {
  const classMap = {
    'CREATE': 'bg-green-100 border-green-300 text-green-600',
    'UPDATE': 'bg-blue-100 border-blue-300 text-blue-600',
    'STATUS_CHANGE': 'bg-orange-100 border-orange-300 text-orange-600',
    'PAYMENT_STATUS_CHANGE': 'bg-purple-100 border-purple-300 text-purple-600',
    'CANCEL': 'bg-red-100 border-red-300 text-red-600',
    'DELETE': 'bg-red-100 border-red-300 text-red-600'
  }
  return classMap[action] || 'bg-surface-100 border-surface-300 text-surface-600'
}

const getAuditSeverity = (action) => {
  const severityMap = {
    'CREATE': 'success',
    'UPDATE': 'info',
    'STATUS_CHANGE': 'warning',
    'PAYMENT_STATUS_CHANGE': 'secondary',
    'CANCEL': 'danger',
    'DELETE': 'danger'
  }
  return severityMap[action] || 'secondary'
}

const getFieldDisplayName = (field) => {
  const fieldMap = {
    'maHoaDon': 'Mã hóa đơn',
    'loaiHoaDon': 'Loại hóa đơn',
    'trangThaiDonHang': 'Trạng thái đơn hàng',
    'trangThaiThanhToan': 'Trạng thái thanh toán',
    'tongThanhToan': 'Tổng thanh toán',
    'khachHangId': 'Khách hàng',
    'nhanVienId': 'Nhân viên',
    'phiVanChuyen': 'Phí vận chuyển',
    'ghiChu': 'Ghi chú',
    'lyDoHuy': 'Lý do hủy'
  }
  return fieldMap[field] || field
}

const formatFieldValue = (field, value) => {
  if (value === null || value === undefined) return 'Không có'

  // Format currency fields
  if (['tongThanhToan', 'phiVanChuyen'].includes(field)) {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(value)
  }

  // Format status fields
  if (field === 'trangThaiDonHang') {
    const statusMap = {
      'CHO_XAC_NHAN': 'Chờ xác nhận',
      'DA_XAC_NHAN': 'Đã xác nhận',
      'DANG_XU_LY': 'Đang xử lý',
      'CHO_GIAO_HANG': 'Chờ giao hàng',
      'DANG_GIAO_HANG': 'Đang giao hàng',
      'HOAN_THANH': 'Hoàn thành',
      'DA_HUY': 'Đã hủy'
    }
    return statusMap[value] || value
  }

  if (field === 'trangThaiThanhToan') {
    const paymentStatusMap = {
      'CHUA_THANH_TOAN': 'Chưa thanh toán',
      'DA_THANH_TOAN': 'Đã thanh toán',
      'THANH_TOAN_MOT_PHAN': 'Thanh toán một phần',
      'HOAN_TIEN': 'Hoàn tiền'
    }
    return paymentStatusMap[value] || value
  }

  if (field === 'loaiHoaDon') {
    const typeMap = {
      'ONLINE': 'Trực tuyến',
      'TAI_QUAY': 'Tại quầy'
    }
    return typeMap[value] || value
  }

  return value
}

// const formatJsonValues = (jsonString) => { // Currently unused
//   try {
//     const obj = JSON.parse(jsonString)
//     return Object.entries(obj)
//       .map(([key, value]) => `${getFieldDisplayName(key)}: ${formatFieldValue(key, value)}`)
//       .join('\n')
//   } catch (e) {
//     return jsonString
//   }
// }

// Enhanced JSON parsing for better audit display
const parseJsonValues = (jsonString) => {
  if (!jsonString) return {}
  try {
    return typeof jsonString === 'string' ? JSON.parse(jsonString) : jsonString
  } catch (e) {
    console.warn('Failed to parse JSON:', jsonString)
    return {}
  }
}

// Get old value from JSON string
const getOldValue = (oldJsonString, key) => {
  const oldValues = parseJsonValues(oldJsonString)
  return oldValues[key] || 'Không có'
}
</script>

<style scoped>
.audit-log-container {
  max-width: 100%;
}

/* Timeline customization */
:deep(.p-timeline-event-content) {
  padding-left: 1rem;
}

:deep(.p-timeline-event-marker) {
  border: none !important;
  background: transparent !important;
}

/* Card hover effects */
:deep(.p-card) {
  transition: all 0.2s ease;
}

:deep(.p-card:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

/* Badge styling */
:deep(.p-badge) {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
}

/* Pre formatting */
pre {
  font-family: 'Courier New', monospace;
  font-size: 0.75rem;
  line-height: 1.4;
  color: inherit;
}
</style>
