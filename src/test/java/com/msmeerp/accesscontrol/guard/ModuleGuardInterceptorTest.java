package com.msmeerp.accesscontrol.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.service.TenantModuleService;
import com.msmeerp.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleGuardInterceptorTest {

    @Mock
    private TenantModuleService tenantModuleService;

    private ModuleGuardInterceptor guard;

    @BeforeEach
    void setUp() {
        // Spring Boot's mapper has the Java-time module; the bare one can't write ApiResponse's timestamp.
        guard = new ModuleGuardInterceptor(tenantModuleService, new ObjectMapper().findAndRegisterModules());
        TenantContext.setTenantId("tenant-1");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1" + path);
        request.setContextPath("/api/v1");
        return request;
    }

    @Test
    void blocksADisabledModuleWith403() throws Exception {
        when(tenantModuleService.isModuleEnabled("tenant-1", ModuleCode.SALES)).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(guard.preHandle(request("/sales/invoices"), response, null)).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("MODULE_DISABLED", "Sales module is turned off");
    }

    @Test
    void letsEnabledModulesAndUnmappedPathsThrough() throws Exception {
        when(tenantModuleService.isModuleEnabled("tenant-1", ModuleCode.INVENTORY)).thenReturn(true);

        assertThat(guard.preHandle(request("/inventory/products"), new MockHttpServletResponse(), null)).isTrue();
        assertThat(guard.preHandle(request("/company"), new MockHttpServletResponse(), null)).isTrue();
        assertThat(guard.preHandle(request("/salesforce"), new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void partiesStayOpenWhileEitherSalesOrPurchaseIsOn() throws Exception {
        when(tenantModuleService.isModuleEnabled("tenant-1", ModuleCode.SALES)).thenReturn(false);
        when(tenantModuleService.isModuleEnabled("tenant-1", ModuleCode.PURCHASE)).thenReturn(true);

        assertThat(guard.preHandle(request("/parties"), new MockHttpServletResponse(), null)).isTrue();
        assertThat(ModuleGuardInterceptor.modulesFor("/parties/4")).isEqualTo(List.of(ModuleCode.SALES, ModuleCode.PURCHASE));
    }
}
