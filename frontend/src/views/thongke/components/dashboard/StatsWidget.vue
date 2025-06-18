<script >
import ThongKeService from '@/apis/dashboard'
import{ ref, onMounted }from 'vue'
const DoanhSo = ref(null)

onMounted(() => {
  ThongKeService.getDoanhThu()
    .then((response) => {
      DoanhSo.value = response.data.DoanhSo
    })
    .catch((error) => {
      console.error('Lỗi khi tải dữ liệu doanh số:', error)
    })
})
</script>

<template>
    <div class="col-span-12 lg:col-span-6 xl:col-span-3 basis-1/4 mr-3 ">
        <div class="card mb-0">
            <div class="flex justify-between mb-4">
                <div>
                    <span class="block text-muted-color font-medium mb-4">Đơn hàng</span>
                    <div class="text-surface-900 dark:text-surface-0 font-medium text-xl">352</div>
                </div>
                <div class="flex items-center justify-center bg-blue-100 dark:bg-blue-400/10 rounded-border" style="width: 2.5rem; height: 2.5rem">
                    <i class="pi pi-shopping-cart text-blue-500 !text-xl"></i>
                </div>
            </div>
            <span class="text-primary font-medium">24 đơn hàng </span>
            <span class="text-muted-color">mới</span>
        </div>
    </div>
    <div class="col-span-12 lg:col-span-6 xl:col-span-3 basis-1/4 mr-3">
        <div class="card mb-0">
            <div class="flex justify-between mb-4">
                <div>
                    <span class="block text-muted-color font-medium mb-4">Doanh thu</span>
                    <div class="text-surface-900 dark:text-surface-0 font-medium text-xl">231,000,00.00đ</div>
                </div>
                <div class="flex items-center justify-center bg-orange-100 dark:bg-orange-400/10 rounded-border" style="width: 2.5rem; height: 2.5rem">
                    <i class="pi pi-dollar text-orange-500 !text-xl"></i>
                </div>
            </div>
            <span class="text-primary font-medium">%52+ </span>
            <span class="text-muted-color">so với tháng trước</span>
        </div>
    </div>
    <div class="col-span-12 lg:col-span-6 xl:col-span-3 basis-1/4 mr-3">
        <div class="card mb-0">
            <div class="flex justify-between mb-4">
                <div>
                    <span class="block text-muted-color font-medium mb-4">Doanh số</span>
                    <div class="text-surface-900 dark:text-surface-0 font-medium text-xl">
                      <!-- {{ DoanhSo > 0 ? DoanhSo : "Loading..." }} -->
                        5231
                    </div>
                </div>
                <div class="flex items-center justify-center bg-cyan-100 dark:bg-cyan-400/10 rounded-border" style="width: 2.5rem; height: 2.5rem">
                    <i class="pi pi-users text-cyan-500 !text-xl"></i>
                </div>
            </div>
            <span class="text-primary font-medium">520+ </span>
            <span class="text-muted-color"> tuần vừa qua</span>
        </div>
    </div>
    <div class="col-span-12 lg:col-span-6 xl:col-span-3 basis-1/4">
        <div class="card mb-0">
            <div class="flex justify-between mb-4">
                <div>
                    <span class="block text-muted-color font-medium mb-4">Bình luận</span>
                    <div class="text-surface-900 dark:text-surface-0 font-medium text-xl">152 Chưa đọc</div>
                </div>
                <div class="flex items-center justify-center bg-purple-100 dark:bg-purple-400/10 rounded-border" style="width: 2.5rem; height: 2.5rem">
                    <i class="pi pi-comment text-purple-500 !text-xl"></i>
                </div>
            </div>
            <span class="text-primary font-medium">85 </span>
            <span class="text-muted-color">đã trả lời</span>
        </div>
    </div>
</template>
