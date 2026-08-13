package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.*;
import com.EDJ.ArCash.Models.CardAuditEvent;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.VirtualCard;
import com.EDJ.ArCash.Models.Imp.CardAuditType;
import com.EDJ.ArCash.Models.Imp.CardStatus;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.interfaces.CardAuditService;
import com.EDJ.ArCash.Service.interfaces.CardPinService;
import com.EDJ.ArCash.Service.interfaces.CardUnlockService;
import com.EDJ.ArCash.Service.interfaces.VirtualCardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/cards", produces = "application/json")
@Tag(name = "Tarjetas", description = "Prepagas virtuales ARS/USD")
public class VirtualCardController {

    private static final String UNLOCK_HEADER = "X-Card-Unlock";

    private final VirtualCardService virtualCardService;
    private final CardPinService cardPinService;
    private final CardUnlockService cardUnlockService;
    private final CardAuditService cardAuditService;

    public VirtualCardController(
            VirtualCardService virtualCardService,
            CardPinService cardPinService,
            CardUnlockService cardUnlockService,
            CardAuditService cardAuditService) {
        this.virtualCardService = virtualCardService;
        this.cardPinService = cardPinService;
        this.cardUnlockService = cardUnlockService;
        this.cardAuditService = cardAuditService;
    }

    @GetMapping
    public ResponseEntity<VirtualCardListResponse> list(
            @AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        boolean pinConfigured = cardPinService.isConfigured(user.getId());
        List<VirtualCardSummaryResponse> cards = virtualCardService.listOrBackfillForUser(user)
                .stream()
                .map(card -> toSummary(card, pinConfigured))
                .toList();
        return ResponseEntity.ok(VirtualCardListResponse.builder()
                .pinConfigured(pinConfigured)
                .cards(cards)
                .build());
    }

    @PostMapping("/pin")
    public ResponseEntity<?> setPin(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CardPinRequest request) {
        CardPinService.PinResult result = cardPinService.setPin(
                principal.getUser(),
                request.getPin(),
                request.getConfirmPin(),
                request.getCurrentPin());
        return toUnlockResponse(result);
    }

    @PostMapping("/pin/verify")
    public ResponseEntity<?> verifyPin(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CardPinVerifyRequest request) {
        CardPinService.PinResult result = cardPinService.verify(principal.getUser(), request.getPin());
        return toUnlockResponse(result);
    }

    @GetMapping("/{cardId}/reveal")
    public ResponseEntity<?> reveal(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long cardId,
            @RequestHeader(value = UNLOCK_HEADER, required = false) String unlockToken) {
        User user = principal.getUser();
        if (!cardUnlockService.isValid(unlockToken, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Debés desbloquear con tu PIN para ver los datos."));
        }
        Optional<VirtualCard> owned = virtualCardService.findOwned(cardId, user.getId());
        if (owned.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tarjeta no encontrada"));
        }
        VirtualCard card = owned.get();
        if (card.getStatus() == CardStatus.CANCELLED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La tarjeta está dada de baja."));
        }
        String pan = virtualCardService.decryptPan(card);
        String cvc = virtualCardService.decryptCvc(card);
        cardAuditService.record(user, card, CardAuditType.REVEAL, "Datos revelados");
        String holder = (user.getName() + " " + user.getLastName()).trim().toUpperCase();
        return ResponseEntity.ok(VirtualCardRevealResponse.builder()
                .id(card.getId())
                .currency(card.getCurrency().name())
                .pan(VirtualCardService.formatPan(pan))
                .last4(card.getLast4())
                .cvc(cvc)
                .expMonth(card.getExpMonth())
                .expYear(card.getExpYear())
                .status(card.getStatus().name())
                .dailyLimit(card.getDailyLimit())
                .holderName(holder)
                .build());
    }

    @PatchMapping("/{cardId}/status")
    public ResponseEntity<?> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long cardId,
            @RequestBody CardStatusRequest request) {
        User user = principal.getUser();
        Optional<VirtualCard> owned = virtualCardService.findOwned(cardId, user.getId());
        if (owned.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tarjeta no encontrada"));
        }
        CardStatus status;
        try {
            status = CardStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado inválido"));
        }
        if (status != CardStatus.ACTIVE && status != CardStatus.PAUSED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usá el endpoint de baja para cancelar la tarjeta."));
        }
        try {
            VirtualCard updated = virtualCardService.updateStatus(owned.get(), status);
            cardAuditService.record(
                    user,
                    updated,
                    status == CardStatus.PAUSED ? CardAuditType.PAUSE : CardAuditType.RESUME,
                    "Estado: " + status.name());
            return ResponseEntity.ok(toSummary(updated, cardPinService.isConfigured(user.getId())));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PatchMapping("/{cardId}/limit")
    public ResponseEntity<?> updateLimit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long cardId,
            @RequestBody CardLimitRequest request) {
        if (request.getDailyLimit() < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "El límite no puede ser negativo"));
        }
        User user = principal.getUser();
        Optional<VirtualCard> owned = virtualCardService.findOwned(cardId, user.getId());
        if (owned.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tarjeta no encontrada"));
        }
        try {
            VirtualCard updated = virtualCardService.updateLimit(owned.get(), request.getDailyLimit());
            cardAuditService.record(
                    user,
                    updated,
                    CardAuditType.LIMIT_CHANGE,
                    "Límite diario: " + request.getDailyLimit());
            return ResponseEntity.ok(toSummary(updated, cardPinService.isConfigured(user.getId())));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{cardId}/cancel")
    public ResponseEntity<?> cancel(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long cardId) {
        User user = principal.getUser();
        Optional<VirtualCard> owned = virtualCardService.findOwned(cardId, user.getId());
        if (owned.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tarjeta no encontrada"));
        }
        try {
            VirtualCard updated = virtualCardService.cancel(owned.get());
            cardAuditService.record(user, updated, CardAuditType.CANCEL, "Tarjeta dada de baja");
            return ResponseEntity.ok(toSummary(updated, cardPinService.isConfigured(user.getId())));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{cardId}/reissue")
    public ResponseEntity<?> reissue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long cardId) {
        User user = principal.getUser();
        Optional<VirtualCard> owned = virtualCardService.findOwned(cardId, user.getId());
        if (owned.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tarjeta no encontrada"));
        }
        try {
            VirtualCard updated = virtualCardService.reissue(owned.get());
            cardAuditService.record(user, updated, CardAuditType.REISSUE, "Nueva prepaga emitida");
            return ResponseEntity.ok(toSummary(updated, cardPinService.isConfigured(user.getId())));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/audit")
    public ResponseEntity<List<CardAuditEventResponse>> audit(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<CardAuditEventResponse> events = cardAuditService.latestForUser(principal.getUser().getId())
                .stream()
                .map(this::toAudit)
                .toList();
        return ResponseEntity.ok(events);
    }

    private ResponseEntity<?> toUnlockResponse(CardPinService.PinResult result) {
        CardUnlockResponse body = CardUnlockResponse.builder()
                .success(result.success())
                .message(result.message())
                .unlockToken(result.unlockToken())
                .locked(result.locked())
                .expiresInSeconds(result.success() ? 300 : 0)
                .build();
        if (result.success()) {
            return ResponseEntity.ok(body);
        }
        if (result.locked()) {
            return ResponseEntity.status(HttpStatus.LOCKED).body(body);
        }
        return ResponseEntity.badRequest().body(body);
    }

    private VirtualCardSummaryResponse toSummary(VirtualCard card, boolean pinConfigured) {
        User user = card.getUser();
        String holder = (user.getName() + " " + user.getLastName()).trim().toUpperCase();
        VirtualCardService.ReissueEligibility eligibility = virtualCardService.reissueEligibility(card);
        return VirtualCardSummaryResponse.builder()
                .id(card.getId())
                .accountId(card.getAccount().getIdAccount())
                .currency(card.getCurrency().name())
                .last4(card.getLast4())
                .status(card.getStatus().name())
                .dailyLimit(card.getDailyLimit())
                .holderName(holder)
                .pinConfigured(pinConfigured)
                .expMonth(card.getExpMonth())
                .expYear(card.getExpYear())
                .expired(virtualCardService.isExpired(card))
                .cancelledAt(card.getCancelledAt())
                .canReissue(eligibility.allowed())
                .reissueMessage(eligibility.message())
                .build();
    }

    private CardAuditEventResponse toAudit(CardAuditEvent event) {
        return CardAuditEventResponse.builder()
                .id(event.getId())
                .cardId(event.getCard() != null ? event.getCard().getId() : null)
                .type(event.getEventType().name())
                .meta(event.getMeta())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
