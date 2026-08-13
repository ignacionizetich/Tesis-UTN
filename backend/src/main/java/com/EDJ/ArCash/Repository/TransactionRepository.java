package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByIdOriginOrIdDestination(Account acc1, Account acc2);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM Transaction t
        WHERE t.idOrigin.idAccount IN :accountIds
           OR t.idDestination.idAccount IN :accountIds
        """)
    int deleteAllByAccountIds(@Param("accountIds") Collection<Long> accountIds);
}
