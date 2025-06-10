<template>
  <GenericAttributeManager :config="screenConfig" />
</template>

<script setup>
import GenericAttributeManager from './GenericAttributeManager.vue'

const screenConfig = {
  type: 'screen',
  label: 'Màn hình',
  title: 'Quản lý Màn hình',
  description: 'Quản lý danh sách màn hình trong hệ thống',
  tableTitle: 'Danh sách Màn hình',
  dialogTitle: 'Thông tin Màn hình',
  icon: 'pi pi-tablet',
  fieldName: 'moTaManHinh',
  fieldLabel: 'Mô tả Màn hình',
  codeFieldName: 'maManHinh'
}
</script>
