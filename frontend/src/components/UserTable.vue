<template>
  <div class="card">
    <DataTable
      v-model:filters="filters"
      v-model:expandedRows="expandedRows"
      :value="sortedProcessedUsers"
      paginator
      removableSort
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20, 50]"
      showGridlines
      dataKey="id"
      filterDisplay="menu"
      :loading="loading"
      :globalFilterFields="['maNguoiDung', 'hoTen', 'email', 'soDienThoai']"
      class="p-datatable-sm"
      v-bind="getDataTableSortProps()"
      @sort="onSort"
    >
      <template #header>
        <div class="flex flex-col gap-4">
          <div class="flex flex-col lg:flex-row gap-4 items-start lg:items-center justify-between">
            <!-- Left Controls -->
            <div class="flex flex-wrap gap-2 items-center">
              <Button
                icon="pi pi-filter-slash"
                label="Xóa bộ lọc"
                outlined
                size="small"
                @click="clearFilter"
              />

              <div class="flex gap-2">
                <Button
                  icon="pi pi-plus"
                  label="Mở rộng"
                  text
                  size="small"
                  severity="secondary"
                  @click="expandAll"
                />
                <Button
                  icon="pi pi-minus"
                  label="Thu gọn"
                  text
                  size="small"
                  severity="secondary"
                  @click="collapseAll"
                />
              </div>

              <!-- Sort Indicator -->
              <div class="flex items-center gap-2 text-sm text-surface-600">
                <i :class="getSortIndicator.icon"></i>
                <span>{{ getSortIndicator.label }}</span>
              </div>
            </div>

            <!-- Right Controls -->
            <div class="flex flex-col sm:flex-row gap-3 w-full lg:w-auto">
              <!-- Filters -->
              <div class="flex flex-wrap gap-2">
                <Select
                  v-model="filters.gioiTinh.value"
                  :options="genders"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Giới tính"
                  showClear
                  class="w-32"
                />

                <Select
                  v-model="filters.trangThai.value"
                  :options="statuses"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Trạng thái"
                  showClear
                  class="w-36"
                >
                  <template #option="slotProps">
                    <Tag
                      :value="slotProps.option.label"
                      :severity="slotProps.option.value ? 'success' : 'danger'"
                    />
                  </template>
                </Select>

                <Select
                  v-if="showStaffFields"
                  v-model="filters.vaiTro.value"
                  :options="roles"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Vai trò"
                  showClear
                  class="w-32"
                />
              </div>

              <!-- Search -->
              <div class="flex gap-2 items-center">
                <IconField>
                  <InputIcon>
                    <i class="pi pi-search" />
                  </InputIcon>
                  <InputText
                    v-model="filters['global'].value"
                    placeholder="Tìm kiếm người dùng..."
                    class="w-80"
                  />
                </IconField>
                <Button
                  v-if="filters['global'].value"
                  icon="pi pi-times"
                  text
                  rounded
                  size="small"
                  severity="secondary"
                  @click="clearGlobalFilter"
                  v-tooltip.top="'Xóa tìm kiếm'"
                  class="!w-8 !h-8"
                />
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- Empty State -->
      <template #empty>
        <div class="py-8 text-center ">
          <i class="pi pi-search text-2xl mb-2" />
          <p>Không tìm thấy người dùng</p>
        </div>
      </template>

      <!-- Loading State -->
      <template #loading>
        <div class="py-8 text-center ">
          <i class="pi pi-spinner pi-spin text-2xl mb-2" />
          <p>Đang tải dữ liệu...</p>
        </div>
      </template>

      <!-- Table Columns -->
      <Column expander style="width: 3rem" headerClass="!text-md" />

      <!-- STT Column -->
      <Column header="STT" style="width: 4rem" headerClass="!text-md" class="!text-sm">
        <template #body="{ index }">
          <span class="font-medium">{{ index + 1 }}</span>
        </template>
      </Column>

      <Column
        field="maNguoiDung"
        header="Mã"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      />

      <Column field="hoTen" header="Họ tên" sortable headerClass="!text-md" class="!text-sm">
        <template #body="{ data }">
          <div class="flex items-center gap-3">
            <img v-if="data.avatar" :src="data.avatar" class="w-8 h-8 rounded-full object-cover" />
            <i v-else  class="pi pi-user text-primary" />
            <span>{{ data.hoTen }}</span>
          </div>
        </template>
      </Column>

      <Column
        field="ngaySinh"
        header="Ngày sinh"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDate(data.ngaySinh) }}
        </template>
      </Column>

      <Column
        field="gioiTinh"
        header="Giới tính"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          <Tag
            :value="formatGender(data.gioiTinh)"
            :severity="data.gioiTinh === 'NAM' ? 'info' : 'warning'"
          />
        </template>
      </Column>

      <Column field="email" header="Email" sortable headerClass="!text-md" class="!text-sm" />

      <Column
        field="soDienThoai"
        header="SĐT"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      />

      <Column
        v-if="showStaffFields"
        field="vaiTro"
        header="Vai trò"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          <Tag :value="formatRole(data.vaiTro)" severity="success" />
        </template>
      </Column>

      <Column
        field="trangThai"
        header="Trạng thái"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          <Tag
            :value="data.trangThai ? 'Hoạt động' : 'Không hoạt động'"
            :severity="data.trangThai ? 'success' : 'danger'"
          />
        </template>
      </Column>

      <Column
        field="ngayTao"
        header="Ngày tạo"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDate(data.ngayTao) }}
        </template>
      </Column>

      <Column
        field="ngayCapNhat"
        header="Ngày cập nhật"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDate(data.ngayCapNhat) }}
        </template>
      </Column>

      <Column header="Hành động" headerClass="!text-md" class="!text-sm" style="width: 120px">
        <template #body="{ data }">
          <div class="flex gap-1">
            <Button
              icon="pi pi-pencil"
              text
              rounded
              size="small"
              @click="$emit('edit', data)"
              class="!w-8 !h-8 !text-blue-500 hover:!bg-blue-50"
              v-tooltip.top="'Chỉnh sửa'"
            />
            <Button
              v-if="data.trangThai"
              icon="pi pi-trash"
              text
              rounded
              size="small"
              @click="$emit('delete', data)"
              class="!w-8 !h-8 !text-red-500 hover:!bg-red-50"
              v-tooltip.top="'Vô hiệu hóa'"
            />
            <Button
              v-else
              icon="pi pi-replay"
              text
              rounded
              size="small"
              @click="$emit('restore', data)"
              class="!w-8 !h-8 !text-green-500 hover:!bg-green-50"
              v-tooltip.top="'Khôi phục'"
            />
          </div>
        </template>
      </Column>

      <!-- Row Expansion -->
      <template #expansion="slotProps">
        <div class="p-4 bg-surface-50">
          <div class="flex items-center gap-2 mb-3">
            <i class="pi pi-map-marker text-primary"></i>
            <h5 class="font-semibold text-surface-900 m-0">Địa chỉ</h5>
          </div>
          <DataTable :value="slotProps.data.diaChis" class="p-datatable-sm">
            <!-- STT Column for Address Table -->
            <Column header="STT" style="width: 3rem" headerClass="!bg-surface-100 !text-sm" class="!text-sm">
              <template #body="{ index }">
                <span class="font-medium">{{ index + 1 }}</span>
              </template>
            </Column>
            <Column field="duong" header="Đường" headerClass="!bg-surface-100 !text-sm" class="!text-sm" />
            <Column field="phuongXa" header="Phường/Xã" headerClass="!bg-surface-100 !text-sm" class="!text-sm" />
            <Column
              field="quanHuyen"
              header="Quận/Huyện"
              headerClass="!bg-surface-100 !text-sm"
              class="!text-sm"
            />
            <Column
              field="tinhThanh"
              header="Tỉnh/Thành"
              headerClass="!bg-surface-100 !text-sm"
              class="!text-sm"
            />
            <Column field="loaiDiaChi" header="Loại" headerClass="!bg-surface-100 !text-sm" class="!text-sm" />
            <Column header="Mặc định" headerClass="!bg-surface-100 !text-sm" class="!text-sm" style="width: 100px">
              <template #body="{ data }">
                <Tag v-if="data.laMacDinh" value="Mặc định" severity="success" />
              </template>
            </Column>
          </DataTable>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { FilterMatchMode } from '@primevue/core/api'
import { useDataTableSorting } from '@/composables/useDataTableSorting'
import { useDataTableRealTime } from '@/composables/useDataTableRealTime'
import { useCustomerStore } from '@/stores/customerstore'

const props = defineProps({
  users: Array,
  loading: Boolean,
  showStaffFields: Boolean,
})

const emit = defineEmits(['refresh-users'])

// Store integration
const customerStore = useCustomerStore()

// Auto-Sorting Composable
const {
  getDataTableSortProps,
  onSort,
  applySorting,
  getSortIndicator
} = useDataTableSorting({
  defaultSortField: 'ngayCapNhat',
  defaultSortOrder: -1, // Newest first
  enableUserOverride: true
})

// Real-time DataTable integration
const realTimeDataTable = useDataTableRealTime({
  entityType: 'nguoiDung',
  storeKey: 'userTable',
  refreshCallback: async (refreshInfo) => {
    console.log('🔄 UserTable: Real-time refresh triggered:', refreshInfo)

    // Refresh customer data from store
    await customerStore.forceRefreshCustomers()

    // Emit refresh event to parent component
    emit('refresh-users')
  },
  debounceDelay: 300,
  enableSelectiveUpdates: true,
  topicFilters: ['nguoi-dung', 'user']
})

const processedUsers = computed(() =>
  props.users.map((user) => ({
    ...user,
    ngaySinh: convertToDate(user.ngaySinh),
  })),
)

// Apply auto-sorting to processed users
const sortedProcessedUsers = computed(() => {
  return applySorting(processedUsers.value)
})

const convertToDate = (dateValue) => {
  if (!dateValue) return null
  const date = new Date(dateValue)
  return isNaN(date.getTime()) ? null : date
}

const expandedRows = ref([])
const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS },
  gioiTinh: { value: null, matchMode: FilterMatchMode.EQUALS },
  trangThai: { value: null, matchMode: FilterMatchMode.EQUALS },
  vaiTro: { value: null, matchMode: FilterMatchMode.EQUALS },
})

// Options
const genders = ref([
  { label: 'Nam', value: 'NAM' },
  { label: 'Nữ', value: 'NU' },
])

const roles = ref([
  { label: 'Nhân viên', value: 'STAFF' },
  { label: 'Quản trị', value: 'ADMIN' },
])

const statuses = ref([
  { label: 'Hoạt động', value: true },
  { label: 'Không hoạt động', value: false },
])

// Formatters
/**
 * Formats an ISO date string into Vietnam timezone date and time string.
 * Following DiscountList.vue pattern for consistent timezone display
 * @param {string} dateString - The ISO date string (UTC).
 * @returns {string} Formatted date-time string in Vietnam timezone or empty string if input is invalid.
 */
const formatDate = (dateString) => {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) return '' // Check for invalid date

    // Format in Vietnam timezone (Asia/Ho_Chi_Minh)
    const options = {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'Asia/Ho_Chi_Minh',
      hour12: false // Use 24-hour format for business operations
    }
    return new Intl.DateTimeFormat('vi-VN', options).format(date)
  } catch (error) {
    console.error('Error formatting date-time:', error)
    return '' // Return empty on error
  }
}

const formatGender = (gender) => ({ NAM: 'Nam', NU: 'Nữ' })[gender] || gender

const formatRole = (role) => ({ STAFF: 'Nhân viên', ADMIN: 'Quản trị' })[role] || role

// Actions
const clearFilter = () => {
  filters.value = {
    global: { value: null, matchMode: FilterMatchMode.CONTAINS },
    gioiTinh: { value: null, matchMode: FilterMatchMode.EQUALS },
    trangThai: { value: null, matchMode: FilterMatchMode.EQUALS },
    vaiTro: { value: null, matchMode: FilterMatchMode.EQUALS },
  }
}

const expandAll = () =>
  (expandedRows.value = props.users.reduce((acc, user) => ({ ...acc, [user.id]: true }), {}))

const collapseAll = () => (expandedRows.value = [])

const clearGlobalFilter = () => {
  filters.value.global.value = null
}
</script>
