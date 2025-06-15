<script setup>
import { computed } from 'vue'
import Card from 'primevue/card'
import Skeleton from 'primevue/skeleton'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    required: true
  },
  icon: {
    type: String,
    required: true
  },
  iconColor: {
    type: String,
    required: true
  },
  iconBgColor: {
    type: String,
    required: true
  }
})

// Computed property for icon classes
const iconClasses = computed(() => [
  props.icon,
  props.iconColor,
  'text-xl'
])

// Computed property for icon background classes
const iconBgClasses = computed(() => [
  'flex',
  'items-center',
  'justify-center',
  props.iconBgColor,
  'rounded-full',
  'w-12',
  'h-12'
])
</script>

<template>
  <Card class="h-full">
    <template #title>
      <div class="flex items-center gap-3">
        <div :class="iconBgClasses">
          <i :class="iconClasses"></i>
        </div>
        <div>
          <h3 class="text-lg font-semibold text-surface-900 dark:text-surface-0 m-0">{{ title }}</h3>
          <p class="text-surface-600 dark:text-surface-400 text-sm m-0">{{ subtitle }}</p>
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
        <!-- Main Content Slot -->
        <slot name="main-content" :data="data"></slot>
        
        <!-- Additional Content Slot -->
        <slot name="additional-content" :data="data"></slot>
        
        <!-- Quick Stats Slot -->
        <slot name="quick-stats" :data="data"></slot>
        
        <!-- Footer Content Slot -->
        <slot name="footer-content" :data="data"></slot>
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

.space-y-3 > * + * {
  margin-top: 0.75rem;
}
</style>
