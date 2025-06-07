<template>
  <div class="customer-list">
    <Toolbar class="mb-4">
      <template #start>
        <Button
          label="Thêm khách hàng"
          icon="pi pi-plus"
          class=" mr-2"
          @click="goToAdd"
        />
        <Button
          label="In"
          icon="pi pi-print"
          class="mr-2"
          severity="secondary"
          @click="printPDF"
        />
        <Button
          label="Xuất"
          icon="pi pi-upload"
          class="mr-2"
          severity="secondary"
          @click="exportExcel"
        />
      </template>
      <template #end>
        <ToggleButton
          v-model="showInactive"
          onLabel="Hiển thị không hoạt động"
          offLabel="Ẩn không hoạt động"
          class="w-25rem"
        />
      </template>
    </Toolbar>

    <UserTable
      :users="filteredCustomers"
      :loading="loading"
      title="Danh sách khách hàng"
      @edit="goToEdit"
      @delete="confirmDelete"
      @restore="confirmRestore"
      ref="customerTable"
    />

    <Toast />
    <ConfirmDialog />
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useCustomerStore } from '@/stores/customerstore'
import UserTable from '@/components/UserTable.vue'
import Toolbar from 'primevue/toolbar'
import Button from 'primevue/button'
import ToggleButton from 'primevue/togglebutton'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import * as XLSX from 'xlsx'

export default {
  components: {
    UserTable,
    Toolbar,
    Button,
    ToggleButton,
    Toast,
    ConfirmDialog,
  },
  setup() {
    const router = useRouter()
    const toast = useToast()
    const confirm = useConfirm()
    const customerStore = useCustomerStore()
    const showInactive = ref(false)
    const loading = ref(false)
    const customerTable = ref(null)

    // Load customers on mount
    onMounted(async () => {
      loading.value = true
      try {
        await customerStore.fetchCustomers()
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể tải danh sách khách hàng',
          life: 3000,
        })
      } finally {
        loading.value = false
      }
    })

    // Computed properties
    const filteredCustomers = computed(() => {
      return showInactive.value
        ? [...customerStore.activeCustomers, ...customerStore.inactiveCustomers]
        : customerStore.activeCustomers
    })

    // Navigation methods
    const goToAdd = () => {
      router.push({ name: 'CustomerAdd' })
    }

    const goToEdit = (customer) => {
      router.push({ name: 'CustomerEdit', params: { id: customer.id } })
    }

    // Delete confirmation
    const confirmDelete = (customer) => {
      confirm.require({
        message: `Bạn có chắc chắn muốn vô hiệu hóa khách hàng ${customer.hoTen}?`,
        header: 'Xác nhận',
        icon: 'pi pi-exclamation-triangle',
        accept: () => deleteCustomer(customer.id),
      })
    }

    const deleteCustomer = async (id) => {
      try {
        await customerStore.deleteCustomer(id)
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Khách hàng đã được vô hiệu hóa',
          life: 3000,
        })
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể vô hiệu hóa khách hàng',
          life: 3000,
        })
      }
    }

    // Restore confirmation
    const confirmRestore = (customer) => {
      confirm.require({
        message: `Bạn có chắc chắn muốn khôi phục khách hàng ${customer.hoTen}?`,
        header: 'Xác nhận',
        icon: 'pi pi-exclamation-triangle',
        accept: () => restoreCustomer(customer.id),
      })
    }

    const restoreCustomer = async (id) => {
      try {
        await customerStore.restoreCustomer(id)
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Khách hàng đã được khôi phục',
          life: 3000,
        })
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể khôi phục khách hàng',
          life: 3000,
        })
      }
    }

    // Format địa chỉ mặc định
    const formatDefaultAddress = (diaChis) => {
      if (!diaChis || diaChis.length === 0) return 'Không có';

      // Lấy địa chỉ mặc định hoặc địa chỉ đầu tiên
      const diaChi = diaChis.find(d => d.laMacDinh) || diaChis[0];

      return `${diaChi.duong}, ${diaChi.phuongXa}, ${diaChi.quanHuyen}, ${diaChi.tinhThanh}, ${diaChi.quocGia}`;
    }

    // Export to Excel function
    const exportExcel = () => {
      try {
        const customers = filteredCustomers.value

        // Transform data for Customers sheet
        const exportData = customers.map((customer) => ({
          'ID': customer.id,
          'Mã Người Dùng': customer.maNguoiDung,
          'Họ Tên': customer.hoTen,
          'Giới Tính': customer.gioiTinh,
          'Ngày Sinh': customer.ngaySinh,
          'Số Điện Thoại': customer.soDienThoai,
          'Email': customer.email,
          'Địa Chỉ Mặc Định': formatDefaultAddress(customer.diaChis),
          'Trạng Thái': customer.trangThai !== false ? 'Hoạt động' : 'Không hoạt động'
        }))

        // Transform data for Addresses sheet
        let addressesData = [];
        customers.forEach(customer => {
          if (customer.diaChis && customer.diaChis.length > 0) {
            customer.diaChis.forEach(address => {
              addressesData.push({
                'ID Người Dùng': customer.id,
                'Họ Tên Người Dùng': customer.hoTen,
                'ID Địa Chỉ': address.id,
                'Loại Địa Chỉ': address.loaiDiaChi,
                'Đường': address.duong,
                'Phường/Xã': address.phuongXa,
                'Quận/Huyện': address.quanHuyen,
                'Tỉnh/Thành': address.tinhThanh,
                'Quốc Gia': address.quocGia,
                'Là Mặc Định': address.laMacDinh ? 'Có' : 'Không',
                'Ngày Tạo': address.ngayTao,
                'Ngày Cập Nhật': address.ngayCapNhat
              });
            });
          }
        });

        // Create workbook
        const workbook = XLSX.utils.book_new()

        // Create Customers worksheet and add to workbook
        const customersWorksheet = XLSX.utils.json_to_sheet(exportData)
        XLSX.utils.book_append_sheet(workbook, customersWorksheet, 'Khách Hàng')

        // Create Addresses worksheet and add to workbook
        const addressesWorksheet = XLSX.utils.json_to_sheet(addressesData)
        XLSX.utils.book_append_sheet(workbook, addressesWorksheet, 'Địa Chỉ')

        // Generate file name with current date
        const date = new Date()
        const fileName = `danh_sach_khach_hang_${date.getDate()}_${date.getMonth() + 1}_${date.getFullYear()}.xlsx`

        // Export to file
        XLSX.writeFile(workbook, fileName)

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Xuất danh sách khách hàng thành công',
          life: 3000,
        })
      } catch (error) {
        console.error('Lỗi xuất Excel:', error);
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể xuất danh sách khách hàng',
          life: 3000,
        })
      }
    }

    // Print to PDF function
    const printPDF = () => {
      try {
        const customers = filteredCustomers.value

        // Tạo chuỗi HTML cho nội dung PDF
        let htmlContent = `
          <html>
          <head>
            <style>
              body { font-family: Arial, sans-serif; }
              h1 { text-align: center; margin-bottom: 10px; }
              .date { text-align: right; margin-bottom: 20px; }
              table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
              th, td { padding: 8px; text-align: left; border: 1px solid #ddd; }
              th { background-color: #4285f4; color: white; }
              tr:nth-child(even) { background-color: #f2f2f2; }
              .customer-row { background-color: #e6e6e6; font-weight: bold; }
              .address-row { background-color: #f9f9f9; }
              .address-header { background-color: #9fc5e8; color: black; font-weight: bold; }
              .address-table { margin-left: 20px; width: 95%; }
              .no-address { font-style: italic; color: #666; }
            </style>
          </head>
          <body>
            <h1>Danh Sách Khách Hàng</h1>
            <div class="date">Ngày: ${new Date().toLocaleDateString('vi-VN')}</div>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Mã KH</th>
                  <th>Họ Tên</th>
                  <th>Giới Tính</th>
                  <th>Ngày Sinh</th>
                  <th>Số Điện Thoại</th>
                  <th>Email</th>
                  <th>Trạng Thái</th>
                </tr>
              </thead>
              <tbody>
        `;

        // Thêm dữ liệu khách hàng và địa chỉ
        customers.forEach(customer => {
          // Thêm hàng khách hàng
          htmlContent += `
            <tr class="customer-row">
              <td>${customer.id || ''}</td>
              <td>${customer.maNguoiDung || ''}</td>
              <td>${customer.hoTen || ''}</td>
              <td>${customer.gioiTinh || ''}</td>
              <td>${customer.ngaySinh || ''}</td>
              <td>${customer.soDienThoai || ''}</td>
              <td>${customer.email || ''}</td>
              <td>${customer.trangThai !== false ? 'Hoạt động' : 'Không hoạt động'}</td>
            </tr>
          `;

          // Thêm bảng địa chỉ cho khách hàng
          htmlContent += `
            <tr>
              <td colspan="8">
          `;

          if (customer.diaChis && customer.diaChis.length > 0) {
            htmlContent += `
              <table class="address-table">
                <thead>
                  <tr class="address-header">
                    <th>ID</th>
                    <th>Loại Địa Chỉ</th>
                    <th>Đường</th>
                    <th>Phường/Xã</th>
                    <th>Quận/Huyện</th>
                    <th>Tỉnh/Thành</th>
                    <th>Quốc Gia</th>
                    <th>Mặc Định</th>
                  </tr>
                </thead>
                <tbody>
            `;

            customer.diaChis.forEach(address => {
              htmlContent += `
                <tr class="address-row">
                  <td>${address.id || ''}</td>
                  <td>${address.loaiDiaChi || ''}</td>
                  <td>${address.duong || ''}</td>
                  <td>${address.phuongXa || ''}</td>
                  <td>${address.quanHuyen || ''}</td>
                  <td>${address.tinhThanh || ''}</td>
                  <td>${address.quocGia || ''}</td>
                  <td>${address.laMacDinh ? 'Có' : 'Không'}</td>
                </tr>
              `;
            });

            htmlContent += `
                </tbody>
              </table>
            `;
          } else {
            htmlContent += `<div class="no-address">Không có địa chỉ nào</div>`;
          }

          htmlContent += `
              </td>
            </tr>
          `;
        });

        // Đóng HTML
        htmlContent += `
              </tbody>
            </table>
          </body>
          </html>
        `;

        // Tạo element để render HTML
        const printWindow = window.open('', '_blank');
        printWindow.document.write(htmlContent);
        printWindow.document.close();

        // Chờ cho nội dung được load
        printWindow.onload = function() {
          // In trang
          printWindow.print();
          // Tùy chọn: đóng cửa sổ sau khi in
          // printWindow.close();
        };

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'In danh sách khách hàng thành công',
          life: 3000,
        });
      } catch (error) {
        console.error("In lỗi:", error);
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể in danh sách khách hàng',
          life: 3000,
        });
      }
    }

    return {
      showInactive,
      loading,
      filteredCustomers,
      goToAdd,
      goToEdit,
      confirmDelete,
      confirmRestore,
      exportExcel,
      printPDF,
      customerTable
    }
  },
}
</script>
