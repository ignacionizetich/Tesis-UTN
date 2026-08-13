package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.RefreshToken;
import java.util.Optional;

public interface RefreshTokenCleanupService {
    void removeExpiredOrRevokedTokens();

    Optional<RefreshToken> getRefreshTokenAndRevokedFalse(String token);
}
