package com.skyheights.realestate.modules.communication.repository;

import com.skyheights.realestate.modules.communication.entity.AutomationRule;
import com.skyheights.realestate.modules.communication.enums.AutomationTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {

    Optional<AutomationRule> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<AutomationRule> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    List<AutomationRule> findByOrganizationIdAndIsActiveTrueAndIsDeletedFalse(Long orgId);

    List<AutomationRule> findByOrganizationIdAndTriggerEventAndIsActiveTrueAndIsDeletedFalse(Long orgId, AutomationTrigger triggerEvent);

    Optional<AutomationRule> findByOrganizationIdAndCodeAndIsDeletedFalse(Long orgId, String code);

    boolean existsByOrganizationIdAndCodeAndIsDeletedFalse(Long orgId, String code);
}
