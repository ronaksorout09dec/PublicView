package com.skyheights.realestate.modules.tenant.repository;

import com.skyheights.realestate.modules.tenant.entity.LeaseAgreement;
import com.skyheights.realestate.modules.tenant.enums.LeaseStatus;
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
public interface LeaseAgreementRepository extends JpaRepository<LeaseAgreement, Long> {

    Optional<LeaseAgreement> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Optional<LeaseAgreement> findByLeaseNumber(String leaseNumber);

    Page<LeaseAgreement> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<LeaseAgreement> findByTenantIdAndIsDeletedFalse(Long tenantId);

    List<LeaseAgreement> findByUnitIdAndIsDeletedFalse(Long unitId);

    List<LeaseAgreement> findByUnitIdAndStatusAndIsDeletedFalse(Long unitId, LeaseStatus status);

    boolean existsByUnitIdAndStatusAndIsDeletedFalse(Long unitId, LeaseStatus status);

    @Query("SELECT l FROM LeaseAgreement l WHERE l.organization.id = :orgId AND l.isDeleted = false " +
            "AND (:propertyId IS NULL OR l.property.id = :propertyId) " +
            "AND (:unitId IS NULL OR l.unit.id = :unitId) " +
            "AND (:tenantId IS NULL OR l.tenant.id = :tenantId) " +
            "AND (:status IS NULL OR l.status = :status) " +
            "AND (:search IS NULL OR LOWER(l.leaseNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LeaseAgreement> search(@Param("orgId") Long orgId,
                                @Param("propertyId") Long propertyId,
                                @Param("unitId") Long unitId,
                                @Param("tenantId") Long tenantId,
                                @Param("status") LeaseStatus status,
                                @Param("search") String search,
                                Pageable pageable);

    // For expiry alerts: active leases ending on date
    List<LeaseAgreement> findByStatusAndEndDateAndIsDeletedFalse(LeaseStatus status, LocalDate endDate);

    List<LeaseAgreement> findByStatusAndEndDateBetweenAndIsDeletedFalse(LeaseStatus status, LocalDate start, LocalDate end);

    List<LeaseAgreement> findByStatusAndEndDateBeforeAndIsDeletedFalse(LeaseStatus status, LocalDate date);

    @Query("SELECT l FROM LeaseAgreement l WHERE l.status = :status AND l.endDate = :targetDate AND l.isDeleted = false")
    List<LeaseAgreement> findExpiringOn(@Param("status") LeaseStatus status, @Param("targetDate") LocalDate targetDate);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, LeaseStatus status);
}
