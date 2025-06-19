package com.lapxpert.backend.hoadon.repository;

import com.lapxpert.backend.hoadon.entity.HoaDonPhieuGiamGia;
import com.lapxpert.backend.hoadon.entity.HoaDonPhieuGiamGiaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface HoaDonPhieuGiamGiaRepository extends JpaRepository<HoaDonPhieuGiamGia, HoaDonPhieuGiamGiaId> {
    List<HoaDonPhieuGiamGia> findByHoaDonId(Long hoaDonId);

    /**
     * Insert voucher-order relationship using native SQL to avoid transient entity issues
     */
    @Modifying
    @Query(value = "INSERT INTO hoa_don_phieu_giam_gia (hoa_don_id, phieu_giam_gia_id, gia_tri_da_giam) VALUES (:hoaDonId, :phieuGiamGiaId, :giaTriDaGiam)", nativeQuery = true)
    void insertVoucherOrderRelationship(@Param("hoaDonId") Long hoaDonId,
                                       @Param("phieuGiamGiaId") Long phieuGiamGiaId,
                                       @Param("giaTriDaGiam") BigDecimal giaTriDaGiam);
}
