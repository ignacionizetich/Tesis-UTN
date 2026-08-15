package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    Permissions permissions = user.getPermissions();

    if (permissions == Permissions.ROOT) {
      // ROOT hereda todo lo que puede hacer ADMIN (rutas /api/admin/**),
      // más ROLE_ROOT para los endpoints exclusivos de gestión de admins.
      return List.of(
        new SimpleGrantedAuthority("ROLE_ROOT"),
        new SimpleGrantedAuthority("ROLE_ADMIN")
      );
    }

    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + permissions.name()));
  }

  @Override
  public String getPassword() {
    // Esto asume que la relación user.getCredentials() nunca es nula para un usuario autenticado
    return user.getCredentials().getPass();
  }

  @Override
  public String getUsername() {
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
