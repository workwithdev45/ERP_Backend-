package com.msmeerp.onboarding.repository;

import com.msmeerp.onboarding.entity.CompanyOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyOnboardingRepository extends JpaRepository<CompanyOnboarding, Long> {

    Optional<CompanyOnboarding> findByAdminEmail(String adminEmail);

    Optional<CompanyOnboarding> findByRegistrationToken(String registrationToken);

    Optional<CompanyOnboarding> findByAdminEmailAndRegistrationToken(String adminEmail, String registrationToken);

    boolean existsByAdminEmailAndStatus(String adminEmail, CompanyOnboarding.OnboardingStatus status);
}
