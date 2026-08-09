package com.skyheights.realestate.modules.crm.repository;

import com.skyheights.realestate.modules.crm.entity.WaitlistEntry;
import com.skyheights.realestate.modules.crm.enums.WaitlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Page<WaitlistEntry> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<WaitlistEntry> findByPropertyIdAndStatusAndIsDeletedFalseOrderByPriorityScoreDescPositionAsc(Long propertyId, WaitlistStatus status);

    List<WaitlistEntry> findByPropertyIdAndUnitTypeAndStatusAndIsDeletedFalseOrderByPriorityScoreDescPositionAsc(Long propertyId, String unitType, WaitlistStatus status);

    @Query("SELECT COALESCE(MAX(w.position), 0) FROM WaitlistEntry w WHERE w.property.id = :propertyId AND w.isDeleted = false")
    int findMaxPositionByPropertyId(@Param("propertyId") Long propertyId);

    @Query("SELECT w FROM WaitlistEntry w WHERE w.organization.id = :orgId AND w.isDeleted = false " +
            "AND (:propertyId IS NULL OR w.property.id = :propertyId) " +
            "AND (:unitType IS NULL OR w.unitType = :unitType) " +
            "AND (:status IS NULL OR w.status = :status)")
    Page<WaitlistEntry> search(@Param("orgId") Long orgId,
                               @Param("propertyId") Long propertyId,
                               @Param("unitType") String unitType,
                               @Param("status") WaitlistStatus status,
                               Pageable pageable);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, WaitlistStatus status);

    boolean existsByLeadIdAndPropertyIdAndStatusAndIsDeletedFalse(Long leadId, Long propertyId, WaitlistStatus status);
}
