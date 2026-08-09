package com.skyheights.realestate.modules.communication.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.communication.dto.AutomationLogResponse;
import com.skyheights.realestate.modules.communication.dto.AutomationRuleCreateRequest;
import com.skyheights.realestate.modules.communication.dto.AutomationRuleResponse;
import com.skyheights.realestate.modules.communication.service.AutomationService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    @PostMapping("/rules")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<AutomationRuleResponse>> createRule(@CurrentUser UserPrincipal currentUser,
                                                                          @Valid @RequestBody AutomationRuleCreateRequest request) {
        AutomationRuleResponse resp = automationService.createRule(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Automation rule created"));
    }

    @GetMapping("/rules")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<List<AutomationRuleResponse>>> getAllRules(@CurrentUser UserPrincipal currentUser) {
        List<AutomationRuleResponse> list = automationService.getAllRules(currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(list, "Automation rules fetched"));
    }

    @GetMapping("/rules/{id}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<AutomationRuleResponse>> getRule(@CurrentUser UserPrincipal currentUser,
                                                                       @PathVariable Long id) {
        AutomationRuleResponse resp = automationService.getRule(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Automation rule fetched"));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<AutomationRuleResponse>> updateRule(@CurrentUser UserPrincipal currentUser,
                                                                          @PathVariable Long id,
                                                                          @Valid @RequestBody AutomationRuleCreateRequest request) {
        AutomationRuleResponse resp = automationService.updateRule(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Automation rule updated"));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@CurrentUser UserPrincipal currentUser,
                                                        @PathVariable Long id) {
        automationService.deleteRule(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Automation rule deleted"));
    }

    @PostMapping("/rules/{id}/trigger")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<AutomationLogResponse>> triggerRule(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> context) {
        AutomationLogResponse resp = automationService.triggerRule(currentUser.getOrgId(), id, context);
        return ResponseEntity.ok(ApiResponse.success(resp, "Automation rule triggered (60/30-day expiry, rent due, etc)"));
    }

    @GetMapping("/logs")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<Page<AutomationLogResponse>>> getLogs(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long ruleId,
            @PageableDefault(size = 20, sort = "triggeredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AutomationLogResponse> page = automationService.getExecutionLogs(currentUser.getOrgId(), ruleId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Automation execution logs fetched"));
    }

    @PostMapping("/trigger-event/{triggerEvent}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> triggerEvent(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String triggerEvent,
            @RequestBody(required = false) Map<String, Object> context) {
        try {
            var event = com.skyheights.realestate.modules.communication.enums.AutomationTrigger.valueOf(triggerEvent.toUpperCase());
            automationService.handleTriggerEvent(currentUser.getOrgId(), event, context);
            return ResponseEntity.ok(ApiResponse.success(null, "Triggered automation for event " + triggerEvent));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid trigger event: " + triggerEvent + ". Valid: RENT_DUE_7D, RENT_DUE_3D, RENT_OVERDUE_1D, LEASE_EXPIRY_60D, LEASE_EXPIRY_30D, etc");
        }
    }
}
