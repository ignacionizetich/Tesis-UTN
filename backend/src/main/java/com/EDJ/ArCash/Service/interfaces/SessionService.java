package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.User;

public interface SessionService {
    public boolean tieneSesionActiva(Long userId);

    public void revokeAllUserTokens(Long userId);

}
