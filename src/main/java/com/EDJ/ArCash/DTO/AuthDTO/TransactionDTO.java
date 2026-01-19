package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.Models.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "DTO para mostrar la información de una transacción")
public class TransactionDTO {
    @Schema(description = "ID de la transacción", example = "1")
    private Long idTransaction;

    @Schema(description = "ID de la operación asociada", example = "OP123456")
    private String idOperation;

    @Schema(description = "ID de la cuenta de origen", example = "10")
    private Long idOrigin;

    @Schema(description = "ID de la cuenta de destino", example = "20")
    private Long idDestination;

    @Schema(description = "Monto de la transacción", example = "1500.00")
    private Double amount;

    @Schema(description = "Estado de la transacción", example = "COMPLETADA")
    private String state;

    @Schema(description = "Fecha de la transacción", example = "2024-06-01T12:34:56")
    private String date;

    @Schema(description = "Alias del usuario de origen", example = "juan.perez")
    private String originUsername;

    @Schema(description = "Alias del usuario de destino", example = "maria.gomez")
    private String destinationUsername;

    @Schema(description = "Alias de la cuenta de origen", example = "cuenta.juan")
    private String originAlias;

    @Schema(description = "Alias de la cuenta de destino", example = "cuenta.maria")
    private String destinationAlias;

    @Schema(description = "Moneda de la transacción", example = "ARS")
    private String currency;

    @Schema(description = "Monto original antes de conversión", example = "10000.00")
    private Double originalAmount;

    @Schema(description = "Moneda original antes de conversión", example = "ARS")
    private String originalCurrency;

    @Schema(description = "Tasa de cambio aplicada", example = "1000.00")
    private Double exchangeRate;

    @Schema(description = "Indica si hubo conversión de moneda", example = "true")
    private Boolean converted;

    public TransactionDTO(Transaction transaction) {
        setIdTransaction(transaction.getId());
        setIdOrigin(transaction.getIdOrigin().getIdAccount());
        setIdDestination(transaction.getIdDestination().getIdAccount());
        setAmount(transaction.getBalance());
        setIdOperation(transaction.getId_operation());
        setState(transaction.getState());
        setDate(transaction.getTransaction_date());
        this.originUsername = transaction.getIdOrigin().getUser().getAlias();
        this.destinationUsername = transaction.getIdDestination().getUser().getAlias();
        this.originAlias = transaction.getIdOrigin().getAccountNickname();
        this.destinationAlias = transaction.getIdDestination().getAccountNickname();
        
        // Información de moneda y conversión
        this.currency = transaction.getCurrency() != null ? transaction.getCurrency().toString() : null;
        this.originalAmount = transaction.getOriginalAmount();
        this.originalCurrency = transaction.getOriginalCurrency() != null ? transaction.getOriginalCurrency().toString() : null;
        this.exchangeRate = transaction.getExchangeRate();
        this.converted = transaction.getOriginalAmount() != null && transaction.getExchangeRate() != null;
    }
}