package com.lapxpert.backend.thongke.application.controller;

import com.lapxpert.backend.hoadon.service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/thong-ke")
@CrossOrigin(origins = "*")
public class ThongKeController {
    @Autowired
    HoaDonService hoaDonService;




    @GetMapping("/month")
    public Map<String, Object> getSalesDataMonth() {
        List<String> labels = generateLabels(30, "Ngày");
        List<Map<String, Object>> datasets = generateDatasets("month");

        return Map.of("labels", labels, "datasets", datasets);
    }

    @GetMapping("/week")
    public Map<String, Object> getSalesDataWeek() {
        List<String> labels = List.of("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật");
        List<Map<String, Object>> datasets = generateDatasets("week");

        return Map.of("labels", labels, "datasets", datasets);
    }

    @GetMapping("/year")
    public Map<String, Object> getSalesDataYear() {
        List<String> labels = List.of("Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12");
        List<Map<String, Object>> datasets = generateDatasets("year");

        return Map.of("labels", labels, "datasets", datasets);
    }

    //tự gen ra dữ liệu mẫu
    private List<Map<String, Object>> generateDatasets(String period) {
        List<String> brands = List.of("Apple", "ASUS", "Dell", "Lenovo", "MSI", "HP");
        List<String> colors = List.of(
                "rgba(255, 99, 132, 0.5)", "#ff6384",
                "rgba(54, 162, 235, 0.5)", "#36A2EB",
                "rgba(75, 192, 192, 0.5)", "#4BC0C0",
                "rgba(153, 102, 255, 0.5)", "#9966FF",
                "rgba(255, 159, 64, 0.5)", "#FF9F40",
                "rgba(255, 206, 86, 0.5)", "#FFCE56"
        );

        List<List<Integer>> salesData = switch (period) {
            case "week" -> List.of(
                    List.of(1262, 2912, 2165, 2335, 2943, 1765, 1734),
                    List.of(1500, 1900, 1300, 1700, 2100, 1800, 2000),
                    List.of(1400, 1600, 1800, 2200, 2500, 2000, 2300),
                    List.of(1200, 1400, 1600, 1900, 2100, 1800, 2000),
                    List.of(1000, 1300, 1500, 1700, 2000, 1800, 1900),
                    List.of(900, 1100, 1300, 1600, 1900, 1700, 1800)
            );
            case "month" -> List.of(
                    generateRandomData(30, 1000, 3000),
                    generateRandomData(30, 1200, 3200),
                    generateRandomData(30, 1400, 3500),
                    generateRandomData(30, 1600, 3700),
                    generateRandomData(30, 1800, 4000),
                    generateRandomData(30, 2000, 4200)
            );
            case "year" -> List.of(
                    generateRandomData(12, 1000, 5000),
                    generateRandomData(12, 1500, 6000),
                    generateRandomData(12, 2000, 7000),
                    generateRandomData(12, 2500, 8000),
                    generateRandomData(12, 3000, 9000),
                    generateRandomData(12, 3500, 10000)
            );
            default -> throw new IllegalArgumentException("Lỗi period: " + period);
        };


        List<Map<String, Object>> datasets = IntStream.range(0, brands.size())
                .mapToObj(i -> createDataset("bar", brands.get(i), colors.get(i * 2), colors.get(i * 2 + 1), salesData.get(i)))
                .collect(Collectors.toList());

        //Đây là tính tổng doanh thu :Đ
        datasets.add(createLineDataset("Tổng doanh thu", "#42A5F5", "rgba(66, 165, 245, 0.2)",
                salesData.stream().reduce((a, b) -> IntStream.range(0, a.size()).map(i -> a.get(i) + b.get(i)).boxed().toList()).orElse(List.of())));

        return datasets;
    }

    private List<Integer> generateRandomData(int size, int min, int max) {
        Random random = new Random();
        return IntStream.range(0, size)
                .map(i -> random.nextInt(max - min + 1) + min)
                .boxed()
                .toList();
    }

    private List<String> generateLabels(int days, String prefix) {
        return IntStream.rangeClosed(1, days)
                .mapToObj(i -> prefix + " " + i)
                .toList();
    }

    private Map<String, Object> createLineDataset(String label, String borderColor, String backgroundColor, List<Integer> data) {
        return Map.of(
                "type", "line",
                "label", label,
                "borderColor", borderColor,
                "pointBackgroundColor", borderColor,
                "backgroundColor", backgroundColor,
                "data", data,
                "fill", true,
                "tension", 0.3
        );
    }

    private Map<String, Object> createDataset(String type, String label, String backgroundColor, String borderColor, List<Integer> data) {
        return Map.of(
                "type", type,
                "label", label,
                "backgroundColor", backgroundColor,
                "borderColor", borderColor,
                "data", data
        );
    }
    @GetMapping("/top-month")
    public Map<String, Object> getTopMonthData() {
        return Map.of(
                "labels", List.of("Dell", "HP", "Apple", "Lenovo", "Asus", "Acer","MSI"),
                "data", List.of(100, 119, 152,359, 242,234,143)
        );
    }
    @GetMapping("/top-day")
    public Map<String, Object> getTopDayData() {
        return Map.of(
                "labels", List.of("Dell", "HP", "Apple", "Lenovo", "Asus", "Acer","MSI"),
                "data", List.of(1, 2, 4,1, 2,1,1)
        );
    }
    @GetMapping("/top-week")
    public Map<String, Object> getTopWeekData() {
        return Map.of(
                "labels", List.of("Dell", "HP", "Apple", "Lenovo", "Asus", "Acer","MSI"),
                "data", List.of(50, 67, 78,154, 123,112,67)
        );
    }
}
