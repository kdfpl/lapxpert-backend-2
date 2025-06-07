<template>
  <div class="order-status-badge">
    <Tag
      :value="statusInfo.label"
      :severity="statusInfo.severity"
      :icon="statusInfo.icon"
      class="status-tag"
    />
    <div v-if="showProgress" class="status-progress mt-1">
      <div class="progress-bar">
        <div 
          class="progress-fill"
          :style="{ width: `${progressPercentage}%` }"
          :class="`progress-${statusInfo.severity}`"
        ></div>
      </div>
      <span class="progress-text">{{ progressPercentage }}%</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useOrderStore } from '@/stores/orderStore'

const props = defineProps({
  status: {
    type: String,
    required: true
  },
  showProgress: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'normal', // normal, small, large
    validator: (value) => ['small', 'normal', 'large'].includes(value)
  }
})

const orderStore = useOrderStore()

const statusInfo = computed(() => {
  return orderStore.getOrderStatusInfo(props.status)
})

const progressPercentage = computed(() => {
  const statusOrder = [
    'CHO_XAC_NHAN',    // 10%
    'DA_XAC_NHAN',     // 20%
    'DANG_CHUAN_BI',   // 40%
    'DANG_GIAO_HANG',  // 70%
    'DA_GIAO_HANG',    // 90%
    'HOAN_THANH'       // 100%
  ]
  
  const currentIndex = statusOrder.indexOf(props.status)
  
  if (currentIndex === -1) {
    // Handle special statuses
    switch (props.status) {
      case 'DA_HUY':
      case 'THAT_BAI':
        return 0
      case 'TRA_HANG':
      case 'HOAN_TIEN':
        return 50
      default:
        return 0
    }
  }
  
  return Math.round(((currentIndex + 1) / statusOrder.length) * 100)
})

const tagClass = computed(() => {
  const baseClass = 'status-tag'
  const sizeClass = `status-tag-${props.size}`
  return `${baseClass} ${sizeClass}`
})
</script>

<style scoped>
.order-status-badge {
  display: inline-block;
}

.status-tag {
  font-weight: 500;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
}

.status-tag-small {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
}

.status-tag-normal {
  font-size: 0.875rem;
  padding: 0.375rem 0.75rem;
}

.status-tag-large {
  font-size: 1rem;
  padding: 0.5rem 1rem;
}

.status-progress {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background-color: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-success {
  background-color: #10b981;
}

.progress-info {
  background-color: #3b82f6;
}

.progress-warning {
  background-color: #f59e0b;
}

.progress-danger {
  background-color: #ef4444;
}

.progress-secondary {
  background-color: #6b7280;
}

.progress-text {
  font-size: 0.75rem;
  color: #6b7280;
  font-weight: 500;
  min-width: 2rem;
  text-align: right;
}

/* Custom severity colors for Vietnamese context */
:deep(.p-tag.p-tag-success) {
  background-color: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

:deep(.p-tag.p-tag-info) {
  background-color: #dbeafe;
  color: #1e40af;
  border: 1px solid #bfdbfe;
}

:deep(.p-tag.p-tag-warning) {
  background-color: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

:deep(.p-tag.p-tag-danger) {
  background-color: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

:deep(.p-tag.p-tag-secondary) {
  background-color: #f3f4f6;
  color: #374151;
  border: 1px solid #e5e7eb;
}

/* Dark mode support */
:deep(.dark .p-tag.p-tag-success) {
  background-color: #064e3b;
  color: #6ee7b7;
  border-color: #047857;
}

:deep(.dark .p-tag.p-tag-info) {
  background-color: #1e3a8a;
  color: #93c5fd;
  border-color: #2563eb;
}

:deep(.dark .p-tag.p-tag-warning) {
  background-color: #78350f;
  color: #fbbf24;
  border-color: #d97706;
}

:deep(.dark .p-tag.p-tag-danger) {
  background-color: #7f1d1d;
  color: #fca5a5;
  border-color: #dc2626;
}

:deep(.dark .p-tag.p-tag-secondary) {
  background-color: #374151;
  color: #d1d5db;
  border-color: #4b5563;
}

/* Animation for status changes */
.status-tag {
  transition: all 0.3s ease;
}

.status-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* Pulse animation for active statuses */
.status-tag.status-active {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
}

/* Special styling for critical statuses */
.status-tag[data-status="DA_HUY"],
.status-tag[data-status="THAT_BAI"] {
  position: relative;
  overflow: hidden;
}

.status-tag[data-status="DA_HUY"]::before,
.status-tag[data-status="THAT_BAI"]::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

/* Responsive design */
@media (max-width: 640px) {
  .status-tag-normal {
    font-size: 0.75rem;
    padding: 0.25rem 0.5rem;
  }
  
  .status-tag-large {
    font-size: 0.875rem;
    padding: 0.375rem 0.75rem;
  }
  
  .status-progress {
    flex-direction: column;
    align-items: stretch;
    gap: 0.25rem;
  }
  
  .progress-text {
    text-align: center;
    min-width: auto;
  }
}

/* Accessibility improvements */
.status-tag:focus {
  outline: 2px solid #3b82f6;
  outline-offset: 2px;
}

.status-tag[aria-pressed="true"] {
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* Print styles */
@media print {
  .status-tag {
    background: white !important;
    color: black !important;
    border: 1px solid black !important;
  }
  
  .progress-bar {
    border: 1px solid black;
  }
  
  .progress-fill {
    background: black !important;
  }
}
</style>
