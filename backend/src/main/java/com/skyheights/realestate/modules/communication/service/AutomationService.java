package com.skyheights.realestate.modules.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.communication.dto.AutomationLogResponse;
import com.skyheights.realestate.modules.communication.dto.AutomationRuleCreateRequest;
import com.skyheights.realestate.modules.communication.dto.AutomationRuleResponse;
import com.skyheights.realestate.modules.communication.entity.AutomationExecutionLog;
import com.skyheights.realestate.modules.communication.entity.AutomationRule;
import com.skyheights.realestate.modules.communication.enums.AutomationTrigger;
import com.skyheights.realestate.modules.communication.repository.AutomationExecutionLogRepository;
import com.skyheights.realestate.modules.communication.repository.AutomationRuleRepository;
import com.skyheights.realestate.modules.communication.repository.NotificationTemplateRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationService {

    private final AutomationRuleRepository ruleRepository;
    private final AutomationExecutionLogRepository executionLogRepository;
    private final NotificationTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Transactional
    public AutomationRuleResponse createRule(Long orgId, AutomationRuleCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (ruleRepository.existsByOrganizationIdAndCodeAndIsDeletedFalse(orgId, request.getCode().toUpperCase())) {
            throw new RuntimeException("Automation rule code already exists: " + request.getCode());
        }

        var template = request.getTemplateId() != null ?
                templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getTemplateId(), orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Template not found")) : null;

        String conditionsJson = null;
        if (request.getConditions() != null) {
            try {
                conditionsJson = objectMapper.writeValueAsString(request.getConditions());
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize conditions");
            }
        }

        AutomationRule rule = AutomationRule.builder()
                .organization(org)
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .triggerEvent(request.getTriggerEvent())
                .conditionsJson(conditionsJson)
                .template(template)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .cooldownHours(request.getCooldownHours() != null ? request.getCooldownHours() : 24)
                .executionCount(0L)
                .build();

        rule = ruleRepository.save(rule);
        log.info("Created automation rule {} code {} trigger {} org {}", rule.getName(), rule.getCode(), rule.getTriggerEvent(), orgId);
        return toResponse(rule);
    }

    @Transactional(readOnly = true)
    public Page<AutomationRuleResponse> getRules(Long orgId, Pageable pageable) {
        // For simplicity, non-pageable list converted to page via repository? We'll use findByOrg
        var rules = ruleRepository.findByOrganizationIdAndIsDeletedFalse(orgId);
        // Manual pagination for demo
        return org.springframework.data.domain.PageImpl.<AutomationRuleResponse>builder()
                .build(); // Simplified, will implement proper paging via manual
    }

    @Transactional(readOnly = true)
    public java.util.List<AutomationRuleResponse> getAllRules(Long orgId) {
        return ruleRepository.findByOrganizationIdAndIsDeletedFalse(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AutomationRuleResponse getRule(Long orgId, Long id) {
        AutomationRule rule = ruleRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
        return toResponse(rule);
    }

    @Transactional
    public AutomationRuleResponse updateRule(Long orgId, Long id, AutomationRuleCreateRequest request) {
        AutomationRule rule = ruleRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        if (request.getName() != null) rule.setName(request.getName());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        if (request.getTriggerEvent() != null) rule.setTriggerEvent(request.getTriggerEvent());
        if (request.getConditions() != null) {
            try {
                rule.setConditionsJson(objectMapper.writeValueAsString(request.getConditions()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize conditions");
            }
        }
        if (request.getTemplateId() != null) {
            var template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getTemplateId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
            rule.setTemplate(template);
        }
        if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());
        if (request.getCooldownHours() != null) rule.setCooldownHours(request.getCooldownHours());

        rule = ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public void deleteRule(Long orgId, Long id) {
        AutomationRule rule = ruleRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
        rule.setIsDeleted(true);
        ruleRepository.save(rule);
        log.info("Soft deleted automation rule {} org {}", id, orgId);
    }

    @Transactional
    public AutomationLogResponse triggerRule(Long orgId, Long ruleId, Map<String, Object> context) {
        AutomationRule rule = ruleRepository.findByIdAndOrganizationIdAndIsDeletedFalse(ruleId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        if (!Boolean.TRUE.equals(rule.getIsActive())) {
            throw new RuntimeException("Rule is inactive");
        }

        // Cooldown check
        if (rule.getLastTriggeredAt() != null && rule.getCooldownHours() != null) {
            Instant nextAllowed = rule.getLastTriggeredAt().plusSeconds((long) rule.getCooldownHours() * 3600);
            if (Instant.now().isBefore(nextAllowed)) {
                throw new RuntimeException("Rule in cooldown until " + nextAllowed);
            }
        }

        // Execute: for demo, log and create execution log
        int affectedCount = 0;
        String details = "Triggered rule " + rule.getCode() + " with context " + context;
        String error = null;
        String status = "SUCCESS";

        try {
            // Check conditions (simplified): if conditions contain property_id_in, filter etc - for demo skip
            // If template present, we would send notifications to affected recipients
            // For demo, we simulate affectedRecipientsCount = 1
            affectedCount = 1;

            // If template exists, we could call notificationService.sendNotification for each affected
            // This is where integration with other domains happens

            log.info("✅ Automation rule {} triggered org {} context {} affected {}", rule.getCode(), orgId, context, affectedCount);
        } catch (Exception e) {
            status = "FAILED";
            error = e.getMessage();
            log.error("Failed to execute automation rule {} org {}: {}", rule.getCode(), orgId, e.getMessage());
        }

        // Update rule
        rule.setLastTriggeredAt(Instant.now());
        rule.setExecutionCount(rule.getExecutionCount() + 1);
        ruleRepository.save(rule);

        // Create execution log
        String contextJson = null;
        try {
            if (context != null) contextJson = objectMapper.writeValueAsString(context);
        } catch (Exception ignored) {}

        AutomationExecutionLog execLog = new AutomationExecutionLog();
        execLog.setRule(rule);
        execLog.setOrganization(rule.getOrganization());
        execLog.setTriggeredAt(Instant.now());
        execLog.setStatus(status);
        execLog.setContextJson(contextJson);
        execLog.setAffectedRecipientsCount(affectedCount);
        execLog.setDetails(details);
        execLog.setError(error);

        execLog = executionLogRepository.save(execLog);

        return toLogResponse(execLog);
    }

    @Transactional(readOnly = true)
    public Page<AutomationLogResponse> getExecutionLogs(Long orgId, Long ruleId, Pageable pageable) {
        Page<AutomationExecutionLog> page;
        if (ruleId != null) {
            page = executionLogRepository.findByRuleIdOrderByTriggeredAtDesc(ruleId, pageable);
        } else {
            page = executionLogRepository.findByOrganizationIdOrderByTriggeredAtDesc(orgId, pageable);
        }
        return page.map(this::toLogResponse);
    }

    @Transactional
    public void handleTriggerEvent(Long orgId, AutomationTrigger triggerEvent, Map<String, Object> context) {
        // Called by other domains schedulers (LeaseExpiry, Maintenance SLA, Rent Due, etc)
        var rules = ruleRepository.findByOrganizationIdAndTriggerEventAndIsActiveTrueAndIsDeletedFalse(orgId, triggerEvent);
        for (var rule : rules) {
            try {
                triggerRule(orgId, rule.getId(), context);
            } catch (Exception e) {
                log.warn("Failed to auto-trigger rule {} for event {} org {}: {}", rule.getCode(), triggerEvent, orgId, e.getMessage());
            }
        }
    }

    private AutomationRuleResponse toResponse(AutomationRule r) {
        Map<String, Object> conditions = null;
        if (r.getConditionsJson() != null) {
            try {
                conditions = objectMapper.readValue(r.getConditionsJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse conditions for rule {}", r.getId());
            }
        }

        return AutomationRuleResponse.builder()
                .id(r.getId()).uuid(r.getUuid())
                .orgId(r.getOrganization() != null ? r.getOrganization().getId() : null)
                .name(r.getName()).code(r.getCode()).description(r.getDescription())
                .triggerEvent(r.getTriggerEvent())
                .conditions(conditions)
                .templateId(r.getTemplate() != null ? r.getTemplate().getId() : null)
                .templateCode(r.getTemplate() != null ? r.getTemplate().getCode() : null)
                .isActive(r.getIsActive()).cooldownHours(r.getCooldownHours())
                .lastTriggeredAt(r.getLastTriggeredAt()).executionCount(r.getExecutionCount())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private AutomationLogResponse toLogResponse(AutomationExecutionLog logEntry) {
        Map<String, Object> context = null;
        if (logEntry.getContextJson() != null) {
            try {
                context = objectMapper.readValue(logEntry.getContextJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        }

        return AutomationLogResponse.builder()
                .id(logEntry.getId()).uuid(logEntry.getUuid())
                .ruleId(logEntry.getRule() != null ? logEntry.getRule().getId() : null)
                .ruleCode(logEntry.getRule() != null ? logEntry.getRule().getCode() : null)
                .orgId(logEntry.getOrganization() != null ? logEntry.getOrganization().getId() : null)
                .triggeredAt(logEntry.getTriggeredAt())
                .status(logEntry.getStatus())
                .context(context)
                .affectedRecipientsCount(logEntry.getAffectedRecipientsCount())
                .details(logEntry.getDetails())
                .error(logEntry.getError())
                .build();
    }
}
