package com.skyheights.realestate.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.ai.ConversationMessage;
import com.skyheights.realestate.ai.ConversationSession;
import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.dto.CallSummaryRequest;
import com.skyheights.realestate.dto.CallSummaryResponse;
import com.skyheights.realestate.dto.LeadResponse;
import com.skyheights.realestate.entity.Lead;
import com.skyheights.realestate.service.AIService;
import com.skyheights.realestate.service.LeadService;
import com.skyheights.realestate.service.PromptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CallSummaryController {

    private final AIService aiService;
    private final LeadService leadService;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @PostMapping("/call-summary")
    public ResponseEntity<ApiResponse<CallSummaryResponse>> createCallSummary(@RequestBody CallSummaryRequest request) {
        log.info("POST /api/call-summary - Generating summary for {} messages", request.getConversationHistory().size());

        try {
            // Build conversation session from request history
            ConversationSession session = ConversationSession.builder()
                    .sessionId("call-summary-" + System.currentTimeMillis())
                    .systemPrompt(promptService.generateSystemPrompt())
                    .build();

            for (CallSummaryRequest.ConversationMessageDto msg : request.getConversationHistory()) {
                ConversationMessage cm = ConversationMessage.builder()
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .timestamp(System.currentTimeMillis())
                        .build();
                session.getMessages().add(cm);
            }

            // Generate summary via AIService (which calls Ollama)
            String summaryJson = aiService.generateLeadSummary(session);
            log.info("Generated summary JSON: {}", summaryJson);

            // Parse lead from summary
            String cleanJson = summaryJson;
            // If summaryJson is not pure JSON, try to extract
            JsonNode node;
            try {
                node = objectMapper.readTree(cleanJson);
            } catch (Exception e) {
                // Try to extract JSON block
                int start = cleanJson.indexOf('{');
                int end = cleanJson.lastIndexOf('}');
                if (start != -1 && end != -1) {
                    cleanJson = cleanJson.substring(start, end + 1);
                    node = objectMapper.readTree(cleanJson);
                } else {
                    throw e;
                }
            }

            Lead lead = Lead.builder()
                    .customerName(getTextOrNull(node, "customerName", request.getCustomerName()))
                    .phone(getTextOrNull(node, "phone", request.getPhone()))
                    .location(getTextOrNull(node, "location"))
                    .propertyType(getTextOrNull(node, "propertyType"))
                    .configuration(getTextOrNull(node, "configuration"))
                    .budget(getTextOrNull(node, "budget"))
                    .purpose(getTextOrNull(node, "purpose"))
                    .timeline(getTextOrNull(node, "timeline"))
                    .conversationSummary(getTextOrNull(node, "conversationSummary"))
                    .build();

            // Fallback for required fields validation - use defaults if missing
            if (lead.getCustomerName() == null || lead.getCustomerName().isBlank() || "null".equalsIgnoreCase(lead.getCustomerName())) {
                lead.setCustomerName(request.getCustomerName() != null ? request.getCustomerName() : "Unknown");
            }
            if (lead.getPhone() == null || lead.getPhone().isBlank() || "null".equalsIgnoreCase(lead.getPhone())) {
                lead.setPhone(request.getPhone() != null ? request.getPhone() : "0000000000");
            }
            // If phone is still invalid, generate placeholder but log warning
            if (!lead.getPhone().matches("^[6-9]\\d{9}$")) {
                log.warn("Phone {} is invalid, attempting to use request phone {}", lead.getPhone(), request.getPhone());
                if (request.getPhone() != null && request.getPhone().matches("^[6-9]\\d{9}$")) {
                    lead.setPhone(request.getPhone());
                }
            }

            // Only save if we have valid required fields, otherwise return summary without saving
            LeadResponse savedLead = null;
            String message = "Summary generated";
            if (lead.getPhone() != null && lead.getPhone().matches("^[6-9]\\d{9}$") && lead.getCustomerName() != null && !lead.getCustomerName().isBlank()) {
                try {
                    savedLead = leadService.createLeadFromEntity(lead);
                    message = "Summary generated and lead saved";
                    log.info("Lead saved from call summary: {}", savedLead.getId());
                } catch (Exception e) {
                    log.warn("Failed to save lead, returning summary only: {}", e.getMessage());
                    message = "Summary generated (lead not saved: " + e.getMessage() + ")";
                }
            } else {
                message = "Summary generated (insufficient data to save lead - need valid Name and Phone)";
            }

            CallSummaryResponse response = CallSummaryResponse.builder()
                    .summary(lead.getConversationSummary())
                    .lead(savedLead)
                    .structuredJson(cleanJson)
                    .success(true)
                    .message(message)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, message));

        } catch (Exception e) {
            log.error("Failed to generate call summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate summary: " + e.getMessage()));
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        return getTextOrNull(node, field, null);
    }

    private String getTextOrNull(JsonNode node, String field, String fallback) {
        if (node.has(field) && !node.get(field).isNull()) {
            String val = node.get(field).asText();
            if (val != null && !"null".equalsIgnoreCase(val) && !val.isBlank()) {
                return val;
            }
        }
        return fallback;
    }
}
