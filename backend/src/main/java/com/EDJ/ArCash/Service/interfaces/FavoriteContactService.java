package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.result.FavoriteUpdateResult;
import java.util.List;

public interface FavoriteContactService {
    public boolean addFavoriteContact(Long userId, Long accountId, String contactAlias, String description);

    public List<FavoriteContact> getFavoriteContactsByUser(Long userId);

    public List<FavoriteContact> getFavoriteContactsByUserOrderedByUsage(Long userId);

    public void updateLastUsedForContact(Long favoriteContactId);

    public boolean removeFavoriteContact(Long userId, Long favoriteContactId);

    public FavoriteUpdateResult updateFavoriteContactForOwner(
                Long contactId, Long userId, String newAlias, String newDescription);

    public boolean updateFavoriteContact(Long contactId, Long userId, String newAlias, String newDescription);

}
