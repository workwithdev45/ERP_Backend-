package com.msmeerp.accesscontrol.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.service.TenantModuleService;
import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.tenant.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

/**
 * G14 backend guard: a module the company has switched off is off for its API too, not just hidden
 * in the sidebar. Requests to a disabled module's endpoints get 403 with code MODULE_DISABLED.
 * Customers/vendors (/parties) stay reachable while either Sales or Purchase is on.
 */
@Component
@RequiredArgsConstructor
public class ModuleGuardInterceptor implements HandlerInterceptor {

    public static final String ERROR_CODE = "MODULE_DISABLED";

    private static final Map<String, List<ModuleCode>> MODULE_BY_PATH = Map.of(
            "/sales", List.of(ModuleCode.SALES),
            "/purchase", List.of(ModuleCode.PURCHASE),
            "/inventory", List.of(ModuleCode.INVENTORY),
            "/parties", List.of(ModuleCode.SALES, ModuleCode.PURCHASE)
    );

    private final TenantModuleService tenantModuleService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getTenantId();
        List<ModuleCode> modules = modulesFor(request.getRequestURI().substring(request.getContextPath().length()));
        if (tenantId == null || modules == null
                || modules.stream().anyMatch(module -> tenantModuleService.isModuleEnabled(tenantId, module))) {
            return true;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Map<String, String>> body = ApiResponse.<Map<String, String>>builder()
                .error(true)
                .success(false)
                .message(label(modules) + " is turned off for your company. An admin can switch it on in Settings → Modules.")
                .data(Map.of("code", ERROR_CODE, "module", modules.get(0).name()))
                .build();
        objectMapper.writeValue(response.getOutputStream(), body);
        return false;
    }

    static List<ModuleCode> modulesFor(String path) {
        return MODULE_BY_PATH.entrySet().stream()
                .filter(e -> path.equals(e.getKey()) || path.startsWith(e.getKey() + "/"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String label(List<ModuleCode> modules) {
        return modules.size() > 1 ? "Sales and Purchase are both off, so customers and vendors are"
                : "The " + modules.get(0).name().charAt(0) + modules.get(0).name().substring(1).toLowerCase() + " module";
    }
}
