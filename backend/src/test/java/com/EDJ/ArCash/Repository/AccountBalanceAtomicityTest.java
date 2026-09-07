package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comprueba contra una base real (H2) que el saldo se mueve dentro del UPDATE.
 *
 * <p>Los tests de servicio corren con el repositorio mockeado, asi que validan como reacciona
 * la aplicacion al resultado del debito, pero no que el SQL sea correcto. Aca se ejecutan las
 * consultas de verdad: si el guard del WHERE desapareciera, estos tests se caen.
 */
@DataJpaTest
@ActiveProfiles("test")
class AccountBalanceAtomicityTest {

    private static final double DELTA = 0.0001;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("creditBalance suma sobre el valor ya comprometido en la base")
    void creditBalanceSumaEnLaBase() {
        Account cuenta = cuentaConSaldo(1000.0);

        int filas = accountRepository.creditBalance(cuenta.getIdAccount(), 250.50);

        assertEquals(1, filas);
        assertEquals(1250.50, saldoEnLaBase(cuenta), DELTA);
    }

    @Test
    @DisplayName("debitBalance descuenta cuando el saldo alcanza")
    void debitBalanceDescuentaConSaldoSuficiente() {
        Account cuenta = cuentaConSaldo(1000.0);

        int filas = accountRepository.debitBalance(cuenta.getIdAccount(), 400.0);

        assertEquals(1, filas);
        assertEquals(600.0, saldoEnLaBase(cuenta), DELTA);
    }

    @Test
    @DisplayName("debitBalance permite dejar la cuenta exactamente en cero")
    void debitBalancePermiteVaciarLaCuenta() {
        Account cuenta = cuentaConSaldo(750.0);

        int filas = accountRepository.debitBalance(cuenta.getIdAccount(), 750.0);

        assertEquals(1, filas);
        assertEquals(0.0, saldoEnLaBase(cuenta), DELTA);
    }

    @Test
    @DisplayName("debitBalance no afecta ninguna fila si el saldo no alcanza y deja el saldo intacto")
    void debitBalanceRechazaSinSaldoSuficiente() {
        Account cuenta = cuentaConSaldo(100.0);

        int filas = accountRepository.debitBalance(cuenta.getIdAccount(), 100.01);

        // 0 filas es la senal que usa el servicio para rechazar la operacion.
        assertEquals(0, filas);
        assertEquals(100.0, saldoEnLaBase(cuenta), DELTA);
    }

    @Test
    @DisplayName("Dos debitos que juntos exceden el saldo: el segundo no afecta ninguna fila")
    void segundoDebitoNoSobregiraLaCuenta() {
        Account cuenta = cuentaConSaldo(300.0);
        Long id = cuenta.getIdAccount();

        assertEquals(1, accountRepository.debitBalance(id, 200.0));
        assertEquals(0, accountRepository.debitBalance(id, 200.0));

        assertEquals(100.0, saldoEnLaBase(cuenta), DELTA);
    }

    @Test
    @DisplayName("Las consultas de saldo no afectan cuentas inexistentes")
    void cuentaInexistenteNoAfectaFilas() {
        assertEquals(0, accountRepository.creditBalance(-1L, 100.0));
        assertEquals(0, accountRepository.debitBalance(-1L, 100.0));
    }

    private Account cuentaConSaldo(double saldo) {
        Account account = new Account();
        account.setAccountType(Currency.ARS);
        account.setBalance(saldo);
        account.setAccountNickname("alias." + System.nanoTime());
        account.setAccountCvu("cvu." + System.nanoTime());
        return accountRepository.saveAndFlush(account);
    }

    /**
     * Relee el saldo salteando la entidad en memoria: las consultas @Modifying limpian el
     * contexto, y leer el getter de la instancia original mostraria el valor viejo.
     */
    private double saldoEnLaBase(Account cuenta) {
        return accountRepository.findByIdAccount(cuenta.getIdAccount())
                .orElseThrow()
                .getBalance();
    }
}
