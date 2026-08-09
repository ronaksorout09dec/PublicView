package com.skyheights.realestate.modules.maintenance.scheduler;

import com.skyheights.realestate.modules.maintenance.entity.MaintenanceTicket;
import com.skyheights.realestate.modules.maintenance.enums.TicketStatus;
import com.skyheights.realestate.modules.maintenance.repository.MaintenanceTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Prop-OS Maintenance SLA Scheduler
 * Checks for tickets past SLA due time and logs warnings
 * Future: creates NotificationLog for manager + tenant via Communication domain
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceSlaScheduler {

    private final MaintenanceTicketRepository ticketRepository;

    // Every 30 minutes check SLA breaches
    @Scheduled(fixedRate = 1800000) // 30 min
    @Transactional(readOnly = true)
    public void checkSlaBreaches() {
        Instant now = Instant.now();
        List<MaintenanceTicket> breached = ticketRepository.findByStatusAndSlaDueAtBeforeAndIsDeletedFalse(TicketStatus.IN_PROGRESS, now);
        breached.addAll(ticketRepository.findByStatusAndSlaDueAtBeforeAndIsDeletedFalse(TicketStatus.ASSIGNED, now));
        breached.addAll(ticketRepository.findByStatusAndSlaDueAtBeforeAndIsDeletedFalse(TicketStatus.OPEN, now));
        breached.addAll(ticketRepository.findByStatusAndSlaDueAtBeforeAndIsDeletedFalse(TicketStatus.BIDDING, now));

        for (MaintenanceTicket ticket : breached) {
            log.warn("⚠️ SLA BREACH: Ticket {} '{}' category {} priority {} status {} SLA due {} now {} property {} org {}",
                    ticket.getId(), ticket.getTitle(), ticket.getCategory(), ticket.getPriority(),
                    ticket.getStatus(), ticket.getSlaDueAt(), now,
                    ticket.getProperty() != null ? ticket.getProperty().getName() : "N/A",
                    ticket.getOrganization().getId());

            // TODO Phase 5: Notification to manager + tenant
            // notificationService.createFromTemplate(orgId, templateCode: TICKET_SLA_BREACH, recipient: manager, related: ticket)
        }

        if (!breached.isEmpty()) {
            log.info("MaintenanceSlaScheduler: Found {} SLA breached tickets at {}", breached.size(), now);
        }
    }

    @Scheduled(cron = "0 0 8 * * *") // Daily 8 AM UTC = 1:30 PM IST
    @Transactional(readOnly = true)
    public void dailySlaSummary() {
        long open = ticketRepository.countByOrganizationIdAndStatusAndIsDeletedFalse(1L, TicketStatus.OPEN); // placeholder orgId, in prod iterate orgs
        // For demo, log summary per org would require org loop
        log.info("MaintenanceSlaScheduler: Daily summary check — placeholder for org-wise SLA summary");
    }
}
