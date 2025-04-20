package com.lapxpert.backend.phieugiamgia.domain.service;
import com.lapxpert.backend.nguoidung.application.dto.KhachHangDTO;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaNguoiDungDto;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDungId;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaNguoiDungRepository;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private PhieuGiamGiaNguoiDungRepository phieuGiamGiaNguoiDungRepository;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private EmailService emailService;

    public List<PhieuGiamGiaDto> getAllPhieuGiamGia() {
        List<PhieuGiamGia> phieuGiamGias = phieuGiamGiaRepository.findAll();
        return phieuGiamGias.stream()
                .map(phieuGiamGia -> {
                    List<Long> danhSachNguoiDung = new ArrayList<>();
                    if (phieuGiamGia.getPhieuRiengTu()) {
                        // Nếu phiếu giảm giá là riêng tư, thêm các khách hàng vào danh sách
                        danhSachNguoiDung = phieuGiamGia.getDanhSachNguoiDung().stream()
                                .map(nguoiDung -> nguoiDung.getId().getNguoiDungId())
                                .collect(Collectors.toList());
                    }

                    return new PhieuGiamGiaDto(
                            phieuGiamGia.getId(),
                            phieuGiamGia.getMaPhieuGiamGia(),
                            phieuGiamGia.getLoaiPhieuGiamGia(),
                            phieuGiamGia.getTrangThai(),
                            phieuGiamGia.getGiaTriGiam(),
                            phieuGiamGia.getGiaTriDonHangToiThieu(),
                            phieuGiamGia.getNgayBatDau(),
                            phieuGiamGia.getNgayKetThuc(),
                            phieuGiamGia.getMoTa(),
                            phieuGiamGia.getPhieuRiengTu(),
                            phieuGiamGia.getSoLuongBanDau(),
                            phieuGiamGia.getSoLuongDaDung(),
                            phieuGiamGia.getNgayCapNhat(),
                            phieuGiamGia.getNgayTao(),
                            danhSachNguoiDung
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void taoPhieu(PhieuGiamGiaDto req) {
        PhieuGiamGia phieu = new PhieuGiamGia();
        phieu.setMaPhieuGiamGia(req.getMaPhieuGiamGia());
        phieu.setGiaTriGiam(req.getGiaTriGiam());
        phieu.setGiaTriDonHangToiThieu(req.getGiaTriDonHangToiThieu());
        phieu.setNgayBatDau(req.getNgayBatDau());
        phieu.setNgayKetThuc(req.getNgayKetThuc());
        phieu.setPhieuRiengTu(req.getPhieuRiengTu());
        phieu.setLoaiPhieuGiamGia(req.getLoaiPhieuGiamGia() != null ? req.getLoaiPhieuGiamGia() : false);
        phieu.setSoLuongBanDau(req.getSoLuongBanDau() != null ? req.getSoLuongBanDau() : 0);

        if (phieu.getNgayBatDau().isAfter(phieu.getNgayKetThuc())) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể lớn hơn ngày kết thúc");
        }

        phieu.setTrangThai(PhieuGiamGia.fromDates(req.getNgayBatDau(), req.getNgayKetThuc()));
        phieuGiamGiaRepository.save(phieu);

        if (req.getPhieuRiengTu() && req.getDanhSachNguoiDung() != null) {
            // Nếu phiếu giảm giá là riêng tư
            for (Long nguoiDungId : req.getDanhSachNguoiDung()) {
                PhieuGiamGiaNguoiDungId id = new PhieuGiamGiaNguoiDungId(phieu.getId(), nguoiDungId);
                PhieuGiamGiaNguoiDung phieuND = new PhieuGiamGiaNguoiDung();
                phieuND.setId(id);
                phieuND.setPhieuGiamGia(phieu);
                phieuND.setNguoiDung(nguoiDungRepository.findById(nguoiDungId)
                        .orElseThrow(() -> new IllegalArgumentException("Người dùng với ID " + nguoiDungId + " không tồn tại")));
                phieuGiamGiaNguoiDungRepository.save(phieuND);
            }

            // Gửi email cho những khách hàng đã chọn
            List<String> selectedCustomerEmails = req.getDanhSachNguoiDung().stream()
                    .map(nguoiDungId -> nguoiDungRepository.findById(nguoiDungId)
                            .map(NguoiDung::getEmail)
                            .orElse(null))
                    .filter(email -> email != null)
                    .collect(Collectors.toList());

            String subject = "Ưu đãi đặc biệt dành riêng cho bạn!";
            String text = "Chào bạn,\n\n"
                    + "LapXpert trân trọng gửi đến bạn phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "** "
                    + "với những ưu đãi đặc biệt. Mã này được tạo dành riêng cho bạn và có hiệu lực từ ngày "
                    + phieu.getNgayBatDau() + " đến " + phieu.getNgayKetThuc() + ".\n\n"
                    +"Phiếu sẽ được áp dụng với hóa đơn từ"+phieu.getGiaTriDonHangToiThieu()+".\n\n"
                    + "Hãy nhanh tay sử dụng để không bỏ lỡ nhé!\n\n"
                    + "Trân trọng,\nLapXpert Team";
            emailService.sendBulkEmail(selectedCustomerEmails, subject, text);

        } else {
            // Nếu phiếu giảm giá công khai
            List<String> allCustomerEmails = nguoiDungRepository.findAll().stream()
                    .map(NguoiDung::getEmail)
                    .collect(Collectors.toList());

            String subject = "Ưu đãi mới cho tất cả khách hàng LapXpert!";
            String text = "Chào bạn,\n\n"
                    + "Chúng tôi vừa phát hành Phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "** áp dụng cho tất cả khách hàng, "
                    + "có hiệu lực từ ngày " + phieu.getNgayBatDau() + " đến " + phieu.getNgayKetThuc() + ".\n\n"
                    + "Nhanh tay sử dụng để nhận ưu đãi hấp dẫn!\n\n"
                    + "Trân trọng,\nLapXpert Team";
            emailService.sendBulkEmail(allCustomerEmails, subject, text);
        }
    }
    @Transactional
    public void capNhatPhieu(PhieuGiamGiaDto req, Long phieuId) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(phieuId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu"));

        PhieuGiamGia.TrangThaiPhieuGiamGia trangThaiCu = phieu.getTrangThai(); // Lưu lại trạng thái cũ

        // Cập nhật thông tin phiếu giảm giá
        phieu.setMaPhieuGiamGia(req.getMaPhieuGiamGia());
        phieu.setGiaTriGiam(req.getGiaTriGiam());
        phieu.setGiaTriDonHangToiThieu(req.getGiaTriDonHangToiThieu());
        phieu.setNgayBatDau(req.getNgayBatDau());
        phieu.setNgayKetThuc(req.getNgayKetThuc());
        phieu.setLoaiPhieuGiamGia(req.getLoaiPhieuGiamGia());
        phieu.setSoLuongBanDau(req.getSoLuongBanDau());
        phieu.setPhieuRiengTu(req.getPhieuRiengTu());
        phieu.setTrangThai(PhieuGiamGia.fromDates(req.getNgayBatDau(), req.getNgayKetThuc()));
        phieu.setNgayCapNhat(OffsetDateTime.now());
        phieuGiamGiaRepository.save(phieu);

        // Kiểm tra và gửi email nếu trạng thái phiếu giảm giá thay đổi
        if (trangThaiCu != phieu.getTrangThai()) {
            List<String> emails;
            String subject;
            String text;

            if (phieu.getTrangThai() == PhieuGiamGia.TrangThaiPhieuGiamGia.DA_DIEN_RA) {
                // Nếu trạng thái chuyển thành "Đã diễn ra"
                if (phieu.getPhieuRiengTu()) {
                    emails = phieu.getDanhSachNguoiDung().stream()
                            .map(nguoiDung -> nguoiDung.getNguoiDung().getEmail())
                            .collect(Collectors.toList());
                } else {
                    emails = nguoiDungRepository.findAll().stream()
                            .map(NguoiDung::getEmail)
                            .collect(Collectors.toList());
                }

                subject = "Phiếu giảm giá đã bắt đầu!";
                text = "Chào bạn,\n\n"
                        + "Phiếu giảm giá có mã là: **" + phieu.getMaPhieuGiamGia() + "** chính thức có hiệu lực từ hôm nay. "
                        + "Hãy nhanh tay sử dụng để nhận ưu đãi hấp dẫn nhé!\n\n"
                        + "Trân trọng,\nLapXpert Team";
            } else if (phieu.getTrangThai() == PhieuGiamGia.TrangThaiPhieuGiamGia.KET_THUC) {
                // Nếu trạng thái chuyển thành "Đã kết thúc"
                if (phieu.getPhieuRiengTu()) {
                    emails = phieu.getDanhSachNguoiDung().stream()
                            .map(nguoiDung -> nguoiDung.getNguoiDung().getEmail())
                            .collect(Collectors.toList());
                } else {
                    emails = nguoiDungRepository.findAll().stream()
                            .map(NguoiDung::getEmail)
                            .collect(Collectors.toList());
                }

                subject = "Phiếu giảm giá đã kết thúc!";
                text = "Chào bạn,\n\n"
                        + "Phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "** đã hết hạn. "
                        + "Cảm ơn bạn đã quan tâm và hy vọng sẽ có cơ hội phục vụ bạn trong các chương trình ưu đãi tiếp theo.\n\n"
                        + "Trân trọng,\nLapXpert Team";
            } else {
                // Trường hợp nếu không phải trạng thái "Đã diễn ra" hay "Đã kết thúc", không cần gửi email.
                return;
            }

            // Gửi email
            emailService.sendBulkEmail(emails, subject, text);
        }

        // Nếu phiếu giảm giá là riêng tư
        if (req.getPhieuRiengTu()) {
            if (req.getDanhSachNguoiDung() == null || req.getDanhSachNguoiDung().isEmpty()) {
                throw new IllegalArgumentException("Danh sách người dùng không thể trống khi phiếu là riêng tư.");
            }

            // Lấy danh sách khách hàng hiện tại
            List<Long> danhSachKhachHangCu = phieu.getDanhSachNguoiDung().stream()
                    .map(nd -> nd.getNguoiDung().getId())
                    .collect(Collectors.toList());

            // Lấy danh sách khách hàng mới từ yêu cầu
            List<Long> danhSachKhachHangMoi = req.getDanhSachNguoiDung();

            // Xóa các khách hàng không còn có trong danh sách mới
            danhSachKhachHangCu.removeAll(danhSachKhachHangMoi);
            if (!danhSachKhachHangCu.isEmpty()) {
                // Xóa khách hàng đã bỏ chọn
                phieuGiamGiaNguoiDungRepository.deleteByPhieuGiamGiaIdAndNguoiDungIdNotIn(phieu.getId(), danhSachKhachHangCu);
            }

            for (Long nguoiDungId : danhSachKhachHangMoi) {
                if (!danhSachKhachHangCu.contains(nguoiDungId)) {
                    PhieuGiamGiaNguoiDungId id = new PhieuGiamGiaNguoiDungId(phieu.getId(), nguoiDungId);
                    PhieuGiamGiaNguoiDung phieuND = new PhieuGiamGiaNguoiDung();
                    phieuND.setId(id);
                    phieuND.setPhieuGiamGia(phieu);
                    phieuND.setNguoiDung(
                            nguoiDungRepository.findById(nguoiDungId)
                                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại: " + nguoiDungId))
                    );
                    phieuGiamGiaNguoiDungRepository.save(phieuND);
                }
            }

            List<String> selectedCustomerEmails = req.getDanhSachNguoiDung().stream()
                    .map(nguoiDungId -> nguoiDungRepository.findById(nguoiDungId)
                            .map(NguoiDung::getEmail)
                            .orElse(null))
                    .filter(email -> email != null)
                    .collect(Collectors.toList());

            if (!selectedCustomerEmails.isEmpty()) {
                String subject = "Ưu đãi đặc biệt dành riêng cho bạn! Cập nhật thông tin phiếu giảm giá.";
                String text = "Chào bạn,\n\n"
                        + "LapXpert trân trọng thông báo, bạn đã được thêm vào danh sách nhận ưu đãi của phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "**. "
                        + "Phiếu giảm giá này có hiệu lực từ ngày " + phieu.getNgayBatDau() + " đến " + phieu.getNgayKetThuc() + ".\n\n"
                        + "Hãy nhanh tay sử dụng để không bỏ lỡ nhé!\n\n"
                        + "Trân trọng,\nLapXpert Team";
                emailService.sendBulkEmail(selectedCustomerEmails, subject, text);
            }
        } else {
            // Nếu phiếu giảm giá không phải là phiếu riêng tư, xóa tất cả khách hàng đã gán
            phieuGiamGiaNguoiDungRepository.deleteByPhieuGiamGiaId(phieu.getId());
        }
    }
    public PhieuGiamGiaDto getPhieuGiamGiaById(Long id) {
        PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu không tồn tại với ID: " + id));

        List<Long> danhSachNguoiDung = new ArrayList<>();
        if (phieuGiamGia.getPhieuRiengTu()) {
            danhSachNguoiDung = phieuGiamGia.getDanhSachNguoiDung().stream()
                    .map(nd -> nd.getId().getNguoiDungId())
                    .collect(Collectors.toList());
        }

        return new PhieuGiamGiaDto(
                phieuGiamGia.getId(),
                phieuGiamGia.getMaPhieuGiamGia(),
                phieuGiamGia.getLoaiPhieuGiamGia(),
                phieuGiamGia.getTrangThai(),
                phieuGiamGia.getGiaTriGiam(),
                phieuGiamGia.getGiaTriDonHangToiThieu(),
                phieuGiamGia.getNgayBatDau(),
                phieuGiamGia.getNgayKetThuc(),
                phieuGiamGia.getMoTa(),
                phieuGiamGia.getPhieuRiengTu(),
                phieuGiamGia.getSoLuongBanDau(),
                phieuGiamGia.getSoLuongDaDung(),
                phieuGiamGia.getNgayCapNhat(),
                phieuGiamGia.getNgayTao(),
                danhSachNguoiDung
        );
    }

    @Transactional
    public void deletePhieuGiamGia(Long phieuGiamGiaId) {

        PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findById(phieuGiamGiaId)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));

        phieuGiamGia.setNgayKetThuc(OffsetDateTime.now());

        phieuGiamGia.setTrangThai(PhieuGiamGia.TrangThaiPhieuGiamGia.KET_THUC);

        phieuGiamGiaRepository.save(phieuGiamGia);
        if (phieuGiamGia.getPhieuRiengTu()) {
            List<String> selectedCustomerEmails = phieuGiamGia.getDanhSachNguoiDung().stream()
                    .map(nguoiDung -> nguoiDung.getNguoiDung().getEmail())
                    .collect(Collectors.toList());
            String subject = "Xin lỗi vì ưu đãi đã bị hủy";
            String text = "Chào bạn,\n\n"
                    + "Chúng tôi xin lỗi vì phiếu giảm giá **" + phieuGiamGia.getMaPhieuGiamGia() + "** đã bị hủy trước khi hết hạn. "
                    + "Rất tiếc vì sự bất tiện này và hy vọng sẽ được phục vụ bạn với nhiều ưu đãi hấp dẫn khác trong thời gian tới.\n\n"
                    + "Trân trọng,\nLapXpert Team";
            emailService.sendBulkEmail(selectedCustomerEmails, subject, text);
        }
    }
    @Scheduled(cron = "0 0 0 * * *") // Mỗi ngày lúc 00:00
    public void nhacHoatDongPhieu() {
        List<PhieuGiamGia> danhSachPhieu = phieuGiamGiaRepository.findByTrangThai(PhieuGiamGia.TrangThaiPhieuGiamGia.CHUA_DIEN_RA);

        LocalDate homNay = LocalDate.now();

        for (PhieuGiamGia phieu : danhSachPhieu) {
            if (phieu.getNgayBatDau().toLocalDate().isEqual(homNay)) {
                phieu.setTrangThai(PhieuGiamGia.TrangThaiPhieuGiamGia.DA_DIEN_RA);
                phieuGiamGiaRepository.save(phieu);

                List<String> emails;
                if (phieu.getPhieuRiengTu()) {
                    emails = phieu.getDanhSachNguoiDung().stream()
                            .map(nguoiDung -> nguoiDung.getNguoiDung().getEmail())
                            .collect(Collectors.toList());
                } else {
                    emails = nguoiDungRepository.findAll().stream()
                            .map(NguoiDung::getEmail)
                            .collect(Collectors.toList());
                }

                String subject = "Mã giảm giá đã bắt đầu!";
                String text = "Chào bạn,\n\n"
                        + "Phiếu giảm giá có mã là **" + phieu.getMaPhieuGiamGia() + "** chính thức có hiệu lực từ hôm nay. "
                        + "Hãy nhanh tay sử dụng để nhận ưu đãi hấp dẫn nhé!\n\n"
                        + "Trân trọng,\nLapXpert Team";

                emailService.sendBulkEmail(emails, subject, text);
            }
        }
    }
}