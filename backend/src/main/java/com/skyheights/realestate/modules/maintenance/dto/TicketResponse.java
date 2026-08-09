package com.skyheights.realestate.modules.maintenance.dto;

import com.skyheights.realestate.modules.maintenance.enums.TicketPriority;
import com.skyheights.realestate.modules.maintenance.enums.TicketStatus;
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
public class TicketResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private Long unitId;
    private String unitNumber;
    private Long tenantId;
    private String tenantName;
    private Long raisedByUserId;
    private String raisedByName;
    private String category;
    private TicketPriority priority;
    private String title;
    private String description;
    private TicketStatus status;
    private Long assignedVendorId;
    private String assignedVendorName;
    private Long assignedBidId;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private Instant scheduledAt;
    private Instant completedAt;
    private String completionNotes;
    private Integer ratingByTenant;
    private String feedback;
    private Instant slaDueAt;
    private boolean slaBreached;

    private List<MediaResponse> media;
    private long bidsCount;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaResponse {
        private Long id;
        private String uuid;
        private String s3Key;
        private String presignedUrl;
        private String mediaType;
        private Long fileSize;
        private String caption;
        private Instant createdAt;
    }
}
