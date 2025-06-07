package com.lapxpert.backend.phieugiamgia.domain.repository;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Long> {

    /**
     * Find vouchers by campaign status
     */
    List<PhieuGiamGia> findByTrangThai(TrangThaiCampaign trangThai);

    /**
     * Find voucher by unique code
     */
    Optional<PhieuGiamGia> findByMaPhieuGiamGia(String maPhieuGiamGia);

    /**
     * Find active public vouchers (vouchers with no user assignments)
     * Uses LEFT JOIN to check for absence of user assignments
     */
    @Query("SELECT p FROM PhieuGiamGia p LEFT JOIN p.danhSachNguoiDung d WHERE d.id IS NULL AND p.trangThai = :status")
    List<PhieuGiamGia> findPublicActiveVouchers(@Param("status") TrangThaiCampaign status);

    /**
     * Find private vouchers (vouchers with user assignments)
     */
    @Query("SELECT DISTINCT p FROM PhieuGiamGia p JOIN p.danhSachNguoiDung d")
    List<PhieuGiamGia> findPrivateVouchers();

    /**
     * Find public vouchers (vouchers with no user assignments)
     */
    @Query("SELECT p FROM PhieuGiamGia p LEFT JOIN p.danhSachNguoiDung d WHERE d.id IS NULL")
    List<PhieuGiamGia> findPublicVouchers();

    /**
     * Find voucher for update operations
     * Note: For production, consider adding @Lock(LockModeType.PESSIMISTIC_WRITE) for concurrency control
     */
    @Query("SELECT p FROM PhieuGiamGia p WHERE p.maPhieuGiamGia = :code")
    Optional<PhieuGiamGia> findByMaPhieuGiamGiaForUpdate(@Param("code") String code);

    /**
     * Find active vouchers eligible for specific customer with optimized query
     * Includes both public vouchers and private vouchers assigned to the customer
     */
    @Query("""
        SELECT DISTINCT p FROM PhieuGiamGia p
        LEFT JOIN p.danhSachNguoiDung assignments
        WHERE p.trangThai = :status
        AND p.soLuongDaDung < p.soLuongBanDau
        AND (assignments.id IS NULL OR assignments.nguoiDung.id = :customerId)
        AND (assignments.id IS NULL OR assignments.daSuDung = false)
        """)
    List<PhieuGiamGia> findActiveVouchersForCustomer(@Param("customerId") Long customerId,
                                                    @Param("status") TrangThaiCampaign status);

    /**
     * Find vouchers needing status update with batch processing
     * Used by scheduler for efficient status updates
     */
    @Query("""
        SELECT p FROM PhieuGiamGia p
        WHERE (p.ngayBatDau <= :currentTime AND p.trangThai = :notStarted)
        OR (p.ngayKetThuc <= :currentTime AND p.trangThai = :active)
        """)
    List<PhieuGiamGia> findVouchersNeedingStatusUpdate(@Param("currentTime") Instant currentTime,
                                                      @Param("notStarted") TrangThaiCampaign notStarted,
                                                      @Param("active") TrangThaiCampaign active);

    /**
     * Count active vouchers for performance monitoring
     */
    @Query("SELECT COUNT(p) FROM PhieuGiamGia p WHERE p.trangThai = :status")
    long countByTrangThai(@Param("status") TrangThaiCampaign status);

    /**
     * Find vouchers expiring within specified days for notification
     */
    @Query("""
        SELECT p FROM PhieuGiamGia p
        WHERE p.trangThai = :status
        AND p.ngayKetThuc BETWEEN :startTime AND :endTime
        """)
    List<PhieuGiamGia> findVouchersExpiringBetween(@Param("status") TrangThaiCampaign status,
                                                   @Param("startTime") Instant startTime,
                                                   @Param("endTime") Instant endTime);

    /**
     * Find vouchers that are currently running
     */
    default List<PhieuGiamGia> findCurrentlyActiveVouchers() {
        return findByTrangThai(TrangThaiCampaign.DA_DIEN_RA);
    }

    /**
     * Find vouchers that need status update (expired campaigns)
     */
    @Query("SELECT p FROM PhieuGiamGia p WHERE p.ngayKetThuc < :currentTime AND p.trangThai != :expiredStatus")
    List<PhieuGiamGia> findExpiredCampaigns(@Param("currentTime") Instant currentTime,
                                           @Param("expiredStatus") TrangThaiCampaign expiredStatus);



    /**
     * Check if voucher code exists
     */
    boolean existsByMaPhieuGiamGia(String maPhieuGiamGia);

    /**
     * Search vouchers by code or description (case insensitive)
     */
    List<PhieuGiamGia> findByMaPhieuGiamGiaContainingIgnoreCaseOrMoTaContainingIgnoreCase(String code, String description);

    /**
     * Increment voucher usage count using direct update to avoid entity loading
     */
    @Modifying
    @Query("UPDATE PhieuGiamGia p SET p.soLuongDaDung = p.soLuongDaDung + 1 WHERE p.id = :voucherId")
    void incrementUsageCount(@Param("voucherId") Long voucherId);
}