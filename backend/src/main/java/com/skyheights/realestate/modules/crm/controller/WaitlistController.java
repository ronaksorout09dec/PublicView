package com.skyheights.realestate.modules.crm.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.crm.dto.WaitlistCreateRequest;
import com.skyheights.realestate.modules.crm.dto.WaitlistResponse;
import com.skyheights.realestate.modules.crm.enums.WaitlistStatus;
import com.skyheights.realestate.modules.crm.service.WaitlistService;
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
@RequestMapping("/api/crm/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE') or @permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<WaitlistResponse>> addToWaitlist(@CurrentUser UserPrincipal currentUser,
                                                                      @Valid @RequestBody WaitlistCreateRequest request) {
        WaitlistResponse resp = waitlistService.addToWaitlist(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Added to waitlist"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE') or @permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<Page<WaitlistResponse>>> getWaitlist(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) WaitlistStatus status,
            @PageableDefault(size = 20, sort = "position", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<WaitlistResponse> page = waitlistService.searchWaitlist(currentUser.getOrgId(), propertyId, unitType, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Waitlist fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE')")
    public ResponseEntity<ApiResponse<WaitlistResponse>> getEntry(@CurrentUser UserPrincipal currentUser,
                                                                 @PathVariable Long id) {
        WaitlistResponse resp = waitlistService.getEntry(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Waitlist entry fetched"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE')")
    public ResponseEntity<ApiResponse<WaitlistResponse>> updateStatus(@CurrentUser UserPrincipal currentUser,
                                                                      @PathVariable Long id,
                                                                      @RequestParam WaitlistStatus status) {
        WaitlistResponse resp = waitlistService.updateStatus(currentUser.getOrgId(), id, status);
        return ResponseEntity.ok(ApiResponse.success(resp, "Waitlist status updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> remove(@CurrentUser UserPrincipal currentUser,
                                                   @PathVariable Long id) {
        waitlistService.removeFromWaitlist(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Removed from waitlist"));
    }

    @GetMapping("/next")
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE')")
    public ResponseEntity<ApiResponse<WaitlistResponse>> getNextInLine(
            @RequestParam Long propertyId,
            @RequestParam String unitType) {
        WaitlistResponse resp = waitlistService.getNextInLineForProperty(propertyId, unitType);
        if (resp == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No waitlist entry found"));
        }
        return ResponseEntity.ok(ApiResponse.success(resp, "Next in line fetched"));
    }
}
