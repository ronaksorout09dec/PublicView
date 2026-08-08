package com.skyheights.realestate.modules.communication.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_templates", uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_template_code", columnNames = {"org_id","code"}),
    @UniqueConstraint(name = "uk_org_template_name", columnNames = {"org_id","name"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "body_whatsapp_template_id", length = 100)
    private String bodyWhatsappTemplateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", columnDefinition = "jsonb")
    private String variablesJson;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "locale", length = 20)
    @Builder.Default
    private String locale = "en";
}
