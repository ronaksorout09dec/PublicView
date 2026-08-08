package com.skyheights.realestate.modules.iot.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.iot.enums.LockProvider;
import com.skyheights.realestate.modules.iot.enums.LockStatus;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "smart_lock_devices", indexes = {
    @Index(name = "idx_lock_property", columnList = "property_id"),
    @Index(name = "idx_lock_unit", columnList = "unit_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmartLockDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private LockProvider provider;

    @Column(name = "device_id_external", nullable = false, unique = true)
    private String deviceIdExternal;

    @Column(name = "mac_address", length = 100)
    private String macAddress;

    @Column(name = "api_key_encrypted", length = 500)
    private String apiKeyEncrypted;

    @Column(name = "api_secret_encrypted", length = 500)
    private String apiSecretEncrypted;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private LockStatus status = LockStatus.ACTIVE;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "signal_strength")
    private Integer signalStrength;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;
}
