package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByDni(String dni);

    @Query("SELECT u FROM User u JOIN u.credentials c WHERE c.username = :alias")
    Optional<User> findByAlias(@Param("alias") String alias);

    List<User> findByEnabledTrue();
    /// aca no agregamos nada, JPA maneja las operaciones CRUD con metodos predefinidos

    List<User> findAllByEmail(String email);
}
