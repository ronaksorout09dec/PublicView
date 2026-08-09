package com.skyheights.realestate.modules.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantCreateRequest {

    // Option 1: Link to existing AppUser
    private Long userId;

    // Option 2: Create new user inline
    private String fullName;

    @Email(message = "Invalid email")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone")
    private String phone;

    private String password; // for new user, min 8 chars

    private Long propertyId;
    private Long unitId;

    private String tenancyType; // PRIMARY, CO_TENANT default PRIMARY

    private String employerName;
    private String occupation;

    private BigDecimal monthlyIncome;

    private String emergencyContactName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid emergency contact phone")
    private String emergencyContactPhone;

    private LocalDate moveInDate;
    private LocalDate expectedMoveOutDate;

    private String notes;
}
