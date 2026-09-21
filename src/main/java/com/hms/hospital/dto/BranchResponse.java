package com.hms.hospital.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {

    private String id;
    private String tenantId;
    private String tenantName;
    @JsonAlias({"name", "branchName"})
    private String branchName;
    private String location;
    private String phone;
    private String email;
    private boolean active;
    private BranchAdminSummary admin;
    private Instant createdAt;
    private Instant updatedAt;

    public String getName() {
        return branchName;
    }

    public void setName(String name) {
        this.branchName = name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchAdminSummary {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String phoneNumber;
    }
}
