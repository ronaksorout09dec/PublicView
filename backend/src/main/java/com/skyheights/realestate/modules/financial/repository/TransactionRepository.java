package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.Transaction;
import com.skyheights.realestate.modules.financial.enums.TransactionCategory;
import com.skyheights.realestate.modules.financial.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.organization.id = :orgId AND t.isDeleted = false " +
            "AND (:propertyId IS NULL OR t.property.id = :propertyId) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (CAST(:start AS date) IS NULL OR t.date >= :start) " +
            "AND (CAST(:end AS date) IS NULL OR t.date <= :end)")
    Page<Transaction> search(@Param("orgId") Long orgId,
                             @Param("propertyId") Long propertyId,
                             @Param("type") TransactionType type,
                             @Param("category") TransactionCategory category,
                             @Param("start") LocalDate start,
                             @Param("end") LocalDate end,
                             Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount),0) FROM Transaction t WHERE t.organization.id = :orgId AND t.type = :type AND t.isDeleted = false AND t.date BETWEEN :start AND :end")
    BigDecimal sumByOrgAndTypeAndDateBetween(@Param("orgId") Long orgId,
                                             @Param("type") TransactionType type,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);

    List<Transaction> findByInvoiceIdAndIsDeletedFalse(Long invoiceId);
}
