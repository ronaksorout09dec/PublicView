package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.KycDocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycCreateRequest {

    @NotNull(message = "Tenant ID required")
    private Long tenantId;

    @NotNull(message = "Document type required")
    private KycDocumentType documentType;

    private String documentNumber; // will be encrypted in service

    private String s3Key; // if already uploaded via /kyc/upload
    private String frontS3Key;
    private String backS3Key;

    private LocalDate expiryDate;
}
