package com.skyheights.realestate.modules.financial.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "utility_types", uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_utility_name", columnNames = {"org_id","name"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UtilityType extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "unit_label", length = 50)
    private String unitLabel;

    @Column(name = "default_rate", precision = 10, scale = 4)
    private BigDecimal defaultRate;
}
