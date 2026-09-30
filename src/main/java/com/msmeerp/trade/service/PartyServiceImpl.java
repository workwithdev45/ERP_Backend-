package com.msmeerp.trade.service;

import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.trade.dto.PartyDto;
import com.msmeerp.trade.dto.PartyRequest;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.PartyType;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.repository.PartyRepository;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PartyServiceImpl implements PartyService {

    private static final Set<DocumentStatus> OPEN_BILLING = Set.of(DocumentStatus.UNPAID, DocumentStatus.PARTIALLY_PAID);

    private final PartyRepository partyRepository;
    private final TradeDocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PartyDto> listParties(PartyType role) {
        String tenantId = TenantContext.getTenantId();
        List<Party> parties = role == null
                ? partyRepository.findByTenantIdOrderByNameAsc(tenantId)
                : partyRepository.findByTenantIdAndPartyTypeInOrderByNameAsc(tenantId, Set.of(role, PartyType.BOTH));
        Map<Long, BigDecimal> outstanding = outstandingByParty(tenantId, role);
        return parties.stream().map(p -> toDto(p, outstanding.getOrDefault(p.getId(), BigDecimal.ZERO))).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PartyDto getParty(Long id) {
        String tenantId = TenantContext.getTenantId();
        return toDto(find(tenantId, id), outstandingByParty(tenantId, null).getOrDefault(id, BigDecimal.ZERO));
    }

    @Override
    @Transactional
    public PartyDto createParty(PartyRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (partyRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.getName().trim())) {
            throw new BadRequestException("A party named '" + request.getName().trim() + "' already exists");
        }
        Party party = new Party();
        apply(party, request);
        party.setTenantId(tenantId);
        return toDto(partyRepository.save(party), BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public PartyDto updateParty(Long id, PartyRequest request) {
        String tenantId = TenantContext.getTenantId();
        Party party = find(tenantId, id);
        if (partyRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, request.getName().trim(), id)) {
            throw new BadRequestException("A party named '" + request.getName().trim() + "' already exists");
        }
        apply(party, request);
        return toDto(partyRepository.save(party), outstandingByParty(tenantId, null).getOrDefault(id, BigDecimal.ZERO));
    }

    private Party find(String tenantId, Long id) {
        return partyRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found with id: " + id));
    }

    private void apply(Party party, PartyRequest request) {
        party.setPartyType(request.getPartyType());
        party.setName(request.getName().trim());
        party.setGstin(StringUtils.hasText(request.getGstin()) ? request.getGstin().trim().toUpperCase() : null);
        party.setPhone(trim(request.getPhone()));
        party.setEmail(trim(request.getEmail()));
        party.setAddressLine1(trim(request.getAddressLine1()));
        party.setCity(trim(request.getCity()));
        party.setState(trim(request.getState()));
        party.setPincode(trim(request.getPincode()));
        party.setPaymentTermsDays(request.getPaymentTermsDays() == null ? 30 : request.getPaymentTermsDays());
        party.setCreditLimit(request.getCreditLimit());
        party.setActive(request.getActive() == null || request.getActive());
    }

    /** Receivables for customers, payables for vendors; both when no role is given. */
    private Map<Long, BigDecimal> outstandingByParty(String tenantId, PartyType role) {
        Stream<TradeDocument> invoices = role == PartyType.VENDOR ? Stream.empty()
                : documentRepository.findByTenantIdAndDocTypeAndStatusIn(tenantId, DocumentType.SALES_INVOICE, OPEN_BILLING).stream();
        Stream<TradeDocument> bills = role == PartyType.CUSTOMER ? Stream.empty()
                : documentRepository.findByTenantIdAndDocTypeAndStatusIn(tenantId, DocumentType.PURCHASE_BILL, OPEN_BILLING).stream();
        return Stream.concat(invoices, bills)
                .collect(Collectors.groupingBy(TradeDocument::getPartyId,
                        Collectors.reducing(BigDecimal.ZERO, TradeDocument::getBalance, BigDecimal::add)));
    }

    private PartyDto toDto(Party party, BigDecimal outstanding) {
        return PartyDto.builder()
                .id(party.getId())
                .partyType(party.getPartyType())
                .name(party.getName())
                .gstin(party.getGstin())
                .phone(party.getPhone())
                .email(party.getEmail())
                .addressLine1(party.getAddressLine1())
                .city(party.getCity())
                .state(party.getState())
                .pincode(party.getPincode())
                .paymentTermsDays(party.getPaymentTermsDays())
                .creditLimit(party.getCreditLimit())
                .active(party.getActive())
                .outstanding(outstanding)
                .build();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
