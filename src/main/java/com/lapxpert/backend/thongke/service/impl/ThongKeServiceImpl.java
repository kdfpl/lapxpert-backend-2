package com.lapxpert.backend.thongke.service.impl;

import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import com.lapxpert.backend.thongke.dto.*;
import com.lapxpert.backend.thongke.service.ThongKeService;
import com.lapxpert.backend.hoadon.entity.HoaDon;
import com.lapxpert.backend.hoadon.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.enums.LoaiHoaDon;
import com.lapxpert.backend.hoadon.repository.HoaDonRepository;
import com.lapxpert.backend.hoadon.repository.HoaDonChiTietRepository;
import com.lapxpert.backend.sanpham.repository.SanPhamRepository;

import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import com.lapxpert.backend.sanpham.repository.SerialNumberRepository;
import com.lapxpert.backend.nguoidung.repository.NguoiDungRepository;
import com.lapxpert.backend.nguoidung.entity.VaiTro;
import com.lapxpert.backend.nguoidung.entity.TrangThaiNguoiDung;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ThongKe (Statistics) Service Implementation
 * Implements business logic for statistics calculation and aggregation
 * Following Vietnamese naming conventions and clean architecture principles
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ThongKeServiceImpl implements ThongKeService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamRepository sanPhamRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final NguoiDungRepository nguoiDungRepository;

    // ==================== DOANH THU (REVENUE) STATISTICS ====================

    @Override
    public DoanhThuTheoNgayDto layDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay) {
        log.debug("Getting daily revenue statistics from {} to {}", tuNgay, denNgay);
        
        // Set default dates if not provided
        if (denNgay == null) {
            denNgay = LocalDate.now();
        }
        if (tuNgay == null) {
            tuNgay = denNgay.minusDays(30);
        }
        
        // Get completed orders in the date range
        List<HoaDon> orders = hoaDonRepository.findByNgayTaoBetweenAndTrangThaiDonHang(
            tuNgay.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            denNgay.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC),
            TrangThaiDonHang.HOAN_THANH
        );
        
        // Group by date and calculate daily revenue
        Map<LocalDate, BigDecimal> dailyRevenue = orders.stream()
            .collect(Collectors.groupingBy(
                order -> order.getNgayTao().atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                Collectors.reducing(BigDecimal.ZERO, HoaDon::getTongThanhToan, BigDecimal::add)
            ));
        
        // Generate labels and data for all days in range
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();
        
        LocalDate currentDate = tuNgay;
        while (!currentDate.isAfter(denNgay)) {
            labels.add(currentDate.toString());
            data.add(dailyRevenue.getOrDefault(currentDate, BigDecimal.ZERO));
            currentDate = currentDate.plusDays(1);
        }
        
        // Calculate summary statistics
        BigDecimal tongDoanhThu = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        int soNgay = data.size();
        BigDecimal doanhThuTrungBinhNgay = soNgay > 0 ? 
            tongDoanhThu.divide(BigDecimal.valueOf(soNgay), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        // Find highest and lowest revenue days
        LocalDate ngayDoanhThuCaoNhat = null;
        BigDecimal doanhThuCaoNhat = BigDecimal.ZERO;
        LocalDate ngayDoanhThuThapNhat = null;
        BigDecimal doanhThuThapNhat = null;
        
        for (int i = 0; i < labels.size(); i++) {
            BigDecimal revenue = data.get(i);
            LocalDate date = LocalDate.parse(labels.get(i));
            
            if (revenue.compareTo(doanhThuCaoNhat) > 0) {
                doanhThuCaoNhat = revenue;
                ngayDoanhThuCaoNhat = date;
            }
            
            if (doanhThuThapNhat == null || revenue.compareTo(doanhThuThapNhat) < 0) {
                doanhThuThapNhat = revenue;
                ngayDoanhThuThapNhat = date;
            }
        }
        
        // Calculate growth percentage (compare with previous period)
        LocalDate previousPeriodStart = tuNgay.minusDays(ChronoUnit.DAYS.between(tuNgay, denNgay) + 1);
        LocalDate previousPeriodEnd = tuNgay.minusDays(1);
        
        List<HoaDon> previousOrders = hoaDonRepository.findByNgayTaoBetweenAndTrangThaiDonHang(
            previousPeriodStart.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            previousPeriodEnd.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC),
            TrangThaiDonHang.HOAN_THANH
        );
        
        BigDecimal previousRevenue = previousOrders.stream()
            .map(HoaDon::getTongThanhToan)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Double tyLeTangTruong = calculateGrowthPercentage(tongDoanhThu, previousRevenue);
        
        return DoanhThuTheoNgayDto.builder()
            .labels(labels)
            .data(data)
            .tongDoanhThu(tongDoanhThu)
            .doanhThuTrungBinhNgay(doanhThuTrungBinhNgay)
            .tuNgay(tuNgay)
            .denNgay(denNgay)
            .soNgay(soNgay)
            .tyLeTangTruong(tyLeTangTruong)
            .ngayDoanhThuCaoNhat(ngayDoanhThuCaoNhat)
            .doanhThuCaoNhat(doanhThuCaoNhat)
            .ngayDoanhThuThapNhat(ngayDoanhThuThapNhat)
            .doanhThuThapNhat(doanhThuThapNhat)
            .build();
    }

    @Override
    public DoanhThuTheoThangDto layDoanhThuTheoThang(Integer nam) {
        log.debug("Getting monthly revenue statistics for year {}", nam);
        
        if (nam == null) {
            nam = LocalDate.now().getYear();
        }
        
        // Get completed orders for the year
        LocalDate startOfYear = LocalDate.of(nam, 1, 1);
        LocalDate endOfYear = LocalDate.of(nam, 12, 31);
        
        List<HoaDon> orders = hoaDonRepository.findByNgayTaoBetweenAndTrangThaiDonHang(
            startOfYear.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            endOfYear.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC),
            TrangThaiDonHang.HOAN_THANH
        );
        
        // Group by month and calculate monthly revenue
        Map<Integer, BigDecimal> monthlyRevenue = orders.stream()
            .collect(Collectors.groupingBy(
                order -> order.getNgayTao().atZone(java.time.ZoneOffset.UTC).toLocalDate().getMonthValue(),
                Collectors.reducing(BigDecimal.ZERO, HoaDon::getTongThanhToan, BigDecimal::add)
            ));
        
        // Generate labels and data for all months
        List<String> labels = Arrays.asList(
            "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        );
        
        List<BigDecimal> data = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            data.add(monthlyRevenue.getOrDefault(month, BigDecimal.ZERO));
        }
        
        // Calculate summary statistics
        BigDecimal tongDoanhThuNam = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal doanhThuTrungBinhThang = tongDoanhThuNam.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        
        // Find best and worst months
        Integer thangTotNhat = 1;
        BigDecimal doanhThuThangTotNhat = data.get(0);
        Integer thangXauNhat = 1;
        BigDecimal doanhThuThangXauNhat = data.get(0);
        
        for (int i = 1; i < data.size(); i++) {
            if (data.get(i).compareTo(doanhThuThangTotNhat) > 0) {
                doanhThuThangTotNhat = data.get(i);
                thangTotNhat = i + 1;
            }
            if (data.get(i).compareTo(doanhThuThangXauNhat) < 0) {
                doanhThuThangXauNhat = data.get(i);
                thangXauNhat = i + 1;
            }
        }
        
        // Calculate year-over-year growth
        List<HoaDon> previousYearOrders = hoaDonRepository.findByNgayTaoBetweenAndTrangThaiDonHang(
            LocalDate.of(nam - 1, 1, 1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            LocalDate.of(nam - 1, 12, 31).atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC),
            TrangThaiDonHang.HOAN_THANH
        );
        
        BigDecimal doanhThuNamTruoc = previousYearOrders.stream()
            .map(HoaDon::getTongThanhToan)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Double tyLeTangTruongNam = calculateGrowthPercentage(tongDoanhThuNam, doanhThuNamTruoc);
        
        // Generate quarter breakdown with growth calculation
        List<DoanhThuTheoThangDto.QuarterRevenueDto> doanhThuTheoQuy = new ArrayList<>();

        // Calculate previous year quarterly revenue for growth comparison
        Map<Integer, BigDecimal> previousYearMonthlyRevenue = previousYearOrders.stream()
            .collect(Collectors.groupingBy(
                order -> order.getNgayTao().atZone(java.time.ZoneOffset.UTC).toLocalDate().getMonthValue(),
                Collectors.reducing(BigDecimal.ZERO, HoaDon::getTongThanhToan, BigDecimal::add)
            ));

        for (int quarter = 1; quarter <= 4; quarter++) {
            int startMonth = (quarter - 1) * 3;

            // Calculate current year quarter revenue
            BigDecimal quarterRevenue = data.subList(startMonth, startMonth + 3).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate previous year quarter revenue for comparison
            BigDecimal previousYearQuarterRevenue = BigDecimal.ZERO;
            for (int month = startMonth + 1; month <= startMonth + 3; month++) {
                previousYearQuarterRevenue = previousYearQuarterRevenue.add(
                    previousYearMonthlyRevenue.getOrDefault(month, BigDecimal.ZERO)
                );
            }

            // Calculate quarter-over-quarter growth
            Double quarterGrowthPercentage = calculateGrowthPercentage(quarterRevenue, previousYearQuarterRevenue);

            doanhThuTheoQuy.add(DoanhThuTheoThangDto.QuarterRevenueDto.builder()
                .quy(quarter)
                .doanhThu(quarterRevenue)
                .tyLeTangTruong(quarterGrowthPercentage)
                .build());
        }
        
        return DoanhThuTheoThangDto.builder()
            .labels(labels)
            .data(data)
            .nam(nam)
            .tongDoanhThuNam(tongDoanhThuNam)
            .doanhThuTrungBinhThang(doanhThuTrungBinhThang)
            .thangTotNhat(thangTotNhat)
            .doanhThuThangTotNhat(doanhThuThangTotNhat)
            .thangXauNhat(thangXauNhat)
            .doanhThuThangXauNhat(doanhThuThangXauNhat)
            .tyLeTangTruongNam(tyLeTangTruongNam)
            .doanhThuNamTruoc(doanhThuNamTruoc)
            .doanhThuTheoQuy(doanhThuTheoQuy)
            .build();
    }

    @Override
    public DoanhThuTongQuanDto layDoanhThuTongQuan() {
        log.debug("Getting revenue overview");
        
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfLastWeek = startOfWeek.minusDays(7);
        LocalDate endOfLastWeek = startOfWeek.minusDays(1);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);
        LocalDate startOfYear = today.withDayOfYear(1);
        LocalDate startOfLastYear = startOfYear.minusYears(1);
        LocalDate endOfLastYear = startOfYear.minusDays(1);
        
        // Calculate revenue for different periods
        BigDecimal doanhThuHomNay = calculateRevenueForPeriod(today, today);
        BigDecimal doanhThuHomQua = calculateRevenueForPeriod(yesterday, yesterday);
        BigDecimal doanhThuTuanNay = calculateRevenueForPeriod(startOfWeek, today);
        BigDecimal doanhThuTuanTruoc = calculateRevenueForPeriod(startOfLastWeek, endOfLastWeek);
        BigDecimal doanhThuThangNay = calculateRevenueForPeriod(startOfMonth, today);
        BigDecimal doanhThuThangTruoc = calculateRevenueForPeriod(startOfLastMonth, endOfLastMonth);
        BigDecimal doanhThuNamNay = calculateRevenueForPeriod(startOfYear, today);
        BigDecimal doanhThuNamTruoc = calculateRevenueForPeriod(startOfLastYear, endOfLastYear);
        
        // Calculate growth percentages
        Double tyLeTangTruongNgay = calculateGrowthPercentage(doanhThuHomNay, doanhThuHomQua);
        Double tyLeTangTruongTuan = calculateGrowthPercentage(doanhThuTuanNay, doanhThuTuanTruoc);
        Double tyLeTangTruongThang = calculateGrowthPercentage(doanhThuThangNay, doanhThuThangTruoc);
        Double tyLeTangTruongNam = calculateGrowthPercentage(doanhThuNamNay, doanhThuNamTruoc);
        
        // Calculate average daily revenue this month
        long daysInMonth = ChronoUnit.DAYS.between(startOfMonth, today) + 1;
        BigDecimal doanhThuTrungBinhNgay = daysInMonth > 0 ? 
            doanhThuThangNay.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        // Find best revenue day this month
        // TODO: Implement finding best day and amount
        LocalDate ngayDoanhThuTotNhat = today;
        BigDecimal doanhThuTotNhat = doanhThuHomNay;
        
        // Calculate revenue breakdown by order type and payment method
        // TODO: Implement these calculations based on actual data
        DoanhThuTongQuanDto.DoanhThuTheoLoaiDto doanhThuTheoLoai = 
            DoanhThuTongQuanDto.DoanhThuTheoLoaiDto.builder()
                .taiQuay(BigDecimal.ZERO)
                .online(BigDecimal.ZERO)
                .tyLeTaiQuay(0.0)
                .tyLeOnline(0.0)
                .build();
        
        DoanhThuTongQuanDto.DoanhThuTheoThanhToanDto doanhThuTheoThanhToan =
            DoanhThuTongQuanDto.DoanhThuTheoThanhToanDto.builder()
                .tienMat(doanhThuThangNay.multiply(BigDecimal.valueOf(0.6))) // Estimate 60% cash
                .chuyenKhoan(BigDecimal.ZERO)
                .vnpay(doanhThuThangNay.multiply(BigDecimal.valueOf(0.4))) // Estimate 40% VNPay
                .cod(BigDecimal.ZERO)
                .build();
        
        return DoanhThuTongQuanDto.builder()
            .doanhThuHomNay(doanhThuHomNay)
            .doanhThuHomQua(doanhThuHomQua)
            .doanhThuTuanNay(doanhThuTuanNay)
            .doanhThuTuanTruoc(doanhThuTuanTruoc)
            .doanhThuThangNay(doanhThuThangNay)
            .doanhThuThangTruoc(doanhThuThangTruoc)
            .doanhThuNamNay(doanhThuNamNay)
            .doanhThuNamTruoc(doanhThuNamTruoc)
            .tyLeTangTruongNgay(tyLeTangTruongNgay)
            .tyLeTangTruongTuan(tyLeTangTruongTuan)
            .tyLeTangTruongThang(tyLeTangTruongThang)
            .tyLeTangTruongNam(tyLeTangTruongNam)
            .doanhThuTrungBinhNgay(doanhThuTrungBinhNgay)
            .ngayDoanhThuTotNhat(ngayDoanhThuTotNhat)
            .doanhThuTotNhat(doanhThuTotNhat)
            .doanhThuTheoLoai(doanhThuTheoLoai)
            .doanhThuTheoThanhToan(doanhThuTheoThanhToan)
            .build();
    }

    // ==================== HELPER METHODS ====================

    private BigDecimal calculateRevenueForPeriod(LocalDate startDate, LocalDate endDate) {
        List<HoaDon> orders = hoaDonRepository.findByNgayTaoBetweenAndTrangThaiDonHang(
            startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            endDate.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC),
            TrangThaiDonHang.HOAN_THANH
        );
        
        return orders.stream()
            .map(HoaDon::getTongThanhToan)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double calculateGrowthPercentage(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }

        BigDecimal growth = current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        return growth.doubleValue();
    }

    // ==================== DON HANG (ORDER) STATISTICS ====================

    @Override
    public DonHangTongQuanDto layDonHangTongQuan() {
        log.debug("Getting order overview statistics");

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfLastWeek = startOfWeek.minusDays(7);
        LocalDate endOfLastWeek = startOfWeek.minusDays(1);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);
        LocalDate startOfYear = today.withDayOfYear(1);

        // Count orders for different periods
        Long donHangHomNay = countOrdersForPeriod(today, today);
        Long donHangTuanNay = countOrdersForPeriod(startOfWeek, today);
        Long donHangThangNay = countOrdersForPeriod(startOfMonth, today);
        Long donHangNamNay = countOrdersForPeriod(startOfYear, today);
        Long tongSoDonHang = hoaDonRepository.count();

        // Count orders by status
        Long donHangChoXacNhan = hoaDonRepository.countByTrangThaiDonHang(TrangThaiDonHang.CHO_XAC_NHAN);
        Long donHangDangXuLy = hoaDonRepository.countByTrangThaiDonHang(TrangThaiDonHang.DANG_XU_LY);
        Long donHangDangGiao = hoaDonRepository.countByTrangThaiDonHang(TrangThaiDonHang.DANG_GIAO_HANG);
        Long donHangHoanThanh = hoaDonRepository.countByTrangThaiDonHang(TrangThaiDonHang.HOAN_THANH);
        Long donHangDaHuy = hoaDonRepository.countByTrangThaiDonHang(TrangThaiDonHang.DA_HUY);
        Long donHangTraHang = hoaDonRepository.countByTrangThaiDonHang(TrangThaiDonHang.DA_TRA_HANG);

        // Calculate average order value
        List<HoaDon> completedOrders = hoaDonRepository.findByTrangThaiDonHang(TrangThaiDonHang.HOAN_THANH);
        BigDecimal giaTriDonHangTrungBinh = completedOrders.isEmpty() ? BigDecimal.ZERO :
            completedOrders.stream()
                .map(HoaDon::getTongThanhToan)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(completedOrders.size()), 2, RoundingMode.HALF_UP);

        // Find highest order value
        BigDecimal giaTriDonHangCaoNhat = completedOrders.stream()
            .map(HoaDon::getTongThanhToan)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        // Calculate rates
        Double tyLeHoanThanh = tongSoDonHang > 0 ?
            (donHangHoanThanh.doubleValue() / tongSoDonHang.doubleValue()) * 100 : 0.0;
        Double tyLeHuy = tongSoDonHang > 0 ?
            (donHangDaHuy.doubleValue() / tongSoDonHang.doubleValue()) * 100 : 0.0;
        Double tyLeTraHang = tongSoDonHang > 0 ?
            (donHangTraHang.doubleValue() / tongSoDonHang.doubleValue()) * 100 : 0.0;

        // Calculate growth rates
        Long donHangHomQua = countOrdersForPeriod(yesterday, yesterday);
        Long donHangTuanTruoc = countOrdersForPeriod(startOfLastWeek, endOfLastWeek);
        Long donHangThangTruoc = countOrdersForPeriod(startOfLastMonth, endOfLastMonth);

        Double tyLeTangTruongNgay = calculateGrowthPercentage(
            BigDecimal.valueOf(donHangHomNay), BigDecimal.valueOf(donHangHomQua)).doubleValue();
        Double tyLeTangTruongTuan = calculateGrowthPercentage(
            BigDecimal.valueOf(donHangTuanNay), BigDecimal.valueOf(donHangTuanTruoc)).doubleValue();
        Double tyLeTangTruongThang = calculateGrowthPercentage(
            BigDecimal.valueOf(donHangThangNay), BigDecimal.valueOf(donHangThangTruoc)).doubleValue();

        // Calculate order breakdown by type
        Long taiQuayOrders = hoaDonRepository.countByLoaiHoaDon(LoaiHoaDon.TAI_QUAY);
        Long onlineOrders = hoaDonRepository.countByLoaiHoaDon(LoaiHoaDon.ONLINE);

        Double tyLeTaiQuay = tongSoDonHang > 0 ?
            (taiQuayOrders.doubleValue() / tongSoDonHang.doubleValue()) * 100 : 0.0;
        Double tyLeOnline = tongSoDonHang > 0 ?
            (onlineOrders.doubleValue() / tongSoDonHang.doubleValue()) * 100 : 0.0;

        DonHangTongQuanDto.DonHangTheoLoaiDto donHangTheoLoai =
            DonHangTongQuanDto.DonHangTheoLoaiDto.builder()
                .taiQuay(taiQuayOrders)
                .online(onlineOrders)
                .tyLeTaiQuay(tyLeTaiQuay)
                .tyLeOnline(tyLeOnline)
                .build();

        return DonHangTongQuanDto.builder()
            .donHangHomNay(donHangHomNay)
            .donHangTuanNay(donHangTuanNay)
            .donHangThangNay(donHangThangNay)
            .donHangNamNay(donHangNamNay)
            .tongSoDonHang(tongSoDonHang)
            .donHangChoXacNhan(donHangChoXacNhan)
            .donHangDangXuLy(donHangDangXuLy)
            .donHangDangGiao(donHangDangGiao)
            .donHangHoanThanh(donHangHoanThanh)
            .donHangDaHuy(donHangDaHuy)
            .donHangTraHang(donHangTraHang)
            .giaTriDonHangTrungBinh(giaTriDonHangTrungBinh)
            .giaTriDonHangCaoNhat(giaTriDonHangCaoNhat)
            .tyLeHoanThanh(tyLeHoanThanh)
            .tyLeHuy(tyLeHuy)
            .tyLeTraHang(tyLeTraHang)
            .tyLeTangTruongNgay(tyLeTangTruongNgay)
            .tyLeTangTruongTuan(tyLeTangTruongTuan)
            .tyLeTangTruongThang(tyLeTangTruongThang)
            .donHangTheoLoai(donHangTheoLoai)
            .build();
    }

    @Override
    public DonHangTheoTrangThaiDto layDonHangTheoTrangThai() {
        log.debug("Getting order statistics by status");

        // Get all order statuses and their counts
        Map<TrangThaiDonHang, Long> statusCounts = Arrays.stream(TrangThaiDonHang.values())
            .collect(Collectors.toMap(
                status -> status,
                status -> hoaDonRepository.countByTrangThaiDonHang(status)
            ));

        Long tongSoDonHang = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        // Prepare chart data
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        List<DonHangTheoTrangThaiDto.TrangThaiChiTietDto> chiTietTrangThai = new ArrayList<>();

        // Define colors for different statuses
        Map<TrangThaiDonHang, String> statusColors = Map.of(
            TrangThaiDonHang.CHO_XAC_NHAN, "#FFA500",
            TrangThaiDonHang.DANG_XU_LY, "#1E90FF",
            TrangThaiDonHang.DANG_GIAO_HANG, "#32CD32",
            TrangThaiDonHang.HOAN_THANH, "#228B22",
            TrangThaiDonHang.DA_HUY, "#DC143C",
            TrangThaiDonHang.DA_TRA_HANG, "#8B0000"
        );

        // Define status descriptions
        Map<TrangThaiDonHang, String> statusDescriptions = Map.of(
            TrangThaiDonHang.CHO_XAC_NHAN, "Đơn hàng đang chờ xác nhận",
            TrangThaiDonHang.DANG_XU_LY, "Đơn hàng đang được xử lý",
            TrangThaiDonHang.DANG_GIAO_HANG, "Đơn hàng đang được giao",
            TrangThaiDonHang.HOAN_THANH, "Đơn hàng đã hoàn thành",
            TrangThaiDonHang.DA_HUY, "Đơn hàng đã bị hủy",
            TrangThaiDonHang.DA_TRA_HANG, "Đơn hàng đã được trả lại"
        );

        for (Map.Entry<TrangThaiDonHang, Long> entry : statusCounts.entrySet()) {
            TrangThaiDonHang status = entry.getKey();
            Long count = entry.getValue();

            String statusName = getStatusDisplayName(status);
            labels.add(statusName);
            data.add(count);

            Double percentage = tongSoDonHang > 0 ?
                (count.doubleValue() / tongSoDonHang.doubleValue()) * 100 : 0.0;

            chiTietTrangThai.add(DonHangTheoTrangThaiDto.TrangThaiChiTietDto.builder()
                .tenTrangThai(statusName)
                .maTrangThai(status.name())
                .soLuong(count)
                .tyLe(percentage)
                .mauSac(statusColors.getOrDefault(status, "#808080"))
                .moTa(statusDescriptions.getOrDefault(status, ""))
                .build());
        }

        return DonHangTheoTrangThaiDto.builder()
            .labels(labels)
            .data(data)
            .tongSoDonHang(tongSoDonHang)
            .chiTietTrangThai(chiTietTrangThai)
            .build();
    }

    @Override
    public Map<String, Object> layGiaTriDonHangTrungBinh() {
        log.debug("Getting average order value statistics");

        List<HoaDon> completedOrders = hoaDonRepository.findByTrangThaiDonHang(TrangThaiDonHang.HOAN_THANH);

        if (completedOrders.isEmpty()) {
            return Map.of(
                "giaTriTrungBinh", BigDecimal.ZERO,
                "tongSoDonHang", 0L,
                "giaTriCaoNhat", BigDecimal.ZERO,
                "giaTriThapNhat", BigDecimal.ZERO
            );
        }

        BigDecimal tongGiaTri = completedOrders.stream()
            .map(HoaDon::getTongThanhToan)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal giaTriTrungBinh = tongGiaTri.divide(
            BigDecimal.valueOf(completedOrders.size()), 2, RoundingMode.HALF_UP);

        BigDecimal giaTriCaoNhat = completedOrders.stream()
            .map(HoaDon::getTongThanhToan)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        BigDecimal giaTriThapNhat = completedOrders.stream()
            .map(HoaDon::getTongThanhToan)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        return Map.of(
            "giaTriTrungBinh", giaTriTrungBinh,
            "tongSoDonHang", (long) completedOrders.size(),
            "giaTriCaoNhat", giaTriCaoNhat,
            "giaTriThapNhat", giaTriThapNhat,
            "tongGiaTri", tongGiaTri
        );
    }

    // ==================== HELPER METHODS ====================

    private Long countOrdersForPeriod(LocalDate startDate, LocalDate endDate) {
        return hoaDonRepository.countByNgayTaoBetween(
            startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            endDate.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC)
        );
    }

    private String getStatusDisplayName(TrangThaiDonHang status) {
        return switch (status) {
            case CHO_XAC_NHAN -> "Chờ xác nhận";
            case DANG_XU_LY -> "Đang xử lý";
            case DANG_GIAO_HANG -> "Đang giao hàng";
            case HOAN_THANH -> "Hoàn thành";
            case DA_HUY -> "Đã hủy";
            case DA_TRA_HANG -> "Trả hàng";
            default -> status.name();
        };
    }

    // ==================== SAN PHAM (PRODUCT) STATISTICS ====================

    @Override
    public SanPhamBanChayDto laySanPhamBanChayNhat(Integer soLuong, LocalDate tuNgay, LocalDate denNgay) {
        log.debug("Getting top {} selling products from {} to {}", soLuong, tuNgay, denNgay);

        // Set default dates if not provided
        if (denNgay == null) {
            denNgay = LocalDate.now();
        }
        if (tuNgay == null) {
            tuNgay = denNgay.minusDays(30);
        }

        // Set default limit if not provided
        if (soLuong == null || soLuong <= 0) {
            soLuong = 10;
        }

        // Convert dates to Instant for database query
        Instant tuNgayInstant = tuNgay.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant denNgayInstant = denNgay.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        // Get top selling products from completed orders
        Pageable pageable = Pageable.ofSize(soLuong);
        List<Object[]> topSellingData = hoaDonChiTietRepository.findTopSellingProducts(
            tuNgayInstant, denNgayInstant, pageable);

        List<SanPhamBanChayDto.SanPhamBanChayChiTietDto> danhSachSanPham = new ArrayList<>();

        for (int i = 0; i < topSellingData.size(); i++) {
            Object[] row = topSellingData.get(i);
            Long sanPhamId = (Long) row[0];
            String tenSanPham = (String) row[1];
            String hinhAnh = (String) row[2];
            String thuongHieu = (String) row[3];
            Long soLuongBan = ((Number) row[4]).longValue();
            BigDecimal doanhThu = (BigDecimal) row[5];

            // Calculate additional fields
            BigDecimal giaTrungBinh = soLuongBan > 0 ?
                doanhThu.divide(BigDecimal.valueOf(soLuongBan), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            SanPhamBanChayDto.SanPhamBanChayChiTietDto item = SanPhamBanChayDto.SanPhamBanChayChiTietDto.builder()
                .sanPhamId(sanPhamId)
                .tenSanPham(tenSanPham)
                .hinhAnh(hinhAnh != null ? hinhAnh : "")
                .thuongHieu(thuongHieu != null ? thuongHieu : "Không có")
                .soLuongBan(soLuongBan)
                .doanhThu(doanhThu)
                .giaTrungBinh(giaTrungBinh)
                .tonKho(0L) // TODO: Implement stock count query
                .thuHang(i + 1)
                .tyLeBanHang(0.0) // Will be calculated after getting totals
                .tyLeTangTruong(0.0) // TODO: Implement growth calculation
                .build();

            danhSachSanPham.add(item);
        }

        // Calculate totals
        BigDecimal tongDoanhThu = danhSachSanPham.stream()
            .map(SanPhamBanChayDto.SanPhamBanChayChiTietDto::getDoanhThu)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long tongSoLuongBan = danhSachSanPham.stream()
            .mapToLong(SanPhamBanChayDto.SanPhamBanChayChiTietDto::getSoLuongBan)
            .sum();

        // Calculate percentages and rankings
        for (int i = 0; i < danhSachSanPham.size(); i++) {
            SanPhamBanChayDto.SanPhamBanChayChiTietDto item = danhSachSanPham.get(i);
            item.setThuHang(i + 1);
            if (tongDoanhThu.compareTo(BigDecimal.ZERO) > 0) {
                double percentage = item.getDoanhThu().divide(tongDoanhThu, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
                item.setTyLeBanHang(percentage);
            }
        }

        return SanPhamBanChayDto.builder()
            .tuNgay(tuNgay)
            .denNgay(denNgay)
            .soLuong(soLuong)
            .danhSachSanPham(danhSachSanPham)
            .tongDoanhThu(tongDoanhThu)
            .tongSoLuongBan(tongSoLuongBan)
            .build();
    }

    @Override
    public SanPhamSapHetHangDto laySanPhamSapHetHang(Integer nguongTonKho) {
        log.debug("Getting low stock products with threshold {}", nguongTonKho);

        // Set default threshold if not provided
        final int threshold = (nguongTonKho == null || nguongTonKho < 0) ? 10 : nguongTonKho;

        // Get low stock products using SerialNumber counts
        List<Object[]> lowStockData = serialNumberRepository.getInventoryStatsByVariant();

        List<SanPhamSapHetHangDto.SanPhamSapHetHangChiTietDto> danhSachSanPham = new ArrayList<>();

        for (Object[] row : lowStockData) {
            Long sanPhamChiTietId = (Long) row[0];
            String trangThaiStr = (String) row[1];
            Long count = ((Number) row[2]).longValue();

            // Only include AVAILABLE stock and filter by threshold
            if ("AVAILABLE".equals(trangThaiStr) && count <= threshold) {
                // Get product details - simplified for now
                String tenSanPham = "Sản phẩm " + sanPhamChiTietId;
                String hinhAnh = "";
                String thuongHieu = "Không có";
                BigDecimal giaBan = BigDecimal.valueOf(1000000); // Default price

                // Calculate stock value
                BigDecimal giaTriTonKho = giaBan.multiply(BigDecimal.valueOf(count));

                // Determine stock status
                String mucDoTonKho;
                if (count == 0) {
                    mucDoTonKho = "HET_HANG";
                } else if (count <= threshold / 2) {
                    mucDoTonKho = "NGUY_HIEM";
                } else {
                    mucDoTonKho = "THAP";
                }

                SanPhamSapHetHangDto.SanPhamSapHetHangChiTietDto item = SanPhamSapHetHangDto.SanPhamSapHetHangChiTietDto.builder()
                    .sanPhamId(sanPhamChiTietId)
                    .tenSanPham(tenSanPham)
                    .hinhAnh(hinhAnh)
                    .thuongHieu(thuongHieu)
                    .tonKho(count)
                    .gia(giaBan)
                    .giaTriTonKho(giaTriTonKho)
                    .banTrungBinhNgay(0.0) // TODO: Calculate average sales
                    .soNgayConLai(0) // TODO: Calculate days remaining
                    .mucDoTonKho(mucDoTonKho)
                    .soLuongDeXuat((long) threshold * 2) // Suggest double threshold
                    .ngayNhapCuoi("Không có dữ liệu")
                    .build();

                danhSachSanPham.add(item);
            }
        }

        // Calculate summary statistics
        Long tongSoSanPham = (long) danhSachSanPham.size();
        Long sanPhamHetHang = danhSachSanPham.stream()
            .mapToLong(item -> item.getTonKho() == 0 ? 1 : 0)
            .sum();
        Long sanPhamTonKhoNguyHiem = danhSachSanPham.stream()
            .mapToLong(item -> item.getTonKho() > 0 && item.getTonKho() <= threshold / 2 ? 1 : 0)
            .sum();

        BigDecimal tongGiaTriTonKho = danhSachSanPham.stream()
            .map(SanPhamSapHetHangDto.SanPhamSapHetHangChiTietDto::getGiaTriTonKho)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SanPhamSapHetHangDto.builder()
            .nguongTonKho(threshold)
            .tongSoSanPham(tongSoSanPham)
            .danhSachSanPham(danhSachSanPham)
            .tongGiaTriTonKho(tongGiaTriTonKho)
            .sanPhamHetHang(sanPhamHetHang)
            .sanPhamTonKhoNguyHiem(sanPhamTonKhoNguyHiem)
            .build();
    }

    @Override
    public SanPhamTheoDanhMucDto laySanPhamTheoDanhMuc() {
        log.debug("Getting product performance by category");

        // Get current date range (last 30 days)
        LocalDate denNgay = LocalDate.now();
        LocalDate tuNgay = denNgay.minusDays(30);
        Instant tuNgayInstant = tuNgay.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant denNgayInstant = denNgay.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        // Get category performance data
        Pageable pageable = Pageable.ofSize(20); // Top 20 categories
        List<Object[]> categoryData = hoaDonChiTietRepository.findTopSellingCategories(
            tuNgayInstant, denNgayInstant, pageable);

        List<String> labels = new ArrayList<>();
        List<BigDecimal> doanhThuData = new ArrayList<>();
        List<Long> soLuongData = new ArrayList<>();
        List<SanPhamTheoDanhMucDto.DanhMucChiTietDto> chiTietDanhMuc = new ArrayList<>();

        BigDecimal tongDoanhThu = BigDecimal.ZERO;
        Long tongSoLuong = 0L;

        for (int i = 0; i < categoryData.size(); i++) {
            Object[] row = categoryData.get(i);
            Long danhMucId = (Long) row[0];
            String tenDanhMuc = (String) row[1];
            Long soLuong = ((Number) row[2]).longValue();
            BigDecimal doanhThu = (BigDecimal) row[3];

            labels.add(tenDanhMuc);
            doanhThuData.add(doanhThu);
            soLuongData.add(soLuong);

            tongDoanhThu = tongDoanhThu.add(doanhThu);
            tongSoLuong += soLuong;

            // Calculate percentages
            Double tyLeDoanhThu = 0.0;
            Double tyLeSoLuong = 0.0;
            if (tongDoanhThu.compareTo(BigDecimal.ZERO) > 0) {
                tyLeDoanhThu = doanhThu.divide(tongDoanhThu, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            }
            if (tongSoLuong > 0) {
                tyLeSoLuong = (soLuong.doubleValue() / tongSoLuong.doubleValue()) * 100;
            }

            SanPhamTheoDanhMucDto.DanhMucChiTietDto chiTiet = SanPhamTheoDanhMucDto.DanhMucChiTietDto.builder()
                .danhMucId(danhMucId)
                .tenDanhMuc(tenDanhMuc)
                .moTa("Danh mục " + tenDanhMuc)
                .soLuongSanPham(0L) // TODO: Count products in category
                .soLuongBan(soLuong)
                .doanhThu(doanhThu)
                .giaTrungBinh(soLuong > 0 ? doanhThu.divide(BigDecimal.valueOf(soLuong), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .tyLeDoanhThu(tyLeDoanhThu)
                .tyLeSoLuong(tyLeSoLuong)
                .tyLeTangTruong(0.0) // TODO: Calculate growth
                .sanPhamBanChayNhat("Không có dữ liệu")
                .tonKho(0L) // TODO: Calculate stock
                .giaTriTonKho(BigDecimal.ZERO) // TODO: Calculate stock value
                .thuHang(i + 1)
                .build();

            chiTietDanhMuc.add(chiTiet);
        }

        return SanPhamTheoDanhMucDto.builder()
            .labels(labels)
            .doanhThuData(doanhThuData)
            .soLuongData(soLuongData)
            .tongDoanhThu(tongDoanhThu)
            .tongSoLuong(tongSoLuong)
            .chiTietDanhMuc(chiTietDanhMuc)
            .build();
    }

    // ==================== KHACH HANG (CUSTOMER) STATISTICS ====================

    @Override
    public KhachHangMoiDto layKhachHangMoi(LocalDate tuNgay, LocalDate denNgay) {
        log.debug("Getting new customer statistics from {} to {}", tuNgay, denNgay);

        // Set default dates if not provided
        if (denNgay == null) {
            denNgay = LocalDate.now();
        }
        if (tuNgay == null) {
            tuNgay = denNgay.minusDays(30);
        }

        // Convert to Instant for database queries
        Instant tuNgayInstant = tuNgay.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant denNgayInstant = denNgay.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        // Get total new customers in the period
        Long tongKhachHangMoi = nguoiDungRepository.countNewCustomersBetween(
            VaiTro.CUSTOMER, tuNgayInstant, denNgayInstant);

        // Get daily new customer counts for chart
        List<Object[]> dailyCounts = nguoiDungRepository.getDailyNewCustomerCounts(
            "CUSTOMER", tuNgayInstant, denNgayInstant);

        // Process daily data for chart
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        LocalDate bestDay = tuNgay;
        Long bestDayCount = 0L;

        for (Object[] row : dailyCounts) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate date = sqlDate.toLocalDate();
            Long count = ((Number) row[1]).longValue();

            labels.add(date.toString());
            data.add(count);

            if (count > bestDayCount) {
                bestDayCount = count;
                bestDay = date;
            }
        }

        // Calculate average new customers per day
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(tuNgay, denNgay) + 1;
        Double khachHangMoiTrungBinhNgay = daysBetween > 0 ?
            tongKhachHangMoi.doubleValue() / daysBetween : 0.0;

        // Get previous period for growth calculation
        LocalDate tuNgayTruoc = tuNgay.minusDays(daysBetween);
        LocalDate denNgayTruoc = tuNgay.minusDays(1);
        Instant tuNgayTruocInstant = tuNgayTruoc.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant denNgayTruocInstant = denNgayTruoc.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        Long khachHangMoiKyTruoc = nguoiDungRepository.countNewCustomersBetween(
            VaiTro.CUSTOMER, tuNgayTruocInstant, denNgayTruocInstant);

        // Calculate growth rate
        Double tyLeTangTruong = calculateGrowthPercentage(
            BigDecimal.valueOf(tongKhachHangMoi), BigDecimal.valueOf(khachHangMoiKyTruoc));

        // Get first-time vs returning customers
        List<NguoiDung> firstTimeCustomers =
            nguoiDungRepository.findFirstTimeCustomers(VaiTro.CUSTOMER, tuNgayInstant, denNgayInstant);
        List<NguoiDung> returningCustomers =
            nguoiDungRepository.findReturningCustomers(VaiTro.CUSTOMER, tuNgayInstant, denNgayInstant);

        Long khachHangQuayLai = (long) returningCustomers.size();

        // Calculate retention rate
        Double tyLeGiuChan = tongKhachHangMoi > 0 ?
            (khachHangQuayLai.doubleValue() / tongKhachHangMoi.doubleValue()) * 100 : 0.0;

        // For now, we'll use simplified online/offline breakdown
        // In a real implementation, you might track registration source
        Long khachHangOnline = Math.round(tongKhachHangMoi * 0.7); // Assume 70% online
        Long khachHangTaiQuay = tongKhachHangMoi - khachHangOnline;

        Double tyLeOnline = tongKhachHangMoi > 0 ?
            (khachHangOnline.doubleValue() / tongKhachHangMoi.doubleValue()) * 100 : 0.0;
        Double tyLeTaiQuay = tongKhachHangMoi > 0 ?
            (khachHangTaiQuay.doubleValue() / tongKhachHangMoi.doubleValue()) * 100 : 0.0;

        // Calculate average first order value (simplified)
        BigDecimal giaTriDonHangDauTrungBinh = BigDecimal.ZERO;
        if (!firstTimeCustomers.isEmpty()) {
            // This would require more complex query to get actual first order values
            // For now, using a placeholder calculation
            giaTriDonHangDauTrungBinh = new BigDecimal("500000"); // 500k VND average
        }

        return KhachHangMoiDto.builder()
            .tuNgay(tuNgay)
            .denNgay(denNgay)
            .labels(labels)
            .data(data)
            .tongKhachHangMoi(tongKhachHangMoi)
            .khachHangMoiTrungBinhNgay(khachHangMoiTrungBinhNgay)
            .ngayTotNhat(bestDay)
            .khachHangMoiNgayTotNhat(bestDayCount)
            .tyLeTangTruong(tyLeTangTruong)
            .khachHangMoiKyTruoc(khachHangMoiKyTruoc)
            .chiTiet(KhachHangMoiDto.KhachHangMoiChiTietDto.builder()
                .khachHangOnline(khachHangOnline)
                .khachHangTaiQuay(khachHangTaiQuay)
                .khachHangGioiThieu(0L) // Would need tracking system
                .khachHangMarketing(0L) // Would need tracking system
                .tyLeOnline(tyLeOnline)
                .tyLeTaiQuay(tyLeTaiQuay)
                .giaTriDonHangDauTrungBinh(giaTriDonHangDauTrungBinh)
                .khachHangQuayLai(khachHangQuayLai)
                .tyLeGiuChan(tyLeGiuChan)
                .build())
            .build();
    }

    @Override
    public Map<String, Object> layTyLeGiuChanKhachHang() {
        log.debug("Getting customer retention rate");

        // Calculate retention for the last 3 months
        LocalDate denNgay = LocalDate.now();
        LocalDate tuNgay = denNgay.minusMonths(3);
        Instant tuNgayInstant = tuNgay.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant denNgayInstant = denNgay.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        // Get total active customers (customers who made orders)
        Long tongKhachHang = hoaDonRepository.countActiveCustomers(
            tuNgayInstant, denNgayInstant, TrangThaiDonHang.HOAN_THANH);

        // Get repeat customers (customers who made more than one order)
        Long khachHangQuayLai = hoaDonRepository.countRepeatCustomers(
            tuNgayInstant, denNgayInstant, TrangThaiDonHang.HOAN_THANH);

        // Get new customers in this period
        Long khachHangMoi = nguoiDungRepository.countNewCustomersBetween(
            VaiTro.CUSTOMER, tuNgayInstant, denNgayInstant);

        // Calculate existing customers
        Long khachHangCu = tongKhachHang - khachHangMoi;

        // Calculate retention rate
        Double tyLeGiuChan = tongKhachHang > 0 ?
            (khachHangQuayLai.doubleValue() / tongKhachHang.doubleValue()) * 100 : 0.0;

        return Map.of(
            "tyLeGiuChan", tyLeGiuChan,
            "khachHangQuayLai", khachHangQuayLai,
            "tongKhachHang", tongKhachHang,
            "khachHangMoi", khachHangMoi,
            "khachHangCu", khachHangCu
        );
    }

    @Override
    public Map<String, Object> layGiaTriKhachHangTrungBinh() {
        log.debug("Getting average customer value");

        try {
            // Get customer value statistics from completed orders
            Object[] customerStats = hoaDonRepository.getCustomerValueStatistics(TrangThaiDonHang.HOAN_THANH);

            if (customerStats == null || customerStats.length == 0 || customerStats[0] == null) {
                log.debug("No customer value statistics found, returning zero values");
                return Map.of(
                    "giaTriTrungBinh", BigDecimal.ZERO,
                    "giaTriCaoNhat", BigDecimal.ZERO,
                    "giaTriThapNhat", BigDecimal.ZERO,
                    "tongKhachHang", 0L,
                    "tongGiaTri", BigDecimal.ZERO
                );
            }

            log.debug("Customer stats array length: {}, first element: {}", customerStats.length, customerStats[0]);

            // Extract statistics from query result with safe casting
            // [customer_count, total_value, avg_value, max_value, min_value]
            Long tongKhachHang = 0L;
            BigDecimal tongGiaTri = BigDecimal.ZERO;
            BigDecimal giaTriTrungBinh = BigDecimal.ZERO;
            BigDecimal giaTriCaoNhat = BigDecimal.ZERO;
            BigDecimal giaTriThapNhat = BigDecimal.ZERO;

            // Safe extraction with type checking
            if (customerStats[0] instanceof Number) {
                tongKhachHang = ((Number) customerStats[0]).longValue();
            }
            if (customerStats.length > 1 && customerStats[1] instanceof BigDecimal) {
                tongGiaTri = (BigDecimal) customerStats[1];
            }
            if (customerStats.length > 2 && customerStats[2] instanceof BigDecimal) {
                giaTriTrungBinh = (BigDecimal) customerStats[2];
            }
            if (customerStats.length > 3 && customerStats[3] instanceof BigDecimal) {
                giaTriCaoNhat = (BigDecimal) customerStats[3];
            }
            if (customerStats.length > 4 && customerStats[4] instanceof BigDecimal) {
                giaTriThapNhat = (BigDecimal) customerStats[4];
            }

            return Map.of(
                "giaTriTrungBinh", giaTriTrungBinh,
                "giaTriCaoNhat", giaTriCaoNhat,
                "giaTriThapNhat", giaTriThapNhat,
                "tongKhachHang", tongKhachHang,
                "tongGiaTri", tongGiaTri
            );

        } catch (Exception e) {
            log.error("Error getting customer value statistics: {}", e.getMessage(), e);
            // Return default values on error
            return Map.of(
                "giaTriTrungBinh", BigDecimal.ZERO,
                "giaTriCaoNhat", BigDecimal.ZERO,
                "giaTriThapNhat", BigDecimal.ZERO,
                "tongKhachHang", 0L,
                "tongGiaTri", BigDecimal.ZERO
            );
        }
    }

    // ==================== DASHBOARD SUMMARY ====================

    @Override
    public DashboardSummaryDto layDashboardSummary() {
        log.debug("Getting dashboard summary");

        LocalDateTime now = LocalDateTime.now();

        // Get revenue summary
        DoanhThuTongQuanDto doanhThuTongQuan = layDoanhThuTongQuan();
        DashboardSummaryDto.DoanhThuSummary doanhThuSummary = DashboardSummaryDto.DoanhThuSummary.builder()
            .homNay(doanhThuTongQuan.getDoanhThuHomNay())
            .tuanNay(doanhThuTongQuan.getDoanhThuTuanNay())
            .thangNay(doanhThuTongQuan.getDoanhThuThangNay())
            .namNay(doanhThuTongQuan.getDoanhThuNamNay())
            .tangTruongNgay(doanhThuTongQuan.getTyLeTangTruongNgay())
            .tangTruongTuan(doanhThuTongQuan.getTyLeTangTruongTuan())
            .tangTruongThang(doanhThuTongQuan.getTyLeTangTruongThang())
            .tangTruongNam(doanhThuTongQuan.getTyLeTangTruongNam())
            .build();

        // Get order summary
        DonHangTongQuanDto donHangTongQuan = layDonHangTongQuan();
        DashboardSummaryDto.DonHangSummary donHangSummary = DashboardSummaryDto.DonHangSummary.builder()
            .tongSo(donHangTongQuan.getTongSoDonHang())
            .choXacNhan(donHangTongQuan.getDonHangChoXacNhan())
            .dangXuLy(donHangTongQuan.getDonHangDangXuLy())
            .hoanThanh(donHangTongQuan.getDonHangHoanThanh())
            .daHuy(donHangTongQuan.getDonHangDaHuy())
            .tyLeHoanThanh(donHangTongQuan.getTyLeHoanThanh())
            .giaTriTrungBinh(donHangTongQuan.getGiaTriDonHangTrungBinh())
            .build();

        // Get product summary
        DashboardSummaryDto.SanPhamSummary sanPhamSummary = laySanPhamSummary();

        // Get customer summary using real data
        DashboardSummaryDto.KhachHangSummary khachHangSummary = layKhachHangSummary();

        // Get notification summary (placeholder)
        DashboardSummaryDto.ThongBaoSummary thongBaoSummary = DashboardSummaryDto.ThongBaoSummary.builder()
            .donHangMoi(donHangTongQuan.getDonHangChoXacNhan())
            .sanPhamSapHetHang(0L)
            .khachHangMoi(0L)
            .danhGiaMoi(0L)
            .tongThongBao(donHangTongQuan.getDonHangChoXacNhan())
            .build();

        return DashboardSummaryDto.builder()
            .capNhatLanCuoi(now)
            .doanhThu(doanhThuSummary)
            .donHang(donHangSummary)
            .sanPham(sanPhamSummary)
            .khachHang(khachHangSummary)
            .thongBao(thongBaoSummary)
            .build();
    }

    /**
     * Get product summary for dashboard using real data from database
     */
    private DashboardSummaryDto.SanPhamSummary laySanPhamSummary() {
        log.debug("Getting product summary for dashboard");

        // Get total product count
        Long tongSoSanPham = sanPhamRepository.count();

        // Get products with low stock using SerialNumber status
        // Count serial numbers that are reserved or sold (approximation for low stock)
        Long sapHetHang = serialNumberRepository.countByTrangThai(TrangThaiSerialNumber.RESERVED) +
                         serialNumberRepository.countByTrangThai(TrangThaiSerialNumber.SOLD);

        // Get out of stock products (sold serial numbers)
        Long hetHang = serialNumberRepository.countByTrangThai(TrangThaiSerialNumber.SOLD);

        // Get top selling products from last 30 days using real data
        LocalDate tuNgay = LocalDate.now().minusDays(30);
        LocalDate denNgay = LocalDate.now();
        Instant tuNgayInstant = tuNgay.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant denNgayInstant = denNgay.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        Pageable topProductsPageable = PageRequest.of(0, 5); // Top 5 products
        List<Object[]> topSellingData = hoaDonChiTietRepository.findTopSellingProducts(
            tuNgayInstant, denNgayInstant, topProductsPageable);

        List<DashboardSummaryDto.SanPhamBanChayChiTietDto> banChayNhat = topSellingData.stream()
            .map(data -> {
                Long sanPhamId = ((Number) data[0]).longValue();
                String tenSanPham = (String) data[1];
                String hinhAnh = (String) data[2];
                String thuongHieu = (String) data[3]; // This is mo_ta_thuong_hieu from database
                Long soLuongBan = ((Number) data[4]).longValue();
                BigDecimal doanhThu = (BigDecimal) data[5];

                return DashboardSummaryDto.SanPhamBanChayChiTietDto.builder()
                    .id(sanPhamId)
                    .tenSanPham(tenSanPham)
                    .hinhAnh(hinhAnh != null && !hinhAnh.isEmpty() ? hinhAnh : "/images/default-product.jpg")
                    .thuongHieu(thuongHieu)
                    .soLuongBan(soLuongBan)
                    .doanhThu(doanhThu)
                    .build();
            })
            .collect(Collectors.toList());

        // Get top categories by sales using real data
        Pageable topCategoriesPageable = PageRequest.of(0, 5); // Top 5 categories
        List<Object[]> topCategoryData = hoaDonChiTietRepository.findTopSellingCategories(
            tuNgayInstant, denNgayInstant, topCategoriesPageable);

        List<DashboardSummaryDto.DanhMucTotDto> danhMucTot = topCategoryData.stream()
            .map(data -> {
                Long danhMucId = ((Number) data[0]).longValue();
                String tenDanhMuc = (String) data[1]; // This is mo_ta_danh_muc from database
                Long soLuong = ((Number) data[2]).longValue();
                BigDecimal doanhThu = (BigDecimal) data[3];

                return DashboardSummaryDto.DanhMucTotDto.builder()
                    .id(danhMucId)
                    .tenDanhMuc(tenDanhMuc)
                    .soLuong(soLuong)
                    .doanhThu(doanhThu)
                    .build();
            })
            .collect(Collectors.toList());

        return DashboardSummaryDto.SanPhamSummary.builder()
            .tongSo(tongSoSanPham)
            .sapHetHang(sapHetHang)
            .hetHang(hetHang)
            .banChayNhat(banChayNhat)
            .danhMucTot(danhMucTot)
            .build();
    }

    /**
     * Get customer summary for dashboard using real data
     */
    private DashboardSummaryDto.KhachHangSummary layKhachHangSummary() {
        log.debug("Getting customer summary for dashboard");

        // Get total customer count
        Long tongSo = nguoiDungRepository.countByVaiTroAndTrangThai(
            VaiTro.CUSTOMER, TrangThaiNguoiDung.HOAT_DONG);

        // Get new customers in the last 30 days
        LocalDate tuNgay = LocalDate.now().minusDays(30);
        LocalDate denNgay = LocalDate.now();
        Instant tuNgayInstant = tuNgay.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant denNgayInstant = denNgay.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);

        Long khachHangMoi = nguoiDungRepository.countNewCustomersBetween(
            VaiTro.CUSTOMER, tuNgayInstant, denNgayInstant);

        // Get active customers (customers who made orders in last 30 days)
        Long khachHangHoatDong = hoaDonRepository.countActiveCustomers(
            tuNgayInstant, denNgayInstant, TrangThaiDonHang.HOAN_THANH);

        // Get retention rate from the retention method
        Map<String, Object> retentionData = layTyLeGiuChanKhachHang();
        Double tyLeGiuChan = (Double) retentionData.get("tyLeGiuChan");

        // Get average customer value from the customer value method
        Map<String, Object> valueData = layGiaTriKhachHangTrungBinh();
        BigDecimal giaTriTrungBinh = (BigDecimal) valueData.get("giaTriTrungBinh");

        return DashboardSummaryDto.KhachHangSummary.builder()
            .tongSo(tongSo)
            .moi(khachHangMoi)
            .hoatDong(khachHangHoatDong)
            .tyLeGiuChan(tyLeGiuChan)
            .giaTriTrungBinh(giaTriTrungBinh)
            .build();
    }
}
