package com.skyheights.realestate.modules.organization.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.enums.OrgStatus;
import com.skyheights.realestate.modules.organization.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_org_slug", columnList = "slug"),
    @Index(name = "idx_org_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrgStatus status = OrgStatus.ACTIVE;

    @Column(name = "billing_email")
    private String billingEmail;

    @Column(name = "max_properties")
    @Builder.Default
    private Integer maxProperties = 5;

    @Column(name = "logo_s3_key", length = 500)
    private String logoS3Key;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    // Relations — One org has many users/properties (mapped in child)
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @Builder.Default
    private List<AppUser> users = new ArrayList<>();
}
