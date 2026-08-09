package com.skyheights.realestate.modules.maintenance.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.maintenance.dto.VendorCreateRequest;
import com.skyheights.realestate.modules.maintenance.dto.VendorResponse;
import com.skyheights.realestate.modules.maintenance.entity.VendorProfile;
import com.skyheights.realestate.modules.maintenance.enums.VendorSpecialization;
import com.skyheights.realestate.modules.maintenance.repository.VendorPayoutRepository;
import com.skyheights.realestate.modules.maintenance.repository.VendorProfileRepository;
import com.skyheights.realestate.modules.maintenance.enums.PayoutStatus;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Role;
import com.skyheights.realestate.modules.organization.entity.UserRole;
import com.skyheights.realestate.modules.organization.enums.RoleName;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.organization.repository.RoleRepository;
import com.skyheights.realestate.modules.organization.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {

    private final VendorProfileRepository vendorRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VendorPayoutRepository payoutRepository;

    @Transactional
    public VendorResponse createVendor(Long orgId, VendorCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        AppUser user;
        if (request.getUserId() != null) {
            user = appUserRepository.findByIdAndIsDeletedFalse(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Long userOrgId = user.getOrganization() != null ? user.getOrganization().getId() : user.getOrgId();
            if (!userOrgId.equals(orgId)) throw new RuntimeException("User must belong to same org");
        } else {
            if (request.getEmail() == null || request.getFullName() == null) {
                throw new RuntimeException("For new vendor user, fullName and email required");
            }
            if (appUserRepository.existsByEmailIgnoreCaseAndOrgId(request.getEmail(), orgId)) {
                throw new RuntimeException("Email already exists in org");
            }
            String rawPass = request.getPassword() != null ? request.getPassword() : "Vendor123!";
            user = AppUser.builder()
                    .email(request.getEmail().toLowerCase())
                    .passwordHash(passwordEncoder.encode(rawPass))
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .organization(org)
                    .build();
            user = appUserRepository.save(user);

            Role vendorRole = roleRepository.findByNameAndOrgIdIsNull(RoleName.VENDOR)
                    .orElseThrow(() -> new RuntimeException("VENDOR role not found"));
            UserRole ur = UserRole.builder().user(user).role(vendorRole).organization(org).build();
            userRoleRepository.save(ur);
            log.info("Created new AppUser {} for vendor org {}", user.getEmail(), orgId);
        }

        if (vendorRepository.findByUserIdAndIsDeletedFalse(user.getId()).isPresent()) {
            throw new RuntimeException("Vendor profile already exists for user " + user.getEmail());
        }

        VendorProfile vendor = VendorProfile.builder()
                .organization(org)
                .user(user)
                .companyName(request.getCompanyName())
                .specialization(request.getSpecialization())
                .yearsExperience(request.getYearsExperience())
                .rating(BigDecimal.ZERO)
                .totalJobsCompleted(0)
                .isVerified(false)
                .bankAccountEncrypted(request.getBankAccount()) // TODO encrypt
                .bankIfsc(request.getBankIfsc())
                .status("ACTIVE")
                .build();

        vendor = vendorRepository.save(vendor);
        log.info("Created vendor profile {} company {} org {}", vendor.getId(), vendor.getCompanyName(), orgId);
        return toResponse(vendor);
    }

    @Transactional(readOnly = true)
    public Page<VendorResponse> searchVendors(Long orgId, VendorSpecialization specialization, String search, Boolean isVerified, Pageable pageable) {
        Page<VendorProfile> page = vendorRepository.search(orgId, specialization, search, isVerified, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VendorResponse getVendor(Long orgId, Long id) {
        VendorProfile vendor = vendorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        return toResponse(vendor);
    }

    @Transactional
    public VendorResponse verifyVendor(Long orgId, Long id, boolean verified) {
        VendorProfile vendor = vendorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        vendor.setIsVerified(verified);
        vendor = vendorRepository.save(vendor);
        log.info("Vendor {} verification set to {} org {}", id, verified, orgId);
        return toResponse(vendor);
    }

    @Transactional
    public void deleteVendor(Long orgId, Long id) {
        VendorProfile vendor = vendorRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        vendor.setIsDeleted(true);
        vendorRepository.save(vendor);
        log.info("Soft deleted vendor {} org {}", id, orgId);
    }

    private VendorResponse toResponse(VendorProfile v) {
        BigDecimal totalPaid = payoutRepository.sumNetPayableByVendorAndStatus(v.getId(), PayoutStatus.PAID);
        BigDecimal pending = payoutRepository.sumNetPayableByVendorAndStatus(v.getId(), PayoutStatus.PENDING)
                .add(payoutRepository.sumNetPayableByVendorAndStatus(v.getId(), PayoutStatus.APPROVED));

        return VendorResponse.builder()
                .id(v.getId()).uuid(v.getUuid())
                .orgId(v.getOrganization() != null ? v.getOrganization().getId() : null)
                .userId(v.getUser() != null ? v.getUser().getId() : null)
                .fullName(v.getUser() != null ? v.getUser().getFullName() : null)
                .email(v.getUser() != null ? v.getUser().getEmail() : null)
                .phone(v.getUser() != null ? v.getUser().getPhone() : null)
                .companyName(v.getCompanyName())
                .specialization(v.getSpecialization())
                .yearsExperience(v.getYearsExperience())
                .rating(v.getRating())
                .totalJobsCompleted(v.getTotalJobsCompleted())
                .isVerified(v.getIsVerified())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .totalPaid(totalPaid)
                .pendingPayout(pending)
                .build();
    }
}
