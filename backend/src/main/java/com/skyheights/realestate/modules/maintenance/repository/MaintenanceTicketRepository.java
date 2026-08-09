package com.skyheights.realestate.modules.maintenance.repository;

import com.skyheights.realestate.modules.maintenance.entity.MaintenanceTicket;
import com.skyheights.realestate.modules.maintenance.enums.TicketPriority;
import com.skyheights.realestate.modules.maintenance.enums.TicketStatus;
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
public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {

    Optional<MaintenanceTicket> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Page<MaintenanceTicket> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<MaintenanceTicket> findByTenantIdAndIsDeletedFalse(Long tenantId);

    List<MaintenanceTicket> findByAssignedVendorIdAndIsDeletedFalse(Long vendorId);

    @Query("SELECT t FROM MaintenanceTicket t WHERE t.organization.id = :orgId AND t.isDeleted = false " +
            "AND (:propertyId IS NULL OR t.property.id = :propertyId) " +
            "AND (:unitId IS NULL OR t.unit.id = :unitId) " +
            "AND (:tenantId IS NULL OR t.tenant.id = :tenantId) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:priority IS NULL OR t.priority = :priority) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:assignedVendorId IS NULL OR t.assignedVendor.id = :assignedVendorId) " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MaintenanceTicket> search(@Param("orgId") Long orgId,
                                   @Param("propertyId") Long propertyId,
                                   @Param("unitId") Long unitId,
                                   @Param("tenantId") Long tenantId,
                                   @Param("status") TicketStatus status,
                                   @Param("priority") TicketPriority priority,
                                   @Param("category") String category,
                                   @Param("assignedVendorId") Long assignedVendorId,
                                   @Param("search") String search,
                                   Pageable pageable);

    List<MaintenanceTicket> findByOrganizationIdAndStatusAndSlaDueAtBeforeAndIsDeletedFalse(Long orgId, TicketStatus status, Instant before);

    List<MaintenanceTicket> findByStatusAndSlaDueAtBeforeAndIsDeletedFalse(TicketStatus status, Instant before);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, TicketStatus status);

    long countByAssignedVendorIdAndIsDeletedFalse(Long vendorId);
}
