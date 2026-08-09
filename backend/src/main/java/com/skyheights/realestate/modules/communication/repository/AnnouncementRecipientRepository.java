package com.skyheights.realestate.modules.communication.repository;

import com.skyheights.realestate.modules.communication.entity.AnnouncementRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRecipientRepository extends JpaRepository<AnnouncementRecipient, Long> {

    List<AnnouncementRecipient> findByAnnouncementId(Long announcementId);

    Optional<AnnouncementRecipient> findByAnnouncementIdAndRecipientUserId(Long announcementId, Long recipientUserId);

    long countByAnnouncementIdAndStatus(Long announcementId, String status);

    List<AnnouncementRecipient> findByRecipientUserIdAndStatus(Long recipientUserId, String status);
}
