package com.skyheights.realestate.modules.communication.dto;

import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
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
public class TemplateCreateRequest {

    @NotBlank(message = "Template name required")
    private String name;

    @NotBlank(message = "Template code required - unique per org e.g RENT_REMINDER_3D")
    private String code;

    @NotNull(message = "Channel required")
    private NotificationChannel channel;

    private String subject; // templated with {{var}}

    @NotBlank(message = "Body required")
    private String body; // template with {{variables}}

    private String bodyWhatsappTemplateId; // external WhatsApp template ID

    private List<String> variables; // ["tenant_name","rent_amount"]

    private String category; // RENT, LEASE, MAINTENANCE, ANNOUNCEMENT, GENERAL
    private String locale; // en, hi, en_HI
    private Boolean isActive;
}
