<template>
  <div class="staff-list">
    <Toolbar class="mb-4">
      <template #start>
        <Button
          label="Thêm nhân viên"
          icon="pi pi-plus"
          class="mr-2"
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
      :users="filteredStaff"
      :loading="loading"
      :showStaffFields="true"
      title="Danh sách nhân viên"
      @edit="goToEdit"
      @delete="confirmDelete"
      @restore="confirmRestore"
      ref="staffTable"
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
import { useStaffStore } from '@/stores/staffstore'
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
    const staffStore = useStaffStore()
    const showInactive = ref(false)
    const loading = ref(false)
    const staffTable = ref(null)

    // Load staff on mount
    onMounted(async () => {
      loading.value = true
      try {
        await staffStore.fetchStaff()
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể tải danh sách nhân viên',
          life: 3000,
        })
      } finally {
        loading.value = false
      }
    })

    // Computed properties
    const filteredStaff = computed(() => {
      return showInactive.value
        ? [...staffStore.activeStaff, ...staffStore.inactiveStaff]
        : staffStore.activeStaff
    })

    // Navigation methods
    const goToAdd = () => {
      router.push({ name: 'StaffAdd' })
    }

    const goToEdit = (staff) => {
      router.push({ name: 'StaffEdit', params: { id: staff.id } })
    }

    // Delete confirmation
    const confirmDelete = (staff) => {
      confirm.require({
        message: `Bạn có chắc chắn muốn vô hiệu hóa nhân viên ${staff.hoTen}?`,
        header: 'Xác nhận',
        icon: 'pi pi-exclamation-triangle',
        accept: () => deleteStaff(staff.id),
      })
    }

    const deleteStaff = async (id) => {
      try {
        await staffStore.deleteStaff(id)
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Nhân viên đã được vô hiệu hóa',
          life: 3000,
        })
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể vô hiệu hóa nhân viên',
          life: 3000,
        })
      }
    }

    // Restore confirmation
    const confirmRestore = (staff) => {
      confirm.require({
        message: `Bạn có chắc chắn muốn khôi phục nhân viên ${staff.hoTen}?`,
        header: 'Xác nhận',
        icon: 'pi pi-exclamation-triangle',
        accept: () => restoreStaff(staff.id),
      })
    }

    const restoreStaff = async (id) => {
      try {
        await staffStore.restoreStaff(id)
        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Nhân viên đã được khôi phục',
          life: 3000,
        })
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể khôi phục nhân viên',
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
        const staff = filteredStaff.value

        // Transform data for Staff sheet
        const exportData = staff.map((member) => ({
          'ID': member.id,
          'Mã Nhân Viên': member.maNguoiDung,
          'Họ Tên': member.hoTen,
          'Giới Tính': member.gioiTinh,
          'Ngày Sinh': member.ngaySinh,
          'Số Điện Thoại': member.soDienThoai,
          'Email': member.email,
          'CCCD': member.cccd,
          'Vai Trò': member.vaiTro,
          'Địa Chỉ Mặc Định': formatDefaultAddress(member.diaChis),
          'Trạng Thái': member.trangThai ? 'Hoạt động' : 'Không hoạt động'
        }))

        // Transform data for Addresses sheet
        let addressesData = [];
        staff.forEach(member => {
          if (member.diaChis && member.diaChis.length > 0) {
            member.diaChis.forEach(address => {
              addressesData.push({
                'ID Nhân Viên': member.id,
                'Họ Tên Nhân Viên': member.hoTen,
                'ID Địa Chỉ': address.id,
                'Loại Địa Chỉ': address.loaiDiaChi || 'Chưa xác định',
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

        // Create Staff worksheet and add to workbook
        const staffWorksheet = XLSX.utils.json_to_sheet(exportData)
        XLSX.utils.book_append_sheet(workbook, staffWorksheet, 'Nhân Viên')

        // Create Addresses worksheet and add to workbook
        const addressesWorksheet = XLSX.utils.json_to_sheet(addressesData)
        XLSX.utils.book_append_sheet(workbook, addressesWorksheet, 'Địa Chỉ')

        // Generate file name with current date
        const date = new Date()
        const fileName = `danh_sach_nhan_vien_${date.getDate()}_${date.getMonth() + 1}_${date.getFullYear()}.xlsx`

        // Export to file
        XLSX.writeFile(workbook, fileName)

        toast.add({
          severity: 'success',
          summary: 'Thành công',
          detail: 'Xuất danh sách nhân viên thành công',
          life: 3000,
        })
      } catch (error) {
        console.error('Lỗi xuất Excel:', error);
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể xuất danh sách nhân viên',
          life: 3000,
        })
      }
    }

    // Print to PDF function
    const printPDF = () => {
      try {
        const staff = filteredStaff.value

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
              .staff-row { background-color: #e6e6e6; font-weight: bold; }
              .address-row { background-color: #f9f9f9; }
              .address-header { background-color: #9fc5e8; color: black; font-weight: bold; }
              .address-table { margin-left: 20px; width: 95%; }
              .no-address { font-style: italic; color: #666; }
            </style>
          </head>
          <body>
            <h1>Danh Sách Nhân Viên</h1>
            <div class="date">Ngày: ${new Date().toLocaleDateString('vi-VN')}</div>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Mã NV</th>
                  <th>Họ Tên</th>
                  <th>Giới Tính</th>
                  <th>Ngày Sinh</th>
                  <th>Số Điện Thoại</th>
                  <th>Email</th>
                  <th>CCCD</th>
                  <th>Vai Trò</th>
                  <th>Trạng Thái</th>
                </tr>
              </thead>
              <tbody>
        `;

        // Thêm dữ liệu nhân viên và địa chỉ
        staff.forEach(member => {
          // Thêm hàng nhân viên
          htmlContent += `
            <tr class="staff-row">
              <td>${member.id || ''}</td>
              <td>${member.maNguoiDung || ''}</td>
              <td>${member.hoTen || ''}</td>
              <td>${member.gioiTinh || ''}</td>
              <td>${member.ngaySinh || ''}</td>
              <td>${member.soDienThoai || ''}</td>
              <td>${member.email || ''}</td>
              <td>${member.cccd || ''}</td>
              <td>${member.vaiTro || ''}</td>
              <td>${member.trangThai ? 'Hoạt động' : 'Không hoạt động'}</td>
            </tr>
          `;

          // Thêm bảng địa chỉ cho nhân viên
          htmlContent += `
            <tr>
              <td colspan="10">
          `;

          if (member.diaChis && member.diaChis.length > 0) {
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

            member.diaChis.forEach(address => {
              htmlContent += `
                <tr class="address-row">
                  <td>${address.id || ''}</td>
                  <td>${address.loaiDiaChi || 'Chưa xác định'}</td>
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
          detail: 'In danh sách nhân viên thành công',
          life: 3000,
        });
      } catch (error) {
        console.error("In lỗi:", error);
        toast.add({
          severity: 'error',
          summary: 'Lỗi',
          detail: error.message || 'Không thể in danh sách nhân viên',
          life: 3000,
        });
      }
    }

    return {
      showInactive,
      loading,
      filteredStaff,
      goToAdd,
      goToEdit,
      confirmDelete,
      confirmRestore,
      exportExcel,
      printPDF,
      staffTable
    }
  },
}
</script>
