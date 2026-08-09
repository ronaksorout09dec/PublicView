package com.skyheights.realestate.modules.maintenance.dto;

import com.skyheights.realestate.modules.maintenance.enums.VendorSpecialization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorCreateRequest {

    private Long userId; // existing AppUser

    // Or create new user inline
    private String fullName;
    private String email;
    private String phone;
    private String password;

    @NotBlank(message = "Company name required")
    private String companyName;

    @NotNull(message = "Specialization required")
    private VendorSpecialization specialization;

    private Integer yearsExperience;
    private String bankAccount;
    private String bankIfsc;
}
