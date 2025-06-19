<script setup>
import { computed } from 'vue'
import Badge from 'primevue/badge'
import BaseThongKeCard from './BaseThongKeCard.vue'
import { useRevenueGrowthSeverity } from '@/composables/useThongKeCardSeverity.js'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({
      homNay: 0,
      tuanNay: 0,
      thangNay: 0,
      namNay: 0,
      tangTruongTheoThang: 0,
      ngayDoanhThuTotNhat: null,
      doanhThuTotNhat: 0
    })
  },
  loading: {
    type: Boolean,
    default: false
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
const tangTruongValue = computed(() => props.data.tangTruongTheoThang)
const { severity: tangTruongSeverity, icon: tangTruongIcon } = useRevenueGrowthSeverity(tangTruongValue)
</script>

<template>
  <BaseThongKeCard
    :data="data"
    :loading="loading"
    title="Doanh Thu"
    subtitle="Tổng quan doanh thu"
    icon="pi pi-dollar"
    icon-color="text-green-500"
    icon-bg-color="bg-green-100 dark:bg-green-400/10"
  >
    <template #main-content="{ data }">
      <!-- Main Revenue Display -->
      <div class="text-center p-4 bg-surface-50 dark:bg-surface-800 rounded-lg">
        <div class="text-3xl font-bold text-green-600 dark:text-green-400 mb-2">
          {{ formatCurrency(data.thangNay) }}
        </div>
        <div class="text-surface-600 dark:text-surface-400 text-sm">
          Doanh thu tháng này
        </div>
        <div v-if="data.tangTruongTheoThang !== 0" class="mt-2">
          <Badge
            :value="formatPercentage(Math.abs(data.tangTruongTheoThang))"
            :severity="tangTruongSeverity"
            class="text-xs"
          >
            <template #default>
              <i :class="tangTruongIcon" class="mr-1"></i>
              {{ formatPercentage(Math.abs(data.tangTruongTheoThang)) }}
            </template>
          </Badge>
          <span class="text-surface-600 dark:text-surface-400 text-xs ml-2">so với tháng trước</span>
        </div>
      </div>
    </template>

    <template #additional-content="{ data }">
      <!-- Revenue Breakdown -->
      <div class="grid grid-cols-2 gap-4">
        <div class="text-center p-3 border border-surface-200 dark:border-surface-700 rounded-lg">
          <div class="text-lg font-semibold text-surface-900 dark:text-surface-0">
            {{ formatCurrency(data.homNay) }}
          </div>
          <div class="text-surface-600 dark:text-surface-400 text-xs">Hôm nay</div>
        </div>

        <div class="text-center p-3 border border-surface-200 dark:border-surface-700 rounded-lg">
          <div class="text-lg font-semibold text-surface-900 dark:text-surface-0">
            {{ formatCurrency(data.tuanNay) }}
          </div>
          <div class="text-surface-600 dark:text-surface-400 text-xs">Tuần này</div>
        </div>
      </div>

      <!-- Year Revenue -->
      <div class="text-center p-3 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
        <div class="text-xl font-semibold text-primary-600 dark:text-primary-400">
          {{ formatCurrency(data.namNay) }}
        </div>
        <div class="text-primary-600 dark:text-primary-400 text-sm">Doanh thu năm nay</div>
      </div>
    </template>

    <template #footer-content="{ data }">
      <!-- Best Day -->
      <div v-if="data.ngayDoanhThuTotNhat && data.doanhThuTotNhat > 0"
           class="text-center p-3 border border-orange-200 dark:border-orange-700 rounded-lg bg-orange-50 dark:bg-orange-900/20">
        <div class="text-orange-600 dark:text-orange-400 text-sm font-medium">Ngày tốt nhất</div>
        <div class="text-lg font-semibold text-orange-700 dark:text-orange-300">
          {{ formatCurrency(data.doanhThuTotNhat) }}
        </div>
        <div class="text-orange-600 dark:text-orange-400 text-xs">
          {{ new Date(data.ngayDoanhThuTotNhat).toLocaleDateString('vi-VN') }}
        </div>
      </div>
    </template>
  </BaseThongKeCard>
</template>


