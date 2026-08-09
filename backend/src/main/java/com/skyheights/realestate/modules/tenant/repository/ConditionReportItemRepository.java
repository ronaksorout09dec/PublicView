package com.skyheights.realestate.modules.tenant.repository;

import com.skyheights.realestate.modules.tenant.entity.ConditionReportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConditionReportItemRepository extends JpaRepository<ConditionReportItem, Long> {

    List<ConditionReportItem> findByReportId(Long reportId);
}
