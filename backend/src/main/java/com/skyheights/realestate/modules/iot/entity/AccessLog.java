package com.skyheights.realestate.modules.iot.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.iot.enums.AccessType;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "access_logs", indexes = {
    @Index(name = "idx_access_device_time", columnList = "device_id, accessed_at"),
    @Index(name = "idx_access_user", columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccessLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private SmartLockDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id")
    private AccessPin pin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 30)
    private AccessType accessType;

    @Column(name = "accessed_at", nullable = false)
    @Builder.Default
    private Instant accessedAt = Instant.now();

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "location_latlon", length = 100)
    private String locationLatlon;

    @Column(name = "provider_event_id")
    private String providerEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;
}
