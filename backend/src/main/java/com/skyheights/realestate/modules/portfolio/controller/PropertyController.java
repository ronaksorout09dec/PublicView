package com.skyheights.realestate.modules.portfolio.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.portfolio.dto.PropertyCreateRequest;
import com.skyheights.realestate.modules.portfolio.dto.PropertyResponse;
import com.skyheights.realestate.modules.portfolio.dto.PropertyUpdateRequest;
import com.skyheights.realestate.modules.portfolio.enums.PropertyStatus;
import com.skyheights.realestate.modules.portfolio.enums.PropertyType;
import com.skyheights.realestate.modules.portfolio.service.PropertyService;
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
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('PROPERTY_WRITE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(@CurrentUser UserPrincipal currentUser,
                                                                        @Valid @RequestBody PropertyCreateRequest request) {
        PropertyResponse resp = propertyService.createProperty(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Property created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<Page<PropertyResponse>>> getProperties(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PropertyType type,
            @RequestParam(required = false) PropertyStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PropertyResponse> page = propertyService.searchProperties(currentUser.getOrgId(), city, search, type, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Properties fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<PropertyResponse>> getProperty(@CurrentUser UserPrincipal currentUser,
                                                                     @PathVariable Long id) {
        PropertyResponse resp = propertyService.getProperty(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Property fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('PROPERTY_WRITE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(@CurrentUser UserPrincipal currentUser,
                                                                        @PathVariable Long id,
                                                                        @Valid @RequestBody PropertyUpdateRequest request) {
        PropertyResponse resp = propertyService.updateProperty(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Property updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('PROPERTY_DELETE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(@CurrentUser UserPrincipal currentUser,
                                                            @PathVariable Long id) {
        propertyService.deleteProperty(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Property deleted"));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("@permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<PropertyResponse>> getPropertyStats(@CurrentUser UserPrincipal currentUser,
                                                                          @PathVariable Long id) {
        PropertyResponse resp = propertyService.getPropertyStats(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Property stats fetched"));
    }
}
