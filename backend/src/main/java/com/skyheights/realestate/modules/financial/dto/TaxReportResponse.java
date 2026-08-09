package com.skyheights.realestate.modules.financial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxReportResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private String financialYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;
    private BigDecimal totalTds;
    private BigDecimal totalGst;
    private String reportJson;
    private String reportPdfS3Key;
    private String reportPdfPresignedUrl;
    private Instant generatedAt;
    private Long generatedByUserId;
    private String generatedByName;
    private Instant createdAt;
}
