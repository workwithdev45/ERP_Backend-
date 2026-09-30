package com.msmeerp.trade.repository;

import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.TradeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeDocumentRepository extends JpaRepository<TradeDocument, Long> {
    Optional<TradeDocument> findByTenantIdAndId(String tenantId, Long id);

    List<TradeDocument> findByTenantIdAndDocTypeOrderByIdDesc(String tenantId, DocumentType docType);

    List<TradeDocument> findByTenantIdAndSourceDocumentIdOrderByIdAsc(String tenantId, Long sourceDocumentId);

    List<TradeDocument> findByTenantIdAndIdIn(String tenantId, Collection<Long> ids);

    List<TradeDocument> findByTenantIdAndDocTypeAndStatusIn(String tenantId, DocumentType docType, Collection<DocumentStatus> statuses);

    List<TradeDocument> findByTenantIdAndDocTypeAndPartyIdAndStatusIn(String tenantId, DocumentType docType, Long partyId,
                                                                     Collection<DocumentStatus> statuses);
}
