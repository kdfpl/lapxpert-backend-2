<template>
  <GenericAttributeManager :config="brandConfig" />
</template>

<script setup>
import GenericAttributeManager from './GenericAttributeManager.vue'

const brandConfig = {
  type: 'brand',
  label: 'Thương hiệu',
  title: 'Quản lý Thương hiệu',
  description: 'Quản lý danh sách thương hiệu sản phẩm trong hệ thống',
  tableTitle: 'Danh sách Thương hiệu',
  dialogTitle: 'Thông tin Thương hiệu',
  icon: 'pi pi-tag',
  fieldName: 'moTaThuongHieu',
  fieldLabel: 'Mô tả Thương hiệu',
  codeFieldName: 'maThuongHieu'
}
</script>
