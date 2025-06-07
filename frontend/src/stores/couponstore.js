import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import phieuGiamGiaApi from '@/apis/coupon';
import { useToast } from 'primevue/usetoast';

/**
 * Consolidated PhieuGiamGia Store
 * Merges functionality from couponstore.js and voucherStore.js
 * Handles all voucher operations with proper error handling and DTO alignment
 */
export const usePhieuGiamGiaStore = defineStore('phieuGiamGia', () => {
  const toast = useToast();

  // State
  const phieuGiamGiaList = ref([]);
  const currentPhieu = ref(null);
  const loading = ref(false);
  const error = ref(null);

  // Search and filtering
  const search = ref('');
  const statusFilter = ref('all');
  const startDate = ref('');
  const endDate = ref('');
  const showSuggestions = ref(false);
  const searchSuggestions = ref([]);

  // Validation and order integration
  const validationResults = ref({});
  const appliedVouchers = ref([]);

  // Discount type options with proper enum values
  const discountTypeOptions = ref([
    { label: 'Giảm theo phần trăm', value: 'PHAN_TRAM' },
    { label: 'Giảm số tiền cố định', value: 'SO_TIEN_CO_DINH' }
  ]);

  // Status options with proper enum values
  const statusOptions = ref([
    { label: 'Chưa diễn ra', value: 'CHUA_DIEN_RA' },
    { label: 'Đang diễn ra', value: 'DA_DIEN_RA' },
    { label: 'Kết thúc', value: 'KET_THUC' },
    { label: 'Đã hủy', value: 'BI_HUY' }
  ]);

  // Computed
  const filteredVouchers = computed(() => {
    let filtered = phieuGiamGiaList.value;

    if (search.value) {
      const searchText = search.value.toLowerCase();
      filtered = filtered.filter(item =>
        item.maPhieuGiamGia.toLowerCase().includes(searchText) ||
        (item.moTa && item.moTa.toLowerCase().includes(searchText))
      );
    }

    if (statusFilter.value && statusFilter.value !== 'all') {
      filtered = filtered.filter(item => item.trangThai === statusFilter.value);
    }

    if (startDate.value) {
      filtered = filtered.filter(item => new Date(item.ngayBatDau) >= new Date(startDate.value));
    }

    if (endDate.value) {
      filtered = filtered.filter(item => new Date(item.ngayKetThuc) <= new Date(endDate.value));
    }

    // Sort by status priority (Active, Upcoming, Ended, Cancelled)
    return filtered.sort((a, b) => {
      const order = {
        DA_DIEN_RA: 1,
        CHUA_DIEN_RA: 2,
        KET_THUC: 3,
        BI_HUY: 4
      };
      return order[a.trangThai] - order[b.trangThai];
    });
  });

  // Actions
  async function fetchPhieuGiamGia(params = {}) {
    loading.value = true;
    error.value = null;
    try {
      const filterParams = {
        status: statusFilter.value,
        search: search.value,
        startDate: startDate.value,
        endDate: endDate.value,
        ...params
      };

      const response = await phieuGiamGiaApi.getAllVouchers(filterParams);
      phieuGiamGiaList.value = response || [];
    } catch (err) {
      error.value = err.message;
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: err.message,
        life: 5000
      });
    } finally {
      loading.value = false;
    }
  }

  async function fetchPhieuById(id) {
    loading.value = true;
    error.value = null;
    try {
      const response = await phieuGiamGiaApi.getVoucherById(id);
      currentPhieu.value = response;
      return response;
    } catch (err) {
      error.value = err.message;
      console.error("Lỗi khi lấy phiếu giảm giá theo ID:", err);
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: err.message,
        life: 5000
      });
      throw err;
    } finally {
      loading.value = false;
    }
  }



  async function createPhieu(voucherData) {
    loading.value = true;
    error.value = null;
    try {
      // Ensure proper DTO structure with enum values
      const data = {
        ...voucherData,
        loaiGiamGia: voucherData.loaiGiamGia, // Should be 'PHAN_TRAM' or 'SO_TIEN_CO_DINH'
        trangThai: voucherData.trangThai || 'CHUA_DIEN_RA',
        danhSachNguoiDung: voucherData.danhSachNguoiDung || []
      };

      const response = await phieuGiamGiaApi.createVoucher(data);
      phieuGiamGiaList.value.push(response);

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Tạo phiếu giảm giá thành công!',
        life: 3000
      });

      return response;
    } catch (err) {
      error.value = err.message;
      console.error("Lỗi khi tạo phiếu giảm giá:", err);
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: err.message,
        life: 5000
      });
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function updatePhieu(id, voucherData) {
    loading.value = true;
    error.value = null;
    try {
      // Ensure proper DTO structure with enum values
      const data = {
        ...voucherData,
        loaiGiamGia: voucherData.loaiGiamGia,
        danhSachNguoiDung: voucherData.danhSachNguoiDung || []
      };

      const response = await phieuGiamGiaApi.updateVoucher(id, data);

      // Update voucher in list
      const index = phieuGiamGiaList.value.findIndex(voucher => voucher.id === id);
      if (index !== -1) {
        phieuGiamGiaList.value[index] = { ...phieuGiamGiaList.value[index], ...response };
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Cập nhật phiếu giảm giá thành công!',
        life: 3000
      });

      return response;
    } catch (err) {
      error.value = err.message;
      console.error("Lỗi khi cập nhật phiếu giảm giá:", err);
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: err.message,
        life: 5000
      });
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function closePhieu(id) {
    loading.value = true;
    error.value = null;
    try {
      const response = await phieuGiamGiaApi.deleteVoucher(id);

      // Update voucher status in list
      const index = phieuGiamGiaList.value.findIndex(voucher => voucher.id === id);
      if (index !== -1) {
        phieuGiamGiaList.value[index].trangThai = 'BI_HUY';
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Đóng phiếu giảm giá thành công!',
        life: 3000
      });

      return response;
    } catch (err) {
      error.value = err.message;
      console.error("Lỗi khi đóng phiếu giảm giá:", err);
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: err.message,
        life: 5000
      });
      throw err;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Enhanced close voucher with audit trail support
   * @param {number} id - Voucher ID
   * @param {string} reason - Reason for closing the voucher
   * @returns {Promise<Object>} Closed voucher data
   */
  async function closePhieuWithAudit(id, reason) {
    loading.value = true;
    error.value = null;
    try {
      const response = await phieuGiamGiaApi.deleteVoucherWithAudit(id, reason);

      // Update voucher status in list
      const index = phieuGiamGiaList.value.findIndex(voucher => voucher.id === id);
      if (index !== -1) {
        phieuGiamGiaList.value[index].trangThai = 'KET_THUC';
        phieuGiamGiaList.value[index].ngayKetThuc = new Date().toISOString();
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Đóng phiếu giảm giá thành công!',
        life: 3000
      });

      return response;
    } catch (err) {
      error.value = err.message;
      console.error("Lỗi khi đóng phiếu giảm giá với audit:", err);
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: err.message,
        life: 5000
      });
      throw err;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Enhanced create voucher with audit trail support
   * @param {Object} voucherData - Voucher data
   * @param {string} reason - Reason for creating the voucher
   * @returns {Promise<Object>} Created voucher data
   */
  async function createPhieuWithAudit(voucherData, reason) {
    loading.value = true;
    error.value = null;
    try {
      const response = await phieuGiamGiaApi.createVoucherWithAudit(voucherData, reason);
      phieuGiamGiaList.value.push(response);

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Tạo phiếu giảm giá thành công!',
        life: 3000
      });

      return response;
    } catch (err) {
      error.value = err.message;
      console.error("Lỗi khi tạo phiếu giảm giá với audit:", err);
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: err.message,
        life: 5000
      });
      throw err;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Enhanced update voucher with audit trail support
   * @param {number} id - Voucher ID
   * @param {Object} voucherData - Updated voucher data
   * @param {string} reason - Reason for updating the voucher
   * @returns {Promise<Object>} Updated voucher data
   */
  async function updatePhieuWithAudit(id, voucherData, reason) {
    loading.value = true;
    error.value = null;
    try {
      const response = await phieuGiamGiaApi.updateVoucherWithAudit(id, voucherData, reason);

      // Update voucher in list
      const index = phieuGiamGiaList.value.findIndex(voucher => voucher.id === id);
      if (index !== -1) {
        phieuGiamGiaList.value[index] = { ...phieuGiamGiaList.value[index], ...response };
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Cập nhật phiếu giảm giá thành công!',
        life: 3000
      });

      return response;
    } catch (err) {
      error.value = err.message;
      console.error("Lỗi khi cập nhật phiếu giảm giá với audit:", err);
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: err.message,
        life: 5000
      });
      throw err;
    } finally {
      loading.value = false;
    }
  }

  // Filter and search actions
  function setStatusFilter(status) {
    statusFilter.value = status;
  }

  function setStartDate(date) {
    startDate.value = date;
  }

  function setEndDate(date) {
    endDate.value = date;
  }

  function clearFilters() {
    search.value = '';
    statusFilter.value = null;
    startDate.value = null;
    endDate.value = null;
  }

  function setSearchSuggestions(suggestions) {
    searchSuggestions.value = suggestions;
    showSuggestions.value = suggestions.length > 0;
  }

  function hideSuggestions() {
    showSuggestions.value = false;
  }

  function clearError() {
    error.value = null;
  }

  // Utility functions
  function getDiscountTypeLabel(type) {
    const option = discountTypeOptions.value.find(opt => opt.value === type);
    return option ? option.label : type;
  }

  function getStatusLabel(status) {
    const option = statusOptions.value.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  function getStatusSeverity(status) {
    const severityMap = {
      CHUA_DIEN_RA: 'warn',
      DA_DIEN_RA: 'success',
      KET_THUC: 'danger',
      BI_HUY: 'secondary'
    };
    return severityMap[status] || 'info';
  }

  // Return store interface
  return {
    // State
    phieuGiamGiaList,
    currentPhieu,
    loading,
    error,
    search,
    statusFilter,
    startDate,
    endDate,
    showSuggestions,
    searchSuggestions,
    validationResults,
    appliedVouchers,
    discountTypeOptions,
    statusOptions,

    // Computed
    filteredVouchers,

    // Actions
    fetchPhieuGiamGia,
    fetchPhieuById,
    createPhieu,
    updatePhieu,
    closePhieu,
    // Enhanced audit trail methods
    closePhieuWithAudit,
    createPhieuWithAudit,
    updatePhieuWithAudit,
    setStatusFilter,
    setStartDate,
    setEndDate,
    clearFilters,
    setSearchSuggestions,
    hideSuggestions,
    clearError,

    // Utilities
    getDiscountTypeLabel,
    getStatusLabel,
    getStatusSeverity
  };
});
