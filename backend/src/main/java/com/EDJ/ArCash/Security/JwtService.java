package com.EDJ.ArCash.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

/**
 * Emision y parseo de JWT. Dueño unico de la clave secreta.
 * El estado de sesion (refresh tokens en base) vive en SessionService.
 */
@Component
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final Key secretKey;

    public JwtService(@Value("${spring.jwt.secret}") String signedJwt) {
        byte[] keyBytes = Base64.getDecoder().decode(signedJwt);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String idUser, String role) {
        return Jwts.builder()
                .setSubject(idUser)
                .claim("role", role)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String idUser, String role) {
        return Jwts.builder()
                .setSubject(idUser)
                .claim("role", role)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600000))
                .signWith(secretKey)
                .compact();
    }

    public Claims getClaimJWT(String token) {
        JwtParserBuilder parserBuilder = Jwts.parserBuilder();
        return parserBuilder.setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    public String extractUserId(String token) {
        Claims claims = getClaimJWT(token);
        return claims.getSubject();
    }
}
