package com.msmeerp.onboarding.service.impl;

import com.msmeerp.onboarding.entity.CompanyOnboarding;
import com.msmeerp.onboarding.repository.CompanyOnboardingRepository;
import com.msmeerp.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * G1: a signup abandoned before reaching ACTIVE (e.g. the browser closed right after "claim
 * workspace") permanently occupies that workspace ID and blocks the admin's email from
 * registering again. This job releases anything still incomplete after 7 days so both become
 * available again — the reserved-but-abandoned tenant is deactivated (not deleted, in case
 * someone wants it restored), and the onboarding record is cleared so a fresh register() works.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingCleanupJob {

    private static final Duration ABANDONED_AFTER = Duration.ofDays(7);

    private final CompanyOnboardingRepository onboardingRepository;
    private final TenantRepository tenantRepository;

    @Scheduled(cron = "0 0 3 * * *") // 03:00 server time, daily
    @Transactional
    public void releaseAbandonedSignups() {
        Instant cutoff = Instant.now().minus(ABANDONED_AFTER);
        List<CompanyOnboarding> abandoned = onboardingRepository
                .findByStatusNotAndCreatedAtBefore(CompanyOnboarding.OnboardingStatus.ACTIVE, cutoff);

        for (CompanyOnboarding onboarding : abandoned) {
            if (onboarding.getPortalId() != null) {
                tenantRepository.findByPortalId(onboarding.getPortalId()).ifPresent(tenant -> {
                    tenant.setActive(false);
                    tenantRepository.save(tenant);
                    log.info("Released abandoned workspace '{}' (never activated)", tenant.getPortalId());
                });
            }
            onboardingRepository.delete(onboarding);
        }

        if (!abandoned.isEmpty()) {
            log.info("Onboarding cleanup: released {} abandoned signup(s)", abandoned.size());
        }
    }
}
