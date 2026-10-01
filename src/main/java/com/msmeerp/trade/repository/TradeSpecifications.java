package com.msmeerp.trade.repository;

import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Payment;
import com.msmeerp.trade.entity.PaymentDirection;
import com.msmeerp.trade.entity.TradeDocument;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** W15: filters for paged document and payment lists — always tenant-scoped. */
public final class TradeSpecifications {

    private TradeSpecifications() {
    }

    /** Documents of one type, optionally narrowed by status, party and a search over number, party and reference. */
    public static Specification<TradeDocument> documents(String tenantId, DocumentType type, Collection<DocumentStatus> statuses,
                                                         Long partyId, String search) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("tenantId"), tenantId));
            where.add(cb.equal(root.get("docType"), type));
            if (statuses != null && !statuses.isEmpty()) {
                where.add(root.get("status").in(statuses));
            }
            if (partyId != null) {
                where.add(cb.equal(root.get("partyId"), partyId));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                where.add(cb.or(
                        cb.like(cb.lower(root.get("docNumber")), like),
                        cb.like(cb.lower(root.get("partyName")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("partyReference"), "")), like)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    public static Specification<Payment> payments(String tenantId, PaymentDirection direction, String search) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("tenantId"), tenantId));
            where.add(cb.equal(root.get("direction"), direction));
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                where.add(cb.or(
                        cb.like(cb.lower(root.get("paymentNumber")), like),
                        cb.like(cb.lower(root.get("partyName")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("reference"), "")), like)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
