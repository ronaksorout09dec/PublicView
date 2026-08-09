package com.skyheights.realestate.modules.crm.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.crm.enums.LeadSource;
import com.skyheights.realestate.modules.crm.enums.LeadStatus;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crm_leads", indexes = {
    @Index(name = "idx_crm_leads_org", columnList = "org_id"),
    @Index(name = "idx_crm_leads_status", columnList = "status"),
    @Index(name = "idx_crm_leads_phone", columnList = "phone"),
    @Index(name = "idx_crm_leads_property", columnList = "property_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrmLead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "interested_unit_type", length = 50)
    private String interestedUnitType;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50)
    @Builder.Default
    private LeadSource source = LeadSource.WEBSITE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "configuration", length = 50)
    private String configuration;

    @Column(name = "timeline", length = 100)
    private String timeline;

    @Column(name = "purpose", length = 100)
    private String purpose;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_staff_id")
    private AppUser assignedTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "conversation_summary", columnDefinition = "TEXT")
    private String conversationSummary;

    @Column(name = "lost_reason")
    private String lostReason;

    @Column(name = "next_followup_at")
    private Instant nextFollowupAt;

    @Column(name = "ai_score", precision = 3, scale = 2)
    private BigDecimal aiScore;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LeadVisit> visits = new ArrayList<>();
}
