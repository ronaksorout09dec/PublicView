package com.skyheights.realestate.modules.financial.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.financial.dto.InvoiceCreateRequest;
import com.skyheights.realestate.modules.financial.dto.InvoiceResponse;
import com.skyheights.realestate.modules.financial.dto.InvoiceUpdateRequest;
import com.skyheights.realestate.modules.financial.enums.InvoiceStatus;
import com.skyheights.realestate.modules.financial.enums.InvoiceType;
import com.skyheights.realestate.modules.financial.service.InvoiceService;
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
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('INVOICE_MANAGE') or @permEval.hasHierarchy(60)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(@CurrentUser UserPrincipal currentUser,
                                                                      @Valid @RequestBody InvoiceCreateRequest request) {
        InvoiceResponse resp = invoiceService.createInvoice(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Invoice created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('INVOICE_VIEW') or @permEval.hasPermission('INVOICE_MANAGE')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoices(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long leaseId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) InvoiceType type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<InvoiceResponse> page = invoiceService.searchInvoices(currentUser.getOrgId(), propertyId, unitId, tenantId, leaseId, status, type, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Invoices fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('INVOICE_VIEW') or @permEval.hasPermission('INVOICE_MANAGE')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@CurrentUser UserPrincipal currentUser,
                                                                   @PathVariable Long id) {
        InvoiceResponse resp = invoiceService.getInvoice(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Invoice fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('INVOICE_MANAGE')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(@CurrentUser UserPrincipal currentUser,
                                                                      @PathVariable Long id,
                                                                      @Valid @RequestBody InvoiceUpdateRequest request) {
        InvoiceResponse resp = invoiceService.updateInvoice(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Invoice updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('INVOICE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@CurrentUser UserPrincipal currentUser,
                                                           @PathVariable Long id) {
        invoiceService.deleteInvoice(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Invoice deleted"));
    }

    @PostMapping("/{id}/late-fee")
    @PreAuthorize("@permEval.hasPermission('INVOICE_MANAGE')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> applyLateFee(@CurrentUser UserPrincipal currentUser,
                                                                     @PathVariable Long id) {
        InvoiceResponse resp = invoiceService.applyLateFee(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Late fee applied"));
    }

    @PostMapping("/auto-generate")
    @PreAuthorize("@permEval.hasPermission('INVOICE_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Integer>> autoGenerate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        LocalDate targetMonth = month != null ? month : LocalDate.now().withDayOfMonth(1);
        int count = invoiceService.autoGenerateRentInvoicesForMonth(targetMonth);
        return ResponseEntity.ok(ApiResponse.success(count, "Auto-generated " + count + " rent invoices for " + targetMonth.getMonth() + " " + targetMonth.getYear()));
    }
}
