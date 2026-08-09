package com.skyheights.realestate.modules.maintenance.dto;

import com.skyheights.realestate.modules.maintenance.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long workOrderId;
    private Long ticketId;
    private String ticketTitle;
    private Long vendorId;
    private String vendorCompanyName;
    private BigDecimal amount;
    private BigDecimal tdsDeducted;
    private BigDecimal netPayable;
    private PayoutStatus status;
    private String paymentMethod;
    private String utrNumber;
    private Long transactionId;
    private Instant paidAt;
    private Long paidByUserId;
    private String paidByName;
    private String notes;
    private String invoiceS3Key;
    private String invoicePresignedUrl;
    private Instant createdAt;
}
