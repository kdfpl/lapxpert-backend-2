<script>
import ThongKeService from '@/apis/dashboard'
import { Line } from "vue-chartjs";
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale
} from "chart.js";

ChartJS.register(Title, Tooltip, Legend, LineElement, PointElement, CategoryScale, LinearScale);

export default {
  components: { Line },
  props: {
    isSidebarOpen: Boolean, // Nhận trạng thái sidebar từ component cha
  },
  data() {
    return {
      chartData: { labels: [], datasets: [] },
      chartOptions: {
        responsive: true,
        maintainAspectRatio: false, // Cho phép biểu đồ thay đổi theo container
        plugins: {
          legend: {
            position: "bottom",
          },
        },
        scales: {
          y: {
            beginAtZero: true,
          },
        },
      },
      observer: null, // ResizeObserver để theo dõi kích thước
    };
  },

  mounted() {
    this.fetchData();
    this.initResizeObserver();
  },

  watch: {
    isSidebarOpen() {
      this.$nextTick(() => {
        this.$forceUpdate(); // Cập nhật lại biểu đồ khi sidebar thay đổi
      });
    },
  },

  beforeUnmount() {
    if (this.observer) {
      this.observer.disconnect();
    }
  },

  methods: {
    async fetchData() {
      try {
        // Calculate date range for this week
        const today = new Date()
        const startOfWeek = new Date(today.setDate(today.getDate() - today.getDay()))
        const endOfWeek = new Date(today.setDate(today.getDate() - today.getDay() + 6))

        const tuNgay = startOfWeek.toISOString().split('T')[0]
        const denNgay = endOfWeek.toISOString().split('T')[0]

        const response = await ThongKeService.layDoanhThuTheoNgay(tuNgay, denNgay)

        // Format data for chart
        this.chartData = {
          labels: response.data.labels || [],
          datasets: [{
            label: 'Doanh thu tuần này (VNĐ)',
            data: response.data.data || [],
            borderColor: '#36A2EB',
            backgroundColor: 'rgba(54, 162, 235, 0.2)',
            fill: true,
            tension: 0.4
          }]
        }
      } catch (error) {
        console.error("Lỗi khi lấy dữ liệu từ API:", error);
        this.chartData = { labels: [], datasets: [] }
      }
    },

    initResizeObserver() {
      const container = this.$refs.chartContainer;
      if (container) {
        this.observer = new ResizeObserver(() => {
          this.$forceUpdate(); // Cập nhật lại biểu đồ khi container thay đổi kích thước
        });
        this.observer.observe(container);
      }
    },
  },
};
</script>

<template>
  <div ref="chartContainer"
       class="transition-all duration-300 w-full h-[50vh] p-4"
       :class="isSidebarOpen ? 'md:w-3/4' : 'md:w-full'"> <div style="text-align: center;"><h6 >Bảng theo tuần
      </h6></div>
    <Line :data="chartData" :options="chartOptions" />
  </div>
</template>
