<template>
  <Dialog
    v-model:visible="visible"
    modal
    header="Thêm địa chỉ giao hàng nhanh"
    :style="{ width: '500px' }"
    :closable="true"
    @hide="resetForm"
  >
    <form @submit.prevent="createAddress" class="space-y-4">
      <!-- Street Address -->
      <div>
        <label for="duong" class="block text-sm font-medium mb-1">
          Địa chỉ đường <span class="text-red-500">*</span>
        </label>
        <InputText
          id="duong"
          v-model="addressData.duong"
          placeholder="Nhập số nhà, tên đường..."
          class="w-full"
          :class="{ 'p-invalid': errors.duong }"
          required
        />
        <small v-if="errors.duong" class="p-error">{{ errors.duong }}</small>
      </div>

      <!-- Province/City -->
      <div>
        <label for="tinhThanh" class="block text-sm font-medium mb-1">
          Tỉnh/Thành phố <span class="text-red-500">*</span>
        </label>
        <Dropdown
          id="tinhThanh"
          v-model="selectedProvince"
          :options="provinces"
          optionLabel="name"
          placeholder="Chọn tỉnh/thành phố"
          class="w-full"
          :class="{ 'p-invalid': errors.tinhThanh }"
          @change="onProvinceChange"
          :loading="loadingProvinces"
        />
        <small v-if="errors.tinhThanh" class="p-error">{{ errors.tinhThanh }}</small>
      </div>

      <!-- District -->
      <div>
        <label for="quanHuyen" class="block text-sm font-medium mb-1">
          Quận/Huyện <span class="text-red-500">*</span>
        </label>
        <Dropdown
          id="quanHuyen"
          v-model="selectedDistrict"
          :options="districts"
          optionLabel="name"
          placeholder="Chọn quận/huyện"
          class="w-full"
          :class="{ 'p-invalid': errors.quanHuyen }"
          @change="onDistrictChange"
          :disabled="!selectedProvince"
          :loading="loadingDistricts"
        />
        <small v-if="errors.quanHuyen" class="p-error">{{ errors.quanHuyen }}</small>
      </div>

      <!-- Ward -->
      <div>
        <label for="phuongXa" class="block text-sm font-medium mb-1">
          Phường/Xã <span class="text-red-500">*</span>
        </label>
        <Dropdown
          id="phuongXa"
          v-model="selectedWard"
          :options="wards"
          optionLabel="name"
          placeholder="Chọn phường/xã"
          class="w-full"
          :class="{ 'p-invalid': errors.phuongXa }"
          :disabled="!selectedDistrict"
          :loading="loadingWards"
        />
        <small v-if="errors.phuongXa" class="p-error">{{ errors.phuongXa }}</small>
      </div>

      <!-- Address Type -->
      <div>
        <label for="loaiDiaChi" class="block text-sm font-medium mb-1">
          Loại địa chỉ
        </label>
        <Dropdown
          id="loaiDiaChi"
          v-model="addressData.loaiDiaChi"
          :options="addressTypes"
          placeholder="Chọn loại địa chỉ"
          class="w-full"
        />
      </div>

      <!-- Default Address -->
      <div class="flex items-center gap-2">
        <Checkbox
          id="laMacDinh"
          v-model="addressData.laMacDinh"
          :binary="true"
        />
        <label for="laMacDinh" class="text-sm">Đặt làm địa chỉ mặc định</label>
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
          label="Thêm địa chỉ"
          @click="createAddress"
          :loading="creating"
          :disabled="!isFormValid"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import addressApi from '@/apis/address'

// PrimeVue Components
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'
import Checkbox from 'primevue/checkbox'
import Button from 'primevue/button'

// Props and Emits
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  customer: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'address-created'])

// Store and composables
const toast = useToast()

// Local state
const creating = ref(false)
const addressData = ref({
  duong: '',
  phuongXa: '',
  quanHuyen: '',
  tinhThanh: '',
  loaiDiaChi: 'Nhà riêng',
  laMacDinh: false
})

const errors = ref({})

// Address API data
const provinces = ref([])
const districts = ref([])
const wards = ref([])
const selectedProvince = ref(null)
const selectedDistrict = ref(null)
const selectedWard = ref(null)

const loadingProvinces = ref(false)
const loadingDistricts = ref(false)
const loadingWards = ref(false)

const addressTypes = [
  'Nhà riêng',
  'Văn phòng',
  'Khác'
]

// Computed
const visible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const isFormValid = computed(() => {
  return addressData.value.duong.trim() &&
         selectedProvince.value &&
         selectedDistrict.value &&
         selectedWard.value &&
         Object.keys(errors.value).length === 0
})

// Methods
const loadProvinces = async () => {
  try {
    loadingProvinces.value = true
    const response = await addressApi.getProvinces()
    provinces.value = response.data
  } catch (error) {
    console.error('Error loading provinces:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải danh sách tỉnh/thành phố',
      life: 3000
    })
  } finally {
    loadingProvinces.value = false
  }
}

const onProvinceChange = async () => {
  if (!selectedProvince.value) return
  
  // Reset dependent selections
  selectedDistrict.value = null
  selectedWard.value = null
  districts.value = []
  wards.value = []
  
  try {
    loadingDistricts.value = true
    const response = await addressApi.getDistricts(selectedProvince.value.code)
    districts.value = response.data.districts
    addressData.value.tinhThanh = selectedProvince.value.name
  } catch (error) {
    console.error('Error loading districts:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải danh sách quận/huyện',
      life: 3000
    })
  } finally {
    loadingDistricts.value = false
  }
}

const onDistrictChange = async () => {
  if (!selectedDistrict.value) return
  
  // Reset dependent selections
  selectedWard.value = null
  wards.value = []
  
  try {
    loadingWards.value = true
    const response = await addressApi.getWards(selectedDistrict.value.code)
    wards.value = response.data.wards
    addressData.value.quanHuyen = selectedDistrict.value.name
  } catch (error) {
    console.error('Error loading wards:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tải danh sách phường/xã',
      life: 3000
    })
  } finally {
    loadingWards.value = false
  }
}

const validateForm = () => {
  errors.value = {}

  // Validate street address
  if (!addressData.value.duong.trim()) {
    errors.value.duong = 'Địa chỉ đường là bắt buộc'
  }

  // Validate province
  if (!selectedProvince.value) {
    errors.value.tinhThanh = 'Vui lòng chọn tỉnh/thành phố'
  }

  // Validate district
  if (!selectedDistrict.value) {
    errors.value.quanHuyen = 'Vui lòng chọn quận/huyện'
  }

  // Validate ward
  if (!selectedWard.value) {
    errors.value.phuongXa = 'Vui lòng chọn phường/xã'
  }

  return Object.keys(errors.value).length === 0
}

const createAddress = async () => {
  if (!validateForm()) {
    return
  }

  if (!props.customer) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không tìm thấy thông tin khách hàng',
      life: 3000
    })
    return
  }

  creating.value = true
  try {
    // Prepare address data
    const addressPayload = {
      duong: addressData.value.duong.trim(),
      phuongXa: selectedWard.value.name,
      quanHuyen: selectedDistrict.value.name,
      tinhThanh: selectedProvince.value.name,
      loaiDiaChi: addressData.value.loaiDiaChi,
      laMacDinh: addressData.value.laMacDinh
    }

    console.log('Creating address with data:', addressPayload)

    // Emit the address data for parent component to handle
    emit('address-created', addressPayload)

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã thêm địa chỉ giao hàng thành công',
      life: 3000
    })

    // Close dialog and reset form
    visible.value = false
    resetForm()
  } catch (error) {
    console.error('Error creating address:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể thêm địa chỉ. Vui lòng thử lại.',
      life: 5000
    })
  } finally {
    creating.value = false
  }
}

const resetForm = () => {
  addressData.value = {
    duong: '',
    phuongXa: '',
    quanHuyen: '',
    tinhThanh: '',
    loaiDiaChi: 'Nhà riêng',
    laMacDinh: false
  }
  selectedProvince.value = null
  selectedDistrict.value = null
  selectedWard.value = null
  districts.value = []
  wards.value = []
  errors.value = {}
}

// Watch for ward selection
watch(selectedWard, (newWard) => {
  if (newWard) {
    addressData.value.phuongXa = newWard.name
  }
})

// Initialize
onMounted(() => {
  loadProvinces()
})
</script>
