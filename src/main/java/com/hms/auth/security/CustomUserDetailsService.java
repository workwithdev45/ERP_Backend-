package com.hms.auth.security;

import com.hms.tenant.context.TenantContext;
import com.hms.user.entity.User;
import com.hms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String tenantId = TenantContext.getTenantId();

        User user;
        if (tenantId != null && !tenantId.equalsIgnoreCase("hms-main")) {
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

        return UserPrincipal.create(user);
    }
}
