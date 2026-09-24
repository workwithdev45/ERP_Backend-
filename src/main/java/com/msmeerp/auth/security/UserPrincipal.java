package com.msmeerp.auth.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.msmeerp.accesscontrol.entity.RoleModulePermission;
import com.msmeerp.accesscontrol.entity.UserModulePermission;
import com.msmeerp.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

    private Long id;
    private String tenantId;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;
    private boolean active;

    public static UserPrincipal create(User user) {
        return create(user, Collections.emptyList());
    }

    public static UserPrincipal create(User user, List<UserModulePermission> modulePermissions) {
        return create(user, modulePermissions, Collections.emptyList());
    }

    public static UserPrincipal create(User user,
                                       List<UserModulePermission> modulePermissions,
                                       List<RoleModulePermission> roleModulePermissions) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add Roles + their baked-in permissions (e.g. ADMIN's full access, USER_MANAGE, ROLE_MANAGE)
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
            role.getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.getName().toUpperCase()))
            );
        });

        // Add per-module action grants inherited from the user's roles (e.g. SALES_VIEW for a SALES_EXEC role)
        roleModulePermissions.forEach(roleModulePermission ->
                roleModulePermission.getActions().forEach(action ->
                        authorities.add(new SimpleGrantedAuthority(
                                roleModulePermission.getModuleCode().name() + "_" + action.name()))
                )
        );

        // Add fine-grained per-user, per-module action grants (e.g. SALES_VIEW, SALES_CREATE)
        modulePermissions.forEach(modulePermission ->
                modulePermission.getActions().forEach(action ->
                        authorities.add(new SimpleGrantedAuthority(
                                modulePermission.getModuleCode().name() + "_" + action.name()))
                )
        );

        return UserPrincipal.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .active(user.getStatus() == User.UserStatus.ACTIVE)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
