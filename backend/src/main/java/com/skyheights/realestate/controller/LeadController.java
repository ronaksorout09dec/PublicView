package com.skyheights.realestate.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.dto.LeadRequest;
import com.skyheights.realestate.dto.LeadResponse;
import com.skyheights.realestate.service.LeadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public ResponseEntity<ApiResponse<LeadResponse>> createLead(@Valid @RequestBody LeadRequest request) {
        log.info("POST /api/leads - Creating lead: {}", request.getCustomerName());
        LeadResponse response = leadService.createLead(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Lead created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getAllLeads() {
        log.info("GET /api/leads - Fetching all leads");
        List<LeadResponse> leads = leadService.getAllLeads();
        return ResponseEntity.ok(ApiResponse.success(leads, "Leads fetched successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponse>> getLeadById(@PathVariable Long id) {
        log.info("GET /api/leads/{} - Fetching lead", id);
        LeadResponse lead = leadService.getLeadById(id);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead fetched successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLead(@PathVariable Long id) {
        log.info("DELETE /api/leads/{} - Deleting lead", id);
        leadService.deleteLead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Lead deleted successfully"));
    }
}
