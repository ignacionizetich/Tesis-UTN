package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.RecoveryToken;
import com.EDJ.ArCash.Models.User;

public interface RecoveryTokenService {
    public String createRecoveryToken(User user);

}
