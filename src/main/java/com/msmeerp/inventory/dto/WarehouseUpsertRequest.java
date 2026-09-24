package com.msmeerp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseUpsertRequest {

    @NotBlank(message = "Warehouse name is required")
    private String name;

    @NotBlank(message = "Warehouse code is required")
    private String code;

    private String location;
    private Boolean active;
}
