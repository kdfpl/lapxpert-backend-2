package com.lapxpert.backend.phieugiamgia.domain.entity;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Entity representing the assignment of vouchers to users
 * Tracks when users received vouchers and whether they've been used
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "phieu_giam_gia_nguoi_dung",
    indexes = {
        @Index(name = "idx_phieu_giam_gia_nguoi_dung_da_su_dung", columnList = "da_su_dung"),
        @Index(name = "idx_phieu_giam_gia_nguoi_dung_ngay_nhan", columnList = "ngay_nhan")
    })
@EntityListeners(AuditingEntityListener.class)
public class PhieuGiamGiaNguoiDung {
    @EmbeddedId
    private PhieuGiamGiaNguoiDungId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("phieuGiamGiaId")
    @JoinColumn(name = "phieu_giam_gia_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PhieuGiamGia phieuGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("nguoiDungId")
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private NguoiDung nguoiDung;

    /**
     * Date and time when the user received this voucher
     */
    @Column(name = "ngay_nhan")
    private OffsetDateTime ngayNhan;

    /**
     * Whether this voucher has been used by the user
     */
    @Column(name = "da_su_dung", nullable = false)
    @Builder.Default
    private Boolean daSuDung = false;

    /**
     * Timestamp when the voucher was used by this user
     */
    @Column(name = "ngay_su_dung")
    private Instant ngaySuDung;

    @CreatedDate
    @Column(name = "ngay_tao", updatable = false)
    private Instant ngayTao;

    @LastModifiedDate
    @Column(name = "ngay_cap_nhat")
    private Instant ngayCapNhat;

    /**
     * Mark this voucher as used
     */
    public void markAsUsed() {
        this.daSuDung = true;
        this.ngaySuDung = Instant.now();
    }

    /**
     * Check if this voucher is available for use
     * @return true if voucher can be used
     */
    public boolean isAvailableForUse() {
        return !daSuDung && phieuGiamGia != null && phieuGiamGia.isActive();
    }

    /**
     * Set the received date to now if not already set
     */
    @PrePersist
    public void setReceivedDateIfNull() {
        if (ngayNhan == null) {
            ngayNhan = OffsetDateTime.now();
        }
    }
}