<template>
  <Fluid>
    <Toast />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-users text-lg text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">
              {{ isEditMode ? 'Cập nhật nhân viên' : 'Thêm nhân viên mới' }}
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              {{ isEditMode ? 'Chỉnh sửa thông tin nhân viên' : 'Tạo hồ sơ nhân viên mới trong hệ thống' }}
            </p>
          </div>
        </div>
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          size="small"
          @click="goBack"
          v-tooltip.left="'Quay lại'"
        />
      </div>
    </div>

    <form @submit.prevent="handleSubmit">
      <div class="flex flex-col gap-6">

        <!-- Personal Information Card -->
        <div class="card">
          <div class="flex items-center gap-2 mb-4">
            <i class="pi pi-user text-primary"></i>
            <span class="font-semibold text-xl">Thông tin cá nhân</span>
          </div>

          <div class="grid grid-cols-12 gap-6">
            <!-- Avatar Section -->
            <div class="col-span-12 lg:col-span-4">
              <div class="flex flex-col gap-4">
                <label class="font-semibold">Ảnh đại diện <span class="text-red-500">*</span></label>

                <!-- Avatar Preview -->
                <div class="flex justify-center">
                  <div class="relative">
                    <div class="w-32 h-32 border-2 border-dashed border-surface-300 rounded-lg flex items-center justify-center overflow-hidden bg-surface-50">
                      <img
                        v-if="imagePreview"
                        :src="imagePreview"
                        alt="Avatar preview"
                        class="w-full h-full object-cover"
                      />
                      <div v-else class="text-center">
                        <i class="pi pi-camera text-3xl text-surface-400 mb-2 block"></i>
                        <span class="text-surface-600 text-sm">Chọn ảnh</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Upload Button -->
                <FileUpload
                  mode="basic"
                  name="avatar"
                  accept="image/*"
                  :maxFileSize="2000000"
                  chooseLabel="Chọn ảnh"
                  @select="onAvatarSelect"
                  :class="{
                    'p-invalid': submitted && !form.avatar && !imagePreview,
                  }"
                  customUpload
                />

                <small
                  class="text-red-500"
                  v-if="submitted && !form.avatar && !imagePreview"
                >
                  Vui lòng chọn ảnh đại diện
                </small>
              </div>
            </div>

            <!-- Basic Information Fields -->
            <div class="col-span-12 lg:col-span-8">
              <div class="grid grid-cols-12 gap-4">

                <!-- Full Name -->
                <div class="col-span-12 md:col-span-6">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Họ và tên <span class="text-red-500">*</span>
                    </label>
                    <InputText
                      v-model="form.hoTen"
                      placeholder="Nhập họ và tên"
                      :invalid="submitted && !form.hoTen"
                    />
                    <small class="text-red-500" v-if="submitted && !form.hoTen">
                      Họ tên là bắt buộc
                    </small>
                  </div>
                </div>

                <!-- Gender -->
                <div class="col-span-12 md:col-span-6">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Giới tính <span class="text-red-500">*</span>
                    </label>
                    <Select
                      v-model="form.gioiTinh"
                      :options="genderOptions"
                      optionLabel="label"
                      optionValue="value"
                      placeholder="Chọn giới tính"
                      :invalid="submitted && form.gioiTinh === null"
                    />
                    <small class="text-red-500" v-if="submitted && form.gioiTinh === null">
                      Giới tính là bắt buộc
                    </small>
                  </div>
                </div>

                <!-- Birth Date -->
                <div class="col-span-12 md:col-span-6">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Ngày sinh <span class="text-red-500">*</span>
                    </label>
                    <DatePicker
                      v-model="form.ngaySinh"
                      dateFormat="dd/mm/yy"
                      :showIcon="true"
                      :maxDate="maxBirthDate"
                      placeholder="Chọn ngày sinh"
                      :invalid="submitted && !form.ngaySinh"
                    />
                    <small class="text-red-500" v-if="submitted && !form.ngaySinh">
                      Ngày sinh là bắt buộc
                    </small>
                  </div>
                </div>

                <!-- CCCD/CMND with QR Scanner -->
                <div class="col-span-12 md:col-span-6">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      CCCD/CMND <span class="text-red-500">*</span>
                    </label>
                    <InputGroup>
                      <InputText
                        v-model="form.cccd"
                        placeholder="Nhập số CCCD/CMND"
                        :invalid="(submitted && !form.cccd) || cccdError"
                        @blur="validateCCCD"
                      />
                      <Button
                        icon="pi pi-qrcode"
                        severity="info"
                        @click="showQRScanner = true"
                        v-tooltip.top="'Quét QR CCCD'"
                      />
                    </InputGroup>
                    <small class="text-red-500" v-if="submitted && !form.cccd">
                      CCCD/CMND là bắt buộc
                    </small>
                    <small class="text-red-500" v-if="cccdError">
                      {{ cccdError }}
                    </small>
                  </div>
                </div>

                <!-- Email -->
                <div class="col-span-12 md:col-span-6">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Email <span class="text-red-500">*</span>
                    </label>
                    <InputText
                      v-model="form.email"
                      placeholder="Nhập email"
                      :invalid="(submitted && !form.email) || emailError"
                      @blur="validateEmail"
                    />
                    <small class="text-red-500" v-if="submitted && !form.email">
                      Email là bắt buộc
                    </small>
                    <small class="text-red-500" v-if="emailError">
                      {{ emailError }}
                    </small>
                  </div>
                </div>

                <!-- Phone -->
                <div class="col-span-12 md:col-span-6">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Số điện thoại <span class="text-red-500">*</span>
                    </label>
                    <InputText
                      v-model="form.soDienThoai"
                      placeholder="Nhập số điện thoại"
                      :invalid="(submitted && !form.soDienThoai) || phoneError"
                      @blur="validatePhone"
                    />
                    <small class="text-red-500" v-if="submitted && !form.soDienThoai">
                      Số điện thoại là bắt buộc
                    </small>
                    <small class="text-red-500" v-if="phoneError">
                      {{ phoneError }}
                    </small>
                  </div>
                </div>

                <!-- Role -->
                <div class="col-span-12 md:col-span-6">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Vai trò <span class="text-red-500">*</span>
                    </label>
                    <Select
                      v-model="form.vaiTro"
                      :options="roleOptions"
                      optionLabel="label"
                      optionValue="value"
                      placeholder="Chọn vai trò"
                      :invalid="submitted && !form.vaiTro"
                    />
                    <small class="text-red-500" v-if="submitted && !form.vaiTro">
                      Vai trò là bắt buộc
                    </small>
                  </div>
                </div>

                <!-- Status -->
                <div class="col-span-12 md:col-span-6">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">Trạng thái</label>
                    <Select
                      v-model="form.trangThai"
                      :options="statusOptions"
                      optionLabel="label"
                      optionValue="value"
                      placeholder="Chọn trạng thái"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Address Section -->
        <div class="card">
          <div class="flex items-center gap-2 mb-4">
            <i class="pi pi-map-marker text-primary"></i>
            <span class="font-semibold text-xl">Địa chỉ</span>
          </div>

          <div class="flex flex-col gap-4">
            <div
              v-for="(address, index) in form.diaChis"
              :key="index"
              class="border border-surface-200 rounded-lg p-4"
            >
              <div class="flex justify-between items-center mb-4">
                <div class="flex items-center gap-2">
                  <i class="pi pi-home text-primary"></i>
                  <span class="font-semibold">Địa chỉ {{ index + 1 }}</span>
                </div>
                <Button
                  v-if="form.diaChis.length > 1"
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  @click="removeAddress(index)"
                  :disabled="form.diaChis.length === 1"
                  v-tooltip.top="'Xóa địa chỉ'"
                />
              </div>

              <div class="grid grid-cols-12 gap-4">
                <!-- Street -->
                <div class="col-span-12">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Đường/Số nhà <span class="text-red-500">*</span>
                    </label>
                    <InputText
                      v-model="address.duong"
                      placeholder="Nhập đường/số nhà"
                      :invalid="submitted && !address.duong"
                    />
                    <small class="text-red-500" v-if="submitted && !address.duong">
                      Đường/Số nhà là bắt buộc
                    </small>
                  </div>
                </div>

                <!-- Province -->
                <div class="col-span-12 md:col-span-4">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Tỉnh/Thành phố <span class="text-red-500">*</span>
                    </label>
                    <Select
                      v-model="address.tinhThanh"
                      :options="provinces"
                      optionLabel="name"
                      optionValue="code"
                      placeholder="Chọn tỉnh/thành"
                      :invalid="submitted && !address.tinhThanh"
                      @change="getDistricts(address, index)"
                    />
                    <small class="text-red-500" v-if="submitted && !address.tinhThanh">
                      Tỉnh/Thành phố là bắt buộc
                    </small>
                  </div>
                </div>

                <!-- District -->
                <div class="col-span-12 md:col-span-4">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Quận/Huyện <span class="text-red-500">*</span>
                    </label>
                    <Select
                      v-model="address.quanHuyen"
                      :options="districts[index]"
                      optionLabel="name"
                      optionValue="code"
                      placeholder="Chọn quận/huyện"
                      :invalid="submitted && !address.quanHuyen"
                      @change="getWards(address, index)"
                      :disabled="!address.tinhThanh"
                    />
                    <small class="text-red-500" v-if="submitted && !address.quanHuyen">
                      Quận/Huyện là bắt buộc
                    </small>
                  </div>
                </div>

                <!-- Ward -->
                <div class="col-span-12 md:col-span-4">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">
                      Phường/Xã <span class="text-red-500">*</span>
                    </label>
                    <Select
                      v-model="address.phuongXa"
                      :options="wards[index]"
                      optionLabel="name"
                      optionValue="code"
                      placeholder="Chọn phường/xã"
                      :invalid="submitted && !address.phuongXa"
                      :disabled="!address.quanHuyen"
                    />
                    <small class="text-red-500" v-if="submitted && !address.phuongXa">
                      Phường/Xã là bắt buộc
                    </small>
                  </div>
                </div>

                <!-- Address Type -->
                <div class="col-span-12 md:col-span-8">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">Loại địa chỉ</label>
                    <InputText
                      v-model="address.loaiDiaChi"
                      placeholder="Ví dụ: Nhà riêng, Công ty..."
                    />
                  </div>
                </div>

                <!-- Default Address Checkbox -->
                <div class="col-span-12 md:col-span-4">
                  <div class="flex flex-col gap-2">
                    <label class="font-semibold">Tùy chọn</label>
                    <div class="flex items-center gap-2">
                      <Checkbox
                        v-model="address.laMacDinh"
                        :binary="true"
                        @change="setDefaultAddress(index)"
                        :disabled="form.diaChis.length === 1"
                        inputId="defaultAddress"
                      />
                      <label for="defaultAddress">Địa chỉ mặc định</label>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="flex justify-end mt-4">
              <Button
                label="Thêm địa chỉ"
                icon="pi pi-plus"
                outlined
                @click="addNewAddress"
                :disabled="form.diaChis.length >= 5"
                v-tooltip.top="'Tối đa 5 địa chỉ'"
              />
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
              :key="entry.id || index"
              class="border-l-4 pl-4 py-3 rounded-r-lg"
              :class="getAuditBorderColor(entry.hanhDong)"
            >
              <!-- Header with action and timestamp -->
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-3">
                  <i :class="[getAuditIcon(entry.hanhDong), getAuditIconColor(entry.hanhDong), 'text-lg']"></i>
                  <span class="font-medium text-base">{{ getActionDisplayName(entry.hanhDong) }}</span>
                  <span class="text-sm text-surface-500">{{ formatAuditDate(entry.thoiGianThayDoi) }}</span>
                </div>
              </div>

              <!-- User and Reason Information -->
              <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
                <div class="space-y-2">
                  <div class="text-sm text-surface-700">
                    <strong class="text-surface-900">Người thực hiện:</strong>
                    <span class="font-medium ml-2">{{ entry.nguoiThucHien || 'Hệ thống' }}</span>
                  </div>

                  <div v-if="entry.lyDoThayDoi" class="text-sm text-surface-700">
                    <strong class="text-surface-900">Lý do:</strong>
                    <span class="italic ml-2">{{ entry.lyDoThayDoi }}</span>
                  </div>
                </div>
              </div>

              <!-- Change Details Section -->
              <div v-if="entry.giaTriCu || entry.giaTriMoi" class="bg-surface-50 rounded-lg p-4">
                <strong class="text-surface-900 text-base block mb-3">Chi tiết thay đổi:</strong>

                <!-- Parse and display changes for UPDATE entries (both old and new values) -->
                <div v-if="entry.giaTriCu && entry.giaTriMoi" class="space-y-3">
                  <div v-for="change in parseAuditChanges(entry.giaTriCu, entry.giaTriMoi)" :key="change.field" class="border-b border-surface-200 pb-3 last:border-b-0 last:pb-0">
                    <div class="font-medium text-surface-700 mb-2 text-sm">{{ change.fieldName }}:</div>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <!-- Old Value Column -->
                      <div>
                        <div class="text-red-600 bg-red-50 p-2 rounded text-sm">{{ change.oldValue }}</div>
                      </div>
                      <!-- New Value Column -->
                      <div>
                        <div class="text-green-600 bg-green-50 p-2 rounded text-sm">{{ change.newValue }}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Parse and display values for CREATE entries (only new values) -->
                <div v-else-if="entry.giaTriMoi" class="space-y-3">
                  <div v-for="field in parseCreateAuditValues(entry.giaTriMoi)" :key="field.field" class="border-b border-surface-200 pb-3 last:border-b-0 last:pb-0">
                    <div class="font-medium text-surface-700 mb-2 text-sm">{{ field.fieldName }}:</div>
                    <div class="text-green-600 bg-green-50 p-2 rounded text-sm">{{ field.value }}</div>
                  </div>
                </div>

                <!-- Fallback for raw JSON display (only for old values without new values) -->
                <div v-else-if="entry.giaTriCu" class="space-y-3">
                  <div class="text-sm">
                    <span class="font-medium text-surface-700">Giá trị cũ:</span>
                    <div class="text-red-600 mt-1 font-mono bg-red-50 p-2 rounded">{{ formatJsonData(entry.giaTriCu) }}</div>
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

        <!-- Form Actions -->
        <div class="flex justify-end gap-3 pt-6 border-t border-surface-200">
          <Button
            label="Hủy bỏ"
            icon="pi pi-times"
            severity="secondary"
            outlined
            @click="goBack"
          />
          <Button
            type="submit"
            :label="isEditMode ? 'Cập nhật' : 'Tạo mới'"
            icon="pi pi-check"
          />
        </div>
      </div>
    </form>

    <!-- QR Scanner Dialog -->
    <Dialog
      v-model:visible="showQRScanner"
      modal
      header="Quét QR CCCD"
      :style="{ width: '500px' }"
      @hide="stopScanner"
      :closable="true"
    >
      <div class="text-center">
        <p class="mb-4">Vui lòng quét mã QR từ CCCD của nhân viên</p>
        <div class="border-2 border-primary border-dashed rounded-lg p-4 mb-4">
          <!-- QR Scanner component -->
          <div class="flex flex-col items-center justify-center">
            <div v-if="!qrScanResult" class="w-full">
              <div v-if="!cameraError">
                <qrcode-stream
                  @detect="onDetect"
                  @init="onInit"
                  :track="paintBoundingBox"
                  class="w-full h-64 rounded-lg overflow-hidden"
                />
                <p class="text-surface-600 mt-2">Đưa mã QR vào khung hình để quét</p>
              </div>
              <div v-else class="p-4 bg-red-50 rounded-lg">
                <i class="pi pi-exclamation-triangle text-red-500 text-xl"></i>
                <p class="text-red-700 font-medium mt-2">Lỗi khi truy cập camera</p>
                <p class="mt-2">{{ cameraError }}</p>
                <Button
                  label="Cấp quyền camera"
                  icon="pi pi-camera"
                  severity="info"
                  @click="requestCameraPermission"
                  class="mt-3"
                />
              </div>
            </div>
            <div v-else class="p-4 bg-green-50 rounded-lg w-full">
              <p class="text-green-700 font-medium">Quét thành công!</p>
              <p class="mt-2">{{ qrScanResult }}</p>
            </div>
          </div>
        </div>
        <div class="flex justify-center gap-2">
          <Button
            label="Hủy"
            icon="pi pi-times"
            severity="secondary"
            @click="showQRScanner = false"
          />
          <Button
            label="Xác nhận"
            icon="pi pi-check"
            severity="success"
            @click="applyQRScanResult"
            :disabled="!qrScanResult"
          />
        </div>
      </div>
    </Dialog>
  </Fluid>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useStaffStore } from '@/stores/staffstore'
import addressApi from '@/apis/address'
import userApi from '@/apis/user'
import storageApi from '@/apis/storage'
// Import thư viện QR scanner
import { QrcodeStream } from 'vue-qrcode-reader'

const toast = useToast()
const router = useRouter()
const route = useRoute()
const staffStore = useStaffStore()

const isEditMode = computed(() => route.name === 'StaffEdit')
const staffId = computed(() => route.params.id)

// QR Scanner
const showQRScanner = ref(false)
const qrScanResult = ref(null)
const cameraError = ref(null)
const hasCamera = ref(true)

// Audit trail
const auditTrail = ref([])

// Form data
const form = ref({
  avatar: null,
  hoTen: '',
  gioiTinh: null,
  ngaySinh: null,
  cccd: '',
  email: '',
  soDienThoai: '',
  vaiTro: null,
  trangThai: 'HOAT_DONG',
  diaChis: [], // Khởi tạo mảng rỗng
})

// Khởi tạo địa chỉ mặc định nếu không có
const initDefaultAddress = () => {
  if (form.value.diaChis.length === 0) {
    form.value.diaChis.push({
      duong: '',
      phuongXa: '',
      quanHuyen: '',
      tinhThanh: '',
      loaiDiaChi: '',
      laMacDinh: true,
    })
  }
}

const imagePreview = ref(null)
const submitted = ref(false)
const emailError = ref(null)
const phoneError = ref(null)
const cccdError = ref(null)

// Options
const genderOptions = ref([
  { label: 'Nam', value: 'NAM' },
  { label: 'Nữ', value: 'NU' },
])

const roleOptions = ref([
  { label: 'Nhân viên', value: 'STAFF' },
  { label: 'Admin', value: 'ADMIN' },
])

const statusOptions = ref([
  { label: 'Hoạt động', value: 'HOAT_DONG' },
  { label: 'Không hoạt động', value: 'KHONG_HOAT_DONG' },
])

// Address data
const provinces = ref([])
const districts = ref([])
const wards = ref([])

const maxBirthDate = computed(() => {
  const date = new Date()
  date.setFullYear(date.getFullYear() - 10)
  return date
})

// Hàm xử lý khi phát hiện QR code
const onDetect = (detectedCodes) => {
  if (detectedCodes && detectedCodes.length > 0) {
    // Lấy kết quả đầu tiên
    const result = detectedCodes[0].rawValue

    // Tối ưu hóa việc trích xuất số CCCD
    let cccdNumber = result

    // Xử lý các định dạng QR code khác nhau
    if (result.includes('|')) {
      // Định dạng: CCCD|123456789012|...
      const parts = result.split('|')
      cccdNumber = parts.find((part) => /^\d{9,12}$/.test(part)) || cccdNumber
    } else if (result.includes(':')) {
      // Định dạng khác: CCCD:123456789012
      const parts = result.split(':')
      cccdNumber = parts.find((part) => /^\d{9,12}$/.test(part)) || cccdNumber
    }

    // Chỉ lấy số
    cccdNumber = cccdNumber.replace(/\D/g, '')

    // Giới hạn độ dài
    if (cccdNumber.length > 12) {
      cccdNumber = cccdNumber.substring(0, 12)
    }

    // Chỉ chấp nhận nếu đủ 9-12 số
    if (cccdNumber.length >= 9 && cccdNumber.length <= 12) {
      qrScanResult.value = cccdNumber
    }
  }
}

// Hàm khởi tạo QR scanner
// Replace the current onInit function with this enhanced version
const onInit = async (promise) => {
  try {
    await promise
    cameraError.value = null
    hasCamera.value = true

    // Add notification when camera is successfully initialized
    toast.add({
      severity: 'info',
      summary: 'Camera',
      detail: 'Camera đã được kích hoạt thành công',
      life: 2000,
    })
  } catch (error) {
    console.error('Camera error:', error)
    // More detailed error handling based on specific error types
    if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
      cameraError.value =
        'Quyền truy cập camera bị từ chối. Vui lòng cho phép truy cập camera trong cài đặt trình duyệt.'
    } else if (error.name === 'NotFoundError' || error.name === 'OverconstrainedError') {
      cameraError.value =
        'Không tìm thấy camera. Vui lòng đảm bảo camera đã được kết nối đúng cách.'
    } else if (error.name === 'NotReadableError' || error.name === 'AbortError') {
      cameraError.value =
        'Camera đang được sử dụng bởi ứng dụng khác. Vui lòng đóng các ứng dụng đang sử dụng camera và thử lại.'
    } else {
      cameraError.value = `Lỗi camera: ${error.message || 'Không xác định'}`
    }
    hasCamera.value = false

    // Notify user about camera access issue
    toast.add({
      severity: 'error',
      summary: 'Lỗi Camera',
      detail: cameraError.value,
      life: 5000,
    })
  }
}

const requestCameraPermission = async () => {
  try {
    // Explicitly request camera permission
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    // Stop the stream immediately as we just want to trigger the permission dialog
    stream.getTracks().forEach((track) => track.stop())

    // Reopen QR scanner which should now have permission
    showQRScanner.value = false
    setTimeout(() => {
      showQRScanner.value = true
      qrScanResult.value = null
      cameraError.value = null
    }, 500)

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã được cấp quyền truy cập camera',
      life: 3000,
    })
  } catch (error) {
    console.error('Permission request error:', error)
    cameraError.value =
      'Không thể truy cập camera. Vui lòng kiểm tra cài đặt quyền của trình duyệt.'
    toast.add({
      severity: 'error',
      summary: 'Lỗi Camera',
      detail: cameraError.value,
      life: 5000,
    })
  }
}

// Dừng QR scanner khi dialog đóng
const stopScanner = () => {
  qrScanResult.value = null
  cameraError.value = null
}

// QR Scan Functions
const applyQRScanResult = () => {
  if (qrScanResult.value) {
    form.value.cccd = qrScanResult.value
    showQRScanner.value = false
    validateCCCD()
  }
}

// Theo dõi sự thay đổi của dialog QR
watch(showQRScanner, (newVal) => {
  if (!newVal) {
    stopScanner()
  }
})

// Audit trail functions
const loadAuditHistory = async () => {
  if (!isEditMode.value || !staffId.value) return

  try {
    const response = await userApi.getStaffAuditHistory(staffId.value)
    auditTrail.value = response.data || []
  } catch (error) {
    console.error('Error loading audit history:', error)
    // Don't show error toast for audit history as it's not critical
    auditTrail.value = []
  }
}

const formatAuditDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const getActionDisplayName = (action) => {
  const actionMap = {
    'CREATE': 'Tạo mới',
    'UPDATE': 'Cập nhật',
    'DELETE': 'Xóa',
    'STATUS_CHANGE': 'Thay đổi trạng thái',
    'ROLE_CHANGE': 'Thay đổi vai trò'
  }
  return actionMap[action] || action
}

const getAuditIcon = (action) => {
  const iconMap = {
    'CREATE': 'pi pi-plus-circle',
    'UPDATE': 'pi pi-pencil',
    'DELETE': 'pi pi-trash',
    'STATUS_CHANGE': 'pi pi-sync',
    'ROLE_CHANGE': 'pi pi-user-edit'
  }
  return iconMap[action] || 'pi pi-info-circle'
}

const getAuditIconColor = (action) => {
  const colorMap = {
    'CREATE': 'text-green-600',
    'UPDATE': 'text-blue-600',
    'DELETE': 'text-red-600',
    'STATUS_CHANGE': 'text-purple-600',
    'ROLE_CHANGE': 'text-orange-600'
  }
  return colorMap[action] || 'text-surface-600'
}

const getAuditBorderColor = (action) => {
  const colorMap = {
    'CREATE': 'border-green-400 bg-green-50',
    'UPDATE': 'border-blue-400 bg-blue-50',
    'DELETE': 'border-red-400 bg-red-50',
    'STATUS_CHANGE': 'border-purple-400 bg-purple-50',
    'ROLE_CHANGE': 'border-orange-400 bg-orange-50'
  }
  return colorMap[action] || 'border-surface-400 bg-surface-50'
}

const formatJsonData = (jsonString) => {
  if (!jsonString) return ''
  try {
    const data = JSON.parse(jsonString)
    return Object.entries(data)
      .map(([key, value]) => `${key}: ${value}`)
      .join(', ')
  } catch (error) {
    return jsonString
  }
}

// Parse audit changes to compare old and new values
const parseAuditChanges = (oldValuesJson, newValuesJson) => {
  if (!oldValuesJson || !newValuesJson) return []

  try {
    const oldValues = JSON.parse(oldValuesJson)
    const newValues = JSON.parse(newValuesJson)
    const changes = []

    // Field name mappings for user-friendly display
    const fieldNames = {
      maNguoiDung: 'Mã người dùng',
      hoTen: 'Họ và tên',
      gioiTinh: 'Giới tính',
      ngaySinh: 'Ngày sinh',
      email: 'Email',
      soDienThoai: 'Số điện thoại',
      cccd: 'CCCD',
      avatar: 'Ảnh đại diện',
      vaiTro: 'Vai trò',
      trangThai: 'Trạng thái'
    }

    // Compare each field
    for (const [field, newValue] of Object.entries(newValues)) {
      const oldValue = oldValues[field]
      if (oldValue !== newValue) {
        changes.push({
          field: field,
          fieldName: fieldNames[field] || field,
          oldValue: formatAuditValue(field, oldValue),
          newValue: formatAuditValue(field, newValue)
        })
      }
    }

    return changes
  } catch (error) {
    console.error('Error parsing audit changes:', error)
    return []
  }
}

// Format audit values for display
const formatAuditValue = (field, value) => {
  if (value === null || value === undefined) return 'Không có'

  switch (field) {
    case 'gioiTinh':
      return value === 'NAM' ? 'Nam' : value === 'NU' ? 'Nữ' : value
    case 'trangThai':
      return value === 'HOAT_DONG' ? 'Hoạt động' : value === 'KHONG_HOAT_DONG' ? 'Không hoạt động' : value
    case 'vaiTro':
      return value === 'CUSTOMER' ? 'Khách hàng' : value === 'STAFF' ? 'Nhân viên' : value === 'ADMIN' ? 'Quản trị viên' : value
    case 'ngaySinh':
      return value ? new Date(value).toLocaleDateString('vi-VN') : 'Không có'
    case 'avatar':
      return value ? 'Có ảnh đại diện' : 'Không có ảnh'
    default:
      return String(value)
  }
}

// Parse audit values for CREATE entries (only new values)
const parseCreateAuditValues = (newValuesJson) => {
  if (!newValuesJson) return []

  try {
    const newValues = JSON.parse(newValuesJson)
    const fields = []

    // Field name mappings for user-friendly display
    const fieldNames = {
      maNguoiDung: 'Mã người dùng',
      hoTen: 'Họ và tên',
      gioiTinh: 'Giới tính',
      ngaySinh: 'Ngày sinh',
      email: 'Email',
      soDienThoai: 'Số điện thoại',
      cccd: 'CCCD',
      avatar: 'Ảnh đại diện',
      vaiTro: 'Vai trò',
      trangThai: 'Trạng thái'
    }

    // Process each field
    for (const [field, value] of Object.entries(newValues)) {
      if (value !== null && value !== undefined) {
        fields.push({
          field: field,
          fieldName: fieldNames[field] || field,
          value: formatAuditValue(field, value)
        })
      }
    }

    return fields
  } catch (error) {
    console.error('Error parsing create audit values:', error)
    return []
  }
}

onMounted(async () => {
  await loadProvinces()
  initDefaultAddress() // Đảm bảo luôn có ít nhất 1 địa chỉ

  if (isEditMode.value) {
    // Giữ nguyên logic load data cho edit mode
    try {
      const staff = await staffStore.fetchStaffById(staffId.value)

      // Kiểm tra và xử lý dữ liệu nhân viên trước khi gán vào form
      const staffData = staff || {}

      // Khởi tạo districts và wards
      districts.value = []
      wards.value = []

      // Cập nhật form data với kiểm tra từng trường
      form.value = {
        avatar: staffData.avatar || null,
        hoTen: staffData.hoTen || '',
        gioiTinh: staffData.gioiTinh || null,
        ngaySinh: staffData.ngaySinh ? new Date(staffData.ngaySinh) : null,
        cccd: staffData.cccd || '',
        email: staffData.email || '',
        soDienThoai: staffData.soDienThoai || '',
        vaiTro: staffData.vaiTro || null,
        trangThai: staffData.trangThai !== undefined ? staffData.trangThai : true,
        diaChis:
          staffData.diaChis?.length > 0
            ? staffData.diaChis.map((addr) => ({
                ...addr,
                tinhThanhName: addr.tinhThanh,
                quanHuyenName: addr.quanHuyen,
                phuongXaName: addr.phuongXa,
                tinhThanh: '',
                quanHuyen: '',
                phuongXa: '',
              }))
            : [],
      }

      // Đảm bảo có ít nhất 1 địa chỉ
      if (form.value.diaChis.length === 0) {
        form.value.diaChis.push({
          duong: '',
          phuongXa: '',
          quanHuyen: '',
          tinhThanh: '',
          loaiDiaChi: '',
          laMacDinh: true,
        })
      }

      // Khởi tạo districts và wards với đúng số lượng địa chỉ
      districts.value = new Array(form.value.diaChis.length).fill().map(() => [])
      wards.value = new Array(form.value.diaChis.length).fill().map(() => [])

      // Load ảnh đại diện nếu có
      if (staffData.avatar) {
        imagePreview.value = staffData.avatar
      }

      // Load địa chỉ
      for (let i = 0; i < form.value.diaChis.length; i++) {
        const address = form.value.diaChis[i]
        const provinceName = address.tinhThanhName
        const districtName = address.quanHuyenName
        const wardName = address.phuongXaName

        const province = provinces.value.find((p) => p.name === provinceName)
        if (province) {
          address.tinhThanh = province.code
          await getDistricts(address, i)

          if (districts.value[i]?.length > 0) {
            const district = districts.value[i].find((d) => d.name === districtName)
            if (district) {
              address.quanHuyen = district.code
              await getWards(address, i)

              if (wards.value[i]?.length > 0) {
                const ward = wards.value[i].find((w) => w.name === wardName)
                if (ward) {
                  address.phuongXa = ward.code
                }
              }
            }
          }
        }

        // Dọn dẹp các trường tạm
        delete address.tinhThanhName
        delete address.quanHuyenName
        delete address.phuongXaName
      }

      // Load audit history after loading staff data
      await loadAuditHistory()
    } catch (error) {
      console.error('Error loading staff:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không thể tải thông tin nhân viên',
        life: 3000,
      })
    }
  }
})

// Load provinces
const loadProvinces = async () => {
  try {
    const response = await addressApi.getProvinces()
    provinces.value = response.data
  } catch (error) {
    console.error('Error loading provinces:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải danh sách tỉnh/thành',
      life: 3000,
    })
  }
}

// Get districts for a province
const getDistricts = async (address, index) => {
  try {
    const response = await addressApi.getDistricts(address.tinhThanh)
    districts.value[index] = response.data.districts || []
    address.quanHuyen = ''
    address.phuongXa = ''
    wards.value[index] = []
  } catch (error) {
    console.error('Error loading districts:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải danh sách quận/huyện',
      life: 3000,
    })
  }
}

// Get wards for a district
const getWards = async (address, index) => {
  try {
    const response = await addressApi.getWards(address.quanHuyen)
    wards.value[index] = response.data.wards || []
    address.phuongXa = ''
  } catch (error) {
    console.error('Error loading wards:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải danh sách phường/xã',
      life: 3000,
    })
  }
}

// Handle avatar selection
const onAvatarSelect = (event) => {
  const file = event.files[0]
  if (file) {
    form.value.avatar = file
    const reader = new FileReader()
    reader.onload = (e) => {
      imagePreview.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

// Validate email
const validateEmail = () => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!form.value.email) {
    emailError.value = null
    return false
  }
  if (!emailRegex.test(form.value.email)) {
    emailError.value = 'Email không hợp lệ'
    return false
  }
  emailError.value = null
  return true
}

// Validate phone
const validatePhone = () => {
  const phoneRegex = /^[0-9]{10,11}$/
  if (!form.value.soDienThoai) {
    phoneError.value = null
    return false
  }
  if (!phoneRegex.test(form.value.soDienThoai)) {
    phoneError.value = 'Số điện thoại phải có 10-11 chữ số'
    return false
  }
  phoneError.value = null
  return true
}

// Validate CCCD
const validateCCCD = () => {
  const cccdRegex = /^[0-9]{9,12}$/
  if (!form.value.cccd) {
    cccdError.value = null
    return false
  }
  if (!cccdRegex.test(form.value.cccd)) {
    cccdError.value = 'CCCD/CMND phải có 9-12 chữ số'
    return false
  }
  cccdError.value = null
  return true
}

const paintBoundingBox = (detectedCodes, ctx) => {
  for (const detectedCode of detectedCodes) {
    const [firstPoint, ...otherPoints] = detectedCode.cornerPoints;

    ctx.strokeStyle = "#4ade80";
    ctx.lineWidth = 2;
    ctx.fillStyle = "rgba(74, 222, 128, 0.1)";

    ctx.beginPath();
    ctx.moveTo(firstPoint.x, firstPoint.y);
    for (const { x, y } of otherPoints) {
      ctx.lineTo(x, y);
    }
    ctx.lineTo(firstPoint.x, firstPoint.y);
    ctx.closePath();
    ctx.stroke();
    ctx.fill();
  }
};

// Add new address
const addNewAddress = () => {
  form.value.diaChis.push({
    duong: '',
    phuongXa: '',
    quanHuyen: '',
    tinhThanh: '',
    loaiDiaChi: '',
    laMacDinh: false,
  })
  districts.value.push([])
  wards.value.push([])
}

// Remove address
const removeAddress = (index) => {
  const wasDefault = form.value.diaChis[index].laMacDinh

  form.value.diaChis.splice(index, 1)
  districts.value.splice(index, 1)
  wards.value.splice(index, 1)

  if (wasDefault && form.value.diaChis.length > 0) {
    form.value.diaChis[0].laMacDinh = true
  }
}

// Set default address
const setDefaultAddress = (index) => {
  if (form.value.diaChis[index].laMacDinh) {
    form.value.diaChis.forEach((addr, i) => {
      if (i !== index) {
        addr.laMacDinh = false
      }
    })
  } else if (form.value.diaChis.filter((addr) => addr.laMacDinh).length === 0) {
    form.value.diaChis[0].laMacDinh = true
  }
}

// Handle form submission
const handleSubmit = async () => {
  submitted.value = true

  // Validate form
  const isEmailValid = validateEmail()
  const isPhoneValid = validatePhone()
  const isCCCDValid = validateCCCD()

  // Check required fields
  const isFormValid =
    form.value.hoTen &&
    form.value.gioiTinh !== null &&
    form.value.ngaySinh &&
    form.value.cccd &&
    form.value.email &&
    form.value.soDienThoai &&
    form.value.vaiTro !== null &&
    isEmailValid &&
    isPhoneValid &&
    isCCCDValid &&
    form.value.diaChis.every(
      (addr) => addr.duong && addr.tinhThanh && addr.quanHuyen && addr.phuongXa,
    )

  if (!isFormValid) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng điền đầy đủ thông tin bắt buộc',
      life: 3000,
    })
    return
  }

  try {
    let avatarUrl = null

    // Upload avatar file if it's a File object (new upload)
    if (form.value.avatar && form.value.avatar instanceof File) {
      try {
        avatarUrl = await storageApi.uploadFile(form.value.avatar, 'avatars')
        if (!avatarUrl) {
          throw new Error('No file URL returned from upload')
        }
      } catch (uploadError) {
        console.error('Avatar upload error:', uploadError)
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: uploadError.message || 'Không thể tải lên ảnh đại diện',
          life: 3000,
        })
        return
      }
    } else if (typeof form.value.avatar === 'string') {
      // If avatar is already a string (URL), use it as is (for edit mode)
      avatarUrl = form.value.avatar
    }

    // Prepare data for API
    const staffData = {
      ...form.value,
      avatar: avatarUrl, // Use the uploaded URL or existing URL
      ngaySinh: form.value.ngaySinh.toISOString().split('T')[0],
      diaChis: form.value.diaChis.map((addr, index) => {
        // Tìm tên từ code hoặc giữ nguyên nếu không tìm thấy (trường hợp đã là tên)
        const province = provinces.value.find((p) => p.code === addr.tinhThanh)
        const district = districts.value[index]?.find((d) => d.code === addr.quanHuyen)
        const ward = wards.value[index]?.find((w) => w.code === addr.phuongXa)

        return {
          ...addr,
          tinhThanh: province ? province.name : addr.tinhThanh,
          quanHuyen: district ? district.name : addr.quanHuyen,
          phuongXa: ward ? ward.name : addr.phuongXa,
        }
      }),
    }

    if (isEditMode.value) {
      await staffStore.updateStaff(staffId.value, staffData)
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Cập nhật nhân viên thành công',
        life: 3000,
      })

      // Reload audit history to show the new update entry
      await loadAuditHistory()
    } else {
      await staffStore.createStaff(staffData)
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Tạo nhân viên mới thành công',
        life: 3000,
      })

      // For new staff, redirect to list
      router.push({ name: 'employees' })
    }
  } catch (error) {
    const errorMessage = error.message || 'Có lỗi xảy ra khi xử lý yêu cầu'
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: errorMessage,
      life: 5000,
    })
  }
}

// Navigate back
const goBack = () => {
  router.push({ name: 'employees' })
}
</script>

<style>

</style>
