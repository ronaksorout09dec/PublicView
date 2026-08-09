package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.SecurityDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecurityDepositRepository extends JpaRepository<SecurityDeposit, Long> {

    Optional<SecurityDeposit> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Optional<SecurityDeposit> findByLeaseIdAndIsDeletedFalse(Long leaseId);

    Optional<SecurityDeposit> findByTenantIdAndIsDeletedFalse(Long tenantId);

    Optional<SecurityDeposit> findByUnitIdAndIsDeletedFalse(Long unitId);
}
