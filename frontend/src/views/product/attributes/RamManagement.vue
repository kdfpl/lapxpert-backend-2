<template>
  <GenericAttributeManager :config="ramConfig" />
</template>

<script setup>
import GenericAttributeManager from './GenericAttributeManager.vue'

const ramConfig = {
  type: 'ram',
  label: 'RAM',
  title: 'Quản lý RAM',
  description: 'Quản lý danh sách RAM trong hệ thống',
  tableTitle: 'Danh sách RAM',
  dialogTitle: 'Thông tin RAM',
  icon: 'pi pi-server',
  fieldName: 'moTaRam',
  fieldLabel: 'Mô tả RAM',
  codeFieldName: 'maRam'
}
</script>
