package com.EDJ.ArCash.Models;

import com.EDJ.ArCash.Models.Imp.LoanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "principal", nullable = false)
    private double principal;

    /** Tasa mensual en decimal (ej. 0.04 = 4%). */
    @Column(name = "monthly_rate", nullable = false)
    private double monthlyRate;

    @Column(name = "installment_count", nullable = false)
    private int installmentCount;

    @Column(name = "installment_amount", nullable = false)
    private double installmentAmount;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "created_at")
    private String createdAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<LoanInstallment> installments = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (status == null) {
            status = LoanStatus.ACTIVE;
        }
    }
}
