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
     void createAccount(User user);

     Account createUsdAccount(User user);

     Account ensureArsAccount(User user);

     OpenUsdResult openUsdAccount(User user);

     DepositResult deposit(Long accountId, Long userId, double amount);

     Optional<AccountBalanceView> getOwnedBalance(Long accountId, Long userId);

     QrDataResult getQrDataForOwner(Long accountId, Long userId);

     boolean updateBalance(double balanceToAdd, Long id);

     Optional<Account> findAccountByID(long id);

     List<Account> findAccountsByUser(Long userId);

     Optional<Account> encontrarCuentaPorAlias(String alias);

     Optional<Account> encontrarCuentaPorCvu(String cvu);

     Optional<AccountSearchResponse> searchByAliasOrCvu(String input);

     AliasChangeResult changeAlias(String newAlias, Long id, Long userId);

}
