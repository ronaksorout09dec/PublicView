package com.skyheights.realestate.modules.communication.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.communication.dto.TemplateCreateRequest;
import com.skyheights.realestate.modules.communication.dto.TemplateResponse;
import com.skyheights.realestate.modules.communication.service.TemplateService;
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
@RequestMapping("/api/notification/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE') or @permEval.hasHierarchy(60)")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(@CurrentUser UserPrincipal currentUser,
                                                                        @Valid @RequestBody TemplateCreateRequest request) {
        TemplateResponse resp = templateService.createTemplate(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Template created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE') or @permEval.hasPermission('COMMUNICATION_SEND')")
    public ResponseEntity<ApiResponse<Page<TemplateResponse>>> getTemplates(@CurrentUser UserPrincipal currentUser,
                                                                            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TemplateResponse> page = templateService.getTemplates(currentUser.getOrgId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Templates fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(@CurrentUser UserPrincipal currentUser,
                                                                     @PathVariable Long id) {
        TemplateResponse resp = templateService.getTemplate(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Template fetched"));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<TemplateResponse>> getByCode(@CurrentUser UserPrincipal currentUser,
                                                                   @PathVariable String code) {
        TemplateResponse resp = templateService.getTemplateByCode(currentUser.getOrgId(), code);
        return ResponseEntity.ok(ApiResponse.success(resp, "Template fetched by code"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(@CurrentUser UserPrincipal currentUser,
                                                                        @PathVariable Long id,
                                                                        @Valid @RequestBody TemplateCreateRequest request) {
        TemplateResponse resp = templateService.updateTemplate(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Template updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@CurrentUser UserPrincipal currentUser,
                                                            @PathVariable Long id) {
        templateService.deleteTemplate(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted"));
    }
}
