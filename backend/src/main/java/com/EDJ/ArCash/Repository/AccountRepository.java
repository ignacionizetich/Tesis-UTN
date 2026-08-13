package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
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


}
