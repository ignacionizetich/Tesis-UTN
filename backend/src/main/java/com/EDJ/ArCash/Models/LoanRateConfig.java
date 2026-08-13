package com.EDJ.ArCash.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_rate_config")
public class LoanRateConfig {

    @Id
    @Column(name = "installments")
    private Integer installments;

    /** Tasa mensual en decimal (0.04 = 4%). */
    @Column(name = "monthly_rate", nullable = false)
    private double monthlyRate;

    @Column(name = "updated_at")
    private String updatedAt;

    public LoanRateConfig(Integer installments, double monthlyRate) {
        this.installments = installments;
        this.monthlyRate = monthlyRate;
    }

    @PrePersist
    @PreUpdate
    private void touch() {
        updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
