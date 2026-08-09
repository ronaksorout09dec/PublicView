package com.skyheights.realestate.modules.communication.repository;

import com.skyheights.realestate.modules.communication.entity.NotificationLog;
import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import com.skyheights.realestate.modules.communication.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<NotificationLog> findByStatusAndNextRetryAtBeforeAndIsDeletedFalse(NotificationStatus status, Instant before);

    List<NotificationLog> findByStatusInAndIsDeletedFalse(List<NotificationStatus> statuses);

    @Query("SELECT n FROM NotificationLog n WHERE n.organization.id = :orgId AND n.isDeleted = false " +
            "AND (:channel IS NULL OR n.channel = :channel) " +
            "AND (:status IS NULL OR n.status = :status) " +
            "AND (:recipientType IS NULL OR n.recipientType = :recipientType) " +
            "AND (:relatedEntityType IS NULL OR n.relatedEntityType = :relatedEntityType)")
    Page<NotificationLog> search(@Param("orgId") Long orgId,
                                 @Param("channel") NotificationChannel channel,
                                 @Param("status") NotificationStatus status,
                                 @Param("recipientType") String recipientType,
                                 @Param("relatedEntityType") String relatedEntityType,
                                 Pageable pageable);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, NotificationStatus status);

    List<NotificationLog> findByRelatedEntityTypeAndRelatedEntityIdAndIsDeletedFalse(String relatedEntityType, Long relatedEntityId);
}
