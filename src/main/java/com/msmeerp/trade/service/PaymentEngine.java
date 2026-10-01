package com.msmeerp.trade.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.trade.dto.AgeingDocumentDto;
import com.msmeerp.trade.dto.AgeingPartyDto;
import com.msmeerp.trade.dto.AllocationDto;
import com.msmeerp.trade.dto.ListQuery;
import com.msmeerp.trade.dto.PaymentAllocationRequest;
import com.msmeerp.trade.dto.PaymentDto;
import com.msmeerp.trade.dto.PaymentRequest;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.Payment;
import com.msmeerp.trade.entity.PaymentAllocation;
import com.msmeerp.trade.entity.PaymentDirection;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.repository.PaymentRepository;
import com.msmeerp.trade.repository.TradeSpecifications;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Customer receipts / vendor payments allocated against invoices / bills, and the ageing reports. */
@Component
@RequiredArgsConstructor
public class PaymentEngine {

    private static final Set<DocumentStatus> OPEN_BILLING = Set.of(DocumentStatus.UNPAID, DocumentStatus.PARTIALLY_PAID);

    private final DocumentEngine engine;
    private final PaymentRepository paymentRepository;
    private final TradeDocumentRepository documentRepository;

    /**
     * Records a payment and applies it to the listed bills/invoices. Any amount not allocated stays
     * on the payment as an advance.
     */
    public PaymentDto record(PaymentDirection direction, Party party, DocumentType settles, PaymentRequest request) {
        List<PaymentAllocationRequest> requested = request.getAllocations() == null ? List.of() : request.getAllocations();
        BigDecimal allocatedTotal = requested.stream().map(PaymentAllocationRequest::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocatedTotal.compareTo(request.getAmount()) > 0) {
            throw new BadRequestException("Allocated ₹" + allocatedTotal + " is more than the payment of ₹" + request.getAmount());
        }

        Payment payment = Payment.builder()
                .paymentNumber(engine.nextPaymentNumber(direction))
                .direction(direction)
                .partyId(party.getId())
                .partyName(party.getName())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now())
                .amount(request.getAmount())
                .mode(request.getMode())
                .reference(StringUtils.hasText(request.getReference()) ? request.getReference().trim() : null)
                .notes(StringUtils.hasText(request.getNotes()) ? request.getNotes().trim() : null)
                .createdBy(engine.currentUser())
                .build();
        payment.setTenantId(engine.tenantId());

        for (PaymentAllocationRequest allocationRequest : requested) {
            TradeDocument document = engine.requireDocument(allocationRequest.getDocumentId(), settles);
            if (!document.getPartyId().equals(party.getId())) {
                throw new BadRequestException(document.getDocNumber() + " belongs to " + document.getPartyName());
            }
            engine.requireStatus(document, DocumentStatus.UNPAID, DocumentStatus.PARTIALLY_PAID);
            if (allocationRequest.getAmount().compareTo(document.getBalance()) > 0) {
                throw new BadRequestException(document.getDocNumber() + " has only ₹" + document.getBalance() + " outstanding");
            }
            engine.settle(document, allocationRequest.getAmount());
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .payment(payment)
                    .document(document)
                    .amount(allocationRequest.getAmount())
                    .build();
            allocation.setTenantId(engine.tenantId());
            payment.getAllocations().add(allocation);
        }

        return toDto(paymentRepository.save(payment));
    }

    public PagedResponse<PaymentDto> list(PaymentDirection direction, ListQuery query) {
        Page<Payment> page = paymentRepository.findAll(
                TradeSpecifications.payments(engine.tenantId(), direction, query.getQ()), query.pageable());
        return PagedResponse.from(page.map(this::toDto));
    }

    /** Open bills/invoices grouped by party and bucketed by days past their due date. */
    public List<AgeingPartyDto> ageing(DocumentType type) {
        LocalDate today = LocalDate.now();
        Map<Long, List<TradeDocument>> byParty = documentRepository
                .findByTenantIdAndDocTypeAndStatusIn(engine.tenantId(), type, OPEN_BILLING).stream()
                .filter(d -> d.getBalance().signum() > 0)
                .sorted(Comparator.comparing(TradeDocument::getDocDate))
                .collect(Collectors.groupingBy(TradeDocument::getPartyId, LinkedHashMap::new, Collectors.toList()));

        List<AgeingPartyDto> rows = new ArrayList<>();
        byParty.forEach((partyId, documents) -> {
            BigDecimal[] buckets = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
            List<AgeingDocumentDto> documentRows = new ArrayList<>();
            for (TradeDocument document : documents) {
                LocalDate due = document.getDueDate() != null ? document.getDueDate() : document.getDocDate();
                long overdue = ChronoUnit.DAYS.between(due, today);
                int bucket = overdue <= 0 ? 0 : overdue <= 30 ? 1 : overdue <= 60 ? 2 : overdue <= 90 ? 3 : 4;
                buckets[bucket] = buckets[bucket].add(document.getBalance());
                documentRows.add(AgeingDocumentDto.builder()
                        .documentId(document.getId())
                        .docNumber(document.getDocNumber())
                        .docDate(document.getDocDate())
                        .dueDate(due)
                        .totalAmount(document.getTotalAmount())
                        .balance(document.getBalance())
                        .daysOverdue(overdue)
                        .bucket(new String[]{"Not due", "1–30 days", "31–60 days", "61–90 days", "90+ days"}[bucket])
                        .build());
            }
            rows.add(AgeingPartyDto.builder()
                    .partyId(partyId)
                    .partyName(documents.get(0).getPartyName())
                    .notDue(buckets[0])
                    .days1To30(buckets[1])
                    .days31To60(buckets[2])
                    .days61To90(buckets[3])
                    .over90(buckets[4])
                    .total(buckets[0].add(buckets[1]).add(buckets[2]).add(buckets[3]).add(buckets[4]))
                    .documents(documentRows)
                    .build());
        });
        rows.sort(Comparator.comparing(AgeingPartyDto::getTotal).reversed());
        return rows;
    }

    private PaymentDto toDto(Payment payment) {
        List<AllocationDto> allocations = payment.getAllocations().stream()
                .map(a -> AllocationDto.builder()
                        .paymentId(payment.getId())
                        .paymentNumber(payment.getPaymentNumber())
                        .paymentDate(payment.getPaymentDate())
                        .mode(payment.getMode().name())
                        .documentId(a.getDocument().getId())
                        .documentNumber(a.getDocument().getDocNumber())
                        .amount(a.getAmount())
                        .build())
                .collect(Collectors.toList());
        BigDecimal allocated = allocations.stream().map(AllocationDto::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return PaymentDto.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .direction(payment.getDirection().name())
                .partyId(payment.getPartyId())
                .partyName(payment.getPartyName())
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .allocatedAmount(allocated)
                .unallocatedAmount(payment.getAmount().subtract(allocated))
                .mode(payment.getMode().name())
                .reference(payment.getReference())
                .notes(payment.getNotes())
                .allocations(allocations)
                .build();
    }
}
