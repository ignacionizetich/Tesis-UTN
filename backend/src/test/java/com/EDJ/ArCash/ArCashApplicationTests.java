package com.EDJ.ArCash;

import com.EDJ.ArCash.DTO.AuthDTO.TransactionDTO;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.Transaction;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.TransactionRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Service.interfaces.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArCashApplicationTests {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("El contexto de Spring levanta con todos los beans de la aplicacion")
    void contextLoads() {
        assertNotNull(transactionService);
        assertNotNull(userRepository);
        assertNotNull(accountRepository);
        assertNotNull(transactionRepository);
    }

    @Test
    @DisplayName("listaTransacciones devuelve las transacciones de la cuenta mapeadas a TransactionDTO")
    void listaTransaccionesDevuelveLasTransaccionesDeLaCuenta() {
        Account origen = crearCuenta("origen");
        Account destino = crearCuenta("destino");
        crearTransaccion(origen, destino, 1500.0);

        List<TransactionDTO> resultado = transactionService.listaTransacciones(origen.getIdAccount());

        assertEquals(1, resultado.size());

        TransactionDTO dto = resultado.get(0);
        assertEquals(origen.getIdAccount(), dto.getIdOrigin());
        assertEquals(destino.getIdAccount(), dto.getIdDestination());
        assertEquals(1500.0, dto.getAmount());
        assertEquals("COMPLETED", dto.getState());
        assertEquals(Currency.ARS.toString(), dto.getCurrency());
        assertEquals(origen.getAccountNickname(), dto.getOriginAlias());
        assertEquals(destino.getAccountNickname(), dto.getDestinationAlias());
        assertEquals(origen.getUser().getAlias(), dto.getOriginUsername());
        assertEquals(destino.getUser().getAlias(), dto.getDestinationUsername());
        assertEquals("Juan Perez", dto.getOriginFullName());
        assertEquals("Juan Perez", dto.getDestinationFullName());
        assertFalse(dto.getSameOwner());
        assertFalse(dto.getConverted());
        assertNotNull(dto.getIdOperation());
        assertNotNull(dto.getDate());
    }

    @Test
    @DisplayName("listaTransacciones devuelve lista vacia si la cuenta no existe")
    void listaTransaccionesDevuelveListaVaciaSiLaCuentaNoExiste() {
        List<TransactionDTO> resultado = transactionService.listaTransacciones(-1L);

        assertTrue(resultado.isEmpty());
    }

    private Account crearCuenta(String sufijo) {
        User user = new User("Juan", "Perez", "dni-" + sufijo, sufijo + "@arcash.test", "usuario." + sufijo);
        user.setPermissions(Permissions.USER);
        user.setEnabled(true);
        user.setActive(true);
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setAccountType(Currency.ARS);
        account.setBalance(0.0);
        account.setAccountNickname("cuenta." + sufijo);
        account.setAccountCvu("cvu-" + sufijo);
        return accountRepository.save(account);
    }

    private void crearTransaccion(Account origen, Account destino, double monto) {
        Transaction transaction = new Transaction();
        transaction.setIdOrigin(origen);
        transaction.setIdDestination(destino);
        transaction.setBalance(monto);
        transaction.setState("COMPLETED");
        transaction.setCurrency(Currency.ARS);
        transactionRepository.save(transaction);
    }
}
