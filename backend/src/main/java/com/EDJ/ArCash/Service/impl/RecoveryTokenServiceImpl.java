package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.RecoveryTokenService;

import com.EDJ.ArCash.Models.RecoveryToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.RecoveryTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecoveryTokenServiceImpl implements RecoveryTokenService {

    private final RecoveryTokenRepository recoveryTokenRepository;

    @Transactional
    public String createRecoveryToken(User user){
        // Buscar si ya existe un token para este usuario
        RecoveryToken existingToken = recoveryTokenRepository.findByUser(user);

        if (existingToken != null) {
            // Si existe, regenerar el token
            existingToken.regenerateToken();
            recoveryTokenRepository.save(existingToken);
            return existingToken.getToken();
        } else {
            // Si no existe, crear uno nuevo
            RecoveryToken recoveryToken = new RecoveryToken(user);
            recoveryTokenRepository.save(recoveryToken);
            return recoveryToken.getToken();
        }
    }
}
