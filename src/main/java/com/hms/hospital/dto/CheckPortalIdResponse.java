package com.hms.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPortalIdResponse {

    @Builder.Default
    private boolean error = false;

    private String message;

    private boolean available;
}
