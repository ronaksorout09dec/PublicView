package com.skyheights.realestate.modules.iot.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.iot.enums.PinType;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "access_pins", indexes = {
    @Index(name = "idx_pins_device_active", columnList = "device_id, is_active"),
    @Index(name = "idx_pins_valid_to", columnList = "valid_to")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccessPin extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private SmartLockDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_for_user_id")
    private AppUser generatedForUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_for_type", nullable = false, length = 50)
    private PinType generatedForType;

    @Column(name = "pin_code_encrypted", nullable = false, length = 500)
    private String pinCodeEncrypted;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Column(name = "label")
    private String label;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(name = "max_uses")
    @Builder.Default
    private Integer maxUses = 1;

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdByUser;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;
}
