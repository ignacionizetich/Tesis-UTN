package com.EDJ.ArCash.Controller.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Consultar el saldo sin token devuelve 401")
    void consultarSaldoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/accounts/1/showBalance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Pedir los datos del QR sin token devuelve 401")
    void qrDataSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/accounts/1/qr-data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ingresar dinero sin token devuelve 401")
    void ingresarDineroSinTokenDevuelve401() throws Exception {
        mockMvc.perform(put("/api/accounts/1/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cambiar el alias sin token devuelve 401")
    void cambiarAliasSinTokenDevuelve401() throws Exception {
        mockMvc.perform(put("/api/accounts/1/changeAlias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newAlias\":\"mi.alias.nuevo\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Listar las cuentas sin token devuelve 401")
    void listarCuentasSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/accounts/user-accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Abrir una cuenta en dolares sin token devuelve 401")
    void abrirCuentaUsdSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/accounts/usd"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Comprar dolares sin token devuelve 401")
    void comprarDolaresSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/transactions/1/buy-usd/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isUnauthorized());
    }
}
