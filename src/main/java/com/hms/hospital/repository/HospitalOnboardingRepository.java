package com.hms.hospital.repository;

import com.hms.hospital.entity.HospitalOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalOnboardingRepository extends JpaRepository<HospitalOnboarding, Long> {

    Optional<HospitalOnboarding> findByAdminEmail(String adminEmail);

    Optional<HospitalOnboarding> findByRegistrationToken(String registrationToken);

    Optional<HospitalOnboarding> findByAdminEmailAndRegistrationToken(String adminEmail, String registrationToken);

    boolean existsByAdminEmailAndStatus(String adminEmail, HospitalOnboarding.OnboardingStatus status);
}
