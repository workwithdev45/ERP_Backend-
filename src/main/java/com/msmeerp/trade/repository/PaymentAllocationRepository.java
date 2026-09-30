package com.msmeerp.trade.repository;

import com.msmeerp.trade.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    @Query("SELECT a FROM PaymentAllocation a JOIN FETCH a.payment p WHERE a.tenantId = :tenantId AND a.document.id = :documentId ORDER BY p.paymentDate, p.id")
    List<PaymentAllocation> findByDocument(@Param("tenantId") String tenantId, @Param("documentId") Long documentId);
}
