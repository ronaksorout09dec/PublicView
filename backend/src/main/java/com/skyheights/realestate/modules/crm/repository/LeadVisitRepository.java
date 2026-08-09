package com.skyheights.realestate.modules.crm.repository;

import com.skyheights.realestate.modules.crm.entity.LeadVisit;
import com.skyheights.realestate.modules.crm.enums.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LeadVisitRepository extends JpaRepository<LeadVisit, Long> {

    Page<LeadVisit> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<LeadVisit> findByLeadIdAndIsDeletedFalse(Long leadId);

    Page<LeadVisit> findByLeadIdAndIsDeletedFalse(Long leadId, Pageable pageable);

    Page<LeadVisit> findByPropertyIdAndIsDeletedFalse(Long propertyId, Pageable pageable);

    List<LeadVisit> findByStaffIdAndIsDeletedFalse(Long staffId);

    @Query("SELECT v FROM LeadVisit v WHERE v.organization.id = :orgId AND v.isDeleted = false " +
            "AND (:leadId IS NULL OR v.lead.id = :leadId) " +
            "AND (:propertyId IS NULL OR v.property.id = :propertyId) " +
            "AND (:status IS NULL OR v.status = :status) " +
            "AND (:staffId IS NULL OR v.staff.id = :staffId)")
    Page<LeadVisit> search(@Param("orgId") Long orgId,
                           @Param("leadId") Long leadId,
                           @Param("propertyId") Long propertyId,
                           @Param("status") VisitStatus status,
                           @Param("staffId") Long staffId,
                           Pageable pageable);

    List<LeadVisit> findByScheduledAtBetweenAndIsDeletedFalse(Instant start, Instant end);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, VisitStatus status);
}
