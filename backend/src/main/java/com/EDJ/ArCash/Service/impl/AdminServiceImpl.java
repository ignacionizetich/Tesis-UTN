package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.SessionService;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.interfaces.AdminService;
import com.EDJ.ArCash.Service.result.*;

import com.EDJ.ArCash.DTO.AuthDTO.AdminRequest;
import com.EDJ.ArCash.DTO.AuthDTO.UserResponse;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountService accountService;

  @Autowired
  private SessionService sessionService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public List<UserResponse> getAuthUsers() {
    List<User> users = userRepository.findByEnabledTrue();
    return users.stream()
      .map(user -> {
        Long idAccount = user.getAccounts() != null && !user.getAccounts().isEmpty()
          ? user.getAccounts().get(0).getIdAccount()
          : null;
        String username = user.getAlias() != null ? user.getAlias() : null;
        return new UserResponse(
          user.getId(),
          user.getName(),
          user.getLastName(),
          user.getDni(),
          user.getEmail(),
          username,
          idAccount,
          user.isEnabled(),
          user.isActive(),
          user.getPermissions(),
          user.getCreationDate()
        );
      })
      .toList();
  }

  // ------------------------------------------------------------------
  // Gestión de cuentas USER (endpoint genérico /users/{id}/disable|enable)
  // ------------------------------------------------------------------

  // Soft-disable: marca inactive y corta la sesion de inmediato (revoca refresh).
  @Transactional
  public void disableUser(Long userId) {
    User user = getUserOrThrow(userId);
    requirePermission(user, Permissions.USER,
      "Para deshabilitar una cuenta con permisos de administrador usá /users/{id}/disable-admin");

    user.setActive(false);
    userRepository.save(user);
    sessionService.revokeAllUserTokens(userId);
  }

  @Transactional
  public void enableUser(Long userId) {
    User user = getUserOrThrow(userId);
    requirePermission(user, Permissions.USER,
      "Para habilitar una cuenta con permisos de administrador usá /users/{id}/enable-admin");

    user.setActive(true);
    // Si nunca valido el email, puede no tener cuenta ARS; sin ella el login falla.
    accountService.ensureArsAccount(user);
    userRepository.save(user);
  }

  // ------------------------------------------------------------------
  // Gestión de cuentas ADMIN (endpoints exclusivos de ROOT)
  // ------------------------------------------------------------------

  @Transactional
  public void disableAdmin(Long userId) {
    User user = getUserOrThrow(userId);
    requirePermission(user, Permissions.ADMIN,
      "Esta operación solo aplica a cuentas con permisos de administrador");

    user.setActive(false);
    userRepository.save(user);
    sessionService.revokeAllUserTokens(userId);
  }

  @Transactional
  public void enableAdmin(Long userId) {
    User user = getUserOrThrow(userId);
    requirePermission(user, Permissions.ADMIN,
      "Esta operación solo aplica a cuentas con permisos de administrador");

    user.setActive(true);
    userRepository.save(user);
  }

  // ------------------------------------------------------------------
  // Creación de administradores (exclusivo de ROOT vía @PreAuthorize)
  // ------------------------------------------------------------------

  @Transactional
  public AdminCreateResult createAdmin(AdminRequest adminRequest) {
    try {
      if (existsByUsername(adminRequest.getUsername())) {
        return AdminCreateResult.conflict("username", "nombre de usuario no está disponible");
      }
      if (existsByEmail(adminRequest.getEmail())) {
        return AdminCreateResult.conflict("email", "email ya se encuentra en uso");
      }
      if (existsByDni(adminRequest.getDni())) {
        return AdminCreateResult.conflict("dni", "DNI ya está registrado");
      }

      User user = new User();
      user.setName(capitalizePersonName(adminRequest.getName()));
      user.setLastName(capitalizePersonName(adminRequest.getLastName()));
      user.setPermissions(Permissions.ADMIN);
      user.setDni(adminRequest.getDni());
      user.setEmail(adminRequest.getEmail());
      user.setAlias(adminRequest.getUsername());
      user.setEnabled(true);
      user.setActive(true);
      user.setCredentials(new Credentials(
        user,
        user.getAlias(),
        passwordEncoder.encode(adminRequest.getPassword())
      ));

      try {
        cargarAdmin(user);
        return AdminCreateResult.success();
      } catch (DataIntegrityViolationException e) {
        if (existsByUsername(adminRequest.getUsername())) {
          return AdminCreateResult.conflict("username", "nombre de usuario no está disponible");
        }
        if (existsByEmail(adminRequest.getEmail())) {
          return AdminCreateResult.conflict("email", "email ya se encuentra en uso");
        }
        if (existsByDni(adminRequest.getDni())) {
          return AdminCreateResult.conflict("dni", "DNI ya está registrado");
        }
        return AdminCreateResult.conflictGeneric();
      }
    } catch (Exception e) {
      return AdminCreateResult.error();
    }
  }

  public void cargarAdmin(User user) {
    userRepository.save(user);
    accountService.createAccount(user);
  }

  public boolean existsByUsername(String username) {
    return userRepository.existsByAlias(username);
  }

  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  public boolean existsByDni(String dni) {
    return userRepository.existsByDni(dni);
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  private User getUserOrThrow(Long userId) {
    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      throw new IllegalArgumentException("Usuario no encontrado");
    }
    return userOpt.get();
  }

  /** Corta la operación si el target no tiene exactamente el nivel de permiso esperado. */
  private void requirePermission(User user, Permissions expected, String errorMessage) {
    if (user.getPermissions() != expected) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  private static String capitalizePersonName(String value) {
    return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
  }
}
