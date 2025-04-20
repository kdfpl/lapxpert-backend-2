package com.lapxpert.backend.thongke.application.controller;

import com.lapxpert.backend.thongke.domain.entity.DoanhThuHangNgay;
import com.lapxpert.backend.thongke.domain.entity.DoanhThuThangDTO;
import com.lapxpert.backend.thongke.domain.entity.HoaDonSanPhamView;
import com.lapxpert.backend.thongke.domain.entity.TongDoanhThuThangDTO;
import com.lapxpert.backend.thongke.domain.service.ThongKeDSService;
import com.lapxpert.backend.thongke.domain.service.ThongKeDTService;
import com.lapxpert.backend.thongke.domain.service.ThongKeHDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RestController
@RequestMapping("/thong-ke")
@CrossOrigin(origins = "*")
public class ThongKeController {
    @Autowired
    ThongKeHDService thongKeHDService;
    @Autowired
    ThongKeDTService thongKeDTService;

    @Autowired
    ThongKeDSService thongKeDSService;


    @GetMapping("/this-month")
    public Map<String, Object> getSalesDataThisMonth() {
        List<DoanhThuHangNgay> salesData = thongKeDTService.DoanhThuTungHangTrongThangNay();

        List<String> labels = generateLabels(30, "Ngày");


        Map<String, Map<String, String>> brandStyles = new HashMap<>();
        brandStyles.put("Apple", Map.of(
                "backgroundColor", "rgba(255, 99, 132, 0.5)",
                "borderColor", "#ff6384"
        ));
        brandStyles.put("Asus", Map.of(
                "backgroundColor", "rgba(1, 255, 0, 0.8)",
                "borderColor", "#36A2EB"
        ));
        brandStyles.put("Acer", Map.of(
                "backgroundColor", "rgba(0, 235, 255, 0.8)",
                "borderColor", "#36A2EB"
        ));
        brandStyles.put("Dell", Map.of(
                "backgroundColor", "rgba(75, 192, 192, 0.5)",
                "borderColor", "#4BC0C0"
        ));
        brandStyles.put("Lenovo", Map.of(
                "backgroundColor", "rgba(153, 102, 255, 0.5)",
                "borderColor", "#9966FF"
        ));
        brandStyles.put("MSI", Map.of(
                "backgroundColor", "rgba(255, 159, 64, 0.5)",
                "borderColor", "#FF9F40"
        ));
        brandStyles.put("HP", Map.of(
                "backgroundColor", "rgba(255, 206, 86, 0.5)",
                "borderColor", "#FFCE56"
        ));
        Map<String, List<Integer>> brandToRevenue = new LinkedHashMap<>();
        for (DoanhThuHangNgay doanhThu : salesData) {
            String brand = doanhThu.getBrand();
            brandToRevenue.computeIfAbsent(brand, k -> new ArrayList<>()).add(doanhThu.getRevenue());
        }

        List<Map<String, Object>> datasets = new ArrayList<>();

        for (Map.Entry<String, List<Integer>> entry : brandToRevenue.entrySet()) {
            String brand = entry.getKey();
            List<Integer> data = entry.getValue();

            Map<String, Object> dataset = new HashMap<>();
            dataset.put("label", brand);
            dataset.put("data", data);
            dataset.put("type", "bar");

            if (brandStyles.containsKey(brand)) {
                dataset.put("backgroundColor", brandStyles.get(brand).get("backgroundColor"));
                dataset.put("borderColor", brandStyles.get(brand).get("borderColor"));
            } else {
                dataset.put("backgroundColor", "rgba(0, 0, 0, 0.5)");
                dataset.put("borderColor", "#000000");
            }

            datasets.add(dataset);
        }

        List<Integer> tong = thongKeDTService.TongDoanhThuTungNgayTrongThangNay();
        Map<String, Object> totalDataset = new HashMap<>();
        totalDataset.put("label", "Tổng doanh thu");
        totalDataset.put("data", tong);
        totalDataset.put("type", "line");
        totalDataset.put("backgroundColor", "rgba(66, 165, 245, 0.2)");
        totalDataset.put("borderColor", "#42A5F5");
        totalDataset.put("pointBackgroundColor", "#42A5F5");
        totalDataset.put("fill", true);
        totalDataset.put("tension", 0.3);
        datasets.add(totalDataset);

        return Map.of(
                "labels", labels,
                "datasets", datasets
        );
    }

    @GetMapping("/this-week")
    public Map<String, Object> getSalesDataThisWeek() {
        List<DoanhThuHangNgay> salesData = thongKeDTService.DoanhThuTungHangTrongTuanNay();

        List<String> labels = List.of("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật");

        Map<String, Map<String, String>> brandStyles = new HashMap<>();
        brandStyles.put("Apple", Map.of(
                "backgroundColor", "rgba(255, 99, 132, 0.5)",
                "borderColor", "#ff6384"
        ));
        brandStyles.put("Asus", Map.of(
                "backgroundColor", "rgba(1, 255, 0, 0.8)",
                "borderColor", "#36A2EB"
        ));
        brandStyles.put("Acer", Map.of(
                "backgroundColor", "rgba(0, 235, 255, 0.8)",
                "borderColor", "#36A2EB"
        ));
        brandStyles.put("Dell", Map.of(
                "backgroundColor", "rgba(75, 192, 192, 0.5)",
                "borderColor", "#4BC0C0"
        ));
        brandStyles.put("Lenovo", Map.of(
                "backgroundColor", "rgba(153, 102, 255, 0.5)",
                "borderColor", "#9966FF"
        ));
        brandStyles.put("MSI", Map.of(
                "backgroundColor", "rgba(255, 159, 64, 0.5)",
                "borderColor", "#FF9F40"
        ));
        brandStyles.put("HP", Map.of(
                "backgroundColor", "rgba(255, 206, 86, 0.5)",
                "borderColor", "#FFCE56"
        ));

        Map<String, List<Integer>> brandToRevenue = new LinkedHashMap<>();
        for (DoanhThuHangNgay doanhThu : salesData) {
            String brand = doanhThu.getBrand();
            brandToRevenue.computeIfAbsent(brand, k -> new ArrayList<>()).add(doanhThu.getRevenue());
        }

        List<Map<String, Object>> datasets = new ArrayList<>();

        for (Map.Entry<String, List<Integer>> entry : brandToRevenue.entrySet()) {
            String brand = entry.getKey();
            List<Integer> data = entry.getValue();

            Map<String, Object> dataset = new HashMap<>();
            dataset.put("label", brand);
            dataset.put("data", data);
            dataset.put("type", "bar");

            if (brandStyles.containsKey(brand)) {
                dataset.put("backgroundColor", brandStyles.get(brand).get("backgroundColor"));
                dataset.put("borderColor", brandStyles.get(brand).get("borderColor"));
            } else {
                dataset.put("backgroundColor", "rgba(0, 255, 8, 0.8)");
                dataset.put("borderColor", "#a2eb36 ");
            }

            datasets.add(dataset);
        }

        List<Integer> tong = thongKeDTService.TongDoanhThuTungNgayTrongTuanNay();
        Map<String, Object> totalDataset = new HashMap<>();
        totalDataset.put("label", "Tổng doanh thu");
        totalDataset.put("data", tong);
        totalDataset.put("type", "line");
        totalDataset.put("backgroundColor", "rgba(66, 165, 245, 0.2)");
        totalDataset.put("borderColor", "#42A5F5");
        totalDataset.put("pointBackgroundColor", "#42A5F5");
        totalDataset.put("fill", true);
        totalDataset.put("tension", 0.3);
        datasets.add(totalDataset);

        return Map.of(
                "labels", labels,
                "datasets", datasets
        );
    }

    @GetMapping("/this-year")
    public Map<String, Object> getSalesDataThisYear() {
        List<DoanhThuThangDTO> salesData = thongKeDTService.DoanhThuTungHangTrongNamNay();
        List<String> labels = List.of("Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12");

        Map<String, Map<String, String>> brandStyles = new HashMap<>();
        brandStyles.put("Apple", Map.of(
                "backgroundColor", "rgba(255, 99, 132, 0.5)",
                "borderColor", "#ff6384"
        ));
        brandStyles.put("Asus", Map.of(
                "backgroundColor", "rgba(1, 255, 0, 0.8)",
                "borderColor", "#36A2EB"
        ));
        brandStyles.put("Acer", Map.of(
                "backgroundColor", "rgba(0, 235, 255, 0.8)",
                "borderColor", "#36A2EB"
        ));
        brandStyles.put("Dell", Map.of(
                "backgroundColor", "rgba(75, 192, 192, 0.5)",
                "borderColor", "#4BC0C0"
        ));
        brandStyles.put("Lenovo", Map.of(
                "backgroundColor", "rgba(153, 102, 255, 0.5)",
                "borderColor", "#9966FF"
        ));
        brandStyles.put("MSI", Map.of(
                "backgroundColor", "rgba(255, 159, 64, 0.5)",
                "borderColor", "#FF9F40"
        ));
        brandStyles.put("HP", Map.of(
                "backgroundColor", "rgba(255, 206, 86, 0.5)",
                "borderColor", "#FFCE56"
        ));

        Map<String, List<Integer>> brandToRevenue = new LinkedHashMap<>();
        for (DoanhThuThangDTO doanhThuT : salesData) {
            String brand = doanhThuT.getBrand();
            brandToRevenue.computeIfAbsent(brand, k -> new ArrayList<>()).add(doanhThuT.getTotalRevenue());
        }

        List<Map<String, Object>> datasets = new ArrayList<>();

        for (Map.Entry<String, List<Integer>> entry : brandToRevenue.entrySet()) {
            String brand = entry.getKey();
            List<Integer> data = entry.getValue();

            Map<String, Object> dataset = new HashMap<>();
            dataset.put("label", brand);
            dataset.put("data", data);
            dataset.put("type", "bar");

            if (brandStyles.containsKey(brand)) {
                dataset.put("backgroundColor", brandStyles.get(brand).get("backgroundColor"));
                dataset.put("borderColor", brandStyles.get(brand).get("borderColor"));
            } else {
                dataset.put("backgroundColor", "rgba(0, 0, 0, 0.5)");
                dataset.put("borderColor", "#000000");
            }

            datasets.add(dataset);
        }

        List<TongDoanhThuThangDTO> tong = thongKeDTService.TongDoanhThuTungThangTrongNamNay();
        List<Integer> TongDT = new ArrayList<>();
        for (int i = 0; i < tong.size(); i++) {
            TongDT.add(tong.get(i).getTotalRevenue());
        }


        Map<String, Object> totalDataset = new HashMap<>();
        totalDataset.put("label", "Tổng doanh thu");
        totalDataset.put("data", TongDT);
        totalDataset.put("type", "line");
        totalDataset.put("backgroundColor", "rgba(66, 165, 245, 0.2)");
        totalDataset.put("borderColor", "#42A5F5");
        totalDataset.put("pointBackgroundColor", "#42A5F5");
        totalDataset.put("fill", true);
        totalDataset.put("tension", 0.3);
        datasets.add(totalDataset);

        return Map.of(
                "labels", labels,
                "datasets", datasets
        );
    }

    @GetMapping("/custom-data")
    public Map<String, Object> getSalesDataCustom(@RequestParam(name = "start_date") LocalDate start_date,@RequestParam(name = "end_date") LocalDate end_date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(start_date.toString(), formatter);
        LocalDate endDate = LocalDate.parse(end_date.toString(), formatter);

        List<DoanhThuHangNgay> salesData = thongKeDTService.DoanhThuCustomTime(start_date, end_date);

        List<String> labels = Stream.iterate(startDate, date -> date.plusDays(1))
                .limit(startDate.datesUntil(endDate.plusDays(1)).count())
                .map(LocalDate::toString) // hoặc .map(d -> d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .collect(Collectors.toList());

        Map<String, Map<String, String>> brandStyles = new HashMap<>();
        brandStyles.put("Apple", Map.of(
                "backgroundColor", "rgba(255, 99, 132, 0.5)",
                "borderColor", "#ff6384"
        ));
        brandStyles.put("Asus", Map.of(
                "backgroundColor", "rgba(1, 255, 0, 0.8)",
                "borderColor", "#36A2EB"
        ));
        brandStyles.put("Acer", Map.of(
                "backgroundColor", "rgba(0, 235, 255, 0.8)",
                "borderColor", "#36A2EB"
        ));
        brandStyles.put("Dell", Map.of(
                "backgroundColor", "rgba(75, 192, 192, 0.5)",
                "borderColor", "#4BC0C0"
        ));
        brandStyles.put("Lenovo", Map.of(
                "backgroundColor", "rgba(153, 102, 255, 0.5)",
                "borderColor", "#9966FF"
        ));
        brandStyles.put("MSI", Map.of(
                "backgroundColor", "rgba(255, 159, 64, 0.5)",
                "borderColor", "#FF9F40"
        ));
        brandStyles.put("HP", Map.of(
                "backgroundColor", "rgba(255, 206, 86, 0.5)",
                "borderColor", "#FFCE56"
        ));

        Map<String, List<Integer>> brandToRevenue = new LinkedHashMap<>();
        for (DoanhThuHangNgay doanhThuT : salesData) {
            String brand = doanhThuT.getBrand();
            brandToRevenue.computeIfAbsent(brand, k -> new ArrayList<>()).add(doanhThuT.getRevenue());
        }

        List<Map<String, Object>> datasets = new ArrayList<>();

        for (Map.Entry<String, List<Integer>> entry : brandToRevenue.entrySet()) {
            String brand = entry.getKey();
            List<Integer> data = entry.getValue();

            Map<String, Object> dataset = new HashMap<>();
            dataset.put("label", brand);
            dataset.put("data", data);
            dataset.put("type", "bar");

            if (brandStyles.containsKey(brand)) {
                dataset.put("backgroundColor", brandStyles.get(brand).get("backgroundColor"));
                dataset.put("borderColor", brandStyles.get(brand).get("borderColor"));
            } else {
                dataset.put("backgroundColor", "rgba(0, 0, 0, 0.5)");
                dataset.put("borderColor", "#000000");
            }

            datasets.add(dataset);
        }

        List<Integer> tong = thongKeDTService.TongDoanhThuTungNgayCustom(start_date,end_date);



        Map<String, Object> totalDataset = new HashMap<>();
        totalDataset.put("label", "Tổng doanh thu");
        totalDataset.put("data", tong);
        totalDataset.put("type", "line");
        totalDataset.put("backgroundColor", "rgba(66, 165, 245, 0.2)");
        totalDataset.put("borderColor", "#42A5F5");
        totalDataset.put("pointBackgroundColor", "#42A5F5");
        totalDataset.put("fill", true);
        totalDataset.put("tension", 0.3);
        datasets.add(totalDataset);

        return Map.of(
                "labels", labels,
                "datasets", datasets
        );
    }

    private List<String> generateLabels(int days, String prefix) {
        return IntStream.rangeClosed(1, days)
                .mapToObj(i -> prefix + " " + i)
                .toList();
    }

    @GetMapping("/top-month")
    public Map<String, Object> getTopThangData() {
        List<String> label = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (int i=0; i < thongKeDSService.getTopDoanhSoThang().size(); i++) {
            label.add(thongKeDSService.getTopDoanhSoThang().get(i).getBrand());
            data.add(thongKeDSService.getTopDoanhSoThang().get(i).getTotalSales());
        }
        return Map.of(
                "labels", label,
                "data",data
        );
    }

    @GetMapping("/top-week")
    public Map<String, Object> getTopTuanData() {
        List<String> label = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (int i=0; i < thongKeDSService.getTopDoanhSoTuan().size(); i++) {
            label.add(thongKeDSService.getTopDoanhSoTuan().get(i).getBrand());
            data.add(thongKeDSService.getTopDoanhSoTuan().get(i).getTotalSales());
        }
        return Map.of(
                "labels", label,
                "data",data
        );
    }

    @GetMapping("/top-day")
    public Map<String, Object> getTopNgayData() {
        List<String> label = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (int i=0; i < thongKeDSService.getTopDoanhSoNgay().size(); i++) {
            label.add(thongKeDSService.getTopDoanhSoNgay().get(i).getBrand());
            data.add(thongKeDSService.getTopDoanhSoNgay().get(i).getTotalSales());
        }
        return Map.of(
                "labels", label,
                "data",data
        );
    }

    @GetMapping("/top-custom")
    public Map<String, Object> getTopCustomData(@RequestParam(name = "start_dateTop") LocalDate start_date, @RequestParam   (name = "end_dateTop") LocalDate end_date) {
        List<String> label = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (int i=0; i < thongKeDSService.getTopDoanhSoCustom(start_date, end_date).size(); i++) {
            label.add(thongKeDSService.getTopDoanhSoCustom(start_date, end_date).get(i).getBrand());
            data.add(thongKeDSService.getTopDoanhSoCustom(start_date, end_date).get(i).getTotalSales());
        }
        return Map.of(
                "labels", label,
                "data",data
        );
    }



    @GetMapping("/hoa-don")
    public List<HoaDonSanPhamView> getHoaDonsCoSanPhamByTrangThai(@RequestParam(value = "trangThai", required = false) String trangThai) {
        return thongKeHDService.getHoaDonsCoSanPhamByTrangThai(trangThai);
    }
    @GetMapping("/doanh-so")
        public Map<String, Object> DoanhSo(){
        return Map.of("DoanhSo",5555);

        }

}
