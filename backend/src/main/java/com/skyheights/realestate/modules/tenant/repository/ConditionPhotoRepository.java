package com.skyheights.realestate.modules.tenant.repository;

import com.skyheights.realestate.modules.tenant.entity.ConditionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConditionPhotoRepository extends JpaRepository<ConditionPhoto, Long> {

    List<ConditionPhoto> findByReportId(Long reportId);

    List<ConditionPhoto> findByReportItemId(Long reportItemId);
}
