package com.skyheights.realestate.modules.tenant.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.tenant.enums.KycDocumentType;
import com.skyheights.realestate.modules.tenant.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "kyc_documents", indexes = {
    @Index(name = "idx_kyc_tenant", columnList = "tenant_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantProfile tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private KycDocumentType documentType;

    @Column(name = "document_number")
    private String documentNumber; // encrypted in service

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "front_s3_key", length = 500)
    private String frontS3Key;

    @Column(name = "back_s3_key", length = 500)
    private String backS3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 30)
    @Builder.Default
    private KycStatus verificationStatus = KycStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private AppUser verifiedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
