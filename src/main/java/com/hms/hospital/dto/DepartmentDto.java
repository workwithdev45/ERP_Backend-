package com.hms.hospital.dto;

import com.hms.hospital.entity.Department;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {
    private Long id;
    private String tenantId;
    private String tenantName;

    @NotBlank(message = "Branch ID is required")
    private String branchId;

    private String branchName;

    @NotBlank(message = "Department name is required")
    private String name;

    @NotBlank(message = "Department code is required")
    private String code;

    private Department.DepartmentType departmentType;
    private boolean active;
}
