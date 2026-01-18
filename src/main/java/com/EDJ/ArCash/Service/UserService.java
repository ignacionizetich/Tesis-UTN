package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.capitalize;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final AccountService accountService;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final ValidationTokenService validationTokenService;

    private final EventPublisher eventPublisher;

    public UserService(PasswordEncoder passwordEncoder,UserRepository userRepository, AccountService accountService, CredentialsService credentialsService, EmailService emailService, ValidationTokenService validationTokenService, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.validationTokenService = validationTokenService;
        this.eventPublisher = eventPublisher;
    }


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
        user.setCredentials(new Credentials(user, user.getAlias(), passwordEncoder.encode(rawPassword)));
        user.setValidationToken(new ValidationToken(user));

        // Guardar todo de una sola vez
        userRepository.save(user);

        // Publicar evento de usuario registrado
        Event event = new Event(EventType.USER_REGISTERED);
        event.addData("user", user);
        event.addData("token", user.getValidationToken().getToken());
        eventPublisher.publish(event);

        // El email se enviará a través del observer, comentamos el envío directo
        // emailService.sendVerificationEmail(user, user.getValidationToken().getToken());
    }
    
    private void validateUserConflicts(User user) throws RuntimeException {
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        // Verificar si el email ya existe
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            errors.add("EMAIL_ALREADY_EXISTS");
        }
        
        // Verificar si el alias ya existe
        if (findByAlias(user.getAlias()) != null) {
            errors.add("ALIAS_ALREADY_EXISTS");
        }
        
        // Verificar si el DNI ya existe
        if (findByDni(user.getDni()) != null) {
            errors.add("DNI_ALREADY_EXISTS");
        }
        
        // Si hay errores, lanzar excepción con todos los conflictos
        if (!errors.isEmpty()) {
            throw new RuntimeException(String.join(",", errors));
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
        accountService.createAccount(user, "PESOS"); ///UNA VEZ VALIDE SU CUENTA
        user.setEnabled(true);
        user.setActive(true);
        userRepository.save(user);
        validationTokenService.usedToken(user);
    }

    /**
     * Reenvía el enlace de validación de email para usuarios no validados
     * @param email Email del usuario
     * @return true si se envió exitosamente, false si no se pudo enviar
     */
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



}
