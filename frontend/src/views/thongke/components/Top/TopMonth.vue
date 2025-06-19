<script>
import axios from "axios";
import { Doughnut } from "vue-chartjs";
import { Chart as ChartJS, Title, Tooltip, Legend, ArcElement, } from "chart.js";

ChartJS.register(Title, Tooltip, Legend, ArcElement);

export default {
  components: { Doughnut },
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
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: "bottom",
          },
        },
        cutout: "55%",
      },
      windowWidth: window.innerWidth,
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
      try {
        const response = await axios.get("http://localhost:8080/thong-ke/top-month");
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
    <div style="text-align: center;"><h6 >Theo Tháng
    </h6></div>
    <Doughnut :data="chartData" :options="chartOptions" />
  </div>
</template>
