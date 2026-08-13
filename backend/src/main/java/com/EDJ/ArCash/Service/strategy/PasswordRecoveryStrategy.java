package com.EDJ.ArCash.Service.strategy;

public interface PasswordRecoveryStrategy {
    
    boolean sendRecoveryEmail(String email);
    
    boolean validateRecoveryToken(String token);
    
    boolean resendRecoveryLink(String email);
}
