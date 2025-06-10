import { computed } from 'vue'
import { useProductStore } from '@/stores/productstore'

/**
 * Composable for calculating dynamic price ranges based on actual product data
 * Provides reactive min/max prices that update when product data changes
 */
export function useDynamicPricing() {
  const productStore = useProductStore()

  // Calculate the minimum price across all products and variants
  const minPrice = computed(() => {
    if (!productStore.products?.length) return 0

    const allPrices = []

    productStore.products.forEach(product => {
      if (product.sanPhamChiTiets?.length) {
        product.sanPhamChiTiets.forEach(variant => {
          // Use promotional price if available and lower than regular price
          const regularPrice = variant.giaBan
          const promoPrice = variant.giaKhuyenMai
          const effectivePrice = (promoPrice && promoPrice < regularPrice) ? promoPrice : regularPrice
          
          if (effectivePrice && effectivePrice > 0) {
            allPrices.push(effectivePrice)
          }
        })
      }
    })

    return allPrices.length > 0 ? Math.min(...allPrices) : 0
  })

  // Calculate the maximum price across all products and variants
  const maxPrice = computed(() => {
    if (!productStore.products?.length) return 50000000 // Fallback to default

    const allPrices = []

    productStore.products.forEach(product => {
      if (product.sanPhamChiTiets?.length) {
        product.sanPhamChiTiets.forEach(variant => {
          // Use promotional price if available and lower than regular price
          const regularPrice = variant.giaBan
          const promoPrice = variant.giaKhuyenMai
          const effectivePrice = (promoPrice && promoPrice < regularPrice) ? promoPrice : regularPrice
          
          if (effectivePrice && effectivePrice > 0) {
            allPrices.push(effectivePrice)
          }
        })
      }
    })

    if (allPrices.length === 0) return 50000000 // Fallback to default

    const calculatedMax = Math.max(...allPrices)
    
    // Add 20% buffer to the maximum price for better UX
    // Round up to nearest 500,000 for cleaner slider values
    const bufferedMax = calculatedMax * 1.2
    const roundedMax = Math.ceil(bufferedMax / 500000) * 500000
    
    return Math.max(roundedMax, 1000000) // Minimum 1M VND for reasonable slider range
  })

  // Get the appropriate step size based on the price range
  const priceStep = computed(() => {
    const range = maxPrice.value - minPrice.value
    
    if (range <= 10000000) return 100000      // 100K steps for ranges up to 10M
    if (range <= 50000000) return 500000     // 500K steps for ranges up to 50M
    if (range <= 100000000) return 1000000   // 1M steps for ranges up to 100M
    return 2000000                           // 2M steps for larger ranges
  })

  // Get default price range for filters (0 to max)
  const defaultPriceRange = computed(() => [0, maxPrice.value])

  // Format price for display
  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(price)
  }

  // Format price as number (no currency symbol)
  const formatPriceNumber = (price) => {
    return new Intl.NumberFormat('vi-VN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(price)
  }

  // Check if price data is available
  const hasPriceData = computed(() => {
    return productStore.products?.some(product => 
      product.sanPhamChiTiets?.some(variant => 
        variant.giaBan && variant.giaBan > 0
      )
    )
  })

  return {
    minPrice,
    maxPrice,
    priceStep,
    defaultPriceRange,
    formatPrice,
    formatPriceNumber,
    hasPriceData
  }
}
