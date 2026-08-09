package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.User;

public record FavoriteContactResponse(
        Long id,
        String contactAlias,
        String description,
        String creationDate,
        String lastUsed,
        boolean active,
        String accountOwnerName,
        String accountOwnerAlias,
        String accountCbu,
        String accountAlias,
        String accountType
) {
    public static FavoriteContactResponse from(FavoriteContact favorite) {
        Account favoriteAccount = favorite.getFavoriteAccount();
        User accountOwner = favoriteAccount.getUser();

        return new FavoriteContactResponse(
                favorite.getId(),
                favorite.getContactAlias(),
                favorite.getDescription(),
                favorite.getCreationDate().toString(),
                favorite.getLastUsed() != null ? favorite.getLastUsed().toString() : null,
                favorite.isActive(),
                accountOwner.getName() + " " + accountOwner.getLastName(),
                accountOwner.getAlias(),
                favoriteAccount.getAccountCvu(),
                favoriteAccount.getAccountNickname(),
                "PESOS"
        );
    }
}
