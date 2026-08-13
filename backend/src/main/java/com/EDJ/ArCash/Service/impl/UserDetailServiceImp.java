package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.UserDetailService;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Security.CustomUserDetails; // <-- La clave es que devuelva ESTA clase
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImp implements UserDetailService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    public UserDetailServiceImp(UserRepository userRepository, CredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException {

        try {
            // Caso 1: El filtro JWT nos pasa un ID (ej: "15")
            Long userId = Long.parseLong(usernameOrId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + userId));

            // Devolvemos TU clase personalizada
            return new CustomUserDetails(user);

        } catch (NumberFormatException e) {
            // Caso 2: El login nos pasa un username (ej: "ElNizee")
            Credentials credentials = credentialRepository.findByUsername(usernameOrId)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username: " + usernameOrId));

            // Devolvemos TU clase personalizada
            return new CustomUserDetails(credentials.getUser());
        }
    }
}