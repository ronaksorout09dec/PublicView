package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionReportCreateRequest {

    @NotNull(message = "Lease ID required")
    private Long leaseId;

    @NotNull(message = "Type required")
    private ReportType type;

    private Long templateId;

    private String overallCondition; // EXCELLENT, GOOD, FAIR, POOR, DAMAGED
    private String notes;

    private List<ConditionReportItemCreateRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConditionReportItemCreateRequest {
        @NotNull
        private String area;
        @NotNull
        private String itemName;
        @NotNull
        private String condition;
        private String description;
        private java.math.BigDecimal estimatedRepairCost;
        private List<String> photoS3Keys; // already uploaded via S3
        private List<String> photoCaptions;
    }
}
