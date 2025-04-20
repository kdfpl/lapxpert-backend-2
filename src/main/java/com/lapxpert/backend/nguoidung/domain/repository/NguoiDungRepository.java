package com.lapxpert.backend.nguoidung.domain.repository;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    List<NguoiDung> findByVaiTro(VaiTro vaiTro);

    Optional<NguoiDung> findTopByMaNguoiDungStartingWithOrderByMaNguoiDungDesc(String maNguoiDungPrefix);

    Optional<NguoiDung> findByEmail(String email);
    Optional<NguoiDung> findByEmailAndMatKhau(String email,String matKhau);

    Optional<NguoiDung> findBySoDienThoai(String phone);

    List<NguoiDung> findByVaiTroIn(List<VaiTro> asList);

}
