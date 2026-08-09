package com.skyheights.realestate.modules.maintenance.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.maintenance.enums.VendorSpecialization;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vendor_profiles", indexes = {
    @Index(name = "idx_vendor_org", columnList = "org_id"),
    @Index(name = "idx_vendor_spec", columnList = "specialization")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorProfile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialization", nullable = false, length = 100)
    private VendorSpecialization specialization;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_jobs_completed")
    @Builder.Default
    private Integer totalJobsCompleted = 0;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verification_docs_s3", length = 500)
    private String verificationDocsS3;

    @Column(name = "bank_account_encrypted", length = 500)
    private String bankAccountEncrypted;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "ACTIVE";
}
