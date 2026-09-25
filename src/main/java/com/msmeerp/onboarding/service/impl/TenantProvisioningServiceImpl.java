package com.msmeerp.onboarding.service.impl;

import com.msmeerp.accesscontrol.entity.ModuleCode;
import com.msmeerp.accesscontrol.entity.Permission;
import com.msmeerp.accesscontrol.entity.Role;
import com.msmeerp.accesscontrol.repository.PermissionRepository;
import com.msmeerp.accesscontrol.repository.RoleRepository;
import com.msmeerp.accesscontrol.service.TenantModuleService;
import com.msmeerp.onboarding.service.TenantProvisioningService;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.entity.UserTenantMap;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.tenant.repository.UserTenantMapRepository;
import com.msmeerp.user.entity.User;
import com.msmeerp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningServiceImpl implements TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserTenantMapRepository userTenantMapRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantModuleService tenantModuleService;

    @Override
    @Transactional
    public Tenant provisionTenant(String adminEmail, String portalId, boolean startBlank, String businessType) {
        String cleanPortalId = portalId.toLowerCase().trim();
        String displayName = formatDisplayName(cleanPortalId);
        String tenantUuid = UUID.randomUUID().toString();

        log.info("Provisioning ERP tenant: {} (UUID: {}, startBlank: {}) for admin: {}", cleanPortalId, tenantUuid, startBlank, adminEmail);

        // 1. Create Tenant Record
        Tenant tenant = Tenant.builder()
                .id(tenantUuid)
                .name(displayName)
                .displayName(displayName)
                .subdomain(cleanPortalId)
                .portalId(cleanPortalId)
                .adminEmail(adminEmail)
                .businessType(businessType)
                .subscriptionTier("Standard")
                .timezone("Asia/Kolkata")
                .locale("en_IN")
                .currency("INR")
                .firstLoginPending(true)
                .active(true)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        // 2. Ensure standard ERP module permissions exist in the central catalog
        List<Permission> allPermissions = ensureStandardPermissions();

        // 2b. G14/G10: seed this tenant's module switches, then narrow them to what a
        // business of this type typically needs (still just a preset — an Admin can flip
        // any module back on later from Settings -> Modules).
        tenantModuleService.initializeDefaultModules(tenantUuid);
        applyModulePreset(tenantUuid, businessType);

        // 3. Seed the standard tenant-scoped roles: ADMIN (tenant owner) and USER (staff, module-permission scoped)
        Role adminRole = createRole(tenantUuid, "ADMIN", "Owns this company's ERP workspace: manages users, roles, and module permissions", new HashSet<>(allPermissions));
        createRole(tenantUuid, "USER", "Standard staff access, scoped to individually assigned module permissions", new HashSet<>());

        // 4. Create the tenant Admin user (password placeholder, activated when they set a password)
        String placeholderPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        String defaultUsername = adminEmail.split("@")[0];

        User adminUser = User.builder()
                .username(defaultUsername)
                .email(adminEmail)
                .password(placeholderPassword)
                .firstName("Admin")
                .lastName("Owner")
                .phoneNumber(null)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .roles(Set.of(adminRole))
                .build();
        adminUser.setTenantId(tenantUuid);
        userRepository.save(adminUser);

        // 5. Track this email against the tenant for multi-company lookup
        updateUserTenantMap(adminEmail, cleanPortalId);

        log.info("Successfully provisioned ERP tenant portal: {} (tenantId: {})", cleanPortalId, tenantUuid);
        return savedTenant;
    }

    private Role createRole(String tenantId, String roleName, String description, Set<Permission> permissions) {
        Role role = Role.builder()
                .name(roleName)
                .description(description)
                .systemRole(true)
                .permissions(permissions)
                .build();
        role.setTenantId(tenantId);
        return roleRepository.save(role);
    }

    private List<Permission> ensureStandardPermissions() {
        List<Permission> existing = permissionRepository.findAll();
        if (!existing.isEmpty()) {
            return existing;
        }

        List<Permission> permissionsToCreate = List.of(
                // Sales
                Permission.builder().name("SALES_READ").module("SALES").description("View quotes, sales orders, and invoices").build(),
                Permission.builder().name("SALES_WRITE").module("SALES").description("Create and edit sales orders and invoices").build(),
                Permission.builder().name("SALES_DELETE").module("SALES").description("Delete or void sales records").build(),
                // Purchase
                Permission.builder().name("PURCHASE_READ").module("PURCHASE").description("View vendors, purchase orders, and bills").build(),
                Permission.builder().name("PURCHASE_WRITE").module("PURCHASE").description("Create and edit purchase orders and vendor bills").build(),
                Permission.builder().name("PURCHASE_DELETE").module("PURCHASE").description("Delete or void purchase records").build(),
                // Inventory
                Permission.builder().name("INVENTORY_READ").module("INVENTORY").description("View stock items, warehouses, and stock levels").build(),
                Permission.builder().name("INVENTORY_WRITE").module("INVENTORY").description("Manage stock items and record stock movements").build(),
                Permission.builder().name("INVENTORY_DELETE").module("INVENTORY").description("Delete stock items or warehouses").build(),
                // Production
                Permission.builder().name("PRODUCTION_READ").module("PRODUCTION").description("View bills of materials and work orders").build(),
                Permission.builder().name("PRODUCTION_WRITE").module("PRODUCTION").description("Create and manage work orders and BOMs").build(),
                // Accounts
                Permission.builder().name("ACCOUNTS_READ").module("ACCOUNTS").description("View ledgers, journal entries, and payments").build(),
                Permission.builder().name("ACCOUNTS_WRITE").module("ACCOUNTS").description("Record journal entries and payments").build(),
                // CRM
                Permission.builder().name("CRM_READ").module("CRM").description("View leads and pipeline").build(),
                Permission.builder().name("CRM_WRITE").module("CRM").description("Manage leads, follow-ups, and conversions").build(),
                // HR
                Permission.builder().name("HR_READ").module("HR").description("View employee records and attendance").build(),
                Permission.builder().name("HR_WRITE").module("HR").description("Manage employee records, attendance, and payroll runs").build(),
                // Reports
                Permission.builder().name("REPORTS_READ").module("REPORTS").description("View cross-module analytics and exports").build(),
                // Admin
                Permission.builder().name("USER_MANAGE").module("ADMIN").description("Manage staff and user accounts").build(),
                Permission.builder().name("ROLE_MANAGE").module("ADMIN").description("Manage access roles and permissions").build(),
                Permission.builder().name("SETTINGS_MANAGE").module("ADMIN").description("Manage company/tenant settings").build()
        );

        return permissionRepository.saveAll(permissionsToCreate);
    }

    private void updateUserTenantMap(String email, String portalId) {
        UserTenantMap map = userTenantMapRepository.findByEmail(email)
                .orElse(UserTenantMap.builder()
                        .email(email)
                        .tenantIds(portalId)
                        .build());

        if (map.getId() != null) {
            Set<String> set = new HashSet<>(Arrays.asList(map.getTenantIds().split(",")));
            set.add(portalId);
            map.setTenantIds(String.join(",", set));
        }
        userTenantMapRepository.save(map);
    }

    /** G10: a starting point only — every module stays reachable from Settings -> Modules afterwards. */
    private void applyModulePreset(String tenantId, String businessType) {
        if (businessType == null) {
            return;
        }
        switch (businessType.toUpperCase()) {
            case "TRADER" -> tenantModuleService.setModuleEnabledForTenant(tenantId, ModuleCode.PRODUCTION, false);
            case "SERVICES" -> {
                tenantModuleService.setModuleEnabledForTenant(tenantId, ModuleCode.PRODUCTION, false);
                tenantModuleService.setModuleEnabledForTenant(tenantId, ModuleCode.INVENTORY, false);
            }
            default -> { /* MANUFACTURER (or unrecognized) keeps every module enabled */ }
        }
    }

    private String formatDisplayName(String portalId) {
        String[] parts = portalId.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
