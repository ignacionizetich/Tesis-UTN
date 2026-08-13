package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.AuthDTO.AccountSearchResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.result.AccountBalanceView;
import com.EDJ.ArCash.Service.result.AliasChangeResult;
import com.EDJ.ArCash.Service.result.DepositResult;
import com.EDJ.ArCash.Service.result.OpenUsdResult;
import com.EDJ.ArCash.Service.result.QrDataResult;
import java.util.List;
import java.util.Optional;

public interface AccountService {
    public void createAccount(User user);

    public Account createUsdAccount(User user);

    public Account ensureArsAccount(User user);

    public OpenUsdResult openUsdAccount(User user);

    public DepositResult deposit(Long accountId, Long userId, double amount);

    public Optional<AccountBalanceView> getOwnedBalance(Long accountId, Long userId);

    public QrDataResult getQrDataForOwner(Long accountId, Long userId);

    public boolean updateBalance(double balanceToAdd, Long id);

    public Optional<Account> findAccountByID(long id);

    public List<Account> findAccountsByUser(Long userId);

    public Optional<Account> encontrarCuentaPorAlias(String alias);

    public Optional<Account> encontrarCuentaPorCvu(String cvu);

    public Optional<AccountSearchResponse> searchByAliasOrCvu(String input);

    public AliasChangeResult changeAlias(String newAlias, Long id, Long userId);

}
