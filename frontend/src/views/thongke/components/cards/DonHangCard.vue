<script setup>
import { computed } from 'vue'
import Badge from 'primevue/badge'
import ProgressBar from 'primevue/progressbar'
import BaseThongKeCard from './BaseThongKeCard.vue'
import { useCompletionRateSeverity } from '@/composables/useThongKeCardSeverity.js'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({
      tongSo: 0,
      choXacNhan: 0,
      dangXuLy: 0,
      hoanThanh: 0,
      daHuy: 0,
      tyLeHoanThanh: 0
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
  formatPercentage: {
    type: Function,
    required: true
  }
})

// Use composable for severity calculation
const completionRate = computed(() => props.data.tyLeHoanThanh)
const { severity: completionSeverity } = useCompletionRateSeverity(completionRate)

const statusItems = computed(() => [
  {
    label: 'Chờ xác nhận',
    value: props.data.choXacNhan,
    color: 'text-yellow-600 dark:text-yellow-400',
    bgColor: 'bg-yellow-100 dark:bg-yellow-400/10',
    icon: 'pi pi-clock'
  },
  {
    label: 'Đang xử lý',
    value: props.data.dangXuLy,
    color: 'text-blue-600 dark:text-blue-400',
    bgColor: 'bg-blue-100 dark:bg-blue-400/10',
    icon: 'pi pi-cog'
  },
  {
    label: 'Hoàn thành',
    value: props.data.hoanThanh,
    color: 'text-green-600 dark:text-green-400',
    bgColor: 'bg-green-100 dark:bg-green-400/10',
    icon: 'pi pi-check-circle'
  },
  {
    label: 'Đã hủy',
    value: props.data.daHuy,
    color: 'text-red-600 dark:text-red-400',
    bgColor: 'bg-red-100 dark:bg-red-400/10',
    icon: 'pi pi-times-circle'
  }
])
</script>

<template>
  <BaseThongKeCard
    :data="data"
    :loading="loading"
    title="Đơn Hàng"
    subtitle="Tình trạng đơn hàng"
    icon="pi pi-shopping-cart"
    icon-color="text-blue-500"
    icon-bg-color="bg-blue-100 dark:bg-blue-400/10"
  >
    <template #main-content="{ data }">
      <!-- Total Orders -->
      <div class="text-center p-4 bg-surface-50 dark:bg-surface-800 rounded-lg">
        <div class="text-3xl font-bold text-blue-600 dark:text-blue-400 mb-2">
          {{ formatNumber(data.tongSo) }}
        </div>
        <div class="text-surface-600 dark:text-surface-400 text-sm">
          Tổng số đơn hàng
        </div>
      </div>

      <!-- Completion Rate -->
      <div class="p-3 border border-surface-200 dark:border-surface-700 rounded-lg">
        <div class="flex justify-between items-center mb-2">
          <span class="text-surface-700 dark:text-surface-300 text-sm font-medium">Tỷ lệ hoàn thành</span>
          <Badge
            :value="formatPercentage(data.tyLeHoanThanh)"
            :severity="completionSeverity"
            class="text-xs"
          />
        </div>
        <ProgressBar
          :value="data.tyLeHoanThanh"
          :showValue="false"
          class="h-2"
        />
      </div>
    </template>

    <template #additional-content="{ data: _data }">
      <!-- Order Status Breakdown -->
      <div class="space-y-3">
        <div v-for="item in statusItems" :key="item.label"
             class="flex items-center justify-between p-3 border border-surface-200 dark:border-surface-700 rounded-lg">
          <div class="flex items-center gap-3">
            <div :class="['flex items-center justify-center rounded-full w-8 h-8', item.bgColor]">
              <i :class="[item.icon, item.color, 'text-sm']"></i>
            </div>
            <span class="text-surface-700 dark:text-surface-300 text-sm">{{ item.label }}</span>
          </div>
          <div :class="['font-semibold', item.color]">
            {{ formatNumber(item.value) }}
          </div>
        </div>
      </div>
    </template>

    <template #quick-stats="{ data }">
      <!-- Quick Stats -->
      <div class="grid grid-cols-2 gap-3">
        <div class="text-center p-3 bg-green-50 dark:bg-green-900/20 rounded-lg">
          <div class="text-lg font-semibold text-green-600 dark:text-green-400">
            {{ formatNumber(data.hoanThanh) }}
          </div>
          <div class="text-green-600 dark:text-green-400 text-xs">Thành công</div>
        </div>

        <div class="text-center p-3 bg-red-50 dark:bg-red-900/20 rounded-lg">
          <div class="text-lg font-semibold text-red-600 dark:text-red-400">
            {{ formatNumber(data.daHuy) }}
          </div>
          <div class="text-red-600 dark:text-red-400 text-xs">Đã hủy</div>
        </div>
      </div>
    </template>
  </BaseThongKeCard>
</template>


