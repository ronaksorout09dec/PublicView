package com.skyheights.realestate.modules.communication.repository;

import com.skyheights.realestate.modules.communication.entity.BroadcastAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BroadcastAnnouncementRepository extends JpaRepository<BroadcastAnnouncement, Long> {

    Page<BroadcastAnnouncement> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<BroadcastAnnouncement> findByOrganizationIdAndIsActiveTrueAndIsDeletedFalse(Long orgId);

    List<BroadcastAnnouncement> findByPropertyIdAndIsActiveTrueAndIsDeletedFalse(Long propertyId);

    @Query("SELECT b FROM BroadcastAnnouncement b WHERE b.organization.id = :orgId AND b.isDeleted = false " +
            "AND (:propertyId IS NULL OR b.property.id = :propertyId) " +
            "AND (:isActive IS NULL OR b.isActive = :isActive) " +
            "AND (b.expiresAt IS NULL OR b.expiresAt > :now)")
    Page<BroadcastAnnouncement> search(@Param("orgId") Long orgId,
                                       @Param("propertyId") Long propertyId,
                                       @Param("isActive") Boolean isActive,
                                       @Param("now") Instant now,
                                       Pageable pageable);

    List<BroadcastAnnouncement> findByExpiresAtBeforeAndIsActiveTrueAndIsDeletedFalse(Instant before);
}
