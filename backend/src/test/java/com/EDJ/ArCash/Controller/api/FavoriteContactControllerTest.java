package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.JwtUtils;
import com.EDJ.ArCash.Service.FavoriteContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de caracterizacion del contrato HTTP de /api/favorites.
 *
 * Se corre con addFilters = false porque el controller parsea el JWT por su
 * cuenta desde el header Authorization: con la cadena de filtros activa, un
 * token de prueba seria rechazado con 401 antes de llegar al handler, y la
 * rama de 401 propia del controller quedaria inalcanzable.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FavoriteContactControllerTest {

    private static final String TOKEN = "token-de-prueba";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final Long ID_USUARIO = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteContactService favoriteContactService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        when(jwtUtils.extractUserId(TOKEN)).thenReturn(String.valueOf(ID_USUARIO));
    }

    // --- POST /add ---

    @Test
    @DisplayName("El alta exitosa devuelve 200 con status SUCCESS")
    void addDevuelveSuccess() throws Exception {
        when(favoriteContactService.addFavoriteContact(ID_USUARIO, 10L, "Juancito", "amigo")).thenReturn(true);

        mockMvc.perform(post("/api/favorites/add")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":10,\"contactAlias\":\"Juancito\",\"description\":\"amigo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Contacto agregado a favoritos correctamente"));
    }

    @Test
    @DisplayName("Si el servicio devuelve false el alta sale con 400 y un mensaje generico")
    void addDevuelveBadRequestSiElServicioRechaza() throws Exception {
        when(favoriteContactService.addFavoriteContact(anyLong(), anyLong(), any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/favorites/add")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":10,\"contactAlias\":\"Juancito\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                // El mensaje no distingue entre cuenta propia, duplicado o cuenta inexistente.
                .andExpect(jsonPath("$.message").value("No se pudo agregar el contacto a favoritos"));
    }

    @Test
    @DisplayName("Sin header Authorization responde el 401 propio del controller")
    void addDevuelveUnauthorizedSinHeader() throws Exception {
        mockMvc.perform(post("/api/favorites/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":10,\"contactAlias\":\"Juancito\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Token no proporcionado o inválido"));

        verify(favoriteContactService, never()).addFavoriteContact(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Sin contactAlias revienta con NPE y sale como 500 por el handler global")
    void addSinAliasDevuelveErrorInternoDelHandlerGlobal() throws Exception {
        // AddFavoriteContactRequest no tiene ninguna constraint pese al @Valid,
        // asi que el alias null llega hasta el .trim() y lanza NullPointerException.
        // Al sacar el try/catch del controller el cuerpo dejo de ser
        // {"status":"ERROR","message":...} y paso al formato de ErrorResponse.
        // El codigo HTTP y el texto de message, que es lo que lee el frontend,
        // siguen siendo los mismos.
        mockMvc.perform(post("/api/favorites/add")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":10}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/favorites/add"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    // --- GET /list y /list/recent ---

    @Test
    @DisplayName("El listado devuelve el contacto con accountType fijo en PESOS")
    void listDevuelveElContactoConAccountTypeHardcodeado() throws Exception {
        when(favoriteContactService.getFavoriteContactsByUser(ID_USUARIO))
                .thenReturn(List.of(favoritoDeEjemplo()));

        mockMvc.perform(get("/api/favorites/list").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.favorites.length()").value(1))
                .andExpect(jsonPath("$.favorites[0].id").value(77))
                .andExpect(jsonPath("$.favorites[0].contactAlias").value("Juancito"))
                .andExpect(jsonPath("$.favorites[0].description").value("amigo"))
                .andExpect(jsonPath("$.favorites[0].creationDate").value("2026-01-15 10:30:00"))
                .andExpect(jsonPath("$.favorites[0].lastUsed").isEmpty())
                .andExpect(jsonPath("$.favorites[0].active").value(true))
                // El frontend parte este string por espacios para reconstruir nombre y apellido.
                .andExpect(jsonPath("$.favorites[0].accountOwnerName").value("Juan Perez"))
                .andExpect(jsonPath("$.favorites[0].accountOwnerAlias").value("juan.perez"))
                .andExpect(jsonPath("$.favorites[0].accountCbu").value("0000003100010000000001"))
                .andExpect(jsonPath("$.favorites[0].accountAlias").value("juan.pesos"))
                // La cuenta del fixture es en USD y aun asi se informa PESOS.
                .andExpect(jsonPath("$.favorites[0].accountType").value("PESOS"));
    }

    @Test
    @DisplayName("Sin favoritos devuelve una lista vacia, no un 404")
    void listDevuelveListaVacia() throws Exception {
        when(favoriteContactService.getFavoriteContactsByUser(ID_USUARIO)).thenReturn(List.of());

        mockMvc.perform(get("/api/favorites/list").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.favorites.length()").value(0));
    }

    @Test
    @DisplayName("El listado reciente usa la consulta ordenada por ultimo uso")
    void listRecentUsaLaConsultaOrdenada() throws Exception {
        when(favoriteContactService.getFavoriteContactsByUserOrderedByUsage(ID_USUARIO))
                .thenReturn(List.of(favoritoDeEjemplo()));

        mockMvc.perform(get("/api/favorites/list/recent").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.favorites.length()").value(1));

        verify(favoriteContactService, never()).getFavoriteContactsByUser(anyLong());
    }

    // --- DELETE ---

    @Test
    @DisplayName("El borrado exitoso devuelve 200 con status SUCCESS")
    void deleteDevuelveSuccess() throws Exception {
        when(favoriteContactService.removeFavoriteContact(ID_USUARIO, 77L)).thenReturn(true);

        mockMvc.perform(delete("/api/favorites/77").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Contacto eliminado de favoritos correctamente"));
    }

    @Test
    @DisplayName("Si el servicio devuelve false el borrado sale con 400")
    void deleteDevuelveBadRequestSiElServicioRechaza() throws Exception {
        when(favoriteContactService.removeFavoriteContact(ID_USUARIO, 77L)).thenReturn(false);

        mockMvc.perform(delete("/api/favorites/77").header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("No se pudo eliminar el contacto favorito"));
    }

    // --- PUT /update ---

    @Test
    @DisplayName("La edicion exitosa devuelve 200 con status SUCCESS")
    void updateDevuelveSuccess() throws Exception {
        when(favoriteContactService.updateFavoriteContact(eq(77L), eq(ID_USUARIO), any(), any())).thenReturn(true);

        mockMvc.perform(put("/api/favorites/update/77")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactAlias\":\"Nuevo alias\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Contacto favorito actualizado correctamente"));
    }

    @Test
    @DisplayName("La edicion sin ningun campo devuelve 400")
    void updateSinCamposDevuelveBadRequest() throws Exception {
        mockMvc.perform(put("/api/favorites/update/77")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Debe proporcionar al menos un campo para actualizar"));

        verify(favoriteContactService, never()).updateFavoriteContact(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Si el servicio devuelve false la edicion sale con 404, no con 400")
    void updateDevuelveNotFoundSiElServicioRechaza() throws Exception {
        when(favoriteContactService.updateFavoriteContact(eq(77L), eq(ID_USUARIO), any(), any())).thenReturn(false);

        mockMvc.perform(put("/api/favorites/update/77")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactAlias\":\"Nuevo alias\"}"))
                // 404 aunque el mismo mensaje admite que puede tratarse de un contacto ajeno.
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("No se pudo actualizar el contacto. Verifique que existe y le pertenece."));
    }

    @Test
    @DisplayName("Un alias demasiado largo sale por el handler global con otro formato de error")
    void updateConAliasLargoDevuelveElFormatoDelHandlerGlobal() throws Exception {
        String aliasLargo = "a".repeat(51);

        mockMvc.perform(put("/api/favorites/update/77")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactAlias\":\"" + aliasLargo + "\"}"))
                .andExpect(status().isBadRequest())
                // Este cuerpo no se parece al de los errores manuales del mismo controller.
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    private FavoriteContact favoritoDeEjemplo() {
        User duenio = new User();
        duenio.setName("Juan");
        duenio.setLastName("Perez");
        duenio.setAlias("juan.perez");

        Account cuentaFavorita = new Account();
        cuentaFavorita.setUser(duenio);
        cuentaFavorita.setAccountCvu("0000003100010000000001");
        cuentaFavorita.setAccountNickname("juan.pesos");
        cuentaFavorita.setAccountType(Currency.USD);

        User propietario = new User();
        propietario.setId(ID_USUARIO);

        FavoriteContact favorito = new FavoriteContact(propietario, cuentaFavorita, "Juancito");
        favorito.setId(77L);
        favorito.setDescription("amigo");
        favorito.setCreationDate("2026-01-15 10:30:00");
        return favorito;
    }
}
