package com.hms.accesscontrol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {
    private Long id;

    @NotBlank(message = "Permission name is required")
    private String name;

    @NotBlank(message = "Module is required")
    private String module;

    private String description;
}
