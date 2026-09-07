package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.TokenCleanupService;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.FavoriteContactRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.TransactionRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Repository.ValidationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupServiceImpl implements TokenCleanupService {


    private final ValidationTokenRepository validationTokenRepository;


    private final UserRepository userRepository;


    private final AccountRepository accountRepository;


    private final TransactionRepository transactionRepository;


    private final FavoriteContactRepository favoriteContactRepository;


    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * ?")
    public void removeExpiredUnvalidatedUsers() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Buscando tokens expirados antes de: {}", now);

        List<ValidationToken> expiredUnusedTokens =
                validationTokenRepository.findAllByUsedFalseAndExpirationDateBefore(now);
        log.info("Tokens no usados expirados: {}", expiredUnusedTokens.size());

        int deletedUsers = 0;
        for (ValidationToken token : expiredUnusedTokens) {
            User user = token.getUser();
            if (user == null) {
                continue;
            }
            // Solo usuarios no validados / deshabilitados (evita borrar cuentas reales en uso).
            if (user.isEnabled()) {
                log.warn(
                        "Se omite usuario id={} con token expirado no usado porque está enabled=true",
                        user.getId());
                continue;
            }

            try {
                deleteUserWithDependencies(user);
                deletedUsers++;
            } catch (Exception e) {
                log.error(
                        "No se pudo eliminar usuario no validado id={}: {}",
                        user.getId(),
                        e.getMessage(),
                        e);
            }
        }

        List<ValidationToken> expiredUsedTokens =
                validationTokenRepository.findAllByUsedTrueAndExpirationDateBefore(now);
        if (!expiredUsedTokens.isEmpty()) {
            validationTokenRepository.deleteAllInBatch(expiredUsedTokens);
        }

        log.info(
                "Cleanup finalizado. Usuarios eliminados={}, tokens usados expirados borrados={}",
                deletedUsers,
                expiredUsedTokens.size());
    }

    private void deleteUserWithDependencies(User user) {
        List<Account> accounts = accountRepository.findAllByUser_Id(user.getId());
        List<Long> accountIds = accounts.stream().map(Account::getIdAccount).toList();

        if (accountIds.isEmpty()) {
            favoriteContactRepository.deleteByOwner(user);
        } else {
            favoriteContactRepository.deleteAllRelatedToUserOrAccounts(user, accountIds);
            transactionRepository.deleteAllByAccountIds(accountIds);
        }

        refreshTokenRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }
}
