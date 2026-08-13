package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FavoriteContactRepository extends JpaRepository<FavoriteContact, Long> {

    List<FavoriteContact> findByOwner(User owner); // Para debuggear

    List<FavoriteContact> findByOwnerAndActiveTrue(User user);

    boolean existsByOwnerAndFavoriteAccount(User user, Account account);

    // Ordenar por último uso (nulls al final)
    @Query("SELECT f FROM FavoriteContact f WHERE f.owner = :owner AND f.active = true ORDER BY f.lastUsed DESC NULLS LAST")
    List<FavoriteContact> findByOwnerAndActiveTrueOrderByLastUsed(@Param("owner") User owner);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FavoriteContact f WHERE f.owner = :user")
    int deleteByOwner(@Param("user") User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM FavoriteContact f
        WHERE f.owner = :user
           OR f.favoriteAccount.idAccount IN :accountIds
        """)
    int deleteAllRelatedToUserOrAccounts(
            @Param("user") User user,
            @Param("accountIds") Collection<Long> accountIds);
}
