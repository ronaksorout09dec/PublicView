package com.skyheights.realestate.modules.tenant.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.tenant.dto.ChecklistTemplateCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.ChecklistTemplateResponse;
import com.skyheights.realestate.modules.tenant.entity.ChecklistTemplate;
import com.skyheights.realestate.modules.tenant.enums.ReportType;
import com.skyheights.realestate.modules.tenant.repository.ChecklistTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistTemplateService {

    private final ChecklistTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public ChecklistTemplateResponse createTemplate(Long orgId, ChecklistTemplateCreateRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        ChecklistTemplate template = ChecklistTemplate.builder()
                .organization(org)
                .type(request.getType())
                .name(request.getName())
                .description(request.getDescription())
                .itemsJson(request.getItemsJson())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        template = templateRepository.save(template);
        log.info("Created checklist template {} type {} org {}", template.getName(), template.getType(), orgId);
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public List<ChecklistTemplateResponse> getTemplates(Long orgId, ReportType type) {
        List<ChecklistTemplate> templates;
        if (type != null) {
            templates = templateRepository.findByOrganizationIdAndTypeAndIsDeletedFalse(orgId, type);
        } else {
            templates = templateRepository.findByOrganizationIdAndIsDeletedFalse(orgId);
        }
        return templates.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChecklistTemplateResponse getTemplate(Long orgId, Long id) {
        ChecklistTemplate template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        return toResponse(template);
    }

    @Transactional
    public ChecklistTemplateResponse updateTemplate(Long orgId, Long id, ChecklistTemplateCreateRequest request) {
        ChecklistTemplate template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (request.getType() != null) template.setType(request.getType());
        if (request.getName() != null) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getItemsJson() != null) template.setItemsJson(request.getItemsJson());
        if (request.getIsActive() != null) template.setIsActive(request.getIsActive());

        template = templateRepository.save(template);
        return toResponse(template);
    }

    @Transactional
    public void deleteTemplate(Long orgId, Long id) {
        ChecklistTemplate template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        template.setIsDeleted(true);
        templateRepository.save(template);
        log.info("Soft deleted checklist template {} org {}", id, orgId);
    }

    private ChecklistTemplateResponse toResponse(ChecklistTemplate t) {
        return ChecklistTemplateResponse.builder()
                .id(t.getId())
                .uuid(t.getUuid())
                .orgId(t.getOrganization() != null ? t.getOrganization().getId() : null)
                .type(t.getType())
                .name(t.getName())
                .description(t.getDescription())
                .itemsJson(t.getItemsJson())
                .isActive(t.getIsActive())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
