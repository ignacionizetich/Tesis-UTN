package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.RecoveryToken;
import com.EDJ.ArCash.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface RecoveryTokenRepository extends JpaRepository<RecoveryToken,Long> {

        Optional<RecoveryToken> findByToken(String token);

        RecoveryToken findByUser(User user);

        void deleteByUser_Id(Long id);

}
