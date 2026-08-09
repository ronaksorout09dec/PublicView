package com.skyheights.realestate.modules.tenant.repository;

import com.skyheights.realestate.modules.tenant.entity.ChecklistTemplate;
import com.skyheights.realestate.modules.tenant.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {

    Optional<ChecklistTemplate> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<ChecklistTemplate> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    List<ChecklistTemplate> findByOrganizationIdAndTypeAndIsDeletedFalse(Long orgId, ReportType type);

    List<ChecklistTemplate> findByOrganizationIdAndIsActiveTrueAndIsDeletedFalse(Long orgId);
}
