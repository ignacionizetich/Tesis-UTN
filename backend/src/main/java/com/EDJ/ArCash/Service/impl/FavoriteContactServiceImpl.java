package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.FavoriteContactService;
import com.EDJ.ArCash.Service.result.*;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.FavoriteContactRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteContactServiceImpl implements FavoriteContactService {

    private final FavoriteContactRepository favoriteContactRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;



    public boolean addFavoriteContact(Long userId, Long accountId, String contactAlias, String description) {
        try {
            // Verificar que el usuario existe
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }

            // Verificar que la cuenta existe
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            Account account = accountOpt.get();

            // Verificar que el usuario no esté intentando agregarse a sí mismo
            if (account.getUser().getId().equals(userId)) {
                return false;
            }

            // Verificar que no exista ya esta relación
            if (favoriteContactRepository.existsByOwnerAndFavoriteAccount(user, account)) {
                return false;
            }

            // Crear el contacto favorito
            FavoriteContact favoriteContact = new FavoriteContact(user, account, contactAlias);
            if (description != null && !description.trim().isEmpty()) {
                favoriteContact.setDescription(description.trim());
            }

            favoriteContactRepository.save(favoriteContact);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<FavoriteContact> getFavoriteContactsByUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }

        User user = userOpt.get();

        // Ahora buscar solo los activos y los retorno
        return favoriteContactRepository.findByOwnerAndActiveTrue(user);
    }

    // En FavoriteContactService.java
    public List<FavoriteContact> getFavoriteContactsByUserOrderedByUsage(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }

        User user = userOpt.get();
        return favoriteContactRepository.findByOwnerAndActiveTrueOrderByLastUsed(user);
    }

    // Método para actualizar lastUsed cuando se use en transferencia
    public void updateLastUsedForContact(Long favoriteContactId) {
        Optional<FavoriteContact> favoriteOpt = favoriteContactRepository.findById(favoriteContactId);
        if (favoriteOpt.isPresent()) {
            FavoriteContact favorite = favoriteOpt.get();
            favorite.updateLastUsed(); // Ya tienes este método implementado
            favoriteContactRepository.save(favorite);
        }
    }

    public boolean removeFavoriteContact(Long userId, Long favoriteContactId) {
        try {
            Optional<FavoriteContact> favoriteOpt = favoriteContactRepository.findById(favoriteContactId);
            if (favoriteOpt.isEmpty()) {
                return false;
            }

            FavoriteContact favorite = favoriteOpt.get();

            // Verificar que el contacto favorito pertenece al usuario
            if (!favorite.getOwner().getId().equals(userId)) {
                return false;
            }

            // Soft delete - marcar como inactivo
            favorite.setActive(false);
            favoriteContactRepository.save(favorite);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public FavoriteUpdateResult updateFavoriteContactForOwner(
            Long contactId, Long userId, String newAlias, String newDescription) {
        if ((newAlias == null || newAlias.trim().isEmpty()) && newDescription == null) {
            return FavoriteUpdateResult.badRequest();
        }
        boolean success = updateFavoriteContact(contactId, userId, newAlias, newDescription);
        return success ? FavoriteUpdateResult.ok() : FavoriteUpdateResult.notFound();
    }

    @Transactional
    public boolean updateFavoriteContact(Long contactId, Long userId, String newAlias, String newDescription) {
        try {

            Optional<FavoriteContact> optionalContact = favoriteContactRepository.findById(contactId);

            if (optionalContact.isEmpty()) {
                System.out.println("ERROR: Contacto favorito no encontrado con ID: " + contactId);
                return false;
            }

            FavoriteContact contact = optionalContact.get();
            // Verificar que el contacto pertenece al usuario
            if (!contact.getOwner().getId().equals(userId)) {
                return false;
            }

            // Verificar que el contacto esté activo
            if (!contact.isActive()) {
                System.out.println("ERROR: El contacto está inactivo");
                return false;
            }

            if (newAlias != null && !newAlias.trim().isEmpty()) {
                contact.setContactAlias(newAlias.trim());
            }

            if (newDescription != null) {
                contact.setDescription(newDescription.trim());
            }

            favoriteContactRepository.save(contact);
            return true;

        } catch (Exception e) {
            System.out.println("ERROR actualizando contacto favorito: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
