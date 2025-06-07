<template>
  <Dialog
    v-model:visible="visible"
    modal
    header="Thêm khách hàng nhanh"
    :style="{ width: '450px' }"
    :closable="true"
    @hide="resetForm"
  >
    <form @submit.prevent="createCustomer" class="space-y-4">
      <!-- Full Name -->
      <div>
        <label for="hoTen" class="block text-sm font-medium mb-1">
          Họ và tên <span class="text-red-500">*</span>
        </label>
        <InputText
          id="hoTen"
          v-model="customerData.hoTen"
          placeholder="Nhập họ và tên khách hàng"
          class="w-full"
          :class="{ 'p-invalid': errors.hoTen }"
          required
        />
        <small v-if="errors.hoTen" class="p-error">{{ errors.hoTen }}</small>
      </div>

      <!-- Phone Number -->
      <div>
        <label for="soDienThoai" class="block text-sm font-medium mb-1">
          Số điện thoại <span class="text-red-500">*</span>
        </label>
        <InputText
          id="soDienThoai"
          v-model="customerData.soDienThoai"
          placeholder="Nhập số điện thoại"
          class="w-full"
          :class="{ 'p-invalid': errors.soDienThoai }"
          required
        />
        <small v-if="errors.soDienThoai" class="p-error">{{ errors.soDienThoai }}</small>
      </div>

      <!-- Email -->
      <div>
        <label for="email" class="block text-sm font-medium mb-1">
          Email <span class="text-surface-500">(tùy chọn)</span>
        </label>
        <InputText
          id="email"
          v-model="customerData.email"
          type="email"
          placeholder="Nhập địa chỉ email (không bắt buộc)"
          class="w-full"
          :class="{ 'p-invalid': errors.email }"
        />
        <small v-if="errors.email" class="p-error">{{ errors.email }}</small>
      </div>

      <!-- Gender -->
      <div>
        <label class="block text-sm font-medium mb-1">Giới tính</label>
        <div class="flex gap-4">
          <div class="flex items-center">
            <RadioButton
              id="male"
              v-model="customerData.gioiTinh"
              name="gioiTinh"
              value="NAM"
            />
            <label for="male" class="ml-2 text-sm">Nam</label>
          </div>
          <div class="flex items-center">
            <RadioButton
              id="female"
              v-model="customerData.gioiTinh"
              name="gioiTinh"
              value="NU"
            />
            <label for="female" class="ml-2 text-sm">Nữ</label>
          </div>
          <div class="flex items-center">
            <RadioButton
              id="other"
              v-model="customerData.gioiTinh"
              name="gioiTinh"
              value="KHAC"
            />
            <label for="other" class="ml-2 text-sm">Khác</label>
          </div>
        </div>
      </div>

      <!-- Date of Birth -->
      <div>
        <label for="ngaySinh" class="block text-sm font-medium mb-1">Ngày sinh</label>
        <Calendar
          id="ngaySinh"
          v-model="customerData.ngaySinh"
          placeholder="Chọn ngày sinh"
          class="w-full"
          dateFormat="dd/mm/yy"
          :maxDate="new Date()"
          showIcon
        />
      </div>
    </form>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button
          label="Hủy"
          severity="secondary"
          outlined
          @click="visible = false"
          :disabled="creating"
        />
        <Button
          label="Tạo khách hàng"
          @click="createCustomer"
          :loading="creating"
          :disabled="!isFormValid"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useCustomerStore } from '@/stores/customerstore'

// PrimeVue Components
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import RadioButton from 'primevue/radiobutton'
import Calendar from 'primevue/calendar'

// Props and Emits
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'customer-created'])

// Store and composables
const toast = useToast()
const customerStore = useCustomerStore()

// Local state
const creating = ref(false)
const customerData = ref({
  hoTen: '',
  soDienThoai: '',
  email: '',
  gioiTinh: 'NAM',
  ngaySinh: null
})

const errors = ref({})

// Computed
const visible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const isFormValid = computed(() => {
  return customerData.value.hoTen.trim() &&
         customerData.value.soDienThoai.trim() &&
         Object.keys(errors.value).length === 0
})

// Methods
const validateForm = () => {
  errors.value = {}

  // Validate full name
  if (!customerData.value.hoTen.trim()) {
    errors.value.hoTen = 'Họ và tên là bắt buộc'
  } else if (customerData.value.hoTen.trim().length < 2) {
    errors.value.hoTen = 'Họ và tên phải có ít nhất 2 ký tự'
  }

  // Validate phone number
  if (!customerData.value.soDienThoai.trim()) {
    errors.value.soDienThoai = 'Số điện thoại là bắt buộc'
  } else if (!/^[0-9]{10,11}$/.test(customerData.value.soDienThoai.trim())) {
    errors.value.soDienThoai = 'Số điện thoại không hợp lệ (10-11 chữ số)'
  }

  // Validate email (optional)
  if (customerData.value.email && customerData.value.email.trim()) {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(customerData.value.email.trim())) {
      errors.value.email = 'Email không hợp lệ'
    }
  }

  return Object.keys(errors.value).length === 0
}

const createCustomer = async () => {
  if (!validateForm()) {
    return
  }

  creating.value = true
  try {
    // Prepare customer data for API
    const customerPayload = {
      hoTen: customerData.value.hoTen.trim(),
      soDienThoai: customerData.value.soDienThoai.trim(),
      email: customerData.value.email && customerData.value.email.trim() ? customerData.value.email.trim() : null,
      gioiTinh: customerData.value.gioiTinh,
      ngaySinh: customerData.value.ngaySinh ? customerData.value.ngaySinh.toISOString().split('T')[0] : null,
      trangThai: 'HOAT_DONG' // Set default active status as required by backend
    }

    console.log('Creating customer with data:', customerPayload)

    // Create customer using store
    const newCustomer = await customerStore.createCustomer(customerPayload)

    if (newCustomer) {
      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `Đã tạo khách hàng ${newCustomer.hoTen} thành công`,
        life: 3000
      })

      // Emit the created customer
      emit('customer-created', newCustomer)

      // Close dialog and reset form
      visible.value = false
      resetForm()
    }
  } catch (error) {
    console.error('Error creating customer:', error)

    // Handle specific validation errors from backend
    if (error.response && error.response.data && error.response.data.errors) {
      const apiErrors = error.response.data.errors
      Object.keys(apiErrors).forEach(field => {
        if (errors.value[field] !== undefined) {
          errors.value[field] = apiErrors[field][0] || apiErrors[field]
        }
      })
    } else {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không thể tạo khách hàng. Vui lòng thử lại.',
        life: 5000
      })
    }
  } finally {
    creating.value = false
  }
}

const resetForm = () => {
  customerData.value = {
    hoTen: '',
    soDienThoai: '',
    email: '',
    gioiTinh: 'NAM',
    ngaySinh: null
  }
  errors.value = {}
}

// Watch for real-time validation
watch(() => customerData.value.hoTen, () => {
  if (errors.value.hoTen) {
    validateForm()
  }
})

watch(() => customerData.value.soDienThoai, () => {
  if (errors.value.soDienThoai) {
    validateForm()
  }
})

watch(() => customerData.value.email, () => {
  if (errors.value.email) {
    validateForm()
  }
})
</script>
