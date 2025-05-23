package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.User;
import org.springframework.security.core.GrantedAuthority;
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
        // Agregar roles si los tenés, por ahora vacío
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return user.getCredentials().getPass();
    }

    @Override
    public String getUsername() {
        return user.getAlias();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // podés controlar esto si querés
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // idem
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
