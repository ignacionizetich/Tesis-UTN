
package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Repository.ValidationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ValidationTokenService {

    private final ValidationTokenRepository validationTokenRepository;

    public ValidationTokenService(ValidationTokenRepository validationTokenRepository) {
        this.validationTokenRepository = validationTokenRepository;
    }



    @Transactional
    public void usedToken(User user){
        ValidationToken token = validationTokenRepository.findByUser(user);
        if (token != null) {
            token.setUsed(true);
            validationTokenRepository.save(token);
        }
    }

    public Optional<ValidationToken> buscarToken(String token){
        return validationTokenRepository.findByToken(token);
    }

    /**
     * Crea un nuevo token de validación para el usuario, o actualiza el existente
     * @param user Usuario para el cual crear el token
     * @return El token de validación (nuevo o actualizado)
     */
    @Transactional
    public ValidationToken createNewToken(User user) {
        try {
            // Buscar si ya existe un token para este usuario
            ValidationToken existingToken = validationTokenRepository.findByUser(user);
            
            if (existingToken != null) {
                // Si existe, actualizar el token existente
                existingToken.regenerateToken(); // Regenerar el valor del token
                existingToken.setUsed(false); // Marcar como no usado
                // Guardar el token actualizado
                return validationTokenRepository.save(existingToken);
            } else {
                // Si no existe, crear un nuevo token
                ValidationToken newToken = new ValidationToken(user);
                return validationTokenRepository.save(newToken);
            }
        } catch (Exception e) {
            System.err.println("Error en createNewToken: " + e.getMessage());
            throw e;
        }
    }

}