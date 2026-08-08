package com.skyheights.realestate.modules.iot.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.iot.enums.LockProvider;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "iot_webhook_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IoTWebhookConfig extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private LockProvider provider;

    @Column(name = "webhook_url", nullable = false, length = 500)
    private String webhookUrl;

    @Column(name = "secret_encrypted", length = 500)
    private String secretEncrypted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "events_subscribed", columnDefinition = "jsonb")
    private String eventsSubscribed;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_received_at")
    private Instant lastReceivedAt;

    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers_json", columnDefinition = "jsonb")
    private String headersJson;
}
