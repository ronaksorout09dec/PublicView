package com.skyheights.realestate.modules.crm.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.crm.enums.WaitlistStatus;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "waitlist_entries", indexes = {
    @Index(name = "idx_waitlist_property", columnList = "property_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WaitlistEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "unit_type", nullable = false, length = 50)
    private String unitType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private CrmLead lead;

    @Column(name = "position")
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @Column(name = "priority_score")
    @Builder.Default
    private Integer priorityScore = 0;

    @Column(name = "desired_move_in")
    private LocalDate desiredMoveIn;
}
