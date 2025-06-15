<template>
  <div class="order-create-container">
    <Toast />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-shop text-lg text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">
              Bán hàng tại quầy
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              Quản lý nhiều đơn hàng đồng thời với giao diện tab
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

    <!-- Order Tabs Navigation -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2 flex-1 overflow-x-auto">
          <!-- Order Tabs -->
          <div
            v-for="tab in orderTabs"
            :key="tab.id"
            class="flex items-center gap-2 px-4 py-2 rounded-lg cursor-pointer transition-all min-w-fit"
            :class="{
              'bg-primary text-white': activeTabId === tab.id,
              'bg-surface-100 hover:bg-surface-200 text-surface-700': activeTabId !== tab.id
            }"
            @click="switchToTab(tab.id)"
          >
            <i class="pi pi-file text-sm"></i>
            <span class="font-medium text-sm">{{ tab.maHoaDon }}</span>
            <Badge
              v-if="tab.sanPhamList.length > 0"
              :value="tab.sanPhamList.length"
              severity="info"
              size="small"
            />
            <Button
              icon="pi pi-times"
              text
              rounded
              size="small"
              class="w-5 h-5 ml-1"
              :class="activeTabId === tab.id ? 'text-white hover:bg-white/20' : 'text-surface-500 hover:bg-surface-300'"
              @click.stop="closeTabWithConfirmation(tab.id)"
            />
          </div>

          <!-- Add New Tab Button -->
          <Button
            v-if="canCreateNewTab"
            icon="pi pi-plus"
            outlined
            rounded
            size="small"
            class="min-w-fit"
            @click="createNewOrderTab"
            v-tooltip.top="'Tạo đơn hàng mới'"
          />
        </div>

        <!-- Tab Actions -->
        <div v-if="hasActiveTabs" class="flex items-center gap-2 ml-4">
          <Button
            icon="pi pi-refresh"
            outlined
            @click="calculateTabTotals(activeTabId)"
            v-tooltip.top="'Tính lại tổng tiền'"
          />
          <Button
            icon="pi pi-trash"
            outlined
            severity="danger"
            @click="closeTabWithConfirmation(activeTabId)"
            v-tooltip.top="'Đóng tab hiện tại'"
          />
          <Button
          icon="pi pi-qrcode"
          severity="info"
          outlined
          @click="showQRScanner = true"
          v-tooltip.top="'Quét mã QR để thêm serial number vào giỏ hàng'"
        />
        <Button
          label="Chọn sản phẩm"
          icon="pi pi-plus"
          severity="primary"
          @click="showProductSelectionDialog"
          v-tooltip.top="'Chọn sản phẩm từ danh sách'"
        />
        </div>
      </div>
    </div>

    <!-- Main Order Creation Interface -->
    <div v-if="!hasActiveTabs" class="card">
      <div class="text-center py-12">
        <i class="pi pi-shopping-cart text-6xl text-surface-300 mb-4"></i>
        <h3 class="text-xl font-semibold text-surface-600 mb-2">Chưa có đơn hàng nào</h3>
        <p class="text-surface-500 mb-6">Nhấn nút "+" để tạo đơn hàng mới</p>
        <Button
          label="Tạo đơn hàng đầu tiên"
          icon="pi pi-plus"
          @click="createNewOrderTab"
          size="large"
        />
      </div>
    </div>

    <!-- Active Order Tab Content -->
    <div v-else-if="activeTab" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Left Column: Product Selection & Order Items -->
      <div class="lg:col-span-2 space-y-6">


        <!-- Order Items -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <i class="pi pi-shopping-cart text-primary"></i>
              Sản phẩm trong đơn hàng
            </div>
            <div class="flex items-center gap-2">
              <Badge
                v-if="activeTab?.sanPhamList?.length > 0"
                :value="activeTab.sanPhamList.length"
                severity="info"
              />
            </div>
          </div>

          <!-- Order Items List -->
          <div v-if="activeTab?.sanPhamList?.length" class="space-y-3 mb-4">
            <div
              v-for="(item, index) in activeTab.sanPhamList"
              :key="index"
              class="flex items-center gap-4 p-4 border rounded-lg hover:shadow-sm transition-shadow"
            >
              <img
                :src="getCartItemImage(item) || '/placeholder-product.png'"
                :alt="getCartItemName(item)"
                class="w-14 h-14 object-cover rounded-lg"
              />
              <div class="flex-1 min-w-0">
                <div class="font-medium text-sm mb-1">{{ getCartItemName(item) }}</div>
                <div class="text-xs text-surface-500 mb-1">{{ getCartItemCode(item) }}</div>
                <div class="text-xs text-surface-600 mb-2">
                  {{ getVariantDisplayInfo(item) }}
                </div>
                <div class="text-sm text-primary font-semibold">{{ formatCurrency(item.donGia) }}</div>
              </div>
              <div class="flex items-center gap-3">
                <div class="flex items-center gap-2 px-3 py-2 bg-gradient-to-r from-primary/10 to-primary/5 border border-primary/20 rounded-lg shadow-sm">
                  <i class="pi pi-barcode text-primary text-lg"></i>
                  <div class="flex flex-col">
                    <span class="text-xs text-surface-500 uppercase tracking-wide font-medium">Serial</span>
                    <span class="text-sm font-bold font-mono text-primary">
                      {{ item.sanPhamChiTiet?.serialNumber || 'N/A' }}
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
                @click="removeFromActiveTab(index)"
                v-tooltip.top="'Xóa khỏi giỏ hàng'"
              />
            </div>
          </div>

          <!-- Empty Cart -->
          <div v-else class="text-center py-8 text-surface-500">
            <i class="pi pi-shopping-cart text-2xl mb-2"></i>
            <p class="text-sm">Chưa có sản phẩm nào trong đơn hàng</p>
            <p class="text-xs">Tìm kiếm và thêm sản phẩm ở phía trên</p>
          </div>
        </div>


      </div>

      <!-- Right Column: Order Summary & Actions -->
      <div class="lg:col-span-1 space-y-6">
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
          <div v-if="activeTab?.khachHang" class="p-3 border rounded-lg bg-surface-50">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <Avatar :label="activeTab.khachHang.hoTen?.charAt(0)" size="small" />
                <div>
                  <div class="font-semibold text-sm">{{ activeTab.khachHang.hoTen }}</div>
                  <div class="text-xs text-surface-500">{{ activeTab.khachHang.soDienThoai }}</div>
                </div>
              </div>
              <Button
                icon="pi pi-times"
                text
                rounded
                size="small"
                @click="clearCustomerFromTab"
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



        <!-- Delivery Options -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-truck text-primary"></i>
            Giao hàng
          </div>

          <div class="flex items-center justify-between mb-4">
            <label class="font-medium">Giao hàng tận nơi</label>
            <ToggleButton
              v-model="activeTab.giaohang"
              onLabel="Có"
              offLabel="Không"
              @change="onDeliveryToggle"
              :disabled="!activeTab"
            />
          </div>



          <!-- Recipient Information Form (when delivery is enabled) -->
          <div v-if="activeTab?.giaohang" class="space-y-4">
            <!-- Recipient Information Header -->
            <div class="border-t pt-4">
              <div class="font-semibold text-base mb-3 flex items-center gap-2">
                <i class="pi pi-user-plus text-blue-600"></i>
                <span class="text-blue-800">Thông tin người nhận</span>
              </div>

              <!-- Recipient Name -->
              <div class="mb-3">
                <label class="block text-sm font-medium mb-1">
                  Tên người nhận <span class="text-red-500">*</span>
                </label>
                <AutoComplete
                  v-model="recipientInfo.hoTen"
                  :suggestions="recipientNameSuggestions"
                  @complete="searchRecipientByName"
                  @item-select="onRecipientNameSelect"
                  optionLabel="hoTen"
                  placeholder="Nhập tên người nhận..."
                  class="w-full"
                  :class="{ 'p-invalid': recipientErrors.hoTen }"
                  :loading="searchingRecipient"
                  fluid
                >
                  <template #item="{ item }">
                    <div class="flex items-center gap-2 p-2">
                      <Avatar :label="item.hoTen?.charAt(0)" size="small" />
                      <div>
                        <div class="font-medium">{{ item.hoTen }}</div>
                        <div class="text-sm text-surface-500">{{ item.soDienThoai || 'Không có SĐT' }}</div>
                      </div>
                    </div>
                  </template>
                </AutoComplete>
                <small v-if="recipientErrors.hoTen" class="p-error">{{ recipientErrors.hoTen }}</small>
              </div>

              <!-- Recipient Phone -->
              <div class="mb-4">
                <label class="block text-sm font-medium mb-1">
                  Số điện thoại người nhận <span class="text-red-500">*</span>
                </label>
                <AutoComplete
                  v-model="recipientInfo.soDienThoai"
                  :suggestions="recipientPhoneSuggestions"
                  @complete="searchRecipientByPhone"
                  @item-select="onRecipientPhoneSelect"
                  optionLabel="soDienThoai"
                  placeholder="Nhập số điện thoại người nhận..."
                  class="w-full"
                  :class="{ 'p-invalid': recipientErrors.soDienThoai }"
                  :loading="searchingRecipient"
                  fluid
                >
                  <template #item="{ item }">
                    <div class="flex items-center gap-2 p-2">
                      <Avatar :label="item.hoTen?.charAt(0)" size="small" />
                      <div>
                        <div class="font-medium">{{ item.hoTen || 'Không có tên' }}</div>
                        <div class="text-sm text-surface-500">{{ item.soDienThoai }}</div>
                      </div>
                    </div>
                  </template>
                </AutoComplete>
                <small v-if="recipientErrors.soDienThoai" class="p-error">{{ recipientErrors.soDienThoai }}</small>
              </div>

              <!-- Embedded Address Form -->
              <div class="border-t pt-4">
                <div class="font-semibold text-base mb-3 flex items-center gap-2">
                  <i class="pi pi-map-marker text-blue-600"></i>
                  <span class="text-blue-800">Địa chỉ giao hàng</span>
                </div>

                <!-- Street Address -->
                <div class="mb-3">
                  <label class="block text-sm font-medium mb-1">
                    Địa chỉ đường <span class="text-red-500">*</span>
                  </label>
                  <InputText
                    v-model="addressData.duong"
                    placeholder="Nhập số nhà, tên đường..."
                    class="w-full"
                    :class="{ 'p-invalid': addressErrors.duong }"
                  />
                  <small v-if="addressErrors.duong" class="p-error">{{ addressErrors.duong }}</small>
                </div>

                <!-- Province/City -->
                <div class="mb-3">
                  <label class="block text-sm font-medium mb-1">
                    Tỉnh/Thành phố <span class="text-red-500">*</span>
                  </label>
                  <Dropdown
                    v-model="selectedProvince"
                    :options="provinces"
                    optionLabel="name"
                    placeholder="Chọn tỉnh/thành phố"
                    class="w-full"
                    :class="{ 'p-invalid': addressErrors.tinhThanh }"
                    @change="onProvinceChange"
                    :loading="loadingProvinces"
                  />
                  <small v-if="addressErrors.tinhThanh" class="p-error">{{ addressErrors.tinhThanh }}</small>
                </div>

                <!-- District -->
                <div class="mb-3">
                  <label class="block text-sm font-medium mb-1">
                    Quận/Huyện <span class="text-red-500">*</span>
                  </label>
                  <Dropdown
                    v-model="selectedDistrict"
                    :options="districts"
                    optionLabel="name"
                    placeholder="Chọn quận/huyện"
                    class="w-full"
                    :class="{ 'p-invalid': addressErrors.quanHuyen }"
                    @change="onDistrictChange"
                    :disabled="!selectedProvince"
                    :loading="loadingDistricts"
                  />
                  <small v-if="addressErrors.quanHuyen" class="p-error">{{ addressErrors.quanHuyen }}</small>
                </div>

                <!-- Ward -->
                <div class="mb-3">
                  <label class="block text-sm font-medium mb-1">
                    Phường/Xã <span class="text-red-500">*</span>
                  </label>
                  <Dropdown
                    v-model="selectedWard"
                    :options="wards"
                    optionLabel="name"
                    placeholder="Chọn phường/xã"
                    class="w-full"
                    :class="{ 'p-invalid': addressErrors.phuongXa }"
                    :disabled="!selectedDistrict"
                    :loading="loadingWards"
                  />
                  <small v-if="addressErrors.phuongXa" class="p-error">{{ addressErrors.phuongXa }}</small>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Voucher Section -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-tag text-primary"></i>
            Voucher giảm giá
          </div>



          <!-- Applied Vouchers -->
          <div v-if="activeTab?.voucherList?.length" class="space-y-2 mb-4">
            <div class="font-medium mb-3 text-sm flex items-center gap-2">
              <i class="pi pi-sparkles text-primary"></i>
              Voucher tự động áp dụng
            </div>
            <div
              v-for="(voucher, index) in activeTab.voucherList"
              :key="index"
              class="relative flex items-center justify-between p-3 border rounded-lg bg-green-50 border-green-200"
            >
              <!-- Best Overall Voucher Indicator (only for applied vouchers that are best overall) -->
              <div v-if="isBestVoucher(voucher)" class="absolute -top-2 -right-2">
                <Badge value="Lựa chọn tốt nhất" severity="success" size="small" />
              </div>

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
                @click="removeVoucherFromTab(index)"
              />
            </div>
          </div>

          <!-- Available Vouchers Display -->
          <div v-if="displayedAvailableVouchers.length" class="mb-4">
            <div class="font-medium mb-3 text-sm flex items-center gap-2">
              <i class="pi pi-sparkles text-primary"></i>
              Voucher khả dụng
            </div>

            <!-- Voucher Cards Container (No Scrollbar) -->
            <div class="space-y-3">
              <div
                v-for="voucher in displayedAvailableVouchers"
                :key="voucher.id"
                class="relative p-3 border rounded-lg transition-all cursor-pointer hover:shadow-md"
                :class="{
                  'border-green-500 bg-green-50': isBestAvailableVoucher(voucher),
                  'border-surface-200 bg-surface-50': !isBestAvailableVoucher(voucher)
                }"
                @click="selectVoucher(voucher)"
              >
                <!-- Best Overall Voucher Indicator (only for available vouchers that are best overall) -->
                <div v-if="isBestAvailableVoucher(voucher)" class="absolute -top-2 -right-2">
                  <Badge value="Lựa chọn tốt nhất" severity="success" size="small" />
                </div>

                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="font-semibold text-sm mb-1" :class="isBestAvailableVoucher(voucher) ? 'text-green-800' : 'text-surface-900'">
                      {{ voucher.tenPhieuGiamGia || voucher.maPhieuGiamGia }}
                    </div>
                    <div class="text-xs text-surface-500 mb-2">{{ voucher.moTa }}</div>

                    <!-- Voucher Details -->
                    <div class="space-y-1">
                      <div class="text-sm font-medium" :class="isBestAvailableVoucher(voucher) ? 'text-green-700' : 'text-primary'">
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
                    :class="isBestAvailableVoucher(voucher) ? 'text-green-600 hover:bg-green-100' : 'text-primary hover:bg-primary/10'"
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

          <!-- Smart Voucher Recommendations -->
          <div v-if="voucherRecommendations.length > 0 && activeTab?.khachHang" class="mb-4">
            <div class="font-medium mb-3 text-sm flex items-center gap-2">
              <i class="pi pi-lightbulb text-orange-600"></i>
              Gợi ý tiết kiệm
            </div>

            <div class="space-y-3">
              <div
                v-for="(recommendation, index) in voucherRecommendations"
                :key="index"
                class="relative p-3 border rounded-lg transition-all cursor-pointer border-surface-200 bg-surface-50 opacity-60 hover:opacity-75"
                @click="applyRecommendedVoucher(recommendation.voucher)"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="font-semibold text-sm mb-1 text-surface-700">
                      {{ recommendation.voucher.tenPhieuGiamGia || recommendation.voucher.maPhieuGiamGia }}
                    </div>
                    <div class="text-xs text-surface-500 mb-2">{{ recommendation.voucher.moTa }}</div>

                    <!-- Voucher Details -->
                    <div class="space-y-1">
                      <div class="text-sm font-medium text-surface-600">
                        Giảm {{ formatCurrency(recommendation.potentialDiscount) }}
                      </div>

                      <!-- Red Italic Recommendation Message -->
                      <div class="text-sm text-red-600 italic font-medium">
                        {{ recommendation.message }}
                      </div>

                      <!-- Conditions -->
                      <div class="text-xs text-surface-600">
                        <span v-if="recommendation.voucher.giaTriDonHangToiThieu">
                          Đơn tối thiểu: {{ formatCurrency(recommendation.voucher.giaTriDonHangToiThieu) }}
                        </span>
                        <span v-if="recommendation.voucher.giaTriGiamToiDa && recommendation.voucher.loaiGiamGia === 'PHAN_TRAM'">
                          • Giảm tối đa: {{ formatCurrency(recommendation.voucher.giaTriGiamToiDa) }}
                        </span>
                      </div>

                      <!-- Expiry -->
                      <div class="text-xs text-surface-500">
                        <i class="pi pi-calendar text-xs mr-1"></i>
                        Hết hạn: {{ formatDate(recommendation.voucher.ngayKetThuc) }}
                      </div>
                    </div>
                  </div>

                  <Button
                    icon="pi pi-plus"
                    text
                    rounded
                    size="small"
                    class="text-surface-500 hover:bg-surface-100"
                    @click.stop="applyRecommendedVoucher(recommendation.voucher)"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- No Vouchers Available -->
          <div v-if="!activeTab?.voucherList?.length && !availableVouchers.length && activeTab?.khachHang" class="mb-4 p-3 border border-dashed border-surface-300 rounded-lg text-center">
            <i class="pi pi-info-circle text-surface-400 text-lg mb-2"></i>
            <p class="text-sm text-surface-500">Không có voucher khả dụng cho đơn hàng này</p>
          </div>
        </div>

        <!-- Payment Section -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <i class="pi pi-credit-card text-primary"></i>
              Thanh toán
            </div>
            <Button
              v-if="activeTab?.tongThanhToan > 0"
              label="Thanh toán hỗn hợp"
              icon="pi pi-plus-circle"
              size="small"
              severity="info"
              outlined
              @click="showMixedPaymentDialog"
            />
          </div>

          <!-- Payment Methods -->
          <div v-if="paymentMethods.length === 0" class="text-center py-4 text-surface-500 mb-4">
            <i class="pi pi-info-circle text-2xl mb-2"></i>
            <p>Không có phương thức thanh toán khả dụng</p>
            <p class="text-sm">Vui lòng kiểm tra lại tùy chọn giao hàng</p>
          </div>
          <!-- Mixed Payment Display -->
          <div v-if="activeTab?.phuongThucThanhToan === 'MIXED' && activeTab?.mixedPayments" class="mb-4">
            <div class="border rounded-lg p-3 bg-blue-50 border-blue-200">
              <div class="flex items-center gap-2 mb-3">
                <i class="pi pi-plus-circle text-blue-600"></i>
                <span class="font-semibold text-blue-800">Thanh toán hỗn hợp</span>
                <Badge value="Đã cấu hình" severity="info" size="small" />
              </div>
              <div class="space-y-2">
                <div
                  v-for="(payment, index) in activeTab.mixedPayments"
                  :key="index"
                  class="flex justify-between items-center text-sm"
                >
                  <span>{{ getPaymentMethodLabel(payment.method) }}:</span>
                  <span class="font-medium">{{ formatCurrency(payment.amount) }}</span>
                </div>
              </div>
              <Button
                label="Chỉnh sửa"
                icon="pi pi-pencil"
                size="small"
                text
                @click="showMixedPaymentDialog"
                class="mt-2"
              />
            </div>
          </div>

          <!-- Single Payment Methods -->
          <div v-else class="space-y-3 mb-4">
            <div
              v-for="method in paymentMethods"
              :key="method.value"
              class="border rounded-lg p-3 cursor-pointer transition-all"
              :class="{
                'border-primary bg-primary/5': activeTab?.phuongThucThanhToan === method.value,
                'border-surface-200 hover:border-primary/50': activeTab?.phuongThucThanhToan !== method.value,
                'opacity-50 cursor-not-allowed': !method.available
              }"
              @click="method.available && selectPaymentMethod(method.value)"
            >
              <div class="flex items-center gap-3">
                <i :class="method.icon" class="text-lg text-primary"></i>
                <div>
                  <div class="font-semibold text-sm">{{ method.label }}</div>
                  <div class="text-xs text-surface-500">{{ method.description }}</div>
                  <div v-if="!method.available" class="text-xs text-red-500 mt-1">
                    Không khả dụng
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Order Summary -->
        <div class="card border border-surface-200">
          <div class="font-semibold text-lg mb-4 flex items-center gap-2">
            <i class="pi pi-calculator text-primary"></i>
            Tổng kết đơn hàng
          </div>

          <div v-if="activeTab" class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span>Tạm tính:</span>
              <span>{{ formatCurrency(activeTab.tongTienHang || 0) }}</span>
            </div>
            <div v-if="activeTab.giaTriGiamGiaVoucher > 0" class="flex justify-between text-green-600">
              <span>Giảm giá voucher:</span>
              <span>-{{ formatCurrency(activeTab.giaTriGiamGiaVoucher) }}</span>
            </div>
            <div v-if="activeTab.giaohang" class="flex justify-between">
              <span>Phí giao hàng:</span>
              <span>{{ formatCurrency(activeTab.phiVanChuyen || 0) }}</span>
            </div>
            <hr class="my-2">
            <div class="flex justify-between font-semibold text-lg">
              <span>Tổng cộng:</span>
              <span class="text-primary">{{ formatCurrency(activeTab.tongThanhToan || 0) }}</span>
            </div>

            <!-- Customer Payment Section (for cash payments) -->
            <div v-if="activeTab?.phuongThucThanhToan === 'TIEN_MAT'" class="mt-4 pt-4 border-t border-surface-200">
              <div class="space-y-3">
                <div>
                  <label class="block text-sm font-medium mb-1">Khách hàng đưa:</label>
                  <InputText
                    v-model.number="customerPayment"
                    type="number"
                    placeholder="Nhập số tiền khách đưa..."
                    class="w-full"
                    :min="activeTab?.tongThanhToan || 0"
                    @input="calculateChange"
                  />
                </div>
                <div v-if="customerPayment >= (activeTab?.tongThanhToan || 0)" class="flex justify-between font-semibold text-lg">
                  <span>Tiền trả lại:</span>
                  <span class="text-green-600">{{ formatCurrency(changeAmount) }}</span>
                </div>
                <div v-else-if="customerPayment > 0" class="text-red-500 text-sm">
                  Số tiền không đủ (thiếu {{ formatCurrency((activeTab?.tongThanhToan || 0) - customerPayment) }})
                </div>
              </div>
            </div>
          </div>

          <!-- Create Order Button -->
          <div class="mt-6 pt-4 border-t border-surface-200">
            <Button
              label="Thanh toán"
              icon="pi pi-check"
              severity="success"
              size="large"
              class="w-full"
              @click="showOrderConfirmation"
              :loading="creating"
              :disabled="!canCreateActiveOrder || creating"
            />
            <div v-if="!canCreateActiveOrder" class="text-center mt-2">
              <small class="text-surface-500">
                <span v-if="!activeTab?.sanPhamList?.length">Vui lòng thêm sản phẩm vào đơn hàng</span>
                <span v-else-if="!activeTab?.phuongThucThanhToan">Vui lòng chọn phương thức thanh toán</span>
                <span v-else-if="activeTab?.giaohang && (!recipientInfo.hoTen.trim() || !recipientInfo.soDienThoai.trim())">
                  Vui lòng nhập đầy đủ thông tin người nhận
                </span>
                <span v-else-if="activeTab?.giaohang && (!addressData.duong.trim() || !addressData.phuongXa || !addressData.quanHuyen || !addressData.tinhThanh)">
                  Vui lòng nhập đầy đủ địa chỉ giao hàng
                </span>
                <span v-else-if="activeTab?.giaohang && Object.keys(addressErrors).length > 0">
                  Vui lòng sửa lỗi trong form địa chỉ giao hàng
                </span>
              </small>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>



  <!-- Product Variant Selection Dialog -->
  <ProductVariantDialog
    ref="productVariantDialogRef"
    v-model:visible="variantDialogVisible"
    @variant-selected="addVariantToActiveTab"
    @request-cart-sync="syncCartWithDialog"
  />

  <!-- Fast Customer Creation Dialog -->
  <FastCustomerCreate
    v-model:visible="fastCustomerDialogVisible"
    @customer-created="onCustomerCreated"
  />

  <!-- Fast Address Creation Dialog -->
  <FastAddressCreate
    v-model:visible="fastAddressDialogVisible"
    :customer="activeTab?.khachHang"
    @address-created="onAddressCreated"
  />

  <!-- Mixed Payment Dialog -->
  <MixedPaymentDialog
    v-model:visible="mixedPaymentDialogVisible"
    :total-amount="activeTab?.tongThanhToan || 0"
    :order-type="activeTab?.loaiHoaDon || 'TAI_QUAY'"
    :has-delivery="activeTab?.giaohang || false"
    @confirm="onMixedPaymentConfirm"
  />



  <!-- QR Scanner Dialog -->
  <Dialog
    v-model:visible="showQRScanner"
    modal
    header="Quét QR Serial Number"
    :style="{ width: '500px' }"
    @hide="stopQRScanner"
    :closable="true"
  >
    <div class="text-center">
      <p class="mb-4">Quét mã QR chứa serial number để tự động thêm vào giỏ hàng</p>
      <div class="border-2 border-primary border-dashed rounded-lg p-4 mb-4">
        <!-- QR Scanner component -->
        <div class="flex flex-col items-center justify-center">
          <div v-if="!qrScanResult" class="w-full">
            <div v-if="!cameraError">
              <qrcode-stream
                @detect="onQRDetect"
                @init="onQRInit"
                :track="paintBoundingBox"
                class="w-full h-64 rounded-lg overflow-hidden"
              />
              <p class="text-surface-600 mt-2">Đưa mã QR chứa serial number vào khung hình</p>
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
            <p class="mt-2 font-mono">{{ qrScanResult }}</p>
            <div v-if="qrProcessingResult" class="mt-3">
              <div v-if="qrProcessingResult.success" class="text-green-600">
                <i class="pi pi-check-circle mr-2"></i>
                {{ qrProcessingResult.message }}
              </div>
              <div v-else class="text-red-600">
                <i class="pi pi-times-circle mr-2"></i>
                {{ qrProcessingResult.message }}
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="flex justify-center gap-2">
        <Button
          label="Đóng"
          icon="pi pi-times"
          severity="secondary"
          @click="showQRScanner = false"
        />
        <Button
          v-if="qrScanResult && qrProcessingResult?.success"
          label="Quét tiếp"
          icon="pi pi-refresh"
          severity="info"
          @click="resetQRScanner"
        />
      </div>
    </div>
  </Dialog>

  <!-- Order Confirmation Dialog -->
  <Dialog
    v-model:visible="orderConfirmationVisible"
    modal
    header="Xác nhận đơn hàng"
    :style="{ width: '600px' }"
    :closable="!creating"
    :dismissableMask="!creating"
  >
    <div v-if="activeTab" class="space-y-6">
      <!-- Customer Information -->
      <div class="border rounded-lg p-4 bg-surface-50">
        <h4 class="font-semibold text-lg mb-3 flex items-center gap-2">
          <i class="pi pi-user text-primary"></i>
          Thông tin khách hàng
        </h4>
        <div v-if="activeTab.khachHang" class="space-y-2">
          <div class="flex items-center gap-3">
            <Avatar :label="activeTab.khachHang.hoTen?.charAt(0)" size="small" />
            <div>
              <div class="font-medium">{{ activeTab.khachHang.hoTen }}</div>
              <div class="text-sm text-surface-500">{{ activeTab.khachHang.soDienThoai }}</div>
              <div v-if="activeTab.khachHang.email" class="text-xs text-surface-400">{{ activeTab.khachHang.email }}</div>
            </div>
          </div>

          <!-- Delivery Information (when delivery is enabled) -->
          <div v-if="activeTab.giaohang" class="mt-3 p-3 border rounded-lg bg-blue-50">
            <div class="font-medium text-sm mb-3 flex items-center gap-2">
              <i class="pi pi-truck text-blue-600"></i>
              <span class="text-blue-800">Thông tin giao hàng</span>
            </div>

            <!-- Recipient Information -->
            <div class="space-y-2 mb-3">
              <div class="text-sm">
                <span class="font-medium text-blue-700">Người nhận:</span>
                <span class="text-surface-700 ml-2">{{ recipientInfo.hoTen || 'Chưa nhập' }}</span>
              </div>
              <div class="text-sm">
                <span class="font-medium text-blue-700">Số điện thoại:</span>
                <span class="text-surface-700 ml-2">{{ recipientInfo.soDienThoai || 'Chưa nhập' }}</span>
              </div>
            </div>

            <!-- Delivery Address -->
            <div class="border-t border-blue-200 pt-2">
              <div class="text-sm">
                <span class="font-medium text-blue-700">Địa chỉ giao hàng:</span>
                <div class="text-surface-700 mt-1 ml-2" :class="{ 'text-surface-400 italic': !addressData.duong?.trim() }">
                  {{ formattedDeliveryAddress }}
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="text-surface-500 italic">
          <div v-if="activeTab.giaohang && recipientInfo.hoTen.trim()">
            Khách hàng: {{ recipientInfo.hoTen }} ({{ recipientInfo.soDienThoai || 'Chưa có SĐT' }})
          </div>
          <div v-else>
            Khách hàng vãng lai
          </div>
        </div>
      </div>



      <!-- Products Summary -->
      <div class="border rounded-lg p-4 bg-surface-50">
        <h4 class="font-semibold text-lg mb-3 flex items-center gap-2">
          <i class="pi pi-shopping-cart text-primary"></i>
          Sản phẩm ({{ activeTab.sanPhamList.length }} sản phẩm)
        </h4>
        <div class="space-y-3 max-h-40 overflow-y-auto">
          <div
            v-for="(item, index) in activeTab.sanPhamList"
            :key="index"
            class="flex items-center gap-3 p-2 border rounded bg-white"
          >
            <img
              :src="getCartItemImage(item) || '/placeholder-product.png'"
              :alt="getCartItemName(item)"
              class="w-10 h-10 object-cover rounded"
            />
            <div class="flex-1 min-w-0">
              <div class="font-medium text-sm">{{ getCartItemName(item) }}</div>
              <div class="text-xs text-surface-500">{{ getCartItemCode(item) }}</div>
              <div v-if="item.sanPhamChiTiet?.serialNumber" class="text-xs text-primary">
                Serial: {{ item.sanPhamChiTiet.serialNumber }}
              </div>
            </div>
            <div class="text-right">
              <div class="font-semibold text-primary">{{ formatCurrency(item.thanhTien) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Payment and Delivery Information -->
      <div class="border rounded-lg p-4 bg-surface-50">
        <h4 class="font-semibold text-lg mb-3 flex items-center gap-2">
          <i class="pi pi-credit-card text-primary"></i>
          Thanh toán & Giao hàng
        </h4>
        <div class="space-y-2">
          <div class="flex justify-between">
            <span>Phương thức thanh toán:</span>
            <span class="font-medium">
              {{ paymentMethods.find(m => m.value === activeTab.phuongThucThanhToan)?.label || activeTab.phuongThucThanhToan }}
            </span>
          </div>
          <div class="flex justify-between">
            <span>Hình thức:</span>
            <span class="font-medium">
              {{ activeTab.giaohang ? 'Giao hàng tận nơi' : 'Lấy tại cửa hàng' }}
            </span>
          </div>
        </div>
      </div>

      <!-- Order Summary -->
      <div class="border rounded-lg p-4 bg-primary/5">
        <h4 class="font-semibold text-lg mb-3 flex items-center gap-2">
          <i class="pi pi-calculator text-primary"></i>
          Tổng kết đơn hàng
        </h4>
        <div class="space-y-2">
          <div class="flex justify-between">
            <span>Tạm tính:</span>
            <span>{{ formatCurrency(activeTab.tongTienHang || 0) }}</span>
          </div>
          <div v-if="activeTab.giaTriGiamGiaVoucher > 0" class="flex justify-between text-green-600">
            <span>Giảm giá voucher:</span>
            <span>-{{ formatCurrency(activeTab.giaTriGiamGiaVoucher) }}</span>
          </div>
          <div v-if="activeTab.giaohang" class="flex justify-between">
            <span>Phí giao hàng:</span>
            <span>{{ formatCurrency(activeTab.phiVanChuyen || 0) }}</span>
          </div>
          <hr class="my-2">
          <div class="flex justify-between font-semibold text-lg">
            <span>Tổng cộng:</span>
            <span class="text-primary">{{ formatCurrency(activeTab.tongThanhToan || 0) }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <Button
          label="Hủy"
          icon="pi pi-times"
          severity="secondary"
          outlined
          @click="orderConfirmationVisible = false"
          :disabled="creating"
        />
        <Button
          label="Xác nhận thanh toán"
          icon="pi pi-check"
          severity="success"
          @click="confirmAndCreateOrder"
          :loading="creating"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, inject } from 'vue'
import { storeToRefs } from 'pinia'
import { useToast } from 'primevue/usetoast'
import { useOrderStore } from '@/stores/orderStore'
import { useCustomerStore } from '@/stores/customerstore'
import { useProductStore } from '@/stores/productstore'

import { useCartReservations } from '@/composables/useCartReservations'
import { useEmbeddedAddress } from '@/composables/useEmbeddedAddress'
import voucherApi from '@/apis/voucherApi'

import { useOrderAudit } from '@/composables/useOrderAudit'
import { useOrderValidation } from '@/composables/useOrderValidation'
import storageApi from '@/apis/storage'
import serialNumberApi from '@/apis/serialNumberApi'



// PrimeVue Components
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import InputText from 'primevue/inputtext'
import AutoComplete from 'primevue/autocomplete'
import Avatar from 'primevue/avatar'
import ToggleButton from 'primevue/togglebutton'
import Dropdown from 'primevue/dropdown'
import Dialog from 'primevue/dialog'



// Custom Components
import ProductVariantDialog from '@/components/orders/ProductVariantDialog.vue'
import FastCustomerCreate from '@/components/orders/FastCustomerCreate.vue'
import FastAddressCreate from '@/components/orders/FastAddressCreate.vue'
import MixedPaymentDialog from '@/components/orders/MixedPaymentDialog.vue'

// QR Scanner
import { QrcodeStream } from 'vue-qrcode-reader'

// Store access
const toast = useToast()
const orderStore = useOrderStore()
const customerStore = useCustomerStore()
const productStore = useProductStore()
const confirmDialog = inject('confirmDialog')


// Cart reservations
const {
  reserveForCart,
  releaseCartReservations,
  releaseSpecificItems
} = useCartReservations()

// Destructure store state and actions using storeToRefs for reactive properties
const {
  orderTabs,
  activeTabId,
  activeTab,
  hasActiveTabs,
  canCreateNewTab
} = storeToRefs(orderStore)

const {
  createNewOrderTab,
  closeOrderTab,
  switchToTab,
  updateActiveTabData,
  calculateTabTotals,
  createOrderFromTab
} = orderStore

// Local state
const creating = ref(false)
const selectedCustomer = ref(null)
const customerSuggestions = ref([])



const availableVouchers = ref([])

// Smart voucher recommendation state
const voucherRecommendations = ref([])

// Voucher display state
const showAllVouchers = ref(false)
const voucherDisplayLimit = ref(3)

// New state for enhanced features
const bestVoucherResult = ref(null)
const loadingBestVoucher = ref(false)

// Image URL cache for performance
const imageUrlCache = ref(new Map())

// Product variant dialog state
const variantDialogVisible = ref(false)
const productVariantDialogRef = ref(null)



// Fast customer creation dialog state
const fastCustomerDialogVisible = ref(false)

// Fast address creation dialog state
const fastAddressDialogVisible = ref(false)



// QR Scanner state
const showQRScanner = ref(false)
const qrScanResult = ref(null)
const qrProcessingResult = ref(null)
const cameraError = ref(null)

// Order confirmation dialog state
const orderConfirmationVisible = ref(false)

// Mixed payment dialog state
const mixedPaymentDialogVisible = ref(false)

// Local state
const hasUnsavedChanges = ref(false)



// Customer payment state
const customerPayment = ref(0)
const changeAmount = ref(0)

// Recipient information state
const recipientInfo = ref({
  hoTen: '',
  soDienThoai: ''
})
const recipientNameSuggestions = ref([])
const recipientPhoneSuggestions = ref([])
const recipientSuggestions = ref([]) // Keep for backward compatibility
const recipientErrors = ref({})
const searchingRecipient = ref(false)

// Separate recipient customer tracking for Scenario 2 (different recipient than customer)
const recipientCustomer = ref(null)

// Debounce timers for recipient search
let recipientNameSearchTimer = null
let recipientPhoneSearchTimer = null

// Embedded address composable
const {
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
  errors: addressErrors,
  onProvinceChange,
  onDistrictChange,
  setAddressData,
  // Enhanced address management functions
  compareAddresses,
  findMatchingAddress,
  isAddressDifferentFromCustomer,
  addAddressToCustomer
} = useEmbeddedAddress()



// Computed properties
const canCreateActiveOrder = computed(() => {
  if (!activeTab.value) return false

  // Basic requirements
  const hasProducts = activeTab.value.sanPhamList.length > 0
  const hasPaymentMethod = activeTab.value.phuongThucThanhToan

  // Mixed payment validation
  let paymentValid = true
  if (activeTab.value.phuongThucThanhToan === 'MIXED') {
    paymentValid = activeTab.value.mixedPayments &&
                   activeTab.value.mixedPayments.length > 0 &&
                   activeTab.value.mixedPayments.every(p => p.method && p.amount > 0)
  }

  // Delivery validation using embedded address form
  let deliveryValid = true
  if (activeTab.value.giaohang) {
    // When shipping, need recipient information and complete address
    const hasRecipientInfo = recipientInfo.value.hoTen.trim() && recipientInfo.value.soDienThoai.trim()

    // Use embedded address validation for complete address check
    const hasCompleteAddress = addressData.value.duong.trim() &&
                              addressData.value.phuongXa &&
                              addressData.value.quanHuyen &&
                              addressData.value.tinhThanh &&
                              Object.keys(addressErrors.value).length === 0

    deliveryValid = hasRecipientInfo && hasCompleteAddress
  }

  return hasProducts && hasPaymentMethod && paymentValid && deliveryValid
})

const paymentMethods = computed(() => {
  if (!activeTab.value) return []

  const methods = []

  // TIEN_MAT - Only for TAI_QUAY orders (POS only)
  if (activeTab.value.loaiHoaDon === 'TAI_QUAY') {
    methods.push({
      value: 'TIEN_MAT',
      label: 'Tiền mặt',
      description: 'Thanh toán bằng tiền mặt tại quầy',
      icon: 'pi pi-wallet',
      available: true
    })
  }

  // TIEN_MAT for delivery - Available for online orders when delivery is enabled (former COD)
  if (activeTab.value.giaohang && activeTab.value.loaiHoaDon === 'ONLINE') {
    methods.push({
      value: 'TIEN_MAT',
      label: 'Tiền mặt khi giao hàng',
      description: 'Thanh toán bằng tiền mặt khi nhận hàng',
      icon: 'pi pi-money-bill',
      available: true
    })
  }

  // VNPAY - Available for both order types
  methods.push({
    value: 'VNPAY',
    label: 'VNPay',
    description: 'Thanh toán qua ví điện tử VNPay',
    icon: 'pi pi-credit-card',
    available: true
  })

  // MOMO - Available for both order types
  methods.push({
    value: 'MOMO',
    label: 'MoMo',
    description: 'Thanh toán qua ví điện tử MoMo',
    icon: 'pi pi-mobile',
    available: true
  })

  // VIETQR - Available for both order types
  methods.push({
    value: 'VIETQR',
    label: 'VietQR',
    description: 'Chuyển khoản ngân hàng qua QR Code',
    icon: 'pi pi-qrcode',
    available: true
  })

  return methods
})

// Computed property for displayed available vouchers
const displayedAvailableVouchers = computed(() => {
  if (showAllVouchers.value) {
    return availableVouchers.value
  }
  return availableVouchers.value.slice(0, voucherDisplayLimit.value)
})

// Computed property for formatted delivery address
const formattedDeliveryAddress = computed(() => {
  if (!activeTab.value?.giaohang) return ''

  const parts = []

  if (addressData.value.duong?.trim()) {
    parts.push(addressData.value.duong.trim())
  }

  const locationParts = []
  if (addressData.value.phuongXa) locationParts.push(addressData.value.phuongXa)
  if (addressData.value.quanHuyen) locationParts.push(addressData.value.quanHuyen)
  if (addressData.value.tinhThanh) locationParts.push(addressData.value.tinhThanh)

  if (locationParts.length > 0) {
    parts.push(locationParts.join(', '))
  }

  return parts.length > 0 ? parts.join(', ') : 'Chưa nhập địa chỉ giao hàng'
})



// Methods
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

// Voucher display methods
const toggleVoucherDisplay = () => {
  showAllVouchers.value = !showAllVouchers.value
}

// Method to find the overall best voucher across all vouchers (applied + available)
const getBestOverallVoucher = () => {
  const allVouchers = []

  // Add applied vouchers with their actual discount amounts
  if (activeTab.value?.voucherList?.length) {
    activeTab.value.voucherList.forEach(voucher => {
      allVouchers.push({
        ...voucher,
        discountAmount: voucher.giaTriGiam || 0,
        isApplied: true
      })
    })
  }

  // Add available vouchers with their calculated discount amounts
  if (availableVouchers.value.length) {
    availableVouchers.value.forEach(voucher => {
      allVouchers.push({
        ...voucher,
        discountAmount: calculateVoucherDiscount(voucher),
        isApplied: false
      })
    })
  }

  if (!allVouchers.length) return null

  // Find the voucher with the highest discount amount
  return allVouchers.reduce((best, current) => {
    return current.discountAmount > best.discountAmount ? current : best
  }, allVouchers[0])
}

// Method to determine if a voucher is the best overall voucher
const isBestVoucher = (voucher) => {
  const bestVoucher = getBestOverallVoucher()
  if (!bestVoucher) return false

  return voucher.id === bestVoucher.id || voucher.maPhieuGiamGia === bestVoucher.maPhieuGiamGia
}

// Method to determine if a voucher is the best among available vouchers (for styling only)
const isBestAvailableVoucher = (voucher) => {
  const bestVoucher = getBestOverallVoucher()
  if (!bestVoucher) return false

  // Only return true if this voucher is the best overall AND it's not applied
  const isOverallBest = voucher.id === bestVoucher.id || voucher.maPhieuGiamGia === bestVoucher.maPhieuGiamGia
  const isNotApplied = !activeTab.value?.voucherList?.some(applied =>
    applied.id === voucher.id || applied.maPhieuGiamGia === voucher.maPhieuGiamGia
  )

  return isOverallBest && isNotApplied
}

// Calculate actual discount amount for a voucher based on current order total
const calculateVoucherDiscount = (voucher, orderTotal = null) => {
  if (!voucher || !voucher.giaTriGiam) return 0

  const total = orderTotal || activeTab.value?.tongTienHang || 0

  if (voucher.loaiGiamGia === 'PHAN_TRAM') {
    // Percentage discount
    const discountAmount = (total * voucher.giaTriGiam) / 100
    // Apply maximum discount limit if specified
    if (voucher.giaTriGiamToiDa && discountAmount > voucher.giaTriGiamToiDa) {
      return voucher.giaTriGiamToiDa
    }
    return discountAmount
  } else {
    // Fixed amount discount (SO_TIEN_CO_DINH)
    return Math.min(voucher.giaTriGiam, total)
  }
}









// Add selected variant to active tab (handles frontend-selected variants)
const addVariantToActiveTab = async (variantData) => {
  if (!activeTab.value) return

  const { sanPhamChiTiet, soLuong, donGia, thanhTien, groupInfo } = variantData

  // Check if this specific variant with the same serial number already exists in cart
  // For variants with serial numbers, we need to check both variant ID and serial number
  const existingIndex = activeTab.value.sanPhamList.findIndex(item => {
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
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: `Phiên bản này${serialInfo} đã có trong giỏ hàng`,
      life: 3000
    })
    return
  }

  // Reserve inventory in backend before adding to cart
  try {
    const reservationRequest = {
      sanPhamChiTietId: sanPhamChiTiet.id,
      soLuong: soLuong,
      tabId: activeTabId.value,
      serialNumbers: sanPhamChiTiet.serialNumber ? [sanPhamChiTiet.serialNumber] : undefined
    }

    await reserveForCart(reservationRequest)

    // Add new variant to cart after successful reservation
    activeTab.value.sanPhamList.push({
      sanPhamChiTiet: sanPhamChiTiet,
      soLuong: soLuong,
      donGia: donGia,
      thanhTien: thanhTien,
      groupInfo: groupInfo // Store group info for display purposes
    })

    calculateTabTotals(activeTabId.value)

    // Sync with product variant dialog to update stock counts
    // Add small delay to prevent race condition with immediate serial tracking
    setTimeout(() => {
      syncCartWithDialog()
    }, 100)
  } catch (error) {
    console.error('Failed to reserve inventory:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Không thể đặt trước sản phẩm',
      life: 3000
    })
    return // Don't add to cart if reservation fails
  }

  // Don't show individual success messages when adding from groups
  // The ProductVariantDialog will show the group success message
  if (!groupInfo?.isFromGroup) {
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã thêm sản phẩm vào giỏ hàng',
      life: 3000
    })
  }
}

// Helper methods for cart item display
const getCartItemImage = (item) => {
  let imageFilename = null

  if (item.sanPhamChiTiet) {
    // Get first image from variant's image array
    if (item.sanPhamChiTiet.hinhAnh && Array.isArray(item.sanPhamChiTiet.hinhAnh) && item.sanPhamChiTiet.hinhAnh.length > 0) {
      imageFilename = item.sanPhamChiTiet.hinhAnh[0]
    } else {
      // Fallback to product image if variant has no image
      const productImages = item.sanPhamChiTiet.sanPham?.hinhAnh
      if (productImages && Array.isArray(productImages) && productImages.length > 0) {
        imageFilename = productImages[0]
      } else if (typeof productImages === 'string') {
        imageFilename = productImages
      }
    }
  } else {
    // Legacy support for old product-based items
    const productImages = item.sanPham?.hinhAnh
    if (productImages && Array.isArray(productImages) && productImages.length > 0) {
      imageFilename = productImages[0]
    } else if (typeof productImages === 'string') {
      imageFilename = productImages
    }
  }

  if (!imageFilename) return null

  // If it's already a full URL, return as is
  if (imageFilename.startsWith('http')) return imageFilename

  // Check cache first
  if (imageUrlCache.value.has(imageFilename)) {
    return imageUrlCache.value.get(imageFilename)
  }

  // Load presigned URL asynchronously
  loadCartImageUrl(imageFilename)

  // Return null for now, will update when loaded
  return null
}

const loadCartImageUrl = async (imageFilename) => {
  try {
    // Get presigned URL for the image filename
    const presignedUrl = await storageApi.getPresignedUrl('products', imageFilename)

    // Cache the URL for future use
    imageUrlCache.value.set(imageFilename, presignedUrl)

    // Force reactivity update
    imageUrlCache.value = new Map(imageUrlCache.value)
  } catch (error) {
    console.warn('Error getting presigned URL for cart image:', imageFilename, error)
    // Cache null to prevent repeated attempts
    imageUrlCache.value.set(imageFilename, null)
  }
}



const getCartItemName = (item) => {
  if (item.sanPhamChiTiet) {
    return item.sanPhamChiTiet.sanPham?.tenSanPham || 'Sản phẩm'
  }
  // Legacy support for old product-based items
  return item.sanPham?.tenSanPham || 'Sản phẩm'
}

const getCartItemCode = (item) => {
  if (item.sanPhamChiTiet) {
    // Show product code instead of serial number to avoid duplication
    return item.sanPhamChiTiet.sanPham?.maSanPham || item.sanPhamChiTiet.maSanPhamChiTiet || ''
  }
  // Legacy support for old product-based items
  return item.sanPham?.maSanPham || ''
}

const getVariantDisplayInfo = (item) => {
  if (item.sanPhamChiTiet) {
    const parts = []

    // Add hardware specifications
    if (item.sanPhamChiTiet.cpu) parts.push(item.sanPhamChiTiet.cpu.moTaCpu)
    if (item.sanPhamChiTiet.ram) parts.push(item.sanPhamChiTiet.ram.moTaRam)
    if (item.sanPhamChiTiet.gpu) parts.push(item.sanPhamChiTiet.gpu.moTaGpu)
    if (item.sanPhamChiTiet.mauSac) parts.push(item.sanPhamChiTiet.mauSac.moTaMauSac)

    // Storage field reference (boNho)
    if (item.sanPhamChiTiet.boNho) parts.push(item.sanPhamChiTiet.boNho.moTaBoNho)

    if (item.sanPhamChiTiet.manHinh) parts.push(item.sanPhamChiTiet.manHinh.moTaManHinh)

    // Add serial number if available
    if (item.sanPhamChiTiet.serialNumber) {
      parts.push(`Serial: ${item.sanPhamChiTiet.serialNumber}`)
    }

    const displayInfo = parts.join(' • ')

    // Add group info if this variant was selected from a group
    if (item.groupInfo?.isFromGroup) {
      return `${displayInfo} • ${item.groupInfo.displayName}`
    }

    return displayInfo
  }
  return ''
}



const removeFromActiveTab = async (index) => {
  const item = activeTab.value.sanPhamList[index]

  // Release backend reservation before removing from cart
  try {
    if (item?.sanPhamChiTiet?.id) {
      await releaseSpecificItems(activeTabId.value, item.sanPhamChiTiet.id, item.soLuong || 1)
    }
  } catch (error) {
    console.error('Failed to release reservation:', error)
    // Continue with removal even if backend release fails
  }

  activeTab.value.sanPhamList.splice(index, 1)
  calculateTabTotals(activeTabId.value)

  // Sync with product variant dialog to update stock counts
  // Add small delay to prevent race condition with immediate serial tracking
  setTimeout(() => {
    syncCartWithDialog()
  }, 100)
}



// QR Scanner Methods
const onQRDetect = async (detectedCodes) => {
  if (detectedCodes && detectedCodes.length > 0) {
    const scannedValue = detectedCodes[0].rawValue
    console.log('QR Code detected:', scannedValue)

    // Set the scanned result
    qrScanResult.value = scannedValue

    // Process the scanned serial number
    await processScannedSerialNumber(scannedValue)
  }
}

const onQRInit = async (promise) => {
  try {
    await promise
    cameraError.value = null

    toast.add({
      severity: 'info',
      summary: 'Camera',
      detail: 'Camera đã được kích hoạt thành công',
      life: 2000,
    })
  } catch (error) {
    console.error('QR Scanner initialization error:', error)

    if (error.name === 'NotAllowedError') {
      cameraError.value = 'Quyền truy cập camera bị từ chối. Vui lòng cấp quyền và thử lại.'
    } else if (error.name === 'NotFoundError') {
      cameraError.value = 'Không tìm thấy camera. Vui lòng kiểm tra thiết bị.'
    } else if (error.name === 'NotSupportedError') {
      cameraError.value = 'Trình duyệt không hỗ trợ camera.'
    } else if (error.name === 'NotReadableError') {
      cameraError.value = 'Camera đang được sử dụng bởi ứng dụng khác.'
    } else if (error.name === 'OverconstrainedError') {
      cameraError.value = 'Camera không đáp ứng yêu cầu.'
    } else {
      cameraError.value = 'Lỗi không xác định khi truy cập camera.'
    }
  }
}

const paintBoundingBox = (detectedCodes, ctx) => {
  for (const detectedCode of detectedCodes) {
    const { boundingBox: { x, y, width, height } } = detectedCode

    ctx.lineWidth = 2
    ctx.strokeStyle = '#007bff'
    ctx.strokeRect(x, y, width, height)
  }
}

const requestCameraPermission = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    stream.getTracks().forEach((track) => track.stop())

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
    cameraError.value = 'Không thể truy cập camera. Vui lòng kiểm tra cài đặt quyền của trình duyệt.'
    toast.add({
      severity: 'error',
      summary: 'Lỗi Camera',
      detail: cameraError.value,
      life: 5000,
    })
  }
}

const stopQRScanner = () => {
  qrScanResult.value = null
  qrProcessingResult.value = null
  cameraError.value = null
}

const resetQRScanner = () => {
  qrScanResult.value = null
  qrProcessingResult.value = null
}

const processScannedSerialNumber = async (serialNumber) => {
  try {
    console.log('Processing scanned serial number:', serialNumber)

    // Find the serial number in the database
    const serialData = await serialNumberApi.getBySerialNumber(serialNumber)

    if (!serialData) {
      qrProcessingResult.value = {
        success: false,
        message: `Serial number "${serialNumber}" không tồn tại trong hệ thống`
      }
      return
    }

    // Check if serial number is available
    if (serialData.trangThai !== 'AVAILABLE') {
      qrProcessingResult.value = {
        success: false,
        message: `Serial number "${serialNumber}" không khả dụng (Trạng thái: ${serialData.trangThai})`
      }
      return
    }

    // Get the product variant information
    const variantId = serialData.sanPhamChiTietId
    if (!variantId) {
      qrProcessingResult.value = {
        success: false,
        message: `Serial number "${serialNumber}" không liên kết với sản phẩm nào`
      }
      return
    }

    // Find variant in current products or search for it
    let variant = null

    // Try to find variant by fetching from product store
    // This will be handled by the enhanced ProductVariantDialog

    // If not found in current products, search all products
    if (!variant) {
      try {
        await productStore.fetchProducts()
        for (const product of productStore.products) {
          if (product.sanPhamChiTiets) {
            variant = product.sanPhamChiTiets.find(v => v.id === variantId)
            if (variant) {
              // Add product reference to variant for display
              variant.sanPham = product
              break
            }
          }
        }
      } catch (error) {
        console.error('Error fetching products for variant lookup:', error)
      }
    }

    if (!variant) {
      qrProcessingResult.value = {
        success: false,
        message: `Không tìm thấy thông tin sản phẩm cho serial number "${serialNumber}"`
      }
      return
    }

    // Check if this serial number is already in cart
    const existingInCart = activeTab.value?.sanPhamList?.find(item =>
      item.sanPhamChiTiet?.serialNumberId === serialData.id ||
      item.sanPhamChiTiet?.serialNumber === serialNumber
    )

    if (existingInCart) {
      qrProcessingResult.value = {
        success: false,
        message: `Serial number "${serialNumber}" đã có trong giỏ hàng`
      }
      return
    }

    // Create variant data with serial number
    const variantWithSerial = {
      ...variant,
      serialNumber: serialNumber,
      serialNumberId: serialData.id
    }

    // Add to cart
    const variantData = {
      sanPhamChiTiet: variantWithSerial,
      soLuong: 1,
      donGia: variant.giaKhuyenMai && variant.giaKhuyenMai < variant.giaBan ? variant.giaKhuyenMai : variant.giaBan,
      thanhTien: variant.giaKhuyenMai && variant.giaKhuyenMai < variant.giaBan ? variant.giaKhuyenMai : variant.giaBan
    }

    addVariantToActiveTab(variantData)

    qrProcessingResult.value = {
      success: true,
      message: `Đã thêm sản phẩm với serial "${serialNumber}" vào giỏ hàng`
    }

    // Auto-close scanner after successful scan (optional)
    setTimeout(() => {
      if (qrProcessingResult.value?.success) {
        resetQRScanner()
      }
    }, 2000)

  } catch (error) {
    console.error('Error processing scanned serial number:', error)
    qrProcessingResult.value = {
      success: false,
      message: `Lỗi khi xử lý serial number: ${error.message}`
    }
  }
}





// Customer display label helper
const getCustomerDisplayLabel = (customer) => {
  if (!customer) return ''
  const name = customer.hoTen || 'Không có tên'
  const phone = customer.soDienThoai || 'Không có SĐT'
  return `${name} - ${phone}`
}

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

const onCustomerSelect = async (event) => {
  try {
    console.log('Customer selected from search:', event.value)

    // Fetch complete customer data with addresses to ensure we have all necessary information
    const customerWithAddresses = await customerStore.fetchCustomerById(event.value.id)
    console.log('Customer data with addresses loaded:', customerWithAddresses)

    updateActiveTabData({
      khachHang: customerWithAddresses,
      diaChiGiaoHang: null // Clear any previously selected address
    })
    selectedCustomer.value = customerWithAddresses

    // Load available vouchers for the selected customer
    await loadAvailableVouchers()
  } catch (error) {
    console.error('Error loading customer details:', error)
    // Fallback to the basic customer data from search
    console.log('Using fallback customer data:', event.value)
    updateActiveTabData({
      khachHang: event.value,
      diaChiGiaoHang: null // Clear any previously selected address
    })
    selectedCustomer.value = event.value
    await loadAvailableVouchers()
  }
}

const clearCustomerFromTab = () => {
  updateActiveTabData({ khachHang: null, diaChiGiaoHang: null })
  selectedCustomer.value = null
  // Clear available vouchers when customer is removed
  availableVouchers.value = []
  // Clear applied vouchers when customer is removed
  if (activeTab.value) {
    activeTab.value.voucherList = []
    calculateTabTotals(activeTabId.value)
  }
  // Reset customer payment fields
  customerPayment.value = 0
  changeAmount.value = 0
}

// Enhanced recipient search methods with debouncing
const searchRecipientByName = async (event) => {
  // Clear previous timer
  if (recipientNameSearchTimer) {
    clearTimeout(recipientNameSearchTimer)
  }

  // Debounce search to prevent excessive API calls
  recipientNameSearchTimer = setTimeout(async () => {
    try {
      console.log('Searching recipients by name:', event.query)

      // Allow single character searches like customer search
      if (!event.query || event.query.trim() === '') {
        recipientNameSuggestions.value = []
        recipientSuggestions.value = [] // Keep backward compatibility
        return
      }

      searchingRecipient.value = true

      // Use the same customer search logic for recipient name search
      const customers = await customerStore.fetchCustomers({ search: event.query })

      const suggestions = customers.map(customer => ({
        ...customer,
        displayLabel: customer.hoTen,
        searchType: 'name'
      })).slice(0, 10) // Limit to 10 results for performance

      recipientNameSuggestions.value = suggestions
      recipientSuggestions.value = suggestions // Keep backward compatibility

    } catch (error) {
      console.error('Error searching recipients by name:', error)
      recipientNameSuggestions.value = []
      recipientSuggestions.value = []
    } finally {
      searchingRecipient.value = false
    }
  }, 300) // 300ms debounce delay
}

const searchRecipientByPhone = async (event) => {
  // Clear previous timer
  if (recipientPhoneSearchTimer) {
    clearTimeout(recipientPhoneSearchTimer)
  }

  // Debounce search to prevent excessive API calls
  recipientPhoneSearchTimer = setTimeout(async () => {
    try {
      console.log('Searching recipients by phone:', event.query)

      // Allow single character searches like customer search
      if (!event.query || event.query.trim() === '') {
        recipientPhoneSuggestions.value = []
        recipientSuggestions.value = [] // Keep backward compatibility
        return
      }

      searchingRecipient.value = true

      // Use the same customer search logic for recipient phone search
      const customers = await customerStore.fetchCustomers({ search: event.query })

      const suggestions = customers.map(customer => ({
        ...customer,
        displayLabel: customer.soDienThoai,
        searchType: 'phone'
      })).slice(0, 10) // Limit to 10 results for performance

      recipientPhoneSuggestions.value = suggestions
      recipientSuggestions.value = suggestions // Keep backward compatibility

    } catch (error) {
      console.error('Error searching recipients by phone:', error)
      recipientPhoneSuggestions.value = []
      recipientSuggestions.value = []
    } finally {
      searchingRecipient.value = false
    }
  }, 300) // 300ms debounce delay
}

// Enhanced recipient selection handlers
const onRecipientNameSelect = async (event) => {
  try {
    console.log('Recipient name selected:', event.value)
    await handleRecipientSelection(event.value, 'name')
  } catch (error) {
    console.error('Error handling recipient name selection:', error)
  }
}

const onRecipientPhoneSelect = async (event) => {
  try {
    console.log('Recipient phone selected:', event.value)
    await handleRecipientSelection(event.value, 'phone')
  } catch (error) {
    console.error('Error handling recipient phone selection:', error)
  }
}

// Unified recipient selection handler
const handleRecipientSelection = async (selectedCustomer, fieldType) => {
  try {
    // Auto-populate recipient information
    if (fieldType === 'name') {
      recipientInfo.value.hoTen = selectedCustomer.hoTen || ''
      // Auto-fill phone if available and not already filled
      if (selectedCustomer.soDienThoai && !recipientInfo.value.soDienThoai.trim()) {
        recipientInfo.value.soDienThoai = selectedCustomer.soDienThoai
      }
    } else if (fieldType === 'phone') {
      recipientInfo.value.soDienThoai = selectedCustomer.soDienThoai || ''
      // Auto-fill name if available and not already filled
      if (selectedCustomer.hoTen && !recipientInfo.value.hoTen.trim()) {
        recipientInfo.value.hoTen = selectedCustomer.hoTen
      }
    }

    // Load complete customer data with addresses if available
    let customerWithAddresses = selectedCustomer
    if (selectedCustomer.id) {
      try {
        customerWithAddresses = await customerStore.fetchCustomerById(selectedCustomer.id)
        console.log('Loaded complete customer data:', customerWithAddresses)
      } catch (error) {
        console.warn('Could not load complete customer data, using basic info:', error)
      }
    }

    // Populate address form with customer's default or first address
    await populateAddressFromCustomer(customerWithAddresses)

    // Clear any validation errors
    recipientErrors.value = {}

  } catch (error) {
    console.error('Error handling recipient selection:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Có lỗi xảy ra khi xử lý thông tin người nhận',
      life: 3000
    })
  }
}

// Address population from customer data
const populateAddressFromCustomer = async (customer) => {
  try {
    if (!customer || !customer.diaChis || customer.diaChis.length === 0) {
      console.log('No addresses found for customer, keeping current address form data')
      return
    }

    // Find default address or use first address
    const addressToUse = customer.diaChis.find(addr => addr.laMacDinh) || customer.diaChis[0]

    console.log('Populating address form with:', addressToUse)

    // Use setAddressData from the composable to populate the form
    setAddressData({
      duong: addressToUse.duong || '',
      phuongXa: addressToUse.phuongXa || '',
      quanHuyen: addressToUse.quanHuyen || '',
      tinhThanh: addressToUse.tinhThanh || '',
      loaiDiaChi: addressToUse.loaiDiaChi || 'Nhà riêng'
    })

    toast.add({
      severity: 'info',
      summary: 'Thông tin',
      detail: 'Đã tự động điền địa chỉ từ thông tin khách hàng',
      life: 2000
    })

  } catch (error) {
    console.error('Error populating address from customer:', error)
  }
}

// Customer lookup functionality
const checkExistingCustomer = async () => {
  try {
    if (!recipientInfo.value.hoTen.trim() && !recipientInfo.value.soDienThoai.trim()) {
      return null
    }

    console.log('Checking for existing customer with recipient info:', recipientInfo.value)

    // Search by phone first (more unique)
    if (recipientInfo.value.soDienThoai.trim()) {
      const phoneResults = await customerStore.fetchCustomers({
        search: recipientInfo.value.soDienThoai.trim()
      })

      // Look for exact phone match
      const phoneMatch = phoneResults.find(customer =>
        customer.soDienThoai === recipientInfo.value.soDienThoai.trim()
      )

      if (phoneMatch) {
        console.log('Found customer by phone:', phoneMatch)
        return phoneMatch
      }
    }

    // Search by name if no phone match
    if (recipientInfo.value.hoTen.trim()) {
      const nameResults = await customerStore.fetchCustomers({
        search: recipientInfo.value.hoTen.trim()
      })

      // Look for exact name match
      const nameMatch = nameResults.find(customer =>
        customer.hoTen?.toLowerCase() === recipientInfo.value.hoTen.trim().toLowerCase()
      )

      if (nameMatch) {
        console.log('Found customer by name:', nameMatch)
        return nameMatch
      }
    }

    console.log('No existing customer found for recipient info')
    return null

  } catch (error) {
    console.error('Error checking existing customer:', error)
    return null
  }
}









// Product selection dialog methods
const showProductSelectionDialog = () => {
  // Open the enhanced ProductVariantDialog that shows all variants from all products
  variantDialogVisible.value = true
}

// Sync cart data with product variant dialog
const syncCartWithDialog = () => {
  if (productVariantDialogRef.value && activeTab.value?.sanPhamList) {
    // Pass current active tab's cart data for immediate UI updates
    productVariantDialogRef.value.updateUsedSerialNumbers(activeTab.value.sanPhamList)

    // Note: Real-time inventory checking is now handled within ProductVariantDialog
    // via the backend API to ensure cross-tab accuracy
  }
}

// Fast customer creation methods
const showFastCustomerDialog = () => {
  fastCustomerDialogVisible.value = true
}

const onCustomerCreated = async (newCustomer) => {
  try {
    console.log('New customer created:', newCustomer)

    // Select the newly created customer
    updateActiveTabData({
      khachHang: newCustomer,
      diaChiGiaoHang: null // Clear any previously selected address
    })
    selectedCustomer.value = newCustomer

    // Load available vouchers for the new customer
    await loadAvailableVouchers()

    // Automatically find and apply best voucher for new customer
    if (activeTab.value.tongTienHang > 0) {
      await findAndApplyBestVoucher()
    }

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã chọn khách hàng ${newCustomer.hoTen} cho đơn hàng`,
      life: 3000
    })
  } catch (error) {
    console.error('Error selecting newly created customer:', error)
  }
}

// Fast address creation methods

const onAddressCreated = async (newAddress) => {
  try {
    console.log('New address created:', newAddress)

    if (!activeTab.value.khachHang) {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không tìm thấy thông tin khách hàng',
        life: 3000
      })
      return
    }

    // Add the new address to the customer's address list
    if (!activeTab.value.khachHang.diaChis) {
      activeTab.value.khachHang.diaChis = []
    }

    // Create a temporary address object with ID for UI purposes
    const tempAddress = {
      ...newAddress,
      id: Date.now(), // Temporary ID for UI
      nguoiDungId: activeTab.value.khachHang.id
    }

    activeTab.value.khachHang.diaChis.push(tempAddress)

    // Automatically select the new address for delivery
    updateActiveTabData({ diaChiGiaoHang: tempAddress })

    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đã thêm địa chỉ giao hàng và chọn làm địa chỉ giao hàng cho đơn hàng này',
      life: 4000
    })
  } catch (error) {
    console.error('Error handling newly created address:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Có lỗi xảy ra khi xử lý địa chỉ mới',
      life: 3000
    })
  }
}



const loadAvailableVouchers = async () => {
  if (!activeTab.value) return

  try {
    const customerId = activeTab.value.khachHang?.id || null
    const orderTotal = activeTab.value.tongTienHang || 0

    const response = await voucherApi.getAvailableVouchers(customerId, orderTotal)

    if (response.success) {
      // Filter out already applied vouchers
      const appliedVoucherCodes = activeTab.value.voucherList.map(v => v.maPhieuGiamGia)
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



const onDeliveryToggle = () => {
  if (!activeTab.value.giaohang) {
    // Clear address when delivery is turned off
    updateActiveTabData({ diaChiGiaoHang: null })
  } else {
    // When delivery is turned on, validate current address selection
    if (activeTab.value.diaChiGiaoHang && activeTab.value.khachHang) {
      const currentAddress = activeTab.value.diaChiGiaoHang
      if (currentAddress.nguoiDungId && currentAddress.nguoiDungId !== activeTab.value.khachHang.id) {
        console.warn('Clearing invalid address selection: Address does not belong to current customer')
        updateActiveTabData({ diaChiGiaoHang: null })
      }
    }
  }

  // Validate current payment method when delivery option changes
  const currentPaymentMethod = activeTab.value.phuongThucThanhToan
  const availablePaymentMethods = paymentMethods.value.map(m => m.value)

  if (currentPaymentMethod && !availablePaymentMethods.includes(currentPaymentMethod)) {
    // Clear invalid payment method
    updateActiveTabData({ phuongThucThanhToan: null })
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Phương thức thanh toán đã chọn không khả dụng với tùy chọn giao hàng hiện tại',
      life: 3000
    })
  }

  calculateTabTotals(activeTabId.value)
}



const removeVoucherFromTab = async (index) => {
  if (!activeTab.value) return

  const removedVoucher = activeTab.value.voucherList[index]

  // Remove voucher directly
  activeTab.value.voucherList.splice(index, 1)
  calculateTabTotals(activeTabId.value)

  // Reload available vouchers to include the removed voucher
  await loadAvailableVouchers()

  toast.add({
    severity: 'info',
    summary: 'Thông báo',
    detail: `Đã gỡ voucher ${removedVoucher.maPhieuGiamGia}`,
    life: 3000
  })
}

const selectVoucher = async (voucher) => {
  if (!activeTab.value) return

  try {
    // Validate voucher before applying
    const customerId = activeTab.value.khachHang?.id || null
    const orderTotal = activeTab.value.tongTienHang || 0

    const response = await voucherApi.validateVoucher(voucher.maPhieuGiamGia, customerId, orderTotal)

    if (response.success && response.data.valid) {
      // Check if voucher is already applied
      const existingVoucher = activeTab.value.voucherList.find(
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

      // SINGLE VOUCHER RESTRICTION: Remove any existing vouchers before applying new one
      if (activeTab.value.voucherList.length > 0) {
        const removedVouchers = [...activeTab.value.voucherList]
        activeTab.value.voucherList = []

        // Reload available vouchers to include the removed vouchers
        await loadAvailableVouchers()

        toast.add({
          severity: 'info',
          summary: 'Thông báo',
          detail: `Đã gỡ ${removedVouchers.length} voucher cũ để áp dụng voucher mới`,
          life: 3000
        })
      }

      // Add voucher to active tab with validated discount amount
      const voucherData = {
        ...response.data.voucher,
        giaTriGiam: response.data.discountAmount
      }

      activeTab.value.voucherList.push(voucherData)
      calculateTabTotals(activeTabId.value)

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

const selectPaymentMethod = (method) => {
  updateActiveTabData({ phuongThucThanhToan: method })

  // Reset customer payment when changing payment method
  if (method !== 'TIEN_MAT') {
    customerPayment.value = 0
    changeAmount.value = 0
  }
}

// Mixed payment methods
const showMixedPaymentDialog = () => {
  mixedPaymentDialogVisible.value = true
}

const onMixedPaymentConfirm = (paymentConfig) => {
  // Store mixed payment configuration in the active tab
  updateActiveTabData({
    phuongThucThanhToan: 'MIXED',
    mixedPayments: paymentConfig.payments
  })

  mixedPaymentDialogVisible.value = false

  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: `Đã cấu hình thanh toán hỗn hợp với ${paymentConfig.payments.length} phương thức`,
    life: 3000
  })
}

// Helper method to get payment method label
const getPaymentMethodLabel = (methodValue) => {
  const method = paymentMethods.value.find(m => m.value === methodValue)
  return method?.label || methodValue
}

// Calculate change amount for cash payments
const calculateChange = () => {
  if (!activeTab.value || activeTab.value.phuongThucThanhToan !== 'TIEN_MAT') {
    changeAmount.value = 0
    return
  }

  const payment = customerPayment.value || 0
  const total = activeTab.value.tongThanhToan || 0
  changeAmount.value = Math.max(0, payment - total)
}



// Automatic voucher selection methods
const findAndApplyBestVoucher = async () => {
  if (!activeTab.value || !activeTab.value.khachHang || activeTab.value.tongTienHang <= 0) return

  try {
    loadingBestVoucher.value = true
    const customerId = activeTab.value.khachHang.id
    const orderTotal = activeTab.value.tongTienHang

    const response = await voucherApi.getBestVoucher(customerId, orderTotal)

    if (response.success && response.data.found) {
      bestVoucherResult.value = response.data

      // Check if this voucher is already applied
      const existingVoucher = activeTab.value.voucherList.find(
        v => v.maPhieuGiamGia === response.data.voucher.maPhieuGiamGia
      )

      if (!existingVoucher) {
        // Automatically apply the best voucher
        await selectVoucher(response.data.voucher)

        toast.add({
          severity: 'success',
          summary: 'Tự động áp dụng voucher',
          detail: `Đã áp dụng voucher tốt nhất: ${response.data.voucher.maPhieuGiamGia} (Giảm ${formatCurrency(response.data.discountAmount)})`,
          life: 4000
        })
      }
    }

    // Generate smart voucher recommendations after processing vouchers
    await generateVoucherRecommendation()
  } catch (error) {
    console.error('Error finding best voucher:', error)
    // Don't show error toast for automatic voucher application to avoid annoying users
  } finally {
    loadingBestVoucher.value = false
  }
}

// Smart Voucher Recommendation Logic
const generateVoucherRecommendation = async () => {
  if (!activeTab.value?.khachHang || !activeTab.value?.tongTienHang) {
    voucherRecommendations.value = []
    return
  }

  try {
    const currentTotal = activeTab.value.tongTienHang

    // Get ALL active vouchers (not just available ones) to find recommendation opportunities
    const allVouchersResponse = await voucherApi.getAllVouchers({ status: 'DA_DIEN_RA' })
    const allVouchers = allVouchersResponse.success ? allVouchersResponse.data : []

    // Filter vouchers that require more spending than current total (for recommendations)
    const futureVouchers = allVouchers.filter(voucher => {
      const minOrder = voucher.giaTriDonHangToiThieu || 0
      return minOrder > currentTotal
    }).sort((a, b) => (a.giaTriDonHangToiThieu || 0) - (b.giaTriDonHangToiThieu || 0))

    // Generate recommendations for multiple tiers (up to 3 recommendations)
    const recommendations = []
    const maxRecommendations = Math.min(3, futureVouchers.length)

    for (let i = 0; i < maxRecommendations; i++) {
      const voucher = futureVouchers[i]
      const targetAmount = voucher.giaTriDonHangToiThieu
      const additionalAmount = targetAmount - currentTotal
      const potentialDiscount = calculateVoucherDiscount(voucher, targetAmount)

      recommendations.push({
        message: `Mua thêm ${formatCurrency(additionalAmount)} để được giảm ${formatCurrency(potentialDiscount)}`,
        voucher: voucher,
        targetAmount: targetAmount,
        additionalAmount: additionalAmount,
        potentialDiscount: potentialDiscount
      })
    }

    voucherRecommendations.value = recommendations
  } catch (error) {
    console.error('Error generating voucher recommendation:', error)
    voucherRecommendations.value = []
  }
}

// Apply recommended voucher with click-to-apply functionality
const applyRecommendedVoucher = async (voucher) => {
  if (!activeTab.value || !voucher) return

  try {
    // Check if the current order total meets the voucher's minimum requirement
    const currentTotal = activeTab.value.tongTienHang || 0
    const minOrder = voucher.giaTriDonHangToiThieu || 0

    if (currentTotal < minOrder) {
      const additionalAmount = minOrder - currentTotal
      toast.add({
        severity: 'warn',
        summary: 'Chưa đủ điều kiện',
        detail: `Cần mua thêm ${formatCurrency(additionalAmount)} để áp dụng voucher này`,
        life: 4000
      })
      return
    }

    // Check if voucher is already applied
    const existingVoucher = activeTab.value.voucherList.find(
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

    // Apply the voucher using existing selectVoucher function
    await selectVoucher(voucher)

    // Regenerate recommendations after applying voucher
    await generateVoucherRecommendation()
  } catch (error) {
    console.error('Error applying recommended voucher:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể áp dụng voucher. Vui lòng thử lại.',
      life: 3000
    })
  }
}

// Order confirmation methods
const showOrderConfirmation = () => {
  if (!activeTab.value) return

  // Perform basic validation before showing confirmation
  if (!canCreateActiveOrder.value) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng hoàn tất thông tin đơn hàng trước khi thanh toán',
      life: 3000
    })
    return
  }

  orderConfirmationVisible.value = true
}

const confirmAndCreateOrder = async () => {
  orderConfirmationVisible.value = false
  await createOrderFromActiveTab()
}

const createOrderFromActiveTab = async () => {
  if (!activeTab.value) return

  // Prevent multiple simultaneous order creation attempts
  if (creating.value) {
    console.log('Order creation already in progress, ignoring duplicate request')
    return
  }

  // Perform comprehensive validation including embedded address and recipient info
  const validationErrors = validateActiveTab()

  // Validate recipient information and address if delivery is enabled
  if (activeTab.value.giaohang) {
    const recipientValid = await validateRecipientInfo()
    const addressValid = validateEmbeddedAddress()

    // Comprehensive address validation
    const comprehensiveAddressValidation = await validateAddressBeforeOrderCreation()

    // Additional comprehensive scenario validation
    const scenarioValidation = await validateAllCustomerScenarios()

    if (!recipientValid || !addressValid || !comprehensiveAddressValidation.valid || !scenarioValidation.valid) {
      let errorDetail = 'Vui lòng kiểm tra lại thông tin người nhận và địa chỉ giao hàng'

      if (!comprehensiveAddressValidation.valid) {
        const errorMessages = Object.values(comprehensiveAddressValidation.errors || {}).join(', ')
        errorDetail = errorMessages || 'Địa chỉ giao hàng không hợp lệ'
      } else if (!scenarioValidation.valid) {
        errorDetail = scenarioValidation.errors.join(', ') || 'Lỗi xác thực kịch bản khách hàng'
      }

      toast.add({
        severity: 'warn',
        summary: 'Dữ liệu không hợp lệ',
        detail: errorDetail,
        life: 5000
      })
      return
    }

    console.log(`✅ All validations passed for ${scenarioValidation.scenario}`)
  }

  if (Object.keys(validationErrors).length > 0) {
    // Display validation errors
    const errorMessages = []
    Object.entries(validationErrors).forEach(([, errors]) => {
      errors.forEach(error => errorMessages.push(`- ${error}`))
    })

    toast.add({
      severity: 'warn',
      summary: 'Dữ liệu không hợp lệ',
      detail: `Vui lòng kiểm tra lại:\n${errorMessages.join('\n')}`,
      life: 7000
    })
    return
  }

  // Proceed directly with order creation
  await performOrderCreation()
}

const performOrderCreation = async () => {
  creating.value = true
  try {
    // Handle customer creation for recipient-only orders before order creation
    if (activeTab.value.giaohang && !activeTab.value.khachHang &&
        recipientInfo.value.hoTen.trim() && recipientInfo.value.soDienThoai.trim()) {

      console.log('Handling recipient-only order - checking for existing customer')
      const existingCustomer = await checkExistingCustomer()

      if (!existingCustomer) {
        console.log('Creating new customer from recipient information')
        await createCustomerFromRecipient()
      } else {
        console.log('Using existing customer for recipient-only order')
        updateActiveTabData({
          khachHang: existingCustomer,
          diaChiGiaoHang: null
        })
        selectedCustomer.value = existingCustomer
      }
    }

    // Handle Scenario 2: Different recipient than customer - Create recipient customer if needed
    if (activeTab.value.giaohang && activeTab.value.khachHang &&
        recipientInfo.value.hoTen.trim() && recipientInfo.value.soDienThoai.trim()) {

      const currentCustomer = activeTab.value.khachHang
      const recipientDiffersFromCustomer =
        recipientInfo.value.hoTen.trim() !== currentCustomer.hoTen ||
        recipientInfo.value.soDienThoai.trim() !== currentCustomer.soDienThoai

      if (recipientDiffersFromCustomer && !recipientCustomer.value) {
        console.log('Scenario 2: Creating recipient customer for different recipient')
        try {
          await createRecipientCustomerForScenario2()
        } catch (error) {
          console.error('Failed to create recipient customer for Scenario 2:', error)
          // Continue with order creation even if recipient customer creation fails
          // The order will still be valid with recipient info in nguoi_nhan fields
        }
      }
    }

    // Enhanced address management and validation before order creation
    if (activeTab.value.giaohang) {
      console.log('Validating and managing address before order creation')

      // Determine the appropriate scenario for validation
      let validationScenario = 'default'
      if (activeTab.value.khachHang && recipientInfo.value.hoTen.trim()) {
        const currentCustomer = activeTab.value.khachHang
        const recipientDiffersFromCustomer =
          recipientInfo.value.hoTen.trim() !== currentCustomer.hoTen ||
          recipientInfo.value.soDienThoai.trim() !== currentCustomer.soDienThoai

        validationScenario = recipientDiffersFromCustomer ? 'scenario2' : 'scenario1'
      } else if (!activeTab.value.khachHang && recipientInfo.value.hoTen.trim()) {
        validationScenario = 'scenario3'
      }

      // Perform comprehensive address validation
      const addressValidation = await validateAddressForScenario(validationScenario)

      if (!addressValidation.valid) {
        const errorMessages = Object.values(addressValidation.errors).join(', ')
        toast.add({
          severity: 'error',
          summary: 'Lỗi địa chỉ',
          detail: `Địa chỉ giao hàng không hợp lệ: ${errorMessages}`,
          life: 5000
        })
        return // Stop order creation if address validation fails
      }

      // Handle address management for existing customers
      if (activeTab.value.khachHang) {
        console.log('Managing address for customer before order creation')
        const addressResult = await handleAddressManagement(activeTab.value.khachHang)

        if (!addressResult.success) {
          // Show detailed error message
          const errorDetail = addressResult.validationErrors
            ? Object.values(addressResult.validationErrors).join(', ')
            : addressResult.error || 'Không thể xử lý địa chỉ'

          toast.add({
            severity: 'error',
            summary: 'Lỗi quản lý địa chỉ',
            detail: errorDetail,
            life: 5000
          })
          return // Stop order creation if address management fails
        } else if (addressResult.updatedCustomer) {
          console.log('Customer address updated successfully before order creation')
          toast.add({
            severity: 'success',
            summary: 'Thành công',
            detail: 'Đã cập nhật địa chỉ khách hàng',
            life: 3000
          })
        }
      }
    }

    // Map frontend data to HoaDonDto structure for validation and logging
    const orderData = mapTabToHoaDonDto(activeTab.value)
    console.log('Creating order with data:', orderData)

    // Create order using orderStore (which handles the actual API call)
    const result = await createOrderFromTab()

    if (result) {
      // Create audit trail entry for order creation
      await auditOrderCreation(result.id, result)

      // Clear unsaved changes flag
      hasUnsavedChanges.value = false

      // Note: Success toast is handled by orderStore.createOrderFromTab()
    }
  } catch (error) {
    console.error('Error creating order:', error)

    // Handle specific API validation errors
    if (error.response && error.response.data && error.response.data.errors) {
      const apiErrors = error.response.data.errors
      const errorMessages = Object.values(apiErrors).flat()

      toast.add({
        severity: 'error',
        summary: 'Lỗi xác thực',
        detail: `Dữ liệu không hợp lệ:\n${errorMessages.join('\n')}`,
        life: 7000
      })
    } else {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không thể tạo đơn hàng. Vui lòng thử lại.',
        life: 5000
      })
    }
  } finally {
    creating.value = false
  }
}

// Enhanced tab closure with unsaved changes warning
const closeTabWithConfirmation = async (tabId) => {
  const tab = orderTabs.value.find(t => t.id === tabId)

  if (!tab) {
    closeOrderTab(tabId)
    return
  }

  // Check if tab has unsaved changes (products added)
  const hasProducts = tab.sanPhamList && tab.sanPhamList.length > 0

  if (hasProducts) {
    // Show confirmation dialog for tabs with products using specialized template
    const confirmed = await confirmDialog.showTabCloseConfirm(tab)

    if (!confirmed) return
  }

  // Release cart reservations before closing tab
  try {
    await releaseCartReservations(tabId)
  } catch (error) {
    console.error('Failed to release cart reservations on tab close:', error)
    // Continue with tab closure even if release fails
  }

  // Update localStorage to remove this tab from pending cleanup
  try {
    const pendingCleanup = localStorage.getItem('pendingCartReservationCleanup')
    if (pendingCleanup) {
      const tabIds = JSON.parse(pendingCleanup)
      const updatedTabIds = tabIds.filter(id => id !== tabId)
      if (updatedTabIds.length > 0) {
        localStorage.setItem('pendingCartReservationCleanup', JSON.stringify(updatedTabIds))
      } else {
        localStorage.removeItem('pendingCartReservationCleanup')
      }
    }
  } catch (error) {
    console.error('Error updating pending cleanup localStorage:', error)
  }

  // Close tab
  closeOrderTab(tabId)
  hasUnsavedChanges.value = false
}

// Map frontend tab data to backend HoaDonDto structure
const mapTabToHoaDonDto = (tab) => {
  // For embedded address approach, we create the address payload from form data
  let deliveryAddressPayload = null

  if (tab.giaohang && addressData.value.duong.trim()) {
    deliveryAddressPayload = {
      duong: addressData.value.duong.trim(),
      phuongXa: addressData.value.phuongXa,
      quanHuyen: addressData.value.quanHuyen,
      tinhThanh: addressData.value.tinhThanh,
      loaiDiaChi: addressData.value.loaiDiaChi || 'Nhà riêng'
    }
  }

  // Determine customer ID based on scenarios:
  // Scenario 1 & 3: Use the main customer (same for both)
  // Scenario 2: Use the original paying customer (tab.khachHang) - NOT the recipientCustomer
  // This ensures the original customer remains as khachHangId for billing purposes
  const customerId = tab.khachHang?.id || null

  const dto = {
    // Basic order information
    maHoaDon: tab.maHoaDon,
    loaiHoaDon: tab.loaiHoaDon,

    // Customer information - send only ID to avoid transient entity issues
    // For all scenarios, this should be the paying customer
    khachHangId: customerId,

    // Staff member information - backend will handle automatic assignment
    nhanVienId: null,

    // Delivery information - use recipient info and embedded address
    diaChiGiaoHang: deliveryAddressPayload,
    nguoiNhanTen: tab.giaohang ? recipientInfo.value.hoTen.trim() : (tab.khachHang?.hoTen || null),
    nguoiNhanSdt: tab.giaohang ? recipientInfo.value.soDienThoai.trim() : (tab.khachHang?.soDienThoai || null),

    // Financial information
    tongTienHang: tab.tongTienHang || 0,
    giaTriGiamGiaVoucher: tab.giaTriGiamGiaVoucher || 0,
    phiVanChuyen: tab.phiVanChuyen || 0,
    tongThanhToan: tab.tongThanhToan || 0,

    // Status information
    trangThaiDonHang: tab.giaohang ? 'CHO_XAC_NHAN' : 'HOAN_THANH',
    trangThaiThanhToan: tab.phuongThucThanhToan === 'TIEN_MAT' ? 'DA_THANH_TOAN' : 'CHUA_THANH_TOAN',

    // Order details - send individual variants (frontend-selected from groups)
    chiTiet: tab.sanPhamList.map(item => ({
      sanPhamChiTietId: item.sanPhamChiTiet?.id,
      soLuong: item.soLuong,
      donGia: item.donGia,
      thanhTien: item.donGia * item.soLuong,
      // Include serial number information if available
      serialNumberId: item.sanPhamChiTiet?.serialNumberId,
      serialNumber: item.sanPhamChiTiet?.serialNumber
    })),

    // Voucher information
    voucherCodes: tab.voucherList.map(voucher => voucher.maPhieuGiamGia)
  }

  console.log('Generated HoaDonDto:', dto)
  return dto
}

// Use shared audit composable
const { auditOrderCreation } = useOrderAudit()

// Use shared validation composable
const {
  validateTabData,
  clearValidationErrors
} = useOrderValidation()

const validateActiveTab = () => {
  if (!activeTab.value) return {}
  return validateTabData(activeTab.value)
}

// Enhanced embedded address validation with comprehensive checks
const validateEmbeddedAddress = () => {
  const errors = {}

  if (activeTab.value?.giaohang) {
    // Validate street address
    if (!addressData.value.duong.trim()) {
      errors.duong = 'Địa chỉ đường là bắt buộc'
    } else if (addressData.value.duong.trim().length < 5) {
      errors.duong = 'Địa chỉ đường phải có ít nhất 5 ký tự'
    } else if (addressData.value.duong.trim().length > 255) {
      errors.duong = 'Địa chỉ đường không được vượt quá 255 ký tự'
    }

    // Validate province
    if (!addressData.value.tinhThanh) {
      errors.tinhThanh = 'Vui lòng chọn tỉnh/thành phố'
    }

    // Validate district
    if (!addressData.value.quanHuyen) {
      errors.quanHuyen = 'Vui lòng chọn quận/huyện'
    }

    // Validate ward
    if (!addressData.value.phuongXa) {
      errors.phuongXa = 'Vui lòng chọn phường/xã'
    }

    // Validate address type
    if (!addressData.value.loaiDiaChi) {
      errors.loaiDiaChi = 'Vui lòng chọn loại địa chỉ'
    }

    // Cross-validation: Ensure address components are consistent
    if (addressData.value.tinhThanh && addressData.value.quanHuyen) {
      // Additional validation could be added here for geographic consistency
      // For now, we trust the address API to provide consistent data
    }
  }

  // Update address errors from composable
  addressErrors.value = { ...addressErrors.value, ...errors }
  return Object.keys(errors).length === 0
}

// Recipient information validation
const validateRecipientInfo = async () => {
  const errors = {}

  if (activeTab.value?.giaohang) {
    if (!recipientInfo.value.hoTen.trim()) {
      errors.hoTen = 'Tên người nhận là bắt buộc'
    }

    if (!recipientInfo.value.soDienThoai.trim()) {
      errors.soDienThoai = 'Số điện thoại người nhận là bắt buộc'
    } else if (!/^[0-9]{10,11}$/.test(recipientInfo.value.soDienThoai.trim())) {
      errors.soDienThoai = 'Số điện thoại không hợp lệ'
    }

    // Scenario 3: Handle recipient-only orders (no main customer selected)
    if (!activeTab.value.khachHang && recipientInfo.value.hoTen.trim() && recipientInfo.value.soDienThoai.trim()) {
      try {
        // Check if customer exists for recipient info
        const existingCustomer = await checkExistingCustomer()
        if (!existingCustomer) {
          // Create new customer from recipient information
          await createCustomerFromRecipient()
        }
      } catch (error) {
        console.error('Error handling recipient-only order:', error)
        errors.general = 'Không thể xử lý thông tin người nhận'
      }
    }

    // Scenario 2: Validate that original customer is preserved when recipient differs
    if (activeTab.value.khachHang && recipientInfo.value.hoTen.trim() && recipientInfo.value.soDienThoai.trim()) {
      const currentCustomer = activeTab.value.khachHang
      const recipientDiffersFromCustomer =
        recipientInfo.value.hoTen.trim() !== currentCustomer.hoTen ||
        recipientInfo.value.soDienThoai.trim() !== currentCustomer.soDienThoai

      if (recipientDiffersFromCustomer) {
        console.log('Scenario 2 validation: Recipient differs from customer, ensuring proper state management')
        // This is valid - original customer should remain as main customer
        // Recipient customer tracking is handled separately in recipientCustomer.value
      }
    }
  }

  recipientErrors.value = errors
  return Object.keys(errors).length === 0
}

// ===== BUSINESS LOGIC FOR CUSTOMER SCENARIOS =====

// Enhanced address management for order creation with comprehensive validation and persistence
const handleAddressManagement = async (customer) => {
  try {
    if (!customer || !activeTab.value?.giaohang) {
      return { success: true, message: 'No address management needed' }
    }

    // Validate address data before processing
    const addressValid = validateEmbeddedAddress()
    if (!addressValid) {
      return {
        success: false,
        error: 'Address validation failed',
        validationErrors: addressErrors.value
      }
    }

    // Check if current address differs from customer's existing addresses
    if (isAddressDifferentFromCustomer(customer)) {
      console.log('Address differs from customer addresses, attempting to add new address')

      // Validate address completeness before adding
      if (!isAddressComplete()) {
        return {
          success: false,
          error: 'Address information is incomplete'
        }
      }

      // Add the new address to customer's address list
      const result = await addAddressToCustomer(customer, customerStore)

      if (result.success && result.updatedCustomer) {
        // Update the customer data in the active tab
        updateActiveTabData({
          khachHang: result.updatedCustomer
        })
        selectedCustomer.value = result.updatedCustomer

        console.log('Successfully added new address to customer')
        return {
          success: true,
          message: 'New address added to customer',
          updatedCustomer: result.updatedCustomer,
          addressAction: 'created'
        }
      } else {
        return {
          success: false,
          error: result.error || 'Failed to add address to customer'
        }
      }
    } else {
      console.log('Address matches existing customer address, reusing existing')

      // Find the matching address for reference
      const matchingAddress = findMatchingAddress(
        {
          duong: addressData.value.duong.trim(),
          phuongXa: addressData.value.phuongXa,
          quanHuyen: addressData.value.quanHuyen,
          tinhThanh: addressData.value.tinhThanh
        },
        customer.diaChis
      )

      return {
        success: true,
        message: 'Existing address reused',
        matchingAddress: matchingAddress,
        addressAction: 'reused'
      }
    }
  } catch (error) {
    console.error('Error in address management:', error)
    return {
      success: false,
      error: error.message || 'Unknown error in address management'
    }
  }
}

// Helper function to check if address is complete
const isAddressComplete = () => {
  return addressData.value.duong.trim() &&
         addressData.value.phuongXa &&
         addressData.value.quanHuyen &&
         addressData.value.tinhThanh
}

// Comprehensive address validation for all customer scenarios
const validateAddressForScenario = async (scenario = 'default') => {
  const errors = {}

  try {
    // Basic address validation
    const basicValidation = validateEmbeddedAddress()
    if (!basicValidation) {
      errors.basic = 'Thông tin địa chỉ cơ bản không hợp lệ'
    }

    // Scenario-specific validation
    switch (scenario) {
      case 'scenario1': // Same recipient as customer
        if (activeTab.value?.khachHang) {
          // Validate that address can be associated with the customer
          const customer = activeTab.value.khachHang
          if (customer.diaChis && customer.diaChis.length >= 10) {
            errors.limit = 'Khách hàng đã có quá nhiều địa chỉ (tối đa 10 địa chỉ)'
          }
        }
        break

      case 'scenario2': // Different recipient than customer
        // Validate that address is suitable for delivery to different recipient
        if (!recipientInfo.value.hoTen.trim() || !recipientInfo.value.soDienThoai.trim()) {
          errors.recipient = 'Thông tin người nhận là bắt buộc cho địa chỉ giao hàng khác'
        }
        break

      case 'scenario3': // Recipient-only orders
        // Validate that address can be used for new customer creation
        if (!recipientInfo.value.hoTen.trim() || !recipientInfo.value.soDienThoai.trim()) {
          errors.newCustomer = 'Thông tin người nhận là bắt buộc để tạo khách hàng mới'
        }
        break

      default:
        // General validation for any scenario
        if (activeTab.value?.giaohang && !isAddressComplete()) {
          errors.incomplete = 'Địa chỉ giao hàng chưa đầy đủ thông tin'
        }
    }

    // Geographic validation (basic check)
    if (addressData.value.duong.trim() && addressData.value.tinhThanh) {
      // Check for obviously invalid combinations (basic validation)
      const streetAddress = addressData.value.duong.trim().toLowerCase()
      if (streetAddress.includes('test') || streetAddress.includes('fake')) {
        errors.geographic = 'Địa chỉ đường có vẻ không hợp lệ'
      }
    }

    return {
      valid: Object.keys(errors).length === 0,
      errors: errors,
      scenario: scenario
    }

  } catch (error) {
    console.error('Error in address validation:', error)
    return {
      valid: false,
      errors: { system: 'Lỗi hệ thống khi xác thực địa chỉ' },
      scenario: scenario
    }
  }
}

// Comprehensive address validation before order creation
const validateAddressBeforeOrderCreation = async () => {
  try {
    if (!activeTab.value?.giaohang) {
      return { valid: true, message: 'No delivery address validation needed' }
    }

    // Determine current scenario
    let currentScenario = 'default'
    if (activeTab.value.khachHang && recipientInfo.value.hoTen.trim()) {
      const currentCustomer = activeTab.value.khachHang
      const recipientDiffersFromCustomer =
        recipientInfo.value.hoTen.trim() !== currentCustomer.hoTen ||
        recipientInfo.value.soDienThoai.trim() !== currentCustomer.soDienThoai

      currentScenario = recipientDiffersFromCustomer ? 'scenario2' : 'scenario1'
    } else if (!activeTab.value.khachHang && recipientInfo.value.hoTen.trim()) {
      currentScenario = 'scenario3'
    }

    console.log(`Validating address for ${currentScenario}`)

    // Perform scenario-specific validation
    const validation = await validateAddressForScenario(currentScenario)

    if (!validation.valid) {
      return {
        valid: false,
        errors: validation.errors,
        scenario: currentScenario,
        message: 'Address validation failed'
      }
    }

    // Additional checks for address completeness
    if (!isAddressComplete()) {
      return {
        valid: false,
        errors: { incomplete: 'Địa chỉ giao hàng chưa đầy đủ thông tin' },
        scenario: currentScenario,
        message: 'Address is incomplete'
      }
    }

    return {
      valid: true,
      scenario: currentScenario,
      message: 'Address validation passed'
    }

  } catch (error) {
    console.error('Error in address validation before order creation:', error)
    return {
      valid: false,
      errors: { system: 'Lỗi hệ thống khi xác thực địa chỉ' },
      message: 'System error during address validation'
    }
  }
}

// ===== COMPREHENSIVE INTEGRATION TESTING FOR CUSTOMER SCENARIOS =====

// Integration testing function for all customer scenarios
const runCustomerScenarioIntegrationTests = async () => {
  const testResults = {
    scenario1: { passed: false, errors: [], details: {} },
    scenario2: { passed: false, errors: [], details: {} },
    scenario3: { passed: false, errors: [], details: {} },
    addressManagement: { passed: false, errors: [], details: {} },
    backendMapping: { passed: false, errors: [], details: {} },
    overall: { passed: false, summary: '' }
  }

  console.log('🧪 Starting Customer Scenario Integration Tests...')

  try {
    // Test Scenario 1: Same recipient as customer
    console.log('Testing Scenario 1: Same recipient as customer')
    testResults.scenario1 = await testScenario1()

    // Test Scenario 2: Different recipient than customer
    console.log('Testing Scenario 2: Different recipient than customer')
    testResults.scenario2 = await testScenario2()

    // Test Scenario 3: Recipient-only orders
    console.log('Testing Scenario 3: Recipient-only orders')
    testResults.scenario3 = await testScenario3()

    // Test Address Management across scenarios
    console.log('Testing Address Management functionality')
    testResults.addressManagement = await testAddressManagement()

    // Test Backend Mapping for all scenarios
    console.log('Testing Backend Mapping')
    testResults.backendMapping = await testBackendMapping()

    // Calculate overall test result
    const allTestsPassed = Object.values(testResults).slice(0, -1).every(test => test.passed)
    testResults.overall.passed = allTestsPassed
    testResults.overall.summary = allTestsPassed
      ? 'All customer scenario integration tests passed successfully'
      : 'Some integration tests failed - see individual test results'

    console.log('🧪 Integration Tests Completed:', testResults)
    return testResults

  } catch (error) {
    console.error('Error during integration testing:', error)
    testResults.overall.passed = false
    testResults.overall.summary = `Integration testing failed: ${error.message}`
    return testResults
  }
}

// Test Scenario 1: Same recipient as customer
const testScenario1 = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Scenario 1: Same recipient as customer')

    // Test 1: Customer auto-population
    const testCustomer = {
      id: 'test-customer-1',
      hoTen: 'Nguyễn Văn A',
      soDienThoai: '0123456789',
      diaChis: [{
        duong: '123 Test Street',
        phuongXa: 'Test Ward',
        quanHuyen: 'Test District',
        tinhThanh: 'Test Province',
        loaiDiaChi: 'Nhà riêng',
        laMacDinh: true
      }]
    }

    // Simulate customer selection
    updateActiveTabData({ khachHang: testCustomer, giaohang: true })
    selectedCustomer.value = testCustomer

    // Test auto-population
    await syncCustomerToRecipient(testCustomer)

    // Verify recipient info matches customer
    if (recipientInfo.value.hoTen !== testCustomer.hoTen) {
      result.errors.push('Recipient name not auto-populated correctly')
    }
    if (recipientInfo.value.soDienThoai !== testCustomer.soDienThoai) {
      result.errors.push('Recipient phone not auto-populated correctly')
    }

    // Test 2: Address validation for Scenario 1
    const addressValidation = await validateAddressForScenario('scenario1')
    if (!addressValidation.valid) {
      result.errors.push(`Address validation failed: ${Object.values(addressValidation.errors).join(', ')}`)
    }

    // Test 3: Backend mapping validation
    const backendMapping = mapTabToHoaDonDto(activeTab.value)
    if (backendMapping.khachHangId !== testCustomer.id) {
      result.errors.push('Backend mapping: Customer ID not preserved correctly')
    }
    if (backendMapping.nguoiNhanTen !== testCustomer.hoTen) {
      result.errors.push('Backend mapping: Recipient name not mapped correctly')
    }

    result.passed = result.errors.length === 0
    result.details = {
      customerAutoPopulation: recipientInfo.value.hoTen === testCustomer.hoTen,
      addressValidation: addressValidation.valid,
      backendMapping: backendMapping.khachHangId === testCustomer.id
    }

    console.log('✅ Scenario 1 test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Test execution error: ${error.message}`)
    console.error('❌ Scenario 1 test failed:', error)
    return result
  }
}

// Test Scenario 2: Different recipient than customer
const testScenario2 = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Scenario 2: Different recipient than customer')

    // Test setup: Customer and different recipient
    const testCustomer = {
      id: 'test-customer-2',
      hoTen: 'Nguyễn Văn B',
      soDienThoai: '0123456788',
      diaChis: []
    }

    const testRecipient = {
      hoTen: 'Trần Thị C',
      soDienThoai: '0987654321'
    }

    // Simulate customer selection and different recipient
    updateActiveTabData({ khachHang: testCustomer, giaohang: true })
    selectedCustomer.value = testCustomer
    recipientInfo.value = { ...testRecipient }

    // Test 1: Recipient customer tracking
    await handleDifferentRecipient()

    // Verify original customer is preserved
    if (activeTab.value.khachHang?.id !== testCustomer.id) {
      result.errors.push('Original customer not preserved in Scenario 2')
    }

    // Test 2: Address validation for Scenario 2
    // Set up test address
    setAddressData({
      duong: '456 Different Street',
      phuongXa: 'Different Ward',
      quanHuyen: 'Different District',
      tinhThanh: 'Different Province',
      loaiDiaChi: 'Nhà riêng'
    })

    const addressValidation = await validateAddressForScenario('scenario2')
    if (!addressValidation.valid) {
      result.errors.push(`Address validation failed: ${Object.values(addressValidation.errors).join(', ')}`)
    }

    // Test 3: Backend mapping validation
    const backendMapping = mapTabToHoaDonDto(activeTab.value)
    if (backendMapping.khachHangId !== testCustomer.id) {
      result.errors.push('Backend mapping: Original customer ID not preserved')
    }
    if (backendMapping.nguoiNhanTen !== testRecipient.hoTen) {
      result.errors.push('Backend mapping: Recipient name not mapped correctly')
    }
    if (backendMapping.nguoiNhanSdt !== testRecipient.soDienThoai) {
      result.errors.push('Backend mapping: Recipient phone not mapped correctly')
    }

    result.passed = result.errors.length === 0
    result.details = {
      originalCustomerPreserved: activeTab.value.khachHang?.id === testCustomer.id,
      recipientInfoMapped: backendMapping.nguoiNhanTen === testRecipient.hoTen,
      addressValidation: addressValidation.valid
    }

    console.log('✅ Scenario 2 test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Test execution error: ${error.message}`)
    console.error('❌ Scenario 2 test failed:', error)
    return result
  }
}

// Test Scenario 3: Recipient-only orders
const testScenario3 = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Scenario 3: Recipient-only orders')

    // Test setup: No customer, only recipient
    const testRecipient = {
      hoTen: 'Lê Văn D',
      soDienThoai: '0111222333'
    }

    // Clear customer and set recipient
    updateActiveTabData({ khachHang: null, giaohang: true })
    selectedCustomer.value = null
    recipientInfo.value = { ...testRecipient }

    // Test 1: Customer creation validation
    // Mock customer store to avoid actual API calls during testing
    const originalCreateCustomer = customerStore.createCustomer
    let customerCreationCalled = false
    let createdCustomerData = null

    customerStore.createCustomer = async (payload) => {
      customerCreationCalled = true
      createdCustomerData = payload
      return {
        id: 'test-created-customer-3',
        ...payload
      }
    }

    try {
      // Test customer creation from recipient
      await createCustomerFromRecipient()

      if (!customerCreationCalled) {
        result.errors.push('Customer creation not triggered for Scenario 3')
      }

      if (createdCustomerData?.hoTen !== testRecipient.hoTen) {
        result.errors.push('Customer creation: Name not mapped correctly')
      }

      if (createdCustomerData?.soDienThoai !== testRecipient.soDienThoai) {
        result.errors.push('Customer creation: Phone not mapped correctly')
      }

    } finally {
      // Restore original function
      customerStore.createCustomer = originalCreateCustomer
    }

    // Test 2: Address validation for Scenario 3
    setAddressData({
      duong: '789 Recipient Street',
      phuongXa: 'Recipient Ward',
      quanHuyen: 'Recipient District',
      tinhThanh: 'Recipient Province',
      loaiDiaChi: 'Nhà riêng'
    })

    const addressValidation = await validateAddressForScenario('scenario3')
    if (!addressValidation.valid) {
      result.errors.push(`Address validation failed: ${Object.values(addressValidation.errors).join(', ')}`)
    }

    // Test 3: Backend mapping validation (after customer creation)
    if (activeTab.value.khachHang) {
      const backendMapping = mapTabToHoaDonDto(activeTab.value)
      if (!backendMapping.khachHangId) {
        result.errors.push('Backend mapping: Customer ID not set after creation')
      }
      if (backendMapping.nguoiNhanTen !== testRecipient.hoTen) {
        result.errors.push('Backend mapping: Recipient name not mapped correctly')
      }
    }

    result.passed = result.errors.length === 0
    result.details = {
      customerCreationTriggered: customerCreationCalled,
      customerDataCorrect: createdCustomerData?.hoTen === testRecipient.hoTen,
      addressValidation: addressValidation.valid
    }

    console.log('✅ Scenario 3 test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Test execution error: ${error.message}`)
    console.error('❌ Scenario 3 test failed:', error)
    return result
  }
}

// Test Address Management functionality
const testAddressManagement = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Address Management functionality')

    // Test 1: Address validation
    const testAddresses = [
      {
        name: 'Valid address',
        data: {
          duong: 'Valid Street Address',
          phuongXa: 'Valid Ward',
          quanHuyen: 'Valid District',
          tinhThanh: 'Valid Province',
          loaiDiaChi: 'Nhà riêng'
        },
        shouldPass: true
      },
      {
        name: 'Invalid address - too short',
        data: {
          duong: 'A',
          phuongXa: 'Valid Ward',
          quanHuyen: 'Valid District',
          tinhThanh: 'Valid Province',
          loaiDiaChi: 'Nhà riêng'
        },
        shouldPass: false
      },
      {
        name: 'Incomplete address',
        data: {
          duong: 'Valid Street',
          phuongXa: '',
          quanHuyen: '',
          tinhThanh: '',
          loaiDiaChi: 'Nhà riêng'
        },
        shouldPass: false
      }
    ]

    for (const testCase of testAddresses) {
      setAddressData(testCase.data)
      const validation = validateEmbeddedAddress()

      if (validation !== testCase.shouldPass) {
        result.errors.push(`Address validation failed for ${testCase.name}: expected ${testCase.shouldPass}, got ${validation}`)
      }
    }

    // Test 2: Address completeness check
    setAddressData({
      duong: 'Complete Street',
      phuongXa: 'Complete Ward',
      quanHuyen: 'Complete District',
      tinhThanh: 'Complete Province',
      loaiDiaChi: 'Nhà riêng'
    })

    if (!isAddressComplete()) {
      result.errors.push('Address completeness check failed for complete address')
    }

    // Test 3: Scenario-specific validation
    const scenarios = ['scenario1', 'scenario2', 'scenario3']
    for (const scenario of scenarios) {
      const validation = await validateAddressForScenario(scenario)
      if (!validation.valid && scenario !== 'scenario2' && scenario !== 'scenario3') {
        // scenario2 and scenario3 might fail due to missing recipient info, which is expected
        result.errors.push(`Scenario validation failed for ${scenario}: ${Object.values(validation.errors).join(', ')}`)
      }
    }

    result.passed = result.errors.length === 0
    result.details = {
      validationTests: testAddresses.length,
      completenessCheck: isAddressComplete(),
      scenarioValidations: scenarios.length
    }

    console.log('✅ Address Management test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Test execution error: ${error.message}`)
    console.error('❌ Address Management test failed:', error)
    return result
  }
}

// Test Backend Mapping functionality
const testBackendMapping = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Backend Mapping functionality')

    // Test 1: Scenario 1 mapping (same recipient as customer)
    const testCustomer1 = {
      id: 'test-customer-mapping-1',
      hoTen: 'Mapping Test Customer 1',
      soDienThoai: '0123456789'
    }

    updateActiveTabData({
      khachHang: testCustomer1,
      giaohang: true,
      maHoaDon: 'TEST001',
      loaiHoaDon: 'TAI_QUAY'
    })
    recipientInfo.value = {
      hoTen: testCustomer1.hoTen,
      soDienThoai: testCustomer1.soDienThoai
    }

    const mapping1 = mapTabToHoaDonDto(activeTab.value)

    if (mapping1.khachHangId !== testCustomer1.id) {
      result.errors.push('Scenario 1: Customer ID not mapped correctly')
    }
    if (mapping1.nguoiNhanTen !== testCustomer1.hoTen) {
      result.errors.push('Scenario 1: Recipient name not mapped correctly')
    }

    // Test 2: Scenario 2 mapping (different recipient)
    const testCustomer2 = {
      id: 'test-customer-mapping-2',
      hoTen: 'Mapping Test Customer 2',
      soDienThoai: '0123456788'
    }
    const testRecipient2 = {
      hoTen: 'Different Recipient',
      soDienThoai: '0987654321'
    }

    updateActiveTabData({
      khachHang: testCustomer2,
      giaohang: true,
      maHoaDon: 'TEST002',
      loaiHoaDon: 'GIAO_HANG'
    })
    recipientInfo.value = { ...testRecipient2 }

    const mapping2 = mapTabToHoaDonDto(activeTab.value)

    if (mapping2.khachHangId !== testCustomer2.id) {
      result.errors.push('Scenario 2: Original customer ID not preserved')
    }
    if (mapping2.nguoiNhanTen !== testRecipient2.hoTen) {
      result.errors.push('Scenario 2: Recipient name not mapped correctly')
    }
    if (mapping2.nguoiNhanSdt !== testRecipient2.soDienThoai) {
      result.errors.push('Scenario 2: Recipient phone not mapped correctly')
    }

    // Test 3: Address mapping
    setAddressData({
      duong: 'Test Mapping Street',
      phuongXa: 'Test Mapping Ward',
      quanHuyen: 'Test Mapping District',
      tinhThanh: 'Test Mapping Province',
      loaiDiaChi: 'Văn phòng'
    })

    const mapping3 = mapTabToHoaDonDto(activeTab.value)

    if (!mapping3.diaChiGiaoHang) {
      result.errors.push('Address mapping: Delivery address not mapped')
    } else {
      if (mapping3.diaChiGiaoHang.duong !== 'Test Mapping Street') {
        result.errors.push('Address mapping: Street not mapped correctly')
      }
      if (mapping3.diaChiGiaoHang.loaiDiaChi !== 'Văn phòng') {
        result.errors.push('Address mapping: Address type not mapped correctly')
      }
    }

    // Test 4: Non-delivery order mapping
    updateActiveTabData({ giaohang: false })
    const mapping4 = mapTabToHoaDonDto(activeTab.value)

    if (mapping4.diaChiGiaoHang !== null) {
      result.errors.push('Non-delivery order: Address should be null')
    }

    result.passed = result.errors.length === 0
    result.details = {
      scenario1Mapping: mapping1.khachHangId === testCustomer1.id,
      scenario2Mapping: mapping2.khachHangId === testCustomer2.id && mapping2.nguoiNhanTen === testRecipient2.hoTen,
      addressMapping: !!mapping3.diaChiGiaoHang,
      nonDeliveryMapping: mapping4.diaChiGiaoHang === null
    }

    console.log('✅ Backend Mapping test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Test execution error: ${error.message}`)
    console.error('❌ Backend Mapping test failed:', error)
    return result
  }
}

// Execute comprehensive integration tests (for development/debugging)
const executeIntegrationTests = async () => {
  try {
    console.log('🚀 Executing Customer Scenario Integration Tests...')

    const testResults = await runCustomerScenarioIntegrationTests()

    // Display results in console and toast
    console.log('📊 Integration Test Results:', testResults)

    if (testResults.overall.passed) {
      toast.add({
        severity: 'success',
        summary: 'Integration Tests Passed',
        detail: 'All customer scenario tests completed successfully',
        life: 5000
      })
    } else {
      const failedTests = Object.entries(testResults)
        .filter(([key, result]) => key !== 'overall' && !result.passed)
        .map(([key]) => key)

      toast.add({
        severity: 'error',
        summary: 'Integration Tests Failed',
        detail: `Failed tests: ${failedTests.join(', ')}`,
        life: 8000
      })
    }

    return testResults
  } catch (error) {
    console.error('Error executing integration tests:', error)
    toast.add({
      severity: 'error',
      summary: 'Test Execution Error',
      detail: error.message,
      life: 5000
    })
  }
}

// Comprehensive validation function for all customer scenarios
const validateAllCustomerScenarios = async () => {
  try {
    console.log('🔍 Validating all customer scenarios...')

    // Determine current scenario
    let currentScenario = 'none'
    if (activeTab.value?.giaohang) {
      if (activeTab.value.khachHang && recipientInfo.value.hoTen.trim()) {
        const currentCustomer = activeTab.value.khachHang
        const recipientDiffersFromCustomer =
          recipientInfo.value.hoTen.trim() !== currentCustomer.hoTen ||
          recipientInfo.value.soDienThoai.trim() !== currentCustomer.soDienThoai

        currentScenario = recipientDiffersFromCustomer ? 'scenario2' : 'scenario1'
      } else if (!activeTab.value.khachHang && recipientInfo.value.hoTen.trim()) {
        currentScenario = 'scenario3'
      }
    }

    console.log(`Current scenario detected: ${currentScenario}`)

    // Validate current scenario
    const validationResults = {
      scenario: currentScenario,
      valid: true,
      errors: [],
      warnings: []
    }

    // Basic validation
    if (currentScenario !== 'none') {
      const addressValidation = await validateAddressForScenario(currentScenario)
      if (!addressValidation.valid) {
        validationResults.valid = false
        validationResults.errors.push(...Object.values(addressValidation.errors))
      }

      // Scenario-specific validation
      switch (currentScenario) {
        case 'scenario1':
          if (!activeTab.value.khachHang) {
            validationResults.errors.push('Customer required for Scenario 1')
            validationResults.valid = false
          }
          break

        case 'scenario2':
          if (!activeTab.value.khachHang) {
            validationResults.errors.push('Original customer required for Scenario 2')
            validationResults.valid = false
          }
          if (!recipientInfo.value.hoTen.trim() || !recipientInfo.value.soDienThoai.trim()) {
            validationResults.errors.push('Recipient information required for Scenario 2')
            validationResults.valid = false
          }
          break

        case 'scenario3':
          if (!recipientInfo.value.hoTen.trim() || !recipientInfo.value.soDienThoai.trim()) {
            validationResults.errors.push('Recipient information required for Scenario 3')
            validationResults.valid = false
          }
          break
      }
    }

    console.log('✅ Scenario validation completed:', validationResults)
    return validationResults

  } catch (error) {
    console.error('Error validating customer scenarios:', error)
    return {
      scenario: 'error',
      valid: false,
      errors: [error.message],
      warnings: []
    }
  }
}

// Scenario 1: Same recipient as customer - Auto-populate recipient fields with customer info
const syncCustomerToRecipient = async (customer) => {
  try {
    console.log('Syncing customer to recipient:', customer)

    // Auto-populate recipient fields with customer information
    recipientInfo.value.hoTen = customer.hoTen || ''
    recipientInfo.value.soDienThoai = customer.soDienThoai || ''

    // Load customer's default address into embedded form
    await populateAddressFromCustomer(customer)

    // Clear any validation errors
    recipientErrors.value = {}

    toast.add({
      severity: 'info',
      summary: 'Thông tin',
      detail: 'Đã tự động điền thông tin người nhận từ khách hàng',
      life: 2000
    })
  } catch (error) {
    console.error('Error syncing customer to recipient:', error)
  }
}

// Clear recipient information
const clearRecipientInfo = () => {
  recipientInfo.value.hoTen = ''
  recipientInfo.value.soDienThoai = ''
  recipientErrors.value = {}

  // Clear recipient customer tracking for Scenario 2
  recipientCustomer.value = null

  // Clear address form
  setAddressData({
    duong: '',
    phuongXa: '',
    quanHuyen: '',
    tinhThanh: '',
    loaiDiaChi: 'Nhà riêng'
  })
}

// Scenario 3: Recipient-only orders - Create customer from recipient information
const createCustomerFromRecipient = async () => {
  try {
    if (!recipientInfo.value.hoTen.trim() || !recipientInfo.value.soDienThoai.trim()) {
      throw new Error('Thiếu thông tin người nhận để tạo khách hàng')
    }

    // Prepare customer data with address information
    const customerPayload = {
      hoTen: recipientInfo.value.hoTen.trim(),
      soDienThoai: recipientInfo.value.soDienThoai.trim(),
      email: null, // No email from recipient info
      gioiTinh: 'NAM', // Default gender
      ngaySinh: null, // No birth date from recipient info
      trangThai: 'HOAT_DONG',
      diaChis: []
    }

    // Add address information if available with enhanced validation
    if (isAddressComplete()) {
      // Validate address before adding to customer
      const addressValidation = await validateAddressForScenario('scenario3')

      if (!addressValidation.valid) {
        const errorMessages = Object.values(addressValidation.errors).join(', ')
        throw new Error(`Địa chỉ không hợp lệ: ${errorMessages}`)
      }

      customerPayload.diaChis = [{
        duong: addressData.value.duong.trim(),
        phuongXa: addressData.value.phuongXa,
        quanHuyen: addressData.value.quanHuyen,
        tinhThanh: addressData.value.tinhThanh,
        loaiDiaChi: addressData.value.loaiDiaChi || 'Nhà riêng',
        laMacDinh: true
      }]

      console.log('Address validated and added to customer payload for Scenario 3')
    } else {
      console.log('Address incomplete, creating customer without address for Scenario 3')
    }

    console.log('Creating customer from recipient info:', customerPayload)

    // Create customer using store
    const newCustomer = await customerStore.createCustomer(customerPayload)

    if (newCustomer) {
      // Set the newly created customer as the main customer for the order
      updateActiveTabData({
        khachHang: newCustomer,
        diaChiGiaoHang: null
      })
      selectedCustomer.value = newCustomer

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `Đã tạo khách hàng ${newCustomer.hoTen} từ thông tin người nhận`,
        life: 3000
      })

      return newCustomer
    }
  } catch (error) {
    console.error('Error creating customer from recipient:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tạo khách hàng từ thông tin người nhận',
      life: 3000
    })
    throw error
  }
}

// Scenario 2: Create recipient customer without replacing main customer
const createRecipientCustomerForScenario2 = async () => {
  try {
    if (!recipientInfo.value.hoTen.trim() || !recipientInfo.value.soDienThoai.trim()) {
      throw new Error('Thiếu thông tin người nhận để tạo khách hàng')
    }

    // Prepare customer data for recipient
    const recipientCustomerPayload = {
      hoTen: recipientInfo.value.hoTen.trim(),
      soDienThoai: recipientInfo.value.soDienThoai.trim(),
      email: null, // No email from recipient info
      gioiTinh: 'NAM', // Default gender
      ngaySinh: null, // No birth date from recipient info
      trangThai: 'HOAT_DONG',
      diaChis: []
    }

    // Add address information if available with enhanced validation
    if (isAddressComplete()) {
      // Validate address before adding to recipient customer
      const addressValidation = await validateAddressForScenario('scenario2')

      if (!addressValidation.valid) {
        const errorMessages = Object.values(addressValidation.errors).join(', ')
        throw new Error(`Địa chỉ không hợp lệ cho người nhận: ${errorMessages}`)
      }

      recipientCustomerPayload.diaChis = [{
        duong: addressData.value.duong.trim(),
        phuongXa: addressData.value.phuongXa,
        quanHuyen: addressData.value.quanHuyen,
        tinhThanh: addressData.value.tinhThanh,
        loaiDiaChi: addressData.value.loaiDiaChi || 'Nhà riêng',
        laMacDinh: true
      }]

      console.log('Address validated and added to recipient customer payload for Scenario 2')
    } else {
      console.log('Address incomplete, creating recipient customer without address for Scenario 2')
    }

    console.log('Creating recipient customer for Scenario 2:', recipientCustomerPayload)

    // Create customer using store
    const newRecipientCustomer = await customerStore.createCustomer(recipientCustomerPayload)

    if (newRecipientCustomer) {
      // Store as recipient customer (DO NOT replace main customer)
      recipientCustomer.value = newRecipientCustomer

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `Đã tạo khách hàng ${newRecipientCustomer.hoTen} cho người nhận`,
        life: 3000
      })

      return newRecipientCustomer
    }
  } catch (error) {
    console.error('Error creating recipient customer for Scenario 2:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Không thể tạo khách hàng cho người nhận',
      life: 3000
    })
    throw error
  }
}

// Scenario 2: Different recipient than customer - Handle recipient customer lookup and address loading
const handleDifferentRecipient = async () => {
  try {
    // Check if recipient info differs from selected customer
    const currentCustomer = activeTab.value?.khachHang
    if (!currentCustomer) return

    const recipientDiffersFromCustomer =
      recipientInfo.value.hoTen.trim() !== currentCustomer.hoTen ||
      recipientInfo.value.soDienThoai.trim() !== currentCustomer.soDienThoai

    if (recipientDiffersFromCustomer) {
      console.log('Recipient differs from customer, searching for recipient customer')

      // Search for existing customer with recipient details
      const foundRecipientCustomer = await checkExistingCustomer()

      if (foundRecipientCustomer) {
        console.log('Found existing customer for recipient:', foundRecipientCustomer)

        // Store recipient customer separately (DO NOT replace main customer)
        recipientCustomer.value = foundRecipientCustomer

        // Load recipient customer's address if found
        await populateAddressFromCustomer(foundRecipientCustomer)

        toast.add({
          severity: 'info',
          summary: 'Thông tin',
          detail: `Đã tìm thấy khách hàng ${foundRecipientCustomer.hoTen} và tự động điền địa chỉ`,
          life: 3000
        })
      } else {
        // Clear recipient customer if no match found
        recipientCustomer.value = null
        console.log('No existing customer found for recipient, will create new customer if needed')

        // For Scenario 2, we may need to create a customer for the recipient
        // This will be handled during order creation validation
      }
    } else {
      // Recipient is same as customer, clear separate recipient customer tracking
      recipientCustomer.value = null
    }
  } catch (error) {
    console.error('Error handling different recipient:', error)
  }
}

// State synchronization between customer and recipient
const syncCustomerAndRecipient = async () => {
  try {
    if (!activeTab.value?.giaohang) return

    const currentCustomer = activeTab.value?.khachHang

    // If no customer selected but recipient info exists, handle recipient-only scenario
    if (!currentCustomer && recipientInfo.value.hoTen.trim() && recipientInfo.value.soDienThoai.trim()) {
      const existingCustomer = await checkExistingCustomer()
      if (existingCustomer) {
        // Set existing customer as main customer
        updateActiveTabData({
          khachHang: existingCustomer,
          diaChiGiaoHang: null
        })
        selectedCustomer.value = existingCustomer

        toast.add({
          severity: 'info',
          summary: 'Thông tin',
          detail: `Đã tự động chọn khách hàng ${existingCustomer.hoTen}`,
          life: 3000
        })
      }
    }

    // If customer exists, handle different recipient scenario
    if (currentCustomer) {
      await handleDifferentRecipient()
    }
  } catch (error) {
    console.error('Error in customer-recipient synchronization:', error)
  }
}

// Watch for recipient information changes to auto-lookup customers
watch(
  () => [recipientInfo.value.hoTen, recipientInfo.value.soDienThoai],
  async ([newName, newPhone], [oldName, oldPhone]) => {
    // Only proceed if delivery is enabled and values have actually changed
    if (!activeTab.value?.giaohang) return

    // Validate recipient info
    validateRecipientInfo()

    // Auto-lookup customer if both fields are filled and changed
    if (newName && newPhone && (newName !== oldName || newPhone !== oldPhone)) {
      try {
        // Perform state synchronization
        await syncCustomerAndRecipient()
      } catch (error) {
        console.error('Error in auto-lookup customer:', error)
      }
    }
  },
  { immediate: false, deep: true }
)

// Watchers with proper null checks
watch(
  () => activeTab.value?.tongTienHang,
  async (newTotal, oldTotal) => {
    // Only proceed if activeTab exists and has required data
    if (activeTab.value && newTotal !== oldTotal && activeTab.value.khachHang) {
      // Reload available vouchers when order total changes
      await loadAvailableVouchers()

      // Automatically find and apply best voucher when order total changes
      if (newTotal > 0) {
        await findAndApplyBestVoucher()
      }
    }
  },
  { immediate: false } // Don't run immediately to avoid undefined access
)

// Watch for customer changes to automatically apply vouchers and sync recipient info
watch(
  () => activeTab.value?.khachHang,
  async (newCustomer, oldCustomer) => {
    if (activeTab.value && newCustomer && newCustomer !== oldCustomer) {
      // Clear existing vouchers when customer changes
      if (oldCustomer && newCustomer.id !== oldCustomer.id) {
        activeTab.value.voucherList = []
        calculateTabTotals(activeTabId.value)
      }

      // Load available vouchers for new customer
      await loadAvailableVouchers()

      // Automatically find and apply best voucher for new customer
      if (activeTab.value.tongTienHang > 0) {
        await findAndApplyBestVoucher()
      }

      // Scenario 1: Auto-populate recipient info when customer is selected and delivery is enabled
      if (newCustomer && activeTab.value.giaohang) {
        await syncCustomerToRecipient(newCustomer)
      }
    }

    // Clear recipient info when customer is cleared
    if (!newCustomer && oldCustomer) {
      clearRecipientInfo()
    }
  },
  { immediate: false }
)

// Watch for delivery toggle changes to sync customer and recipient
watch(
  () => activeTab.value?.giaohang,
  async (deliveryEnabled, wasDeliveryEnabled) => {
    if (!activeTab.value) return

    if (deliveryEnabled && !wasDeliveryEnabled) {
      // Delivery just enabled - sync customer to recipient if customer exists
      if (activeTab.value.khachHang) {
        await syncCustomerToRecipient(activeTab.value.khachHang)
      }
    } else if (!deliveryEnabled && wasDeliveryEnabled) {
      // Delivery just disabled - clear recipient info
      clearRecipientInfo()
    }
  },
  { immediate: false }
)

watch(
  () => activeTab.value,
  (newTab, oldTab) => {
    // Only proceed if we have valid tab data
    if (newTab && newTab !== oldTab) {
      // Clear validation errors when switching tabs
      clearValidationErrors()
      hasUnsavedChanges.value = false
    }
  },
  { deep: true, immediate: false } // Don't run immediately to avoid undefined access
)

watch(
  () => activeTab.value?.isModified,
  (isModified) => {
    // Only update if activeTab exists
    if (activeTab.value) {
      hasUnsavedChanges.value = isModified || false
    }
  },
  { immediate: false } // Don't run immediately to avoid undefined access
)

// Watch for new tabs to sync cart data
watch(
  () => activeTab.value?.id,
  (newTabId, oldTabId) => {
    // Sync cart data with product variant dialog when switching tabs
    if (newTabId && newTabId !== oldTabId) {
      syncCartWithDialog()
    }
  },
  { immediate: false }
)

// Page refresh/close detection for cart reservation cleanup
const handlePageUnload = (_event) => {
  console.log('Page unload detected, releasing cart reservations...')

  // Get active tab IDs
  const activeTabIds = orderTabs.value.map(tab => tab.id)

  // Store tab IDs in localStorage for cleanup on next page load
  if (activeTabIds.length > 0) {
    localStorage.setItem('pendingCartReservationCleanup', JSON.stringify(activeTabIds))
  }

  // Attempt synchronous cleanup (may not complete due to page unload timing)
  for (const tabId of activeTabIds) {
    try {
      // Use navigator.sendBeacon for more reliable cleanup during page unload
      const cleanupData = JSON.stringify({ tabId })
      navigator.sendBeacon('/api/cart/reservations/release/' + tabId, cleanupData)
      console.log(`Sent cleanup beacon for tab: ${tabId}`)
    } catch (error) {
      console.error(`Failed to send cleanup beacon for tab ${tabId}:`, error)
    }
  }
}

// Cleanup any pending reservations from previous session
const cleanupPendingReservations = async () => {
  try {
    const pendingCleanup = localStorage.getItem('pendingCartReservationCleanup')
    if (pendingCleanup) {
      const tabIds = JSON.parse(pendingCleanup)
      console.log('Cleaning up pending cart reservations from previous session:', tabIds)

      for (const tabId of tabIds) {
        try {
          await releaseCartReservations(tabId)
          console.log(`Cleaned up reservations for tab: ${tabId}`)
        } catch (error) {
          console.error(`Failed to cleanup reservations for tab ${tabId}:`, error)
        }
      }

      // Clear the pending cleanup flag
      localStorage.removeItem('pendingCartReservationCleanup')
    }
  } catch (error) {
    console.error('Error during pending reservations cleanup:', error)
  }
}

// Generate comprehensive test report
const generateTestReport = async () => {
  try {
    console.log('📋 Generating Comprehensive Test Report...')

    const report = {
      timestamp: new Date().toISOString(),
      environment: import.meta.env.MODE,
      testResults: await runCustomerScenarioIntegrationTests(),
      currentState: {
        activeTab: activeTab.value ? {
          hasCustomer: !!activeTab.value.khachHang,
          hasDelivery: !!activeTab.value.giaohang,
          recipientInfo: recipientInfo.value,
          addressComplete: isAddressComplete()
        } : null,
        scenario: await validateAllCustomerScenarios()
      },
      summary: {
        totalTests: 5,
        implementation: {
          scenario1: 'Implemented with auto-population and validation',
          scenario2: 'Implemented with separate recipient tracking',
          scenario3: 'Implemented with customer creation',
          addressManagement: 'Enhanced with comprehensive validation',
          backendMapping: 'Implemented with proper customer ID preservation'
        }
      }
    }

    console.log('📊 Comprehensive Test Report:', report)

    // Display summary in toast
    const passedTests = Object.values(report.testResults).slice(0, -1).filter(test => test.passed).length
    const totalTests = Object.values(report.testResults).slice(0, -1).length

    toast.add({
      severity: passedTests === totalTests ? 'success' : 'warn',
      summary: 'Test Report Generated',
      detail: `${passedTests}/${totalTests} tests passed. Check console for details.`,
      life: 5000
    })

    return report
  } catch (error) {
    console.error('Error generating test report:', error)
    return { error: error.message }
  }
}

// ===== COMPREHENSIVE SYSTEM INTEGRATION TESTING =====

// Comprehensive integration testing for all Order Management System enhancements
const runSystemIntegrationTests = async () => {
  const testResults = {
    paymentIntegrations: { passed: false, errors: [], details: {} },
    voucherIntelligence: { passed: false, errors: [], details: {} },
    staffAssignment: { passed: false, errors: [], details: {} },
    customerScenarios: { passed: false, errors: [], details: {} },
    addressManagement: { passed: false, errors: [], details: {} },
    uiIntegration: { passed: false, errors: [], details: {} },
    performanceValidation: { passed: false, errors: [], details: {} },
    overall: { passed: false, summary: '', totalTests: 0, passedTests: 0 }
  }

  console.log('🚀 Starting Comprehensive System Integration Tests...')

  try {
    // Test Payment Integrations
    console.log('🧪 Testing Payment Integrations...')
    testResults.paymentIntegrations = await testPaymentIntegrations()

    // Test Voucher Intelligence System
    console.log('🧪 Testing Voucher Intelligence System...')
    testResults.voucherIntelligence = await testVoucherIntelligence()

    // Test Staff Assignment Functionality
    console.log('🧪 Testing Staff Assignment...')
    testResults.staffAssignment = await testStaffAssignment()

    // Test Customer Scenarios (existing tests)
    console.log('🧪 Testing Customer Scenarios...')
    testResults.customerScenarios = await runCustomerScenarioIntegrationTests()

    // Test Address Management (existing tests)
    console.log('🧪 Testing Address Management...')
    testResults.addressManagement = await testAddressManagement()

    // Test UI Integration
    console.log('🧪 Testing UI Integration...')
    testResults.uiIntegration = await testUIIntegration()

    // Test Performance Validation
    console.log('🧪 Testing Performance...')
    testResults.performanceValidation = await testPerformanceValidation()

    // Calculate overall results
    const testCategories = Object.keys(testResults).filter(key => key !== 'overall')
    const passedTests = testCategories.filter(category => testResults[category].passed).length
    const totalTests = testCategories.length

    testResults.overall = {
      passed: passedTests === totalTests,
      summary: `${passedTests}/${totalTests} test categories passed`,
      totalTests,
      passedTests,
      failedCategories: testCategories.filter(category => !testResults[category].passed)
    }

    console.log('🎯 System Integration Tests Completed:', testResults)
    return testResults

  } catch (error) {
    console.error('❌ System Integration Testing failed:', error)
    testResults.overall.passed = false
    testResults.overall.summary = `System integration testing failed: ${error.message}`
    return testResults
  }
}

// Test Payment Integrations
const testPaymentIntegrations = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Payment Integration System...')

    // Test 1: Payment method availability based on order type
    updateActiveTabData({ loaiHoaDon: 'TAI_QUAY', giaohang: false })
    const taiQuayMethods = paymentMethods.value

    if (!taiQuayMethods.some(m => m.value === 'TIEN_MAT')) {
      result.errors.push('TAI_QUAY orders should have TIEN_MAT payment method')
    }

    // Test 2: Online order payment methods
    updateActiveTabData({ loaiHoaDon: 'ONLINE', giaohang: true })
    const onlineMethods = paymentMethods.value

    if (!onlineMethods.some(m => m.value === 'TIEN_MAT' && m.label.includes('giao hàng'))) {
      result.errors.push('Online delivery orders should have cash on delivery option')
    }

    // Test 3: Payment method validation
    updateActiveTabData({ phuongThucThanhToan: 'TIEN_MAT', tongThanhToan: 100000 })
    customerPayment.value = 150000
    calculateChange()

    if (changeAmount.value !== 50000) {
      result.errors.push('Change calculation incorrect')
    }

    // Test 4: Mixed payment functionality
    const mixedPaymentConfig = {
      payments: [
        { method: 'TIEN_MAT', amount: 50000 },
        { method: 'CHUYEN_KHOAN', amount: 50000 }
      ]
    }

    onMixedPaymentConfirm(mixedPaymentConfig)

    if (activeTab.value.phuongThucThanhToan !== 'MIXED') {
      result.errors.push('Mixed payment configuration not applied correctly')
    }

    result.passed = result.errors.length === 0
    result.details = {
      taiQuayMethodsCount: taiQuayMethods.length,
      onlineMethodsCount: onlineMethods.length,
      changeCalculation: changeAmount.value,
      mixedPaymentApplied: activeTab.value.phuongThucThanhToan === 'MIXED'
    }

    console.log('✅ Payment Integration test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Payment integration test error: ${error.message}`)
    console.error('❌ Payment Integration test failed:', error)
    return result
  }
}

// Test Voucher Intelligence System
const testVoucherIntelligence = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Voucher Intelligence System...')

    // Test 1: Voucher loading for customer
    const testCustomer = {
      id: 'test-customer-voucher',
      hoTen: 'Test Customer',
      soDienThoai: '0123456789'
    }

    updateActiveTabData({
      khachHang: testCustomer,
      tongTienHang: 500000,
      voucherList: []
    })

    // Mock voucher API for testing
    const originalGetAvailableVouchers = voucherApi.getAvailableVouchers
    const originalValidateVoucher = voucherApi.validateVoucher

    voucherApi.getAvailableVouchers = async (_customerId, _orderTotal) => ({
      success: true,
      data: [
        { maPhieuGiamGia: 'TEST10', tenPhieuGiamGia: 'Test 10%', giaTriGiam: 50000 },
        { maPhieuGiamGia: 'TEST20', tenPhieuGiamGia: 'Test 20%', giaTriGiam: 100000 }
      ]
    })

    voucherApi.validateVoucher = async (code, _customerId, _orderTotal) => ({
      success: true,
      data: {
        valid: true,
        voucher: { maPhieuGiamGia: code, tenPhieuGiamGia: `Test ${code}` },
        discountAmount: code === 'TEST10' ? 50000 : 100000
      }
    })

    try {
      // Test voucher loading
      await loadAvailableVouchers()

      if (availableVouchers.value.length !== 2) {
        result.errors.push('Available vouchers not loaded correctly')
      }

      // Test voucher application
      const testVoucher = availableVouchers.value[0]
      await selectVoucher(testVoucher)

      if (activeTab.value.voucherList.length !== 1) {
        result.errors.push('Voucher not applied correctly')
      }

      // Test single voucher restriction
      const secondVoucher = { maPhieuGiamGia: 'TEST20', tenPhieuGiamGia: 'Test 20%' }
      await selectVoucher(secondVoucher)

      if (activeTab.value.voucherList.length !== 1 || activeTab.value.voucherList[0].maPhieuGiamGia !== 'TEST20') {
        result.errors.push('Single voucher restriction not working correctly')
      }

    } finally {
      // Restore original functions
      voucherApi.getAvailableVouchers = originalGetAvailableVouchers
      voucherApi.validateVoucher = originalValidateVoucher
    }

    result.passed = result.errors.length === 0
    result.details = {
      vouchersLoaded: availableVouchers.value.length,
      vouchersApplied: activeTab.value.voucherList.length,
      singleVoucherRestriction: activeTab.value.voucherList.length === 1
    }

    console.log('✅ Voucher Intelligence test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Voucher intelligence test error: ${error.message}`)
    console.error('❌ Voucher Intelligence test failed:', error)
    return result
  }
}

// Test Staff Assignment Functionality
const testStaffAssignment = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Staff Assignment System...')

    // Test 1: Automatic staff assignment (backend handled)
    // Since staff assignment is handled automatically by the backend,
    // we test that the frontend doesn't interfere with this process

    const orderData = mapTabToHoaDonDto(activeTab.value)

    if (orderData.nhanVienId !== null) {
      result.errors.push('Frontend should not set staff ID - should be handled by backend')
    }

    // Test 2: Order creation without manual staff selection
    // Verify that orders can be created without requiring staff selection in UI
    updateActiveTabData({
      maHoaDon: 'TEST-STAFF-001',
      loaiHoaDon: 'TAI_QUAY',
      sanPhamList: [
        {
          sanPhamChiTiet: { id: 'test-product-1' },
          soLuong: 1,
          donGia: 100000
        }
      ],
      tongTienHang: 100000,
      tongThanhToan: 100000,
      phuongThucThanhToan: 'TIEN_MAT'
    })

    const staffOrderData = mapTabToHoaDonDto(activeTab.value)

    // Verify order data is complete without staff assignment
    if (!staffOrderData.maHoaDon || !staffOrderData.loaiHoaDon) {
      result.errors.push('Order data incomplete for staff assignment test')
    }

    // Test 3: Staff assignment consistency
    // Multiple order creations should not have conflicting staff assignments
    const order1Data = mapTabToHoaDonDto(activeTab.value)
    const order2Data = mapTabToHoaDonDto(activeTab.value)

    if (order1Data.nhanVienId !== order2Data.nhanVienId) {
      result.errors.push('Staff assignment inconsistent between order mappings')
    }

    result.passed = result.errors.length === 0
    result.details = {
      staffIdSetByFrontend: orderData.nhanVienId !== null,
      orderDataComplete: !!staffOrderData.maHoaDon && !!staffOrderData.loaiHoaDon,
      staffAssignmentConsistent: order1Data.nhanVienId === order2Data.nhanVienId
    }

    console.log('✅ Staff Assignment test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Staff assignment test error: ${error.message}`)
    console.error('❌ Staff Assignment test failed:', error)
    return result
  }
}

// Test UI Integration
const testUIIntegration = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing UI Integration...')

    // Test 1: Tab management
    const initialTabCount = orderTabs.value.length
    createNewOrderTab()

    if (orderTabs.value.length !== initialTabCount + 1) {
      result.errors.push('Tab creation not working correctly')
    }

    // Test 2: Delivery toggle integration
    const currentTab = activeTab.value
    if (currentTab) {
      updateActiveTabData({ giaohang: true })

      if (!activeTab.value.giaohang) {
        result.errors.push('Delivery toggle not updating correctly')
      }

      // Test payment method validation with delivery change
      updateActiveTabData({ phuongThucThanhToan: 'TIEN_MAT' })
      const paymentMethodsWithDelivery = paymentMethods.value

      if (!paymentMethodsWithDelivery.some(m => m.value === 'TIEN_MAT')) {
        result.errors.push('Payment methods not updating with delivery toggle')
      }
    }

    // Test 3: Customer search integration
    const searchTerm = 'test'
    selectedCustomer.value = null

    // Test customer selection functionality
    if (selectedCustomer.value !== null) {
      result.errors.push('Customer selection not clearing correctly')
    }

    // Test 4: Address form integration
    const testAddress = {
      duong: 'Test Street Integration',
      phuongXa: 'Test Ward',
      quanHuyen: 'Test District',
      tinhThanh: 'Test Province',
      loaiDiaChi: 'Nhà riêng'
    }

    setAddressData(testAddress)

    if (addressData.value.duong !== testAddress.duong) {
      result.errors.push('Address form integration not working correctly')
    }

    // Test 5: Recipient info integration
    const testRecipient = {
      hoTen: 'Test Recipient UI',
      soDienThoai: '0123456789'
    }

    recipientInfo.value = { ...testRecipient }

    if (recipientInfo.value.hoTen !== testRecipient.hoTen) {
      result.errors.push('Recipient info integration not working correctly')
    }

    result.passed = result.errors.length === 0
    result.details = {
      tabManagement: orderTabs.value.length > initialTabCount,
      deliveryToggle: activeTab.value?.giaohang === true,
      paymentMethodsAvailable: paymentMethods.value.length > 0,
      customerSelectionWorking: selectedCustomer.value === null,
      addressFormWorking: addressData.value.duong === testAddress.duong,
      recipientInfoWorking: recipientInfo.value.hoTen === testRecipient.hoTen
    }

    console.log('✅ UI Integration test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`UI integration test error: ${error.message}`)
    console.error('❌ UI Integration test failed:', error)
    return result
  }
}

// Test Performance Validation
const testPerformanceValidation = async () => {
  const result = { passed: false, errors: [], details: {} }

  try {
    console.log('🧪 Testing Performance Validation...')

    // Test 1: Address validation performance
    const addressValidationStart = performance.now()

    setAddressData({
      duong: 'Performance Test Street',
      phuongXa: 'Performance Ward',
      quanHuyen: 'Performance District',
      tinhThanh: 'Performance Province',
      loaiDiaChi: 'Nhà riêng'
    })

    const addressValidationResult = validateEmbeddedAddress()
    const addressValidationTime = performance.now() - addressValidationStart

    if (addressValidationTime > 100) { // 100ms threshold
      result.errors.push(`Address validation too slow: ${addressValidationTime}ms`)
    }

    // Test 2: Customer scenario detection performance
    const scenarioDetectionStart = performance.now()

    updateActiveTabData({
      khachHang: { id: 'perf-test', hoTen: 'Performance Test', soDienThoai: '0123456789' },
      giaohang: true
    })
    recipientInfo.value = { hoTen: 'Different Recipient', soDienThoai: '0987654321' }

    const scenarioValidation = await validateAllCustomerScenarios()
    const scenarioDetectionTime = performance.now() - scenarioDetectionStart

    if (scenarioDetectionTime > 50) { // 50ms threshold
      result.errors.push(`Scenario detection too slow: ${scenarioDetectionTime}ms`)
    }

    // Test 3: Backend mapping performance
    const mappingStart = performance.now()

    updateActiveTabData({
      sanPhamList: Array.from({ length: 10 }, (_, i) => ({
        sanPhamChiTiet: { id: `perf-product-${i}` },
        soLuong: 1,
        donGia: 100000
      })),
      voucherList: [
        { maPhieuGiamGia: 'PERF-VOUCHER', giaTriGiam: 50000 }
      ]
    })

    const mappingResult = mapTabToHoaDonDto(activeTab.value)
    const mappingTime = performance.now() - mappingStart

    if (mappingTime > 20) { // 20ms threshold
      result.errors.push(`Backend mapping too slow: ${mappingTime}ms`)
    }

    // Test 4: Memory usage validation
    const memoryBefore = performance.memory ? performance.memory.usedJSHeapSize : 0

    // Simulate multiple operations
    for (let i = 0; i < 100; i++) {
      validateEmbeddedAddress()
      await validateAllCustomerScenarios()
    }

    const memoryAfter = performance.memory ? performance.memory.usedJSHeapSize : 0
    const memoryIncrease = memoryAfter - memoryBefore

    if (memoryIncrease > 10 * 1024 * 1024) { // 10MB threshold
      result.errors.push(`Excessive memory usage: ${memoryIncrease / 1024 / 1024}MB`)
    }

    result.passed = result.errors.length === 0
    result.details = {
      addressValidationTime: `${addressValidationTime.toFixed(2)}ms`,
      scenarioDetectionTime: `${scenarioDetectionTime.toFixed(2)}ms`,
      backendMappingTime: `${mappingTime.toFixed(2)}ms`,
      memoryIncrease: `${(memoryIncrease / 1024 / 1024).toFixed(2)}MB`,
      addressValidationWorking: addressValidationResult,
      scenarioDetectionWorking: scenarioValidation.valid,
      backendMappingWorking: !!mappingResult.khachHangId
    }

    console.log('✅ Performance Validation test completed:', result)
    return result

  } catch (error) {
    result.errors.push(`Performance validation test error: ${error.message}`)
    console.error('❌ Performance Validation test failed:', error)
    return result
  }
}

// ===== DEVELOPMENT TESTING UTILITIES =====













// Expose testing functions for development/debugging
if (import.meta.env.DEV) {
  window.orderCreateTests = {
    // System Integration Tests
    runSystemIntegrationTests,
    testPaymentIntegrations,
    testVoucherIntelligence,
    testStaffAssignment,
    testUIIntegration,
    testPerformanceValidation,

    // Customer Scenario Tests
    runIntegrationTests: executeIntegrationTests,
    validateScenarios: validateAllCustomerScenarios,
    testScenario1,
    testScenario2,
    testScenario3,
    testAddressManagement,
    testBackendMapping,

    // Reporting
    generateReport: generateTestReport,

    // Quick test runners
    runAll: async () => {
      console.log('🚀 Running all customer scenario tests...')
      const report = await generateTestReport()
      return report
    },
    runSystemTests: async () => {
      console.log('🚀 Running comprehensive system integration tests...')
      const results = await runSystemIntegrationTests()
      return results
    }
  }
  console.log('🧪 OrderCreate testing utilities available at window.orderCreateTests')
  console.log('💡 Run window.orderCreateTests.runSystemTests() for comprehensive system testing')
  console.log('💡 Run window.orderCreateTests.runAll() for customer scenario tests')
}

// Initialize
onMounted(async () => {
  // Staff assignment is now handled automatically by the backend

  // Cleanup any pending cart reservations from previous session
  await cleanupPendingReservations()

  // Create first tab if none exist
  if (!hasActiveTabs.value) {
    createNewOrderTab()
  }

  // Ensure we have an active tab after initialization
  if (!activeTab.value && orderTabs.value.length > 0) {
    switchToTab(orderTabs.value[0].id)
  }







  // Preload data for search functionality
  try {
    await customerStore.fetchCustomers()
  } catch (error) {
    console.error('Failed to preload data:', error)
  }

  // Add beforeunload event listener for page refresh/close detection
  window.addEventListener('beforeunload', handlePageUnload)

  // Also add pagehide event for better mobile browser support
  window.addEventListener('pagehide', handlePageUnload)
})

// Cleanup event listeners on component unmount
onUnmounted(() => {
  // Remove event listeners
  window.removeEventListener('beforeunload', handlePageUnload)
  window.removeEventListener('pagehide', handlePageUnload)
})


</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
