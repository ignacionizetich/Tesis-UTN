package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.FavoriteContactRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests de caracterizacion: describen el comportamiento ACTUAL del servicio,
 * incluidas las decisiones discutibles, para poder refactorizar con red.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteContactServiceTest {

    private static final long ID_USUARIO = 1L;
    private static final long ID_OTRO_USUARIO = 99L;
    private static final long ID_CUENTA = 10L;
    private static final long ID_FAVORITO = 500L;

    @Mock
    private FavoriteContactRepository favoriteContactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private FavoriteContactService favoriteContactService;

    @Captor
    private ArgumentCaptor<FavoriteContact> contactoGuardado;

    // --- addFavoriteContact ---

    @Test
    @DisplayName("No agrega el favorito si el usuario no existe")
    void addFavoriteContactDevuelveFalseSiElUsuarioNoExiste() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.empty());

        boolean resultado = favoriteContactService.addFavoriteContact(ID_USUARIO, ID_CUENTA, "Juan", null);

        assertFalse(resultado);
        verifyNoInteractions(favoriteContactRepository);
    }

    @Test
    @DisplayName("No agrega el favorito si la cuenta no existe")
    void addFavoriteContactDevuelveFalseSiLaCuentaNoExiste() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario(ID_USUARIO)));
        when(accountRepository.findById(ID_CUENTA)).thenReturn(Optional.empty());

        boolean resultado = favoriteContactService.addFavoriteContact(ID_USUARIO, ID_CUENTA, "Juan", null);

        assertFalse(resultado);
        verifyNoInteractions(favoriteContactRepository);
    }

    @Test
    @DisplayName("No permite agregarse la propia cuenta como favorita")
    void addFavoriteContactDevuelveFalseSiLaCuentaEsPropia() {
        User propietario = usuario(ID_USUARIO);
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(propietario));
        when(accountRepository.findById(ID_CUENTA)).thenReturn(Optional.of(cuenta(ID_CUENTA, propietario)));

        boolean resultado = favoriteContactService.addFavoriteContact(ID_USUARIO, ID_CUENTA, "Yo mismo", null);

        assertFalse(resultado);
        verify(favoriteContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("No agrega dos veces el mismo contacto")
    void addFavoriteContactDevuelveFalseSiYaExisteLaRelacion() {
        User propietario = usuario(ID_USUARIO);
        Account cuentaAjena = cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO));
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(propietario));
        when(accountRepository.findById(ID_CUENTA)).thenReturn(Optional.of(cuentaAjena));
        when(favoriteContactRepository.existsByOwnerAndFavoriteAccount(propietario, cuentaAjena)).thenReturn(true);

        boolean resultado = favoriteContactService.addFavoriteContact(ID_USUARIO, ID_CUENTA, "Juan", null);

        assertFalse(resultado);
        verify(favoriteContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("Agrega el favorito con el alias y la descripcion sin espacios sobrantes")
    void addFavoriteContactGuardaElContactoConDescripcionTrimeada() {
        User propietario = usuario(ID_USUARIO);
        Account cuentaAjena = cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO));
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(propietario));
        when(accountRepository.findById(ID_CUENTA)).thenReturn(Optional.of(cuentaAjena));
        when(favoriteContactRepository.existsByOwnerAndFavoriteAccount(propietario, cuentaAjena)).thenReturn(false);

        boolean resultado = favoriteContactService.addFavoriteContact(
                ID_USUARIO, ID_CUENTA, "Juan Perez", "  companiero de trabajo  ");

        assertTrue(resultado);
        verify(favoriteContactRepository).save(contactoGuardado.capture());
        FavoriteContact guardado = contactoGuardado.getValue();
        assertEquals("Juan Perez", guardado.getContactAlias());
        assertEquals("companiero de trabajo", guardado.getDescription());
        assertEquals(propietario, guardado.getOwner());
        assertEquals(cuentaAjena, guardado.getFavoriteAccount());
        assertTrue(guardado.isActive());
    }

    @Test
    @DisplayName("Deja la descripcion en null si llega vacia")
    void addFavoriteContactGuardaSinDescripcionSiLlegaEnBlanco() {
        User propietario = usuario(ID_USUARIO);
        Account cuentaAjena = cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO));
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(propietario));
        when(accountRepository.findById(ID_CUENTA)).thenReturn(Optional.of(cuentaAjena));
        when(favoriteContactRepository.existsByOwnerAndFavoriteAccount(propietario, cuentaAjena)).thenReturn(false);

        boolean resultado = favoriteContactService.addFavoriteContact(ID_USUARIO, ID_CUENTA, "Juan", "   ");

        assertTrue(resultado);
        verify(favoriteContactRepository).save(contactoGuardado.capture());
        assertNull(contactoGuardado.getValue().getDescription());
    }

    @Test
    @DisplayName("Se traga la excepcion del repositorio y devuelve false en vez de propagarla")
    void addFavoriteContactDevuelveFalseCuandoElRepositorioFalla() {
        // Por esto un fallo real de base de datos sale como 400 y no como 500:
        // el catch del controller casi nunca llega a ejecutarse.
        when(userRepository.findById(ID_USUARIO)).thenThrow(new RuntimeException("base de datos caida"));

        boolean resultado = favoriteContactService.addFavoriteContact(ID_USUARIO, ID_CUENTA, "Juan", null);

        assertFalse(resultado);
    }

    // --- consultas ---

    @Test
    @DisplayName("Devuelve lista vacia si el usuario no existe")
    void getFavoriteContactsByUserDevuelveListaVaciaSiElUsuarioNoExiste() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.empty());

        List<FavoriteContact> resultado = favoriteContactService.getFavoriteContactsByUser(ID_USUARIO);

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(favoriteContactRepository);
    }

    @Test
    @DisplayName("Devuelve solo los favoritos activos")
    void getFavoriteContactsByUserDevuelveSoloLosActivos() {
        User propietario = usuario(ID_USUARIO);
        FavoriteContact activo = favorito(ID_FAVORITO, propietario, cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO)));
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(propietario));
        when(favoriteContactRepository.findByOwnerAndActiveTrue(propietario)).thenReturn(List.of(activo));

        List<FavoriteContact> resultado = favoriteContactService.getFavoriteContactsByUser(ID_USUARIO);

        assertEquals(List.of(activo), resultado);
    }

    @Test
    @DisplayName("La variante reciente usa la consulta ordenada por ultimo uso")
    void getFavoriteContactsByUserOrderedByUsageUsaLaConsultaOrdenada() {
        User propietario = usuario(ID_USUARIO);
        FavoriteContact favorito = favorito(ID_FAVORITO, propietario, cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO)));
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(propietario));
        when(favoriteContactRepository.findByOwnerAndActiveTrueOrderByLastUsed(propietario))
                .thenReturn(List.of(favorito));

        List<FavoriteContact> resultado = favoriteContactService.getFavoriteContactsByUserOrderedByUsage(ID_USUARIO);

        assertEquals(List.of(favorito), resultado);
        verify(favoriteContactRepository, never()).findByOwnerAndActiveTrue(any());
    }

    // --- removeFavoriteContact ---

    @Test
    @DisplayName("No elimina un favorito inexistente")
    void removeFavoriteContactDevuelveFalseSiNoExiste() {
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.empty());

        assertFalse(favoriteContactService.removeFavoriteContact(ID_USUARIO, ID_FAVORITO));
        verify(favoriteContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("No elimina un favorito de otro usuario")
    void removeFavoriteContactDevuelveFalseSiElFavoritoEsDeOtroUsuario() {
        FavoriteContact ajeno = favorito(ID_FAVORITO, usuario(ID_OTRO_USUARIO), cuenta(ID_CUENTA, usuario(7L)));
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.of(ajeno));

        assertFalse(favoriteContactService.removeFavoriteContact(ID_USUARIO, ID_FAVORITO));
        verify(favoriteContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("El borrado es logico: marca el favorito como inactivo y no borra la fila")
    void removeFavoriteContactHaceBorradoLogico() {
        FavoriteContact propio = favorito(ID_FAVORITO, usuario(ID_USUARIO), cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO)));
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.of(propio));

        assertTrue(favoriteContactService.removeFavoriteContact(ID_USUARIO, ID_FAVORITO));

        verify(favoriteContactRepository).save(contactoGuardado.capture());
        assertFalse(contactoGuardado.getValue().isActive());
        verify(favoriteContactRepository, never()).delete(any());
    }

    // --- updateFavoriteContact ---

    @Test
    @DisplayName("No actualiza un favorito inexistente")
    void updateFavoriteContactDevuelveFalseSiNoExiste() {
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.empty());

        assertFalse(favoriteContactService.updateFavoriteContact(ID_FAVORITO, ID_USUARIO, "Nuevo", null));
    }

    @Test
    @DisplayName("No actualiza un favorito de otro usuario")
    void updateFavoriteContactDevuelveFalseSiElFavoritoEsDeOtroUsuario() {
        FavoriteContact ajeno = favorito(ID_FAVORITO, usuario(ID_OTRO_USUARIO), cuenta(ID_CUENTA, usuario(7L)));
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.of(ajeno));

        assertFalse(favoriteContactService.updateFavoriteContact(ID_FAVORITO, ID_USUARIO, "Nuevo", null));
        verify(favoriteContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("No actualiza un favorito ya dado de baja")
    void updateFavoriteContactDevuelveFalseSiElFavoritoEstaInactivo() {
        FavoriteContact inactivo = favorito(ID_FAVORITO, usuario(ID_USUARIO), cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO)));
        inactivo.setActive(false);
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.of(inactivo));

        assertFalse(favoriteContactService.updateFavoriteContact(ID_FAVORITO, ID_USUARIO, "Nuevo", null));
        verify(favoriteContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualiza el alias sin espacios sobrantes")
    void updateFavoriteContactActualizaElAliasTrimeado() {
        FavoriteContact propio = favorito(ID_FAVORITO, usuario(ID_USUARIO), cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO)));
        propio.setContactAlias("Alias viejo");
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.of(propio));

        assertTrue(favoriteContactService.updateFavoriteContact(ID_FAVORITO, ID_USUARIO, "  Alias nuevo  ", null));

        verify(favoriteContactRepository).save(contactoGuardado.capture());
        assertEquals("Alias nuevo", contactoGuardado.getValue().getContactAlias());
    }

    @Test
    @DisplayName("Con alias en blanco conserva el alias anterior")
    void updateFavoriteContactConservaElAliasSiLlegaEnBlanco() {
        FavoriteContact propio = favorito(ID_FAVORITO, usuario(ID_USUARIO), cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO)));
        propio.setContactAlias("Alias viejo");
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.of(propio));

        assertTrue(favoriteContactService.updateFavoriteContact(ID_FAVORITO, ID_USUARIO, "   ", "nueva nota"));

        verify(favoriteContactRepository).save(contactoGuardado.capture());
        assertEquals("Alias viejo", contactoGuardado.getValue().getContactAlias());
        assertEquals("nueva nota", contactoGuardado.getValue().getDescription());
    }

    @Test
    @DisplayName("Con descripcion null conserva la anterior, pero con string vacio la pisa")
    void updateFavoriteContactDistingueDescripcionNullDeVacia() {
        FavoriteContact propio = favorito(ID_FAVORITO, usuario(ID_USUARIO), cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO)));
        propio.setDescription("nota original");
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.of(propio));

        favoriteContactService.updateFavoriteContact(ID_FAVORITO, ID_USUARIO, "Alias", null);
        assertEquals("nota original", propio.getDescription());

        favoriteContactService.updateFavoriteContact(ID_FAVORITO, ID_USUARIO, "Alias", "");
        assertEquals("", propio.getDescription());
    }

    // --- updateLastUsedForContact ---

    @Test
    @DisplayName("Registra la fecha de ultimo uso del favorito")
    void updateLastUsedForContactRegistraLaFecha() {
        FavoriteContact propio = favorito(ID_FAVORITO, usuario(ID_USUARIO), cuenta(ID_CUENTA, usuario(ID_OTRO_USUARIO)));
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.of(propio));

        favoriteContactService.updateLastUsedForContact(ID_FAVORITO);

        verify(favoriteContactRepository).save(contactoGuardado.capture());
        assertNotNull(contactoGuardado.getValue().getLastUsed());
    }

    @Test
    @DisplayName("No hace nada si el favorito no existe")
    void updateLastUsedForContactNoHaceNadaSiNoExiste() {
        when(favoriteContactRepository.findById(ID_FAVORITO)).thenReturn(Optional.empty());

        favoriteContactService.updateLastUsedForContact(ID_FAVORITO);

        verify(favoriteContactRepository, never()).save(any());
    }

    // --- fixtures ---

    private User usuario(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setLastName("Gomez");
        user.setAlias("ana.gomez");
        return user;
    }

    private Account cuenta(Long id, User propietario) {
        Account account = new Account();
        account.setIdAccount(id);
        account.setUser(propietario);
        account.setAccountCvu("0000003100010000000001");
        account.setAccountNickname("ana.pesos");
        return account;
    }

    private FavoriteContact favorito(Long id, User propietario, Account cuentaFavorita) {
        FavoriteContact favorite = new FavoriteContact(propietario, cuentaFavorita, "Contacto");
        favorite.setId(id);
        return favorite;
    }
}
