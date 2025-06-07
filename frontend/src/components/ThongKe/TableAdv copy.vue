<template>
  <div class="card">

    <DataTable
      v-model:filters="filters"
      :value="hoaDons"
      paginator
      showGridlines
      :rows="10"
      dataKey="id"
      filterDisplay="menu"
      :loading="loading"
      :globalFilterFields="[
      'global',
        'tenSanPham',
        'ngayTao',
        'giaBan',
        'trangThaiGiaoHang',
      ]"
    >
    <template #filter="{ filterModel }">
          <Select
            v-model="filterModel.value"
            :options="statuses"
            placeholder="Chọn trạng thái"
            showClear
          >
            <template #option="slotProps">
              <Tag :value="slotProps.option" :severity="getSeverity(slotProps.option)" />
            </template>
          </Select>
        </template>
      <template #header>
        <div class="flex justify-between">
          <Button
            type="button"
            icon="pi pi-filter-slash"
            label="Xóa bộ lọc"
            outlined
            @click="clearFilter()"
          />
          <IconField>
            <InputIcon>
              <i class="pi pi-search" />
            </InputIcon>
            <InputText v-model="filters['global'].value" placeholder="Keyword Search" />
          </IconField>
        </div>
      </template>
      <template #empty> Không tìm thấy đơn hàng nào hết. </template>
      <template #loading> Đang tải dữ liệu đơn hàng. Vui lòng đợi. </template>

      <Column header="STT" style="min-width: 2rem">
        <template #body="{ index }">
          {{ index + 1 }}
        </template>
      </Column>

      <Column
        header="Sản phẩm"
        filterField="thuongHieus"
        :showFilterMatchModes="false"
        :filterMenuStyle="{ width: '5 rem' }"
        style="min-width: 15rem"
      >
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <span>{{ data.tenSanPham }}</span>
          </div>
        </template>

      </Column>



      <Column header="Ngày đặt"  field="ngayTao" filterField="ngayTao"    dataType="date" style="min-width: 4rem">
        <template #body="{ data }">
          {{ formatDate(data.ngayTao) }}
        </template>

      </Column>
      <Column
        header="Giá tiền"
        filterField="tongThanhToan"
        dataType="numeric"
        style="min-width: 4rem"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.giaBan) }}
        </template>
      </Column>
      <Column
        header="Trạng thái"
        field="trangThaiGiaoHang"
        :filterMenuStyle="{ width: '14rem' }"
        style="min-width: 5rem"
      >
        <template #body="{ data }">
          <Tag
            :value="getStatusLabel(data.trangThaiGiaoHang)"
            :severity="getSeverity(data.trangThaiGiaoHang)"
          />        </template>

      </Column>

    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ThongKeService from '@/apis/dashboard'
import { FilterMatchMode, FilterOperator } from '@primevue/core/api';
const hoaDons = ref()
const filters = ref()

const statuses = ref([
  'Đang xử lý',
  'Chờ xác nhận',
  'Đã xác nhận',
  'Đang đóng gói',
  'Đang giao hàng',
  'Đã giao hàng',
  'Hoàn thành',
  'Đã hủy',
  'Yêu cầu trả hàng',
  'Đã trả hàng',
])
const loading = ref(true)

onMounted(() => {
  ThongKeService.getAllHoaDons()
    .then((response) => {
      hoaDons.value = response.data
      loading.value = false
    })
    .catch((error) => {
      console.error('Lỗi khi tải dữ liệu hóa đơn:', error)
    })
})

const initFilters = () => {
  filters.value = {
      global: { value: null, matchMode: FilterMatchMode.CONTAINS },
      tenSanPham: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.STARTS_WITH }] },
      ngayTao: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.DATE_IS }] },
      giaBan: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
      trangThaiGiaoHang: { operator: FilterOperator.OR, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
  };
 };


initFilters()

const formatDate = (date) => {
  if (!date) return 'N/A'
  return new Date(date).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

const formatCurrency = (value) => {
  if (value == null) return '0 đ'
  return value.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })
}
const clearFilter = () => {
  initFilters()
}

// Get severity for status
const getSeverity = (status) => {
  switch (status) {
    case 'Đang xử lý':
      return 'warn'
    case 'Chờ xác nhận':
      return 'warn'
    case 'Đã xác nhận':
      return 'primary'
    case 'Đang đóng gói':
      return 'info'
    case 'Đang giao hàng':
      return 'info'
    case 'Đã giao hàng':
      return 'success'
    case 'Hoàn thành':
      return 'success'
    case 'Đã hủy':
      return 'danger'
    case 'Yêu cầu trả hàng':
      return 'danger'
    case 'Đã trả hàng':
      return 'success'
    default:
      return null
  }
}
const getStatusLabel = (status) => {
  switch (status) {
    case 'DANG_XU_LY':
      return 'Đang xử lý'
    case 'CHO_XAC_NHAN':
      return 'Chờ xác nhận'
    case 'DA_XAC_NHAN':
      return 'Đã xác nhận'
    case 'DANG_DONG_GOI':
      return 'Đang đóng gói'
    case 'DANG_GIAO_HANG':
      return 'Đang giao hàng'
    case 'DA_GIAO_HANG':
      return 'Đã giao hàng'
    case 'HOAN_THANH':
      return 'Hoàn thành'
    case 'DA_HUY':
      return 'Đã hủy'
    case 'YEU_CAU_TRA_HANG':
      return 'Yêu cầu trả hàng'
    case 'DA_TRA_HANG':
      return 'Đã trả hàng'
    default:
      return 'N/A'
  }
}
</script>
