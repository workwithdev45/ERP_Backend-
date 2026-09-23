package com.msmeerp.accesscontrol.dto;

import com.msmeerp.accesscontrol.entity.PermissionAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignModulePermissionRequest {

    @NotNull(message = "At least an empty action set is required")
    private Set<PermissionAction> actions;
}
