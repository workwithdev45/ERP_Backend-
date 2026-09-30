package com.msmeerp.trade.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.trade.dto.PartyDto;
import com.msmeerp.trade.dto.PartyRequest;
import com.msmeerp.trade.entity.PartyType;
import com.msmeerp.trade.service.PartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** W5 masters: customers and vendors, shared by Sales and Purchase. */
@RestController
@RequestMapping("/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('SALES_VIEW', 'SALES_READ', 'PURCHASE_VIEW', 'PURCHASE_READ')")
    public ResponseEntity<ApiResponse<List<PartyDto>>> listParties(@RequestParam(required = false) PartyType type) {
        return ResponseEntity.ok(ApiResponse.success(partyService.listParties(type)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('SALES_VIEW', 'SALES_READ', 'PURCHASE_VIEW', 'PURCHASE_READ')")
    public ResponseEntity<ApiResponse<PartyDto>> getParty(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(partyService.getParty(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('SALES_CREATE', 'SALES_WRITE', 'PURCHASE_CREATE', 'PURCHASE_WRITE')")
    public ResponseEntity<ApiResponse<PartyDto>> createParty(@Valid @RequestBody PartyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(partyService.createParty(request), "Party created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('SALES_EDIT', 'SALES_WRITE', 'PURCHASE_EDIT', 'PURCHASE_WRITE')")
    public ResponseEntity<ApiResponse<PartyDto>> updateParty(@PathVariable Long id, @Valid @RequestBody PartyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(partyService.updateParty(id, request), "Party updated successfully"));
    }
}
