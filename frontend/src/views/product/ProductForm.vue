<template>
    <Toast />

    <!-- Page Header -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
            <i class="pi pi-box text-lg text-primary"></i>
          </div>
          <div>
            <h1 class="font-semibold text-xl text-surface-900 m-0">
              {{ isEdit ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới' }}
            </h1>
            <p class="text-surface-500 text-sm mt-1 mb-0">
              {{ isEdit ? `Chỉnh sửa thông tin sản phẩm: ${productForm.tenSanPham}` : 'Tạo sản phẩm mới với hệ thống 8-core attributes và SKU auto-generation' }}
            </p>
          </div>
        </div>
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          size="small"
          @click="goBack"
          v-tooltip.left="'Quay lại'"
        />
      </div>
    </div>

    <form @submit.prevent="handleSubmit">
      <div class="flex flex-col gap-6">

        <!-- Basic Information Section -->
        <div class="card">
          <div class="flex items-center gap-2 mb-4">
            <i class="pi pi-info-circle text-primary"></i>
            <span class="font-semibold text-xl">Thông tin cơ bản</span>
          </div>

          <div class="grid grid-cols-12 gap-4">
            <!-- Product Code -->
            <div class="col-span-12 md:col-span-6">
              <div class="flex flex-col gap-2">
                <label class="font-semibold">
                  Mã sản phẩm <span class="text-red-500">*</span>
                </label>
                <InputText
                  v-model="productForm.maSanPham"
                  placeholder="Ví dụ: MBA-M2-2024 hoặc SP001"
                  :invalid="errors.maSanPham"
                  :disabled="isEdit"
                />
                <small v-if="errors.maSanPham" class="text-red-500">{{ errors.maSanPham }}</small>
                <small class="text-surface-600">Để trống để hệ thống tự động tạo mã sản phẩm.</small>
              </div>
            </div>

            <!-- Product Name -->
            <div class="col-span-12 md:col-span-6">
              <div class="flex flex-col gap-2">
                <label class="font-semibold">
                  Tên sản phẩm <span class="text-red-500">*</span>
                </label>
                <InputText
                  v-model="productForm.tenSanPham"
                  placeholder="Nhập tên sản phẩm"
                  :invalid="errors.tenSanPham"
                />
                <small v-if="errors.tenSanPham" class="text-red-500">{{ errors.tenSanPham }}</small>
              </div>
            </div>

            <!-- Category - Updated to MultiSelect -->
            <div class="col-span-12 md:col-span-6">
              <div class="flex flex-col gap-2">
                <label class="font-semibold">
                  Danh mục <span class="text-red-500">*</span>
                </label>
                <MultiSelect
                  v-model="productForm.danhMucs"
                  :options="categories"
                  optionLabel="moTaDanhMuc"
                  placeholder="Chọn danh mục"
                  :invalid="errors.danhMucs"
                  display="chip"
                  :disabled="categories.length === 0"
                />
                <small v-if="errors.danhMucs" class="text-red-500">{{ errors.danhMucs }}</small>
                <small v-if="categories.length === 0" class="text-orange-500">
                  Chưa có danh mục nào. Vui lòng thêm danh mục trong phần
                  <router-link to="/attributes" class="text-primary underline">Quản lý thuộc tính</router-link>.
                </small>
              </div>
            </div>

            <!-- Brand -->
            <div class="col-span-12 md:col-span-6">
              <div class="flex flex-col gap-2">
                <label class="font-semibold">
                  Thương hiệu <span class="text-red-500">*</span>
                </label>
                <Select
                  v-model="productForm.thuongHieu"
                  :options="brands"
                  optionLabel="moTaThuongHieu"
                  placeholder="Chọn thương hiệu"
                  :invalid="errors.thuongHieu"
                  :disabled="brands.length === 0"
                />
                <small v-if="errors.thuongHieu" class="text-red-500">{{ errors.thuongHieu }}</small>
                <small v-if="brands.length === 0" class="text-orange-500">
                  Chưa có thương hiệu nào. Vui lòng thêm thương hiệu trong phần
                  <router-link to="/attributes" class="text-primary underline">Quản lý thuộc tính</router-link>.
                </small>
              </div>
            </div>

            <!-- Release Date -->
            <div class="col-span-12 md:col-span-6">
              <div class="flex flex-col gap-2">
                <label class="font-semibold">Ngày ra mắt</label>
                <DatePicker
                  v-model="productForm.ngayRaMat"
                  placeholder="Chọn ngày ra mắt"
                  dateFormat="dd/mm/yy"
                  :showIcon="true"
                />
              </div>
            </div>

            <!-- Status -->
            <div class="col-span-12 md:col-span-6">
              <div class="flex flex-col gap-2">
                <label class="font-semibold">Trạng thái</label>
                <div class="flex items-center gap-3">
                  <ToggleSwitch v-model="productForm.trangThai" />
                  <span class="text-sm">
                    {{ productForm.trangThai ? 'Hoạt động' : 'Ngừng hoạt động' }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Description -->
            <div class="col-span-12">
              <div class="flex flex-col gap-2">
                <label class="font-semibold">Mô tả sản phẩm</label>
                <Textarea
                  v-model="productForm.moTa"
                  placeholder="Nhập mô tả chi tiết về sản phẩm..."
                  rows="4"
                  :maxlength="5000"
                />
                <small class="text-surface-600">
                  {{ productForm.moTa?.length || 0 }}/5000 ký tự
                </small>
              </div>
            </div>
          </div>
        </div>

        <!-- Product Images Section -->
        <div class="card">
          <div class="flex items-center gap-2 mb-4">
            <i class="pi pi-images text-primary"></i>
            <span class="font-semibold text-xl">Hình ảnh sản phẩm</span>
          </div>

          <div class="space-y-4">
            <!-- Image Upload Grid -->
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              <!-- Image Slots (up to 5 total) -->
              <div
                v-for="index in 5"
                :key="`image-slot-${index - 1}`"
                class="relative group"
              >
                <div class="w-full aspect-square border-2 border-dashed border-surface-300 rounded-lg flex items-center justify-center overflow-hidden bg-surface-50 hover:bg-surface-100 cursor-pointer transition-colors">
                  <!-- Show image if exists -->
                  <img
                    v-if="imagePreviewUrls[index - 1]"
                    :src="imagePreviewUrls[index - 1]"
                    :alt="`Product image ${index}`"
                    class="w-full h-full object-cover"
                  />
                  <!-- Show placeholder if no image -->
                  <div v-else class="text-center">
                    <i class="pi pi-plus text-2xl text-surface-400 mb-2 block"></i>
                    <span class="text-surface-600 text-sm">Thêm ảnh</span>
                  </div>
                </div>

                <!-- Remove button overlay (only show on hover if image exists) -->
                <div
                  v-if="imagePreviewUrls[index - 1]"
                  @click="removeImage(index - 1)"
                  class="absolute inset-0 w-full h-full bg-red-500/70 hover:bg-red-600/80 opacity-0 group-hover:opacity-100 transition-all duration-300 cursor-pointer flex items-center justify-center rounded-lg"
                  v-tooltip.top="'Xóa ảnh'"
                >
                  <i class="pi pi-times text-white text-3xl drop-shadow-lg"></i>
                </div>

                <!-- File input (only show if no image exists) -->
                <input
                  v-if="!imagePreviewUrls[index - 1]"
                  type="file"
                  accept="image/*"
                  @change="onImageSelect($event, index - 1)"
                  class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                />
              </div>
            </div>

            <!-- Upload Progress -->
            <div v-if="uploadingImages.length > 0" class="space-y-2">
              <label class="font-semibold">Đang tải lên:</label>
              <div v-for="(upload, index) in uploadingImages" :key="index" class="flex items-center gap-3">
                <ProgressBar :value="upload.progress" class="flex-1" />
                <span class="text-sm text-surface-600">{{ upload.name }}</span>
              </div>
            </div>

            <!-- Image Guidelines -->
            <div class="bg-surface-50 p-4 rounded-lg">
              <h4 class="font-medium mb-2">Hướng dẫn tải ảnh:</h4>
              <ul class="text-sm text-surface-600 space-y-1">
                <li>• Tối đa 5 hình ảnh</li>
                <li>• Kích thước tối đa: 5MB mỗi ảnh</li>
                <li>• Định dạng: JPG, PNG, WebP</li>
                <li>• Khuyến nghị: Ảnh vuông 800x800px trở lên</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Product Variants Section -->
        <div class="card">
          <div class="flex items-center gap-2 mb-4">
            <i class="pi pi-list text-primary"></i>
            <span class="font-semibold text-xl">Biến thể sản phẩm</span>
            <Badge :value="productForm.sanPhamChiTiets?.length || 0" severity="info" />
          </div>

          <!-- Serial Number Validation Error -->
          <div v-if="errors.serialNumbers" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <div class="flex items-center gap-2">
              <i class="pi pi-exclamation-triangle text-red-500"></i>
              <span class="text-red-700 font-medium">Lỗi Serial Number:</span>
            </div>
            <p class="text-red-600 mt-1">{{ errors.serialNumbers }}</p>
          </div>

          <div class="space-y-4">
            <!-- Variant Generation Tool -->
            <div class="bg-surface-50 p-4 rounded-lg">
              <h4 class="font-medium mb-3">Tạo biến thể tự động</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                <!-- Colors -->
                <div class="flex flex-col gap-2">
                  <label class="font-semibold">Màu sắc</label>
                  <MultiSelect
                    v-model="selectedColors"
                    :options="colors"
                    optionLabel="moTaMauSac"
                    placeholder="Chọn màu sắc"
                    display="chip"
                  />
                </div>

                <!-- CPUs -->
                <div class="flex flex-col gap-2">
                  <label class="font-semibold">CPU</label>
                  <MultiSelect
                    v-model="selectedCpus"
                    :options="cpus"
                    optionLabel="moTaCpu"
                    placeholder="Chọn CPU"
                    display="chip"
                  />
                </div>

                <!-- RAM -->
                <div class="flex flex-col gap-2">
                  <label class="font-semibold">RAM</label>
                  <MultiSelect
                    v-model="selectedRams"
                    :options="rams"
                    optionLabel="moTaRam"
                    placeholder="Chọn RAM"
                    display="chip"
                  />
                </div>

                <!-- GPU -->
                <div class="flex flex-col gap-2">
                  <label class="font-semibold">GPU</label>
                  <MultiSelect
                    v-model="selectedGpus"
                    :options="gpus"
                    optionLabel="moTaGpu"
                    placeholder="Chọn GPU"
                    display="chip"
                  />
                </div>

                <!-- Storage -->
                <div class="flex flex-col gap-2">
                  <label class="font-semibold">Ổ cứng</label>
                  <MultiSelect
                    v-model="selectedStorage"
                    :options="storage"
                    optionLabel="moTaBoNho"
                    placeholder="Chọn ổ cứng"
                    display="chip"
                  />
                </div>

                <!-- Screen -->
                <div class="flex flex-col gap-2">
                  <label class="font-semibold">Màn hình</label>
                  <MultiSelect
                    v-model="selectedScreen"
                    :options="screens"
                    optionLabel="moTaManHinh"
                    placeholder="Chọn màn hình"
                    display="chip"
                  />
                </div>
              </div>

              <!-- Generate Variants Button -->
              <div class="flex justify-start mt-4">
                <Button
                  label="Tạo biến thể"
                  icon="pi pi-cog"
                  @click="handleGenerateVariants"
                  :disabled="!canGenerateVariants"
                  :loading="generatingVariants"
                />
                <small class="text-surface-600 ml-4 self-center">
                  Giá bán sẽ được thiết lập riêng cho từng biến thể
                </small>
              </div>
            </div>

            <!-- Generated Variants Preview -->
            <div v-if="productForm.sanPhamChiTiets?.length" class="space-y-4">
              <div class="flex items-center justify-between">
                <label class="font-semibold">Biến thể đã tạo:</label>
                <Badge :value="productForm.sanPhamChiTiets.length" severity="info" />
              </div>

              <DataTable
                :value="productForm.sanPhamChiTiets"
                class="p-datatable-sm"
                :paginator="productForm.sanPhamChiTiets.length > 10"
                :rows="10"
                :rowsPerPageOptions="[10, 25, 50]"
                showGridlines
                :rowClass="getVariantRowClass"
              >
                <Column header="STT" style="width: 60px">
                  <template #body="{ index }">
                    <span class="font-medium">{{ index + 1 }}</span>
                  </template>
                </Column>

                <Column header="Cấu hình" style="min-width: 250px">
                  <template #body="{ data }">
                    <div class="text-sm space-y-1">
                      <div v-if="data.mauSac" class="flex items-center gap-2">
                        <i class="pi pi-circle-fill text-xs" :style="{ color: data.mauSac.maMau || '#666' }"></i>
                        {{ data.mauSac.moTaMauSac }}
                      </div>
                      <div v-if="data.cpu">
                        <i class="pi pi-microchip text-xs"></i>
                        {{ data.cpu.moTaCpu }}
                      </div>
                      <div v-if="data.ram">
                        <i class="pi pi-server text-xs"></i>
                        {{ data.ram.moTaRam }}
                      </div>
                      <div v-if="data.gpu">
                        <i class="pi pi-desktop text-xs"></i>
                        {{ data.gpu.moTaGpu }}
                      </div>
                      <div v-if="data.ocung">
                        <i class="pi pi-database text-xs"></i>
                        {{ data.ocung.moTaOCung }}
                      </div>
                      <div v-if="data.manHinh">
                        <i class="pi pi-tablet text-xs"></i>
                        {{ data.manHinh.moTaManHinh }}
                      </div>
                    </div>
                  </template>
                </Column>

                <!-- Enhanced Variant Image Column -->
                <Column header="Ảnh biến thể" style="min-width: 160px">
                  <template #body="{ data, index }">
                    <div class="flex flex-col items-center gap-3">
                      <div class="relative group">
                        <div
                          class="w-16 h-16 rounded-lg overflow-hidden border-2 transition-all duration-200"
                          :class="variantImagePreviews[index] || (data.hinhAnh?.length > 0)
                            ? 'border-primary-200 shadow-sm hover:shadow-md'
                            : 'border-dashed border-surface-300 hover:border-primary-300 hover:bg-primary-50'"
                        >
                          <img
                            v-if="variantImagePreviews[index] || (data.hinhAnh?.length > 0)"
                            :src="variantImagePreviews[index] || getImageUrl(data.hinhAnh[0])"
                            alt="Variant image"
                            class="w-full h-full object-cover transition-transform duration-200 group-hover:scale-105"
                          />
                          <div v-else class="w-full h-full flex items-center justify-center">
                            <i class="pi pi-image text-surface-400 text-lg"></i>
                          </div>
                        </div>

                        <!-- Remove button with better positioning -->
                        <Button
                          v-if="variantImagePreviews[index] || (data.hinhAnh?.length > 0)"
                          icon="pi pi-times"
                          severity="danger"
                          text
                          rounded
                          size="small"
                          class="absolute -top-2 -right-2 w-6 h-6 bg-white shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200"
                          @click="removeVariantImage(data, index)"
                          v-tooltip.top="'Xóa ảnh'"
                        />
                      </div>

                      <!-- Enhanced upload button -->
                      <Button
                        :label="(variantImagePreviews[index] || (data.hinhAnh?.length > 0)) ? 'Thay đổi' : 'Tải lên'"
                        :icon="(variantImagePreviews[index] || (data.hinhAnh?.length > 0)) ? 'pi pi-refresh' : 'pi pi-upload'"
                        severity="secondary"
                        outlined
                        size="small"
                        class="text-xs px-2 py-1 hover:bg-primary-50 hover:border-primary-300 transition-colors duration-200"
                        @click="selectVariantImage(data, index)"
                        v-tooltip.top="'Chọn ảnh cho biến thể'"
                      />
                    </div>
                  </template>
                </Column>

                <Column field="giaBan" header="Giá bán" style="min-width: 150px">
                  <template #body="{ data }">
                    <InputNumber
                      v-model="data.giaBan"
                      mode="currency"
                      currency="VND"
                      locale="vi-VN"
                      size="small"
                      class="w-full"
                    />
                  </template>
                </Column>

                <Column header="Tồn kho" style="width: 120px">
                  <template #body="{ data }">
                    <div class="text-center">
                      <div class="font-semibold">{{ data.serialNumbers?.length || 0 }}</div>
                      <div class="text-xs text-surface-500">
                        <span class="text-green-600">{{ getAvailableSerialCount(data) }} có sẵn</span>
                        <span v-if="getReservedSerialCount(data) > 0" class="text-orange-600 ml-1">
                          • {{ getReservedSerialCount(data) }} đặt trước
                        </span>
                      </div>
                    </div>
                  </template>
                </Column>

                <Column header="Thao tác" style="width: 15rem">
                  <template #body="{ data, index }">
                    <div class="flex gap-1">
                      <Button
                        icon="pi pi-barcode"
                        severity="secondary"
                        text
                        rounded
                        size="small"
                        @click="manageSerialNumbers(data)"
                        v-tooltip="'Quản lý serial numbers'"
                      />
                      <Button
                        icon="pi pi-trash"
                        severity="danger"
                        text
                        rounded
                        size="small"
                        @click="removeVariant(index)"
                        v-tooltip.top="'Xóa biến thể'"
                      />
                    </div>
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </div>

        <!-- Audit Log Section (Edit Mode Only) -->
        <div v-if="isEdit && auditHistory.length > 0" class="card">
          <div class="flex items-center gap-2 mb-4">
            <i class="pi pi-history text-primary"></i>
            <span class="font-semibold text-xl">Lịch sử thay đổi</span>
            <div class="flex items-center gap-2 text-sm text-surface-500 ml-auto">
              <i class="pi pi-clock"></i>
              <span>{{ auditHistory.length }} mục</span>
            </div>
          </div>

          <div class="space-y-4 max-h-96 overflow-y-auto">
            <div
              v-for="(entry, index) in auditHistory"
              :key="entry.id || index"
              class="border-l-4 pl-4 py-3 rounded-r-lg"
              :class="getAuditBorderColor(entry.hanhDong)"
            >
              <!-- Header with action and timestamp -->
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-3">
                  <i :class="[getAuditIcon(entry.hanhDong), getAuditIconColor(entry.hanhDong), 'text-lg']"></i>
                  <span class="font-medium text-base">{{ getActionDisplayName(entry.hanhDong) }}</span>
                  <span class="text-sm text-surface-500">{{ formatAuditDate(entry.thoiGianThayDoi) }}</span>
                </div>
              </div>

              <!-- User and Reason Information -->
              <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
                <div class="space-y-2">
                  <div class="text-sm text-surface-700">
                    <strong class="text-surface-900">Người thực hiện:</strong>
                    <span class="font-medium ml-2">{{ entry.nguoiThucHien || 'Hệ thống' }}</span>
                  </div>

                  <div v-if="entry.lyDoThayDoi" class="text-sm text-surface-700">
                    <strong class="text-surface-900">Lý do:</strong>
                    <span class="italic ml-2">{{ entry.lyDoThayDoi }}</span>
                  </div>
                </div>
              </div>

              <!-- Change Details Section -->
              <div v-if="entry.giaTriCu || entry.giaTriMoi" class="bg-surface-50 rounded-lg p-4">
                <strong class="text-surface-900 text-base block mb-3">Chi tiết thay đổi:</strong>

                <!-- Parse and display changes for UPDATE entries -->
                <div v-if="entry.giaTriCu && entry.giaTriMoi" class="space-y-3">
                  <div v-for="change in parseAuditChanges(entry.giaTriCu, entry.giaTriMoi)" :key="change.field" class="border-b border-surface-200 pb-3 last:border-b-0 last:pb-0">
                    <div class="font-medium text-surface-700 mb-2 text-sm">{{ change.fieldName }}:</div>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <div class="text-red-600 bg-red-50 p-2 rounded text-sm">{{ change.oldValue }}</div>
                      </div>
                      <div>
                        <div class="text-green-600 bg-green-50 p-2 rounded text-sm">{{ change.newValue }}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Parse and display values for CREATE entries -->
                <div v-else-if="entry.giaTriMoi" class="space-y-3">
                  <div v-for="field in parseCreateAuditValues(entry.giaTriMoi)" :key="field.field" class="border-b border-surface-200 pb-3 last:border-b-0 last:pb-0">
                    <div class="font-medium text-surface-700 mb-2 text-sm">{{ field.fieldName }}:</div>
                    <div class="text-green-600 bg-green-50 p-2 rounded text-sm">{{ field.value }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty state -->
          <div v-if="auditHistory.length === 0" class="text-center py-8 text-surface-500">
            <i class="pi pi-history text-2xl mb-2"></i>
            <p class="text-base">Chưa có lịch sử thay đổi</p>
          </div>
        </div>

        <!-- Form Actions -->
        <div class="flex justify-end gap-3 pt-6 border-t border-surface-200">
          <Button
            label="Hủy bỏ"
            icon="pi pi-times"
            severity="secondary"
            outlined
            @click="goBack"
          />
          <Button
            type="submit"
            :label="isEdit ? 'Cập nhật' : 'Tạo mới'"
            icon="pi pi-check"
            :loading="loading"
          />
        </div>
      </div>
    </form>

    <!-- Bulk Image Assignment Dialog -->
    <Dialog
      v-model:visible="showBulkImageDialog"
      modal
      header="Gán ảnh hàng loạt cho biến thể"
      :style="{ width: '50rem' }"
      :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
    >
      <div class="space-y-4">
        <p class="text-surface-600">
          Chọn ảnh để gán cho tất cả biến thể đã chọn. Ảnh sẽ được sao chép cho từng biến thể.
        </p>

        <!-- Variant Group Selection -->
        <div class="space-y-2">
          <label class="font-semibold">Chọn nhóm biến thể:</label>
          <p class="text-sm text-surface-600 mb-2">
            Các biến thể có cùng cấu hình sẽ được nhóm lại. Chọn một nhóm để áp dụng ảnh cho tất cả biến thể trong nhóm.
          </p>
          <div class="grid grid-cols-1 gap-2 max-h-40 overflow-y-auto border border-surface-200 rounded p-3">
            <div
              v-for="(group, groupIndex) in getVariantGroups()"
              :key="groupIndex"
              class="flex items-center gap-2 p-2 border border-surface-100 rounded"
            >
              <Checkbox
                v-model="selectedVariantGroupsForBulk"
                :inputId="`group-${groupIndex}`"
                :value="group.signature"
              />
              <label :for="`group-${groupIndex}`" class="text-sm cursor-pointer flex-1">
                <div class="font-medium">{{ group.displayName }}</div>
                <div class="text-xs text-surface-500">{{ group.indices.length }} biến thể</div>
              </label>
            </div>
          </div>
        </div>

        <!-- Image Upload -->
        <div class="space-y-2">
          <label class="font-semibold">Chọn ảnh:</label>
          <div class="border-2 border-dashed border-surface-300 rounded-lg p-4 text-center">
            <div v-if="bulkImagePreview" class="space-y-2">
              <img
                :src="bulkImagePreview"
                alt="Bulk image preview"
                class="w-32 h-32 object-cover rounded mx-auto"
              />
              <Button
                label="Chọn ảnh khác"
                icon="pi pi-refresh"
                severity="secondary"
                outlined
                size="small"
                @click="selectBulkImage"
              />
            </div>
            <div v-else>
              <i class="pi pi-cloud-upload text-4xl text-surface-400 mb-2 block"></i>
              <p class="text-surface-600 mb-2">Nhấp để chọn ảnh</p>
              <Button
                label="Chọn ảnh"
                icon="pi pi-upload"
                @click="selectBulkImage"
              />
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <Button
          label="Hủy"
          icon="pi pi-times"
          severity="secondary"
          outlined
          @click="closeBulkImageDialog"
        />
        <Button
          label="Áp dụng"
          icon="pi pi-check"
          @click="applyBulkImage"
          :disabled="!bulkImageFile || selectedVariantGroupsForBulk.length === 0"
        />
      </template>
    </Dialog>

    <!-- Serial Number Management Dialog -->
    <Dialog
      v-model:visible="showSerialManagementDialog"
      modal
      header="Quản lý Serial Numbers"
      :style="{ width: '60rem' }"
      :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
    >
      <div v-if="selectedVariantForSerial" class="space-y-4">
        <!-- Variant Information -->
        <div class="bg-surface-50 p-4 rounded-lg">
          <h4 class="font-semibold mb-2">Thông tin biến thể:</h4>
          <div class="text-sm space-y-1">
            <div><strong>SKU:</strong> {{ selectedVariantForSerial.sku || 'Sẽ được tự động tạo' }}</div>
            <div><strong>Cấu hình:</strong> {{ getVariantDisplayName(selectedVariantForSerial) }}</div>
            <div><strong>Giá bán:</strong> {{ formatCurrency(selectedVariantForSerial.giaBan) }}</div>
          </div>
        </div>

        <!-- Add Serial Number Section -->
        <div class="space-y-2">
          <label class="font-semibold">Thêm Serial Number:</label>
          <div class="flex gap-2">
            <InputText
              v-model="newSerialNumber"
              placeholder="Nhập serial number (có thể nhập nhiều, cách nhau bằng dấu phẩy: 123456, 789012)"
              class="flex-1"
              @keyup.enter="addSerialNumber"
            />
            <Button
              label="Thêm"
              icon="pi pi-plus"
              @click="addSerialNumber"
              :disabled="!newSerialNumber.trim()"
            />
          </div>
          <small class="text-surface-600">
            Có thể nhập nhiều serial number cùng lúc, cách nhau bằng dấu phẩy (,) hoặc dấu chấm phẩy (;)
          </small>
        </div>

        <!-- Excel Import Section -->
        <div class="space-y-2">
          <label class="font-semibold">Import từ Excel:</label>
          <div class="flex gap-2">
            <Button
              label="Chọn file Excel"
              icon="pi pi-upload"
              severity="secondary"
              outlined
              @click="selectExcelFile"
            />
            <Button
              label="Tải mẫu Excel"
              icon="pi pi-download"
              severity="info"
              outlined
              @click="downloadExcelTemplate"
            />
          </div>
          <small class="text-surface-600">
            Hỗ trợ file .xlsx và .csv. File phải có cột "serial_number".
          </small>
        </div>

        <!-- Serial Numbers List -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="font-semibold">Danh sách Serial Numbers:</label>
            <Badge :value="variantSerialNumbers.length" severity="info" />
          </div>

          <DataTable
            :value="variantSerialNumbers"
            :paginator="variantSerialNumbers.length > 10"
            :rows="10"
            class="p-datatable-sm"
            showGridlines
          >
            <template #empty>
              <div class="text-center py-4">
                <p class="text-surface-600">Chưa có serial number nào</p>
              </div>
            </template>

            <Column field="serialNumberValue" header="Serial Number" sortable>
              <template #body="{ data }">
                <span class="font-mono text-sm">{{ data.serialNumberValue || data.serialNumber }}</span>
              </template>
            </Column>

            <Column field="trangThai" header="Trạng thái" sortable>
              <template #body="{ data }">
                <Badge
                  :value="getSerialStatusLabel(data.trangThai)"
                  :severity="getSerialStatusSeverity(data.trangThai)"
                />
              </template>
            </Column>

            <Column header="Thao tác" style="width: 8rem">
              <template #body="{ data, index }">
                <div class="flex gap-1">
                  <Button
                    icon="pi pi-pencil"
                    severity="warning"
                    text
                    size="small"
                    @click="editSerialNumber(data, index)"
                    v-tooltip="'Chỉnh sửa'"
                  />
                  <Button
                    icon="pi pi-trash"
                    severity="danger"
                    text
                    size="small"
                    @click="removeSerialNumber(index)"
                    v-tooltip="'Xóa'"
                  />
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>

      <template #footer>
        <Button
          label="Hủy"
          icon="pi pi-times"
          severity="secondary"
          outlined
          @click="closeSerialManagementDialog"
        />
        <Button
          label="Lưu"
          icon="pi pi-check"
          @click="saveSerialNumbers"
        />
      </template>
    </Dialog>

    <!-- Serial Number Edit Dialog -->
    <Dialog
      v-model:visible="showSerialEditDialog"
      modal
      header="Chỉnh sửa Serial Number"
      :style="{ width: '30rem' }"
      :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
    >
      <div class="flex flex-col gap-4">
        <div class="field">
          <label for="editSerialValue" class="block text-sm font-medium mb-2">Serial Number</label>
          <InputText
            id="editSerialValue"
            v-model="editSerialForm.serialNumberValue"
            class="w-full"
            placeholder="Nhập serial number"
          />
        </div>

        <div class="field">
          <label for="editSerialStatus" class="block text-sm font-medium mb-2">Trạng thái</label>
          <Select
            id="editSerialStatus"
            v-model="editSerialForm.trangThai"
            :options="serialNumberStatusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Chọn trạng thái"
            class="w-full"
          />
        </div>
      </div>

      <template #footer>
        <Button
          label="Hủy"
          icon="pi pi-times"
          severity="secondary"
          outlined
          @click="cancelSerialEdit"
        />
        <Button
          label="Lưu"
          icon="pi pi-check"
          @click="saveSerialEdit"
        />
      </template>
    </Dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useProductForm } from '@/composables/useProductForm'
import { useAttributeStore } from '@/stores/attributestore'
import { useProductStore } from '@/stores/productstore'
import storageApi from '@/apis/storage'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const attributeStore = useAttributeStore()
const productStore = useProductStore()

// Use composable for form logic
const { productForm, errors, loading, submitForm: submitFormComposable, resetForm } = useProductForm()

// Component state
const isEdit = computed(() => !!route.params.id)
const productId = computed(() => route.params.id)
const auditHistory = ref([])

// Image upload state
const uploadingImages = ref([])
const imagePreviewUrls = ref(Array.from({ length: 5 }, () => null)) // For immediate image previews (5 slots)
const variantImagePreviews = ref([]) // For variant image previews

// Variant generation state - 8 Core Attributes Only
const selectedColors = ref([])
const selectedCpus = ref([])
const selectedRams = ref([])
const selectedGpus = ref([])
const selectedStorage = ref([])
const selectedScreen = ref([])
const generatingVariants = ref(false)

// Bulk operations state (kept for potential future use)
const showBulkImageDialog = ref(false)
const selectedVariantsForBulk = ref([])
const selectedVariantGroupsForBulk = ref([])
const bulkImageFile = ref(null)
const bulkImagePreview = ref(null)

// Serial number management state
const showSerialManagementDialog = ref(false)
const selectedVariantForSerial = ref(null)
const newSerialNumber = ref('')
const variantSerialNumbers = ref([])

// Serial number edit dialog state
const showSerialEditDialog = ref(false)
const editingSerial = ref(null)
const editingSerialIndex = ref(-1)
const editSerialForm = ref({
  serialNumberValue: '',
  trangThai: 'AVAILABLE'
})

// Track serial number changes for backend persistence
const serialNumberChanges = ref({
  toCreate: [], // New serial numbers to create
  toUpdate: [], // Existing serial numbers to update
  toDelete: []  // Existing serial numbers to delete
})

// Serial number status options matching backend enum
const serialNumberStatusOptions = ref([
  { label: 'Có sẵn', value: 'AVAILABLE' },
  { label: 'Đã đặt trước', value: 'RESERVED' },
  { label: 'Đã bán', value: 'SOLD' },
  { label: 'Đã trả lại', value: 'RETURNED' },
  { label: 'Hỏng hóc', value: 'DAMAGED' },
  { label: 'Không khả dụng', value: 'UNAVAILABLE' },
  { label: 'Đang vận chuyển', value: 'IN_TRANSIT' },
  { label: 'Kiểm tra chất lượng', value: 'QUALITY_CONTROL' },
  { label: 'Máy trưng bày', value: 'DISPLAY_UNIT' },
  { label: 'Đã thanh lý', value: 'DISPOSED' }
])

// Computed properties - 8 Core Attributes Only
const categories = computed(() => attributeStore.category)
const brands = computed(() => attributeStore.brand)
const colors = computed(() => attributeStore.colors)
const cpus = computed(() => attributeStore.cpu)
const rams = computed(() => attributeStore.ram)
const gpus = computed(() => attributeStore.gpu)
const storage = computed(() => attributeStore.storage)
const screens = computed(() => attributeStore.screen)

const canGenerateVariants = computed(() => {
  // At least one of the 6 core attributes must be selected and basic requirements met
  const hasAttributes = selectedColors.value.length > 0 ||
                       selectedCpus.value.length > 0 ||
                       selectedRams.value.length > 0 ||
                       selectedGpus.value.length > 0 ||
                       selectedStorage.value.length > 0 ||
                       selectedScreen.value.length > 0

  return hasAttributes
})

// Methods
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}

// SKU Generation and Deduplication Logic
const generateBaseSku = (variant) => {
  const productCode = productForm.value.maSanPham || 'SP'
  const parts = [productCode]

  // Add core attributes to SKU
  if (variant.cpu?.moTaCpu) {
    parts.push(variant.cpu.moTaCpu.replace(/\s+/g, '').toUpperCase().substring(0, 8))
  }
  if (variant.ram?.moTaRam) {
    parts.push(variant.ram.moTaRam.replace(/\s+/g, '').toUpperCase().substring(0, 6))
  }
  if (variant.mauSac?.moTaMauSac) {
    parts.push(variant.mauSac.moTaMauSac.replace(/\s+/g, '').toUpperCase().substring(0, 4))
  }

  return parts.join('-')
}

const checkSkuExists = async (sku) => {
  try {
    // Check against existing products in the store
    const allProducts = productStore.products || []
    for (const product of allProducts) {
      if (product.sanPhamChiTiets) {
        for (const variant of product.sanPhamChiTiets) {
          if (variant.sku === sku) {
            return true
          }
        }
      }
    }

    // Check against current form variants
    if (productForm.value.sanPhamChiTiets) {
      for (const variant of productForm.value.sanPhamChiTiets) {
        if (variant.sku === sku) {
          return true
        }
      }
    }

    return false
  } catch (error) {
    console.warn('Error checking SKU existence:', error)
    return false
  }
}

const generateUniqueSku = async (baseSku) => {
  let candidateSku = baseSku
  let counter = 1

  // Check if base SKU is available
  if (!(await checkSkuExists(candidateSku))) {
    return candidateSku
  }

  // Generate numbered variants until we find a unique one
  while (counter <= 999) {
    candidateSku = `${baseSku}-${String(counter).padStart(3, '0')}`
    if (!(await checkSkuExists(candidateSku))) {
      return candidateSku
    }
    counter++
  }

  // If we can't find a unique SKU after 999 attempts, throw an error
  throw new Error(`Không thể tạo SKU duy nhất cho: ${baseSku}`)
}

const goBack = () => {
  router.push({ name: 'products' })
}

const handleSubmit = async () => {
  const result = await submitFormComposable(isEdit.value)
  if (result) {
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `${isEdit.value ? 'Cập nhật' : 'Thêm'} sản phẩm thành công`,
      life: 3000
    })
    goBack()
  }
}

const handleGenerateVariants = async () => {
  generatingVariants.value = true
  try {
    await generateVariants()
  } catch (error) {
    console.error('Error generating variants:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Lỗi tạo biến thể',
      life: 3000
    })
  } finally {
    generatingVariants.value = false
  }
}

const generateVariants = async () => {
  const variants = []
  const duplicateCount = ref(0)
  const skuErrors = ref([])

  // Create arrays for 6 core attributes, using [null] if empty to ensure at least one iteration
  const attributeArrays = {
    colors: selectedColors.value.length ? selectedColors.value : [null],
    cpus: selectedCpus.value.length ? selectedCpus.value : [null],
    rams: selectedRams.value.length ? selectedRams.value : [null],
    gpus: selectedGpus.value.length ? selectedGpus.value : [null],
    storage: selectedStorage.value.length ? selectedStorage.value : [null],
    screens: selectedScreen.value.length ? selectedScreen.value : [null]
  }

  // Helper function to check if a variant with the same attributes already exists
  const variantExists = (newVariant) => {
    return productForm.value.sanPhamChiTiets?.some(existingVariant => {
      return (
        existingVariant.mauSac?.id === newVariant.mauSac?.id &&
        existingVariant.cpu?.id === newVariant.cpu?.id &&
        existingVariant.ram?.id === newVariant.ram?.id &&
        existingVariant.gpu?.id === newVariant.gpu?.id &&
        existingVariant.oCung?.id === newVariant.oCung?.id &&
        existingVariant.manHinh?.id === newVariant.manHinh?.id
      )
    })
  }

  // Generate all combinations using nested loops (simplified approach for better performance)
  const generateCombinations = async (arrays, current = {}, index = 0) => {
    const keys = Object.keys(arrays)
    if (index === keys.length) {
      // Create variant object
      const newVariant = {
        mauSac: current.colors,
        cpu: current.cpus,
        ram: current.rams,
        gpu: current.gpus,
        boNho: current.storage,
        manHinh: current.screens,
        giaBan: 0, // Will be set individually for each variant
        giaKhuyenMai: null,
        trangThai: true,
        hinhAnh: [],
        serialNumbers: [] // Initialize empty serial numbers array
      }

      // Check for duplicates before adding
      if (variantExists(newVariant)) {
        duplicateCount.value++
      } else {
        // Generate unique SKU for the variant
        try {
          const baseSku = generateBaseSku(newVariant)
          const uniqueSku = await generateUniqueSku(baseSku)
          newVariant.sku = uniqueSku
          variants.push(newVariant)
        } catch (error) {
          console.error('Error generating SKU:', error)
          skuErrors.value.push(error.message)
          // Add variant without SKU - backend will handle it
          variants.push(newVariant)
        }
      }
      return
    }

    const key = keys[index]
    for (const value of arrays[key]) {
      await generateCombinations(arrays, { ...current, [key]: value }, index + 1)
    }
  }

  await generateCombinations(attributeArrays)

  // Add new variants to existing ones
  if (variants.length > 0) {
    if (productForm.value.sanPhamChiTiets && productForm.value.sanPhamChiTiets.length > 0) {
      productForm.value.sanPhamChiTiets.push(...variants)
    } else {
      productForm.value.sanPhamChiTiets = variants
    }

    // Ensure variant image previews array is properly sized while preserving existing values
    const currentLength = variantImagePreviews.value.length
    const requiredLength = productForm.value.sanPhamChiTiets.length

    if (requiredLength > currentLength) {
      // Extend array with null values for new variants while preserving existing previews
      const extensionArray = Array.from({ length: requiredLength - currentLength }, () => null)
      variantImagePreviews.value.push(...extensionArray)
    }
  }

  // Show appropriate toast messages
  if (skuErrors.value.length > 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo SKU',
      detail: `Có ${skuErrors.value.length} lỗi tạo SKU. Backend sẽ xử lý tự động.`,
      life: 4000
    })
  }

  if (variants.length > 0 && duplicateCount.value > 0) {
    toast.add({
      severity: 'info',
      summary: 'Thông báo',
      detail: `Đã tạo ${variants.length} biến thể mới với SKU duy nhất. ${duplicateCount.value} biến thể trùng lặp đã được bỏ qua.`,
      life: 4000
    })
  } else if (variants.length > 0) {
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã tạo ${variants.length} biến thể với SKU duy nhất`,
      life: 3000
    })
  } else if (duplicateCount.value > 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: `Tất cả ${duplicateCount.value} biến thể đã tồn tại. Không có biến thể mới nào được tạo.`,
      life: 4000
    })
  }
}

const removeVariant = (index) => {
  const variant = productForm.value.sanPhamChiTiets[index]

  // If variant has an ID (existing variant), mark it for deletion instead of removing
  if (variant && variant.id) {
    // Mark variant as deleted by setting a special flag
    variant._markedForDeletion = true
    variant.trangThai = false // Set status to inactive

    toast.add({
      severity: 'info',
      summary: 'Đã đánh dấu xóa',
      detail: 'Biến thể sẽ được xóa khi lưu sản phẩm',
      life: 3000
    })
  } else {
    // For new variants (no ID), remove from array immediately
    productForm.value.sanPhamChiTiets.splice(index, 1)
    // Also remove the corresponding image preview
    variantImagePreviews.value.splice(index, 1)

    toast.add({
      severity: 'success',
      summary: 'Đã xóa',
      detail: 'Biến thể mới đã được xóa',
      life: 3000
    })
  }
}

// Visual styling for variants
const getVariantRowClass = (data) => {
  if (data._markedForDeletion) {
    return 'bg-red-50 opacity-60 line-through'
  }
  return ''
}

// Image handling methods
const getImageUrl = async (image) => {
  if (!image) return null
  // If it's already a full URL, return as is
  if (image.startsWith('http')) return image

  try {
    // Get presigned URL for the image filename
    const presignedUrl = await storageApi.getPresignedUrl('products', image)
    return presignedUrl
  } catch (error) {
    console.warn('Error getting presigned URL, using fallback:', error)
    // Fallback: return the filename as-is for now
    // This will be fixed when the backend endpoint is added
    return image
  }
}

const onImageSelect = async (event, slotIndex) => {
  const file = event.target.files[0]
  if (!file) return

  try {
    // Create immediate preview using FileReader (like StaffForm pattern)
    const reader = new FileReader()
    reader.onload = (e) => {
      // Store preview URL for immediate display at the specific slot index
      imagePreviewUrls.value[slotIndex] = e.target.result
    }
    reader.readAsDataURL(file)

    // Add to uploading state
    const uploadItem = {
      name: file.name,
      progress: 0
    }
    uploadingImages.value.push(uploadItem)

    // Upload to MinIO
    const uploadedFilenames = await storageApi.uploadFiles([file], 'products')

    if (uploadedFilenames && uploadedFilenames.length > 0) {
      // Initialize arrays if needed
      if (!productForm.value.hinhAnh) {
        productForm.value.hinhAnh = []
      }

      // Add filename to the product images array
      productForm.value.hinhAnh.push(uploadedFilenames[0])

      // Get presigned URL for the uploaded image and update preview
      try {
        const presignedUrl = await storageApi.getPresignedUrl('products', uploadedFilenames[0])
        imagePreviewUrls.value[slotIndex] = presignedUrl
      } catch (error) {
        console.warn('Could not get presigned URL for preview, using FileReader preview:', error)
        // Keep the FileReader preview for now
      }

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: 'Tải ảnh lên thành công',
        life: 3000
      })
    }
  } catch (error) {
    console.error('Error uploading image:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Lỗi tải ảnh lên',
      life: 3000
    })
  } finally {
    // Remove from uploading state
    uploadingImages.value = uploadingImages.value.filter(item => item.name !== file.name)
    // Clear the input
    event.target.value = ''
  }
}

const removeImage = (index) => {
  // Clear the image at the specific slot
  if (productForm.value.hinhAnh && productForm.value.hinhAnh[index]) {
    productForm.value.hinhAnh[index] = null
  }
  imagePreviewUrls.value[index] = null

  // Compact the arrays by removing null values and shifting remaining items
  if (productForm.value.hinhAnh) {
    const filteredImages = productForm.value.hinhAnh.filter(img => img !== null && img !== undefined)
    productForm.value.hinhAnh = filteredImages
  }

  const filteredPreviews = imagePreviewUrls.value.filter(url => url !== null && url !== undefined)
  // Re-initialize with 5 slots and place filtered previews at the beginning
  imagePreviewUrls.value = Array.from({ length: 5 }, () => null)
  filteredPreviews.forEach((url, i) => {
    if (i < 5) imagePreviewUrls.value[i] = url
  })
}

const selectVariantImage = async (variant, variantIndex) => {
  // Create a file input for variant image selection
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'

  input.onchange = async (event) => {
    const file = event.target.files[0]
    if (!file) return

    try {
      // Create immediate preview
      const reader = new FileReader()
      reader.onload = (e) => {
        variantImagePreviews.value[variantIndex] = e.target.result
      }
      reader.readAsDataURL(file)

      const uploadedFilenames = await storageApi.uploadFiles([file], 'products')

      if (uploadedFilenames && uploadedFilenames.length > 0) {
        if (!variant.hinhAnh) {
          variant.hinhAnh = []
        }
        variant.hinhAnh = [uploadedFilenames[0]] // Set as the primary image

        // Get presigned URL for the uploaded image and update preview
        try {
          const presignedUrl = await storageApi.getPresignedUrl('products', uploadedFilenames[0])
          variantImagePreviews.value[variantIndex] = presignedUrl
        } catch (error) {
          console.warn('Could not get presigned URL for variant preview:', error)
        }

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Tải ảnh biến thể thành công',
          life: 3000
        })
      }
    } catch (error) {
      console.error('Error uploading variant image:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: error.message || 'Lỗi tải ảnh biến thể',
        life: 3000
      })
    }
  }

  input.click()
}

// Serial number management methods - Use pre-loaded data
const manageSerialNumbers = async (variant) => {
  selectedVariantForSerial.value = variant
  newSerialNumber.value = ''
  showSerialManagementDialog.value = true

  // Clear previous changes tracking
  serialNumberChanges.value = {
    toCreate: [],
    toUpdate: [],
    toDelete: []
  }

  // Use pre-loaded serial numbers (loaded automatically in edit mode)
  if (!variant.serialNumbers) {
    variant.serialNumbers = []
  }
  variantSerialNumbers.value = [...variant.serialNumbers]

  console.log(`Opening serial management for variant ${variant.id || 'new'} with ${variantSerialNumbers.value.length} serial numbers`)
}

// Helper function to check for cross-variant duplicates
const checkCrossVariantDuplicates = (serialNumber) => {
  return productForm.value.sanPhamChiTiets.some(variant => {
    // Skip the current variant being edited
    if (variant === selectedVariantForSerial.value) return false

    return variant.serialNumbers?.some(serial =>
      (serial.serialNumberValue || serial.serialNumber) === serialNumber
    )
  })
}

const addSerialNumber = () => {
  if (!newSerialNumber.value.trim()) return

  // Split by comma and semicolon for batch input
  const serialNumbers = newSerialNumber.value
    .split(/[,;]/)
    .map(s => s.trim())
    .filter(s => s.length > 0)

  if (serialNumbers.length === 0) return

  const newSerials = []
  const duplicates = []
  const crossVariantDuplicates = []
  const errors = []

  // Process each serial number
  for (const serialNumber of serialNumbers) {
    // Check for duplicates in existing list
    const existsInCurrent = variantSerialNumbers.value.some(
      serial => (serial.serialNumberValue || serial.serialNumber) === serialNumber
    )

    // Check for duplicates in the batch being added
    const existsInBatch = newSerials.some(
      serial => serial.serialNumberValue === serialNumber
    )

    // Check for cross-variant duplicates
    const existsInOtherVariants = checkCrossVariantDuplicates(serialNumber)

    if (existsInCurrent || existsInBatch) {
      duplicates.push(serialNumber)
      continue
    }

    if (existsInOtherVariants) {
      crossVariantDuplicates.push(serialNumber)
      continue
    }

    // Validate serial number (basic validation)
    if (serialNumber.length < 3) {
      errors.push(`Serial number "${serialNumber}" quá ngắn (tối thiểu 3 ký tự)`)
      continue
    }

    const newSerial = {
      serialNumberValue: serialNumber,
      serialNumber: serialNumber, // For backward compatibility
      trangThai: 'AVAILABLE',
      _isNew: true // Mark as new for tracking
    }

    newSerials.push(newSerial)

    // Track for backend creation if variant has ID
    if (selectedVariantForSerial.value?.id) {
      serialNumberChanges.value.toCreate.push({
        ...newSerial,
        sanPhamChiTietId: selectedVariantForSerial.value.id
      })
    }
  }

  // Add valid serial numbers to the list
  if (newSerials.length > 0) {
    variantSerialNumbers.value.push(...newSerials)
  }

  // Clear input
  newSerialNumber.value = ''

  // Show results
  if (newSerials.length > 0) {
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: `Đã thêm ${newSerials.length} serial number${newSerials.length > 1 ? 's' : ''}`,
      life: 3000
    })
  }

  if (duplicates.length > 0) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: `${duplicates.length} serial number đã tồn tại trong biến thể này: ${duplicates.slice(0, 3).join(', ')}${duplicates.length > 3 ? '...' : ''}`,
      life: 4000
    })
  }

  // Show warnings for cross-variant duplicates
  if (crossVariantDuplicates.length > 0) {
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: `${crossVariantDuplicates.length} serial number đã tồn tại ở biến thể khác: ${crossVariantDuplicates.slice(0, 3).join(', ')}${crossVariantDuplicates.length > 3 ? '...' : ''}`,
      life: 6000
    })
  }

  if (errors.length > 0) {
    errors.forEach(error => {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: error,
        life: 4000
      })
    })
  }
}

const editSerialNumber = (serial, index) => {
  console.log('Editing serial number:', serial)

  // Set up edit dialog
  editingSerial.value = serial
  editingSerialIndex.value = index
  editSerialForm.value = {
    serialNumberValue: serial.serialNumberValue || serial.serialNumber || '',
    trangThai: serial.trangThai || 'AVAILABLE'
  }
  showSerialEditDialog.value = true
}

const saveSerialEdit = () => {
  const newValue = editSerialForm.value.serialNumberValue.trim()
  const newStatus = editSerialForm.value.trangThai
  const index = editingSerialIndex.value
  const serial = editingSerial.value

  if (!newValue) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Serial number không được để trống',
      life: 3000
    })
    return
  }

  const currentValue = serial.serialNumberValue || serial.serialNumber
  const currentStatus = serial.trangThai

  // Check if anything changed
  if (newValue === currentValue && newStatus === currentStatus) {
    showSerialEditDialog.value = false
    return
  }

  // Check for duplicates if serial number value changed
  if (newValue !== currentValue) {
    // Check for duplicates within current variant
    const existsInCurrent = variantSerialNumbers.value.some(
      (s, i) => i !== index && (s.serialNumberValue || s.serialNumber) === newValue
    )

    if (existsInCurrent) {
      toast.add({
        severity: 'warn',
        summary: 'Cảnh báo',
        detail: 'Serial number đã tồn tại trong biến thể này',
        life: 3000
      })
      return
    }

    // Check for cross-variant duplicates
    const existsInOtherVariants = checkCrossVariantDuplicates(newValue)
    if (existsInOtherVariants) {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Serial number đã tồn tại ở biến thể khác',
        life: 4000
      })
      return
    }
  }

  // Update the serial number in the array
  const oldValue = variantSerialNumbers.value[index].serialNumberValue
  const oldStatus = variantSerialNumbers.value[index].trangThai

  variantSerialNumbers.value[index].serialNumberValue = newValue
  variantSerialNumbers.value[index].serialNumber = newValue // For backward compatibility
  variantSerialNumbers.value[index].trangThai = newStatus

  // Track for backend update if this is an existing serial number
  if (serial.id && selectedVariantForSerial.value?.id) {
    serialNumberChanges.value.toUpdate.push({
      id: serial.id,
      serialNumberValue: newValue,
      sanPhamChiTietId: selectedVariantForSerial.value.id,
      trangThai: newStatus,
      oldValue: oldValue,
      oldStatus: oldStatus
    })
  }

  showSerialEditDialog.value = false

  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Đã cập nhật serial number',
    life: 2000
  })
}

const cancelSerialEdit = () => {
  showSerialEditDialog.value = false
  editingSerial.value = null
  editingSerialIndex.value = -1
  editSerialForm.value = {
    serialNumberValue: '',
    trangThai: 'AVAILABLE'
  }
}

const removeSerialNumber = (index) => {
  const serial = variantSerialNumbers.value[index]

  // Track for backend deletion if this is an existing serial number
  if (serial.id && selectedVariantForSerial.value?.id) {
    serialNumberChanges.value.toDelete.push({
      id: serial.id,
      serialNumberValue: serial.serialNumberValue || serial.serialNumber
    })
  }

  variantSerialNumbers.value.splice(index, 1)
  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Đã xóa serial number',
    life: 2000
  })
}

const selectExcelFile = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.xlsx,.csv'

  input.onchange = (event) => {
    const file = event.target.files[0]
    if (!file) return

    if (file.name.endsWith('.csv')) {
      parseCSVFile(file)
    } else {
      toast.add({
        severity: 'info',
        summary: 'Thông báo',
        detail: 'Hiện tại chỉ hỗ trợ file CSV. Vui lòng sử dụng file .csv',
        life: 3000
      })
    }
  }

  input.click()
}

const parseCSVFile = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const csv = e.target.result
      const lines = csv.split('\n')
      const headers = lines[0].split(',').map(h => h.trim().toLowerCase())

      // Find serial number column
      const serialColumnIndex = headers.findIndex(h =>
        h.includes('serial') || h.includes('sn') || h.includes('serial_number')
      )

      if (serialColumnIndex === -1) {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: 'Không tìm thấy cột serial number trong file CSV',
          life: 3000
        })
        return
      }

      const newSerials = []
      for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim()
        if (!line) continue

        const columns = line.split(',')
        const serialNumber = columns[serialColumnIndex]?.trim()

        if (serialNumber && serialNumber !== '') {
          // Check for duplicates
          const exists = variantSerialNumbers.value.some(
            serial => (serial.serialNumberValue || serial.serialNumber) === serialNumber
          )

          if (!exists) {
            newSerials.push({
              serialNumberValue: serialNumber,
              serialNumber: serialNumber, // For backward compatibility
              trangThai: 'AVAILABLE'
            })
          }
        }
      }

      if (newSerials.length > 0) {
        variantSerialNumbers.value.push(...newSerials)

        // Track for backend creation if variant has ID
        if (selectedVariantForSerial.value?.id) {
          newSerials.forEach(serial => {
            serialNumberChanges.value.toCreate.push({
              ...serial,
              sanPhamChiTietId: selectedVariantForSerial.value.id
            })
          })
        }

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: `Đã import ${newSerials.length} serial numbers từ CSV`,
          life: 3000
        })
      } else {
        toast.add({
          severity: 'warn',
          summary: 'Cảnh báo',
          detail: 'Không có serial number mới nào được thêm',
          life: 3000
        })
      }
    } catch (error) {
      console.error('Error parsing CSV:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Lỗi đọc file CSV. Vui lòng kiểm tra định dạng file.',
        life: 3000
      })
    }
  }

  reader.readAsText(file)
}

const removeVariantImage = (variant, index) => {
  // Remove image from variant
  variant.hinhAnh = []

  // Clear preview
  if (variantImagePreviews.value[index]) {
    variantImagePreviews.value[index] = null
  }

  toast.add({
    severity: 'success',
    summary: 'Thành công',
    detail: 'Đã xóa ảnh biến thể',
    life: 2000
  })
}

const downloadExcelTemplate = () => {
  // Create a simple CSV template
  const csvContent = 'serial_number\nSN001\nSN002\nSN003'
  const blob = new Blob([csvContent], { type: 'text/csv' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'serial_numbers_template.csv'
  a.click()
  window.URL.revokeObjectURL(url)
}

const getSerialStatusLabel = (status) => {
  const statusMap = {
    'AVAILABLE': 'Có sẵn',
    'RESERVED': 'Đã đặt trước',
    'SOLD': 'Đã bán',
    'RETURNED': 'Đã trả lại',
    'DAMAGED': 'Hỏng hóc',
    'UNAVAILABLE': 'Không khả dụng',
    'IN_TRANSIT': 'Đang vận chuyển',
    'QUALITY_CONTROL': 'Kiểm tra chất lượng',
    'DISPLAY_UNIT': 'Máy trưng bày',
    'DISPOSED': 'Đã thanh lý'
  }
  return statusMap[status] || status
}

const getSerialStatusSeverity = (status) => {
  const severityMap = {
    'AVAILABLE': 'success',
    'RESERVED': 'warning',
    'SOLD': 'info',
    'RETURNED': 'secondary',
    'DAMAGED': 'danger',
    'UNAVAILABLE': 'danger',
    'IN_TRANSIT': 'warning',
    'QUALITY_CONTROL': 'warning',
    'DISPLAY_UNIT': 'info',
    'DISPOSED': 'secondary'
  }
  return severityMap[status] || 'secondary'
}

// Helper methods for serial number counting
const getAvailableSerialCount = (variant) => {
  if (!variant.serialNumbers) return 0
  return variant.serialNumbers.filter(serial => serial.trangThai === 'AVAILABLE').length
}

const getReservedSerialCount = (variant) => {
  if (!variant.serialNumbers) return 0
  return variant.serialNumbers.filter(serial => serial.trangThai === 'RESERVED').length
}

// Persist serial number changes to backend
const persistSerialNumberChanges = async () => {
  if (!selectedVariantForSerial.value?.id) {
    // For new variants, changes will be handled when the product is saved
    return { success: true, message: 'Changes will be saved with product' }
  }

  try {
    // Import the serial number API
    const { default: serialNumberApi } = await import('@/apis/serialNumberApi')

    let successCount = 0
    let errorCount = 0
    const errors = []

    // Process deletions first
    for (const deleteItem of serialNumberChanges.value.toDelete) {
      try {
        await serialNumberApi.deleteSerialNumber(deleteItem.id, 'Xóa từ form quản lý sản phẩm')
        successCount++
        console.log(`Deleted serial number: ${deleteItem.serialNumberValue}`)
      } catch (error) {
        errorCount++
        const errorMessage = error.response?.status === 403
          ? 'Không có quyền xóa serial number'
          : error.message || 'Lỗi không xác định'
        errors.push(`Lỗi xóa ${deleteItem.serialNumberValue}: ${errorMessage}`)
        console.error('Error deleting serial number:', error)
      }
    }

    // Process updates
    for (const updateItem of serialNumberChanges.value.toUpdate) {
      try {
        // Check if only status changed or if other fields changed too
        const statusChanged = updateItem.trangThai !== updateItem.oldStatus
        const valueChanged = updateItem.serialNumberValue !== updateItem.oldValue

        if (statusChanged && !valueChanged) {
          // Only status changed - use the status change endpoint
          console.log(`Updating status for serial number ${updateItem.serialNumberValue} from ${updateItem.oldStatus} to ${updateItem.trangThai}`)
          const updateResult = await serialNumberApi.updateSerialNumberStatus(
            updateItem.id,
            updateItem.trangThai,
            'Cập nhật trạng thái từ form quản lý sản phẩm'
          )
          console.log('Status update result from backend:', updateResult)
        } else if (valueChanged && !statusChanged) {
          // Only value changed - use the regular update endpoint
          const updateData = {
            serialNumberValue: updateItem.serialNumberValue,
            sanPhamChiTietId: updateItem.sanPhamChiTietId
          }
          console.log('Updating serial number value with data:', updateData)
          const updateResult = await serialNumberApi.updateSerialNumber(updateItem.id, updateData)
          console.log('Value update result from backend:', updateResult)
        } else if (valueChanged && statusChanged) {
          // Both changed - update value first, then status
          const updateData = {
            serialNumberValue: updateItem.serialNumberValue,
            sanPhamChiTietId: updateItem.sanPhamChiTietId
          }
          console.log('Updating serial number value and status with data:', updateData)
          await serialNumberApi.updateSerialNumber(updateItem.id, updateData)
          await serialNumberApi.updateSerialNumberStatus(
            updateItem.id,
            updateItem.trangThai,
            'Cập nhật trạng thái từ form quản lý sản phẩm'
          )
          console.log('Both value and status updated successfully')
        }

        successCount++
        console.log(`Updated serial number: ${updateItem.oldValue} -> ${updateItem.serialNumberValue} with status: ${updateItem.oldStatus} -> ${updateItem.trangThai}`)
      } catch (error) {
        errorCount++
        const errorMessage = error.response?.status === 403
          ? 'Không có quyền cập nhật serial number'
          : error.message || 'Lỗi không xác định'
        errors.push(`Lỗi cập nhật ${updateItem.serialNumberValue}: ${errorMessage}`)
        console.error('Error updating serial number:', error)
      }
    }

    // Process creations
    for (const createItem of serialNumberChanges.value.toCreate) {
      try {
        await serialNumberApi.createSerialNumber({
          serialNumberValue: createItem.serialNumberValue,
          sanPhamChiTietId: createItem.sanPhamChiTietId,
          trangThai: createItem.trangThai || 'AVAILABLE'
        })
        successCount++
        console.log(`Created serial number: ${createItem.serialNumberValue}`)
      } catch (error) {
        errorCount++
        const errorMessage = error.response?.status === 403
          ? 'Không có quyền tạo serial number'
          : error.message || 'Lỗi không xác định'
        errors.push(`Lỗi tạo ${createItem.serialNumberValue}: ${errorMessage}`)
        console.error('Error creating serial number:', error)
      }
    }

    // Clear the changes after processing
    serialNumberChanges.value = {
      toCreate: [],
      toUpdate: [],
      toDelete: []
    }

    return {
      success: errorCount === 0,
      successCount,
      errorCount,
      errors,
      message: errorCount === 0
        ? `Đã lưu thành công ${successCount} thay đổi serial number`
        : `Lưu thành công ${successCount}, lỗi ${errorCount} thay đổi`
    }
  } catch (error) {
    console.error('Error persisting serial number changes:', error)
    return {
      success: false,
      message: 'Lỗi khi lưu thay đổi serial number: ' + error.message
    }
  }
}

const closeSerialManagementDialog = () => {
  // Save serial numbers back to the variant
  if (selectedVariantForSerial.value) {
    selectedVariantForSerial.value.serialNumbers = [...variantSerialNumbers.value]
  }

  showSerialManagementDialog.value = false
  selectedVariantForSerial.value = null
  variantSerialNumbers.value = []
  newSerialNumber.value = ''
}

const saveSerialNumbers = async () => {
  if (selectedVariantForSerial.value) {
    try {
      // Persist changes to backend if variant exists
      const result = await persistSerialNumberChanges()

      // Update the variant's serial numbers locally
      selectedVariantForSerial.value.serialNumbers = [...variantSerialNumbers.value]

      // Show appropriate toast message
      toast.add({
        severity: result.success ? 'success' : 'warn',
        summary: result.success ? 'Thành công' : 'Cảnh báo',
        detail: result.message,
        life: result.success ? 3000 : 5000
      })

      // Show errors if any
      if (result.errors && result.errors.length > 0) {
        result.errors.forEach(error => {
          toast.add({
            severity: 'error',
            summary: 'Lỗi chi tiết',
            detail: error,
            life: 5000
          })
        })
      }

      closeSerialManagementDialog()
    } catch (error) {
      console.error('Error saving serial numbers:', error)
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: 'Không thể lưu serial numbers: ' + error.message,
        life: 5000
      })
    }
  }
}

const getVariantDisplayName = (variant) => {
  const parts = []
  // 6 Core Product Attributes
  if (variant.mauSac) parts.push(variant.mauSac.moTaMauSac)
  if (variant.cpu) parts.push(variant.cpu.moTaCpu)
  if (variant.ram) parts.push(variant.ram.moTaRam)
  if (variant.gpu) parts.push(variant.gpu.moTaGpu)
  if (variant.oCung) parts.push(variant.oCung.moTaOCung)
  if (variant.manHinh) parts.push(variant.manHinh.moTaManHinh)
  return parts.length > 0 ? parts.join(' - ') : 'Biến thể'
}

// Helper method to get variant attribute signature for grouping
const getVariantAttributeSignature = (variant) => {
  const attributes = {
    // 6 Core Product Attributes
    mauSac: variant.mauSac?.id || null,
    cpu: variant.cpu?.id || null,
    ram: variant.ram?.id || null,
    gpu: variant.gpu?.id || null,
    boNho: variant.boNho?.id || variant.oCung?.id || null,
    manHinh: variant.manHinh?.id || null
  }
  return JSON.stringify(attributes)
}

// Group variants by identical attribute combinations
const getVariantGroups = () => {
  const groups = new Map()

  productForm.value.sanPhamChiTiets.forEach((variant, index) => {
    const signature = getVariantAttributeSignature(variant)
    if (!groups.has(signature)) {
      groups.set(signature, {
        signature,
        displayName: getVariantDisplayName(variant),
        indices: []
      })
    }
    groups.get(signature).indices.push(index)
  })

  return Array.from(groups.values())
}

const selectBulkImage = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'

  input.onchange = (event) => {
    const file = event.target.files[0]
    if (!file) return

    bulkImageFile.value = file

    // Create preview
    const reader = new FileReader()
    reader.onload = (e) => {
      bulkImagePreview.value = e.target.result
    }
    reader.readAsDataURL(file)
  }

  input.click()
}

const closeBulkImageDialog = () => {
  showBulkImageDialog.value = false
  selectedVariantsForBulk.value = []
  selectedVariantGroupsForBulk.value = []
  bulkImageFile.value = null
  bulkImagePreview.value = null
}

const applyBulkImage = async () => {
  if (!bulkImageFile.value || selectedVariantGroupsForBulk.value.length === 0) return

  try {
    // Upload the image
    const uploadedFilenames = await storageApi.uploadFiles([bulkImageFile.value], 'products')

    if (uploadedFilenames && uploadedFilenames.length > 0) {
      const filename = uploadedFilenames[0]

      // Get presigned URL for preview
      let presignedUrl = null
      try {
        presignedUrl = await storageApi.getPresignedUrl('products', filename)
      } catch (error) {
        console.warn('Could not get presigned URL for bulk image:', error)
      }

      // Get all variant indices from selected groups
      const allVariantIndices = []
      const variantGroups = getVariantGroups()

      selectedVariantGroupsForBulk.value.forEach(selectedSignature => {
        const group = variantGroups.find(g => g.signature === selectedSignature)
        if (group) {
          allVariantIndices.push(...group.indices)
        }
      })

      // Ensure variantImagePreviews array is properly sized while preserving existing values
      const currentLength = variantImagePreviews.value.length
      const requiredLength = productForm.value.sanPhamChiTiets.length

      if (requiredLength > currentLength) {
        // Extend array with null values for new variants while preserving existing previews
        const extensionArray = Array.from({ length: requiredLength - currentLength }, () => null)
        variantImagePreviews.value.push(...extensionArray)
      }

      // Apply to all variants in selected groups
      allVariantIndices.forEach(variantIndex => {
        const variant = productForm.value.sanPhamChiTiets[variantIndex]
        if (!variant.hinhAnh) {
          variant.hinhAnh = []
        }
        variant.hinhAnh = [filename]

        // Update preview - ensure index exists
        if (presignedUrl && variantIndex < variantImagePreviews.value.length) {
          variantImagePreviews.value[variantIndex] = presignedUrl
        }
      })

      toast.add({
        severity: 'success',
        summary: 'Thành công',
        detail: `Đã gán ảnh cho ${allVariantIndices.length} biến thể trong ${selectedVariantGroupsForBulk.value.length} nhóm`,
        life: 3000
      })

      closeBulkImageDialog()
    }
  } catch (error) {
    console.error('Error uploading bulk image:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: error.message || 'Lỗi tải ảnh lên',
      life: 3000
    })
  }
}

// Audit trail helper methods
const getAuditBorderColor = (action) => {
  switch (action) {
    case 'CREATE': return 'border-green-400'
    case 'UPDATE': return 'border-blue-400'
    case 'DELETE': return 'border-red-400'
    default: return 'border-surface-300'
  }
}

const getAuditIcon = (action) => {
  switch (action) {
    case 'CREATE': return 'pi pi-plus'
    case 'UPDATE': return 'pi pi-pencil'
    case 'DELETE': return 'pi pi-trash'
    default: return 'pi pi-info'
  }
}

const getAuditIconColor = (action) => {
  switch (action) {
    case 'CREATE': return 'text-green-600'
    case 'UPDATE': return 'text-blue-600'
    case 'DELETE': return 'text-red-600'
    default: return 'text-surface-600'
  }
}

const getActionDisplayName = (action) => {
  switch (action) {
    case 'CREATE': return 'Tạo mới'
    case 'UPDATE': return 'Cập nhật'
    case 'DELETE': return 'Xóa'
    default: return action
  }
}

const formatAuditDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

const parseAuditChanges = (oldValue, newValue) => {
  try {
    const oldData = typeof oldValue === 'string' ? JSON.parse(oldValue) : oldValue
    const newData = typeof newValue === 'string' ? JSON.parse(newValue) : newValue

    const changes = []
    const allKeys = new Set([...Object.keys(oldData || {}), ...Object.keys(newData || {})])

    allKeys.forEach(key => {
      if (oldData[key] !== newData[key]) {
        changes.push({
          field: key,
          fieldName: getFieldDisplayName(key),
          oldValue: formatFieldValue(oldData[key]),
          newValue: formatFieldValue(newData[key])
        })
      }
    })

    return changes
  } catch (error) {
    console.error('Error parsing audit changes:', error)
    return []
  }
}

const parseCreateAuditValues = (value) => {
  try {
    const data = typeof value === 'string' ? JSON.parse(value) : value

    return Object.keys(data || {}).map(key => ({
      field: key,
      fieldName: getFieldDisplayName(key),
      value: formatFieldValue(data[key])
    }))
  } catch (error) {
    console.error('Error parsing audit values:', error)
    return []
  }
}

const getFieldDisplayName = (field) => {
  const fieldNames = {
    tenSanPham: 'Tên sản phẩm',
    maSanPham: 'Mã sản phẩm',
    moTa: 'Mô tả',
    trangThai: 'Trạng thái',
    ngayRaMat: 'Ngày ra mắt',
    thuongHieu: 'Thương hiệu',
    danhMucs: 'Danh mục',
    hinhAnh: 'Hình ảnh'
  }
  return fieldNames[field] || field
}

const formatFieldValue = (value) => {
  if (value === null || value === undefined) return 'Không có'
  if (typeof value === 'boolean') return value ? 'Có' : 'Không'
  if (Array.isArray(value)) return value.length > 0 ? `${value.length} mục` : 'Trống'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

// Auto-load serial numbers for all variants in edit mode
const loadSerialNumbersForAllVariants = async () => {
  if (!productForm.value.sanPhamChiTiets || productForm.value.sanPhamChiTiets.length === 0) {
    return
  }

  try {
    // Import the serial number API
    const { default: serialNumberApi } = await import('@/apis/serialNumberApi')

    // Load serial numbers for each variant that has an ID
    for (const variant of productForm.value.sanPhamChiTiets) {
      if (variant.id) {
        try {
          const serialNumbers = await serialNumberApi.getSerialNumbersByVariant(variant.id)
          // Store serial numbers in the variant object
          variant.serialNumbers = serialNumbers || []
          console.log(`Loaded ${variant.serialNumbers.length} serial numbers for variant ${variant.id}:`)
          serialNumbers.forEach(sn => {
            console.log(`  - Serial: ${sn.serialNumberValue || sn.serialNumber}, Status: ${sn.trangThai}, ID: ${sn.id}`)
          })
        } catch (error) {
          console.warn(`Could not load serial numbers for variant ${variant.id}:`, error)
          // Initialize empty array as fallback
          variant.serialNumbers = []
        }
      } else {
        // For new variants without ID, initialize empty array
        variant.serialNumbers = []
      }
    }
  } catch (error) {
    console.error('Error loading serial numbers for variants:', error)
    // Initialize empty arrays for all variants as fallback
    productForm.value.sanPhamChiTiets.forEach(variant => {
      if (!variant.serialNumbers) {
        variant.serialNumbers = []
      }
    })
  }
}

const loadProduct = async () => {
  if (isEdit.value && productId.value) {
    try {
      // Fetch product by ID from API to get complete data including all variant attributes
      const product = await productStore.fetchProductById(productId.value)

      if (product) {
        // Convert single danhMuc to danhMucs array for MultiSelect
        const productData = { ...product }
        if (productData.danhMuc && !productData.danhMucs) {
          productData.danhMucs = [productData.danhMuc]
        }
        Object.assign(productForm.value, productData)

        // Initialize image previews for existing product images
        imagePreviewUrls.value = Array.from({ length: 5 }, () => null) // Initialize with 5 slots
        if (productData.hinhAnh && productData.hinhAnh.length > 0) {
          // Load presigned URLs for existing images
          for (let i = 0; i < productData.hinhAnh.length; i++) {
            try {
              const presignedUrl = await storageApi.getPresignedUrl('products', productData.hinhAnh[i])
              imagePreviewUrls.value[i] = presignedUrl
            } catch (error) {
              console.warn(`Could not load preview for image ${i}:`, error)
              // Fallback: use filename as-is for now
              imagePreviewUrls.value[i] = productData.hinhAnh[i]
            }
          }
        }

        // Initialize variant image previews
        if (productData.sanPhamChiTiets && productData.sanPhamChiTiets.length > 0) {
          variantImagePreviews.value = Array.from({ length: productData.sanPhamChiTiets.length }, () => null)
          // Load presigned URLs for existing variant images
          for (let i = 0; i < productData.sanPhamChiTiets.length; i++) {
            const variant = productData.sanPhamChiTiets[i]
            if (variant.hinhAnh && variant.hinhAnh.length > 0) {
              try {
                const presignedUrl = await storageApi.getPresignedUrl('products', variant.hinhAnh[0])
                variantImagePreviews.value[i] = presignedUrl
              } catch (error) {
                console.warn(`Could not load preview for variant ${i}:`, error)
              }
            }
          }

          // Auto-load serial numbers for all existing variants
          await loadSerialNumbersForAllVariants()
        }
      } else {
        throw new Error('Không tìm thấy sản phẩm')
      }
    } catch (error) {
      toast.add({
        severity: 'error',
        summary: 'Lỗi',
        detail: error.message || 'Lỗi tải dữ liệu sản phẩm',
        life: 3000
      })
      goBack()
    }
  }
}

const loadAuditHistory = async () => {
  if (isEdit.value && productId.value) {
    try {
      auditHistory.value = await productStore.fetchProductAuditHistory(productId.value)
    } catch (error) {
      console.error('Error loading audit history:', error)
    }
  }
}

// Lifecycle
onMounted(async () => {
  try {
    await Promise.all([
      attributeStore.fetchAllAttributes(),
      productStore.fetchProducts()
    ])

    await loadProduct()
    await loadAuditHistory()
  } catch (error) {
    console.error('Error loading data:', error)
    toast.add({
      severity: 'error',
      summary: 'Lỗi',
      detail: 'Lỗi tải dữ liệu. Vui lòng thử lại.',
      life: 3000
    })
  }
})

// Watch for route changes
watch(() => route.params.id, () => {
  if (route.params.id) {
    loadProduct()
    loadAuditHistory()
  } else {
    resetForm()
  }
})
</script>
