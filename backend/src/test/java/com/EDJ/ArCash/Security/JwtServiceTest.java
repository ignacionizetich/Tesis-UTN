package com.EDJ.ArCash.Security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caracterizacion criptografica de JwtService: emitir y parsear con la misma clave.
 */
class JwtServiceTest {

    private static final String SECRET_BASE64 =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String USER_ID = "42";
    private static final String ROLE = "USER";

    private JwtService jwtService;
    private Key secretKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_BASE64);
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
    }

    @Test
    @DisplayName("Un access token se parsea con los claims esperados")
    void accessTokenRoundtrip() {
        String token = jwtService.generateToken(USER_ID, ROLE);

        var claims = jwtService.getClaimJWT(token);

        assertEquals(USER_ID, claims.getSubject());
        assertEquals(USER_ID, claims.get("userID", String.class));
        assertEquals(ROLE, claims.get("role", String.class));
        assertEquals(JwtService.TYPE_ACCESS, claims.get(JwtService.CLAIM_TYPE, String.class));
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    @DisplayName("Un refresh token se parsea con type=refresh y sin userID")
    void refreshTokenRoundtrip() {
        String token = jwtService.generateRefreshToken(USER_ID, ROLE);

        var claims = jwtService.getClaimJWT(token);

        assertEquals(USER_ID, claims.getSubject());
        assertEquals(ROLE, claims.get("role", String.class));
        assertEquals(JwtService.TYPE_REFRESH, claims.get(JwtService.CLAIM_TYPE, String.class));
        assertEquals(null, claims.get("userID", String.class));
    }

    @Test
    @DisplayName("extractUserId lee el claim userID del access token")
    void extractUserIdLeeElClaim() {
        String token = jwtService.generateToken(USER_ID, ROLE);

        assertEquals(USER_ID, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Un token vencido lanza al parsear")
    void tokenVencidoLanzaAlParsear() {
        String vencido = Jwts.builder()
                .setSubject(USER_ID)
                .claim("userID", USER_ID)
                .claim("role", ROLE)
                .claim(JwtService.CLAIM_TYPE, JwtService.TYPE_ACCESS)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(secretKey)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.getClaimJWT(vencido));
    }

    @Test
    @DisplayName("Un token con la firma alterada lanza al parsear")
    void firmaAlteradaLanzaAlParsear() {
        String valido = jwtService.generateToken(USER_ID, ROLE);
        char ultimo = valido.charAt(valido.length() - 1);
        String alterado = valido.substring(0, valido.length() - 1) + (ultimo == 'A' ? 'B' : 'A');

        assertThrows(SignatureException.class, () -> jwtService.getClaimJWT(alterado));
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza")
    void otraClaveSeRechaza() {
        Key otraClave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(
                "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"));
        String ajeno = Jwts.builder()
                .setSubject(USER_ID)
                .claim("userID", USER_ID)
                .claim("role", ROLE)
                .claim(JwtService.CLAIM_TYPE, JwtService.TYPE_ACCESS)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(otraClave)
                .compact();

        assertThrows(JwtException.class, () -> jwtService.getClaimJWT(ajeno));
    }
}
