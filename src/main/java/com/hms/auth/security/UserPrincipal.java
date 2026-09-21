package com.hms.auth.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hms.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
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
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add Roles
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
            // Add Permissions
            role.getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.getName().toUpperCase()))
            );
        });

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
