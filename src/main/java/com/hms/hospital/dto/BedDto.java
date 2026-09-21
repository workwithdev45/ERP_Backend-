package com.hms.hospital.dto;

import com.hms.hospital.entity.Bed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BedDto {
    private Long id;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    private String roomNumber;

    @NotBlank(message = "Bed number is required")
    private String bedNumber;

    private Bed.BedStatus status;
}
