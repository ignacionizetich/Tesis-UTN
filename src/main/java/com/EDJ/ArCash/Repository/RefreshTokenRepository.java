package com.EDJ.ArCash.Repository;


import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long>{

    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    Optional<RefreshToken> findByUserAndRevokedFalse(User user);

    void deleteByUser(User user);

}
