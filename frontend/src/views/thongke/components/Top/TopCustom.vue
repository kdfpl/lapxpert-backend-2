<script>
import axios from "axios";
import { Doughnut } from "vue-chartjs"; // Chuyển từ Pie sang Doughnut
import { Chart as ChartJS, Title, Tooltip, Legend, ArcElement, } from "chart.js";

ChartJS.register(Title, Tooltip, Legend, ArcElement);

export default {
  components: { Doughnut }, // Sử dụng Doughnut thay vì Pie
  props: {
    start_dateTop: { type: String, required: true }, // Đảm bảo nhận giá trị dưới dạng String
    end_dateTop: { type: String, required: true }
  },
  data() {
    return {
      chartData: {
        labels: [],
        datasets: [
          {
            label: "Số Laptop (%)",
            data: [],
            backgroundColor: [
              "#FF6384", // Dell
              "#36A2EB", // HP
              "#FFCE56", // Apple
              "#4BC0C0", // Lenovo
              "#9966FF", // Asus
            ],
            hoverOffset: 10,
          },
        ],
      },
      chartOptions: {
        responsive: true,
        maintainAspectRatio: false, // Quan trọng để biểu đồ co giãn đúng cách
        plugins: {
          legend: {
            position: "bottom",
          },
        },
        cutout: "55%", // Tạo hiệu ứng Doughnut (lỗ giữa)
      },
      windowWidth: window.innerWidth, // Theo dõi kích thước cửa sổ
    };
  },

  mounted() {
    this.fetchChartData();
    window.addEventListener("resize", this.handleResize);
  },

  beforeUnmount() {
    window.removeEventListener("resize", this.handleResize);
  },

  methods: {
    async fetchChartData() {
      this.isLoading = true;
      if (!this.start_dateTop || !this.end_dateTop) {
      console.error("Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc");
      return; // Dừng phương thức nếu dữ liệu không hợp lệ
    }
    if(this.start_dateTop  > this.end_dateTop){
console.error("Thời gian kết thúc không được quá ngày bắt đầu");
return
    }
      try {
        const response = await axios.get("http://localhost:8080/thong-ke/top-custom", {
          params: {
            start_dateTop: this.start_dateTop,  // Truyền trực tiếp giá trị start_date và end_date
            end_dateTop: this.end_dateTop,
      }
        });

        const { labels, data } = response.data;
        this.updateChartData(labels, data);
      } catch (error) {
        console.error("Lỗi khi lấy dữ liệu:", error);
      }
    },

    updateChartData(labels, data) {
      this.chartData = {
        labels: labels,
        datasets: [
          {
            label: "Số Laptop (%)",
            data: data,
            backgroundColor: [
              "#FF6384",
              "#36A2EB",
              "#FFCE56",
              "#4BC0C0",
              "#9966FF",
              "#CD661D",
              "#CD0000"
            ],
            hoverOffset: 10,
          },
        ],
      };
    },

    handleResize() {
      this.windowWidth = window.innerWidth;
    }
  },
};
</script>

<template>
  <div class="transition-all duration-300 w-full h-[400px]">
    <div style="text-align: center;"><h6 >Theo thời gian đã chọn
    </h6></div>
    <Doughnut :data="chartData" :options="chartOptions" />
  </div>
</template>
