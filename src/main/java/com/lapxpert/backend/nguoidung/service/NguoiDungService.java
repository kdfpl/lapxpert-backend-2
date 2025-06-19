package com.lapxpert.backend.nguoidung.service;

import com.lapxpert.backend.common.service.BusinessEntityService;
import com.lapxpert.backend.common.service.UrlService;
import com.lapxpert.backend.common.service.WebSocketIntegrationService;
import com.lapxpert.backend.nguoidung.entity.DiaChi;
import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.entity.NguoiDungAuditHistory;
import com.lapxpert.backend.nguoidung.entity.VaiTro;
import com.lapxpert.backend.nguoidung.entity.TrangThaiNguoiDung;
import com.lapxpert.backend.nguoidung.dto.KhachHangDTO;
import com.lapxpert.backend.nguoidung.dto.NhanVienDTO;
import com.lapxpert.backend.nguoidung.dto.NguoiDungDto;
import com.lapxpert.backend.nguoidung.dto.DiaChiDto;
import com.lapxpert.backend.nguoidung.mapper.NguoiDungMapper;
import com.lapxpert.backend.nguoidung.mapper.DiaChiMapper;
import com.lapxpert.backend.nguoidung.exception.AddressNotFoundException;
import com.lapxpert.backend.nguoidung.exception.DuplicateUserException;
import com.lapxpert.backend.nguoidung.exception.UserNotFoundException;
import com.lapxpert.backend.nguoidung.exception.UserValidationException;
import com.lapxpert.backend.nguoidung.repository.DiaChiRepository;
import com.lapxpert.backend.nguoidung.repository.NguoiDungRepository;
import com.lapxpert.backend.nguoidung.repository.NguoiDungAuditHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NguoiDungService extends BusinessEntityService<NguoiDung, Long, NguoiDungDto, NguoiDungAuditHistory> {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private NguoiDungAuditHistoryRepository auditHistoryRepository;

    @Autowired
    private DiaChiRepository diaChiRepository;

    @Autowired
    private UrlService urlService;

    @Autowired
    private NguoiDungMapper nguoiDungMapper;

    @Autowired
    private DiaChiMapper diaChiMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private WebSocketIntegrationService webSocketIntegrationService;

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

    // ==================== BUSINESSENTITYSERVICE TEMPLATE METHODS ====================

    @Override
    protected NguoiDungRepository getRepository() {
        return nguoiDungRepository;
    }

    @Override
    protected NguoiDungAuditHistoryRepository getAuditRepository() {
        return auditHistoryRepository;
    }

    @Override
    protected ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    @Override
    protected String getCacheName() {
        return "nguoiDungCache";
    }

    @Override
    protected String getEntityName() {
        return "Người dùng";
    }

    @Override
    protected Long getEntityId(NguoiDung entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(NguoiDung entity, Long id) {
        entity.setId(id);
    }

    @Override
    protected NguoiDungDto toDto(NguoiDung entity) {
        return nguoiDungMapper.toDto(entity);
    }

    @Override
    protected NguoiDung toEntity(NguoiDungDto dto) {
        return nguoiDungMapper.toEntity(dto);
    }

    @Override
    protected void validateEntity(NguoiDung entity) {
        if (entity.getHoTen() == null || entity.getHoTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }
        if (entity.getEmail() == null || entity.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        if (entity.getVaiTro() == null) {
            throw new IllegalArgumentException("Vai trò không được để trống");
        }
        if (entity.getTrangThai() == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }
        // Validate CCCD for staff members
        if ((entity.getVaiTro() == VaiTro.STAFF || entity.getVaiTro() == VaiTro.ADMIN) &&
            (entity.getCccd() == null || entity.getCccd().trim().isEmpty())) {
            throw new IllegalArgumentException("CCCD không được để trống cho nhân viên");
        }
    }

    @Override
    protected void setSoftDeleteStatus(NguoiDung entity, boolean isActive) {
        if (isActive) {
            entity.setTrangThai(TrangThaiNguoiDung.HOAT_DONG);
        } else {
            entity.setTrangThai(TrangThaiNguoiDung.KHONG_HOAT_DONG);
        }
    }

    @Override
    protected String buildAuditJson(NguoiDung entity) {
        return createAuditValues(entity);
    }

    @Override
    protected NguoiDungAuditHistory createAuditEntry(Long entityId, String action, String oldValues, String newValues, String nguoiThucHien, String lyDo) {
        return NguoiDungAuditHistory.builder()
                .nguoiDungId(entityId)
                .hanhDong(action)
                .thoiGianThayDoi(java.time.Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    @Override
    protected void publishEntityCreatedEvent(NguoiDung entity) {
        try {
            // Send WebSocket notification for user creation
            webSocketIntegrationService.sendUserUpdate(
                entity.getId().toString(),
                "CREATED",
                toDto(entity)
            );

            log.debug("Published user created event for user ID: {}", entity.getId());
        } catch (Exception e) {
            log.error("Failed to publish user created event for ID {}: {}", entity.getId(), e.getMessage(), e);
        }
    }

    @Override
    protected void publishEntityUpdatedEvent(NguoiDung entity, NguoiDung oldEntity) {
        try {
            // Send WebSocket notification for user update
            webSocketIntegrationService.sendUserUpdate(
                entity.getId().toString(),
                "UPDATED",
                toDto(entity)
            );

            log.debug("Published user updated event for user ID: {}", entity.getId());
        } catch (Exception e) {
            log.error("Failed to publish user updated event for ID {}: {}", entity.getId(), e.getMessage(), e);
        }
    }

    @Override
    protected void publishEntityDeletedEvent(Long entityId) {
        try {
            // Send WebSocket notification for user deletion
            webSocketIntegrationService.sendUserUpdate(
                entityId.toString(),
                "DELETED",
                null
            );

            log.debug("Published user deleted event for user ID: {}", entityId);
        } catch (Exception e) {
            log.error("Failed to publish user deleted event for ID {}: {}", entityId, e.getMessage(), e);
        }
    }

    @Override
    protected void validateBusinessRules(NguoiDung entity) {
        // Validate email and phone uniqueness
        validateUniqueEmailAndPhone(entity.getEmail(), entity.getSoDienThoai(), null);

        // Validate role-specific rules
        if (entity.getVaiTro() == VaiTro.CUSTOMER) {
            // Customers don't need CCCD
            entity.setCccd(null);
        } else if (entity.getVaiTro() == VaiTro.STAFF || entity.getVaiTro() == VaiTro.ADMIN) {
            // Staff and admin need CCCD
            if (entity.getCccd() == null || entity.getCccd().trim().isEmpty()) {
                throw new IllegalArgumentException("CCCD không được để trống cho nhân viên");
            }
        }
    }

    @Override
    protected void validateBusinessRulesForUpdate(NguoiDung entity, NguoiDung existingEntity) {
        // Validate email and phone uniqueness if changed
        if (!existingEntity.getEmail().equals(entity.getEmail()) ||
            !Objects.equals(existingEntity.getSoDienThoai(), entity.getSoDienThoai())) {
            validateUniqueEmailAndPhone(entity.getEmail(), entity.getSoDienThoai(), entity.getId());
        }

        // Validate role changes
        if (!existingEntity.getVaiTro().equals(entity.getVaiTro())) {
            // Role changes require special validation
            log.info("Role change detected for user {}: {} -> {}",
                    entity.getId(), existingEntity.getVaiTro(), entity.getVaiTro());
        }
    }

    @Override
    protected NguoiDung cloneEntity(NguoiDung entity) {
        // Create a shallow clone for event publishing
        NguoiDung clone = new NguoiDung();
        clone.setId(entity.getId());
        clone.setMaNguoiDung(entity.getMaNguoiDung());
        clone.setHoTen(entity.getHoTen());
        clone.setGioiTinh(entity.getGioiTinh());
        clone.setNgaySinh(entity.getNgaySinh());
        clone.setEmail(entity.getEmail());
        clone.setSoDienThoai(entity.getSoDienThoai());
        clone.setCccd(entity.getCccd());
        clone.setAvatar(entity.getAvatar());
        clone.setVaiTro(entity.getVaiTro());
        clone.setTrangThai(entity.getTrangThai());
        clone.setNgayTao(entity.getNgayTao());
        clone.setNgayCapNhat(entity.getNgayCapNhat());
        clone.setNguoiTao(entity.getNguoiTao());
        clone.setNguoiCapNhat(entity.getNguoiCapNhat());
        return clone;
    }

    @Override
    protected List<NguoiDungAuditHistory> getAuditHistoryByEntityId(Long entityId) {
        return auditHistoryRepository.findByNguoiDungIdOrderByThoiGianThayDoiDesc(entityId);
    }

}