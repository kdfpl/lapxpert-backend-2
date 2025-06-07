<template>
  <GenericAttributeManager :config="gpuConfig" />
</template>

<script setup>
import GenericAttributeManager from './GenericAttributeManager.vue'

const gpuConfig = {
  type: 'gpu',
  label: 'GPU',
  title: 'Quản lý GPU',
  description: 'Quản lý danh sách GPU trong hệ thống',
  tableTitle: 'Danh sách GPU',
  dialogTitle: 'Thông tin GPU',
  icon: 'pi pi-desktop',
  fieldName: 'moTaGpu',
  fieldLabel: 'Mô tả GPU'
}
</script>
