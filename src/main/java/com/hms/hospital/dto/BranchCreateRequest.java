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
public class BranchCreateRequest {

    @NotBlank(message = "Branch name is required")
    @JsonAlias({"name", "branchName"})
    private String branchName;

    @JsonAlias({"location", "address"})
    private String location;

    private String phone;

    private String email;

    public String getName() {
        return branchName;
    }

    public void setName(String name) {
        this.branchName = name;
    }

    public String getResolvedAddress() {
        return location != null && !location.isBlank() ? location : null;
    }
}
