package com.EDJ.ArCash.Models;


import com.EDJ.ArCash.Models.Imp.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAccount;


    /// Muchas cuentas(caja en pesos y dolares) pertenece a un usuario
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    /// SE GENERA UN ALIAS POR DEFECTO QUE LUEGO EL USUARIO PUEDE EDITAR(verificar si se repite en services)
    @Column(unique = true, name = "account_nickname")
    private String accountNickname;

    @Column(name = "balance")
    private double balance;

    /// SE GENERA UN CVU PARA LA CUENTA DEL USUARIO EL CUAL NO VA A SER MODIFICABLE(verificar si se repite en services)
    @Column(unique = true, name = "account_cvu")
    private String accountCvu;


    /// SE SETEA POR DEFECTO LA CUENTA EN TIPO PESOS, LA DE DOLARES LA ABRE EL USUARIO SI ASI LO QUIERA
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Currency accountType;

    @Column(name = "creation_date")
    private String creationDate;

    @OneToMany(mappedBy = "favoriteAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FavoriteContact> favoritedBy;


    public Account (User user){
        this.user = user;
    }

    @PrePersist
    private void GenerateCreationDate(){
        DateTimeFormatter formateador = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime fechaActual = LocalDateTime.now();
        this.creationDate = fechaActual.format(formateador);
    }




}
