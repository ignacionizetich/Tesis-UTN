package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.FavoriteContactRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FavoriteContactService {

    @Autowired
    private FavoriteContactRepository favoriteContactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

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
            System.out.println("Usuario no encontrado con ID: " + userId);
            return List.of();
        }

        User user = userOpt.get();
        System.out.println("Usuario encontrado: " + user.getId());

        // Buscar TODOS los favoritos primero para debuggear
        List<FavoriteContact> allFavorites = favoriteContactRepository.findByOwner(user);
        System.out.println("Total de favoritos (incluyendo inactivos): " + allFavorites.size());

        // Ahora buscar solo los activos
        List<FavoriteContact> activeFavorites = favoriteContactRepository.findByOwnerAndActiveTrue(user);
        System.out.println("Total de favoritos activos: " + activeFavorites.size());

        return activeFavorites;
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
    public boolean updateFavoriteContact(Long contactId, Long userId, String newAlias, String newDescription) {
        try {
            System.out.println("=== ACTUALIZANDO CONTACTO FAVORITO ===");
            System.out.println("ContactId: " + contactId + ", UserId: " + userId);
            System.out.println("Nuevo alias: " + newAlias + ", Nueva descripción: " + newDescription);

            Optional<FavoriteContact> optionalContact = favoriteContactRepository.findById(contactId);

            if (optionalContact.isEmpty()) {
                System.out.println("ERROR: Contacto favorito no encontrado con ID: " + contactId);
                return false;
            }

            FavoriteContact contact = optionalContact.get();
            System.out.println("Contacto encontrado:");
            System.out.println("- ID: " + contact.getId());
            System.out.println("- Owner ID: " + contact.getOwner().getId());
            System.out.println("- Active: " + contact.isActive());
            System.out.println("- Alias actual: " + contact.getContactAlias());

            // Verificar que el contacto pertenece al usuario
            if (!contact.getOwner().getId().equals(userId)) {
                System.out.println("ERROR: El contacto no pertenece al usuario.");
                System.out.println("Owner esperado: " + userId + ", Owner real: " + contact.getOwner().getId());
                return false;
            }

            // Verificar que el contacto esté activo
            if (!contact.isActive()) {
                System.out.println("ERROR: El contacto está inactivo");
                return false;
            }

            // Actualizar los campos
            if (newAlias != null && !newAlias.trim().isEmpty()) {
                System.out.println("Actualizando alias de '" + contact.getContactAlias() + "' a '" + newAlias.trim() + "'");
                contact.setContactAlias(newAlias.trim());
            }

            if (newDescription != null) {
                System.out.println("Actualizando descripción de '" + contact.getDescription() + "' a '" + newDescription.trim() + "'");
                contact.setDescription(newDescription.trim());
            }

            favoriteContactRepository.save(contact);
            System.out.println("✅ Contacto favorito actualizado correctamente");
            return true;

        } catch (Exception e) {
            System.out.println("ERROR actualizando contacto favorito: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}