package com.msmeerp.accesscontrol.service.impl;

import com.msmeerp.accesscontrol.dto.TenantModuleDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.TenantModule;
import com.msmeerp.accesscontrol.repository.TenantModuleRepository;
import com.msmeerp.accesscontrol.service.TenantModuleService;
import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantModuleServiceImpl implements TenantModuleService {

    /** Settings can't be switched off — an Admin locked out of it couldn't turn it back on. */
    private static final ModuleCode ALWAYS_ON = ModuleCode.SETTINGS;

    private final TenantModuleRepository tenantModuleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TenantModuleDto> getModulesForCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        Map<ModuleCode, TenantModule> byCode = tenantModuleRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(TenantModule::getModuleCode, tm -> tm));

        return java.util.Arrays.stream(ModuleCode.values())
                .map(code -> TenantModuleDto.builder()
                        .moduleCode(code)
                        .enabled(byCode.containsKey(code) ? byCode.get(code).isEnabled() : true)
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public TenantModuleDto setModuleEnabled(ModuleCode moduleCode, boolean enabled) {
        if (moduleCode == ALWAYS_ON && !enabled) {
            throw new BadRequestException("The Settings module can't be turned off");
        }

        String tenantId = TenantContext.getTenantId();
        TenantModule tenantModule = tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .orElseGet(() -> {
                    TenantModule created = TenantModule.builder().moduleCode(moduleCode).build();
                    created.setTenantId(tenantId);
                    return created;
                });
        tenantModule.setEnabled(enabled);
        TenantModule saved = tenantModuleRepository.save(tenantModule);

        return TenantModuleDto.builder().moduleCode(saved.getModuleCode()).enabled(saved.isEnabled()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleEnabled(String tenantId, ModuleCode moduleCode) {
        return tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .map(TenantModule::isEnabled)
                .orElse(true);
    }

    @Override
    @Transactional
    public void initializeDefaultModules(String tenantId) {
        for (ModuleCode code : ModuleCode.values()) {
            if (tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, code).isEmpty()) {
                TenantModule tenantModule = TenantModule.builder().moduleCode(code).enabled(true).build();
                tenantModule.setTenantId(tenantId);
                tenantModuleRepository.save(tenantModule);
            }
        }
    }

    @Override
    @Transactional
    public void setModuleEnabledForTenant(String tenantId, ModuleCode moduleCode, boolean enabled) {
        if (moduleCode == ALWAYS_ON && !enabled) {
            return;
        }
        TenantModule tenantModule = tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .orElseGet(() -> {
                    TenantModule created = TenantModule.builder().moduleCode(moduleCode).build();
                    created.setTenantId(tenantId);
                    return created;
                });
        tenantModule.setEnabled(enabled);
        tenantModuleRepository.save(tenantModule);
    }
}
