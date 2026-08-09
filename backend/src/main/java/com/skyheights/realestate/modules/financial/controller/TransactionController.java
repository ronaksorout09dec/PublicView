package com.skyheights.realestate.modules.financial.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.financial.dto.TransactionCreateRequest;
import com.skyheights.realestate.modules.financial.dto.TransactionResponse;
import com.skyheights.realestate.modules.financial.enums.TransactionCategory;
import com.skyheights.realestate.modules.financial.enums.TransactionType;
import com.skyheights.realestate.modules.financial.service.TransactionService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('TRANSACTION_MANAGE') or @permEval.hasHierarchy(60)")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(@CurrentUser UserPrincipal currentUser,
                                                                             @Valid @RequestBody TransactionCreateRequest request) {
        TransactionResponse resp = transactionService.createTransaction(currentUser.getOrgId(), currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Transaction created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('TRANSACTION_MANAGE') or @permEval.hasPermission('REPORT_VIEW')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionResponse> page = transactionService.searchTransactions(currentUser.getOrgId(), propertyId, type, category, start, end, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Transactions fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TRANSACTION_MANAGE') or @permEval.hasPermission('REPORT_VIEW')")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@CurrentUser UserPrincipal currentUser,
                                                                           @PathVariable Long id) {
        TransactionResponse resp = transactionService.getTransaction(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Transaction fetched"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TRANSACTION_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@CurrentUser UserPrincipal currentUser,
                                                              @PathVariable Long id) {
        transactionService.deleteTransaction(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction deleted"));
    }
}
