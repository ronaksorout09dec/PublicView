package com.skyheights.realestate.modules.communication.repository;

import com.skyheights.realestate.modules.communication.entity.AutomationExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutomationExecutionLogRepository extends JpaRepository<AutomationExecutionLog, Long> {

    Page<AutomationExecutionLog> findByRuleIdOrderByTriggeredAtDesc(Long ruleId, Pageable pageable);

    Page<AutomationExecutionLog> findByOrganizationIdOrderByTriggeredAtDesc(Long orgId, Pageable pageable);
}
