import { ref, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useProductStore } from '@/stores/productstore'
import serialNumberApi from '@/apis/serialNumberApi'

export function useProductForm() {
  const toast = useToast()
  const productStore = useProductStore()
  const loading = ref(false)
  const errors = ref({})

  const productForm = ref({
    maSanPham: '',
    tenSanPham: '',
    moTa: '',
    ngayRaMat: null,
    danhMucs: [], // Changed to support multiple categories
    thuongHieu: null,
    hinhAnh: [],
    trangThai: true,
    sanPhamChiTiets: []
  })

  const validateForm = () => {
    errors.value = {}

    // Product code validation - optional, auto-generated if empty
    if (productForm.value.maSanPham?.trim()) {
      if (productForm.value.maSanPham.length < 2) {
        errors.value.maSanPham = 'Mã sản phẩm phải có ít nhất 2 ký tự'
      } else if (productForm.value.maSanPham.length > 100) {
        errors.value.maSanPham = 'Mã sản phẩm không được vượt quá 100 ký tự'
      }
    }
    // Note: If maSanPham is empty, backend will auto-generate it

    // Product name validation
    if (!productForm.value.tenSanPham?.trim()) {
      errors.value.tenSanPham = 'Tên sản phẩm không được để trống'
    } else if (productForm.value.tenSanPham.length < 3) {
      errors.value.tenSanPham = 'Tên sản phẩm phải có ít nhất 3 ký tự'
    } else if (productForm.value.tenSanPham.length > 255) {
      errors.value.tenSanPham = 'Tên sản phẩm không được vượt quá 255 ký tự'
    }

    // Category validation
    if (!productForm.value.danhMucs || productForm.value.danhMucs.length === 0) {
      errors.value.danhMucs = 'Vui lòng chọn ít nhất một danh mục'
    }

    // Brand validation
    if (!productForm.value.thuongHieu) {
      errors.value.thuongHieu = 'Vui lòng chọn thương hiệu'
    }

    // Description validation
    if (productForm.value.moTa && productForm.value.moTa.length > 5000) {
      errors.value.moTa = 'Mô tả không được vượt quá 5000 ký tự'
    }

    // Variants validation (optional - allow products without variants)
    if (productForm.value.sanPhamChiTiets.length > 0) {
      // Validate each variant if they exist
      const variantErrors = []
      productForm.value.sanPhamChiTiets.forEach((variant, index) => {
        const variantError = {}

        // Serial number validation removed - managed separately

        if (!variant.giaBan || variant.giaBan <= 0) {
          variantError.giaBan = 'Giá bán phải lớn hơn 0'
        }

        if (variant.giaKhuyenMai && variant.giaKhuyenMai >= variant.giaBan) {
          variantError.giaKhuyenMai = 'Giá khuyến mãi phải nhỏ hơn giá bán'
        }

        if (Object.keys(variantError).length > 0) {
          variantErrors[index] = variantError
        }
      })

      if (variantErrors.length > 0) {
        errors.value.variants = variantErrors
      }
    }

    // Images validation
    if (productForm.value.hinhAnh.length > 10) {
      errors.value.hinhAnh = 'Không được tải lên quá 10 hình ảnh'
    }

    return Object.keys(errors.value).length === 0
  }

  const submitForm = async (isEdit = false) => {
    if (!validateForm()) {
      toast.add({
        severity: 'error',
        summary: 'Lỗi xác thực',
        detail: 'Vui lòng kiểm tra lại thông tin nhập vào',
        life: 3000
      })
      return false
    }

    loading.value = true
    try {
      let result

      // Prepare form data
      const formData = {
        ...productForm.value,
        // Convert date to proper format if needed
        ngayRaMat: productForm.value.ngayRaMat ?
          new Date(productForm.value.ngayRaMat).toISOString().split('T')[0] : null,
        // Keep danhMucs array for many-to-many relationship
        danhMucs: productForm.value.danhMucs || []
      }

      if (isEdit) {
        // Use updateProductWithVariants if variants are present, otherwise use regular update
        if (formData.sanPhamChiTiets && formData.sanPhamChiTiets.length > 0) {
          result = await productStore.updateProductWithVariants(productForm.value.id, formData)
        } else {
          result = await productStore.updateProduct(productForm.value.id, formData)
        }
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Cập nhật sản phẩm thành công',
          life: 3000
        })
      } else {
        if (formData.sanPhamChiTiets.length > 0) {
          result = await productStore.createProductWithVariants(formData)

          // Create serial numbers for each variant after product creation
          await createSerialNumbersForVariants(result, formData.sanPhamChiTiets)
        } else {
          result = await productStore.createProduct(formData)
        }
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Thêm sản phẩm thành công',
          life: 3000
        })
      }

      return result
    } catch (error) {
      console.error('Form submission error:', error)

      // Handle validation errors from backend
      if (error.response?.status === 400 && error.response?.data?.errors) {
        const backendErrors = error.response.data.errors
        Object.keys(backendErrors).forEach(field => {
          errors.value[field] = backendErrors[field]
        })

        toast.add({
          severity: 'error',
          summary: 'Lỗi xác thực',
          detail: 'Dữ liệu không hợp lệ, vui lòng kiểm tra lại',
          life: 3000
        })
      } else {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || `Lỗi ${isEdit ? 'cập nhật' : 'thêm'} sản phẩm`,
          life: 3000
        })
      }
      return false
    } finally {
      loading.value = false
    }
  }

  const resetForm = () => {
    productForm.value = {
      maSanPham: '',
      tenSanPham: '',
      moTa: '',
      ngayRaMat: null,
      danhMucs: [],
      thuongHieu: null,
      hinhAnh: [],
      trangThai: true,
      sanPhamChiTiets: []
    }
    errors.value = {}
  }

  const addVariant = (variant) => {
    productForm.value.sanPhamChiTiets.push(variant)
  }

  const removeVariant = (index) => {
    productForm.value.sanPhamChiTiets.splice(index, 1)
  }

  const updateVariant = (index, variant) => {
    productForm.value.sanPhamChiTiets[index] = variant
  }

  const addImage = (imageUrl) => {
    if (productForm.value.hinhAnh.length < 10) {
      productForm.value.hinhAnh.push(imageUrl)
    }
  }

  const removeImage = (index) => {
    productForm.value.hinhAnh.splice(index, 1)
  }

  // Computed properties
  const isFormValid = computed(() => {
    return productForm.value.tenSanPham?.trim() &&
           productForm.value.danhMucs?.length > 0 &&
           productForm.value.thuongHieu
    // Note: maSanPham is optional - will be auto-generated if empty
  })

  const hasErrors = computed(() => {
    return Object.keys(errors.value).length > 0
  })

  /**
   * Create serial numbers for product variants after product creation
   * @param {Object} createdProduct - The created product with variants
   * @param {Array} variantData - Original variant data with serial numbers
   */
  const createSerialNumbersForVariants = async (createdProduct, variantData) => {
    try {
      const serialNumberPromises = []

      // Get the created variants from the response
      const createdVariants = createdProduct.sanPhamChiTiets || []

      for (let i = 0; i < variantData.length; i++) {
        const originalVariant = variantData[i]
        const createdVariant = createdVariants[i]

        if (!createdVariant || !originalVariant.serialNumbers || originalVariant.serialNumbers.length === 0) {
          continue
        }

        // Create serial numbers for this variant
        const serialNumberPromise = serialNumberApi.createMultipleSerialNumbers(
          createdVariant.id,
          originalVariant.serialNumbers
        )

        serialNumberPromises.push(serialNumberPromise)
      }

      if (serialNumberPromises.length > 0) {
        const results = await Promise.allSettled(serialNumberPromises)

        let totalSuccessful = 0
        let totalFailed = 0

        results.forEach((result, index) => {
          if (result.status === 'fulfilled') {
            totalSuccessful += result.value.successful.length
            totalFailed += result.value.failed.length

            if (result.value.failed.length > 0) {
              console.warn(`Failed to create some serial numbers for variant ${index}:`, result.value.failed)
            }
          } else {
            console.error(`Failed to create serial numbers for variant ${index}:`, result.reason)

            // Check if it's an authorization error
            if (result.reason.message && result.reason.message.includes('403')) {
              console.warn('Serial number creation requires ADMIN or MANAGER role')
            }

            // Count all serial numbers for this variant as failed
            const originalVariant = variantData[index]
            if (originalVariant.serialNumbers) {
              totalFailed += originalVariant.serialNumbers.length
            }
          }
        })

        // Show summary toast
        if (totalSuccessful > 0) {
          toast.add({
            severity: 'success',
            summary: 'Serial Numbers',
            detail: `Đã tạo thành công ${totalSuccessful} serial numbers`,
            life: 3000
          })
        }

        if (totalFailed > 0) {
          // Check if all failures are due to authorization
          const hasAuthError = results.some(result =>
            result.status === 'rejected' &&
            result.reason.message &&
            result.reason.message.includes('403')
          )

          if (hasAuthError) {
            toast.add({
              severity: 'warn',
              summary: 'Quyền hạn',
              detail: `Sản phẩm đã được tạo thành công, nhưng không thể tạo serial numbers. Cần quyền ADMIN hoặc MANAGER để tạo serial numbers.`,
              life: 8000
            })
          } else {
            toast.add({
              severity: 'warn',
              summary: 'Cảnh báo',
              detail: `${totalFailed} serial numbers không thể tạo được`,
              life: 5000
            })
          }
        }
      }
    } catch (error) {
      console.error('Error creating serial numbers:', error)

      // Check if it's an authorization error
      if (error.message && error.message.includes('403')) {
        toast.add({
          severity: 'warn',
          summary: 'Quyền hạn',
          detail: 'Sản phẩm đã được tạo thành công, nhưng không thể tạo serial numbers. Cần quyền ADMIN hoặc MANAGER.',
          life: 8000
        })
      } else {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: 'Có lỗi xảy ra khi tạo serial numbers',
          life: 5000
        })
      }
    }
  }

  return {
    productForm,
    errors,
    loading,
    isFormValid,
    hasErrors,
    validateForm,
    submitForm,
    resetForm,
    addVariant,
    removeVariant,
    updateVariant,
    addImage,
    removeImage
  }
}
