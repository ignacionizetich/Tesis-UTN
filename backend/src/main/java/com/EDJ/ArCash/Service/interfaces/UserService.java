package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.NonAuthDTO.RegistrerRequest;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.result.EmailActivationResult;
import com.EDJ.ArCash.Service.result.RegisterResult;
import com.EDJ.ArCash.Service.result.ResendEmailResult;
import com.EDJ.ArCash.Service.result.UserDataView;
import com.EDJ.ArCash.Service.result.UsernameChangeResult;
import java.util.Optional;

public interface UserService {
    public void insertarUsuario(User user, String rawPassword) throws RuntimeException;

    public void register(RegistrerRequest dto);

    public RegisterResult registerFromRequest(RegistrerRequest dto);

    public Optional<UserDataView> getUserData(User user);

    public void validarUsuario(User user);

    public EmailActivationResult activateWithToken(String tokenValue);

    public boolean resendValidationEmail(String email);

    public ResendEmailResult resendValidationEmailRequest(String email);

    public Optional<User> findUserByAlias(String alias);

    public UsernameChangeResult changeUsername(Long userId, String newUsername);

    public boolean cambiarAliasYUsername(Long userId, String nuevoAlias);

}
