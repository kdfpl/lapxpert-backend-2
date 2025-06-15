<script setup>
import { computed } from 'vue'
import Badge from 'primevue/badge'
import ProgressBar from 'primevue/progressbar'
import BaseThongKeCard from './BaseThongKeCard.vue'
import { useRetentionSeverity } from '@/composables/useThongKeCardSeverity.js'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({
      tongSo: 0,
      moiThangNay: 0,
      tyLeGiuChan: 0,
      giaTriTrungBinh: 0
    })
  },
  loading: {
    type: Boolean,
    default: false
  },
  formatNumber: {
    type: Function,
    required: true
  },
  formatCurrency: {
    type: Function,
    required: true
  },
  formatPercentage: {
    type: Function,
    required: true
  }
})

// Use composable for severity calculation
const retentionRate = computed(() => props.data.tyLeGiuChan)
const { severity: retentionSeverity } = useRetentionSeverity(retentionRate)

const newCustomerGrowth = computed(() => {
  // Calculate percentage of new customers this month vs total
  if (props.data.tongSo === 0) return 0
  return (props.data.moiThangNay / props.data.tongSo) * 100
})

const avgValueSeverity = computed(() => {
  if (props.data.giaTriTrungBinh >= 10000000) return 'success' // 10M VND
  if (props.data.giaTriTrungBinh >= 5000000) return 'warning'  // 5M VND
  return 'info'
})
</script>

<template>
  <BaseThongKeCard
    :data="data"
    :loading="loading"
    title="Khách Hàng"
    subtitle="Thông tin khách hàng"
    icon="pi pi-users"
    icon-color="text-teal-500"
    icon-bg-color="bg-teal-100 dark:bg-teal-400/10"
  >
    <template #main-content="{ data }">
      <!-- Total Customers -->
      <div class="text-center p-4 bg-surface-50 dark:bg-surface-800 rounded-lg">
        <div class="text-3xl font-bold text-teal-600 dark:text-teal-400 mb-2">
          {{ formatNumber(data.tongSo) }}
        </div>
        <div class="text-surface-600 dark:text-surface-400 text-sm">
          Tổng số khách hàng
        </div>
      </div>

      <!-- New Customers This Month -->
      <div class="p-3 border border-surface-200 dark:border-surface-700 rounded-lg">
        <div class="flex justify-between items-center mb-2">
          <span class="text-surface-700 dark:text-surface-300 text-sm font-medium">Khách hàng mới tháng này</span>
          <Badge
            :value="formatNumber(data.moiThangNay)"
            severity="info"
            class="text-xs"
          />
        </div>
        <div class="text-surface-600 dark:text-surface-400 text-xs">
          {{ formatPercentage(newCustomerGrowth) }} tổng khách hàng
        </div>
      </div>

      <!-- Customer Retention Rate -->
      <div class="p-3 border border-surface-200 dark:border-surface-700 rounded-lg">
        <div class="flex justify-between items-center mb-2">
          <span class="text-surface-700 dark:text-surface-300 text-sm font-medium">Tỷ lệ giữ chân</span>
          <Badge
            :value="formatPercentage(data.tyLeGiuChan)"
            :severity="retentionSeverity"
            class="text-xs"
          />
        </div>
        <ProgressBar
          :value="data.tyLeGiuChan"
          :showValue="false"
          class="h-2"
        />
        <div class="text-surface-600 dark:text-surface-400 text-xs mt-1">
          Khách hàng quay lại mua hàng
        </div>
      </div>
    </template>

    <template #additional-content="{ data }">
      <!-- Average Customer Value -->
      <div class="p-3 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
        <div class="text-center">
          <div class="text-xl font-semibold text-primary-600 dark:text-primary-400 mb-1">
            {{ formatCurrency(data.giaTriTrungBinh) }}
          </div>
          <div class="text-primary-600 dark:text-primary-400 text-sm">
            Giá trị trung bình/khách hàng
          </div>
          <Badge
            :value="avgValueSeverity === 'success' ? 'Cao' : avgValueSeverity === 'warning' ? 'Trung bình' : 'Thấp'"
            :severity="avgValueSeverity"
            class="text-xs mt-2"
          />
        </div>
      </div>
    </template>

    <template #quick-stats="{ data }">
      <!-- Customer Insights -->
      <div class="grid grid-cols-2 gap-3">
        <div class="text-center p-3 bg-green-50 dark:bg-green-900/20 rounded-lg">
          <div class="text-lg font-semibold text-green-600 dark:text-green-400">
            {{ formatNumber(Math.round(data.tongSo * (data.tyLeGiuChan / 100))) }}
          </div>
          <div class="text-green-600 dark:text-green-400 text-xs">Khách trung thành</div>
        </div>

        <div class="text-center p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
          <div class="text-lg font-semibold text-blue-600 dark:text-blue-400">
            {{ formatNumber(data.moiThangNay) }}
          </div>
          <div class="text-blue-600 dark:text-blue-400 text-xs">Khách mới</div>
        </div>
      </div>
    </template>

    <template #footer-content="{ data: _data }">
      <!-- Customer Growth Indicator -->
      <div v-if="newCustomerGrowth > 0" class="text-center p-3 border border-green-200 dark:border-green-700 rounded-lg bg-green-50 dark:bg-green-900/20">
        <div class="flex items-center justify-center gap-2 mb-1">
          <i class="pi pi-arrow-up text-green-600 dark:text-green-400 text-sm"></i>
          <span class="text-green-600 dark:text-green-400 text-sm font-medium">Tăng trưởng tích cực</span>
        </div>
        <div class="text-green-600 dark:text-green-400 text-xs">
          {{ formatPercentage(newCustomerGrowth) }} khách hàng mới tháng này
        </div>
      </div>
    </template>
  </BaseThongKeCard>
</template>


