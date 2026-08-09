package com.skyheights.realestate.modules.organization.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository organizationRepository;

    @GetMapping
    @PreAuthorize("@permEval.hasHierarchy(80) or @permEval.hasPermission('ORG_MANAGE')")
    public ResponseEntity<ApiResponse<List<Organization>>> getAllOrganizations(@CurrentUser UserPrincipal currentUser) {
        List<Organization> orgs;
        if (currentUser.getMaxHierarchyLevel() >= 100) {
            orgs = organizationRepository.findAll();
        } else {
            orgs = organizationRepository.findById(currentUser.getOrgId()).map(List::of).orElse(List.of());
        }
        return ResponseEntity.ok(ApiResponse.success(orgs, "Organizations fetched"));
    }

    @GetMapping("/page")
    @PreAuthorize("@permEval.hasPermission('ORG_MANAGE')")
    public ResponseEntity<ApiResponse<Page<Organization>>> getOrganizationsPage(Pageable pageable) {
        Page<Organization> page = organizationRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Organizations page fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.isSameOrg(#id) or @permEval.hasPermission('ORG_MANAGE')")
    public ResponseEntity<ApiResponse<Organization>> getOrganization(@PathVariable Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return ResponseEntity.ok(ApiResponse.success(org, "Organization fetched"));
    }

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('ORG_MANAGE')")
    public ResponseEntity<ApiResponse<Organization>> createOrganization(@Valid @RequestBody CreateOrgRequest request) {
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Organization slug already exists");
        }
        Organization org = Organization.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .billingEmail(request.getBillingEmail())
                .build();
        org = organizationRepository.save(org);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(org, "Organization created"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrgRequest {
        @jakarta.validation.constraints.NotBlank
        private String name;
        @jakarta.validation.constraints.NotBlank
        private String slug;
        private String billingEmail;
    }
}
