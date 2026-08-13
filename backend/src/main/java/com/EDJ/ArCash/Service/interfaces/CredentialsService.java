package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.result.ResetPasswordResult;

public interface CredentialsService {
    public void createCredentials(User user, String rawPassword);

    public ResetPasswordResult actualizarPassword(String tokenValue, String nuevaPassword, String confirmarPassword);

}
