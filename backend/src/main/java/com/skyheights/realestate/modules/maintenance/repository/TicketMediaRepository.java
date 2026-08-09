package com.skyheights.realestate.modules.maintenance.repository;

import com.skyheights.realestate.modules.maintenance.entity.TicketMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMediaRepository extends JpaRepository<TicketMedia, Long> {

    List<TicketMedia> findByTicketId(Long ticketId);

    List<TicketMedia> findByTicketIdAndMediaType(Long ticketId, String mediaType);
}
