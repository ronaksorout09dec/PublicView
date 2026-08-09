package com.skyheights.realestate.modules.maintenance.repository;

import com.skyheights.realestate.modules.maintenance.entity.WorkOrder;
import com.skyheights.realestate.modules.maintenance.enums.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Optional<WorkOrder> findByTicketIdAndIsDeletedFalse(Long ticketId);

    Page<WorkOrder> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<WorkOrder> findByVendorIdAndIsDeletedFalse(Long vendorId);

    @Query("SELECT w FROM WorkOrder w WHERE w.organization.id = :orgId AND w.isDeleted = false " +
            "AND (:vendorId IS NULL OR w.vendor.id = :vendorId) " +
            "AND (:status IS NULL OR w.status = :status)")
    Page<WorkOrder> search(@Param("orgId") Long orgId,
                           @Param("vendorId") Long vendorId,
                           @Param("status") WorkOrderStatus status,
                           Pageable pageable);
}
