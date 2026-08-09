package com.skyheights.realestate.modules.tenant.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.tenant.dto.ChecklistTemplateCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.ChecklistTemplateResponse;
import com.skyheights.realestate.modules.tenant.enums.ReportType;
import com.skyheights.realestate.modules.tenant.service.ChecklistTemplateService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checklist-templates")
@RequiredArgsConstructor
public class ChecklistTemplateController {

    private final ChecklistTemplateService templateService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<ChecklistTemplateResponse>> createTemplate(@CurrentUser UserPrincipal currentUser,
                                                                                @Valid @RequestBody ChecklistTemplateCreateRequest request) {
        ChecklistTemplateResponse resp = templateService.createTemplate(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Template created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<List<ChecklistTemplateResponse>>> getTemplates(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) ReportType type) {
        List<ChecklistTemplateResponse> list = templateService.getTemplates(currentUser.getOrgId(), type);
        return ResponseEntity.ok(ApiResponse.success(list, "Templates fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE')")
    public ResponseEntity<ApiResponse<ChecklistTemplateResponse>> getTemplate(@CurrentUser UserPrincipal currentUser,
                                                                             @PathVariable Long id) {
        ChecklistTemplateResponse resp = templateService.getTemplate(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Template fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE')")
    public ResponseEntity<ApiResponse<ChecklistTemplateResponse>> updateTemplate(@CurrentUser UserPrincipal currentUser,
                                                                                @PathVariable Long id,
                                                                                @Valid @RequestBody ChecklistTemplateCreateRequest request) {
        ChecklistTemplateResponse resp = templateService.updateTemplate(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Template updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@CurrentUser UserPrincipal currentUser,
                                                           @PathVariable Long id) {
        templateService.deleteTemplate(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted"));
    }
}
