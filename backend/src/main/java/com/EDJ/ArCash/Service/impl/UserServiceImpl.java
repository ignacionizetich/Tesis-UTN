package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.ValidationTokenService;
import com.EDJ.ArCash.Service.interfaces.EmailService;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.interfaces.UserService;
import com.EDJ.ArCash.Service.result.*;

import com.EDJ.ArCash.DTO.NonAuthDTO.RegistrerRequest;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.exception.personalizated.PasswordMissmatchException;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.capitalize;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final AccountService accountService;

    private final AccountRepository accountRepository;

    private final CredentialRepository credentialRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final ValidationTokenService validationTokenService;

    private final EventPublisher eventPublisher;



    public void insertarUsuario(User user, String rawPassword) throws RuntimeException {
        // Validar múltiples conflictos
        validateUserConflicts(user);

        // Formatear nombres
        user.setName(capitalize(user.getName()));
        user.setLastName(capitalize(user.getLastName()));
        user.setEnabled(false);
        user.setActive(false);
        user.setPermissions(Permissions.USER);

        // Crear credenciales y token en cascada

      if(user.getEmail().equals(rawPassword)){
        throw new PasswordMissmatchException("La contraseña no puede ser igual a tu email, por favor elija otra");
      }

        user.setCredentials(new Credentials(user, user.getAlias(), passwordEncoder.encode(rawPassword)));
        user.setValidationToken(new ValidationToken(user));

        // Guardar todo de una sola vez
        userRepository.save(user);

        // Publicar evento de usuario registrado
        Event event = new Event(EventType.USER_REGISTERED);
        event.addData("user", user);
        event.addData("token", user.getValidationToken().getToken());
        eventPublisher.publish(event);

        // emailService.sendVerificationEmail(user, user.getValidationToken().getToken());
    }

    /**
     * Registro publico: construye el User desde el DTO (el controller no arma entidades).
     */
    public void register(RegistrerRequest dto) {
        User user = new User(dto.getName(), dto.getLastName(), dto.getDni(), dto.getEmail(), dto.getAlias());
        insertarUsuario(user, dto.getPassword());
    }

    /**
     * Registro con validacion de campos obligatorios y mapeo de conflictos.
     */
    public RegisterResult registerFromRequest(RegistrerRequest dto) {
        if (dto.getName() == null || dto.getEmail() == null || dto.getPassword() == null || dto.getAlias() == null) {
            return RegisterResult.validation("Todos los campos son obligatorios.");
        }
        try {
            register(dto);
            return RegisterResult.ok();
        } catch (RegistrationConflictException e) {
            return RegisterResult.conflict(RegistrationConflictMessages.format(e.getCodes()));
        } catch (Exception e) {
            return RegisterResult.error();
        }
    }

    /**
     * Datos de perfil del autenticado (User + cuenta ARS primaria).
     */
    public Optional<UserDataView> getUserData(User user) {
        Optional<Account> optionalAccount = accountRepository.findByUser_Id(user.getId());
        if (optionalAccount.isEmpty()) {
            return Optional.empty();
        }
        Account account = optionalAccount.get();
        return Optional.of(new UserDataView(
                user.getName(),
                user.getLastName(),
                user.getDni(),
                user.getEmail(),
                user.getAlias(),
                account.getAccountNickname(),
                account.getIdAccount(),
                account.getAccountCvu(),
                account.getBalance()
        ));
    }

    private void validateUserConflicts(User user) {
        java.util.List<RegistrationConflictCode> errors = new java.util.ArrayList<>();

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            errors.add(RegistrationConflictCode.EMAIL_ALREADY_EXISTS);
        }

        if (findByAlias(user.getAlias()) != null) {
            errors.add(RegistrationConflictCode.ALIAS_ALREADY_EXISTS);
        }

        if (findByDni(user.getDni()) != null) {
            errors.add(RegistrationConflictCode.DNI_ALREADY_EXISTS);
        }

        if (!errors.isEmpty()) {
            throw new RegistrationConflictException(errors);
        }
    }

    private User findByAlias(String alias) {
        // Usar el método del repository para búsqueda por alias
        return userRepository.findByAlias(alias).orElse(null);
    }

    private User findByDni(String dni) {
        // Usar el método del repository para búsqueda por DNI
        return userRepository.findByDni(dni).orElse(null);
    }

    public void validarUsuario(User user){
        accountService.createAccount(user); ///UNA VEZ VALIDE SU CUENTA
        user.setEnabled(true);
        user.setActive(true);
        userRepository.save(user);
        validationTokenService.usedToken(user);
    }

    /**
     * Activa la cuenta a partir del token de verificacion de email (flujo /api/auth/validate).
     */
    @Transactional
    public EmailActivationResult activateWithToken(String tokenValue) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            return EmailActivationResult.missingToken();
        }

        Optional<ValidationToken> optionalToken = validationTokenService.buscarToken(tokenValue);
        if (optionalToken.isEmpty()) {
            return EmailActivationResult.invalid();
        }

        ValidationToken token = optionalToken.get();
        if (token.isUsed()) {
            return EmailActivationResult.alreadyUsed();
        }
        if (token.getExpirationDate().isBefore(java.time.LocalDateTime.now())) {
            return EmailActivationResult.expired();
        }

        validarUsuario(token.getUser());
        return EmailActivationResult.ok();
    }

    public boolean resendValidationEmail(String email) {
        try {
            // Buscar el usuario por email
            java.util.Optional<User> optionalUser = userRepository.findByEmail(email);

            if (optionalUser.isEmpty()) {
                return false; // Usuario no existe
            }

            User user = optionalUser.get();

            // Verificar que el usuario no esté ya validado
            if (user.isEnabled()) {
                return false; // Usuario ya está validado
            }

            // Crear un nuevo token de validación (invalidar el anterior)
            ValidationToken newToken = validationTokenService.createNewToken(user);

            // Enviar el nuevo email de validación
            emailService.sendVerificationEmail(user, newToken.getToken());

            return true;

        } catch (Exception e) {
            // Log del error (opcional)
            System.err.println("Error al reenviar email de validación: " + e.getMessage());
            return false;
        }
    }

    public ResendEmailResult resendValidationEmailRequest(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResendEmailResult.emailRequired();
            }
            resendValidationEmail(email.trim());
            return ResendEmailResult.ok(
                    "Si el email corresponde a una cuenta pendiente de validación, te enviamos un nuevo enlace.");
        } catch (Exception e) {
            return ResendEmailResult.error();
        }
    }

    public Optional<User> findUserByAlias(String alias){
        return userRepository.findByAlias(alias);
    }

    @Transactional
    /**
     * Cambio de username/alias del autenticado (incluye validacion de vacio del HTTP actual).
     */
    public UsernameChangeResult changeUsername(Long userId, String newUsername) {
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return UsernameChangeResult.empty();
        }
        boolean ok = cambiarAliasYUsername(userId, newUsername.trim());
        return ok ? UsernameChangeResult.ok() : UsernameChangeResult.fail();
    }

    public boolean cambiarAliasYUsername(Long userId, String nuevoAlias) {
        logger.info("Cambiando alias para usuario: {}", userId);

        String regex = "^(?=.*[A-Za-z])[A-Za-z\\d]{4,25}$";
        if (nuevoAlias == null || nuevoAlias.trim().isEmpty() ||
                !nuevoAlias.matches(regex) ||
                nuevoAlias.matches("^\\d+$")) {
            logger.warn("Formato de alias inválido para usuario: {}", userId);
            return false;
        }

        Optional<Account> accountOpt = accountRepository.findByUser_Id(userId);
        if (accountOpt.isEmpty()) {
            logger.warn("Cuenta no encontrada para usuario: {}", userId);
            return false;
        }

        Credentials credentials = accountOpt.get().getUser().getCredentials();
        User user = accountOpt.get().getUser();

        if (credentialRepository.findByUsername(nuevoAlias).isPresent()) {
            logger.warn("El alias ya está en uso: {}", nuevoAlias);
            return false;
        }

        String oldAlias = user.getAlias();
        user.setAlias(nuevoAlias);
        userRepository.saveAndFlush(user);

        credentials.setUsername(nuevoAlias);
        credentialRepository.save(credentials);

        Event event = new Event(EventType.ALIAS_CHANGED);
        event.addData("user", user);
        event.addData("oldAlias", oldAlias);
        event.addData("newAlias", nuevoAlias);
        eventPublisher.publish(event);

        logger.info("Alias cambiado exitosamente para usuario: {}", userId);
        return true;
    }
}
