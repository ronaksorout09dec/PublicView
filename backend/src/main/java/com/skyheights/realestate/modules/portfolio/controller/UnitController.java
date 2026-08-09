package com.skyheights.realestate.modules.portfolio.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.portfolio.dto.UnitCreateRequest;
import com.skyheights.realestate.modules.portfolio.dto.UnitResponse;
import com.skyheights.realestate.modules.portfolio.dto.UnitUpdateRequest;
import com.skyheights.realestate.modules.portfolio.enums.UnitStatus;
import com.skyheights.realestate.modules.portfolio.enums.UnitType;
import com.skyheights.realestate.modules.portfolio.service.UnitService;
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
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('UNIT_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@CurrentUser UserPrincipal currentUser,
                                                               @Valid @RequestBody UnitCreateRequest request) {
        UnitResponse resp = unitService.createUnit(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Unit created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<Page<UnitResponse>>> getUnits(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) UnitStatus status,
            @RequestParam(required = false) UnitType type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "unitNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<UnitResponse> page = unitService.searchUnits(currentUser.getOrgId(), propertyId, status, type, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Units fetched"));
    }

    @GetMapping("/vacant")
    @PreAuthorize("@permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<Page<UnitResponse>>> getVacantUnits(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UnitResponse> page = unitService.getVacantUnitsFiltered(currentUser.getOrgId(), propertyId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Vacant units fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<UnitResponse>> getUnit(@CurrentUser UserPrincipal currentUser,
                                                            @PathVariable Long id) {
        UnitResponse resp = unitService.getUnit(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Unit fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('UNIT_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnit(@CurrentUser UserPrincipal currentUser,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody UnitUpdateRequest request) {
        UnitResponse resp = unitService.updateUnit(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Unit updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('UNIT_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(@CurrentUser UserPrincipal currentUser,
                                                       @PathVariable Long id) {
        unitService.deleteUnit(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Unit deleted"));
    }
}
