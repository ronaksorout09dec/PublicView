package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.UtilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilityTypeRepository extends JpaRepository<UtilityType, Long> {

    Optional<UtilityType> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<UtilityType> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    Optional<UtilityType> findByOrganizationIdAndNameAndIsDeletedFalse(Long orgId, String name);
}
