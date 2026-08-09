package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionReportResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long leaseId;
    private String leaseNumber;
    private Long unitId;
    private String unitNumber;
    private Long tenantId;
    private String tenantName;
    private ReportType type;
    private Long templateId;
    private String templateName;
    private Long inspectedByUserId;
    private String inspectedByName;
    private Instant inspectedAt;
    private String overallCondition;
    private String notes;
    private String status;
    private String pdfS3Key;
    private String pdfPresignedUrl;
    private List<ConditionReportItemResponse> items;
    private BigDecimal totalEstimatedRepairCost;
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConditionReportItemResponse {
        private Long id;
        private String area;
        private String itemName;
        private String condition;
        private String description;
        private BigDecimal estimatedRepairCost;
        private List<ConditionPhotoResponse> photos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConditionPhotoResponse {
        private Long id;
        private String uuid;
        private String s3Key;
        private String presignedUrl;
        private String caption;
        private Instant takenAt;
    }
}
