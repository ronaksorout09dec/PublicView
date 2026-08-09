package com.skyheights.realestate.modules.tenant.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.tenant.enums.EsignStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "esign_trackings", indexes = {
    @Index(name = "idx_esign_lease", columnList = "lease_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EsignTracking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    private LeaseAgreement lease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_user_id", nullable = false)
    private AppUser signer;

    @Column(name = "signer_role", nullable = false, length = 30)
    private String signerRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private EsignStatus status = EsignStatus.PENDING;

    @Column(name = "signature_order", nullable = false)
    @Builder.Default
    private Integer signatureOrder = 1;

    @Column(name = "signature_data_s3_key", length = 500)
    private String signatureDataS3Key;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "otp_verified")
    @Builder.Default
    private Boolean otpVerified = false;

    @Column(name = "otp_hash")
    private String otpHash;

    @Column(name = "expiry_at")
    private Instant expiryAt;
}
