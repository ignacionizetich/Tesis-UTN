package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.CardPin;
import com.EDJ.ArCash.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardPinRepository extends JpaRepository<CardPin, Long> {
    Optional<CardPin> findByUser(User user);

    Optional<CardPin> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
