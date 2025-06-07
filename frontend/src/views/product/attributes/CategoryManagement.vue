<template>
  <GenericAttributeManager :config="categoryConfig" />
</template>

<script setup>
import GenericAttributeManager from './GenericAttributeManager.vue'

const categoryConfig = {
  type: 'category',
  label: 'Danh mục',
  title: 'Quản lý Danh mục',
  description: 'Quản lý danh sách danh mục sản phẩm trong hệ thống',
  tableTitle: 'Danh sách Danh mục',
  dialogTitle: 'Thông tin Danh mục',
  icon: 'pi pi-sitemap',
  fieldName: 'moTaDanhMuc',
  fieldLabel: 'Mô tả Danh mục'
}
</script>
