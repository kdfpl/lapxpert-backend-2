package com.lapxpert.backend.thongke.application.service.impl;

import com.lapxpert.backend.thongke.application.service.ThongKeService;
import com.lapxpert.backend.thongke.domain.dto.*;
import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.enums.LoaiHoaDon;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonRepository;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonChiTietRepository;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamRepository;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
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
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
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
        
        // Generate quarter breakdown
        List<DoanhThuTheoThangDto.QuarterRevenueDto> doanhThuTheoQuy = new ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            int startMonth = (quarter - 1) * 3;
            BigDecimal quarterRevenue = data.subList(startMonth, startMonth + 3).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            doanhThuTheoQuy.add(DoanhThuTheoThangDto.QuarterRevenueDto.builder()
                .quy(quarter)
                .doanhThu(quarterRevenue)
                .tyLeTangTruong(0.0) // TODO: Calculate quarter growth
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
                .tienMat(BigDecimal.ZERO)
                .chuyenKhoan(BigDecimal.ZERO)
                .vnpay(BigDecimal.ZERO)
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

        // TODO: Implement actual product sales query
        // This is a placeholder implementation
        return SanPhamBanChayDto.builder()
            .tuNgay(tuNgay)
            .denNgay(denNgay)
            .soLuong(soLuong)
            .danhSachSanPham(List.of())
            .tongDoanhThu(BigDecimal.ZERO)
            .tongSoLuongBan(0L)
            .build();
    }

    @Override
    public SanPhamSapHetHangDto laySanPhamSapHetHang(Integer nguongTonKho) {
        log.debug("Getting low stock products with threshold {}", nguongTonKho);

        // TODO: Implement actual low stock query
        // This is a placeholder implementation
        return SanPhamSapHetHangDto.builder()
            .nguongTonKho(nguongTonKho)
            .tongSoSanPham(0L)
            .danhSachSanPham(List.of())
            .tongGiaTriTonKho(BigDecimal.ZERO)
            .sanPhamHetHang(0L)
            .sanPhamTonKhoNguyHiem(0L)
            .build();
    }

    @Override
    public SanPhamTheoDanhMucDto laySanPhamTheoDanhMuc() {
        log.debug("Getting product performance by category");

        // TODO: Implement actual category performance query
        // This is a placeholder implementation
        return SanPhamTheoDanhMucDto.builder()
            .labels(List.of())
            .doanhThuData(List.of())
            .soLuongData(List.of())
            .tongDoanhThu(BigDecimal.ZERO)
            .tongSoLuong(0L)
            .chiTietDanhMuc(List.of())
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

        // TODO: Implement actual new customer query
        // This is a placeholder implementation
        return KhachHangMoiDto.builder()
            .tuNgay(tuNgay)
            .denNgay(denNgay)
            .labels(List.of())
            .data(List.of())
            .tongKhachHangMoi(0L)
            .khachHangMoiTrungBinhNgay(0.0)
            .ngayTotNhat(tuNgay)
            .khachHangMoiNgayTotNhat(0L)
            .tyLeTangTruong(0.0)
            .khachHangMoiKyTruoc(0L)
            .chiTiet(KhachHangMoiDto.KhachHangMoiChiTietDto.builder()
                .khachHangOnline(0L)
                .khachHangTaiQuay(0L)
                .khachHangGioiThieu(0L)
                .khachHangMarketing(0L)
                .tyLeOnline(0.0)
                .tyLeTaiQuay(0.0)
                .giaTriDonHangDauTrungBinh(BigDecimal.ZERO)
                .khachHangQuayLai(0L)
                .tyLeGiuChan(0.0)
                .build())
            .build();
    }

    @Override
    public Map<String, Object> layTyLeGiuChanKhachHang() {
        log.debug("Getting customer retention rate");

        // TODO: Implement actual customer retention calculation
        // This is a placeholder implementation
        return Map.of(
            "tyLeGiuChan", 0.0,
            "khachHangQuayLai", 0L,
            "tongKhachHang", 0L,
            "khachHangMoi", 0L,
            "khachHangCu", 0L
        );
    }

    @Override
    public Map<String, Object> layGiaTriKhachHangTrungBinh() {
        log.debug("Getting average customer value");

        // TODO: Implement actual customer value calculation
        // This is a placeholder implementation
        return Map.of(
            "giaTriTrungBinh", BigDecimal.ZERO,
            "giaTriCaoNhat", BigDecimal.ZERO,
            "giaTriThapNhat", BigDecimal.ZERO,
            "tongKhachHang", 0L,
            "tongGiaTri", BigDecimal.ZERO
        );
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

        // Get customer summary (placeholder)
        DashboardSummaryDto.KhachHangSummary khachHangSummary = DashboardSummaryDto.KhachHangSummary.builder()
            .tongSo(0L)
            .moi(0L)
            .hoatDong(0L)
            .tyLeGiuChan(0.0)
            .giaTriTrungBinh(BigDecimal.ZERO)
            .build();

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

        // Get products with low stock using available methods
        // Count products that are not available (approximation for low stock)
        Long sapHetHang = sanPhamChiTietRepository.countByTrangThai(TrangThaiSanPham.RESERVED) +
                         sanPhamChiTietRepository.countByTrangThai(TrangThaiSanPham.SOLD);

        // Get out of stock products (sold items)
        Long hetHang = sanPhamChiTietRepository.countByTrangThai(TrangThaiSanPham.SOLD);

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
}
