package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Service.interfaces.CardPinService;
import com.EDJ.ArCash.Service.interfaces.CardUnlockService;
import com.EDJ.ArCash.Service.interfaces.VirtualCardService;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.VirtualCard;
import com.EDJ.ArCash.Models.Imp.CardStatus;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Repository.VirtualCardRepository;
import com.EDJ.ArCash.Security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VirtualCardServiceTest {

    @Autowired
    private VirtualCardService virtualCardService;

    @Autowired
    private CardPinService cardPinService;

    @Autowired
    private CardUnlockService cardUnlockService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VirtualCardRepository virtualCardRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("createForAccount genera last4 y no duplica la tarjeta")
    void createForAccountGeneraTarjetaUnaSolaVez() {
        User user = userRepository.save(newUser("card.user.a"));
        Account account = accountRepository.save(newAccount(user, Currency.ARS));

        VirtualCard first = virtualCardService.createForAccount(account);
        VirtualCard second = virtualCardService.createForAccount(account);

        assertEquals(first.getId(), second.getId());
        assertEquals(4, first.getLast4().length());
        assertTrue(virtualCardRepository.existsByAccount(account));
    }

    @Test
    @DisplayName("PIN incorrecto varias veces termina bloqueando")
    void pinIncorrectoBloqueaDespuesDeCincoIntentos() {
        User user = userRepository.save(newUser("card.user.b"));
        cardPinService.setPin(user, "123456", "123456", null);

        for (int i = 0; i < 4; i++) {
            CardPinService.PinResult fail = cardPinService.verify(user, "000000");
            assertFalse(fail.success());
            assertFalse(fail.locked());
        }
        CardPinService.PinResult last = cardPinService.verify(user, "000000");
        assertFalse(last.success());

        CardPinService.PinResult locked = cardPinService.verify(user, "123456");
        assertTrue(locked.locked() || !locked.success());
    }

    @Test
    @DisplayName("Unlock token válido habilita isValid")
    void unlockTokenEsValidoParaElUsuario() {
        User user = userRepository.save(newUser("card.user.c"));
        CardPinService.PinResult set = cardPinService.setPin(user, "654321", "654321", null);
        assertTrue(set.success());
        assertTrue(cardUnlockService.isValid(set.unlockToken(), user.getId()));
        assertFalse(cardUnlockService.isValid(set.unlockToken(), user.getId() + 99));
        assertFalse(cardUnlockService.isValid(null, user.getId()));
    }

    @Test
    @DisplayName("Reveal sin unlock token responde 403")
    void revealSinUnlockDevuelve403() throws Exception {
        User user = userRepository.save(newUser("card.user.d"));
        Account account = accountRepository.save(newAccount(user, Currency.ARS));
        VirtualCard card = virtualCardService.createForAccount(account);

        mockMvc.perform(get("/api/cards/{id}/reveal", card.getId()).with(comoUsuarioAutenticado(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tras cancelar no se puede reemitir hasta el cooldown si no venció")
    void cancelBloqueaReissueHastaCooldown() {
        User user = userRepository.save(newUser("card.user.e"));
        Account account = accountRepository.save(newAccount(user, Currency.ARS));
        VirtualCard card = virtualCardService.createForAccount(account);

        VirtualCard cancelled = virtualCardService.cancel(card);
        assertEquals(CardStatus.CANCELLED, cancelled.getStatus());
        assertFalse(virtualCardService.reissueEligibility(cancelled).allowed());

        cancelled.setCancelledAt(
                java.time.LocalDateTime.now().minusDays(VirtualCardService.REISSUE_COOLDOWN_DAYS)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        VirtualCard renewed = virtualCardService.reissue(cancelled);
        assertEquals(CardStatus.ACTIVE, renewed.getStatus());
        assertTrue(renewed.getCancelledAt() == null || renewed.getCancelledAt().isBlank());
    }

    @Test
    @DisplayName("Tarjeta vencida puede reemitirse sin esperar cooldown")
    void tarjetaVencidaPuedeReemitirse() {
        User user = userRepository.save(newUser("card.user.f"));
        Account account = accountRepository.save(newAccount(user, Currency.USD));
        VirtualCard card = virtualCardService.createForAccount(account);
        card.setExpMonth(1);
        card.setExpYear(2020);
        virtualCardRepository.save(card);

        assertTrue(virtualCardService.reissueEligibility(card).allowed());
        VirtualCard renewed = virtualCardService.reissue(card);
        assertEquals(CardStatus.ACTIVE, renewed.getStatus());
        assertTrue(renewed.getExpYear() >= java.time.LocalDate.now().getYear());
    }

    private RequestPostProcessor comoUsuarioAutenticado(User user) {
        if (user.getCredentials() == null) {
            user.setCredentials(new Credentials(user, user.getAlias(), "irrelevante"));
        }
        CustomUserDetails principal = new CustomUserDetails(user);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return request -> {
            TestSecurityContextHolder.setAuthentication(auth);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    private static User newUser(String alias) {
        User user = new User("Test", "Card", "dni-" + alias, alias + "@test.local", alias);
        user.setPermissions(Permissions.USER);
        user.setEnabled(true);
        user.setActive(true);
        return user;
    }

    private static Account newAccount(User user, Currency currency) {
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(currency);
        account.setBalance(0);
        account.setAccountNickname("nick." + aliasSafe(user.getAlias()) + "." + currency.name().toLowerCase());
        account.setAccountCvu("cvu" + Math.abs(user.getAlias().hashCode()) + currency.name());
        return account;
    }

    private static String aliasSafe(String alias) {
        return alias.replace(".", "");
    }
}
