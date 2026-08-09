package com.skyheights.realestate.modules.crm.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.crm.dto.LeadVisitCreateRequest;
import com.skyheights.realestate.modules.crm.dto.LeadVisitResponse;
import com.skyheights.realestate.modules.crm.dto.LeadVisitUpdateRequest;
import com.skyheights.realestate.modules.crm.enums.VisitStatus;
import com.skyheights.realestate.modules.crm.service.LeadVisitService;
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
@RequestMapping("/api/crm/visits")
@RequiredArgsConstructor
public class LeadVisitController {

    private final LeadVisitService visitService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE') or @permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<LeadVisitResponse>> createVisit(@CurrentUser UserPrincipal currentUser,
                                                                      @Valid @RequestBody LeadVisitCreateRequest request) {
        LeadVisitResponse resp = visitService.createVisit(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Visit scheduled"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE') or @permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<Page<LeadVisitResponse>>> getVisits(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) Long staffId,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<LeadVisitResponse> page = visitService.searchVisits(currentUser.getOrgId(), leadId, propertyId, status, staffId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Visits fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE') or @permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<LeadVisitResponse>> getVisit(@CurrentUser UserPrincipal currentUser,
                                                                   @PathVariable Long id) {
        LeadVisitResponse resp = visitService.getVisit(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Visit fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE') or @permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<LeadVisitResponse>> updateVisit(@CurrentUser UserPrincipal currentUser,
                                                                      @PathVariable Long id,
                                                                      @Valid @RequestBody LeadVisitUpdateRequest request) {
        LeadVisitResponse resp = visitService.updateVisit(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Visit updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEAD_VISIT_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteVisit(@CurrentUser UserPrincipal currentUser,
                                                         @PathVariable Long id) {
        visitService.deleteVisit(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Visit deleted"));
    }
}
