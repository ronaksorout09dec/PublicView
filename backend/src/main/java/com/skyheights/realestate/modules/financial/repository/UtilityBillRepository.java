package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.UtilityBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilityBillRepository extends JpaRepository<UtilityBill, Long> {

    Optional<UtilityBill> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Page<UtilityBill> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<UtilityBill> findByPropertyIdAndIsDeletedFalse(Long propertyId);

    Optional<UtilityBill> findByPropertyIdAndUtilityTypeIdAndBillingMonthAndIsDeletedFalse(Long propertyId, Long utilityTypeId, LocalDate billingMonth);

    @Query("SELECT b FROM UtilityBill b WHERE b.organization.id = :orgId AND b.isDeleted = false " +
            "AND (:propertyId IS NULL OR b.property.id = :propertyId) " +
            "AND (:utilityTypeId IS NULL OR b.utilityType.id = :utilityTypeId) " +
            "AND (:status IS NULL OR b.status = :status)")
    Page<UtilityBill> search(@Param("orgId") Long orgId,
                             @Param("propertyId") Long propertyId,
                             @Param("utilityTypeId") Long utilityTypeId,
                             @Param("status") String status,
                             Pageable pageable);
}
