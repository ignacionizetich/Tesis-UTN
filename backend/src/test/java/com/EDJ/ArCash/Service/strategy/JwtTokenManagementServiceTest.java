package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.Security.JwtService;
import com.EDJ.ArCash.Service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenManagementServiceTest {

    private JwtService jwtService;
    private SessionService sessionService;
    private JwtTokenManagementService service;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        sessionService = mock(SessionService.class);
        service = new JwtTokenManagementService();
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        ReflectionTestUtils.setField(service, "sessionService", sessionService);
    }

    @Test
    @DisplayName("isValidSession es true si el access token apunta a un usuario con sesion activa")
    void sesionValida() {
        when(jwtService.extractUserId("token")).thenReturn("5");
        when(sessionService.tieneSesionActiva(5L)).thenReturn(true);

        assertTrue(service.isValidSession("token"));
    }

    @Test
    @DisplayName("isValidSession es false si no hay sesion activa")
    void sesionInvalidaSinRefresh() {
        when(jwtService.extractUserId("token")).thenReturn("5");
        when(sessionService.tieneSesionActiva(5L)).thenReturn(false);

        assertFalse(service.isValidSession("token"));
    }

    @Test
    @DisplayName("isValidSession es false si no se puede extraer el userId")
    void sesionInvalidaSinUserId() {
        when(jwtService.extractUserId("token")).thenThrow(new RuntimeException("bad token"));

        assertFalse(service.isValidSession("token"));
    }
}
