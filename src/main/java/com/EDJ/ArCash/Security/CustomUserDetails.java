package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // V--- CAMBIO #1: AHORA SÍ LE DAMOS LOS PERMISOS REALES ---V
        // Leemos el permiso del usuario y se lo damos a Spring Security
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getPermissions().name()));
    }

    @Override
    public String getPassword() {
        // Esto asume que la relación user.getCredentials() nunca es nula para un usuario autenticado
        return user.getCredentials().getPass();
    }

    @Override
    public String getUsername() {
        // V--- CAMBIO #2: USAMOS EL USERNAME REAL, NO EL ALIAS ---V
        return user.getCredentials().getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
