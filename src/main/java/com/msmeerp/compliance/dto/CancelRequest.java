package com.msmeerp.compliance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelRequest {
    @NotNull(message = "Choose a reason")
    @Min(1)
    @Max(4)
    private Integer reasonCode;

    @NotBlank(message = "Add a remark")
    private String remark;
}
