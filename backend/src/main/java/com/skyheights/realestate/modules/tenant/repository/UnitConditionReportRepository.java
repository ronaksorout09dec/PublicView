package com.skyheights.realestate.modules.tenant.repository;

import com.skyheights.realestate.modules.tenant.entity.UnitConditionReport;
import com.skyheights.realestate.modules.tenant.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitConditionReportRepository extends JpaRepository<UnitConditionReport, Long> {

    Optional<UnitConditionReport> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<UnitConditionReport> findByLeaseIdAndIsDeletedFalse(Long leaseId);

    List<UnitConditionReport> findByUnitIdAndIsDeletedFalse(Long unitId);

    Page<UnitConditionReport> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    @Query("SELECT r FROM UnitConditionReport r WHERE r.organization.id = :orgId AND r.isDeleted = false " +
            "AND (:leaseId IS NULL OR r.lease.id = :leaseId) " +
            "AND (:unitId IS NULL OR r.unit.id = :unitId) " +
            "AND (:tenantId IS NULL OR r.tenant.id = :tenantId) " +
            "AND (:type IS NULL OR r.type = :type)")
    Page<UnitConditionReport> search(@Param("orgId") Long orgId,
                                     @Param("leaseId") Long leaseId,
                                     @Param("unitId") Long unitId,
                                     @Param("tenantId") Long tenantId,
                                     @Param("type") ReportType type,
                                     Pageable pageable);
}
