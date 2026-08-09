package com.skyheights.realestate.modules.organization.dto;

import com.skyheights.realestate.modules.organization.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 20, message = "Password must be 8-20 chars")
    private String password;

    private String phone;

    // For first org creation - if orgSlug null, creates personal org?
    private String orgName;

    private String orgSlug;

    // Role to assign - only PROPERTY_MANAGER can register org, others via invitation
    private RoleName role = RoleName.PROPERTY_MANAGER;
}
