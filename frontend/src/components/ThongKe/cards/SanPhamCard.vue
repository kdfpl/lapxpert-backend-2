<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import Skeleton from 'primevue/skeleton'
import Badge from 'primevue/badge'
import Tag from 'primevue/tag'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({
      tongSo: 0,
      sapHetHang: 0,
      banChayNhat: [],
      danhMucTot: []
    })
  },
  loading: {
    type: Boolean,
    default: false
  },
  formatNumber: {
    type: Function,
    required: true
  }
})

const inventorySeverity = computed(() => {
  const percentage = (props.data.sapHetHang / props.data.tongSo) * 100
  if (percentage > 20) return 'danger'
  if (percentage > 10) return 'warning'
  return 'success'
})

const topProducts = computed(() => {
  return props.data.banChayNhat?.slice(0, 3) || []
})

const topCategories = computed(() => {
  return props.data.danhMucTot?.slice(0, 3) || []
})
</script>

<template>
  <Card class="h-full">
    <template #title>
      <div class="flex items-center gap-3">
        <div class="flex items-center justify-center bg-purple-100 dark:bg-purple-400/10 rounded-full w-12 h-12">
          <i class="pi pi-box text-purple-500 text-xl"></i>
        </div>
        <div>
          <h3 class="text-lg font-semibold text-surface-900 dark:text-surface-0 m-0">Sản Phẩm</h3>
          <p class="text-surface-600 dark:text-surface-400 text-sm m-0">Quản lý kho hàng</p>
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
        <!-- Total Products -->
        <div class="text-center p-4 bg-surface-50 dark:bg-surface-800 rounded-lg">
          <div class="text-3xl font-bold text-purple-600 dark:text-purple-400 mb-2">
            {{ formatNumber(data.tongSo) }}
          </div>
          <div class="text-surface-600 dark:text-surface-400 text-sm">
            Tổng số sản phẩm
          </div>
        </div>

        <!-- Inventory Alert -->
        <div class="p-3 border border-surface-200 dark:border-surface-700 rounded-lg">
          <div class="flex justify-between items-center mb-2">
            <span class="text-surface-700 dark:text-surface-300 text-sm font-medium">Sắp hết hàng</span>
            <Badge
              :value="formatNumber(data.sapHetHang)"
              :severity="inventorySeverity"
              class="text-xs"
            />
          </div>
          <div class="text-surface-600 dark:text-surface-400 text-xs">
            {{ data.sapHetHang }} sản phẩm cần nhập thêm
          </div>
        </div>

        <!-- Top Selling Products -->
        <div v-if="topProducts.length > 0" class="space-y-2">
          <h4 class="text-sm font-semibold text-surface-700 dark:text-surface-300 mb-3">
            Sản phẩm bán chạy
          </h4>
          <div v-for="(product, index) in topProducts" :key="product.id || index"
               class="flex items-center justify-between p-2 border border-surface-200 dark:border-surface-700 rounded-lg">
            <div class="flex items-center gap-2">
              <div class="flex items-center justify-center bg-orange-100 dark:bg-orange-400/10 rounded-full w-6 h-6">
                <span class="text-orange-600 dark:text-orange-400 text-xs font-bold">{{ index + 1 }}</span>
              </div>
              <span class="text-surface-700 dark:text-surface-300 text-sm truncate">
                {{ product.tenSanPham || product.name || 'Sản phẩm' }}
              </span>
            </div>
            <Tag
              :value="formatNumber(product.soLuongBan || product.quantity || 0)"
              severity="success"
              class="text-xs"
            />
          </div>
        </div>

        <!-- Top Categories -->
        <div v-if="topCategories.length > 0" class="space-y-2">
          <h4 class="text-sm font-semibold text-surface-700 dark:text-surface-300 mb-3">
            Danh mục tốt nhất
          </h4>
          <div v-for="(category, index) in topCategories" :key="category.id || index"
               class="flex items-center justify-between p-2 bg-teal-50 dark:bg-teal-900/20 rounded-lg">
            <span class="text-teal-700 dark:text-teal-300 text-sm">
              {{ category.moTaDanhMuc || category.tenDanhMuc || category.name || 'Danh mục' }}
            </span>
            <Badge
              :value="formatNumber(category.soLuong || category.count || 0)"
              severity="info"
              class="text-xs"
            />
          </div>
        </div>

        <!-- Quick Stats -->
        <div class="grid grid-cols-2 gap-3">
          <div class="text-center p-3 bg-green-50 dark:bg-green-900/20 rounded-lg">
            <div class="text-lg font-semibold text-green-600 dark:text-green-400">
              {{ formatNumber(data.tongSo - data.sapHetHang) }}
            </div>
            <div class="text-green-600 dark:text-green-400 text-xs">Còn hàng</div>
          </div>

          <div class="text-center p-3 bg-orange-50 dark:bg-orange-900/20 rounded-lg">
            <div class="text-lg font-semibold text-orange-600 dark:text-orange-400">
              {{ formatNumber(data.sapHetHang) }}
            </div>
            <div class="text-orange-600 dark:text-orange-400 text-xs">Cần nhập</div>
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

.space-y-2 > * + * {
  margin-top: 0.5rem;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}
</style>
