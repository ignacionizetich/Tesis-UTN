package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AccountRepository extends JpaRepository<Account, Long> {
    /*ACA NO AGREGAMOS NADA, JPA MANEJA LAS OPERACIONES CRUD CON METODOS PREDEFINIDOS
     *
     * SI NECESITAMOS UN METODO QUE NO ESTA EN JPAREPOSITORY, PODEMOS CREAR NUESTRO METODO PROPIO
     * */

    /// este metodo nos va a servir para verificar si el cvu existe en la base de datos antes de que se cargue una nueva cuenta
    boolean existsByAccountCvu(String account_cvu);

    ///este metodo nos va a servir para verificar si el alias existe en la base de datos antes de que se cargue una nueva cuenta
    boolean existsByAccountNickname(String account_nickname);

    /// METODO PARA ENCONTRAR UNA CUENTA CON UN ALIAS
    Optional<Account> findByAccountNickname(String option1);


    /// METODO PARA ENCONTRAR UNA CUENTA CON UN CVU
    Optional<Account> findByAccountCvu(String option1);


    /// METODO PAR ENCONTRAR UNA CUENTA CON SU ID
    Optional<Account> findByIdAccount(Long id);

    /// METODO PAR ENCONTRAR LA CUENTA EN PESOS DE UN USUARIO (cuenta principal)
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.accountType = :type")
    Optional<Account> findArsAccountByUserId(@Param("userId") Long userId,
                                             @Param("type") Currency type);

    /// METODO PAR ENCONTRAR TODAS LAS CUENTAS DE UN USUARIO
    List<Account> findAllByUser_Id(Long userId);

    /// Compat: misma consulta ARS (login / perfil). Preferir findArsAccountByUserId.
    @Deprecated
    default Optional<Account> findByUser_Id(Long userId) {
        return findArsAccountByUserId(userId, Currency.ARS);
    }

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Account a WHERE a.user = :user AND a.accountType = :currency")
    boolean existsByUserAndAccountType(@Param("user") User user, @Param("currency") Currency currency);

    /**
     * Acredita un importe sumando en la base de datos, no en memoria.
     *
     * <p>Leer el saldo, sumarle el monto en Java y guardar la cuenta pierde acreditaciones
     * concurrentes: dos hilos leen el mismo saldo inicial y el ultimo en guardar sobreescribe
     * al otro. Delegar la suma al motor hace que cada UPDATE parta del valor ya comprometido.
     *
     * @return cantidad de filas afectadas: 0 si la cuenta no existe.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.idAccount = :id")
    int creditBalance(@Param("id") Long id, @Param("amount") double amount);

    /**
     * Debita un importe solo si el saldo alcanza, comprobandolo dentro del mismo UPDATE.
     *
     * <p>La condicion {@code a.balance >= :amount} viaja en el WHERE a proposito: si primero se
     * consultara el saldo y despues se descontara, dos debitos simultaneos podrian pasar ambos
     * el control y dejar la cuenta en negativo. Aca el motor evalua saldo y descuento de forma
     * atomica, asi que el segundo debito no afecta ninguna fila.
     *
     * @return cantidad de filas afectadas: 0 si la cuenta no existe o no tiene saldo suficiente.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.balance = a.balance - :amount WHERE a.idAccount = :id AND a.balance >= :amount")
    int debitBalance(@Param("id") Long id, @Param("amount") double amount);
}
