package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.EsignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsignResponse {

    private Long id;
    private String uuid;
    private Long leaseId;
    private String leaseNumber;
    private Long signerUserId;
    private String signerName;
    private String signerEmail;
    private String signerRole;
    private EsignStatus status;
    private Integer signatureOrder;
    private String signatureDataS3Key;
    private Instant signedAt;
    private String ipAddress;
    private Boolean otpVerified;
    private Instant expiryAt;
    private Instant createdAt;
}
