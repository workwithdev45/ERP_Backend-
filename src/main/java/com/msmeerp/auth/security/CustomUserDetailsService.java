package com.msmeerp.auth.security;

import com.msmeerp.accesscontrol.entity.Role;
import com.msmeerp.accesscontrol.repository.RoleModulePermissionRepository;
import com.msmeerp.accesscontrol.repository.UserModulePermissionRepository;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.user.entity.User;
import com.msmeerp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserModulePermissionRepository userModulePermissionRepository;
    private final RoleModulePermissionRepository roleModulePermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String tenantId = TenantContext.getTenantId();

        User user;
        if (tenantId != null && !tenantId.equalsIgnoreCase("msmeerp-main")) {
            user = userRepository.findByTenantIdAndUsername(tenantId, usernameOrEmail)
                    .or(() -> userRepository.findByTenantIdAndEmail(tenantId, usernameOrEmail))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username/email: " + usernameOrEmail + " for tenant: " + tenantId));
        } else {
            user = userRepository.findByTenantIdAndUsername(tenantId, usernameOrEmail)
                    .or(() -> userRepository.findByTenantIdAndEmail(tenantId, usernameOrEmail))
                    .or(() -> userRepository.findByUsername(usernameOrEmail))
                    .or(() -> userRepository.findByEmail(usernameOrEmail))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username/email: " + usernameOrEmail));
        }

        var modulePermissions = userModulePermissionRepository.findByTenantIdAndUserId(user.getTenantId(), user.getId());
        var roleIds = user.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        var roleModulePermissions = roleModulePermissionRepository.findByTenantIdAndRoleIdIn(user.getTenantId(), roleIds);
        return UserPrincipal.create(user, modulePermissions, roleModulePermissions);
    }
}
