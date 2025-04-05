package com.lapxpert.backend.nguoidung.domain.service;

import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import com.lapxpert.backend.nguoidung.application.dto.KhachHangDTO;
import com.lapxpert.backend.nguoidung.application.dto.NhanVienDTO;
import com.lapxpert.backend.nguoidung.domain.repository.DiaChiRepository;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
@Service
public class NguoiDungService {
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private DiaChiRepository diaChiRepository;

    @Transactional
    public KhachHangDTO getKhachHang(Long id) {

        NguoiDung nguoiDung = nguoiDungRepository.findById(id).get();

        return new KhachHangDTO(
                nguoiDung.getId(),
                nguoiDung.getMaNguoiDung(),
                nguoiDung.getAvatar(),
                nguoiDung.getHoTen(),
                nguoiDung.getGioiTinh(),
                nguoiDung.getNgaySinh(),
                nguoiDung.getEmail(),
                nguoiDung.getSoDienThoai(),
                nguoiDung.getTrangThai(),
                diaChiRepository.findAllByNguoiDungId(nguoiDung.getId())
        );
    }

    @Transactional
    public NhanVienDTO getNhanVien(Long id) {

        NguoiDung nguoiDung = nguoiDungRepository.findById(id).get();

        return new NhanVienDTO(
                nguoiDung.getId(),
                nguoiDung.getMaNguoiDung(),
                nguoiDung.getAvatar(),
                nguoiDung.getHoTen(),
                nguoiDung.getGioiTinh(),
                nguoiDung.getNgaySinh(),
                nguoiDung.getEmail(),
                nguoiDung.getSoDienThoai(),
                nguoiDung.getCccd(),
                nguoiDung.getVaiTro(),
                nguoiDung.getTrangThai(),
                diaChiRepository.findAllByNguoiDungId(nguoiDung.getId())
        );
    }

    @Transactional
    public List<KhachHangDTO> getAllKhachHang() {
        return nguoiDungRepository.findByVaiTro(VaiTro.CUSTOMER)
                .stream()
                .map(nd -> new KhachHangDTO(
                        nd.getId(),
                        nd.getMaNguoiDung(),
                        nd.getAvatar(),
                        nd.getHoTen(),
                        nd.getGioiTinh(),
                        nd.getNgaySinh(),
                        nd.getEmail(),
                        nd.getSoDienThoai(),
                        nd.getTrangThai(),
                        diaChiRepository.findAllByNguoiDungId(nd.getId())
                ))
                .collect(Collectors.toList());
    }

    private void validateUniqueEmailAndPhone(String email, String phone, Long id) {
        Optional<NguoiDung> existingByEmail = nguoiDungRepository.findByEmail(email);
        Optional<NguoiDung> existingByPhone = nguoiDungRepository.findBySoDienThoai(phone);

        if (existingByEmail.isPresent() &&
                existingByPhone.isPresent() &&
                !existingByEmail.get().getId().equals(id) &&
                !existingByPhone.get().getId().equals(id))
        {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Email và số điện thoại đã tồn tại");
        }

        if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Email đã tồn tại");
        }

        if (existingByPhone.isPresent() && !existingByPhone.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Số điện thoại đã tồn tại");
        }


    }


    @Transactional
    public List<NhanVienDTO> getAllNhanVien() {
        return nguoiDungRepository.findByVaiTroIn(Arrays.asList(VaiTro.STAFF, VaiTro.ADMIN))
                .stream()
                .map(nd -> new NhanVienDTO(
                        nd.getId(),
                        nd.getMaNguoiDung(),
                        nd.getAvatar(),
                        nd.getHoTen(),
                        nd.getGioiTinh(),
                        nd.getNgaySinh(),
                        nd.getEmail(),
                        nd.getSoDienThoai(),
                        nd.getCccd(),
                        nd.getVaiTro(),
                        nd.getTrangThai(),
                        diaChiRepository.findAllByNguoiDungId(nd.getId())
                ))
                .collect(Collectors.toList());
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
                throw new IllegalArgumentException("Vai trò không hợp lệ");
        }

        // Tách họ tên thành mảng
        String[] parts = hoTen.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Họ tên phải có ít nhất 2 phần");
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

        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setMaNguoiDung(generateMaNguoiDung(khachHangDTO.getHoTen(), VaiTro.CUSTOMER));
        nguoiDung.setAvatar(khachHangDTO.getAvatar());
        nguoiDung.setHoTen(khachHangDTO.getHoTen());
        nguoiDung.setGioiTinh(khachHangDTO.getGioiTinh());
        nguoiDung.setNgaySinh(khachHangDTO.getNgaySinh());
        nguoiDung.setEmail(khachHangDTO.getEmail());
        nguoiDung.setSoDienThoai(khachHangDTO.getSoDienThoai());
        nguoiDung.setTrangThai(khachHangDTO.getTrangThai());
        nguoiDung.setVaiTro(VaiTro.CUSTOMER);
        nguoiDung.setMatKhau(UUID.randomUUID().toString().replace("-", "").substring(0, 12));

        nguoiDungRepository.save(nguoiDung);

        List<DiaChi> diaChis = khachHangDTO.getDiaChis().stream()
                .peek(diaChi -> diaChi.setNguoiDung(nguoiDung))
                .collect(Collectors.toList());

        diaChiRepository.saveAllAndFlush(khachHangDTO.getDiaChis());

        return new KhachHangDTO(
                nguoiDung.getId(),
                nguoiDung.getMaNguoiDung(),
                nguoiDung.getAvatar(),
                nguoiDung.getHoTen(),
                nguoiDung.getGioiTinh(),
                nguoiDung.getNgaySinh(),
                nguoiDung.getEmail(),
                nguoiDung.getSoDienThoai(),
                nguoiDung.getTrangThai(),
                diaChiRepository.findAllByNguoiDungId(nguoiDung.getId())
        );
    }

    @Transactional
    public NhanVienDTO addNhanVien(NhanVienDTO nhanVienDTO) {
        validateUniqueEmailAndPhone(nhanVienDTO.getEmail(), nhanVienDTO.getSoDienThoai(), null);

        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setMaNguoiDung(generateMaNguoiDung(nhanVienDTO.getHoTen(), VaiTro.STAFF));
        nguoiDung.setAvatar(nhanVienDTO.getAvatar());
        nguoiDung.setHoTen(nhanVienDTO.getHoTen());
        nguoiDung.setNgaySinh(nhanVienDTO.getNgaySinh());
        nguoiDung.setEmail(nhanVienDTO.getEmail());
        nguoiDung.setSoDienThoai(nhanVienDTO.getSoDienThoai());
        nguoiDung.setCccd(nhanVienDTO.getCccd());
        nguoiDung.setVaiTro(nhanVienDTO.getVaiTro());
        nguoiDung.setGioiTinh(nhanVienDTO.getGioiTinh());
        nguoiDung.setTrangThai(nhanVienDTO.getTrangThai());
        nguoiDung.setMatKhau(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        nguoiDungRepository.save(nguoiDung);

        List<DiaChi> diaChis = nhanVienDTO.getDiaChis().stream()
                .peek(diaChi -> diaChi.setNguoiDung(nguoiDung))
                .collect(Collectors.toList());

        diaChiRepository.saveAllAndFlush(nhanVienDTO.getDiaChis());

        return new NhanVienDTO(
                nguoiDung.getId(),
                nguoiDung.getMaNguoiDung(),
                nguoiDung.getAvatar(),
                nguoiDung.getHoTen(),
                nguoiDung.getGioiTinh(),
                nguoiDung.getNgaySinh(),
                nguoiDung.getEmail(),
                nguoiDung.getSoDienThoai(),
                nguoiDung.getCccd(),
                nguoiDung.getVaiTro(),
                nguoiDung.getTrangThai(),
                diaChiRepository.findAllByNguoiDungId(nguoiDung.getId())
        );
    }

    private void updateAddresses(List<DiaChi> newAddresses, NguoiDung existingNguoiDung) {
        // Lấy danh sách địa chỉ hiện tại
        List<DiaChi> existingAddresses = diaChiRepository.findAllByNguoiDungId(existingNguoiDung.getId());

        // Xóa các địa chỉ không còn tồn tại trong danh sách mới
        List<DiaChi> addressesToDelete = existingAddresses.stream()
                .filter(existingAddr -> newAddresses.stream()
                        .noneMatch(newAddr ->
                                newAddr.getId() != null && newAddr.getId().equals(existingAddr.getId())))
                .collect(Collectors.toList());

        if (!addressesToDelete.isEmpty()) {
            diaChiRepository.deleteAll(addressesToDelete);
        }

        // Cập nhật hoặc thêm mới địa chỉ
        for (DiaChi newAddress : newAddresses) {
            if (newAddress.getId() != null) {
                // Cập nhật địa chỉ hiện có
                DiaChi existingAddress = existingAddresses.stream()
                        .filter(addr -> addr.getId().equals(newAddress.getId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại"));

                existingAddress.setDuong(newAddress.getDuong());
                existingAddress.setPhuongXa(newAddress.getPhuongXa());
                existingAddress.setQuanHuyen(newAddress.getQuanHuyen());
                existingAddress.setTinhThanh(newAddress.getTinhThanh());
                existingAddress.setLoaiDiaChi(newAddress.getLoaiDiaChi());
                existingAddress.setLaMacDinh(newAddress.getLaMacDinh());

                diaChiRepository.save(existingAddress);
            } else {
                // Thêm địa chỉ mới
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
        Optional<NguoiDung> existingNguoiDungOpt = nguoiDungRepository.findById(id);

        if (existingNguoiDungOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }

        NguoiDung existingNguoiDung = existingNguoiDungOpt.get();

        if (!existingNguoiDung.getEmail().equals(khachHangDTO.getEmail()) ||
                !existingNguoiDung.getSoDienThoai().equals(khachHangDTO.getSoDienThoai())) {
            validateUniqueEmailAndPhone(khachHangDTO.getEmail(), khachHangDTO.getSoDienThoai(), id);
        }

        existingNguoiDung.setAvatar(khachHangDTO.getAvatar());
        existingNguoiDung.setHoTen(khachHangDTO.getHoTen());
        existingNguoiDung.setNgaySinh(khachHangDTO.getNgaySinh());
        existingNguoiDung.setEmail(khachHangDTO.getEmail());
        existingNguoiDung.setSoDienThoai(khachHangDTO.getSoDienThoai());
        existingNguoiDung.setGioiTinh(khachHangDTO.getGioiTinh());
        existingNguoiDung.setTrangThai(khachHangDTO.getTrangThai());



        nguoiDungRepository.save(existingNguoiDung);

        updateAddresses(khachHangDTO.getDiaChis(),existingNguoiDung);

        return new KhachHangDTO(
                existingNguoiDung.getId(),
                existingNguoiDung.getMaNguoiDung(),
                existingNguoiDung.getAvatar(),
                existingNguoiDung.getHoTen(),
                existingNguoiDung.getGioiTinh(),
                existingNguoiDung.getNgaySinh(),
                existingNguoiDung.getEmail(),
                existingNguoiDung.getSoDienThoai(),
                existingNguoiDung.getTrangThai(),
                diaChiRepository.findAllByNguoiDungId(existingNguoiDung.getId())
        );
    }

    @Transactional
    public NhanVienDTO updateNhanVien(Long id, NhanVienDTO nhanVienDTO) {
        Optional<NguoiDung> existingNguoiDungOpt = nguoiDungRepository.findById(id);

        if (existingNguoiDungOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }

        NguoiDung existingNguoiDung = existingNguoiDungOpt.get();

        if (!existingNguoiDung.getEmail().equals(nhanVienDTO.getEmail()) ||
                !existingNguoiDung.getSoDienThoai().equals(nhanVienDTO.getSoDienThoai())) {
            validateUniqueEmailAndPhone(nhanVienDTO.getEmail(), nhanVienDTO.getSoDienThoai(), id);
        }

        existingNguoiDung.setAvatar(nhanVienDTO.getAvatar());
        existingNguoiDung.setHoTen(nhanVienDTO.getHoTen());
        existingNguoiDung.setNgaySinh(nhanVienDTO.getNgaySinh());
        existingNguoiDung.setEmail(nhanVienDTO.getEmail());
        existingNguoiDung.setCccd(nhanVienDTO.getCccd());
        existingNguoiDung.setVaiTro(nhanVienDTO.getVaiTro());
        existingNguoiDung.setSoDienThoai(nhanVienDTO.getSoDienThoai());
        existingNguoiDung.setGioiTinh(nhanVienDTO.getGioiTinh());
        existingNguoiDung.setTrangThai(nhanVienDTO.getTrangThai());

        nguoiDungRepository.save(existingNguoiDung);

        updateAddresses(nhanVienDTO.getDiaChis(),existingNguoiDung);

        return new NhanVienDTO(
                existingNguoiDung.getId(),
                existingNguoiDung.getMaNguoiDung(),
                existingNguoiDung.getAvatar(),
                existingNguoiDung.getHoTen(),
                existingNguoiDung.getGioiTinh(),
                existingNguoiDung.getNgaySinh(),
                existingNguoiDung.getEmail(),
                existingNguoiDung.getSoDienThoai(),
                existingNguoiDung.getCccd(),
                existingNguoiDung.getVaiTro(),
                existingNguoiDung.getTrangThai(),
                diaChiRepository.findAllByNguoiDungId(existingNguoiDung.getId())
        );
    }

    @Transactional
    public void deleteKhachHang(Long id) {
        Optional<NguoiDung> existingNguoiDungOpt = nguoiDungRepository.findById(id);

        if (existingNguoiDungOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }

        NguoiDung existingNguoiDung = existingNguoiDungOpt.get();
        existingNguoiDung.setTrangThai(false);

        nguoiDungRepository.save(existingNguoiDung);
    }

    @Transactional
    public void deleteNhanVien(Long id) {
        Optional<NguoiDung> existingNguoiDungOpt = nguoiDungRepository.findById(id);

        if (existingNguoiDungOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }

        NguoiDung existingNguoiDung = existingNguoiDungOpt.get();
        existingNguoiDung.setTrangThai(false);

        nguoiDungRepository.save(existingNguoiDung);

    }

    @Transactional
    public void restoreKhachHang(Long id) {
        Optional<NguoiDung> existingNguoiDungOpt = nguoiDungRepository.findById(id);

        if (existingNguoiDungOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }

        NguoiDung existingNguoiDung = existingNguoiDungOpt.get();
        existingNguoiDung.setTrangThai(true);

        nguoiDungRepository.save(existingNguoiDung);
    }

    @Transactional
    public void restoreNhanVien(Long id) {
        Optional<NguoiDung> existingNguoiDungOpt = nguoiDungRepository.findById(id);

        if (existingNguoiDungOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }

        NguoiDung existingNguoiDung = existingNguoiDungOpt.get();
        existingNguoiDung.setTrangThai(true);

        nguoiDungRepository.save(existingNguoiDung);
    }
}