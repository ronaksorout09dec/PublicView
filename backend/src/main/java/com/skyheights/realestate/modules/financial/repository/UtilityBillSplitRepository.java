package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.UtilityBillSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UtilityBillSplitRepository extends JpaRepository<UtilityBillSplit, Long> {

    List<UtilityBillSplit> findByUtilityBillId(Long utilityBillId);

    List<UtilityBillSplit> findByTenantId(Long tenantId);

    List<UtilityBillSplit> findByUnitId(Long unitId);

    List<UtilityBillSplit> findByInvoiceId(Long invoiceId);
}
