package com.skyheights.realestate.modules.financial.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.financial.enums.DepositStatus;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "security_deposits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SecurityDeposit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "lease_id", nullable = false, unique = true)
    private Long leaseId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "total_deposited", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDeposited;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private DepositStatus status = DepositStatus.HELD;

    @Column(name = "held_in_account")
    private String heldInAccount;

    @OneToMany(mappedBy = "deposit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SecurityDepositLedger> ledgerEntries = new ArrayList<>();
}
