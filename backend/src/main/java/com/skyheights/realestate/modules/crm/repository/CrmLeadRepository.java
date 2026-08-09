package com.skyheights.realestate.modules.crm.repository;

import com.skyheights.realestate.modules.crm.entity.CrmLead;
import com.skyheights.realestate.modules.crm.enums.LeadStatus;
import com.skyheights.realestate.modules.crm.enums.LeadSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrmLeadRepository extends JpaRepository<CrmLead, Long> {

    Optional<CrmLead> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Page<CrmLead> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    @Query("SELECT l FROM CrmLead l WHERE l.organization.id = :orgId AND l.isDeleted = false " +
            "AND (:status IS NULL OR l.status = :status) " +
            "AND (:source IS NULL OR l.source = :source) " +
            "AND (:propertyId IS NULL OR l.property.id = :propertyId) " +
            "AND (:assignedTo IS NULL OR l.assignedTo.id = :assignedTo) " +
            "AND (:search IS NULL OR LOWER(l.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR l.phone LIKE CONCAT('%', :search, '%') OR LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:priority IS NULL OR l.priority = :priority)")
    Page<CrmLead> search(@Param("orgId") Long orgId,
                         @Param("status") LeadStatus status,
                         @Param("source") LeadSource source,
                         @Param("propertyId") Long propertyId,
                         @Param("assignedTo") Long assignedTo,
                         @Param("search") String search,
                         @Param("priority") String priority,
                         Pageable pageable);

    List<CrmLead> findByOrganizationIdAndNextFollowupAtBeforeAndStatusNotAndIsDeletedFalse(Long orgId, Instant before, LeadStatus excludedStatus);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, LeadStatus status);

    long countByOrganizationIdAndIsDeletedFalse(Long orgId);

    @Query("SELECT l FROM CrmLead l LEFT JOIN FETCH l.visits WHERE l.id = :id AND l.organization.id = :orgId AND l.isDeleted = false")
    Optional<CrmLead> findByIdWithVisits(@Param("id") Long id, @Param("orgId") Long orgId);

    boolean existsByOrganizationIdAndPhoneAndPropertyIdAndIsDeletedFalse(Long orgId, String phone, Long propertyId);
}
