package com.msmeerp.trade.dto;

import com.msmeerp.trade.entity.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/** Paging and filters for document/payment lists, bound from query parameters. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListQuery {
    public static final int MAX_PAGE_SIZE = 200;

    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 25;
    /** Matches number, party name or party reference. */
    private String q;
    private List<DocumentStatus> status;
    private Long partyId;

    /** Newest first; size is clamped so a client can't pull a whole ledger in one request. */
    public Pageable pageable() {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "id"));
    }
}
