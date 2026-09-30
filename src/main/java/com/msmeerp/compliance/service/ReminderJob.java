package com.msmeerp.compliance.service;

import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/** Sends each active company's automatic payment reminders every morning. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderJob {

    private final TenantRepository tenantRepository;
    private final ReminderService reminderService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata") // 09:00 IST, daily
    public void sendDueReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        for (Tenant tenant : tenantRepository.findAll()) {
            if (!tenant.isActive()) {
                continue;
            }
            TenantContext.setTenantId(tenant.getId());
            try {
                int sent = reminderService.runForCurrentTenant(today);
                if (sent > 0) {
                    log.info("Sent {} payment reminder(s) for {}", sent, tenant.getPortalId());
                }
            } catch (RuntimeException e) {
                log.error("Payment reminders failed for {}", tenant.getPortalId(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
