package com.EDJ.ArCash.DTO;

import com.EDJ.ArCash.Models.Transaction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransactionDTO {
    private Long idTransaction;
    private String idOperation;
    private Long idOrigin;
    private Long idDestination;
    private Double amount;
    private String state;
    private String date;
    private String originUsername;
    private String destinationUsername;
    private String originAlias;
    private String destinationAlias;


    public TransactionDTO(Transaction transaction) {
        setIdTransaction(transaction.getId_transaction());
        setIdOrigin(transaction.getIdOrigin().getIdAccount());
        setIdDestination(transaction.getIdDestination().getIdAccount());
        setAmount(transaction.getBalance());
        setIdOperation(transaction.getId_operation());
        setState(transaction.getState());
        setDate(transaction.getTransaction_date());
        this.originUsername = transaction.getIdOrigin().getUser().getAlias();
        this.destinationUsername = transaction.getIdDestination().getUser().getAlias();
        this.originAlias = transaction.getIdOrigin().getAccountNickname();           // Asigna alias de cuenta origen
        this.destinationAlias = transaction.getIdDestination().getAccountNickname();
    }
}