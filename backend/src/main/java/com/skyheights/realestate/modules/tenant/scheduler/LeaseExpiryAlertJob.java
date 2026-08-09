package com.skyheights.realestate.modules.tenant.scheduler;

import com.skyheights.realestate.modules.tenant.entity.LeaseAgreement;
import com.skyheights.realestate.modules.tenant.enums.LeaseStatus;
import com.skyheights.realestate.modules.tenant.repository.LeaseAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Prop-OS Lease Expiry Alert Job
 * Runs daily 9 AM IST (3:30 AM UTC) to check leases expiring in 60 and 30 days
 * Future Phase 5: Creates NotificationLog entries via Communication domain for Email/WhatsApp/SMS
 * For Phase 3 Domain 2, logs + prepares for notification service integration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaseExpiryAlertJob {

    private final LeaseAgreementRepository leaseRepository;

    // Every day at 9 AM IST = 3:30 AM UTC
    @Scheduled(cron = "0 30 3 * * *") // UTC
    @Transactional(readOnly = true)
    public void checkExpiringLeases() {
        log.info("🔔 LeaseExpiryAlertJob: Starting daily expiry check");

        LocalDate today = LocalDate.now();
        LocalDate in60Days = today.plusDays(60);
        LocalDate in30Days = today.plusDays(30);

        // 60-day alerts
        List<LeaseAgreement> expiring60 = leaseRepository.findExpiringOn(LeaseStatus.ACTIVE, in60Days);
        for (LeaseAgreement lease : expiring60) {
            log.warn("⏰ 60-DAY ALERT: Lease {} (Tenant {} Unit {} Property {}) expires on {} (60 days from today {})",
                    lease.getLeaseNumber(),
                    lease.getTenant() != null && lease.getTenant().getUser() != null ? lease.getTenant().getUser().getFullName() : lease.getTenant().getId(),
                    lease.getUnit() != null ? lease.getUnit().getUnitNumber() : "N/A",
                    lease.getProperty() != null ? lease.getProperty().getName() : "N/A",
                    lease.getEndDate(), today);

            // TODO Phase 5: Create NotificationLog
            // notificationService.createFromTemplate(
            //   orgId: lease.org, templateCode: LEASE_EXPIRY_60D,
            //   recipient: tenant.user, related_entity: lease,
            //   variables: {lease_number, tenant_name, unit_number, expiry_date, days_left:60}
            // );
        }

        // 30-day alerts
        List<LeaseAgreement> expiring30 = leaseRepository.findExpiringOn(LeaseStatus.ACTIVE, in30Days);
        for (LeaseAgreement lease : expiring30) {
            log.warn("⏰ 30-DAY CRITICAL ALERT: Lease {} (Tenant {} Unit {}) expires on {} (30 days from today {})",
                    lease.getLeaseNumber(),
                    lease.getTenant() != null && lease.getTenant().getUser() != null ? lease.getTenant().getUser().getFullName() : lease.getTenant().getId(),
                    lease.getUnit() != null ? lease.getUnit().getUnitNumber() : "N/A",
                    lease.getEndDate(), today);

            // TODO Phase 5: Notification + Broadcast announcement
        }

        // Already expired but still ACTIVE? Mark as EXPIRED automatically
        List<LeaseAgreement> expired = leaseRepository.findByStatusAndEndDateBeforeAndIsDeletedFalse(LeaseStatus.ACTIVE, today);
        for (LeaseAgreement lease : expired) {
            log.error("❌ EXPIRED LEASE AUTO-DETECTION: Lease {} ended on {} but still ACTIVE. Should be marked EXPIRED and unit VACANT. Manual intervention or auto-expire job needed.",
                    lease.getLeaseNumber(), lease.getEndDate());
            // For safety, don't auto-expire in Phase 2; Phase 3 Domain 2 can have auto-expire config
            // If auto-expire enabled, we would:
            // lease.setStatus(EXPIRED);
            // unit.setStatus(VACANT);
        }

        log.info("LeaseExpiryAlertJob: Completed — 60-day: {}, 30-day: {}, expired ACTIVE: {}", expiring60.size(), expiring30.size(), expired.size());
    }

    // For testing, run every hour as well? Disabled for prod, but we can add a test endpoint
    @Scheduled(fixedRate = 3600000) // every hour for log heartbeat in dev, will be removed in prod
    @Transactional(readOnly = true)
    public void hourlyHeartbeatForDev() {
        // Only log in dev if needed
        // log.debug("LeaseExpiryAlertJob hourly heartbeat");
    }
}
