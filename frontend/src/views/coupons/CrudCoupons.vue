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
              {{ isEditMode ? 'Cập nhật phiếu giảm giá' : 'Thêm phiếu giảm giá mới' }}
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              {{ isEditMode ? 'Chỉnh sửa thông tin phiếu giảm giá' : 'Tạo phiếu giảm giá mới trong hệ thống' }}
            </p>
          </div>
        </div>
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          size="small"
          @click="cancel"
          v-tooltip.left="'Quay lại'"
        />
      </div>
    </div>

    <!-- Form Section -->
    <div class="mb-8">
      <div class="flex gap-6">
        <!-- Voucher Form - Dynamic width based on private voucher toggle -->
        <div :class="isPrivateVoucher ? 'flex-1' : 'w-full'">
          <div class="card">
            <h3 class="text-lg font-medium mb-4">Thông tin phiếu giảm giá</h3>

            <div class="flex flex-col gap-4">
              <!-- Voucher Code -->
              <div class="field">
                <label for="voucherCode" class="block text-sm font-medium mb-2">
                  Mã phiếu giảm giá <span class="text-red-500">*</span>
                </label>
                <InputText
                  id="voucherCode"
                  v-model="voucher.maPhieuGiamGia"
                  placeholder="Nhập mã phiếu giảm giá..."
                  class="w-full"
                  :class="{ 'p-invalid': errors.maPhieuGiamGia }"
                  @blur="validateField('maPhieuGiamGia')"
                />
                <small v-if="errors.maPhieuGiamGia" class="p-error">{{ errors.maPhieuGiamGia }}</small>
              </div>

              <!-- Discount Type -->
              <div class="field">
                <label for="discountType" class="block text-sm font-medium mb-2">
                  Loại giảm giá <span class="text-red-500">*</span>
                </label>
                <Select
                  id="discountType"
                  v-model="voucher.loaiGiamGia"
                  :options="store.discountTypeOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Chọn loại giảm giá"
                  class="w-full"
                  :class="{ 'p-invalid': errors.loaiGiamGia }"
                  @change="handleDiscountTypeChange"
                />
                <small v-if="errors.loaiGiamGia" class="p-error">{{ errors.loaiGiamGia }}</small>
              </div>

              <!-- Discount Value -->
              <div class="field">
                <label for="discountValue" class="block text-sm font-medium mb-2">
                  Giá trị giảm <span class="text-red-500">*</span>
                </label>
                <InputNumber
                  id="discountValue"
                  v-model="voucher.giaTriGiam"
                  :placeholder="getDiscountValuePlaceholder()"
                  :suffix="voucher.loaiGiamGia === 'PHAN_TRAM' ? '%' : ' VND'"
                  :min="0"
                  :max="voucher.loaiGiamGia === 'PHAN_TRAM' ? 100 : undefined"
                  :minFractionDigits="voucher.loaiGiamGia === 'PHAN_TRAM' ? 2 : 0"
                  :maxFractionDigits="voucher.loaiGiamGia === 'PHAN_TRAM' ? 2 : 0"
                  class="w-full"
                  :class="{ 'p-invalid': errors.giaTriGiam }"
                  @blur="validateDiscountValue"
                />
                <small v-if="errors.giaTriGiam" class="p-error">{{ errors.giaTriGiam }}</small>
              </div>

              <!-- Minimum Order Value -->
              <div class="field">
                <label for="minOrderValue" class="block text-sm font-medium mb-2">
                  Giá trị đơn hàng tối thiểu
                </label>
                <InputNumber
                  id="minOrderValue"
                  v-model="voucher.giaTriDonHangToiThieu"
                  placeholder="Nhập giá trị tối thiểu..."
                  suffix=" VND"
                  :min="0"
                  class="w-full"
                  :class="{ 'p-invalid': errors.giaTriDonHangToiThieu }"
                  @blur="validateField('giaTriDonHangToiThieu')"
                />
                <small v-if="errors.giaTriDonHangToiThieu" class="p-error">{{ errors.giaTriDonHangToiThieu }}</small>
              </div>

              <!-- Date Range -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="field">
                  <label for="startDate" class="block text-sm font-medium mb-2">
                    Ngày bắt đầu <span class="text-red-500">*</span>
                  </label>
                  <DatePicker
                    id="startDate"
                    v-model="voucher.ngayBatDau"
                    showTime
                    hourFormat="24"
                    dateFormat="dd/mm/yy"
                    placeholder="Chọn ngày bắt đầu"
                    class="w-full"
                    :class="{ 'p-invalid': errors.ngayBatDau }"
                    @date-select="validateDateRange"
                  />
                  <small v-if="errors.ngayBatDau" class="p-error">{{ errors.ngayBatDau }}</small>
                </div>

                <div class="field">
                  <label for="endDate" class="block text-sm font-medium mb-2">
                    Ngày kết thúc <span class="text-red-500">*</span>
                  </label>
                  <DatePicker
                    id="endDate"
                    v-model="voucher.ngayKetThuc"
                    showTime
                    hourFormat="24"
                    dateFormat="dd/mm/yy"
                    placeholder="Chọn ngày kết thúc"
                    class="w-full"
                    :class="{ 'p-invalid': errors.ngayKetThuc }"
                    @date-select="validateDateRange"
                  />
                  <small v-if="errors.ngayKetThuc" class="p-error">{{ errors.ngayKetThuc }}</small>
                </div>
              </div>

              <!-- Quantity (only for public vouchers) -->
              <div v-if="!isPrivateVoucher" class="field">
                <label for="quantity" class="block text-sm font-medium mb-2">
                  Số lượng phiếu <span class="text-red-500">*</span>
                </label>
                <InputNumber
                  id="quantity"
                  v-model="voucher.soLuongBanDau"
                  placeholder="Nhập số lượng phiếu..."
                  :min="1"
                  class="w-full"
                  :class="{ 'p-invalid': errors.soLuongBanDau }"
                  @blur="validateField('soLuongBanDau')"
                />
                <small v-if="errors.soLuongBanDau" class="p-error">{{ errors.soLuongBanDau }}</small>
              </div>

              <!-- Description -->
              <div class="field">
                <label for="description" class="block text-sm font-medium mb-2">
                  Mô tả
                </label>
                <Textarea
                  id="description"
                  v-model="voucher.moTa"
                  placeholder="Nhập mô tả cho phiếu giảm giá..."
                  rows="3"
                  class="w-full"
                />
              </div>

              <!-- Private Voucher Toggle -->
              <div class="field">
                <div class="flex items-center gap-3">
                  <Checkbox
                    id="privateVoucher"
                    v-model="isPrivateVoucher"
                    :binary="true"
                    @change="handlePrivateVoucherToggle"
                  />
                  <label for="privateVoucher" class="text-sm font-medium">
                    Phiếu giảm giá riêng tư
                  </label>
                </div>
                <small class="text-gray-500">
                  Phiếu giảm giá riêng tư chỉ áp dụng cho khách hàng được chọn
                </small>
              </div>

              <!-- Audit Reason (for edit mode) -->
              <div v-if="isEditMode" class="field">
                <label for="auditReason" class="block text-sm font-medium mb-2">
                  Lý do thay đổi <span class="text-red-500">*</span>
                </label>
                <Textarea
                  id="auditReason"
                  v-model="auditReason"
                  placeholder="Nhập lý do thay đổi thông tin phiếu giảm giá..."
                  rows="2"
                  class="w-full"
                  :class="{ 'p-invalid': errors.auditReason }"
                  @blur="validateField('auditReason')"
                />
                <small v-if="errors.auditReason" class="p-error">{{ errors.auditReason }}</small>
                <small class="text-gray-500">
                  Lý do này sẽ được ghi lại trong lịch sử thay đổi để theo dõi audit trail
                </small>
              </div>

              <!-- Form Actions -->
              <div class="flex gap-3 pt-4">
                <Button
                  :label="isEditMode ? 'Cập nhật' : 'Tạo phiếu'"
                  :icon="isEditMode ? 'pi pi-check' : 'pi pi-plus'"
                  :severity="isEditMode ? 'info' : 'success'"
                  :loading="isSubmitting"
                  @click="submitVoucher"
                  class="flex-1"
                />
                <Button
                  v-if="isEditMode && canCloseVoucher"
                  label="Đóng phiếu"
                  icon="pi pi-ban"
                  severity="danger"
                  outlined
                  :loading="isClosing"
                  @click="showCloseDialog = true"
                  class="flex-1"
                />
                <Button
                  label="Hủy"
                  icon="pi pi-times"
                  outlined
                  @click="cancel"
                  class="flex-1"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Customer Assignment Table (positioned alongside form when private voucher is enabled) -->
        <div v-if="isPrivateVoucher" class="flex-1">
          <div class="card">
            <h3 class="text-lg font-medium mb-4">Chọn khách hàng</h3>

            <!-- Customer Search -->
            <div class="mb-4">
              <IconField>
                <InputIcon>
                  <i class="pi pi-search" />
                </InputIcon>
                <InputText
                  v-model="customerSearchQuery"
                  placeholder="Tìm kiếm khách hàng..."
                  class="w-full"
                />
              </IconField>
            </div>

            <!-- Selected Customers Summary -->
            <div v-if="selectedCustomers.length > 0" class="mb-4 p-3 bg-blue-50 rounded-lg">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-blue-800">
                  Đã chọn {{ selectedCustomers.length }} khách hàng
                </span>
                <Button
                  icon="pi pi-times"
                  text
                  size="small"
                  @click="clearSelectedCustomers"
                  class="!text-blue-600"
                />
              </div>
            </div>

            <!-- Customer Selection Table -->
            <div class="border rounded-lg overflow-hidden">
              <DataTable
                :value="filteredCustomers"
                v-model:selection="selectedCustomers"
                selectionMode="checkbox"
                dataKey="id"
                :paginator="true"
                :rows="5"
                :loading="customerStore.loading"
                class="p-datatable-sm"
                @selection-change="handleCustomerSelectionChange"
              >
                <template #empty>
                  <div class="py-4 text-center text-gray-500">
                    Không tìm thấy khách hàng
                  </div>
                </template>

                <Column selectionMode="multiple" style="width: 3rem" />

                <Column field="hoTen" header="Họ tên" sortable>
                  <template #body="{ data }">
                    <div class="font-medium">{{ data.hoTen }}</div>
                  </template>
                </Column>

                <Column field="email" header="Email" sortable>
                  <template #body="{ data }">
                    <div class="text-sm text-gray-600">{{ data.email }}</div>
                  </template>
                </Column>

                <Column field="soDienThoai" header="Số điện thoại" sortable>
                  <template #body="{ data }">
                    <div class="text-sm">{{ data.soDienThoai }}</div>
                  </template>
                </Column>
              </DataTable>
            </div>

            <!-- Validation Message for Private Vouchers -->
            <small v-if="errors.selectedCustomers" class="p-error block mt-2">
              {{ errors.selectedCustomers }}
            </small>
          </div>
        </div>
      </div>
    </div>

    <!-- Audit Log Section -->
    <div v-if="isEditMode && auditTrail.length > 0" class="card">
      <div class="flex items-center gap-2 mb-4">
        <i class="pi pi-history text-primary"></i>
        <span class="font-semibold text-xl">Lịch sử thay đổi</span>
        <div class="flex items-center gap-2 text-sm text-surface-500 ml-auto">
          <i class="pi pi-clock"></i>
          <span>{{ auditTrail.length }} mục</span>
        </div>
      </div>

      <div class="space-y-4 max-h-96 overflow-y-auto">
        <div
          v-for="(entry, index) in auditTrail"
          :key="entry.rawEntry?.id || index"
          class="border-l-4 pl-4 py-3 rounded-r-lg"
          :class="getAuditBorderColor(entry.action)"
        >
          <!-- Header with action and timestamp -->
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-3">
              <i :class="[getAuditIcon(entry.action), getAuditIconColor(entry.action), 'text-lg']"></i>
              <span class="font-medium text-base">{{ entry.action }}</span>
              <span class="text-sm text-surface-500">{{ formatAuditDate(entry.timestamp) }}</span>
            </div>
          </div>

            <!-- Audit information distributed horizontally -->
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <!-- Left side: User and Reason -->
              <div class="space-y-3">
                <div class="text-base text-gray-700">
                  <strong class="text-gray-900">Người thực hiện:</strong>
                  <span class="font-medium ml-2">{{ entry.user || 'Hệ thống' }}</span>
                </div>

                <div v-if="entry.reason" class="text-base text-gray-700">
                  <strong class="text-gray-900">Lý do:</strong>
                  <span class="italic ml-2">{{ entry.reason }}</span>
                </div>
              </div>

              <!-- Right side: Change details -->
              <div v-if="entry.changes && entry.changes.length > 0">
                <div class="text-sm bg-gray-50 rounded-lg p-4">
                  <strong class="text-gray-900 text-base block mb-3">Chi tiết thay đổi:</strong>
                  <div class="space-y-2">
                    <div v-for="change in entry.changes" :key="change.field" class="flex items-start gap-3">
                      <span class="font-medium text-gray-700 min-w-0 flex-shrink-0 text-sm">{{ change.fieldName }}:</span>
                      <div class="min-w-0 flex-1 text-sm">
                        <span class="text-red-600 line-through">{{ change.oldValue }}</span>
                        <span class="mx-2 text-gray-400">→</span>
                        <span class="text-green-600 font-medium">{{ change.newValue }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      <!-- Empty state -->
      <div v-if="auditTrail.length === 0" class="text-center py-8 text-surface-500">
        <i class="pi pi-history text-2xl mb-2"></i>
        <p class="text-base">Chưa có lịch sử thay đổi</p>
      </div>
    </div>

    <!-- Close Voucher Dialog -->
    <Dialog
      v-model:visible="showCloseDialog"
      modal
      header="Đóng phiếu giảm giá"
      :style="{ width: '500px' }"
      :closable="!isClosing"
    >
      <div class="space-y-4">
        <div class="flex items-start gap-3">
          <i class="pi pi-exclamation-triangle text-orange-500 text-xl mt-1"></i>
          <div>
            <p class="font-medium mb-2">
              Bạn có chắc chắn muốn đóng phiếu giảm giá "{{ voucher.maPhieuGiamGia }}"?
            </p>
            <p class="text-sm text-gray-600 mb-4">
              Hành động này sẽ:
            </p>
            <ul class="text-sm text-gray-600 list-disc list-inside space-y-1 mb-4">
              <li>Đặt trạng thái phiếu thành "Kết thúc"</li>
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
          <label for="closeReason" class="block text-sm font-medium mb-2">
            Lý do đóng phiếu <span class="text-red-500">*</span>
          </label>
          <Textarea
            id="closeReason"
            v-model="closeReason"
            placeholder="Nhập lý do đóng phiếu giảm giá..."
            rows="3"
            class="w-full"
            :class="{ 'p-invalid': errors.closeReason }"
            :disabled="isClosing"
          />
          <small v-if="errors.closeReason" class="p-error">{{ errors.closeReason }}</small>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            outlined
            @click="hideCloseVoucherDialog"
            :disabled="isClosing"
          />
          <Button
            label="Đóng phiếu"
            icon="pi pi-ban"
            severity="danger"
            :loading="isClosing"
            @click="confirmCloseVoucher"
          />
        </div>
      </template>
    </Dialog>
</template>

<script setup>
/**
 * PhieuGiamGia (Voucher) Form Component
 * Modernized with proper DTO alignment and comprehensive validation
 */
import { ref, onMounted, computed } from 'vue';
import { useCustomerStore } from '@/stores/customerstore';
import { usePhieuGiamGiaStore } from '@/stores/couponstore';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'primevue/usetoast';
import phieuGiamGiaApi from '@/apis/coupon.js';

// Stores and utilities
const store = usePhieuGiamGiaStore();
const customerStore = useCustomerStore();
const router = useRouter();
const route = useRoute();
const toast = useToast();

// Form state
const voucher = ref({
  id: null,
  maPhieuGiamGia: '',
  loaiGiamGia: null, // Fixed: Use proper enum field name
  giaTriGiam: null,
  giaTriDonHangToiThieu: null,
  ngayBatDau: null,
  ngayKetThuc: null,
  soLuongBanDau: null,
  moTa: '',
  trangThai: 'CHUA_DIEN_RA',
  danhSachNguoiDung: []
});

// Component state
const isPrivateVoucher = ref(false);
const selectedCustomers = ref([]);
const customerSearchQuery = ref('');
const isSubmitting = ref(false);
const errors = ref({});

// Audit trail and enhanced delete functionality
const auditReason = ref('');
const auditTrail = ref([]);
const showCloseDialog = ref(false);
const closeReason = ref('');
const isClosing = ref(false);

// Computed properties
const isEditMode = computed(() => !!route.params.id);

const filteredCustomers = computed(() => {
  if (!customerSearchQuery.value) {
    return customerStore.customers;
  }

  const query = customerSearchQuery.value.toLowerCase();
  return customerStore.customers.filter(customer =>
    customer.hoTen?.toLowerCase().includes(query) ||
    customer.email?.toLowerCase().includes(query) ||
    customer.soDienThoai?.toLowerCase().includes(query)
  );
});

// Computed properties for audit and close functionality
const canCloseVoucher = computed(() => {
  return voucher.value.trangThai &&
         voucher.value.trangThai !== 'KET_THUC' &&
         voucher.value.trangThai !== 'BI_HUY' &&
         voucher.value.id;
});

// Validation functions
function validateField(fieldName) {
  errors.value[fieldName] = '';

  switch (fieldName) {
    case 'maPhieuGiamGia':
      if (!voucher.value.maPhieuGiamGia?.trim()) {
        errors.value[fieldName] = 'Mã phiếu giảm giá không được để trống';
      } else if (voucher.value.maPhieuGiamGia.length > 50) {
        errors.value[fieldName] = 'Mã phiếu giảm giá không được vượt quá 50 ký tự';
      }
      break;

    case 'loaiGiamGia':
      if (!voucher.value.loaiGiamGia) {
        errors.value[fieldName] = 'Vui lòng chọn loại giảm giá';
      }
      break;

    case 'giaTriGiam':
      if (!voucher.value.giaTriGiam || voucher.value.giaTriGiam <= 0) {
        errors.value[fieldName] = 'Giá trị giảm phải lớn hơn 0';
      } else if (voucher.value.loaiGiamGia === 'PHAN_TRAM') {
        if (voucher.value.giaTriGiam > 100) {
          errors.value[fieldName] = 'Phần trăm giảm giá không được vượt quá 100%';
        } else if (voucher.value.giaTriGiam < 0.01) {
          errors.value[fieldName] = 'Phần trăm giảm giá phải từ 0.01%';
        }
      } else if (voucher.value.loaiGiamGia === 'SO_TIEN_CO_DINH') {
        if (voucher.value.giaTriGiam > 100000000) {
          errors.value[fieldName] = 'Số tiền giảm giá không được vượt quá 100,000,000 VND';
        }
      }
      break;

    case 'giaTriDonHangToiThieu':
      if (voucher.value.giaTriDonHangToiThieu && voucher.value.giaTriDonHangToiThieu < 0) {
        errors.value[fieldName] = 'Giá trị đơn hàng tối thiểu không được âm';
      } else if (voucher.value.giaTriDonHangToiThieu &&
                 voucher.value.loaiGiamGia === 'SO_TIEN_CO_DINH' &&
                 voucher.value.giaTriGiam &&
                 voucher.value.giaTriDonHangToiThieu <= voucher.value.giaTriGiam) {
        errors.value[fieldName] = 'Giá trị đơn hàng tối thiểu phải lớn hơn số tiền giảm giá';
      }
      break;

    case 'soLuongBanDau':
      if (!isPrivateVoucher.value && (!voucher.value.soLuongBanDau || voucher.value.soLuongBanDau <= 0)) {
        errors.value[fieldName] = 'Số lượng phiếu phải lớn hơn 0';
      }
      break;

    case 'auditReason':
      if (isEditMode.value && (!auditReason.value || !auditReason.value.trim())) {
        errors.value[fieldName] = 'Vui lòng nhập lý do thay đổi';
      } else if (auditReason.value && auditReason.value.length > 500) {
        errors.value[fieldName] = 'Lý do thay đổi không được vượt quá 500 ký tự';
      }
      break;

    case 'closeReason':
      if (!closeReason.value || !closeReason.value.trim()) {
        errors.value[fieldName] = 'Vui lòng nhập lý do đóng phiếu';
      } else if (closeReason.value.length > 500) {
        errors.value[fieldName] = 'Lý do đóng phiếu không được vượt quá 500 ký tự';
      }
      break;
  }
}

function validateDateRange() {
  errors.value.ngayBatDau = '';
  errors.value.ngayKetThuc = '';

  if (!voucher.value.ngayBatDau) {
    errors.value.ngayBatDau = 'Vui lòng chọn ngày bắt đầu';
    return;
  }

  if (!voucher.value.ngayKetThuc) {
    errors.value.ngayKetThuc = 'Vui lòng chọn ngày kết thúc';
    return;
  }

  const startDate = new Date(voucher.value.ngayBatDau);
  const endDate = new Date(voucher.value.ngayKetThuc);

  if (startDate >= endDate) {
    errors.value.ngayKetThuc = 'Ngày kết thúc phải sau ngày bắt đầu';
  }

  // Check minimum duration (1 hour)
  const minDuration = 60 * 60 * 1000; // 1 hour in milliseconds
  if (endDate.getTime() - startDate.getTime() < minDuration) {
    errors.value.ngayKetThuc = 'Thời gian diễn ra phải ít nhất 1 giờ';
  }
}

function validateForm() {
  // Clear previous errors
  errors.value = {};

  // Validate all fields
  validateField('maPhieuGiamGia');
  validateField('loaiGiamGia');
  validateField('giaTriGiam');
  validateField('giaTriDonHangToiThieu');
  validateField('soLuongBanDau');
  validateDateRange();

  // Validate audit reason for edit mode
  if (isEditMode.value) {
    validateField('auditReason');
  }

  // Validate private voucher customer selection
  if (isPrivateVoucher.value && selectedCustomers.value.length === 0) {
    errors.value.selectedCustomers = 'Vui lòng chọn ít nhất một khách hàng cho phiếu giảm giá riêng tư';
  }

  // Check if there are any errors
  return Object.keys(errors.value).length === 0 || Object.values(errors.value).every(error => !error);
}
// Event handlers
function validateDiscountValue() {
  validateField('giaTriGiam');
  // Re-validate minimum order value since it depends on discount value for fixed amount discounts
  if (voucher.value.giaTriDonHangToiThieu) {
    validateField('giaTriDonHangToiThieu');
  }
}

function handleDiscountTypeChange() {
  validateField('loaiGiamGia');
  // Reset discount value when type changes
  voucher.value.giaTriGiam = null;
  // Re-validate minimum order value since the logic depends on discount type
  if (voucher.value.giaTriDonHangToiThieu) {
    validateField('giaTriDonHangToiThieu');
  }
}

function handlePrivateVoucherToggle() {
  if (!isPrivateVoucher.value) {
    selectedCustomers.value = [];
    errors.value.selectedCustomers = '';
  }
}

function handleCustomerSelectionChange() {
  if (isPrivateVoucher.value) {
    errors.value.selectedCustomers = '';
  }
}

function clearSelectedCustomers() {
  selectedCustomers.value = [];
}

function getDiscountValuePlaceholder() {
  if (voucher.value.loaiGiamGia === 'PHAN_TRAM') {
    return 'Nhập phần trăm giảm (0-100)';
  } else {
    return 'Nhập số tiền giảm';
  }
}

// ==================== AUDIT TRAIL HELPER METHODS ====================

/**
 * Get icon for audit trail entry based on action type
 */
function getAuditIcon(action) {
  const iconMap = {
    'Tạo phiếu giảm giá mới': 'pi pi-plus-circle',
    'Cập nhật thông tin phiếu giảm giá': 'pi pi-pencil',
    'Đóng phiếu giảm giá': 'pi pi-ban',
    'Thay đổi trạng thái': 'pi pi-refresh',
    'Thay đổi khách hàng': 'pi pi-users'
  };
  return iconMap[action] || 'pi pi-info-circle';
}

/**
 * Get icon color for audit action
 */
function getAuditIconColor(action) {
  const colorMap = {
    'Tạo phiếu giảm giá mới': 'text-green-600',
    'Cập nhật thông tin phiếu giảm giá': 'text-blue-600',
    'Đóng phiếu giảm giá': 'text-red-600',
    'Thay đổi trạng thái': 'text-orange-600',
    'Thay đổi khách hàng': 'text-purple-600'
  };
  return colorMap[action] || 'text-gray-600';
}

/**
 * Get border color for audit entry
 */
function getAuditBorderColor(action) {
  const colorMap = {
    'Tạo phiếu giảm giá mới': 'border-green-200 bg-green-50',
    'Cập nhật thông tin phiếu giảm giá': 'border-blue-200 bg-blue-50',
    'Đóng phiếu giảm giá': 'border-red-200 bg-red-50',
    'Thay đổi trạng thái': 'border-orange-200 bg-orange-50',
    'Thay đổi khách hàng': 'border-purple-200 bg-purple-50'
  };
  return colorMap[action] || 'border-gray-200 bg-gray-50';
}

/**
 * Format audit trail timestamp for display
 */
function formatAuditDate(timestamp) {
  if (!timestamp) return '';

  const date = new Date(timestamp);
  return date.toLocaleString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

/**
 * Load complete audit history from backend
 */
async function loadAuditHistory(voucherId) {
  try {
    const auditHistory = await phieuGiamGiaApi.getAuditHistory(voucherId);
    return auditHistory.map(entry => ({
      action: getActionDisplayName(entry.hanhDong),
      timestamp: entry.thoiGianThayDoi,
      user: entry.nguoiThucHien || 'Hệ thống',
      reason: entry.lyDoThayDoi,
      changes: entry.giaTriCu && entry.giaTriMoi ?
        compareAuditValues(JSON.parse(entry.giaTriCu), JSON.parse(entry.giaTriMoi)) : [],
      rawEntry: entry
    }));
  } catch (error) {
    console.error('Error loading audit history:', error);
    return [];
  }
}

/**
 * Get display name for action type
 */
function getActionDisplayName(action) {
  const actionMap = {
    'CREATE': 'Tạo phiếu giảm giá mới',
    'UPDATE': 'Cập nhật thông tin phiếu giảm giá',
    'DELETE': 'Đóng phiếu giảm giá',
    'STATUS_CHANGE': 'Thay đổi trạng thái'
  };
  return actionMap[action] || action;
}

/**
 * Parse audit trail from backend audit fields (fallback for old data)
 */
function parseAuditTrail(voucherData) {
  const trail = [];

  if (!voucherData) return trail;

  // Add creation entry
  if (voucherData.ngayTao) {
    trail.push({
      action: 'Tạo phiếu giảm giá mới',
      timestamp: voucherData.ngayTao,
      user: voucherData.nguoiTao,
      reason: 'Tạo phiếu giảm giá từ admin panel',
      changes: []
    });
  }

  // Add update entries from audit fields
  if (voucherData.lyDoThayDoi && voucherData.giaTriCu && voucherData.giaTriMoi) {
    try {
      const oldValues = JSON.parse(voucherData.giaTriCu);
      const newValues = JSON.parse(voucherData.giaTriMoi);
      const changes = compareAuditValues(oldValues, newValues);

      trail.push({
        action: 'Cập nhật thông tin phiếu giảm giá',
        timestamp: voucherData.ngayCapNhat,
        user: voucherData.nguoiCapNhat,
        reason: voucherData.lyDoThayDoi,
        changes: changes
      });
    } catch (error) {
      console.warn('Could not parse audit trail data:', error);
    }
  }

  return trail.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
}

/**
 * Compare old and new audit values to generate change list
 */
function compareAuditValues(oldValues, newValues) {
  const changes = [];
  const fieldNames = {
    maPhieuGiamGia: 'Mã phiếu',
    loaiGiamGia: 'Loại giảm giá',
    giaTriGiam: 'Giá trị giảm',
    giaTriDonHangToiThieu: 'Giá trị tối thiểu',
    ngayBatDau: 'Ngày bắt đầu',
    ngayKetThuc: 'Ngày kết thúc',
    soLuongBanDau: 'Số lượng',
    trangThai: 'Trạng thái',
    moTa: 'Mô tả'
  };

  for (const [field, newValue] of Object.entries(newValues)) {
    const oldValue = oldValues[field];
    if (oldValue !== newValue) {
      changes.push({
        field: field,
        fieldName: fieldNames[field] || field,
        oldValue: formatAuditValue(field, oldValue),
        newValue: formatAuditValue(field, newValue)
      });
    }
  }

  return changes;
}

/**
 * Format audit values for display
 */
function formatAuditValue(field, value) {
  if (value === null || value === undefined) return 'Không có';

  switch (field) {
    case 'loaiGiamGia':
      return value === 'PHAN_TRAM' ? 'Phần trăm' : 'Số tiền cố định';
    case 'trangThai': {
      const statusMap = {
        'CHUA_DIEN_RA': 'Chưa diễn ra',
        'DA_DIEN_RA': 'Đang diễn ra',
        'KET_THUC': 'Kết thúc',
        'BI_HUY': 'Đã hủy'
      };
      return statusMap[value] || value;
    }
    case 'giaTriGiam':
    case 'giaTriDonHangToiThieu':
      return new Intl.NumberFormat('vi-VN').format(value) + ' VND';
    case 'ngayBatDau':
    case 'ngayKetThuc':
      return new Date(value).toLocaleDateString('vi-VN');
    default:
      return String(value);
  }
}

// Data loading
async function loadVoucherData() {
  const voucherId = route.params.id;
  if (voucherId) {
    try {
      const voucherData = await phieuGiamGiaApi.getVoucherById(voucherId);
      if (voucherData) {
        // Map backend data to form
        voucher.value = {
          ...voucherData,
          ngayBatDau: voucherData.ngayBatDau ? new Date(voucherData.ngayBatDau) : null,
          ngayKetThuc: voucherData.ngayKetThuc ? new Date(voucherData.ngayKetThuc) : null,
        };

        // Handle private voucher customer selection
        if (voucherData.danhSachNguoiDung && voucherData.danhSachNguoiDung.length > 0) {
          isPrivateVoucher.value = true;
          // Load selected customers based on IDs
          selectedCustomers.value = customerStore.customers.filter(customer =>
            voucherData.danhSachNguoiDung.includes(customer.id)
          );
        }

        // Load complete audit history from backend
        try {
          auditTrail.value = await loadAuditHistory(voucherId);
        } catch (error) {
          console.warn('Could not load audit history, falling back to legacy method:', error);
          // Fallback to parsing audit trail from voucher data
          auditTrail.value = parseAuditTrail(voucherData);
        }
      }
    } catch (error) {
      console.error('Error loading voucher data:', error);
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không thể tải dữ liệu phiếu giảm giá',
        life: 5000
      });
    }
  }
}

// Form submission
async function submitVoucher() {
  if (!validateForm()) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng kiểm tra lại thông tin đã nhập',
      life: 5000
    });
    return;
  }

  isSubmitting.value = true;

  try {
    // Prepare clean voucher data - only send required fields for creation
    // Backend handles audit fields, status calculation, and soLuongDaDung automatically
    const voucherData = {
      maPhieuGiamGia: voucher.value.maPhieuGiamGia.trim(),
      loaiGiamGia: voucher.value.loaiGiamGia,
      giaTriGiam: Number(voucher.value.giaTriGiam),
      giaTriDonHangToiThieu: voucher.value.giaTriDonHangToiThieu ? Number(voucher.value.giaTriDonHangToiThieu) : null,
      ngayBatDau: voucher.value.ngayBatDau.toISOString(),
      ngayKetThuc: voucher.value.ngayKetThuc.toISOString(),
      soLuongBanDau: isPrivateVoucher.value
        ? selectedCustomers.value.length
        : Number(voucher.value.soLuongBanDau),
      moTa: voucher.value.moTa?.trim() || null,
      danhSachNguoiDung: isPrivateVoucher.value
        ? selectedCustomers.value.map(customer => customer.id)
        : []
    };

    // For edit mode, include the ID
    if (isEditMode.value) {
      voucherData.id = voucher.value.id;
    }

    console.log('Sending voucher data:', voucherData);

    if (isEditMode.value) {
      // Use audit trail method for updates with fallback
      const reason = auditReason.value.trim() || 'Cập nhật thông tin phiếu giảm giá từ admin panel';
      try {
        await phieuGiamGiaApi.updateVoucherWithAudit(voucher.value.id, voucherData, reason);
      } catch (error) {
        console.warn('Audit method failed, using standard method:', error);
        await phieuGiamGiaApi.updateVoucher(voucher.value.id, voucherData);
      }
    } else {
      // Use audit trail method for creation with fallback
      const reason = 'Tạo phiếu giảm giá mới từ admin panel';
      try {
        await phieuGiamGiaApi.createVoucherWithAudit(voucherData, reason);
      } catch (error) {
        console.warn('Audit method failed, using standard method:', error);
        await phieuGiamGiaApi.createVoucher(voucherData);
      }
    }

    // Navigate back to list
    router.push('/discounts/coupons');

  } catch (error) {
    console.error('Error submitting voucher:', error);
    // Error toast is handled in store
  } finally {
    isSubmitting.value = false;
  }
}

function cancel() {
  router.push('/discounts/coupons');
}

// ==================== ENHANCED CLOSE VOUCHER FUNCTIONALITY ====================

/**
 * Hide close voucher dialog
 */
function hideCloseVoucherDialog() {
  showCloseDialog.value = false;
  closeReason.value = '';
  errors.value.closeReason = '';
}

/**
 * Confirm and execute voucher closure with audit trail
 */
async function confirmCloseVoucher() {
  // Validate close reason
  validateField('closeReason');

  if (errors.value.closeReason) {
    return;
  }

  isClosing.value = true;

  try {
    // Call enhanced close method with audit information, with fallback
    try {
      await phieuGiamGiaApi.deleteVoucherWithAudit(voucher.value.id, closeReason.value.trim());
    } catch (error) {
      console.warn('Audit close method failed, using standard method:', error);
      await phieuGiamGiaApi.deleteVoucher(voucher.value.id);
    }

    // Hide dialog
    hideCloseVoucherDialog();

    // Show success message
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đóng phiếu giảm giá "${voucher.value.maPhieuGiamGia}" thành công!`,
      life: 3000
    });

    // Navigate back to list
    router.push('/discounts/coupons');

  } catch (error) {
    console.error('Error closing voucher:', error);
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Không thể đóng phiếu giảm giá. Vui lòng thử lại sau.',
      life: 5000
    });
  } finally {
    isClosing.value = false;
  }
}

// Initialize component
onMounted(async () => {
  try {
    // Load customers first
    await customerStore.fetchCustomers();

    // Then load voucher data if editing
    await loadVoucherData();
  } catch (error) {
    console.error('Error initializing component:', error);
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải dữ liệu ban đầu',
      life: 5000
    });
  }
});
</script>
