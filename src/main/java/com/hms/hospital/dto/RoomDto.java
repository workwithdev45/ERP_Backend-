package com.hms.hospital.dto;

import com.hms.hospital.entity.Room;
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
public class RoomDto {
    private Long id;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private String departmentName;

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    private Room.RoomType roomType;
    private Integer floorNumber;
    private boolean active;
}
