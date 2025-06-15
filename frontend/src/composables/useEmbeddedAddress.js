import { ref, computed, watch, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import addressApi from '@/apis/address'

/**
 * Composable for embedded address management
 * Extracted from FastAddressCreate.vue for reuse in embedded forms
 * Handles province/district/ward loading, validation, and form state management
 */
export function useEmbeddedAddress() {
  const toast = useToast()

  // Reactive state for address data
  const addressData = ref({
    duong: '',
    phuongXa: '',
    quanHuyen: '',
    tinhThanh: '',
    loaiDiaChi: 'Nhà riêng',
    laMacDinh: false
  })

  // Address API data
  const provinces = ref([])
  const districts = ref([])
  const wards = ref([])
  const selectedProvince = ref(null)
  const selectedDistrict = ref(null)
  const selectedWard = ref(null)

  // Loading states
  const loadingProvinces = ref(false)
  const loadingDistricts = ref(false)
  const loadingWards = ref(false)

  // Validation errors
  const errors = ref({})

  // Address types
  const addressTypes = [
    'Nhà riêng',
    'Văn phòng',
    'Khác'
  ]

  // Computed properties
  const isFormValid = computed(() => {
    return addressData.value.duong.trim() &&
           selectedProvince.value &&
           selectedDistrict.value &&
           selectedWard.value &&
           Object.keys(errors.value).length === 0
  })

  const isAddressComplete = computed(() => {
    return addressData.value.duong.trim() &&
           addressData.value.phuongXa &&
           addressData.value.quanHuyen &&
           addressData.value.tinhThanh
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

  const validateAddressForm = () => {
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

  const resetAddressForm = () => {
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

  const getAddressPayload = () => {
    return {
      duong: addressData.value.duong.trim(),
      phuongXa: selectedWard.value?.name || addressData.value.phuongXa,
      quanHuyen: selectedDistrict.value?.name || addressData.value.quanHuyen,
      tinhThanh: selectedProvince.value?.name || addressData.value.tinhThanh,
      loaiDiaChi: addressData.value.loaiDiaChi,
      laMacDinh: addressData.value.laMacDinh
    }
  }

  const setAddressData = (data) => {
    if (data) {
      addressData.value = { ...addressData.value, ...data }
    }
  }

  // Enhanced address management functions
  const compareAddresses = (address1, address2) => {
    if (!address1 || !address2) return false

    return address1.duong?.trim().toLowerCase() === address2.duong?.trim().toLowerCase() &&
           address1.phuongXa?.toLowerCase() === address2.phuongXa?.toLowerCase() &&
           address1.quanHuyen?.toLowerCase() === address2.quanHuyen?.toLowerCase() &&
           address1.tinhThanh?.toLowerCase() === address2.tinhThanh?.toLowerCase()
  }

  const findMatchingAddress = (currentAddress, customerAddresses) => {
    if (!customerAddresses || customerAddresses.length === 0) return null

    return customerAddresses.find(addr => compareAddresses(currentAddress, addr))
  }

  const isAddressDifferentFromCustomer = (customer) => {
    if (!customer || !customer.diaChis || customer.diaChis.length === 0) {
      return true // No existing addresses, so current address is different
    }

    const currentAddress = getAddressPayload()
    return !findMatchingAddress(currentAddress, customer.diaChis)
  }

  const addAddressToCustomer = async (customer, customerStore) => {
    try {
      if (!customer || !customerStore) {
        throw new Error('Customer or customerStore is required')
      }

      if (!isAddressComplete.value) {
        throw new Error('Address information is incomplete')
      }

      // Check if address already exists
      if (!isAddressDifferentFromCustomer(customer)) {
        console.log('Address already exists for customer, no need to add')
        return { success: true, message: 'Address already exists' }
      }

      // Prepare new address payload
      const newAddress = {
        ...getAddressPayload(),
        laMacDinh: customer.diaChis?.length === 0 // Set as default if it's the first address
      }

      // Create updated customer data with new address
      const updatedCustomerData = {
        ...customer,
        diaChis: [...(customer.diaChis || []), newAddress]
      }

      console.log('Adding new address to customer:', newAddress)

      // Update customer via store
      const updatedCustomer = await customerStore.updateCustomer(customer.id, updatedCustomerData)

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Đã thêm địa chỉ mới vào danh sách của khách hàng',
        life: 3000
      })

      return {
        success: true,
        message: 'Address added successfully',
        updatedCustomer: updatedCustomer
      }

    } catch (error) {
      console.error('Error adding address to customer:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: error.message || 'Không thể thêm địa chỉ vào danh sách khách hàng',
        life: 3000
      })
      return { success: false, error: error.message }
    }
  }

  // Watch for ward selection to update addressData
  const wardWatcher = watch(selectedWard, (newWard) => {
    if (newWard) {
      addressData.value.phuongXa = newWard.name
    }
  })

  // Cleanup watchers on unmount
  onUnmounted(() => {
    wardWatcher()
  })

  // Initialize provinces on composable creation
  loadProvinces()

  // Return reactive state and methods
  return {
    // Reactive state
    addressData,
    provinces,
    districts,
    wards,
    selectedProvince,
    selectedDistrict,
    selectedWard,
    loadingProvinces,
    loadingDistricts,
    loadingWards,
    errors,
    addressTypes,

    // Computed properties
    isFormValid,
    isAddressComplete,

    // Methods
    loadProvinces,
    onProvinceChange,
    onDistrictChange,
    validateAddressForm,
    resetAddressForm,
    getAddressPayload,
    setAddressData,

    // Enhanced address management methods
    compareAddresses,
    findMatchingAddress,
    isAddressDifferentFromCustomer,
    addAddressToCustomer
  }
}
