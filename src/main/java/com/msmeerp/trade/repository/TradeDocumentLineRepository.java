package com.msmeerp.trade.repository;

import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.TradeDocumentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface TradeDocumentLineRepository extends JpaRepository<TradeDocumentLine, Long> {

    /** Lines of the given document type/statuses, newest document first — used for on-order and last-rate lookups. */
    @Query("SELECT l FROM TradeDocumentLine l JOIN FETCH l.document d " +
            "WHERE l.tenantId = :tenantId AND d.docType = :docType AND d.status IN :statuses ORDER BY d.id DESC")
    List<TradeDocumentLine> findByDocumentTypeAndStatus(@Param("tenantId") String tenantId,
                                                        @Param("docType") DocumentType docType,
                                                        @Param("statuses") Collection<DocumentStatus> statuses);

    @Query("SELECT l FROM TradeDocumentLine l JOIN FETCH l.document d " +
            "WHERE l.tenantId = :tenantId AND d.docType IN :types AND d.docDate BETWEEN :from AND :to")
    List<TradeDocumentLine> findByDocumentTypesAndDateRange(@Param("tenantId") String tenantId,
                                                            @Param("types") Collection<DocumentType> types,
                                                            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
