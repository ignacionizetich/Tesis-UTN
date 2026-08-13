package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.Loan;
import com.EDJ.ArCash.Models.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LoanService {
    double MIN_PRINCIPAL = 1_000.0;
    double MAX_PRINCIPAL = 500_000.0;
    Set<Integer> ALLOWED_INSTALLMENTS = Set.of(3, 6, 12);
    String OP_LOAN_CREDIT = "LOAN_CREDIT";
    String OP_LOAN_PAYMENT = "LOAN_PAYMENT";

    double monthlyRateFor(int installments);

    SimulationResult simulate(double principal, int installments);

    List<Loan> listForUser(Long userId);

    Optional<Loan> findOwned(Long loanId, Long userId);

    Loan accept(User user, double principal, int installments);

    Loan payNext(Loan loan);

    record InstallmentPlan(int number, String dueDate, double amount) {}

    record SimulationResult(
            double principal,
            int installments,
            double monthlyRate,
            double installmentAmount,
            double totalAmount,
            List<InstallmentPlan> schedule
    ) {
        public double totalInterest() {
            return Math.round((totalAmount - principal) * 100.0) / 100.0;
        }
    }
}
