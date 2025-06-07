import { ref, computed, watch } from 'vue'
import { debounce } from 'lodash-es'
import { useProductStore } from '@/stores/productstore'

export function useProductSearch() {
  const productStore = useProductStore()
  
  const searchQuery = ref('')
  const searchResults = ref([])
  const searching = ref(false)
  const searchCache = new Map()
  const searchHistory = ref([])

  const performSearch = async (query) => {
    if (!query.trim()) {
      searchResults.value = []
      return
    }

    // Check cache first
    const cacheKey = query.toLowerCase().trim()
    if (searchCache.has(cacheKey)) {
      searchResults.value = searchCache.get(cacheKey)
      return
    }

    searching.value = true
    try {
      const results = await productStore.searchProducts({
        tenSanPham: query,
        limit: 20
      })
      
      searchResults.value = results
      searchCache.set(cacheKey, results)
      
      // Add to search history
      addToSearchHistory(query)
    } catch (error) {
      console.error('Search error:', error)
      searchResults.value = []
    } finally {
      searching.value = false
    }
  }

  const debouncedSearch = debounce(performSearch, 300)

  const addToSearchHistory = (query) => {
    const trimmedQuery = query.trim()
    if (!trimmedQuery) return

    // Remove if already exists
    const existingIndex = searchHistory.value.indexOf(trimmedQuery)
    if (existingIndex > -1) {
      searchHistory.value.splice(existingIndex, 1)
    }

    // Add to beginning
    searchHistory.value.unshift(trimmedQuery)

    // Keep only last 10 searches
    if (searchHistory.value.length > 10) {
      searchHistory.value = searchHistory.value.slice(0, 10)
    }

    // Save to localStorage
    try {
      localStorage.setItem('productSearchHistory', JSON.stringify(searchHistory.value))
    } catch (error) {
      console.warn('Could not save search history to localStorage:', error)
    }
  }

  const loadSearchHistory = () => {
    try {
      const saved = localStorage.getItem('productSearchHistory')
      if (saved) {
        searchHistory.value = JSON.parse(saved)
      }
    } catch (error) {
      console.warn('Could not load search history from localStorage:', error)
      searchHistory.value = []
    }
  }

  const clearSearchHistory = () => {
    searchHistory.value = []
    try {
      localStorage.removeItem('productSearchHistory')
    } catch (error) {
      console.warn('Could not clear search history from localStorage:', error)
    }
  }

  const clearSearchCache = () => {
    searchCache.clear()
  }

  const searchInProducts = (products, query) => {
    if (!query.trim()) return products

    const searchTerm = query.toLowerCase().trim()
    return products.filter(product => {
      // Search in product name
      if (product.tenSanPham?.toLowerCase().includes(searchTerm)) {
        return true
      }

      // Search in product code
      if (product.maSanPham?.toLowerCase().includes(searchTerm)) {
        return true
      }

      // Search in description
      if (product.moTa?.toLowerCase().includes(searchTerm)) {
        return true
      }

      // Search in category name
      if (product.danhMuc?.tenDanhMuc?.toLowerCase().includes(searchTerm)) {
        return true
      }

      // Search in brand name
      if (product.thuongHieu?.moTaThuongHieu?.toLowerCase().includes(searchTerm)) {
        return true
      }

      // Search in variant serial numbers
      if (product.sanPhamChiTiets?.some(variant => 
        variant.serialNumber?.toLowerCase().includes(searchTerm)
      )) {
        return true
      }

      return false
    })
  }

  const highlightSearchTerm = (text, query) => {
    if (!text || !query.trim()) return text

    const searchTerm = query.trim()
    const regex = new RegExp(`(${searchTerm})`, 'gi')
    return text.replace(regex, '<mark>$1</mark>')
  }

  // Watch for search query changes
  watch(searchQuery, (newQuery) => {
    debouncedSearch(newQuery)
  })

  // Computed properties
  const hasSearchResults = computed(() => {
    return searchResults.value.length > 0
  })

  const hasSearchHistory = computed(() => {
    return searchHistory.value.length > 0
  })

  const searchSuggestions = computed(() => {
    if (!searchQuery.value.trim()) {
      return searchHistory.value.slice(0, 5)
    }

    const query = searchQuery.value.toLowerCase().trim()
    return searchHistory.value
      .filter(item => item.toLowerCase().includes(query))
      .slice(0, 5)
  })

  // Initialize search history on composable creation
  loadSearchHistory()

  return {
    searchQuery,
    searchResults,
    searching,
    searchHistory,
    hasSearchResults,
    hasSearchHistory,
    searchSuggestions,
    performSearch,
    debouncedSearch,
    searchInProducts,
    highlightSearchTerm,
    addToSearchHistory,
    clearSearchHistory,
    clearSearchCache,
    clearSearch: () => {
      searchQuery.value = ''
      searchResults.value = []
    }
  }
}
