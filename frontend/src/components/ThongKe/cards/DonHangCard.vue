<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import Skeleton from 'primevue/skeleton'
import Badge from 'primevue/badge'
import ProgressBar from 'primevue/progressbar'

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

const completionSeverity = computed(() => {
  if (props.data.tyLeHoanThanh >= 80) return 'success'
  if (props.data.tyLeHoanThanh >= 60) return 'warning'
  return 'danger'
})

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
  <Card class="h-full">
    <template #title>
      <div class="flex items-center gap-3">
        <div class="flex items-center justify-center bg-blue-100 dark:bg-blue-400/10 rounded-full w-12 h-12">
          <i class="pi pi-shopping-cart text-blue-500 text-xl"></i>
        </div>
        <div>
          <h3 class="text-lg font-semibold text-surface-900 dark:text-surface-0 m-0">Đơn Hàng</h3>
          <p class="text-surface-600 dark:text-surface-400 text-sm m-0">Tình trạng đơn hàng</p>
        </div>
      </div>
    </template>
    
    <template #content>
      <div v-if="loading" class="space-y-4">
        <Skeleton height="2rem" />
        <Skeleton height="1.5rem" />
        <Skeleton height="1.5rem" />
        <Skeleton height="1.5rem" />
      </div>
      
      <div v-else class="space-y-4">
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
      </div>
    </template>
  </Card>
</template>

<style scoped>
.space-y-4 > * + * {
  margin-top: 1rem;
}

.space-y-3 > * + * {
  margin-top: 0.75rem;
}
</style>
