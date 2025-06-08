package com.EDJ.ArCash.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "recovery_tokens")
public class RecoveryToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true, name = "token")
    private String token;

    @Column(nullable = false, name = "expiration_date")
    private LocalDateTime expirationDate;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, name = "used")
    private boolean used;

    public RecoveryToken(User user){
        this.user = user;
        this.token = UUID.randomUUID().toString().substring(0,20);
        this.expirationDate = LocalDateTime.now().plusHours(1);
        this.used = false;

    }

}
