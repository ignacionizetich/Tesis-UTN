package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Loan;
import com.EDJ.ArCash.Models.Imp.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUser_IdOrderByIdDesc(Long userId);

    Optional<Loan> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndStatus(Long userId, LoanStatus status);
}
