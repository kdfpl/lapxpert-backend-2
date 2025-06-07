<template>
    <Toast />
    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-ticket text-lg text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">
              Quản lý phiếu giảm giá
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              Tạo và quản lý các phiếu giảm giá cho khách hàng
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <Button
            label="Thêm phiếu giảm giá"
            icon="pi pi-plus"
            severity="success"
            @click="goToAdd(false)"
          />
          <Button
            label="Đóng nhiều phiếu"
            icon="pi pi-ban"
            severity="danger"
            outlined
            @click="confirmBatchClose"
            :disabled="!selectedVouchers || !selectedVouchers.length"
          />
        </div>
      </div>
    </div>

  <div class="card">

    <div class="font-semibold text-xl mb-4">Bộ lọc</div>

    <!-- Improved Filter Section with Better UX -->
    <div class="mb-6 border rounded-lg p-4">
      <!-- Filter Actions Row -->
      <div class="flex justify-between items-center mb-4">
        <span class="text-sm text-gray-600">Sử dụng các bộ lọc dưới đây để tìm kiếm phiếu giảm giá</span>
        <Button
          type="button"
          icon="pi pi-filter-slash"
          label="Xoá toàn bộ bộ lọc"
          outlined
          @click="clearAllFilters()"
        />
      </div>

      <!-- Filters Grid - Improved Responsive Layout -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <!-- Mã phiếu giảm giá -->
        <div>
          <label class="block mb-2">Mã phiếu giảm giá</label>
          <InputGroup>
            <Button
              v-if="filters['maPhieuGiamGia'].constraints[0].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('maPhieuGiamGia')"
            />
            <InputText
              v-model="filters['maPhieuGiamGia'].constraints[0].value"
              type="text"
              placeholder="Lọc mã"
              fluid
            />
          </InputGroup>
        </div>

        <!-- Loại giảm giá - Changed to Select -->
        <div>
          <label class="block mb-2">Loại giảm giá</label>
          <InputGroup>
            <Button
              v-if="filters['loaiGiamGia'].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('loaiGiamGia')"
            />
            <Select
              v-model="filters['loaiGiamGia'].value"
              :options="store.discountTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Chọn loại giảm giá"
              fluid
            >
              <template #option="{ option }">
                <span>{{ option.label }}</span>
              </template>
            </Select>
          </InputGroup>
        </div>

        <!-- Trạng thái -->
        <div>
          <label class="block mb-2">Trạng thái</label>
          <InputGroup>
            <Button
              v-if="filters['trangThai'].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('trangThai')"
            />
            <Select
              v-model="filters['trangThai'].value"
              :options="store.statusOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Chọn trạng thái"
              fluid
            >
              <template #option="{ option }">
                <Tag :value="option.label" :severity="store.getStatusSeverity(option.value)" />
              </template>
            </Select>
          </InputGroup>
        </div>

        <!-- Phiếu riêng tư -->
        <div>
          <label class="block mb-2">Loại phiếu</label>
          <InputGroup>
            <Button
              v-if="filters['phieuRiengTu'].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('phieuRiengTu')"
            />
            <Select
              v-model="filters['phieuRiengTu'].value"
              :options="[
                { label: 'Phiếu công khai', value: 'false' },
                { label: 'Phiếu riêng tư', value: 'true' }
              ]"
              optionLabel="label"
              optionValue="value"
              placeholder="Chọn loại phiếu"
              fluid
            />
          </InputGroup>
        </div>

        <!-- Giá trị giảm phần trăm - Range Slider (show when no discount type selected or percentage selected) -->
        <div v-if="!filters['loaiGiamGia'].value || filters['loaiGiamGia'].value === 'PHAN_TRAM'">
          <label class="block mb-4">Giá trị giảm (%)</label>
          <div class="px-3">
            <Slider
              v-model="filters['giaTriGiamPhanTram'].value"
              range
              class="mb-2"
              :max="dynamicMaxValues.giaTriGiamPhanTram"
              fluid
            />
            <div class="flex items-center justify-between px-2">
              <span>{{ formatPercentage(filters['giaTriGiamPhanTram'].value ? filters['giaTriGiamPhanTram'].value[0] : 0) }}</span>
              <span>{{ formatPercentage(filters['giaTriGiamPhanTram'].value ? filters['giaTriGiamPhanTram'].value[1] : dynamicMaxValues.giaTriGiamPhanTram) }}</span>
            </div>
          </div>
        </div>

        <!-- Giá trị giảm số tiền - Range Slider (show when no discount type selected or fixed amount selected) -->
        <div v-if="!filters['loaiGiamGia'].value || filters['loaiGiamGia'].value === 'SO_TIEN_CO_DINH'">
          <label class="block mb-4">Giá trị giảm (VND)</label>
          <div class="px-3">
            <Slider
              v-model="filters['giaTriGiamSoTien'].value"
              range
              class="mb-2"
              :max="dynamicMaxValues.giaTriGiamSoTien"
              fluid
            />
            <div class="flex items-center justify-between px-2">
              <span>{{ formatCurrency(filters['giaTriGiamSoTien'].value ? filters['giaTriGiamSoTien'].value[0] : 0) }}</span>
              <span>{{ formatCurrency(filters['giaTriGiamSoTien'].value ? filters['giaTriGiamSoTien'].value[1] : dynamicMaxValues.giaTriGiamSoTien) }}</span>
            </div>
          </div>
        </div>

        <!-- Giá trị đơn hàng tối thiểu - Range Slider -->
        <div>
          <label class="block mb-4">Đơn hàng tối thiểu</label>
          <div class="px-3">
            <Slider
              v-model="filters['giaTriDonHangToiThieu'].value"
              range
              class="mb-2"
              :max="dynamicMaxValues.giaTriDonHangToiThieu"
              fluid
            />
            <div class="flex items-center justify-between px-2">
              <span>{{ formatCurrency(filters['giaTriDonHangToiThieu'].value ? filters['giaTriDonHangToiThieu'].value[0] : 0) }}</span>
              <span>{{ formatCurrency(filters['giaTriDonHangToiThieu'].value ? filters['giaTriDonHangToiThieu'].value[1] : dynamicMaxValues.giaTriDonHangToiThieu) }}</span>
            </div>
          </div>
        </div>

        <!-- Số lượng - Range Slider -->
        <div>
          <label class="block mb-4">Số lượng phiếu</label>
          <div class="px-3">
            <Slider
              v-model="filters['soLuong'].value"
              range
              class="mb-2"
              :max="dynamicMaxValues.soLuong"
              fluid
            />
            <div class="flex items-center justify-between px-2">
              <span>{{ filters['soLuong'].value ? filters['soLuong'].value[0] : 0 }}</span>
              <span>{{ filters['soLuong'].value ? filters['soLuong'].value[1] : dynamicMaxValues.soLuong }}</span>
            </div>
          </div>
        </div>

        <!-- Ngày bắt đầu -->
        <div>
          <label class="block mb-2">Ngày bắt đầu</label>
          <InputGroup>
            <Button
              v-if="filters['ngayBatDau'].constraints[0].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('ngayBatDau')"
            />
            <DatePicker
              v-model="filters['ngayBatDau'].constraints[0].value"
              dateFormat="dd/mm/yy"
              placeholder="dd/mm/yyyy"
              showButtonBar
              showIcon
              fluid
              iconDisplay="input"
            />
          </InputGroup>
        </div>

        <!-- Ngày kết thúc -->
        <div>
          <label class="block mb-2">Ngày kết thúc</label>
          <InputGroup>
            <Button
              v-if="filters['ngayKetThuc'].constraints[0].value"
              icon="pi pi-filter-slash"
              outlined
              @click="clearSpecificFilter('ngayKetThuc')"
            />
            <DatePicker
              v-model="filters['ngayKetThuc'].constraints[0].value"
              dateFormat="dd/mm/yy"
              placeholder="dd/mm/yyyy"
              showButtonBar
              showIcon
              fluid
              iconDisplay="input"
              :minDate="filters['ngayBatDau'].constraints[0].value"
            />
          </InputGroup>
        </div>
      </div>
    </div>

    <!-- Voucher DataTable -->
    <DataTable
      v-model:selection="selectedVouchers"
      :value="filteredVouchers"
      :loading="store.loading"
      paginator
      :rows="10"
      :rowsPerPageOptions="[5, 10, 20, 50]"
      showGridlines
      dataKey="id"
      filterDisplay="menu"
      class="p-datatable-sm"
      currentPageReportTemplate="Hiển thị {first} đến {last} trong tổng số {totalRecords} phiếu"
      :globalFilterFields="['maPhieuGiamGia', 'moTa', 'loaiGiamGia']"
    >
      <template #header>
        <div class="flex justify-between items-center">
          <div class="flex">
            <IconField>
              <InputIcon>
                <i class="pi pi-search" />
              </InputIcon>
              <InputText v-model="filters['global'].value" placeholder="Tìm kiếm..." />
            </IconField>
          </div>
        </div>
      </template>

      <!-- Empty State -->
      <template #empty>
        <div class="py-8 text-center">
          <i class="pi pi-search text-2xl mb-2" />
          <p>Không tìm thấy phiếu giảm giá</p>
        </div>
      </template>

      <!-- Loading State -->
      <template #loading>
        <div class="py-8 text-center">
          <i class="pi pi-spinner pi-spin text-2xl mb-2" />
          <p>Đang tải dữ liệu...</p>
        </div>
      </template>

      <!-- Selection Column - Only show for closeable vouchers -->
      <Column selectionMode="multiple" style="width: 3rem" :exportable="false">
        <template #body="{ data }">
          <Checkbox
            v-if="canDeleteVoucher(data)"
            :binary="true"
            :modelValue="isVoucherSelected(data)"
            @update:modelValue="toggleVoucherSelection(data, $event)"
          />
        </template>
      </Column>

      <!-- STT Column -->
      <Column header="STT" style="width: 4rem" headerClass="!text-md" class="!text-sm">
        <template #body="{ index }">
          <span class="font-medium">{{ index + 1 }}</span>
        </template>
      </Column>

      <!-- Table Columns -->
      <Column
        field="maPhieuGiamGia"
        header="Mã phiếu"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      />

      <Column
        field="loaiGiamGia"
        header="Loại"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          <Tag
            :value="store.getDiscountTypeLabel(data.loaiGiamGia)"
            :severity="data.loaiGiamGia === 'PHAN_TRAM' ? 'info' : 'warning'"
          />
        </template>
      </Column>

      <Column
        field="giaTriGiam"
        header="Giá trị giảm"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDiscountValue(data.giaTriGiam, data.loaiGiamGia) }}
        </template>
      </Column>

      <Column
        field="giaTriDonHangToiThieu"
        header="Đơn tối thiểu"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.giaTriDonHangToiThieu) }}
        </template>
      </Column>

      <Column
        field="soLuongBanDau"
        header="Số lượng"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ data.soLuongDaDung || 0 }}/{{ data.soLuongBanDau }}
        </template>
      </Column>

      <Column
        field="ngayBatDau"
        header="Bắt đầu"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDateTime(data.ngayBatDau) }}
        </template>
      </Column>

      <Column
        field="ngayKetThuc"
        header="Kết thúc"
        sortable
        headerClass="!text-md"
        class="!text-sm"
      >
        <template #body="{ data }">
          {{ formatDateTime(data.ngayKetThuc) }}
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
            :value="store.getStatusLabel(data.trangThai)"
            :severity="store.getStatusSeverity(data.trangThai)"
          />
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
              @click="goToAdd(true, data.id)"
              class="!w-8 !h-8 !text-blue-500 hover:!bg-blue-50"
              v-tooltip.top="'Chỉnh sửa'"
            />
            <Button
              v-if="canDeleteVoucher(data)"
              icon="pi pi-trash"
              text
              rounded
              size="small"
              severity="danger"
              @click="confirmDeleteVoucher(data)"
              class="!w-8 !h-8 !text-red-500 hover:!bg-red-50"
              v-tooltip.top="'Đóng phiếu'"
            />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>

  <!-- Delete Voucher Dialog -->
  <Dialog
    v-model:visible="showDeleteDialog"
    modal
    header="Đóng phiếu giảm giá"
    :style="{ width: '500px' }"
    :closable="!isDeleting"
  >
    <div class="space-y-4">
      <div class="flex items-start gap-3">
        <i class="pi pi-exclamation-triangle text-orange-500 text-xl mt-1"></i>
        <div>
          <p class="font-medium mb-2">
            Bạn có chắc chắn muốn đóng phiếu giảm giá "{{ selectedVoucher?.maPhieuGiamGia }}"?
          </p>
          <p class="text-sm text-gray-600 mb-4">
            Hành động này sẽ:
          </p>
          <ul class="text-sm text-gray-600 list-disc list-inside space-y-1 mb-4">
            <li>Đặt trạng thái phiếu thành "Đã hủy"</li>
            <li>Ngăn không cho sử dụng phiếu trong các đơn hàng mới</li>
            <li>Gửi thông báo đến khách hàng được chỉ định (nếu có)</li>
            <li>Ghi lại lịch sử thay đổi trong audit trail</li>
          </ul>
          <p class="text-sm text-blue-600">
            <i class="pi pi-info-circle mr-1"></i>
            Các đơn hàng đã sử dụng phiếu này sẽ không bị ảnh hưởng.
          </p>
        </div>
      </div>

      <div class="field">
        <label for="deleteReason" class="block text-sm font-medium mb-2">
          Lý do đóng phiếu <span class="text-red-500">*</span>
        </label>
        <Textarea
          id="deleteReason"
          v-model="deleteReason"
          placeholder="Nhập lý do đóng phiếu giảm giá..."
          rows="3"
          class="w-full"
          :class="{ 'p-invalid': deleteReasonError }"
          :disabled="isDeleting"
        />
        <small v-if="deleteReasonError" class="p-error">{{ deleteReasonError }}</small>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-2">
        <Button
          label="Hủy"
          icon="pi pi-times"
          outlined
          @click="hideDeleteDialog"
          :disabled="isDeleting"
        />
        <Button
          label="Đóng phiếu"
          icon="pi pi-ban"
          severity="danger"
          :loading="isDeleting"
          @click="executeDeleteVoucher"
        />
      </div>
    </template>
  </Dialog>

  <!-- Batch Close Vouchers Dialog -->
  <Dialog
    v-model:visible="showBatchCloseDialog"
    modal
    header="Đóng nhiều phiếu giảm giá"
    :style="{ width: '600px' }"
    :closable="!isBatchClosing"
  >
    <div class="space-y-4">
      <div class="flex items-start gap-3">
        <i class="pi pi-exclamation-triangle text-orange-500 text-xl mt-1"></i>
        <div>
          <p class="font-medium mb-2">
            Bạn có chắc chắn muốn đóng {{ selectedVouchers.length }} phiếu giảm giá đã chọn?
          </p>
          <p class="text-sm text-gray-600 mb-4">
            Hành động này sẽ:
          </p>
          <ul class="text-sm text-gray-600 list-disc list-inside space-y-1 mb-4">
            <li>Đặt trạng thái các phiếu thành "Đã hủy"</li>
            <li>Ngăn không cho sử dụng các phiếu trong đơn hàng mới</li>
            <li>Gửi thông báo đến khách hàng được chỉ định (nếu có)</li>
            <li>Ghi lại lịch sử thay đổi trong audit trail</li>
          </ul>
          <p class="text-sm text-blue-600">
            <i class="pi pi-info-circle mr-1"></i>
            Các đơn hàng đã sử dụng các phiếu này sẽ không bị ảnh hưởng.
          </p>
        </div>
      </div>

      <div class="field">
        <label for="batchCloseReason" class="block text-sm font-medium mb-2">
          Lý do đóng phiếu <span class="text-red-500">*</span>
        </label>
        <Textarea
          id="batchCloseReason"
          v-model="batchCloseReason"
          placeholder="Nhập lý do đóng các phiếu giảm giá..."
          rows="3"
          class="w-full"
          :class="{ 'p-invalid': batchCloseReasonError }"
          :disabled="isBatchClosing"
        />
        <small v-if="batchCloseReasonError" class="p-error">{{ batchCloseReasonError }}</small>
      </div>

      <!-- Selected vouchers list -->
      <div class="border rounded-lg p-3 max-h-40 overflow-y-auto">
        <p class="text-sm font-medium mb-2">Phiếu giảm giá sẽ được đóng:</p>
        <div class="space-y-1">
          <div
            v-for="voucher in selectedVouchers"
            :key="voucher.id"
            class="text-sm text-gray-600 flex justify-between"
          >
            <span>{{ voucher.maPhieuGiamGia }}</span>
            <Tag
              :value="store.getStatusLabel(voucher.trangThai)"
              :severity="store.getStatusSeverity(voucher.trangThai)"
              size="small"
            />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-2">
        <Button
          label="Hủy"
          icon="pi pi-times"
          outlined
          @click="hideBatchCloseDialog"
          :disabled="isBatchClosing"
        />
        <Button
          label="Đóng tất cả"
          icon="pi pi-ban"
          severity="danger"
          :loading="isBatchClosing"
          @click="executeBatchClose"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
/**
 * PhieuGiamGia (Voucher) List Component
 * Modernized with PrimeVue components and proper DTO alignment
 * Enhanced with global search, batch operations, and improved UX
 */
import { onMounted, ref, computed } from 'vue';
import { usePhieuGiamGiaStore } from '@/stores/couponstore';
import { useRouter } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import { FilterMatchMode, FilterOperator } from '@primevue/core/api';

// PrimeVue Components
import Toast from 'primevue/toast';

import { format } from 'date-fns';
import phieuGiamGiaApi from '@/apis/coupon.js';

// Store and router setup
const store = usePhieuGiamGiaStore();
const router = useRouter();
const toast = useToast();

// Define the initial structure for filters (following DiscountList.vue pattern)
const initialFilters = {
  global: { value: null, matchMode: FilterMatchMode.CONTAINS },
  maPhieuGiamGia: {
    operator: FilterOperator.AND,
    constraints: [{ value: null, matchMode: FilterMatchMode.CONTAINS }],
  },
  loaiGiamGia: { value: null, matchMode: FilterMatchMode.EQUALS },
  trangThai: { value: null, matchMode: FilterMatchMode.EQUALS },
  phieuRiengTu: { value: null, matchMode: FilterMatchMode.EQUALS },
  giaTriGiamPhanTram: { value: [0, 100], matchMode: FilterMatchMode.BETWEEN },
  giaTriGiamSoTien: { value: [0, 1000000], matchMode: FilterMatchMode.BETWEEN },
  giaTriDonHangToiThieu: { value: [0, 10000000], matchMode: FilterMatchMode.BETWEEN },
  soLuong: { value: [0, 1000], matchMode: FilterMatchMode.BETWEEN },
  ngayBatDau: {
    operator: FilterOperator.AND,
    constraints: [{ value: null, matchMode: FilterMatchMode.CUSTOM }],
  },
  ngayKetThuc: {
    operator: FilterOperator.AND,
    constraints: [{ value: null, matchMode: FilterMatchMode.CUSTOM }],
  },
};

// Initialize filters with dynamic values
const initializeFilters = () => {
  const maxValues = dynamicMaxValues.value;
  return {
    global: { value: null, matchMode: FilterMatchMode.CONTAINS },
    maPhieuGiamGia: {
      operator: FilterOperator.AND,
      constraints: [{ value: null, matchMode: FilterMatchMode.CONTAINS }],
    },
    loaiGiamGia: { value: null, matchMode: FilterMatchMode.EQUALS },
    trangThai: { value: null, matchMode: FilterMatchMode.EQUALS },
    phieuRiengTu: { value: null, matchMode: FilterMatchMode.EQUALS },
    giaTriGiamPhanTram: { value: [0, maxValues.giaTriGiamPhanTram], matchMode: FilterMatchMode.BETWEEN },
    giaTriGiamSoTien: { value: [0, maxValues.giaTriGiamSoTien], matchMode: FilterMatchMode.BETWEEN },
    giaTriDonHangToiThieu: { value: [0, maxValues.giaTriDonHangToiThieu], matchMode: FilterMatchMode.BETWEEN },
    soLuong: { value: [0, maxValues.soLuong], matchMode: FilterMatchMode.BETWEEN },
    ngayBatDau: {
      operator: FilterOperator.AND,
      constraints: [{ value: null, matchMode: FilterMatchMode.CUSTOM }],
    },
    ngayKetThuc: {
      operator: FilterOperator.AND,
      constraints: [{ value: null, matchMode: FilterMatchMode.CUSTOM }],
    },
  };
};

const filters = ref(JSON.parse(JSON.stringify(initialFilters)));

// Dynamic max values computed from actual data
const dynamicMaxValues = computed(() => {
  const data = store.phieuGiamGiaList || [];

  if (data.length === 0) {
    // Fallback to hardcoded defaults when no data
    return {
      giaTriGiamPhanTram: 100,
      giaTriGiamSoTien: 1000000,
      giaTriDonHangToiThieu: 10000000,
      soLuong: 1000
    };
  }

  // Calculate max values from actual data
  const maxGiaTriGiamPhanTram = Math.max(
    ...data
      .filter(item => item.loaiGiamGia === 'PHAN_TRAM')
      .map(item => item.giaTriGiam || 0),
    100 // Minimum default for percentage
  );

  const maxGiaTriGiamSoTien = Math.max(
    ...data
      .filter(item => item.loaiGiamGia === 'SO_TIEN_CO_DINH')
      .map(item => item.giaTriGiam || 0),
    1000000 // Minimum default for fixed amount
  );

  const maxGiaTriDonHangToiThieu = Math.max(
    ...data.map(item => item.giaTriDonHangToiThieu || 0),
    10000000 // Minimum default
  );

  const maxSoLuong = Math.max(
    ...data.map(item => item.soLuongBanDau || 0),
    1000 // Minimum default
  );

  return {
    giaTriGiamPhanTram: Math.ceil(maxGiaTriGiamPhanTram),
    giaTriGiamSoTien: Math.ceil(maxGiaTriGiamSoTien),
    giaTriDonHangToiThieu: Math.ceil(maxGiaTriDonHangToiThieu),
    soLuong: Math.ceil(maxSoLuong)
  };
});

// Date normalization function (from DiscountList.vue)
const normalizeDateToStartOfDay = (dateInput) => {
  if (!dateInput) return null;
  try {
    const date = dateInput instanceof Date ? dateInput : new Date(dateInput);
    if (isNaN(date.getTime())) return null;
    const normalized = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    return normalized;
  } catch (e) {
    console.error('Error normalizing date:', dateInput, e);
    return null;
  }
};

// Computed filtered vouchers (following DiscountList.vue pattern)
const filteredVouchers = computed(() => {
  let data = [...store.phieuGiamGiaList];
  const globalFilter = filters.value.global.value?.toLowerCase();
  const maFilter = filters.value.maPhieuGiamGia.constraints[0].value?.toLowerCase();
  const loaiFilter = filters.value.loaiGiamGia.value;
  const trangThaiFilter = filters.value.trangThai.value;
  const phieuRiengTuFilter = filters.value.phieuRiengTu.value;
  const giaTriGiamPhanTramFilter = filters.value.giaTriGiamPhanTram.value;
  const giaTriGiamSoTienFilter = filters.value.giaTriGiamSoTien.value;
  const giaTriDonHangToiThieuFilter = filters.value.giaTriDonHangToiThieu.value;
  const soLuongFilter = filters.value.soLuong.value;
  const ngayBatDauFilter = normalizeDateToStartOfDay(filters.value.ngayBatDau.constraints[0].value);
  const ngayKetThucFilter = normalizeDateToStartOfDay(filters.value.ngayKetThuc.constraints[0].value);



  if (globalFilter) {
    data = data.filter((item) =>
      Object.values(item).some((val) => String(val).toLowerCase().includes(globalFilter)),
    );
  }
  if (maFilter) {
    data = data.filter((item) => item.maPhieuGiamGia?.toLowerCase().includes(maFilter));
  }
  if (loaiFilter) {
    data = data.filter((item) => item.loaiGiamGia === loaiFilter);
  }
  if (trangThaiFilter) {
    data = data.filter((item) => item.trangThai === trangThaiFilter);
  }

  // PhieuRiengTu filtering
  if (phieuRiengTuFilter !== null && phieuRiengTuFilter !== undefined) {
    data = data.filter((item) => {
      // Check multiple possible field names for private voucher indicator
      const isPrivate = item.phieuRiengTu === true ||
                       item.phieuRiengTu === 'true' ||
                       item.isPrivate === true ||
                       item.isPrivate === 'true' ||
                       (item.danhSachNguoiDung && item.danhSachNguoiDung.length > 0);

      return phieuRiengTuFilter === 'true' ? isPrivate : !isPrivate;
    });
  }

  // Separate filtering for percentage and fixed amount discounts
  if (giaTriGiamPhanTramFilter && giaTriGiamPhanTramFilter.length === 2) {
    data = data.filter((item) => {
      if (item.loaiGiamGia !== 'PHAN_TRAM') return true; // Only filter percentage discounts
      const value = item.giaTriGiam || 0;
      return value >= giaTriGiamPhanTramFilter[0] && value <= giaTriGiamPhanTramFilter[1];
    });
  }

  if (giaTriGiamSoTienFilter && giaTriGiamSoTienFilter.length === 2) {
    data = data.filter((item) => {
      if (item.loaiGiamGia !== 'SO_TIEN_CO_DINH') return true; // Only filter fixed amount discounts
      const value = item.giaTriGiam || 0;
      return value >= giaTriGiamSoTienFilter[0] && value <= giaTriGiamSoTienFilter[1];
    });
  }

  if (giaTriDonHangToiThieuFilter && giaTriDonHangToiThieuFilter.length === 2) {
    data = data.filter((item) => {
      const value = item.giaTriDonHangToiThieu || 0;
      return value >= giaTriDonHangToiThieuFilter[0] && value <= giaTriDonHangToiThieuFilter[1];
    });
  }
  if (soLuongFilter && soLuongFilter.length === 2) {
    data = data.filter((item) => {
      const value = item.soLuongBanDau || 0;
      return value >= soLuongFilter[0] && value <= soLuongFilter[1];
    });
  }
  if (ngayBatDauFilter) {
    data = data.filter((item) => {
      const itemDate = normalizeDateToStartOfDay(item.ngayBatDau);
      return itemDate && itemDate.getTime() === ngayBatDauFilter.getTime();
    });
  }
  if (ngayKetThucFilter) {
    data = data.filter((item) => {
      const itemDate = normalizeDateToStartOfDay(item.ngayKetThuc);
      return itemDate && itemDate.getTime() === ngayKetThucFilter.getTime();
    });
  }

  // Sort by status priority (Active, Upcoming, Ended, Cancelled)
  return data.sort((a, b) => {
    const order = {
      DA_DIEN_RA: 1,
      CHUA_DIEN_RA: 2,
      KET_THUC: 3,
      BI_HUY: 4
    };
    return order[a.trangThai] - order[b.trangThai];
  });
});

// Selection state for batch operations
const selectedVouchers = ref([]);

// Delete functionality state
const showDeleteDialog = ref(false);
const selectedVoucher = ref(null);
const deleteReason = ref('');
const deleteReasonError = ref('');
const isDeleting = ref(false);

// Batch close functionality state
const showBatchCloseDialog = ref(false);
const batchCloseReason = ref('');
const batchCloseReasonError = ref('');
const isBatchClosing = ref(false);

/**
 * Format discount value based on type
 * @param {number} value - Discount value
 * @param {string} type - Discount type (PHAN_TRAM or SO_TIEN_CO_DINH)
 * @returns {string} Formatted discount value
 */
function formatDiscountValue(value, type) {
  if (!value) return '-';

  if (type === 'PHAN_TRAM') {
    return `${value}%`;
  } else {
    return formatCurrency(value);
  }
}

/**
 * Format currency value
 * @param {number} value - Currency value
 * @returns {string} Formatted currency
 */
function formatCurrency(value) {
  if (!value) return '-';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(value);
}

/**
 * Format date time for display
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date
 */
function formatDateTime(dateString) {
  if (!dateString) return '-';
  try {
    const date = new Date(dateString);
    return format(date, 'dd/MM/yyyy HH:mm');
  } catch (error) {
    console.error('Date formatting error:', error);
    return '-';
  }
}

/**
 * Navigate to voucher form
 * @param {boolean} isEdit - Whether this is edit mode
 * @param {number|null} voucherId - Voucher ID for edit mode
 */
function goToAdd(isEdit = false, voucherId = null) {
  if (isEdit && voucherId) {
    router.push({ name: 'couponsCRUD', params: { id: voucherId } });
  } else {
    router.push({ name: 'couponsCRUD' });
  }
}

/**
 * Check if voucher can be deleted (only CHUA_DIEN_RA and DA_DIEN_RA status)
 * @param {Object} voucher - Voucher data
 * @returns {boolean} Whether voucher can be deleted
 */
function canDeleteVoucher(voucher) {
  return voucher.trangThai === 'CHUA_DIEN_RA' || voucher.trangThai === 'DA_DIEN_RA';
}

/**
 * Show delete confirmation dialog
 * @param {Object} voucher - Voucher data to delete
 */
function confirmDeleteVoucher(voucher) {
  selectedVoucher.value = voucher;
  deleteReason.value = '';
  deleteReasonError.value = '';
  showDeleteDialog.value = true;
}

/**
 * Hide delete dialog and reset state
 */
function hideDeleteDialog() {
  showDeleteDialog.value = false;
  selectedVoucher.value = null;
  deleteReason.value = '';
  deleteReasonError.value = '';
}

/**
 * Validate delete reason
 */
function validateDeleteReason() {
  deleteReasonError.value = '';

  if (!deleteReason.value || !deleteReason.value.trim()) {
    deleteReasonError.value = 'Vui lòng nhập lý do đóng phiếu';
    return false;
  }

  if (deleteReason.value.length > 500) {
    deleteReasonError.value = 'Lý do đóng phiếu không được vượt quá 500 ký tự';
    return false;
  }

  return true;
}

/**
 * Execute voucher deletion with audit trail
 */
async function executeDeleteVoucher() {
  // Validate delete reason
  if (!validateDeleteReason()) {
    return;
  }

  isDeleting.value = true;

  try {
    // Call enhanced delete method with audit information, with fallback
    try {
      await phieuGiamGiaApi.deleteVoucherWithAudit(selectedVoucher.value.id, deleteReason.value.trim());
    } catch (error) {
      console.warn('Audit delete method failed, using standard method:', error);
      await phieuGiamGiaApi.deleteVoucher(selectedVoucher.value.id);
    }

    // Hide dialog
    hideDeleteDialog();

    // Show success message
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đóng phiếu giảm giá "${selectedVoucher.value.maPhieuGiamGia}" thành công!`,
      life: 3000
    });

    // Refresh the list
    await store.fetchPhieuGiamGia();

  } catch (error) {
    console.error('Error closing voucher:', error);
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Không thể đóng phiếu giảm giá. Vui lòng thử lại sau.',
      life: 5000
    });
  } finally {
    isDeleting.value = false;
  }
}

/**
 * Clear specific filter (following DiscountList.vue pattern)
 */
function clearSpecificFilter(fieldName) {
  console.log(`Clearing filter for: ${fieldName}`);
  if (fieldName === 'global') {
    filters.value.global.value = null;
  } else if (fieldName === 'giaTriGiamPhanTram') {
    filters.value.giaTriGiamPhanTram.value = [0, dynamicMaxValues.value.giaTriGiamPhanTram];
  } else if (fieldName === 'giaTriGiamSoTien') {
    filters.value.giaTriGiamSoTien.value = [0, dynamicMaxValues.value.giaTriGiamSoTien];
  } else if (fieldName === 'giaTriDonHangToiThieu') {
    filters.value.giaTriDonHangToiThieu.value = [0, dynamicMaxValues.value.giaTriDonHangToiThieu];
  } else if (fieldName === 'soLuong') {
    filters.value.soLuong.value = [0, dynamicMaxValues.value.soLuong];
  } else if (filters.value[fieldName]?.constraints) {
    filters.value[fieldName].constraints[0].value = null;
  } else if (['trangThai', 'loaiGiamGia', 'phieuRiengTu'].includes(fieldName)) {
    filters.value[fieldName].value = null;
  } else {
    console.warn(`Unknown filter field name to clear: ${fieldName}`);
  }
}

/**
 * Clear all filters (following DiscountList.vue pattern)
 */
function clearAllFilters() {
  filters.value = initializeFilters();
}

/**
 * Format percentage values for display
 */
function formatPercentage(value) {
  if (value === null || value === undefined) return '0%';
  return `${value}%`;
}



/**
 * Check if voucher is selected for batch operations
 * @param {Object} voucher - Voucher data
 * @returns {boolean} Whether voucher is selected
 */
function isVoucherSelected(voucher) {
  return selectedVouchers.value.some(v => v.id === voucher.id);
}

/**
 * Toggle voucher selection for batch operations
 * @param {Object} voucher - Voucher data
 * @param {boolean} selected - Whether voucher is selected
 */
function toggleVoucherSelection(voucher, selected) {
  if (selected) {
    if (!isVoucherSelected(voucher)) {
      selectedVouchers.value.push(voucher);
    }
  } else {
    selectedVouchers.value = selectedVouchers.value.filter(v => v.id !== voucher.id);
  }
}

/**
 * Show batch close confirmation dialog
 */
function confirmBatchClose() {
  if (!selectedVouchers.value.length) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng chọn ít nhất một phiếu giảm giá để đóng.',
      life: 3000
    });
    return;
  }

  batchCloseReason.value = '';
  batchCloseReasonError.value = '';
  showBatchCloseDialog.value = true;
}

/**
 * Hide batch close dialog and reset state
 */
function hideBatchCloseDialog() {
  showBatchCloseDialog.value = false;
  batchCloseReason.value = '';
  batchCloseReasonError.value = '';
}

/**
 * Validate batch close reason
 */
function validateBatchCloseReason() {
  batchCloseReasonError.value = '';

  if (!batchCloseReason.value || !batchCloseReason.value.trim()) {
    batchCloseReasonError.value = 'Vui lòng nhập lý do đóng phiếu';
    return false;
  }

  if (batchCloseReason.value.length > 500) {
    batchCloseReasonError.value = 'Lý do đóng phiếu không được vượt quá 500 ký tự';
    return false;
  }

  return true;
}

/**
 * Execute batch close operation
 */
async function executeBatchClose() {
  // Validate batch close reason
  if (!validateBatchCloseReason()) {
    return;
  }

  isBatchClosing.value = true;

  try {
    const voucherIds = selectedVouchers.value.map(v => v.id);

    // Try batch close API first, fallback to individual operations
    try {
      await phieuGiamGiaApi.batchCloseVouchers(voucherIds, batchCloseReason.value.trim());

      // Hide dialog
      hideBatchCloseDialog();

      // Clear selection
      selectedVouchers.value = [];

      // Show success message
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `Đóng thành công ${voucherIds.length} phiếu giảm giá!`,
        life: 3000
      });

    } catch (batchError) {
      console.warn('Batch close API failed, falling back to individual operations:', batchError);

      // Fallback to individual operations
      let successCount = 0;
      let failureCount = 0;
      const failedVouchers = [];

      // Process each selected voucher individually
      for (const voucher of selectedVouchers.value) {
        try {
          // Call enhanced delete method with audit information, with fallback
          try {
            await phieuGiamGiaApi.deleteVoucherWithAudit(voucher.id, batchCloseReason.value.trim());
          } catch (error) {
            console.warn('Audit delete method failed, using standard method:', error);
            await phieuGiamGiaApi.deleteVoucher(voucher.id);
          }
          successCount++;
        } catch (error) {
          console.error(`Error closing voucher ${voucher.maPhieuGiamGia}:`, error);
          failureCount++;
          failedVouchers.push(voucher.maPhieuGiamGia);
        }
      }

      // Hide dialog
      hideBatchCloseDialog();

      // Clear selection
      selectedVouchers.value = [];

      // Show result message
      if (failureCount === 0) {
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đóng thành công ${successCount} phiếu giảm giá!`,
          life: 3000
        });
      } else {
        toast.add({
          severity: 'warn',
          summary: 'Hoàn thành một phần',
          detail: `Đóng thành công ${successCount} phiếu, ${failureCount} phiếu thất bại: ${failedVouchers.join(', ')}`,
          life: 5000
        });
      }
    }

    // Refresh the list
    await store.fetchPhieuGiamGia();

  } catch (error) {
    console.error('Error in batch close operation:', error);
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể đóng các phiếu giảm giá. Vui lòng thử lại sau.',
      life: 5000
    });
  } finally {
    isBatchClosing.value = false;
  }
}



// Initialize component
onMounted(async () => {
  try {
    await store.fetchPhieuGiamGia();
    // Initialize filters with dynamic max values after data is loaded
    filters.value = initializeFilters();
  } catch (error) {
    console.error('Error fetching initial data:', error);
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải dữ liệu ban đầu',
      life: 3000,
    });
  }
});
</script>
