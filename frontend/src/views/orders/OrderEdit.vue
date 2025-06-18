<template>
  <div class="order-edit-container">
    <Toast />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-pencil text-lg text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">
              Chỉnh sửa đơn hàng
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              Cập nhật thông tin đơn hàng {{ orderData?.maHoaDon }}
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <Button
            label="Quay lại"
            icon="pi pi-arrow-left"
            outlined
            @click="$router.push('/orders')"
          />
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="card">
      <div class="text-center py-12">
        <i class="pi pi-spinner pi-spin text-4xl text-surface-300 mb-4"></i>
        <h3 class="text-xl font-semibold text-surface-600 mb-2">Đang tải dữ liệu...</h3>
        <p class="text-surface-500">Vui lòng chờ trong giây lát</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="card">
      <div class="text-center py-12">
        <i class="pi pi-exclamation-triangle text-4xl text-red-500 mb-4"></i>
        <h3 class="text-xl font-semibold text-surface-600 mb-2">Có lỗi xảy ra</h3>
        <p class="text-surface-500 mb-4">{{ error }}</p>
        <Button
          label="Thử lại"
          icon="pi pi-refresh"
          @click="loadOrderData"
        />
      </div>
    </div>

    <!-- Edit Form -->
    <div v-else-if="orderData" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Left Column: Order Items & Customer -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Order Status Info -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-info-circle text-primary"></i>
            Thông tin đơn hàng
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1">Mã đơn hàng</label>
              <InputText
                :value="orderData.maHoaDon"
                readonly
                class="w-full"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">Loại đơn hàng</label>
              <InputText
                :value="orderStore.getOrderTypeInfo(orderData.loaiHoaDon).label"
                readonly
                class="w-full"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">Trạng thái đơn hàng</label>
              <Tag
                :value="orderStore.getOrderStatusInfo(orderData.trangThaiDonHang).label"
                :severity="orderStore.getOrderStatusInfo(orderData.trangThaiDonHang).severity"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1">Trạng thái thanh toán</label>
              <Tag
                :value="orderStore.getPaymentStatusInfo(orderData.trangThaiThanhToan).label"
                :severity="orderStore.getPaymentStatusInfo(orderData.trangThaiThanhToan).severity"
              />
            </div>
          </div>
        </div>

        <!-- Customer Selection -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <i class="pi pi-user text-primary"></i>
              Khách hàng
            </div>
            <Button
              label="Thêm nhanh"
              icon="pi pi-user-plus"
              size="small"
              severity="success"
              outlined
              @click="showFastCustomerDialog"
            />
          </div>

          <!-- Customer Search -->
          <div class="mb-4">
            <AutoComplete
              v-model="selectedCustomer"
              :suggestions="customerSuggestions"
              @complete="searchCustomers"
              @item-select="onCustomerSelect"
              :optionLabel="getCustomerDisplayLabel"
              placeholder="Tìm kiếm khách hàng (tên hoặc số điện thoại)..."
              fluid
            >
              <template #item="{ item }">
                <div class="flex items-center gap-2 p-2">
                  <Avatar :label="item.hoTen?.charAt(0)" size="small" />
                  <div>
                    <div class="font-medium">{{ item.hoTen }} - {{ item.soDienThoai }}</div>
                    <div class="text-sm text-surface-500">{{ item.email || 'Không có email' }}</div>
                  </div>
                </div>
              </template>
            </AutoComplete>
          </div>

          <!-- Selected Customer Display -->
          <div v-if="editForm.khachHang" class="p-3 border rounded-lg bg-surface-50">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <Avatar :label="editForm.khachHang.hoTen?.charAt(0)" size="small" />
                <div>
                  <div class="font-semibold text-sm">{{ editForm.khachHang.hoTen }}</div>
                  <div class="text-xs text-surface-500">{{ editForm.khachHang.soDienThoai }}</div>
                </div>
              </div>
              <Button
                icon="pi pi-times"
                text
                rounded
                size="small"
                @click="clearCustomer"
                class="text-surface-400 hover:text-red-500"
              />
            </div>
          </div>

          <!-- Walk-in Customer Note -->
          <div v-else class="text-center py-3 text-surface-500">
            <i class="pi pi-user-plus text-lg mb-1"></i>
            <p class="text-xs">Khách hàng vãng lai</p>
          </div>
        </div>

        <!-- Voucher Section -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-tag text-primary"></i>
            Voucher giảm giá
          </div>

          <!-- Applied Vouchers -->
          <div v-if="editForm.voucherList?.length" class="space-y-2 mb-4">
            <div class="font-medium mb-3 text-sm flex items-center gap-2">
              <i class="pi pi-sparkles text-primary"></i>
              Voucher đã áp dụng
            </div>
            <div
              v-for="(voucher, index) in editForm.voucherList"
              :key="index"
              class="relative flex items-center justify-between p-3 border rounded-lg bg-green-50 border-green-200"
            >
              <div class="flex-1">
                <div class="font-medium text-green-800 text-sm">{{ voucher.maPhieuGiamGia }}</div>
                <div class="text-xs text-green-600 mt-1">
                  Giảm {{ formatCurrency(voucher.giaTriGiam) }}
                </div>
              </div>
              <Button
                icon="pi pi-times"
                text
                rounded
                size="small"
                severity="danger"
                @click="removeVoucher(index)"
              />
            </div>
          </div>

          <!-- Available Vouchers Display -->
          <div v-if="displayedAvailableVouchers.length" class="mb-4">
            <div class="font-medium mb-3 text-sm flex items-center gap-2">
              <i class="pi pi-sparkles text-primary"></i>
              Voucher khả dụng
            </div>

            <!-- Voucher Cards Container -->
            <div class="space-y-3">
              <div
                v-for="voucher in displayedAvailableVouchers"
                :key="voucher.id"
                class="relative p-3 border rounded-lg transition-all cursor-pointer hover:shadow-md border-surface-200 bg-surface-50"
                @click="selectVoucher(voucher)"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="font-semibold text-sm mb-1 text-surface-900">
                      {{ voucher.tenPhieuGiamGia || voucher.maPhieuGiamGia }}
                    </div>
                    <div class="text-xs text-surface-500 mb-2">{{ voucher.maPhieuGiamGia }}</div>

                    <!-- Voucher Details -->
                    <div class="space-y-1">
                      <div class="text-sm font-medium text-primary">
                        Giảm {{ formatCurrency(calculateVoucherDiscount(voucher)) }}
                      </div>

                      <!-- Conditions -->
                      <div class="text-xs text-surface-600">
                        <span v-if="voucher.giaTriDonHangToiThieu">
                          Đơn tối thiểu: {{ formatCurrency(voucher.giaTriDonHangToiThieu) }}
                        </span>
                        <span v-if="voucher.giaTriGiamToiDa && voucher.loaiGiamGia === 'PHAN_TRAM'">
                          • Giảm tối đa: {{ formatCurrency(voucher.giaTriGiamToiDa) }}
                        </span>
                      </div>

                      <!-- Expiry -->
                      <div class="text-xs text-surface-500">
                        <i class="pi pi-calendar text-xs mr-1"></i>
                        Hết hạn: {{ formatDate(voucher.ngayKetThuc) }}
                      </div>
                    </div>
                  </div>

                  <Button
                    icon="pi pi-plus"
                    text
                    rounded
                    size="small"
                    class="text-primary hover:bg-primary/10"
                  />
                </div>
              </div>
            </div>

            <!-- Show More/Less Button -->
            <div v-if="availableVouchers.length > voucherDisplayLimit" class="text-center mt-3">
              <Button
                :label="showAllVouchers ? 'Thu gọn' : `Xem thêm ${availableVouchers.length - voucherDisplayLimit} voucher`"
                :icon="showAllVouchers ? 'pi pi-angle-up' : 'pi pi-angle-down'"
                text
                size="small"
                @click="toggleVoucherDisplay"
              />
            </div>
          </div>

          <!-- No Vouchers Available -->
          <div v-if="!editForm.voucherList?.length && !availableVouchers.length && editForm.khachHang" class="mb-4 p-3 border border-dashed border-surface-300 rounded-lg text-center">
            <i class="pi pi-info-circle text-surface-400 text-lg mb-2"></i>
            <p class="text-sm text-surface-500">Không có voucher khả dụng cho đơn hàng này</p>
          </div>
        </div>

        <!-- Order Items -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <i class="pi pi-shopping-cart text-primary"></i>
              Sản phẩm trong đơn hàng
            </div>
            <div class="flex items-center gap-2">
              <Badge
                v-if="editForm.sanPhamList?.length > 0"
                :value="editForm.sanPhamList.length"
                severity="info"
              />
              <Button
                label="Thêm sản phẩm"
                icon="pi pi-plus"
                size="small"
                severity="primary"
                outlined
                @click="showProductSelectionDialog"
              />
            </div>
          </div>

          <!-- Order Items List -->
          <div v-if="editForm.sanPhamList?.length" class="space-y-3 mb-4">
            <div
              v-for="(item, index) in editForm.sanPhamList"
              :key="index"
              class="flex items-center gap-4 p-4 border rounded-lg hover:shadow-sm transition-shadow"
            >
              <img
                :src="getItemImage(item) || '/placeholder-product.png'"
                :alt="getItemName(item)"
                class="w-14 h-14 object-cover rounded-lg"
              />
              <div class="flex-1 min-w-0">
                <div class="font-medium text-sm mb-1">{{ getItemName(item) }}</div>
                <div class="text-xs text-surface-500 mb-1">{{ getItemCode(item) }}</div>
                <div class="text-xs text-surface-600 mb-2">
                  {{ getVariantInfo(item) }}
                </div>
                <div class="text-sm text-primary font-semibold">{{ formatCurrency(item.donGia) }}</div>
              </div>
              <div class="flex items-center gap-3">
                <div class="flex items-center gap-2 px-3 py-2 bg-gradient-to-r from-primary/10 to-primary/5 border border-primary/20 rounded-lg shadow-sm">
                  <i class="pi pi-barcode text-primary text-lg"></i>
                  <div class="flex flex-col">
                    <span class="text-xs text-surface-500 uppercase tracking-wide font-medium">Serial</span>
                    <span class="text-sm font-bold font-mono text-primary">
                      {{ item.serialNumber || 'N/A' }}
                    </span>
                  </div>
                </div>
              </div>
              <div class="text-right min-w-0">
                <div class="font-semibold text-lg text-primary">{{ formatCurrency(item.thanhTien) }}</div>
              </div>
              <Button
                icon="pi pi-trash"
                text
                rounded
                size="small"
                severity="danger"
                @click="removeItem(index)"
                v-tooltip.top="'Xóa khỏi đơn hàng'"
              />
            </div>
          </div>

          <!-- Empty Cart -->
          <div v-else class="text-center py-8 text-surface-500">
            <i class="pi pi-shopping-cart text-2xl mb-2"></i>
            <p class="text-sm">Chưa có sản phẩm nào trong đơn hàng</p>
            <p class="text-xs">Nhấn "Thêm sản phẩm" để thêm sản phẩm</p>
          </div>
        </div>
      </div>

      <!-- Right Column: Order Summary & Actions -->
      <div class="lg:col-span-1 space-y-6">
        <!-- Delivery Options -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-truck text-primary"></i>
            Giao hàng
          </div>

          <div class="flex items-center justify-between mb-4">
            <label class="font-medium">Giao hàng tận nơi</label>
            <ToggleButton
              v-model="editForm.giaohang"
              onLabel="Có"
              offLabel="Không"
              @change="onDeliveryToggle"
            />
          </div>

          <!-- Delivery Address (when delivery is enabled) -->
          <div v-if="editForm.giaohang" class="space-y-3">
            <div v-if="editForm.khachHang?.diaChis?.length" class="space-y-2">
              <label class="text-sm font-medium">Chọn địa chỉ giao hàng:</label>
              <div
                v-for="address in editForm.khachHang.diaChis"
                :key="address.id"
                class="border rounded-lg p-2 cursor-pointer transition-all"
                :class="{
                  'border-primary bg-primary/5': editForm.diaChiGiaoHang?.id === address.id,
                  'border-surface-200 hover:border-primary/50': editForm.diaChiGiaoHang?.id !== address.id
                }"
                @click="selectDeliveryAddress(address)"
              >
                <div class="text-sm font-medium">{{ address.loaiDiaChi }}</div>
                <div class="text-xs text-surface-500">
                  {{ address.duong }}, {{ address.phuongXa }}, {{ address.quanHuyen }}
                </div>
                <div class="text-xs text-surface-500">{{ address.tinhThanh }}</div>
              </div>
            </div>
            <div v-else-if="editForm.khachHang" class="text-center py-4 text-surface-500">
              <i class="pi pi-map-marker text-lg mb-2"></i>
              <p class="text-xs mb-3">Khách hàng chưa có địa chỉ giao hàng</p>
              <Button
                label="Thêm địa chỉ giao hàng"
                icon="pi pi-plus"
                size="small"
                severity="info"
                outlined
                @click="showFastAddressDialog"
              />
            </div>
            <div v-else class="text-center py-3 text-surface-500">
              <p class="text-xs">Vui lòng chọn khách hàng trước</p>
            </div>
          </div>
        </div>

        <!-- Order Summary -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-calculator text-primary"></i>
            Tổng kết đơn hàng
          </div>

          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span>Tạm tính:</span>
              <span>{{ formatCurrency(editForm.tongTienHang || 0) }}</span>
            </div>
            <div v-if="editForm.giaTriGiamGiaVoucher > 0" class="flex justify-between text-green-600">
              <span>Giảm giá voucher:</span>
              <span>-{{ formatCurrency(editForm.giaTriGiamGiaVoucher) }}</span>
            </div>
            <div v-if="editForm.giaohang" class="flex justify-between">
              <span>Phí giao hàng:</span>
              <span>{{ formatCurrency(editForm.phiVanChuyen || 0) }}</span>
            </div>
            <hr class="my-2">
            <div class="flex justify-between font-semibold text-lg">
              <span>Tổng cộng:</span>
              <span class="text-primary">{{ formatCurrency(editForm.tongThanhToan || 0) }}</span>
            </div>
          </div>

          <!-- Update Order Button -->
          <div class="mt-6 pt-4 border-t border-surface-200">
            <Button
              label="Cập nhật đơn hàng"
              icon="pi pi-check"
              severity="success"
              size="large"
              class="w-full"
              @click="showUpdateConfirmation"
              :loading="updating"
              :disabled="!canUpdateOrder || updating"
            />
            <div v-if="!canUpdateOrder" class="text-center mt-2">
              <small class="text-surface-500">
                <span v-if="!editForm.sanPhamList?.length">Vui lòng thêm sản phẩm vào đơn hàng</span>
                <span v-else-if="editForm.giaohang && (!editForm.khachHang || !editForm.diaChiGiaoHang)">
                  Vui lòng chọn khách hàng và địa chỉ giao hàng
                </span>
              </small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Fast Customer Creation Dialog -->
    <FastCustomerCreate
      v-model:visible="fastCustomerDialogVisible"
      @customer-created="onCustomerCreated"
    />

    <!-- Fast Address Creation Dialog -->
    <FastAddressCreate
      v-model:visible="fastAddressDialogVisible"
      :customer="editForm.khachHang"
      @address-created="onAddressCreated"
    />

    <!-- Product Variant Selection Dialog -->
    <ProductVariantDialog
      ref="productVariantDialogRef"
      v-model:visible="variantDialogVisible"
      @variant-selected="addVariantToOrder"
      @request-cart-sync="syncCartWithDialog"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useOrderStore } from '@/stores/orderStore'
import { useCustomerStore } from '@/stores/customerstore'
import voucherApi from '@/apis/voucherApi'

// PrimeVue Components
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import InputText from 'primevue/inputtext'
import AutoComplete from 'primevue/autocomplete'
import Avatar from 'primevue/avatar'
import ToggleButton from 'primevue/togglebutton'
import Tag from 'primevue/tag'

// Custom Components
import FastCustomerCreate from '@/views/orders/components/FastCustomerCreate.vue'
import FastAddressCreate from '@/views/orders/components/FastAddressCreate.vue'
import ProductVariantDialog from '@/views/orders/components/ProductVariantDialog.vue'

// Composables
const route = useRoute()
const router = useRouter()
const toast = useToast()
const orderStore = useOrderStore()
const customerStore = useCustomerStore()
const confirmDialog = inject('confirmDialog')

// State
const loading = ref(true)
const updating = ref(false)
const error = ref(null)
const orderData = ref(null)
const editForm = ref({})

// Dialog states
const fastCustomerDialogVisible = ref(false)
const fastAddressDialogVisible = ref(false)
const variantDialogVisible = ref(false)
const productVariantDialogRef = ref(null)

// Customer search
const selectedCustomer = ref(null)
const customerSuggestions = ref([])

// Voucher state
const availableVouchers = ref([])
const showAllVouchers = ref(false)
const voucherDisplayLimit = ref(3)

// Computed
const canUpdateOrder = computed(() => {
  if (!editForm.value) return false

  const hasProducts = editForm.value.sanPhamList?.length > 0
  const deliveryValid = !editForm.value.giaohang ||
    (editForm.value.giaohang && editForm.value.khachHang && editForm.value.diaChiGiaoHang)

  return hasProducts && deliveryValid
})

// Computed property for displayed available vouchers
const displayedAvailableVouchers = computed(() => {
  if (showAllVouchers.value) {
    return availableVouchers.value
  }
  return availableVouchers.value.slice(0, voucherDisplayLimit.value)
})

// Methods
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('vi-VN')
}

const calculateVoucherDiscount = (voucher) => {
  if (!voucher || !editForm.value) return 0

  const orderTotal = editForm.value.tongTienHang || 0

  if (voucher.loaiGiamGia === 'PHAN_TRAM') {
    const discount = (orderTotal * voucher.giaTriGiam) / 100
    return voucher.giaTriGiamToiDa ? Math.min(discount, voucher.giaTriGiamToiDa) : discount
  } else {
    return voucher.giaTriGiam || 0
  }
}

const loadOrderData = async () => {
  const orderId = route.params.id
  if (!orderId) {
    error.value = 'ID đơn hàng không hợp lệ'
    loading.value = false
    return
  }

  try {
    loading.value = true
    error.value = null

    const order = await orderStore.fetchOrderById(orderId)
    if (!order) {
      error.value = 'Không tìm thấy đơn hàng'
      return
    }

    // Check if order can be edited
    if (order.trangThaiThanhToan === 'DA_THANH_TOAN') {
      error.value = 'Không thể chỉnh sửa đơn hàng đã thanh toán'
      return
    }

    if (order.trangThaiDonHang !== 'CHO_XAC_NHAN') {
      error.value = 'Chỉ có thể chỉnh sửa đơn hàng ở trạng thái "Chờ xác nhận"'
      return
    }

    orderData.value = order

    // Initialize edit form with order data
    editForm.value = {
      id: order.id,
      maHoaDon: order.maHoaDon,
      loaiHoaDon: order.loaiHoaDon,
      khachHang: order.khachHang,
      diaChiGiaoHang: order.diaChiGiaoHang,
      giaohang: !!order.diaChiGiaoHang,
      sanPhamList: order.chiTiet?.map(item => ({
        id: item.id,
        sanPhamChiTiet: item.sanPhamChiTiet,
        soLuong: item.soLuong,
        donGia: item.donGia,
        thanhTien: item.thanhTien,
        serialNumber: item.serialNumber
      })) || [],
      voucherList: order.voucherList || [],
      tongTienHang: order.tongTienHang || 0,
      giaTriGiamGiaVoucher: order.giaTriGiamGiaVoucher || 0,
      phiVanChuyen: order.phiVanChuyen || 0,
      tongThanhToan: order.tongThanhToan || 0
    }

    // Set selected customer for autocomplete
    if (order.khachHang) {
      selectedCustomer.value = order.khachHang
      // Load available vouchers for the customer
      await loadAvailableVouchers()
    }

  } catch (err) {
    console.error('Error loading order:', err)
    error.value = err.message || 'Không thể tải dữ liệu đơn hàng'
  } finally {
    loading.value = false
  }
}

const calculateTotals = () => {
  if (!editForm.value) return

  console.log('calculateTotals called')
  console.log('sanPhamList:', editForm.value.sanPhamList)

  // Calculate subtotal from products
  const tongTienHang = editForm.value.sanPhamList.reduce((total, item) => {
    const itemTotal = item.donGia * item.soLuong
    console.log(`Item: ${item.sanPhamChiTiet?.sanPham?.tenSanPham}, donGia: ${item.donGia}, soLuong: ${item.soLuong}, itemTotal: ${itemTotal}`)
    return total + itemTotal
  }, 0)

  console.log('Calculated tongTienHang:', tongTienHang)

  // Calculate voucher discount
  const giaTriGiamGiaVoucher = (editForm.value.voucherList || []).reduce((total, voucher) => {
    return total + (voucher.giaTriGiam || 0)
  }, 0)

  console.log('Calculated giaTriGiamGiaVoucher:', giaTriGiamGiaVoucher)

  // Calculate shipping fee (only for delivery orders)
  const phiVanChuyen = editForm.value.giaohang ? 30000 : 0

  console.log('Calculated phiVanChuyen:', phiVanChuyen)

  // Calculate final total
  const tongThanhToan = Math.max(0, tongTienHang - giaTriGiamGiaVoucher + phiVanChuyen)

  console.log('Calculated tongThanhToan:', tongThanhToan)

  // Update form data
  editForm.value.tongTienHang = tongTienHang
  editForm.value.giaTriGiamGiaVoucher = giaTriGiamGiaVoucher
  editForm.value.phiVanChuyen = phiVanChuyen
  editForm.value.tongThanhToan = tongThanhToan

  console.log('Updated editForm totals:', {
    tongTienHang: editForm.value.tongTienHang,
    giaTriGiamGiaVoucher: editForm.value.giaTriGiamGiaVoucher,
    phiVanChuyen: editForm.value.phiVanChuyen,
    tongThanhToan: editForm.value.tongThanhToan
  })
}

// Customer methods
const searchCustomers = async (event) => {
  try {
    console.log('Searching customers with query:', event.query)

    // Try backend search first
    try {
      const customers = await customerStore.fetchCustomers({ search: event.query })
      console.log('Customer search results from backend:', customers)
      customerSuggestions.value = customers
      console.log('Updated customerSuggestions:', customerSuggestions.value)
      return
    } catch (backendError) {
      console.warn('Backend search failed, falling back to frontend filtering:', backendError)
    }

    // Fallback: Load all customers and filter on frontend
    const allCustomers = await customerStore.fetchCustomers()
    console.log('All customers loaded:', allCustomers)

    if (!event.query || event.query.trim() === '') {
      customerSuggestions.value = allCustomers.slice(0, 10) // Limit to first 10
      return
    }

    const query = event.query.toLowerCase().trim()
    const filteredCustomers = allCustomers.filter(customer => {
      return (
        customer.hoTen?.toLowerCase().includes(query) ||
        customer.soDienThoai?.includes(query) ||
        customer.email?.toLowerCase().includes(query) ||
        customer.maNguoiDung?.toLowerCase().includes(query)
      )
    }).slice(0, 10) // Limit to first 10 results

    console.log('Filtered customers:', filteredCustomers)
    customerSuggestions.value = filteredCustomers

  } catch (error) {
    console.error('Error searching customers:', error)
    customerSuggestions.value = []
  }
}

const getCustomerDisplayLabel = (customer) => {
  return `${customer.hoTen} - ${customer.soDienThoai}`
}

const onCustomerSelect = async (event) => {
  try {
    console.log('Customer selected from search:', event.value)

    // Fetch complete customer data with addresses to ensure we have all necessary information
    const customerWithAddresses = await customerStore.fetchCustomerById(event.value.id)
    console.log('Customer data with addresses loaded:', customerWithAddresses)

    editForm.value.khachHang = customerWithAddresses
    editForm.value.diaChiGiaoHang = null // Reset delivery address when customer changes
    selectedCustomer.value = customerWithAddresses

    // Load available vouchers for the selected customer
    await loadAvailableVouchers()
  } catch (error) {
    console.error('Error loading customer details:', error)
    // Fallback to the basic customer data from search
    console.log('Using fallback customer data:', event.value)
    editForm.value.khachHang = event.value
    editForm.value.diaChiGiaoHang = null // Reset delivery address when customer changes
    selectedCustomer.value = event.value
    await loadAvailableVouchers()
  }
}

const clearCustomer = () => {
  editForm.value.khachHang = null
  editForm.value.diaChiGiaoHang = null
  selectedCustomer.value = null
}

const onCustomerCreated = (customer) => {
  editForm.value.khachHang = customer
  selectedCustomer.value = customer
  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Đã thêm khách hàng mới',
    life: 3000
  })
}

// Voucher methods
const loadAvailableVouchers = async () => {
  if (!editForm.value) return

  try {
    const customerId = editForm.value.khachHang?.id || null
    const orderTotal = editForm.value.tongTienHang || 0

    const response = await voucherApi.getAvailableVouchers(customerId, orderTotal)

    if (response.success) {
      // Filter out already applied vouchers
      const appliedVoucherCodes = (editForm.value.voucherList || []).map(v => v.maPhieuGiamGia)
      availableVouchers.value = response.data.filter(
        voucher => !appliedVoucherCodes.includes(voucher.maPhieuGiamGia)
      )
    } else {
      availableVouchers.value = []
    }
  } catch (error) {
    console.error('Error loading available vouchers:', error)
    availableVouchers.value = []
  }
}

const selectVoucher = async (voucher) => {
  if (!editForm.value) return

  try {
    const customerId = editForm.value.khachHang?.id || null
    const orderTotal = editForm.value.tongTienHang || 0

    const response = await voucherApi.validateVoucher(voucher.maPhieuGiamGia, customerId, orderTotal)

    if (response.success && response.data.valid) {
      // Check if voucher is already applied
      const existingVoucher = (editForm.value.voucherList || []).find(
        v => v.maPhieuGiamGia === voucher.maPhieuGiamGia
      )

      if (existingVoucher) {
        toast.add({
          severity: 'warn',
          summary: 'Cảnh báo',
          detail: 'Voucher này đã được áp dụng',
          life: 3000
        })
        return
      }

      // Add voucher to order with validated discount amount
      const voucherData = {
        ...response.data.voucher,
        giaTriGiam: response.data.discountAmount
      }

      if (!editForm.value.voucherList) {
        editForm.value.voucherList = []
      }
      editForm.value.voucherList.push(voucherData)
      calculateTotals()

      // Remove from available vouchers list
      availableVouchers.value = availableVouchers.value.filter(
        v => v.maPhieuGiamGia !== voucher.maPhieuGiamGia
      )

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `Áp dụng voucher ${voucher.maPhieuGiamGia} thành công`,
        life: 3000
      })
    } else {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: response.data.error || 'Voucher không hợp lệ',
        life: 3000
      })
    }
  } catch (error) {
    console.error('Error applying voucher:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể áp dụng voucher. Vui lòng thử lại.',
      life: 3000
    })
  }
}

const removeVoucher = async (index) => {
  if (!editForm.value || !editForm.value.voucherList) return

  const removedVoucher = editForm.value.voucherList[index]

  // Remove voucher directly
  editForm.value.voucherList.splice(index, 1)
  calculateTotals()

  // Reload available vouchers to include the removed voucher
  await loadAvailableVouchers()

  toast.add({
    severity: 'info',
    summary: 'Thông báo',
    detail: `Đã gỡ voucher ${removedVoucher.maPhieuGiamGia}`,
    life: 3000
  })
}

const toggleVoucherDisplay = () => {
  showAllVouchers.value = !showAllVouchers.value
}

// Delivery methods
const onDeliveryToggle = () => {
  if (!editForm.value.giaohang) {
    editForm.value.diaChiGiaoHang = null
  }
  calculateTotals()
}

const selectDeliveryAddress = (address) => {
  editForm.value.diaChiGiaoHang = address
}

const onAddressCreated = (address) => {
  // Refresh customer data to include new address
  if (editForm.value.khachHang) {
    editForm.value.khachHang.diaChis = editForm.value.khachHang.diaChis || []
    editForm.value.khachHang.diaChis.push(address)
    editForm.value.diaChiGiaoHang = address
  }
  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Đã thêm địa chỉ giao hàng mới',
    life: 3000
  })
}

// Dialog methods
const showFastCustomerDialog = () => {
  fastCustomerDialogVisible.value = true
}

const showFastAddressDialog = () => {
  fastAddressDialogVisible.value = true
}

const showProductSelectionDialog = () => {
  variantDialogVisible.value = true
}

// Sync cart data with product variant dialog
const syncCartWithDialog = () => {
  if (productVariantDialogRef.value && editForm.value?.sanPhamList) {
    // Convert editForm sanPhamList to the format expected by ProductVariantDialog
    const cartItems = editForm.value.sanPhamList.map(item => ({
      sanPhamChiTiet: item.sanPhamChiTiet,
      soLuong: item.soLuong,
      serialNumber: item.serialNumber || item.sanPhamChiTiet?.serialNumber
    }))

    console.log('Syncing cart with dialog:', cartItems)
    productVariantDialogRef.value.updateUsedSerialNumbers(cartItems)
  }
}

// Product methods
const getItemImage = (item) => {
  // Simplified image handling for edit mode
  return item.sanPhamChiTiet?.hinhAnh?.[0] || item.sanPhamChiTiet?.sanPham?.hinhAnh?.[0]
}

const getItemName = (item) => {
  return item.sanPhamChiTiet?.sanPham?.tenSanPham || 'Sản phẩm'
}

const getItemCode = (item) => {
  return item.sanPhamChiTiet?.sanPham?.maSanPham || item.sanPhamChiTiet?.maSanPhamChiTiet || ''
}

const getVariantInfo = (item) => {
  if (!item.sanPhamChiTiet) return ''

  const parts = []
  if (item.sanPhamChiTiet.cpu) parts.push(item.sanPhamChiTiet.cpu.moTaCpu)
  if (item.sanPhamChiTiet.ram) parts.push(item.sanPhamChiTiet.ram.moTaRam)
  if (item.sanPhamChiTiet.gpu) parts.push(item.sanPhamChiTiet.gpu.moTaGpu)
  if (item.sanPhamChiTiet.mauSac) parts.push(item.sanPhamChiTiet.mauSac.moTaMauSac)

  const storage = item.sanPhamChiTiet.boNho || item.sanPhamChiTiet.bonho || item.sanPhamChiTiet.oCung || item.sanPhamChiTiet.ocung
  if (storage) parts.push(storage.moTaBoNho || storage.moTaOCung)

  if (item.sanPhamChiTiet.manHinh) parts.push(item.sanPhamChiTiet.manHinh.moTaManHinh)

  return parts.join(' • ')
}

const addVariantToOrder = (variantData) => {
  console.log('addVariantToOrder called with:', variantData)
  console.log('Current editForm.value:', editForm.value)
  console.log('Current sanPhamList:', editForm.value?.sanPhamList)

  if (!editForm.value) {
    console.error('editForm.value is null or undefined')
    return
  }

  const { sanPhamChiTiet, soLuong, donGia, thanhTien, groupInfo } = variantData

  // Enhanced duplicate check matching OrderCreate.vue logic
  const existingIndex = editForm.value.sanPhamList.findIndex(item => {
    if (item.sanPhamChiTiet?.id !== sanPhamChiTiet.id) {
      return false
    }

    // If both items have serial numbers, compare them
    if (sanPhamChiTiet.serialNumber && item.sanPhamChiTiet?.serialNumber) {
      return item.sanPhamChiTiet.serialNumber === sanPhamChiTiet.serialNumber
    }

    // If both items have serial number IDs, compare them
    if (sanPhamChiTiet.serialNumberId && item.sanPhamChiTiet?.serialNumberId) {
      return item.sanPhamChiTiet.serialNumberId === sanPhamChiTiet.serialNumberId
    }

    // If neither has serial numbers, then it's a duplicate variant
    if (!sanPhamChiTiet.serialNumber && !sanPhamChiTiet.serialNumberId &&
        !item.sanPhamChiTiet?.serialNumber && !item.sanPhamChiTiet?.serialNumberId) {
      return true
    }

    // Different serial numbers or one has serial and other doesn't = not duplicate
    return false
  })

  if (existingIndex !== -1) {
    const serialInfo = sanPhamChiTiet.serialNumber ? ` (Serial: ${sanPhamChiTiet.serialNumber})` : ''
    console.log('Variant already exists at index:', existingIndex)
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: `Phiên bản này${serialInfo} đã có trong đơn hàng`,
      life: 3000
    })
    return
  }

  // Create new item with complete data structure matching OrderCreate.vue
  const newItem = {
    sanPhamChiTiet: {
      ...sanPhamChiTiet,
      // Ensure serial number data is properly included
      serialNumber: sanPhamChiTiet.serialNumber,
      serialNumberId: sanPhamChiTiet.serialNumberId
    },
    soLuong,
    donGia,
    thanhTien,
    // Store serial number at item level for easy access
    serialNumber: sanPhamChiTiet.serialNumber,
    groupInfo: groupInfo // Store group info for display purposes
  }

  console.log('Adding new item:', newItem)
  editForm.value.sanPhamList.push(newItem)
  console.log('Updated sanPhamList:', editForm.value.sanPhamList)

  calculateTotals()
  console.log('After calculateTotals - tongTienHang:', editForm.value.tongTienHang)

  // Sync with product variant dialog to update stock counts
  syncCartWithDialog()

  // Don't show individual success messages when adding from groups
  if (!groupInfo?.isFromGroup) {
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã thêm sản phẩm vào đơn hàng',
      life: 3000
    })
  }
}

const removeItem = (index) => {
  editForm.value.sanPhamList.splice(index, 1)
  calculateTotals()

  // Sync with product variant dialog to update stock counts
  syncCartWithDialog()
}

const showUpdateConfirmation = async () => {
  if (!canUpdateOrder.value) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng hoàn tất thông tin đơn hàng trước khi cập nhật',
      life: 3000
    })
    return
  }

  // Show detailed confirmation dialog using specialized template
  const confirmed = await confirmDialog.showOrderUpdateConfirm(editForm.value)

  if (!confirmed) return

  updateOrder()
}

const updateOrder = async () => {
  try {
    updating.value = true

    console.log('updateOrder called')
    console.log('editForm.value:', editForm.value)
    console.log('sanPhamList:', editForm.value.sanPhamList)

    // Validate that we have products to update
    if (!editForm.value.sanPhamList || editForm.value.sanPhamList.length === 0) {
      toast.add({
        severity: 'warn',
        summary: 'Cảnh báo',
        detail: 'Đơn hàng phải có ít nhất một sản phẩm',
        life: 3000
      })
      return
    }

    const updateData = {
      khachHangId: editForm.value.khachHang?.id || null,
      diaChiGiaoHangId: editForm.value.diaChiGiaoHang?.id || null,
      nguoiNhanTen: editForm.value.khachHang?.hoTen || null,
      nguoiNhanSdt: editForm.value.khachHang?.soDienThoai || null,
      chiTiet: editForm.value.sanPhamList.map(item => {
        const chiTietItem = {
          sanPhamChiTietId: item.sanPhamChiTiet?.id,
          soLuong: item.soLuong,
          donGia: item.donGia,
          thanhTien: item.thanhTien,
          // Ensure serial number data is properly mapped
          serialNumberId: item.sanPhamChiTiet?.serialNumberId || item.serialNumberId,
          serialNumber: item.sanPhamChiTiet?.serialNumber || item.serialNumber
        }

        // Only include ID for existing items (items that were already in the order)
        if (item.id) {
          chiTietItem.id = item.id
        }

        console.log('Mapping chiTiet item:', chiTietItem)
        console.log('Source item:', item)
        return chiTietItem
      }),
      tongTienHang: editForm.value.tongTienHang,
      giaTriGiamGiaVoucher: editForm.value.giaTriGiamGiaVoucher,
      phiVanChuyen: editForm.value.phiVanChuyen,
      tongThanhToan: editForm.value.tongThanhToan,
      voucherCodes: (editForm.value.voucherList || []).map(voucher => voucher.maPhieuGiamGia)
    }

    console.log('updateData being sent to API:', updateData)
    console.log('Number of chiTiet items:', updateData.chiTiet.length)
    console.log('Total amount being sent:', updateData.tongThanhToan)

    const result = await orderStore.updateOrder(editForm.value.id, updateData)

    if (result) {
      console.log('Update result received:', result)
      console.log('Result chiTiet length:', result.chiTiet?.length || 0)
      console.log('Result tongThanhToan:', result.tongThanhToan)

      // Check if the backend properly processed the update
      const hasValidProducts = result.chiTiet && result.chiTiet.length > 0
      const hasValidTotals = result.tongThanhToan > 0 || updateData.tongThanhToan === 0

      if (hasValidProducts && hasValidTotals) {
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Đã cập nhật đơn hàng thành công',
          life: 3000
        })

        // Redirect back to order detail
        router.push(`/orders/${editForm.value.id}`)
      } else if (!hasValidProducts) {
        console.error('Backend did not process chiTiet correctly')
        console.error('Expected chiTiet items:', updateData.chiTiet.length)
        console.error('Received chiTiet items:', result.chiTiet?.length || 0)

        toast.add({
          severity: 'error',
          summary: 'Lỗi Backend',
          detail: 'Backend không xử lý đúng thông tin sản phẩm. Vui lòng kiểm tra lại hoặc liên hệ hỗ trợ.',
          life: 7000
        })
      } else {
        console.warn('Update succeeded but result validation failed:', {
          hasValidProducts,
          hasValidTotals,
          resultTotals: result.tongThanhToan,
          expectedTotals: updateData.tongThanhToan
        })

        toast.add({
          severity: 'warn',
          summary: 'Cảnh báo',
          detail: 'Đơn hàng đã được cập nhật nhưng có thể có vấn đề với dữ liệu. Vui lòng kiểm tra lại.',
          life: 5000
        })

        // Still redirect but with warning
        router.push(`/orders/${editForm.value.id}`)
      }
    } else {
      throw new Error('No result returned from update operation')
    }
  } catch (error) {
    console.error('Error updating order:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `Không thể cập nhật đơn hàng: ${error.message || 'Vui lòng thử lại.'}`,
      life: 5000
    })
  } finally {
    updating.value = false
  }
}

// Watch for order total changes to reload vouchers
watch(
  () => editForm.value?.tongTienHang,
  async (newTotal, oldTotal) => {
    if (editForm.value && newTotal !== oldTotal && editForm.value.khachHang) {
      // Reload available vouchers when order total changes
      await loadAvailableVouchers()
    }
  }
)

// Initialize
onMounted(async () => {
  await loadOrderData()

  // Preload customer data
  try {
    await customerStore.fetchCustomers()
  } catch (error) {
    console.error('Failed to preload customer data:', error)
  }
})
</script>
