<template>
  <GenericAttributeManager :config="colorConfig" />
</template>

<script setup>
import GenericAttributeManager from './GenericAttributeManager.vue'

const colorConfig = {
  type: 'colors',
  label: 'Màu sắc',
  title: 'Quản lý Màu sắc',
  description: 'Quản lý danh sách màu sắc sản phẩm trong hệ thống',
  tableTitle: 'Danh sách Màu sắc',
  dialogTitle: 'Thông tin Màu sắc',
  icon: 'pi pi-palette',
  fieldName: 'moTaMauSac',
  fieldLabel: 'Mô tả Màu sắc',
  codeFieldName: 'maMauSac'
}
</script>
