package com.msmeerp.trade.repository;

import com.msmeerp.trade.entity.Payment;
import com.msmeerp.trade.entity.PaymentDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTenantIdAndDirectionOrderByIdDesc(String tenantId, PaymentDirection direction);
}
