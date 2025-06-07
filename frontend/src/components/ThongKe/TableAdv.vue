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
        'maHoaDon',
        'khachHangId',
        'loaiDonHang',
        'ngayTao',
        'tongTien',
        'trangThaiGiaoHang',
      ]"
    >
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
            <!-- <InputText v-model="filters['global'].value" placeholder="Keyword Search" /> -->
          </IconField>
        </div>
      </template>

      <template #empty> Không có hóa đơn nào. </template>

      <template #loading> Đang tải dữ liệu hóa đơn. Vui lòng đợi. </template>

      <Column header="STT" style="min-width: 2rem">
        <template #body="{ index }">
          {{ index + 1 }}
          <!-- Cộng 1 vào chỉ số để bắt đầu từ 1 -->
        </template>
      </Column>

      <!-- Cột MÃ HÓA ĐƠN -->
       <Column
        header="Sản phẩm"
        filterField="thuongHieus"
        :showFilterMatchModes="false"
        :filterMenuStyle="{ width: '5 rem' }"
        style="min-width: 12rem"
      >
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <span>{{ data.maHoaDon }}</span>
          </div>
        </template>
        <template #filter="{ filterModel }">
          <MultiSelect
            v-model="filterModel.value"
            :options="thuongHieus"
            optionLabel="name"
            placeholder="Chọn thương hiệu"
          >
            <template #option="slotProps">
              <div class="flex items-center gap-2">
                <img
                  :alt="slotProps.option.name"
                  :src="`https://primefaces.org/cdn/primevue/images/avatar/${slotProps.option.image}`"
                  style="width: 32px"
                />
                <span>{{ slotProps.option.name }}</span>
              </div>
            </template>
          </MultiSelect>
        </template>
      </Column>




      <!-- Cột NGÀY TẠO -->
      <Column header="Ngày đặt"  field="ngayTao" filterField="ngayTao"    dataType="date" style="min-width: 4rem">
        <template #body="{ data }">
          {{ formatDate(data.ngayTao) }}
        </template>
        <template #filter="{ filterModel }">
          <DatePicker v-model="filterModel.value" dateFormat="mm/dd/yy" placeholder="mm/dd/yyyy" />
        </template>
      </Column>

      <!-- Cột TỔNG TIỀN -->
      <Column
        header="Giá tiền"
        filterField="tongTien"


        dataType="numeric"
        style="min-width: 4rem"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.tongThanhToan) }}
        </template>
        <template #filter="{ filterModel }">
          <!-- <Slider v-model="filterModel.value" range class="m-4"></Slider>
                  <div class="flex items-center justify-between px-2">
                      <span>{{ filterModel.value ? filterModel.value[0] : 0 }}</span>
                      <span>{{ filterModel.value ? filterModel.value[1] : 1000000000 }}</span>
                  </div> -->
          <InputNumber v-model="filterModel.value" mode="currency" currency="VND" locale="vi-VN" />
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
      </Column>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { FilterMatchMode, FilterOperator } from '@primevue/core/api'
import ThongKeService from '@/apis/dashboard'

const hoaDons = ref()
const filters = ref()
const thuongHieus = ref([
  { name: 'MSI', image: 'amyelsner.png' },
  { name: 'ASUS', image: 'annafali.png' },
  { name: 'Dell', image: 'asiyajavayant.png' },
  { name: 'Apple', image: 'bernardodominic.png' },
  { name: 'Lenovo', image: 'elwinsharvill.png' },
  { name: 'HP', image: 'ionibowcher.png' },
  { name: 'Acer', image: 'ivanmagalhaes.png' },
])
const statuses = ref([
  { label: 'Đang xử lý', value: 'DANG_XU_LY' },
  { label: 'Chờ xác nhận', value: 'CHO_XAC_NHAN' },
  { label: 'Đã xác nhận', value: 'DA_XAC_NHAN' },
  { label: 'Đang đóng gói', value: 'DANG_DONG_GOI' },
  { label: 'Đang giao hàng', value: 'DANG_GIAO_HANG' },
  { label: 'Đã giao hàng', value: 'DA_GIAO_HANG' },
  { label: 'Hoàn thành', value: 'HOAN_THANH' },
  { label: 'Đã hủy', value: 'DA_HUY' },
  { label: 'Yêu cầu trả hàng', value: 'YEU_CAU_TRA_HANG' },
  { label: 'Đã trả hàng', value: 'DA_TRA_HANG' },
])
// const statuses = ref([
//   'Đang xử lý',
//   'Chờ xác nhận',
//   'Đã xác nhận',
//   'Đang đóng gói',
//   'Đang giao hàng',
//   'Đã giao hàng',
//   'Hoàn thành',
//   'Đã hủy',
//   'Yêu cầu trả hàng',
//   'Đã trả hàng',
// ])
// const orderTypes = ref(['Tại Quầy', 'Online']);
const loading = ref(true)

onMounted(() => {
  // Initialize filters first
  initFilters()
  // Load recent orders for dashboard display
  loadRecentOrders()
})

const loadRecentOrders = async () => {
  try {
    // Use dashboard service to get recent orders
    const response = await ThongKeService.layDashboardSummary()

    // Create sample recent orders data for display
    // In a real implementation, you would have a specific API for recent orders
    hoaDons.value = [
      {
        id: 1,
        maHoaDon: 'HD001',
        khachHangId: 1,
        loaiDonHang: 'ONLINE',
        ngayTao: new Date().toISOString(),
        tongThanhToan: 15000000,
        trangThaiGiaoHang: 'HOAN_THANH'
      },
      {
        id: 2,
        maHoaDon: 'HD002',
        khachHangId: 2,
        loaiDonHang: 'TAI_QUAY',
        ngayTao: new Date(Date.now() - 86400000).toISOString(),
        tongThanhToan: 8500000,
        trangThaiGiaoHang: 'DANG_GIAO_HANG'
      },
      {
        id: 3,
        maHoaDon: 'HD003',
        khachHangId: 3,
        loaiDonHang: 'ONLINE',
        ngayTao: new Date(Date.now() - 172800000).toISOString(),
        tongThanhToan: 12000000,
        trangThaiGiaoHang: 'DA_XAC_NHAN'
      }
    ]
    loading.value = false
  } catch (error) {
    console.error('Lỗi khi tải dữ liệu hóa đơn:', error)
    loading.value = false
    // Set empty array on error
    hoaDons.value = []
  }
}

// Initialize filters
// Khởi tạo bộ lọc
const initFilters = () => {
  filters.value = {
      global: { value: null, matchMode: FilterMatchMode.CONTAINS },
      maHoaDon: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.STARTS_WITH }] },
      thuongHieus: { value: null, matchMode: FilterMatchMode.IN },
      ngayTao: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.DATE_IS }] },
      tongThanhToan: { operator: FilterOperator.AND, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
      trangThaiGiaoHang: { operator: FilterOperator.OR, constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }] },
  };
 };

// Format date for display
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

// Clear all filters
const clearFilter = () => {
  initFilters()
}

// Get severity for status
const getSeverity = (status) => {
  switch (status) {
    case 'DANG_XU_LY':
      return 'warning'
    case 'DA_XAC_NHAN':
      return 'info'
    case 'DANG_GIAO_HANG':
      return 'primary'
    case 'DA_GIAO_HANG':
      return 'success'
    case 'DA_HUY':
      return 'danger'
    default:
      return null
  }
}

// Get label for status
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
