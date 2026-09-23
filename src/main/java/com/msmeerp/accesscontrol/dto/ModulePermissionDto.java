package com.msmeerp.accesscontrol.dto;

import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.PermissionAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModulePermissionDto {
    private ModuleCode moduleCode;
    private Set<PermissionAction> actions;
}
