package com.skyheights.realestate.modules.communication.dto;

import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private String name;
    private String code;
    private NotificationChannel channel;
    private String subject;
    private String body;
    private String bodyWhatsappTemplateId;
    private List<String> variables;
    private String category;
    private Boolean isActive;
    private String locale;
    private Instant createdAt;
}
