package com.skyheights.realestate.modules.communication.repository;

import com.skyheights.realestate.modules.communication.entity.NotificationTemplate;
import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Optional<NotificationTemplate> findByOrganizationIdAndCodeAndIsDeletedFalse(Long orgId, String code);

    List<NotificationTemplate> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    Page<NotificationTemplate> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<NotificationTemplate> findByOrganizationIdAndChannelAndIsDeletedFalse(Long orgId, NotificationChannel channel);

    boolean existsByOrganizationIdAndCodeAndIsDeletedFalse(Long orgId, String code);

    boolean existsByOrganizationIdAndNameAndIsDeletedFalse(Long orgId, String name);
}
