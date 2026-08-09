package com.skyheights.realestate.modules.financial.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.financial.dto.*;
import com.skyheights.realestate.modules.financial.service.UtilityService;
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

import java.util.List;

@RestController
@RequestMapping("/api/utilities")
@RequiredArgsConstructor
public class UtilityController {

    private final UtilityService utilityService;

    // Types
    @PostMapping("/types")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<UtilityTypeResponse>> createType(@CurrentUser UserPrincipal currentUser,
                                                                       @Valid @RequestBody UtilityTypeCreateRequest request) {
        UtilityTypeResponse resp = utilityService.createUtilityType(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Utility type created"));
    }

    @GetMapping("/types")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE') or @permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<List<UtilityTypeResponse>>> getTypes(@CurrentUser UserPrincipal currentUser) {
        List<UtilityTypeResponse> list = utilityService.getUtilityTypes(currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(list, "Utility types fetched"));
    }

    // Meters
    @PostMapping("/meters")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE')")
    public ResponseEntity<ApiResponse<UtilityMeterResponse>> createMeter(@CurrentUser UserPrincipal currentUser,
                                                                         @Valid @RequestBody UtilityMeterCreateRequest request) {
        UtilityMeterResponse resp = utilityService.createMeter(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Meter created"));
    }

    @GetMapping("/meters/property/{propertyId}")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE') or @permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<List<UtilityMeterResponse>>> getMetersByProperty(@CurrentUser UserPrincipal currentUser,
                                                                                       @PathVariable Long propertyId) {
        List<UtilityMeterResponse> list = utilityService.getMetersByProperty(currentUser.getOrgId(), propertyId);
        return ResponseEntity.ok(ApiResponse.success(list, "Meters fetched"));
    }

    // Readings
    @PostMapping("/readings")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE')")
    public ResponseEntity<ApiResponse<UtilityReadingResponse>> createReading(@CurrentUser UserPrincipal currentUser,
                                                                             @Valid @RequestBody UtilityReadingCreateRequest request) {
        UtilityReadingResponse resp = utilityService.createReading(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Reading recorded"));
    }

    @GetMapping("/readings/meter/{meterId}")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE')")
    public ResponseEntity<ApiResponse<Page<UtilityReadingResponse>>> getReadings(@PathVariable Long meterId,
                                                                                @PageableDefault(size = 20, sort = "readingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UtilityReadingResponse> page = utilityService.getReadings(meterId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Readings fetched"));
    }

    // Bills
    @PostMapping("/bills")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE')")
    public ResponseEntity<ApiResponse<UtilityBillResponse>> createBill(@CurrentUser UserPrincipal currentUser,
                                                                       @Valid @RequestBody UtilityBillCreateRequest request) {
        UtilityBillResponse resp = utilityService.createBill(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Utility bill created and auto-split"));
    }

    @GetMapping("/bills")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE') or @permEval.hasPermission('INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<Page<UtilityBillResponse>>> getBills(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long utilityTypeId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "billingMonth", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UtilityBillResponse> page = utilityService.searchBills(currentUser.getOrgId(), propertyId, utilityTypeId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Utility bills fetched"));
    }

    @GetMapping("/bills/{id}")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE')")
    public ResponseEntity<ApiResponse<UtilityBillResponse>> getBill(@CurrentUser UserPrincipal currentUser,
                                                                    @PathVariable Long id) {
        UtilityBillResponse resp = utilityService.getBill(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Utility bill fetched"));
    }

    @PostMapping("/bills/{id}/split")
    @PreAuthorize("@permEval.hasPermission('UTILITY_MANAGE')")
    public ResponseEntity<ApiResponse<UtilityBillResponse>> autoSplitBill(@CurrentUser UserPrincipal currentUser,
                                                                          @PathVariable Long id) {
        UtilityBillResponse resp = utilityService.autoSplitBill(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Utility bill auto-split"));
    }
}
