<template>
  <Dialog
    v-model:visible="isVisible"
    modal
    :header="dialogTitle"
    :style="{ width: '450px' }"
    :closable="!isLoading"
    :dismissableMask="!isLoading"
  >
    <div class="space-y-4">
      <!-- Dialog Icon and Message -->
      <div class="flex items-start gap-4">
        <div class="flex-shrink-0">
          <i 
            :class="[getDialogIcon(), getIconColorClass()]" 
            class="text-2xl"
          ></i>
        </div>
        <div class="flex-1">
          <p class="text-surface-700 dark:text-surface-300 leading-relaxed">
            {{ dialogMessage }}
          </p>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <Button
          :label="cancelLabel"
          severity="secondary"
          outlined
          @click="handleCancel"
          :disabled="isLoading"
        />
        <Button
          :label="confirmLabel"
          :severity="getButtonSeverity()"
          @click="handleConfirm"
          :loading="isLoading"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { inject } from 'vue'

// Inject the confirmation dialog composable
const {
  isVisible,
  dialogTitle,
  dialogMessage,
  confirmLabel,
  cancelLabel,
  isLoading,
  handleConfirm,
  handleCancel,
  getButtonSeverity,
  getDialogIcon,
  getIconColorClass
} = inject('confirmDialog')
</script>

<style scoped>
/* Additional styling if needed */
</style>
