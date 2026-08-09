package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.LateFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LateFeeRuleRepository extends JpaRepository<LateFeeRule, Long> {

    Optional<LateFeeRule> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<LateFeeRule> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    List<LateFeeRule> findByOrganizationIdAndPropertyIdAndIsDeletedFalse(Long orgId, Long propertyId);

    List<LateFeeRule> findByOrganizationIdAndIsActiveTrueAndIsDeletedFalse(Long orgId);

    List<LateFeeRule> findByPropertyIdAndIsActiveTrueAndIsDeletedFalse(Long propertyId);
}
