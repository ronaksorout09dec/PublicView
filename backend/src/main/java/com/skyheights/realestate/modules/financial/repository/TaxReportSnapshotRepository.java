package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.TaxReportSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxReportSnapshotRepository extends JpaRepository<TaxReportSnapshot, Long> {

    Optional<TaxReportSnapshot> findByOrganizationIdAndFinancialYearAndIsDeletedFalse(Long orgId, String financialYear);

    List<TaxReportSnapshot> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    boolean existsByOrganizationIdAndFinancialYearAndIsDeletedFalse(Long orgId, String financialYear);
}
