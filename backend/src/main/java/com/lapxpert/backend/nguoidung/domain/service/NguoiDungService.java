package com.lapxpert.backend.nguoidung.domain.service;

import com.lapxpert.backend.common.service.EmailService;
import com.lapxpert.backend.common.service.UrlService;
import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDungAuditHistory;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import com.lapxpert.backend.nguoidung.domain.entity.TrangThaiNguoiDung;
import com.lapxpert.backend.nguoidung.application.dto.KhachHangDTO;
import com.lapxpert.backend.nguoidung.application.dto.NhanVienDTO;
import com.lapxpert.backend.nguoidung.application.dto.DiaChiDto;
import com.lapxpert.backend.nguoidung.application.mapper.NguoiDungMapper;
import com.lapxpert.backend.nguoidung.application.mapper.DiaChiMapper;
import com.lapxpert.backend.nguoidung.domain.exception.AddressNotFoundException;
import com.lapxpert.backend.nguoidung.domain.exception.DuplicateUserException;
import com.lapxpert.backend.nguoidung.domain.exception.UserNotFoundException;
import com.lapxpert.backend.nguoidung.domain.exception.UserValidationException;
import com.lapxpert.backend.nguoidung.domain.repository.DiaChiRepository;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungAuditHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
@Service
public class NguoiDungService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private NguoiDungAuditHistoryRepository auditHistoryRepository;

    @Autowired
    private DiaChiRepository diaChiRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UrlService urlService;

    @Autowired
    private NguoiDungMapper nguoiDungMapper;

    @Autowired
    private DiaChiMapper diaChiMapper;

    @Transactional(readOnly = true)
    public KhachHangDTO getKhachHang(Long id) {
        NguoiDung nguoiDung = nguoiDungRepository.findByIdWithAddresses(id)
                .orElseThrow(() -> UserNotFoundException.customer(id));

        KhachHangDTO dto = nguoiDungMapper.toKhachHangDto(nguoiDung);
        // Convert avatar filename to full URL
        dto.setAvatar(urlService.ensureAvatarUrl(dto.getAvatar()));
        return dto;
    }

    @Transactional(readOnly = true)
    public NhanVienDTO getNhanVien(Long id) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.staff(id));

        NhanVienDTO dto = nguoiDungMapper.toNhanVienDto(nguoiDung);
        // Convert avatar filename to full URL
        dto.setAvatar(urlService.ensureAvatarUrl(dto.getAvatar()));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<KhachHangDTO> getAllKhachHang() {
        List<NguoiDung> customers = nguoiDungRepository.findByVaiTroWithAddresses(VaiTro.CUSTOMER);
        List<KhachHangDTO> dtos = nguoiDungMapper.toKhachHangDtoList(customers);
        // Convert avatar filenames to full URLs
        dtos.forEach(dto -> dto.setAvatar(urlService.ensureAvatarUrl(dto.getAvatar())));
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<KhachHangDTO> searchKhachHang(String searchTerm) {
        List<NguoiDung> customers = nguoiDungRepository.searchByVaiTroAndTermWithAddresses(VaiTro.CUSTOMER, searchTerm);
        List<KhachHangDTO> dtos = nguoiDungMapper.toKhachHangDtoList(customers);
        // Convert avatar filenames to full URLs
        dtos.forEach(dto -> dto.setAvatar(urlService.ensureAvatarUrl(dto.getAvatar())));
        return dtos;
    }

    private void validateUniqueEmailAndPhone(String email, String phone, Long id) {
        Optional<NguoiDung> existingByEmail = Optional.empty();
        Optional<NguoiDung> existingByPhone = nguoiDungRepository.findBySoDienThoai(phone);

        // Only validate email uniqueness if email is provided
        if (email != null && !email.trim().isEmpty()) {
            existingByEmail = nguoiDungRepository.findByEmail(email);
        }

        if (existingByEmail.isPresent() &&
                existingByPhone.isPresent() &&
                !existingByEmail.get().getId().equals(id) &&
                !existingByPhone.get().getId().equals(id))
        {
            throw DuplicateUserException.emailAndPhone(email, phone);
        }

        if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(id)) {
            throw DuplicateUserException.email(email);
        }

        if (existingByPhone.isPresent() && !existingByPhone.get().getId().equals(id)) {
            throw DuplicateUserException.phone(phone);
        }
    }


    @Transactional(readOnly = true)
    public List<NhanVienDTO> getAllNhanVien() {
        List<NguoiDung> staff = nguoiDungRepository.findByVaiTroIn(Arrays.asList(VaiTro.STAFF, VaiTro.ADMIN));
        List<NhanVienDTO> dtos = nguoiDungMapper.toNhanVienDtoList(staff);
        // Convert avatar filenames to full URLs
        dtos.forEach(dto -> dto.setAvatar(urlService.ensureAvatarUrl(dto.getAvatar())));
        return dtos;
    }

    private String generateMaNguoiDung(String hoTen, VaiTro vaiTro) {
        // Lấy phần rút gọn của vai trò
        String prefix;
        switch (vaiTro) {
            case CUSTOMER:
                prefix = "CUS";
                break;
            case STAFF:
                prefix = "STAFF";
                break;
            case ADMIN:
                prefix = "ADM";
                break;
            default:
                throw UserValidationException.invalidRole(vaiTro.toString());
        }

        // Tách họ tên thành mảng
        String[] parts = hoTen.trim().split("\\s+");
        if (parts.length < 2) {
            throw UserValidationException.invalidName(hoTen);
        }

        // Lấy tên (phần cuối cùng)
        String tenChinh = parts[parts.length - 1];

        // Lấy chữ cái đầu của họ và tên đệm
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            initials.append(parts[i].charAt(0));
        }

        // Xây dựng tiền tố mã người dùng
        String baseMaNguoiDung = String.format("%s_%s%s", prefix, tenChinh, initials.toString().toLowerCase());

        // Tìm số thứ tự tiếp theo
        Optional<NguoiDung> lastUser = nguoiDungRepository.findTopByMaNguoiDungStartingWithOrderByMaNguoiDungDesc(baseMaNguoiDung);
        int nextNumber = lastUser.isPresent()
                ? Integer.parseInt(lastUser.get().getMaNguoiDung().replaceAll("\\D+", "")) + 1
                : 1;

        // Trả về mã hoàn chỉnh với số thứ tự có 3 chữ số
        return baseMaNguoiDung + String.format("%03d", nextNumber);
    }


    @Transactional
    public KhachHangDTO addKhachHang(KhachHangDTO khachHangDTO) {
        validateUniqueEmailAndPhone(khachHangDTO.getEmail(), khachHangDTO.getSoDienThoai(), null);

        String rawPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // Convert avatar URL back to filename if needed (for storage)
        if (khachHangDTO.getAvatar() != null) {
            khachHangDTO.setAvatar(urlService.extractFilenameFromUrl(khachHangDTO.getAvatar()));
        }

        // Use mapper to convert DTO to entity
        NguoiDung nguoiDung = nguoiDungMapper.toEntityFromKhachHang(khachHangDTO);

        // Set fields that mapper ignores
        nguoiDung.setMaNguoiDung(generateMaNguoiDung(khachHangDTO.getHoTen(), VaiTro.CUSTOMER));
        nguoiDung.setVaiTro(VaiTro.CUSTOMER);
        nguoiDung.setMatKhau(passwordEncoder.encode(rawPassword));

        // Ensure default status if not provided
        if (nguoiDung.getTrangThai() == null) {
            nguoiDung.setTrangThai(TrangThaiNguoiDung.HOAT_DONG);
        }

        NguoiDung savedNguoiDung = nguoiDungRepository.save(nguoiDung);

        // Handle addresses separately
        if (khachHangDTO.getDiaChis() != null && !khachHangDTO.getDiaChis().isEmpty()) {
            List<DiaChi> diaChis = khachHangDTO.getDiaChis().stream()
                    .map(diaChiDto -> {
                        DiaChi diaChi = diaChiMapper.toEntity(diaChiDto);
                        diaChi.setNguoiDung(savedNguoiDung);
                        return diaChi;
                    })
                    .collect(Collectors.toList());
            diaChiRepository.saveAllAndFlush(diaChis);
        }

        // Create audit entry for user creation
        String newValues = createAuditValues(savedNguoiDung);
        NguoiDungAuditHistory auditEntry = NguoiDungAuditHistory.createEntry(
            savedNguoiDung.getId(),
            newValues,
            savedNguoiDung.getNguoiTao(),
            "Tạo khách hàng mới"
        );
        auditHistoryRepository.save(auditEntry);

//        emailService.sendPasswordEmail(nguoiDung.getEmail(), rawPassword);

        KhachHangDTO result = nguoiDungMapper.toKhachHangDto(savedNguoiDung);
        // Convert avatar filename to full URL for response
        result.setAvatar(urlService.ensureAvatarUrl(result.getAvatar()));
        return result;
    }


    @Transactional
    public NhanVienDTO addNhanVien(NhanVienDTO nhanVienDTO) {
        validateUniqueEmailAndPhone(nhanVienDTO.getEmail(), nhanVienDTO.getSoDienThoai(), null);

        String rawPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // Convert avatar URL back to filename if needed (for storage)
        if (nhanVienDTO.getAvatar() != null) {
            nhanVienDTO.setAvatar(urlService.extractFilenameFromUrl(nhanVienDTO.getAvatar()));
        }

        // Use mapper to convert DTO to entity
        NguoiDung nguoiDung = nguoiDungMapper.toEntityFromNhanVien(nhanVienDTO);

        // Set fields that mapper ignores
        nguoiDung.setMaNguoiDung(generateMaNguoiDung(nhanVienDTO.getHoTen(),
                nhanVienDTO.getVaiTro() != null ? nhanVienDTO.getVaiTro() : VaiTro.STAFF));
        nguoiDung.setMatKhau(passwordEncoder.encode(rawPassword));

        // Ensure default status if not provided
        if (nguoiDung.getTrangThai() == null) {
            nguoiDung.setTrangThai(TrangThaiNguoiDung.HOAT_DONG);
        }

        NguoiDung savedNguoiDung = nguoiDungRepository.save(nguoiDung);

        // Handle addresses separately
        if (nhanVienDTO.getDiaChis() != null && !nhanVienDTO.getDiaChis().isEmpty()) {
            List<DiaChi> diaChis = nhanVienDTO.getDiaChis().stream()
                    .map(diaChiDto -> {
                        DiaChi diaChi = diaChiMapper.toEntity(diaChiDto);
                        diaChi.setNguoiDung(savedNguoiDung);
                        return diaChi;
                    })
                    .collect(Collectors.toList());
            diaChiRepository.saveAllAndFlush(diaChis);
        }

        // Create audit entry for staff creation
        String newValues = createAuditValues(savedNguoiDung);
        NguoiDungAuditHistory auditEntry = NguoiDungAuditHistory.createEntry(
            savedNguoiDung.getId(),
            newValues,
            savedNguoiDung.getNguoiTao(),
            "Tạo nhân viên mới"
        );
        auditHistoryRepository.save(auditEntry);

//        emailService.sendPasswordEmail(nguoiDung.getEmail(), rawPassword);

        NhanVienDTO result = nguoiDungMapper.toNhanVienDto(savedNguoiDung);
        // Convert avatar filename to full URL for response
        result.setAvatar(urlService.ensureAvatarUrl(result.getAvatar()));
        return result;
    }

    private void updateAddresses(List<DiaChiDto> newAddressDtos, NguoiDung existingNguoiDung) {
        // Lấy danh sách địa chỉ hiện tại
        List<DiaChi> existingAddresses = diaChiRepository.findAllByNguoiDungId(existingNguoiDung.getId());

        // Xóa các địa chỉ không còn tồn tại trong danh sách mới
        List<DiaChi> addressesToDelete = existingAddresses.stream()
                .filter(existingAddr -> newAddressDtos.stream()
                        .noneMatch(newAddrDto ->
                                newAddrDto.getId() != null && newAddrDto.getId().equals(existingAddr.getId())))
                .collect(Collectors.toList());

        if (!addressesToDelete.isEmpty()) {
            diaChiRepository.deleteAll(addressesToDelete);
        }

        // Cập nhật hoặc thêm mới địa chỉ
        for (DiaChiDto newAddressDto : newAddressDtos) {
            if (newAddressDto.getId() != null) {
                // Cập nhật địa chỉ hiện có
                DiaChi existingAddress = existingAddresses.stream()
                        .filter(addr -> addr.getId().equals(newAddressDto.getId()))
                        .findFirst()
                        .orElseThrow(() -> new AddressNotFoundException(newAddressDto.getId()));

                existingAddress.setDuong(newAddressDto.getDuong());
                existingAddress.setPhuongXa(newAddressDto.getPhuongXa());
                existingAddress.setQuanHuyen(newAddressDto.getQuanHuyen());
                existingAddress.setTinhThanh(newAddressDto.getTinhThanh());
                existingAddress.setLoaiDiaChi(newAddressDto.getLoaiDiaChi());
                existingAddress.setLaMacDinh(newAddressDto.getLaMacDinh());

                diaChiRepository.save(existingAddress);
            } else {
                // Thêm địa chỉ mới
                DiaChi newAddress = diaChiMapper.toEntity(newAddressDto);
                newAddress.setNguoiDung(existingNguoiDung);
                diaChiRepository.save(newAddress);
            }
        }

        // Đảm bảo chỉ có một địa chỉ mặc định
        ensureSingleDefaultAddress(existingNguoiDung.getId());
    }

    private void ensureSingleDefaultAddress(Long nguoiDungId) {
        List<DiaChi> addresses = diaChiRepository.findAllByNguoiDungId(nguoiDungId);
        List<DiaChi> defaultAddresses = addresses.stream()
                .filter(DiaChi::getLaMacDinh)
                .collect(Collectors.toList());

        if (defaultAddresses.size() > 1) {
            // Chỉ giữ lại địa chỉ đầu tiên làm mặc định
            for (int i = 1; i < defaultAddresses.size(); i++) {
                defaultAddresses.get(i).setLaMacDinh(false);
                diaChiRepository.save(defaultAddresses.get(i));
            }
        } else if (defaultAddresses.isEmpty() && !addresses.isEmpty()) {
            // Nếu không có địa chỉ mặc định, đặt địa chỉ đầu tiên làm mặc định
            addresses.get(0).setLaMacDinh(true);
            diaChiRepository.save(addresses.get(0));
        }
    }



    @Transactional
    public KhachHangDTO updateKhachHang(Long id, KhachHangDTO khachHangDTO) {
        NguoiDung existingNguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.customer(id));

        // Store old values for audit
        String oldValues = createAuditValues(existingNguoiDung);

        // Validate email and phone uniqueness if changed
        if (!existingNguoiDung.getEmail().equals(khachHangDTO.getEmail()) ||
                !Objects.equals(existingNguoiDung.getSoDienThoai(), khachHangDTO.getSoDienThoai())) {
            validateUniqueEmailAndPhone(khachHangDTO.getEmail(), khachHangDTO.getSoDienThoai(), id);
        }

        // Convert avatar URL back to filename if needed (for storage)
        if (khachHangDTO.getAvatar() != null) {
            khachHangDTO.setAvatar(urlService.extractFilenameFromUrl(khachHangDTO.getAvatar()));
        }

        // Use mapper to update entity from DTO
        nguoiDungMapper.updateEntityFromKhachHang(khachHangDTO, existingNguoiDung);

        NguoiDung savedNguoiDung = nguoiDungRepository.save(existingNguoiDung);

        // Handle addresses separately
        if (khachHangDTO.getDiaChis() != null) {
            updateAddresses(khachHangDTO.getDiaChis(), savedNguoiDung);
        }

        // Create audit entry for update
        String newValues = createAuditValues(savedNguoiDung);
        NguoiDungAuditHistory auditEntry = NguoiDungAuditHistory.updateEntry(
            savedNguoiDung.getId(),
            oldValues,
            newValues,
            savedNguoiDung.getNguoiCapNhat(),
            "Cập nhật thông tin khách hàng"
        );
        auditHistoryRepository.save(auditEntry);

        KhachHangDTO result = nguoiDungMapper.toKhachHangDto(savedNguoiDung);
        // Convert avatar filename to full URL for response
        result.setAvatar(urlService.ensureAvatarUrl(result.getAvatar()));
        return result;
    }

    @Transactional
    public NhanVienDTO updateNhanVien(Long id, NhanVienDTO nhanVienDTO) {
        NguoiDung existingNguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.staff(id));

        // Store old values for audit
        String oldValues = createAuditValues(existingNguoiDung);

        // Validate email and phone uniqueness if changed
        if (!existingNguoiDung.getEmail().equals(nhanVienDTO.getEmail()) ||
                !Objects.equals(existingNguoiDung.getSoDienThoai(), nhanVienDTO.getSoDienThoai())) {
            validateUniqueEmailAndPhone(nhanVienDTO.getEmail(), nhanVienDTO.getSoDienThoai(), id);
        }

        // Convert avatar URL back to filename if needed (for storage)
        if (nhanVienDTO.getAvatar() != null) {
            nhanVienDTO.setAvatar(urlService.extractFilenameFromUrl(nhanVienDTO.getAvatar()));
        }

        // Use mapper to update entity from DTO
        nguoiDungMapper.updateEntityFromNhanVien(nhanVienDTO, existingNguoiDung);

        NguoiDung savedNguoiDung = nguoiDungRepository.save(existingNguoiDung);

        // Handle addresses separately
        if (nhanVienDTO.getDiaChis() != null) {
            updateAddresses(nhanVienDTO.getDiaChis(), savedNguoiDung);
        }

        // Create audit entry for update
        String newValues = createAuditValues(savedNguoiDung);
        NguoiDungAuditHistory auditEntry = NguoiDungAuditHistory.updateEntry(
            savedNguoiDung.getId(),
            oldValues,
            newValues,
            savedNguoiDung.getNguoiCapNhat(),
            "Cập nhật thông tin nhân viên"
        );
        auditHistoryRepository.save(auditEntry);

        NhanVienDTO result = nguoiDungMapper.toNhanVienDto(savedNguoiDung);
        // Convert avatar filename to full URL for response
        result.setAvatar(urlService.ensureAvatarUrl(result.getAvatar()));
        return result;
    }

    @Transactional
    public void deleteKhachHang(Long id) {
        NguoiDung existingNguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.customer(id));

        // Store old values for audit
        String oldValues = createAuditValues(existingNguoiDung);

        existingNguoiDung.deactivate();
        NguoiDung savedNguoiDung = nguoiDungRepository.save(existingNguoiDung);

        // Create audit entry for deactivation
        NguoiDungAuditHistory auditEntry = NguoiDungAuditHistory.deleteEntry(
            savedNguoiDung.getId(),
            oldValues,
            savedNguoiDung.getNguoiCapNhat(),
            "Vô hiệu hóa khách hàng"
        );
        auditHistoryRepository.save(auditEntry);
    }

    @Transactional
    public void deleteNhanVien(Long id) {
        NguoiDung existingNguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.staff(id));

        // Store old values for audit
        String oldValues = createAuditValues(existingNguoiDung);

        existingNguoiDung.deactivate();
        NguoiDung savedNguoiDung = nguoiDungRepository.save(existingNguoiDung);

        // Create audit entry for deactivation
        NguoiDungAuditHistory auditEntry = NguoiDungAuditHistory.deleteEntry(
            savedNguoiDung.getId(),
            oldValues,
            savedNguoiDung.getNguoiCapNhat(),
            "Vô hiệu hóa nhân viên"
        );
        auditHistoryRepository.save(auditEntry);
    }

    @Transactional
    public void restoreKhachHang(Long id) {
        NguoiDung existingNguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.customer(id));

        existingNguoiDung.activate();
        NguoiDung savedNguoiDung = nguoiDungRepository.save(existingNguoiDung);

        // Create audit entry for restoration
        NguoiDungAuditHistory auditEntry = NguoiDungAuditHistory.statusChangeEntry(
            savedNguoiDung.getId(),
            "KHONG_HOAT_DONG",
            savedNguoiDung.getTrangThai().toString(),
            savedNguoiDung.getNguoiCapNhat(),
            "Khôi phục khách hàng"
        );
        auditHistoryRepository.save(auditEntry);
    }

    @Transactional
    public void restoreNhanVien(Long id) {
        NguoiDung existingNguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.staff(id));

        existingNguoiDung.activate();
        NguoiDung savedNguoiDung = nguoiDungRepository.save(existingNguoiDung);

        // Create audit entry for restoration
        NguoiDungAuditHistory auditEntry = NguoiDungAuditHistory.statusChangeEntry(
            savedNguoiDung.getId(),
            "KHONG_HOAT_DONG",
            savedNguoiDung.getTrangThai().toString(),
            savedNguoiDung.getNguoiCapNhat(),
            "Khôi phục nhân viên"
        );
        auditHistoryRepository.save(auditEntry);
    }

    // Utility methods for frontend validation
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return nguoiDungRepository.findByEmail(email).isEmpty();
    }

    /**
     * Find user by ID returning Optional for voucher validation
     */
    @Transactional(readOnly = true)
    public Optional<NguoiDung> findByIdOptional(Long id) {
        return nguoiDungRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean isPhoneAvailable(String phone) {
        return nguoiDungRepository.findBySoDienThoai(phone).isEmpty();
    }

    @Transactional(readOnly = true)
    public boolean isCccdAvailable(String cccd) {
        return nguoiDungRepository.findByCccd(cccd).isEmpty();
    }

    /**
     * Create JSON representation of entity for audit trail
     */
    private String createAuditValues(NguoiDung entity) {
        return String.format(
            "{\"maNguoiDung\":\"%s\",\"hoTen\":\"%s\",\"gioiTinh\":\"%s\",\"ngaySinh\":\"%s\",\"email\":\"%s\",\"soDienThoai\":\"%s\",\"cccd\":\"%s\",\"avatar\":\"%s\",\"vaiTro\":\"%s\",\"trangThai\":\"%s\"}",
            entity.getMaNguoiDung() != null ? entity.getMaNguoiDung() : "",
            entity.getHoTen() != null ? entity.getHoTen() : "",
            entity.getGioiTinh() != null ? entity.getGioiTinh() : "",
            entity.getNgaySinh() != null ? entity.getNgaySinh().toString() : "",
            entity.getEmail() != null ? entity.getEmail() : "",
            entity.getSoDienThoai() != null ? entity.getSoDienThoai() : "",
            entity.getCccd() != null ? entity.getCccd() : "",
            entity.getAvatar() != null ? entity.getAvatar() : "",
            entity.getVaiTro() != null ? entity.getVaiTro() : "",
            entity.getTrangThai() != null ? entity.getTrangThai() : ""
        );
    }

    /**
     * Get audit history for a specific user
     */
    @Transactional(readOnly = true)
    public List<NguoiDungAuditHistory> getAuditHistory(Long nguoiDungId) {
        return auditHistoryRepository.findByNguoiDungIdOrderByThoiGianThayDoiDesc(nguoiDungId);
    }


}