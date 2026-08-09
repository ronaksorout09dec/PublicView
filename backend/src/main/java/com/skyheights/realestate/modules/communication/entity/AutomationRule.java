package com.skyheights.realestate.modules.communication.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.communication.enums.AutomationTrigger;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "automation_rules", uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_automation_code", columnNames = {"org_id","code"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AutomationRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false, length = 100)
    private AutomationTrigger triggerEvent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditionsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate template;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "cooldown_hours")
    @Builder.Default
    private Integer cooldownHours = 24;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "execution_count")
    @Builder.Default
    private Long executionCount = 0L;
}
