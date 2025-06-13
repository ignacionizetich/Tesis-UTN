package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Account;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    /// METODO PAR ENCONTRAR UNA CUENTA CON EL ID DEL USUARIO
    Optional<Account> findByUser_Id(Long userId);


}
