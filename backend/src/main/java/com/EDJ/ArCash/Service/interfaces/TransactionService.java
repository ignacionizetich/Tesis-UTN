package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.AuthDTO.TransactionDTO;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Transaction;
import com.EDJ.ArCash.Service.result.BuyUsdResult;
import com.EDJ.ArCash.Service.result.OwnedBuyUsdResult;
import com.EDJ.ArCash.Service.result.OwnedSellUsdResult;
import com.EDJ.ArCash.Service.result.OwnedTransferResult;
import com.EDJ.ArCash.Service.result.SellUsdResult;
import com.EDJ.ArCash.Service.result.TransferOperationResult;
import java.util.List;

public interface TransactionService {
    public OwnedTransferResult transferForOwner(Long userId, Long idOrigen, Long idDestino, double monto);

    public OwnedBuyUsdResult buyUsdForOwner(Long userId, Long accountArsId, Long accountUsdId, double amountArs);

    public OwnedSellUsdResult sellUsdForOwner(Long userId, Long accountUsdId, Long accountArsId, double amountUsd);

    public boolean transaction(Long idOrigen, Long idDestino, double monto);

    public TransferOperationResult transactionWithDetails(Long idOrigen, Long idDestino, double monto);

    public boolean transactionSameCurrency(Account cuentaOrigen, Account cuentaDestino, double monto);

    public TransferOperationResult transactionWithConversionDetails(Account cuentaOrigen, Account cuentaDestino, double monto);

    public boolean transactionWithConversion(Account cuentaOrigen, Account cuentaDestino, double monto);

    public BuyUsdResult buyUsd(Long accountArsId, Long accountUsdId, double amountArs);

    public SellUsdResult sellUsd(Long accountUsdId, Long accountArsId, double amountUsd);

    public List<TransactionDTO> listaTransacciones(Long id);

}
