package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.FavoriteContactService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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
 * Contrato HTTP de /api/favorites con AuthenticationPrincipal.
 * addFilters=false para ejercer la rama principal==null (401 del controller).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FavoriteContactControllerTest {

    private static final Long ID_USUARIO = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteContactService favoriteContactService;

    @AfterEach
    void limpiarSecurityContext() {
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();
    }

    // --- POST /add ---

    @Test
    @DisplayName("El alta exitosa devuelve 200 con status SUCCESS")
    void addDevuelveSuccess() throws Exception {
        when(favoriteContactService.addFavoriteContact(ID_USUARIO, 10L, "Juancito", "amigo")).thenReturn(true);

        mockMvc.perform(post("/api/favorites/add")
                        .with(comoUsuarioAutenticado())
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
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":10,\"contactAlias\":\"Juancito\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("No se pudo agregar el contacto a favoritos"));
    }

    @Test
    @DisplayName("Sin principal responde el 401 propio del controller")
    void addDevuelveUnauthorizedSinPrincipal() throws Exception {
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();

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
        mockMvc.perform(post("/api/favorites/add")
                        .with(comoUsuarioAutenticado())
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

        mockMvc.perform(get("/api/favorites/list").with(comoUsuarioAutenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.favorites.length()").value(1))
                .andExpect(jsonPath("$.favorites[0].id").value(77))
                .andExpect(jsonPath("$.favorites[0].contactAlias").value("Juancito"))
                .andExpect(jsonPath("$.favorites[0].description").value("amigo"))
                .andExpect(jsonPath("$.favorites[0].creationDate").value("2026-01-15 10:30:00"))
                .andExpect(jsonPath("$.favorites[0].lastUsed").isEmpty())
                .andExpect(jsonPath("$.favorites[0].active").value(true))
                .andExpect(jsonPath("$.favorites[0].accountOwnerName").value("Juan Perez"))
                .andExpect(jsonPath("$.favorites[0].accountOwnerAlias").value("juan.perez"))
                .andExpect(jsonPath("$.favorites[0].accountCbu").value("0000003100010000000001"))
                .andExpect(jsonPath("$.favorites[0].accountAlias").value("juan.pesos"))
                .andExpect(jsonPath("$.favorites[0].accountType").value("PESOS"));
    }

    @Test
    @DisplayName("Sin favoritos devuelve una lista vacia, no un 404")
    void listDevuelveListaVacia() throws Exception {
        when(favoriteContactService.getFavoriteContactsByUser(ID_USUARIO)).thenReturn(List.of());

        mockMvc.perform(get("/api/favorites/list").with(comoUsuarioAutenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.favorites.length()").value(0));
    }

    @Test
    @DisplayName("El listado reciente usa la consulta ordenada por ultimo uso")
    void listRecentUsaLaConsultaOrdenada() throws Exception {
        when(favoriteContactService.getFavoriteContactsByUserOrderedByUsage(ID_USUARIO))
                .thenReturn(List.of(favoritoDeEjemplo()));

        mockMvc.perform(get("/api/favorites/list/recent").with(comoUsuarioAutenticado()))
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

        mockMvc.perform(delete("/api/favorites/77").with(comoUsuarioAutenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Contacto eliminado de favoritos correctamente"));
    }

    @Test
    @DisplayName("Si el servicio devuelve false el borrado sale con 400")
    void deleteDevuelveBadRequestSiElServicioRechaza() throws Exception {
        when(favoriteContactService.removeFavoriteContact(ID_USUARIO, 77L)).thenReturn(false);

        mockMvc.perform(delete("/api/favorites/77").with(comoUsuarioAutenticado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("No se pudo eliminar el contacto favorito"));
    }

    // --- PUT /update ---

    @Test
    @DisplayName("La edicion exitosa devuelve 200 con status SUCCESS")
    void updateDevuelveSuccess() throws Exception {
        when(favoriteContactService.updateFavoriteContactForOwner(eq(77L), eq(ID_USUARIO), any(), any()))
                .thenReturn(com.EDJ.ArCash.Service.FavoriteUpdateResult.ok());

        mockMvc.perform(put("/api/favorites/update/77")
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactAlias\":\"Nuevo alias\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Contacto favorito actualizado correctamente"));
    }

    @Test
    @DisplayName("La edicion sin ningun campo devuelve 400")
    void updateSinCamposDevuelveBadRequest() throws Exception {
        when(favoriteContactService.updateFavoriteContactForOwner(eq(77L), eq(ID_USUARIO), any(), any()))
                .thenReturn(com.EDJ.ArCash.Service.FavoriteUpdateResult.badRequest());

        mockMvc.perform(put("/api/favorites/update/77")
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Debe proporcionar al menos un campo para actualizar"));
    }

    @Test
    @DisplayName("Si el servicio devuelve false la edicion sale con 404, no con 400")
    void updateDevuelveNotFoundSiElServicioRechaza() throws Exception {
        when(favoriteContactService.updateFavoriteContactForOwner(eq(77L), eq(ID_USUARIO), any(), any()))
                .thenReturn(com.EDJ.ArCash.Service.FavoriteUpdateResult.notFound());

        mockMvc.perform(put("/api/favorites/update/77")
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactAlias\":\"Nuevo alias\"}"))
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
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactAlias\":\"" + aliasLargo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    private RequestPostProcessor comoUsuarioAutenticado() {
        CustomUserDetails principal = new CustomUserDetails(usuario());
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return request -> {
            TestSecurityContextHolder.setAuthentication(auth);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    private User usuario() {
        User user = new User();
        user.setId(ID_USUARIO);
        user.setPermissions(Permissions.USER);
        user.setCredentials(new Credentials(user, "ana.gomez", "irrelevante"));
        return user;
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
