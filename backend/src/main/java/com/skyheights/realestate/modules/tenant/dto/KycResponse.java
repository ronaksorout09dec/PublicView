package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.KycDocumentType;
import com.skyheights.realestate.modules.tenant.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long tenantId;
    private String tenantName;
    private KycDocumentType documentType;
    private String documentNumberMasked; // masked for security
    private String s3Key;
    private String frontS3Key;
    private String backS3Key;
    private String frontPresignedUrl;
    private String backPresignedUrl;
    private KycStatus verificationStatus;
    private Long verifiedByUserId;
    private String verifiedByName;
    private Instant verifiedAt;
    private String rejectionReason;
    private LocalDate expiryDate;
    private Instant createdAt;
}
