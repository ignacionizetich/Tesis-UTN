package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ValidationTokenRepository extends JpaRepository<ValidationToken, Long> {
    /// HAY QUE CREAR LOS METODOS PARA LAS VALIDACIONES

    List<ValidationToken> findAllByUsedFalseAndExpirationDateBefore(LocalDateTime now);

    ValidationToken findByUser(User user);

    Optional<ValidationToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM ValidationToken vt WHERE vt.user.Id = :userId")
    void deleteByUser_Id(Long userId);


    List<ValidationToken> findAllByUsedTrueAndExpirationDateBefore(LocalDateTime now);
}
