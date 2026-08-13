package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.Imp.LogoutStatus;

public interface TokenManagementStrategy {
    
    String generateAccessToken(String userId, String role);
    
    String generateRefreshToken(String userId, String role);
    
    void saveRefreshToken(User user, String refreshToken);
    
    LogoutStatus revokeUserTokens(String accessToken);
    
    String getActiveRefreshToken(User user);
    
    String extractUserId(String token);

    boolean isValidSession(String accessToken);
}
