package com.skyheights.realestate.modules.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String uuid;
    private String email;
    private String fullName;
    private Long orgId;
    private String orgSlug;
    private String orgName;
    private List<String> roles;
    private List<String> permissions;
    private int hierarchyLevel;
}
