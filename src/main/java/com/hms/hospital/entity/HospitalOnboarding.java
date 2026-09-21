package com.hms.hospital.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "hospital_onboarding")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "onboarding_id")
    private Long onboardingId;

    @Column(name = "admin_email", nullable = false, length = 256, unique = true)
    private String adminEmail;

    @Column(name = "admin_phone", length = 20)
    private String adminPhone;

    @Column(name = "otp", length = 6)
    private String otp;

    @Column(name = "otp_valid_until")
    private Long otpValidUntil;

    @Column(name = "otp_attempts")
    @Builder.Default
    private Integer otpAttempts = 0;

    @Column(name = "last_otp_sent_at")
    private Instant lastOtpSentAt;

    @Column(name = "registration_token", length = 2048)
    private String registrationToken;

    @Column(name = "portal_id", length = 128)
    private String portalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private OnboardingStatus status = OnboardingStatus.PENDING;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kolkata")
    @LastModifiedDate
    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public enum OnboardingStatus {
        PENDING,
        EMAIL_VERIFIED,
        PORTAL_RESERVED,
        ACTIVE,
        FAILED
    }
}
