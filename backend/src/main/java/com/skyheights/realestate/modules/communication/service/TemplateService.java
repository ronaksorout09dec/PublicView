package com.skyheights.realestate.modules.communication.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.communication.dto.TemplateCreateRequest;
import com.skyheights.realestate.modules.communication.dto.TemplateResponse;
import com.skyheights.realestate.modules.communication.entity.NotificationTemplate;
import com.skyheights.realestate.modules.communication.repository.NotificationTemplateRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TemplateResponse createTemplate(Long orgId, TemplateCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (templateRepository.existsByOrganizationIdAndCodeAndIsDeletedFalse(orgId, request.getCode())) {
            throw new RuntimeException("Template code already exists: " + request.getCode());
        }
        if (templateRepository.existsByOrganizationIdAndNameAndIsDeletedFalse(orgId, request.getName())) {
            throw new RuntimeException("Template name already exists: " + request.getName());
        }

        String variablesJson = null;
        if (request.getVariables() != null) {
            try {
                variablesJson = objectMapper.writeValueAsString(request.getVariables());
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize variables");
            }
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .organization(org)
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .channel(request.getChannel())
                .subject(request.getSubject())
                .body(request.getBody())
                .bodyWhatsappTemplateId(request.getBodyWhatsappTemplateId())
                .variablesJson(variablesJson)
                .category(request.getCategory())
                .locale(request.getLocale() != null ? request.getLocale() : "en")
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        template = templateRepository.save(template);
        log.info("Created notification template {} code {} org {}", template.getName(), template.getCode(), orgId);
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public Page<TemplateResponse> getTemplates(Long orgId, Pageable pageable) {
        Page<NotificationTemplate> page = templateRepository.findByOrganizationIdAndIsDeletedFalse(orgId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(Long orgId, Long id) {
        NotificationTemplate template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateByCode(Long orgId, String code) {
        NotificationTemplate template = templateRepository.findByOrganizationIdAndCodeAndIsDeletedFalse(orgId, code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with code " + code));
        return toResponse(template);
    }

    @Transactional
    public TemplateResponse updateTemplate(Long orgId, Long id, TemplateCreateRequest request) {
        NotificationTemplate template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (request.getName() != null && !request.getName().equals(template.getName())) {
            if (templateRepository.existsByOrganizationIdAndNameAndIsDeletedFalse(orgId, request.getName())) {
                throw new RuntimeException("Template name already exists");
            }
            template.setName(request.getName());
        }
        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(template.getCode())) {
            if (templateRepository.existsByOrganizationIdAndCodeAndIsDeletedFalse(orgId, request.getCode())) {
                throw new RuntimeException("Template code already exists");
            }
            template.setCode(request.getCode().toUpperCase());
        }
        if (request.getChannel() != null) template.setChannel(request.getChannel());
        if (request.getSubject() != null) template.setSubject(request.getSubject());
        if (request.getBody() != null) template.setBody(request.getBody());
        if (request.getBodyWhatsappTemplateId() != null) template.setBodyWhatsappTemplateId(request.getBodyWhatsappTemplateId());
        if (request.getVariables() != null) {
            try {
                template.setVariablesJson(objectMapper.writeValueAsString(request.getVariables()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize variables");
            }
        }
        if (request.getCategory() != null) template.setCategory(request.getCategory());
        if (request.getLocale() != null) template.setLocale(request.getLocale());
        if (request.getIsActive() != null) template.setIsActive(request.getIsActive());

        template = templateRepository.save(template);
        return toResponse(template);
    }

    @Transactional
    public void deleteTemplate(Long orgId, Long id) {
        NotificationTemplate template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        template.setIsDeleted(true);
        templateRepository.save(template);
        log.info("Soft deleted template {} org {}", id, orgId);
    }

    private TemplateResponse toResponse(NotificationTemplate t) {
        List<String> variables = null;
        if (t.getVariablesJson() != null) {
            try {
                variables = objectMapper.readValue(t.getVariablesJson(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse variables JSON for template {}", t.getId());
            }
        }

        return TemplateResponse.builder()
                .id(t.getId()).uuid(t.getUuid())
                .orgId(t.getOrganization() != null ? t.getOrganization().getId() : null)
                .name(t.getName()).code(t.getCode()).channel(t.getChannel())
                .subject(t.getSubject()).body(t.getBody())
                .bodyWhatsappTemplateId(t.getBodyWhatsappTemplateId())
                .variables(variables).category(t.getCategory())
                .isActive(t.getIsActive()).locale(t.getLocale())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
