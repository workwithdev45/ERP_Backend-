package com.msmeerp.trade.repository;

import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.PartyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {
    Optional<Party> findByTenantIdAndId(String tenantId, Long id);

    List<Party> findByTenantIdOrderByNameAsc(String tenantId);

    List<Party> findByTenantIdAndPartyTypeInOrderByNameAsc(String tenantId, Collection<PartyType> types);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(String tenantId, String name, Long id);

    boolean existsByTenantIdAndNameIgnoreCase(String tenantId, String name);
}
