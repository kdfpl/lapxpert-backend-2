<script>
import axios from "axios";
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, LineElement, PointElement, CategoryScale, LinearScale)

export default {
  components: { Line },
  props: {
    isSidebarOpen: Boolean, // Nhận trạng thái sidebar từ component cha
  },
  data() {
    return {
      chartData: {
        labels: ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6', 'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'],
        datasets: [
          {
            label: 'Số lượng khách hàng',
            data: [50, 80, 420, 420, 260, 340, 260, 500, 260, 720, 850, 1000],
            borderColor: '#36A2EB',
            backgroundColor: 'rgba(54, 162, 235, 0.2)',
            fill: true,
            tension: 0
          }
        ]
      },
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
        const response = await axios.get("http://localhost:8080/api/khach-hang/");
        this.chartData = response.data;
      } catch (error) {
        console.error("Lỗi khi lấy dữ liệu từ API:", error);
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
  
}
</script>

<template>
 <div ref="chartContainer" 
       class="transition-all duration-300 w-full h-[50vh] p-4"
       :class="isSidebarOpen ? 'md:w-3/4' : 'md:w-full'">
    <Line :data="chartData"  :options="chartOptions" />
  </div>
</template>