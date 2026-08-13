package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.Imp.LogoutStatus;
import com.EDJ.ArCash.Service.result.RecoverMailResult;
import com.EDJ.ArCash.Service.result.RecoveryTokenValidationResult;
import com.EDJ.ArCash.Service.result.RefreshAccessResult;
import com.EDJ.ArCash.Service.result.ResendEmailResult;
import com.EDJ.ArCash.Service.result.SessionCheckResult;

public interface AuthService {
    public LoginResponse login(LoginRequest loginRequest);

    public LogoutStatus logout(String accessToken);

    public boolean isValidSession(String token);

    public boolean enviarCorreoRecuperacion(String email);

    public boolean tokenValido(String tokenValue);

    public boolean resendPasswordRecovery(String email);

    public RefreshAccessResult refreshAccessToken(String refreshToken);

    public SessionCheckResult checkSession(String authHeader);

    public RecoverMailResult sendRecoverMail(String email);

    public RecoveryTokenValidationResult validateRecoveryToken(String token);

    public ResendEmailResult resendPasswordRecoveryEmail(String email);

}
