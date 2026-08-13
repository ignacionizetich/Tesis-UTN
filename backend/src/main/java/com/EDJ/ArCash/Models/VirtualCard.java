package com.EDJ.ArCash.Models;

import com.EDJ.ArCash.Models.Imp.CardStatus;
import com.EDJ.ArCash.Models.Imp.Currency;
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
@Table(name = "virtual_cards")
public class VirtualCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(optional = false)
    @JoinColumn(name = "account_id", unique = true)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Column(name = "pan_encrypted", nullable = false, length = 512)
    private String panEncrypted;

    @Column(name = "last4", nullable = false, length = 4)
    private String last4;

    @Column(name = "exp_month", nullable = false)
    private int expMonth;

    @Column(name = "exp_year", nullable = false)
    private int expYear;

    @Column(name = "cvc_encrypted", nullable = false, length = 256)
    private String cvcEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "daily_limit", nullable = false)
    private double dailyLimit;

    @Column(name = "creation_date")
    private String creationDate;

    /** Timestamp ISO-ish cuando se dio de baja; null si nunca se canceló. */
    @Column(name = "cancelled_at")
    private String cancelledAt;

    @PrePersist
    private void prePersist() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (status == null) {
            status = CardStatus.ACTIVE;
        }
    }
}
