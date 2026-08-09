package com.skyheights.realestate.modules.organization.dto;

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
public class UserResponse {

    private Long id;
    private String uuid;
    private String email;
    private String fullName;
    private String phone;
    private Long orgId;
    private String orgSlug;
    private String status;
    private List<String> roles;
    private List<String> permissions;
    private int hierarchyLevel;
    private Instant createdAt;
    private Instant lastLogin;
}
