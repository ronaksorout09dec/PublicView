package com.skyheights.realestate.modules.tenant.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.tenant.dto.LeaseCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.LeaseResponse;
import com.skyheights.realestate.modules.tenant.dto.LeaseUpdateRequest;
import com.skyheights.realestate.modules.tenant.enums.LeaseStatus;
import com.skyheights.realestate.modules.tenant.service.LeaseService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<LeaseResponse>> createLease(@CurrentUser UserPrincipal currentUser,
                                                                 @Valid @RequestBody LeaseCreateRequest request) {
        LeaseResponse resp = leaseService.createLease(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Lease created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<Page<LeaseResponse>>> getLeases(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) LeaseStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "endDate", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<LeaseResponse> page = leaseService.searchLeases(currentUser.getOrgId(), propertyId, unitId, tenantId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Leases fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<LeaseResponse>> getLease(@CurrentUser UserPrincipal currentUser,
                                                              @PathVariable Long id) {
        LeaseResponse resp = leaseService.getLease(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Lease fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<LeaseResponse>> updateLease(@CurrentUser UserPrincipal currentUser,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody LeaseUpdateRequest request) {
        LeaseResponse resp = leaseService.updateLease(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Lease updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteLease(@CurrentUser UserPrincipal currentUser,
                                                        @PathVariable Long id) {
        leaseService.deleteLease(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Lease deleted"));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<LeaseResponse>> renewLease(@CurrentUser UserPrincipal currentUser,
                                                                @PathVariable Long id,
                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newStartDate,
                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newEndDate) {
        LeaseResponse resp = leaseService.renewLease(currentUser.getOrgId(), id, newStartDate, newEndDate);
        return ResponseEntity.ok(ApiResponse.success(resp, "Lease renewed"));
    }
}
