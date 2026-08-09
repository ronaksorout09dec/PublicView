package com.skyheights.realestate.modules.crm.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.crm.dto.CrmLeadCreateRequest;
import com.skyheights.realestate.modules.crm.dto.CrmLeadResponse;
import com.skyheights.realestate.modules.crm.dto.CrmLeadUpdateRequest;
import com.skyheights.realestate.modules.crm.enums.LeadSource;
import com.skyheights.realestate.modules.crm.enums.LeadStatus;
import com.skyheights.realestate.modules.crm.service.CrmLeadService;
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
@RequestMapping("/api/crm/leads")
@RequiredArgsConstructor
public class CrmLeadController {

    private final CrmLeadService leadService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('LEAD_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<CrmLeadResponse>> createLead(@CurrentUser UserPrincipal currentUser,
                                                                   @Valid @RequestBody CrmLeadCreateRequest request) {
        CrmLeadResponse resp = leadService.createLead(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Lead created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<Page<CrmLeadResponse>>> getLeads(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String priority,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CrmLeadResponse> page = leadService.searchLeads(currentUser.getOrgId(), status, source, propertyId, assignedTo, search, priority, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Leads fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<CrmLeadResponse>> getLead(@CurrentUser UserPrincipal currentUser,
                                                                @PathVariable Long id) {
        CrmLeadResponse resp = leadService.getLead(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Lead fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<CrmLeadResponse>> updateLead(@CurrentUser UserPrincipal currentUser,
                                                                   @PathVariable Long id,
                                                                   @Valid @RequestBody CrmLeadUpdateRequest request) {
        CrmLeadResponse resp = leadService.updateLead(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Lead updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEAD_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteLead(@CurrentUser UserPrincipal currentUser,
                                                        @PathVariable Long id) {
        leadService.deleteLead(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Lead deleted"));
    }
}
