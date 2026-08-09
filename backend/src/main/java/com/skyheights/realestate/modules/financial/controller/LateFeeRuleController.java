package com.skyheights.realestate.modules.financial.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.financial.dto.LateFeeRuleCreateRequest;
import com.skyheights.realestate.modules.financial.dto.LateFeeRuleResponse;
import com.skyheights.realestate.modules.financial.service.LateFeeRuleService;
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
@RequestMapping("/api/late-fee-rules")
@RequiredArgsConstructor
public class LateFeeRuleController {

    private final LateFeeRuleService ruleService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('LATE_FEE_MANAGE') or @permEval.hasHierarchy(60)")
    public ResponseEntity<ApiResponse<LateFeeRuleResponse>> createRule(@CurrentUser UserPrincipal currentUser,
                                                                       @Valid @RequestBody LateFeeRuleCreateRequest request) {
        LateFeeRuleResponse resp = ruleService.createRule(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Late fee rule created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('LATE_FEE_MANAGE') or @permEval.hasPermission('INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<List<LateFeeRuleResponse>>> getRules(@CurrentUser UserPrincipal currentUser) {
        List<LateFeeRuleResponse> list = ruleService.getRules(currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(list, "Late fee rules fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LATE_FEE_MANAGE')")
    public ResponseEntity<ApiResponse<LateFeeRuleResponse>> getRule(@CurrentUser UserPrincipal currentUser,
                                                                    @PathVariable Long id) {
        LateFeeRuleResponse resp = ruleService.getRule(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Late fee rule fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LATE_FEE_MANAGE')")
    public ResponseEntity<ApiResponse<LateFeeRuleResponse>> updateRule(@CurrentUser UserPrincipal currentUser,
                                                                       @PathVariable Long id,
                                                                       @Valid @RequestBody LateFeeRuleCreateRequest request) {
        LateFeeRuleResponse resp = ruleService.updateRule(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Late fee rule updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LATE_FEE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@CurrentUser UserPrincipal currentUser,
                                                        @PathVariable Long id) {
        ruleService.deleteRule(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Late fee rule deleted"));
    }
}
