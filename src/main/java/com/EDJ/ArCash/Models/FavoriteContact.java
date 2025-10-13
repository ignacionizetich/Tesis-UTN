package com.EDJ.ArCash.Models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "favorite_contacts")
@Entity
public class FavoriteContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_user_id")
    @JsonBackReference("user-favorites") // Complemento de @JsonManagedReference
    private User owner;

    @ManyToOne
    @JoinColumn(name = "favorite_account_id")
    @JsonManagedReference("account-favorites")
    private Account favoriteAccount;

    @NotBlank(message = "El alias del contacto no puede estar vacio")
    @Column(name = "contact_alias")
    private String contactAlias;

    @Column(name = "description")
    private String description;

    @Column(name = "creation_date")
    private String creationDate;

    @Column(name = "last_used")
    private String lastUsed;

    @Column(name = "active")
    private boolean active = true;


    public FavoriteContact(User owner, Account favoriteAccount, String contactAlias) {
        this.owner = owner;
        this.favoriteAccount = favoriteAccount;
        this.contactAlias = contactAlias;
    }

    @PrePersist
    private void prePersist() {
        generateCreationDate();
    }

    private void generateCreationDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime currentDate = LocalDateTime.now();
        this.creationDate = currentDate.format(formatter);
    }

    public void updateLastUsed() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime currentDate = LocalDateTime.now();
        this.lastUsed = currentDate.format(formatter);
    }

}
