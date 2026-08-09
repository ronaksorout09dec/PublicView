package com.skyheights.realestate.modules.maintenance.dto;

import com.skyheights.realestate.modules.maintenance.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
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
public class TicketCreateRequest {

    @NotNull(message = "Property ID required")
    private Long propertyId;

    private Long unitId;
    private Long tenantId;

    @NotBlank(message = "Category required")
    private String category; // PLUMBING, ELECTRICAL etc

    private TicketPriority priority;

    @NotBlank(message = "Title required")
    private String title;

    @NotBlank(message = "Description required")
    private String description;

    // S3 keys already uploaded, or will upload via /tickets/{id}/media
    private List<String> mediaS3Keys;
    private List<String> mediaTypes; // IMAGE, VIDEO, DOCUMENT
}
