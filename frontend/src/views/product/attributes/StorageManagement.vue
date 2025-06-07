<template>
  <GenericAttributeManager :config="storageConfig" />
</template>

<script setup>
import GenericAttributeManager from './GenericAttributeManager.vue'

const storageConfig = {
  type: 'storage',
  label: 'Ổ cứng',
  title: 'Quản lý Ổ cứng',
  description: 'Quản lý danh sách ổ cứng trong hệ thống',
  tableTitle: 'Danh sách Ổ cứng',
  dialogTitle: 'Thông tin Ổ cứng',
  icon: 'pi pi-database',
  fieldName: 'moTaOCung',
  fieldLabel: 'Mô tả Ổ cứng'
}
</script>
