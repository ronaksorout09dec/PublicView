package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.Invoice;
import com.skyheights.realestate.modules.financial.enums.InvoiceStatus;
import com.skyheights.realestate.modules.financial.enums.InvoiceType;
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
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Page<Invoice> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<Invoice> findByLeaseIdAndIsDeletedFalse(Long leaseId);

    List<Invoice> findByTenantIdAndIsDeletedFalse(Long tenantId);

    boolean existsByOrganizationIdAndInvoiceNumber(Long orgId, String invoiceNumber);

    boolean existsByLeaseIdAndBillingPeriodStartAndBillingPeriodEndAndTypeAndIsDeletedFalse(Long leaseId, LocalDate start, LocalDate end, InvoiceType type);

    @Query("SELECT i FROM Invoice i WHERE i.organization.id = :orgId AND i.isDeleted = false " +
            "AND (:propertyId IS NULL OR i.property.id = :propertyId) " +
            "AND (:unitId IS NULL OR i.unit.id = :unitId) " +
            "AND (:tenantId IS NULL OR i.tenantId = :tenantId) " +
            "AND (:leaseId IS NULL OR i.leaseId = :leaseId) " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:type IS NULL OR i.type = :type) " +
            "AND (:search IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Invoice> search(@Param("orgId") Long orgId,
                         @Param("propertyId") Long propertyId,
                         @Param("unitId") Long unitId,
                         @Param("tenantId") Long tenantId,
                         @Param("leaseId") Long leaseId,
                         @Param("status") InvoiceStatus status,
                         @Param("type") InvoiceType type,
                         @Param("search") String search,
                         Pageable pageable);

    List<Invoice> findByOrganizationIdAndStatusAndDueDateBeforeAndIsDeletedFalse(Long orgId, InvoiceStatus status, LocalDate date);

    List<Invoice> findByOrganizationIdAndStatusInAndIsDeletedFalse(Long orgId, List<InvoiceStatus> statuses);

    @Query("SELECT i FROM Invoice i WHERE i.status IN :statuses AND i.dueDate < :date AND i.isDeleted = false")
    List<Invoice> findOverdue(@Param("statuses") List<InvoiceStatus> statuses, @Param("date") LocalDate date);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalAmount),0) FROM Invoice i WHERE i.organization.id = :orgId AND i.isDeleted = false AND i.status IN :statuses AND i.issueDate BETWEEN :start AND :end")
    java.math.BigDecimal sumTotalByOrgAndStatusAndIssueDateBetween(@Param("orgId") Long orgId,
                                                                    @Param("statuses") List<InvoiceStatus> statuses,
                                                                    @Param("start") LocalDate start,
                                                                    @Param("end") LocalDate end);
}
