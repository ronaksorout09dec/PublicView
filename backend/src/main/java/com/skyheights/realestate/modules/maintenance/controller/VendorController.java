package com.skyheights.realestate.modules.maintenance.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.maintenance.dto.VendorCreateRequest;
import com.skyheights.realestate.modules.maintenance.dto.VendorResponse;
import com.skyheights.realestate.modules.maintenance.enums.VendorSpecialization;
import com.skyheights.realestate.modules.maintenance.service.VendorService;
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
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('VENDOR_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<VendorResponse>> createVendor(@CurrentUser UserPrincipal currentUser,
                                                                    @Valid @RequestBody VendorCreateRequest request) {
        VendorResponse resp = vendorService.createVendor(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Vendor created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('VENDOR_MANAGE') or @permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<Page<VendorResponse>>> getVendors(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) VendorSpecialization specialization,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isVerified,
            @PageableDefault(size = 20, sort = "rating", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<VendorResponse> page = vendorService.searchVendors(currentUser.getOrgId(), specialization, search, isVerified, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Vendors fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('VENDOR_MANAGE') or @permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<VendorResponse>> getVendor(@CurrentUser UserPrincipal currentUser,
                                                                 @PathVariable Long id) {
        VendorResponse resp = vendorService.getVendor(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Vendor fetched"));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("@permEval.hasPermission('VENDOR_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<VendorResponse>> verifyVendor(@CurrentUser UserPrincipal currentUser,
                                                                    @PathVariable Long id,
                                                                    @RequestParam boolean verified) {
        VendorResponse resp = vendorService.verifyVendor(currentUser.getOrgId(), id, verified);
        return ResponseEntity.ok(ApiResponse.success(resp, verified ? "Vendor verified" : "Vendor unverified"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('VENDOR_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteVendor(@CurrentUser UserPrincipal currentUser,
                                                          @PathVariable Long id) {
        vendorService.deleteVendor(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vendor deleted"));
    }
}
