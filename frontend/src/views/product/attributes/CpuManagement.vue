<template>
  <GenericAttributeManager :config="cpuConfig" />
</template>

<script setup>
import GenericAttributeManager from './GenericAttributeManager.vue'

const cpuConfig = {
  type: 'cpu',
  label: 'CPU',
  title: 'Quản lý CPU',
  description: 'Quản lý danh sách CPU trong hệ thống',
  tableTitle: 'Danh sách CPU',
  dialogTitle: 'Thông tin CPU',
  icon: 'pi pi-microchip',
  fieldName: 'moTaCpu',
  fieldLabel: 'Mô tả CPU',
  codeFieldName: 'maCpu'
}
</script>
