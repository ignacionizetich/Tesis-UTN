package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.io.UnsupportedEncodingException;

import static org.apache.commons.lang3.StringUtils.capitalize;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final AccountService accountService;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final ValidationTokenService validationTokenService;

    public UserService(PasswordEncoder passwordEncoder,UserRepository userRepository, AccountService accountService, CredentialsService credentialsService, EmailService emailService, ValidationTokenService validationTokenService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.validationTokenService = validationTokenService;
    }


    public void insertarUsuario(User user, String rawPassword) {
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

        // Enviar mail asincrónico
        emailService.sendVerificationEmail(user, user.getValidationToken().getToken());
    }


    public void validarUsuario(User user){
        accountService.createAccount(user, "PESOS"); ///UNA VEZ VALIDE SU CUENTA
        user.setEnabled(true);
        user.setActive(true);
        userRepository.save(user);
        validationTokenService.usedToken(user);
    }



}
