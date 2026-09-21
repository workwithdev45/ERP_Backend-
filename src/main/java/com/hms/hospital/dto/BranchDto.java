package com.hms.hospital.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDto {
    private String id;
    private String tenantId;
    private String tenantName;

    @NotBlank(message = "Branch name is required")
    @JsonAlias({"name", "branchName"})
    private String branchName;

    private String address;
    private String phone;
    private String email;
    private boolean active;

    public String getName() {
        return branchName;
    }

    public void setName(String name) {
        this.branchName = name;
    }
}
