package com.msmeerp.trade.repository;

import com.msmeerp.trade.entity.Payment;
import com.msmeerp.trade.entity.PaymentDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTenantIdAndDirectionOrderByIdDesc(String tenantId, PaymentDirection direction);

    List<Payment> findByTenantIdAndPaymentDateBetween(String tenantId, LocalDate from, LocalDate to);
}
