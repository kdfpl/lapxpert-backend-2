package com.lapxpert.backend.common.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base audit entity providing basic audit fields for all entities.
 * Provides standard created/updated timestamps and user tracking.
 * 
 * For entities that need enhanced audit trails with detailed change tracking,
 * use dedicated AuditHistory tables instead of extending AdminAuditableEntity.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditableEntity {

    @CreatedDate
    @Column(name = "ngay_tao", updatable = false, nullable = false)
    private Instant ngayTao;

    @LastModifiedDate
    @Column(name = "ngay_cap_nhat", nullable = false)
    private Instant ngayCapNhat;

    @CreatedBy
    @Column(name = "nguoi_tao", length = 100)
    private String nguoiTao;

    @LastModifiedBy
    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;
}
