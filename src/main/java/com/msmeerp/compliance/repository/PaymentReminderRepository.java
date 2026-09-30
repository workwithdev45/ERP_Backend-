package com.msmeerp.compliance.repository;

import com.msmeerp.compliance.entity.PaymentReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PaymentReminderRepository extends JpaRepository<PaymentReminder, Long> {
    List<PaymentReminder> findByTenantIdAndDocumentIdOrderBySentAtDesc(String tenantId, Long documentId);

    List<PaymentReminder> findTop100ByTenantIdOrderBySentAtDesc(String tenantId);

    boolean existsByTenantIdAndDocumentIdAndTriggerTypeAndSentAtAfter(String tenantId, Long documentId,
                                                                    PaymentReminder.Trigger trigger, Instant after);

    long countByTenantIdAndDocumentIdAndTriggerTypeAndStatus(String tenantId, Long documentId,
                                                            PaymentReminder.Trigger trigger, PaymentReminder.Status status);
}
