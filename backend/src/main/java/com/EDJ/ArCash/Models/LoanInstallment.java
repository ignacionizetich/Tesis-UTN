package com.EDJ.ArCash.Models;

import com.EDJ.ArCash.Models.Imp.LoanInstallmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_installments")
public class LoanInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false, length = 32)
    private String dueDate;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanInstallmentStatus status = LoanInstallmentStatus.PENDING;

    @Column(name = "paid_at", length = 32)
    private String paidAt;
}
