package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.CardUnlockService;
import com.EDJ.ArCash.Service.interfaces.CardAuditService;
import com.EDJ.ArCash.Service.interfaces.CardPinService;

import com.EDJ.ArCash.Models.CardPin;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.Imp.CardAuditType;
import com.EDJ.ArCash.Repository.CardPinRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class CardPinServiceImpl implements CardPinService {

    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{6}$");
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final CardPinRepository cardPinRepository;
    private final PasswordEncoder passwordEncoder;
    private final CardAuditService cardAuditService;
    private final CardUnlockService cardUnlockService;

    public CardPinServiceImpl(
            CardPinRepository cardPinRepository,
            PasswordEncoder passwordEncoder,
            CardAuditService cardAuditService,
            CardUnlockService cardUnlockService) {
        this.cardPinRepository = cardPinRepository;
        this.passwordEncoder = passwordEncoder;
        this.cardAuditService = cardAuditService;
        this.cardUnlockService = cardUnlockService;
    }

    public boolean isConfigured(Long userId) {
        return cardPinRepository.existsByUser_Id(userId);
    }

    public boolean isValidFormat(String pin) {
        return pin != null && PIN_PATTERN.matcher(pin).matches();
    }

    @Transactional
    public PinResult setPin(User user, String pin, String confirm, String currentPin) {
        if (!isValidFormat(pin) || !pin.equals(confirm)) {
            return PinResult.invalid("El PIN debe ser de 6 dígitos y coincidir con la confirmación.");
        }

        Optional<CardPin> existing = cardPinRepository.findByUser_Id(user.getId());
        if (existing.isPresent()) {
            CardPin cardPin = existing.get();
            if (cardPin.isLocked()) {
                return PinResult.locked("PIN bloqueado temporalmente. Probá más tarde.");
            }
            if (currentPin == null || !passwordEncoder.matches(currentPin, cardPin.getPinHash())) {
                registerFailure(user, cardPin);
                return PinResult.invalid("PIN actual incorrecto.");
            }
            cardPin.setPinHash(passwordEncoder.encode(pin));
            cardPin.setFailedAttempts(0);
            cardPin.setLockedUntil(null);
            cardPinRepository.save(cardPin);
            cardAuditService.record(user, null, CardAuditType.PIN_SET, "PIN actualizado");
            String token = cardUnlockService.createUnlock(user.getId());
            return PinResult.ok("PIN actualizado", token);
        }

        CardPin cardPin = new CardPin();
        cardPin.setUser(user);
        cardPin.setPinHash(passwordEncoder.encode(pin));
        cardPin.setFailedAttempts(0);
        cardPinRepository.save(cardPin);
        cardAuditService.record(user, null, CardAuditType.PIN_SET, "PIN creado");
        String token = cardUnlockService.createUnlock(user.getId());
        return PinResult.ok("PIN configurado", token);
    }

    @Transactional
    public PinResult verify(User user, String pin) {
        if (!isValidFormat(pin)) {
            return PinResult.invalid("El PIN debe ser de 6 dígitos.");
        }
        Optional<CardPin> existing = cardPinRepository.findByUser_Id(user.getId());
        if (existing.isEmpty()) {
            return PinResult.invalid("Todavía no configuraste un PIN.");
        }
        CardPin cardPin = existing.get();
        if (cardPin.isLocked()) {
            return PinResult.locked("PIN bloqueado temporalmente. Probá más tarde.");
        }
        if (!passwordEncoder.matches(pin, cardPin.getPinHash())) {
            registerFailure(user, cardPin);
            int left = Math.max(0, MAX_ATTEMPTS - cardPin.getFailedAttempts());
            return PinResult.invalid(left == 0
                    ? "PIN bloqueado por intentos fallidos."
                    : "PIN incorrecto. Te quedan " + left + " intentos.");
        }
        cardPin.setFailedAttempts(0);
        cardPin.setLockedUntil(null);
        cardPinRepository.save(cardPin);
        String token = cardUnlockService.createUnlock(user.getId());
        cardAuditService.record(user, null, CardAuditType.UNLOCK, "Desbloqueo OK");
        return PinResult.ok("Desbloqueo exitoso", token);
    }

    private void registerFailure(User user, CardPin cardPin) {
        cardPin.setFailedAttempts(cardPin.getFailedAttempts() + 1);
        if (cardPin.getFailedAttempts() >= MAX_ATTEMPTS) {
            cardPin.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            cardPin.setFailedAttempts(0);
        }
        cardPinRepository.save(cardPin);
        cardAuditService.record(user, null, CardAuditType.PIN_FAIL, "Intento fallido");
    }
}
