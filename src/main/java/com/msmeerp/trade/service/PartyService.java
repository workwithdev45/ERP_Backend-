package com.msmeerp.trade.service;

import com.msmeerp.trade.dto.PartyDto;
import com.msmeerp.trade.dto.PartyRequest;
import com.msmeerp.trade.entity.PartyType;

import java.util.List;

public interface PartyService {
    /** {@code role} null lists everyone; CUSTOMER/VENDOR also include parties marked BOTH. */
    List<PartyDto> listParties(PartyType role);

    PartyDto getParty(Long id);

    PartyDto createParty(PartyRequest request);

    PartyDto updateParty(Long id, PartyRequest request);
}
