package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import java.util.Optional;

public interface ValidationTokenService {
    public void usedToken(User user);

    public Optional<ValidationToken> buscarToken(String token);

    public ValidationToken createNewToken(User user);

}
