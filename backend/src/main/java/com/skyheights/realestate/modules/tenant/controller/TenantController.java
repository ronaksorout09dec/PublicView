package com.skyheights.realestate.modules.tenant.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.tenant.dto.TenantCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.TenantResponse;
import com.skyheights.realestate.modules.tenant.dto.TenantUpdateRequest;
import com.skyheights.realestate.modules.tenant.enums.TenantStatus;
import com.skyheights.realestate.modules.tenant.service.TenantService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('TENANT_WRITE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@CurrentUser UserPrincipal currentUser,
                                                                    @Valid @RequestBody TenantCreateRequest request) {
        TenantResponse resp = tenantService.createTenant(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Tenant onboarded"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> getTenants(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TenantResponse> page = tenantService.searchTenants(currentUser.getOrgId(), propertyId, unitId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Tenants fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@CurrentUser UserPrincipal currentUser,
                                                                 @PathVariable Long id) {
        TenantResponse resp = tenantService.getTenant(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Tenant fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TENANT_WRITE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(@CurrentUser UserPrincipal currentUser,
                                                                    @PathVariable Long id,
                                                                    @Valid @RequestBody TenantUpdateRequest request) {
        TenantResponse resp = tenantService.updateTenant(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Tenant updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TENANT_WRITE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@CurrentUser UserPrincipal currentUser,
                                                          @PathVariable Long id) {
        tenantService.deleteTenant(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant deleted"));
    }
}
