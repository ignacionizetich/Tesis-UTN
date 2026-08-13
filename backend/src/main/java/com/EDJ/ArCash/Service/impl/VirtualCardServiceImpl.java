package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.CardCryptoService;
import com.EDJ.ArCash.Service.interfaces.VirtualCardService;
import com.EDJ.ArCash.Service.support.*;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.VirtualCard;
import com.EDJ.ArCash.Models.Imp.CardStatus;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.VirtualCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VirtualCardServiceImpl implements VirtualCardService {

    private static final double DEFAULT_LIMIT_ARS = 50_000.0;
    private static final double DEFAULT_LIMIT_USD = 100.0;
    /** Días de espera tras dar de baja antes de poder pedir otra (si no venció). */
    public static final int REISSUE_COOLDOWN_DAYS = 7;

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final VirtualCardRepository virtualCardRepository;
    private final AccountRepository accountRepository;
    private final CardCryptoService cardCryptoService;
    private final CardNumberGenerator cardNumberGenerator;

    public VirtualCardServiceImpl(
            VirtualCardRepository virtualCardRepository,
            AccountRepository accountRepository,
            CardCryptoService cardCryptoService,
            CardNumberGenerator cardNumberGenerator) {
        this.virtualCardRepository = virtualCardRepository;
        this.accountRepository = accountRepository;
        this.cardCryptoService = cardCryptoService;
        this.cardNumberGenerator = cardNumberGenerator;
    }

    @Transactional
    public VirtualCard createForAccount(Account account) {
        if (virtualCardRepository.existsByAccount(account)) {
            return virtualCardRepository.findByAccount(account).orElseThrow();
        }
        return persistNewCard(account);
    }

    @Transactional
    public List<VirtualCard> listOrBackfillForUser(User user) {
        List<Account> accounts = accountRepository.findAllByUser_Id(user.getId());
        for (Account account : accounts) {
            if (!virtualCardRepository.existsByAccount(account)) {
                createForAccount(account);
            }
        }
        return virtualCardRepository.findByUser_IdOrderByIdAsc(user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<VirtualCard> findOwned(Long cardId, Long userId) {
        return virtualCardRepository.findByIdAndUser_Id(cardId, userId);
    }

    @Transactional
    public VirtualCard updateStatus(VirtualCard card, CardStatus status) {
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("La tarjeta está dada de baja.");
        }
        if (status != CardStatus.ACTIVE && status != CardStatus.PAUSED) {
            throw new IllegalArgumentException("Estado inválido");
        }
        card.setStatus(status);
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard updateLimit(VirtualCard card, double dailyLimit) {
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("La tarjeta está dada de baja.");
        }
        card.setDailyLimit(dailyLimit);
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard cancel(VirtualCard card) {
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("La tarjeta ya está dada de baja.");
        }
        card.setStatus(CardStatus.CANCELLED);
        card.setCancelledAt(LocalDateTime.now().format(TS));
        return virtualCardRepository.save(card);
    }

    @Transactional
    public VirtualCard reissue(VirtualCard card) {
        ReissueEligibility eligibility = reissueEligibility(card);
        if (!eligibility.allowed()) {
            throw new IllegalStateException(eligibility.message());
        }
        CardNumberGenerator.GeneratedCardNumbers numbers =
                cardNumberGenerator.generate(card.getCurrency());
        card.setPanEncrypted(cardCryptoService.encrypt(numbers.pan()));
        card.setLast4(numbers.last4());
        card.setExpMonth(numbers.expMonth());
        card.setExpYear(numbers.expYear());
        card.setCvcEncrypted(cardCryptoService.encrypt(numbers.cvc()));
        card.setStatus(CardStatus.ACTIVE);
        card.setCancelledAt(null);
        if (card.getDailyLimit() <= 0) {
            card.setDailyLimit(card.getCurrency() == Currency.USD
                    ? DEFAULT_LIMIT_USD
                    : DEFAULT_LIMIT_ARS);
        }
        return virtualCardRepository.save(card);
    }

    public boolean isExpired(VirtualCard card) {
        YearMonth exp = YearMonth.of(card.getExpYear(), card.getExpMonth());
        return YearMonth.from(LocalDate.now()).isAfter(exp);
    }

    public ReissueEligibility reissueEligibility(VirtualCard card) {
        boolean expired = isExpired(card);
        if (expired) {
            return ReissueEligibility.ok("Podés solicitar una nueva porque la actual venció.");
        }
        if (card.getStatus() != CardStatus.CANCELLED) {
            return ReissueEligibility.blocked(
                    "Solo podés pedir otra si la tarjeta venció o la diste de baja y pasó el período de espera.");
        }
        LocalDateTime cancelledAt = parseCancelledAt(card.getCancelledAt());
        if (cancelledAt == null) {
            return ReissueEligibility.ok("Podés solicitar una nueva prepaga.");
        }
        long days = ChronoUnit.DAYS.between(cancelledAt.toLocalDate(), LocalDate.now());
        if (days >= REISSUE_COOLDOWN_DAYS) {
            return ReissueEligibility.ok("Ya pasó el período de espera. Podés solicitar una nueva.");
        }
        long remaining = REISSUE_COOLDOWN_DAYS - days;
        return ReissueEligibility.blocked(
                "Tras dar de baja debés esperar " + REISSUE_COOLDOWN_DAYS
                        + " días. Te faltan " + remaining + " día(s).");
    }

    public String decryptPan(VirtualCard card) {
        return cardCryptoService.decrypt(card.getPanEncrypted());
    }

    public String decryptCvc(VirtualCard card) {
        return cardCryptoService.decrypt(card.getCvcEncrypted());
    }

    public static String formatPan(String pan) {
        String digits = pan.replaceAll("\\D", "");
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < digits.length(); i += 4) {
            parts.add(digits.substring(i, Math.min(i + 4, digits.length())));
        }
        return String.join(" ", parts);
    }

    private VirtualCard persistNewCard(Account account) {
        CardNumberGenerator.GeneratedCardNumbers numbers =
                cardNumberGenerator.generate(account.getAccountType());

        VirtualCard card = new VirtualCard();
        card.setUser(account.getUser());
        card.setAccount(account);
        card.setCurrency(account.getAccountType());
        card.setPanEncrypted(cardCryptoService.encrypt(numbers.pan()));
        card.setLast4(numbers.last4());
        card.setExpMonth(numbers.expMonth());
        card.setExpYear(numbers.expYear());
        card.setCvcEncrypted(cardCryptoService.encrypt(numbers.cvc()));
        card.setStatus(CardStatus.ACTIVE);
        card.setDailyLimit(account.getAccountType() == Currency.USD
                ? DEFAULT_LIMIT_USD
                : DEFAULT_LIMIT_ARS);
        return virtualCardRepository.save(card);
    }

    private static LocalDateTime parseCancelledAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw, TS);
        } catch (Exception e) {
            return null;
        }
    }
}
