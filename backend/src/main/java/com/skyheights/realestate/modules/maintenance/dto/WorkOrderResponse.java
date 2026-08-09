package com.skyheights.realestate.modules.maintenance.dto;

import com.skyheights.realestate.modules.maintenance.enums.WorkOrderStatus;
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
public class WorkOrderResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long ticketId;
    private String ticketTitle;
    private Long vendorId;
    private String vendorCompanyName;
    private Long bidId;
    private Long assignedByUserId;
    private String assignedByName;
    private WorkOrderStatus status;
    private LocalDate scheduledDate;
    private Instant startDate;
    private Instant completedDate;
    private String completionNotes;
    private Boolean checklistCompleted;
    private Boolean otpVerifiedForCompletion;
    private String invoiceS3Key;
    private String invoicePresignedUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
