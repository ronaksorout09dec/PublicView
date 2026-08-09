package com.skyheights.realestate.modules.financial.scheduler;

import com.skyheights.realestate.modules.financial.enums.InvoiceStatus;
import com.skyheights.realestate.modules.financial.repository.InvoiceRepository;
import com.skyheights.realestate.modules.financial.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Prop-OS Financial Scheduler
 * - Auto-generates rent invoices on 1st of each month 2 AM IST (8:30 PM UTC previous day)
 * - Applies late fees daily 3 AM IST
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialScheduler {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    // 1st of every month at 2 AM IST = 8:30 PM UTC on previous day? Actually IST = UTC+5:30, so 2 AM IST = 20:30 UTC previous day.
    // For simplicity, run 1st at 2:30 AM UTC = 8 AM IST
    @Scheduled(cron = "0 30 2 1 * *") // UTC 2:30 AM on 1st day of month
    @Transactional
    public void autoGenerateMonthlyRentInvoices() {
        log.info("💰 FinancialScheduler: Starting auto-generation of rent invoices for {}", LocalDate.now().withDayOfMonth(1));
        try {
            int count = invoiceService.autoGenerateRentInvoicesForMonth(LocalDate.now().withDayOfMonth(1));
            log.info("✅ Auto-generated {} rent invoices for month {}", count, LocalDate.now().getMonth());
        } catch (Exception e) {
            log.error("❌ Failed to auto-generate rent invoices", e);
        }
    }

    // Daily at 3 AM IST = 21:30 UTC previous day, use 3:30 AM UTC = 9 AM IST
    @Scheduled(cron = "0 30 3 * * *") // UTC 3:30 AM daily = 9 AM IST
    @Transactional
    public void applyLateFeesDaily() {
        log.info("⏰ FinancialScheduler: Starting daily late fee check for overdue invoices");
        try {
            LocalDate today = LocalDate.now();
            List<InvoiceStatus> overdueStatuses = List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID);

            // Find overdue invoices: dueDate < today and status ISSUED/PARTIALLY_PAID
            var overdueInvoices = invoiceRepository.findOverdue(overdueStatuses, today);

            int applied = 0;
            for (var invoice : overdueInvoices) {
                try {
                    // For each overdue, apply late fee
                    // Use organization id from invoice
                    invoiceService.applyLateFee(invoice.getOrganization().getId(), invoice.getId());
                    applied++;
                } catch (Exception e) {
                    log.warn("Failed to apply late fee for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
                }
            }

            log.info("✅ Applied late fees to {} overdue invoices (total overdue {})", applied, overdueInvoices.size());
        } catch (Exception e) {
            log.error("❌ Failed to apply late fees", e);
        }
    }

    // For dev testing, run every hour check (disabled in prod by profile? Keep log)
    @Scheduled(fixedRate = 3600000)
    public void hourlyHeartbeat() {
        // log.debug("FinancialScheduler hourly heartbeat");
    }
}
