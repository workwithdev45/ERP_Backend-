package com.msmeerp.accesscontrol.service.impl;

import com.msmeerp.accesscontrol.dto.TenantModuleDto;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.TenantModule;
import com.msmeerp.accesscontrol.repository.TenantModuleRepository;
import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** G14: every module defaults to enabled unless a row says otherwise, and Settings can't be turned off. */
@ExtendWith(MockitoExtension.class)
class TenantModuleServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private TenantModuleRepository tenantModuleRepository;

    @InjectMocks
    private TenantModuleServiceImpl tenantModuleService;

    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void modulesWithNoStoredRowDefaultToEnabled() {
        when(tenantModuleRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        List<TenantModuleDto> modules = tenantModuleService.getModulesForCurrentTenant();

        assertThat(modules).hasSize(ModuleCode.values().length);
        assertThat(modules).allMatch(TenantModuleDto::isEnabled);
    }

    @Test
    void aStoredDisabledRowOverridesTheDefault() {
        TenantModule salesOff = TenantModule.builder().moduleCode(ModuleCode.SALES).enabled(false).build();
        when(tenantModuleRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(salesOff));

        List<TenantModuleDto> modules = tenantModuleService.getModulesForCurrentTenant();

        assertThat(modules)
                .filteredOn(m -> m.getModuleCode() == ModuleCode.SALES)
                .extracting(TenantModuleDto::isEnabled)
                .containsExactly(false);
    }

    @Test
    void settingsCannotBeDisabled() {
        assertThatThrownBy(() -> tenantModuleService.setModuleEnabled(ModuleCode.SETTINGS, false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aNonSettingsModuleCanBeDisabled() {
        when(tenantModuleRepository.findByTenantIdAndModuleCode(TENANT_ID, ModuleCode.CRM))
                .thenReturn(Optional.empty());
        when(tenantModuleRepository.save(any(TenantModule.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantModuleDto result = tenantModuleService.setModuleEnabled(ModuleCode.CRM, false);

        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getModuleCode()).isEqualTo(ModuleCode.CRM);
    }
}
