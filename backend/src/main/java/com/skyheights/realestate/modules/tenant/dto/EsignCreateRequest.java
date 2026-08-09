package com.skyheights.realestate.modules.tenant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsignCreateRequest {

    @NotNull(message = "Lease ID required")
    private Long leaseId;

    @NotNull(message = "Signer user ID required")
    private Long signerUserId;

    @NotNull(message = "Signer role required")
    private String signerRole; // TENANT, OWNER, MANAGER, WITNESS

    private Integer signatureOrder;
}
