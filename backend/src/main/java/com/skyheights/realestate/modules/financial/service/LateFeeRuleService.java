package com.skyheights.realestate.modules.financial.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.financial.dto.LateFeeRuleCreateRequest;
import com.skyheights.realestate.modules.financial.dto.LateFeeRuleResponse;
import com.skyheights.realestate.modules.financial.entity.LateFeeRule;
import com.skyheights.realestate.modules.financial.repository.LateFeeRuleRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LateFeeRuleService {

    private final LateFeeRuleRepository ruleRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public LateFeeRuleResponse createRule(Long orgId, LateFeeRuleCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        var property = request.getPropertyId() != null ?
                propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Property not found")) : null;

        // Validation based on feeType
        switch (request.getFeeType()) {
            case FIXED:
                if (request.getAmountValue() == null) throw new RuntimeException("amountValue required for FIXED");
                break;
            case PERCENTAGE_PER_DAY:
                if (request.getPercentageRate() == null) throw new RuntimeException("percentageRate required for PERCENTAGE_PER_DAY");
                break;
            case SLAB:
                if (request.getAmountValue() == null) throw new RuntimeException("amountValue required for SLAB");
                break;
        }

        LateFeeRule rule = LateFeeRule.builder()
                .organization(org)
                .property(property)
                .name(request.getName())
                .feeType(request.getFeeType())
                .amountValue(request.getAmountValue())
                .percentageRate(request.getPercentageRate())
                .gracePeriodDays(request.getGracePeriodDays() != null ? request.getGracePeriodDays() : 3)
                .maxCapAmount(request.getMaxCapAmount())
                .compounding(request.getCompounding() != null ? request.getCompounding() : false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        rule = ruleRepository.save(rule);
        log.info("Created late fee rule {} org {}", rule.getName(), orgId);
        return toResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<LateFeeRuleResponse> getRules(Long orgId) {
        return ruleRepository.findByOrganizationIdAndIsDeletedFalse(orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LateFeeRuleResponse getRule(Long orgId, Long id) {
        LateFeeRule rule = ruleRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Late fee rule not found"));
        return toResponse(rule);
    }

    @Transactional
    public LateFeeRuleResponse updateRule(Long orgId, Long id, LateFeeRuleCreateRequest request) {
        LateFeeRule rule = ruleRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        if (request.getPropertyId() != null) {
            var property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
            rule.setProperty(property);
        }
        if (request.getName() != null) rule.setName(request.getName());
        if (request.getFeeType() != null) rule.setFeeType(request.getFeeType());
        if (request.getAmountValue() != null) rule.setAmountValue(request.getAmountValue());
        if (request.getPercentageRate() != null) rule.setPercentageRate(request.getPercentageRate());
        if (request.getGracePeriodDays() != null) rule.setGracePeriodDays(request.getGracePeriodDays());
        if (request.getMaxCapAmount() != null) rule.setMaxCapAmount(request.getMaxCapAmount());
        if (request.getCompounding() != null) rule.setCompounding(request.getCompounding());
        if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());

        rule = ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public void deleteRule(Long orgId, Long id) {
        LateFeeRule rule = ruleRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
        rule.setIsDeleted(true);
        ruleRepository.save(rule);
        log.info("Soft deleted late fee rule {} org {}", id, orgId);
    }

    private LateFeeRuleResponse toResponse(LateFeeRule r) {
        return LateFeeRuleResponse.builder()
                .id(r.getId())
                .uuid(r.getUuid())
                .orgId(r.getOrganization() != null ? r.getOrganization().getId() : null)
                .propertyId(r.getProperty() != null ? r.getProperty().getId() : null)
                .propertyName(r.getProperty() != null ? r.getProperty().getName() : null)
                .name(r.getName())
                .feeType(r.getFeeType())
                .amountValue(r.getAmountValue())
                .percentageRate(r.getPercentageRate())
                .gracePeriodDays(r.getGracePeriodDays())
                .maxCapAmount(r.getMaxCapAmount())
                .compounding(r.getCompounding())
                .isActive(r.getIsActive())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
