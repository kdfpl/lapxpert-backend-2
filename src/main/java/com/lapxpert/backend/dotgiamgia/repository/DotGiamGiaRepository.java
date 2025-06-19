package com.lapxpert.backend.dotgiamgia.repository;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.dotgiamgia.entity.DotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Long> {



    /**
     * Find campaigns by status
     */
    List<DotGiamGia> findByTrangThai(TrangThaiCampaign trangThai);

    /**
     * Find campaign by unique code
     */
    Optional<DotGiamGia> findByMaDotGiamGia(String maDotGiamGia);

    /**
     * Find active campaigns (currently running and not cancelled)
     */
    @Query("SELECT d FROM DotGiamGia d WHERE d.trangThai = :status")
    List<DotGiamGia> findActiveCampaigns(@Param("status") TrangThaiCampaign status);

    /**
     * Find campaigns affecting specific product
     */
    @Query("SELECT d FROM DotGiamGia d JOIN d.sanPhamChiTiets s WHERE s.id = :productId AND d.trangThai = :status")
    List<DotGiamGia> findActiveCampaignsForProduct(@Param("productId") Long productId, @Param("status") TrangThaiCampaign status);

    /**
     * Find campaigns that need status update (expired campaigns)
     */
    @Query("SELECT d FROM DotGiamGia d WHERE d.ngayKetThuc < :currentTime AND d.trangThai != :expiredStatus")
    List<DotGiamGia> findExpiredCampaigns(@Param("currentTime") Instant currentTime, @Param("expiredStatus") TrangThaiCampaign expiredStatus);

    /**
     * Find campaigns that should be activated (start time has passed)
     */
    @Query("SELECT d FROM DotGiamGia d WHERE d.ngayBatDau <= :currentTime AND d.trangThai = :waitingStatus")
    List<DotGiamGia> findCampaignsToActivate(@Param("currentTime") Instant currentTime, @Param("waitingStatus") TrangThaiCampaign waitingStatus);

    /**
     * Find campaigns needing status updates (following PhieuGiamGia pattern)
     * This method finds campaigns that need automatic status updates based on current time
     */
    @Query("SELECT d FROM DotGiamGia d WHERE " +
           "(d.ngayBatDau <= :currentTime AND d.trangThai = :waitingStatus) OR " +
           "(d.ngayKetThuc < :currentTime AND d.trangThai = :activeStatus)")
    List<DotGiamGia> findCampaignsNeedingStatusUpdate(@Param("currentTime") Instant currentTime,
                                                     @Param("waitingStatus") TrangThaiCampaign waitingStatus,
                                                     @Param("activeStatus") TrangThaiCampaign activeStatus);

    /**
     * Count campaigns by status for reporting
     */
    long countByTrangThai(TrangThaiCampaign trangThai);

    /**
     * Check if campaign code exists
     */
    boolean existsByMaDotGiamGia(String maDotGiamGia);

    /**
     * Find currently active campaigns (convenience method)
     */
    default List<DotGiamGia> findCurrentlyActiveCampaigns() {
        return findActiveCampaigns(TrangThaiCampaign.DA_DIEN_RA);
    }
}