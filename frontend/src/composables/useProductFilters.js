import { ref, computed, watch } from 'vue'
import { useProductStore } from '@/stores/productstore'
// import { useAttributeStore } from '@/stores/attributestore' // Currently unused
import { useDynamicPricing } from './useDynamicPricing'

export function useProductFilters() {
  const productStore = useProductStore()
  const { defaultPriceRange } = useDynamicPricing()

  const filters = ref({
    tenSanPham: '',
    maSanPham: '',
    danhMuc: null,
    thuongHieu: null,
    cpu: null,
    ram: null,
    gpu: null,
    priceRange: [0, 50000000], // Will be updated dynamically
    trangThai: null
  })

  // Watch for price range changes and update filter default
  watch(defaultPriceRange, (newRange) => {
    // Only update if current range is at the old default
    if (filters.value.priceRange[0] === 0 && filters.value.priceRange[1] === 50000000) {
      filters.value.priceRange = [...newRange]
    }
  }, { immediate: true })

  const filteredProducts = computed(() => {
    let products = productStore.products

    if (filters.value.tenSanPham) {
      products = products.filter(p =>
        p.tenSanPham.toLowerCase().includes(filters.value.tenSanPham.toLowerCase())
      )
    }

    if (filters.value.maSanPham) {
      products = products.filter(p =>
        p.maSanPham.toLowerCase().includes(filters.value.maSanPham.toLowerCase())
      )
    }

    if (filters.value.danhMuc) {
      products = products.filter(p =>
        p.danhMucs?.some(danhMuc => danhMuc.id === filters.value.danhMuc)
      )
    }

    if (filters.value.thuongHieu) {
      products = products.filter(p => p.thuongHieu?.id === filters.value.thuongHieu)
    }

    // Filter by variant attributes (CPU, RAM, GPU)
    if (filters.value.cpu) {
      products = products.filter(p =>
        p.sanPhamChiTiets?.some(variant => variant.cpu?.id === filters.value.cpu)
      )
    }

    if (filters.value.ram) {
      products = products.filter(p =>
        p.sanPhamChiTiets?.some(variant => variant.ram?.id === filters.value.ram)
      )
    }

    if (filters.value.gpu) {
      products = products.filter(p =>
        p.sanPhamChiTiets?.some(variant => variant.gpu?.id === filters.value.gpu)
      )
    }

    if (filters.value.trangThai !== null) {
      products = products.filter(p => p.trangThai === filters.value.trangThai)
    }

    // Price range filter - enhanced to handle dual-price structure properly
    if (filters.value.priceRange && filters.value.priceRange.length === 2) {
      products = products.filter(p => {
        // If product has no variants, include it in the results (don't filter out)
        // This allows products without pricing to be visible
        if (!p.sanPhamChiTiets?.length) {
          return true
        }

        // Get all variant prices (including promotional prices)
        const prices = p.sanPhamChiTiets.map(detail => {
          // Use promotional price if available and lower than regular price
          const regularPrice = detail.giaBan
          const promoPrice = detail.giaKhuyenMai
          const effectivePrice = (promoPrice && promoPrice < regularPrice) ? promoPrice : regularPrice

          // Handle null/undefined prices by returning 0
          return effectivePrice || 0
        }).filter(price => price > 0) // Remove zero prices

        // If no valid prices found, include the product (don't filter out)
        if (prices.length === 0) {
          return true
        }

        const minPrice = Math.min(...prices)
        const maxPrice = Math.max(...prices)

        // Product overlaps with filter range if:
        // - Product's min price is within filter range, OR
        // - Product's max price is within filter range, OR
        // - Product's price range completely contains filter range
        return (
          (minPrice >= filters.value.priceRange[0] && minPrice <= filters.value.priceRange[1]) ||
          (maxPrice >= filters.value.priceRange[0] && maxPrice <= filters.value.priceRange[1]) ||
          (minPrice <= filters.value.priceRange[0] && maxPrice >= filters.value.priceRange[1])
        )
      })
    }

    return products
  })

  const clearFilters = () => {
    filters.value = {
      tenSanPham: '',
      maSanPham: '',
      danhMuc: null,
      thuongHieu: null,
      cpu: null,
      ram: null,
      gpu: null,
      priceRange: [...defaultPriceRange.value],
      trangThai: null
    }
  }

  const updateFilter = (key, value) => {
    filters.value[key] = value
  }

  const applyFilters = () => {
    // Update store filters
    productStore.updateFilters(filters.value)
  }

  // Watch for filter changes and apply them automatically
  watch(filters, (newFilters) => {
    productStore.updateFilters(newFilters)
  }, { deep: true })

  return {
    filters,
    filteredProducts,
    clearFilters,
    updateFilter,
    applyFilters,
    defaultPriceRange
  }
}
