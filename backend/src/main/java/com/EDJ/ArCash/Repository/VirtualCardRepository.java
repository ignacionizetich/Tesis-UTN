package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.VirtualCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VirtualCardRepository extends JpaRepository<VirtualCard, Long> {
    List<VirtualCard> findByUser_IdOrderByIdAsc(Long userId);

    Optional<VirtualCard> findByIdAndUser_Id(Long id, Long userId);

    Optional<VirtualCard> findByAccount(Account account);

    boolean existsByAccount(Account account);
}
