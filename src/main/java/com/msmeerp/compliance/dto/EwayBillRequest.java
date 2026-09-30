package com.msmeerp.compliance.dto;

import com.msmeerp.compliance.entity.EwayBill;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EwayBillRequest {
    @NotNull(message = "Choose the mode of transport")
    private EwayBill.TransportMode transportMode;

    @NotNull(message = "Distance is required")
    @Min(value = 1, message = "Distance must be at least 1 km")
    @Max(value = 4000, message = "Distance can't exceed 4000 km")
    private Integer distanceKm;

    private String vehicleNo;
    private String transporterId;
    private String transporterName;
    private String transportDocNo;
}
