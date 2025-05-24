package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.DTO.AccountResponse;
import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtils {

    private static Key secretKey;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public JwtUtils(@Value("${spring.jwt.secret}") String signedJwt) {
        secretKey = Keys.hmacShaKeyFor(signedJwt.getBytes(StandardCharsets.UTF_8));
    }



    public static String generateToken(String idUser) {
        return Jwts.builder()
                .setSubject(idUser)
                .claim("userID",idUser)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();
    }

    public static String generateRefreshToken(String idUser) {
        return Jwts.builder()
                .setSubject(idUser)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600000)) // Expira en 7 días
                .signWith(secretKey)
                .compact();
    }

    public boolean isTokenValid(String refreshToken) {
        try {
            Claims claims = getClaimJWT(refreshToken);
            Date expiration = claims.getExpiration();

            // Check if token exists and is not revoked
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByRefreshToken(refreshToken);
            if (tokenOpt.isEmpty() || tokenOpt.get().isRevoked()) {
                return false;
            }

            return expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeRefreshToken(String refreshToken) {
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByRefreshToken(refreshToken);
        if (tokenOptional.isPresent()) {
            RefreshToken token = tokenOptional.get();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
    }


    public static Claims getClaimJWT(String token){
        JwtParserBuilder parserBuilder = Jwts.parser();
        return parserBuilder.setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }


    public ResponseEntity<?> validateAccessToken(HttpServletRequest request){

        // 1. Obtener token de la cabecera Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(498).body(new AccountResponse(false, "Token no proporcionado", 0));
        }
        String token = authHeader.substring(7);

        // 2. Extraer claims
        Claims claims = JwtUtils.getClaimJWT(token);
        String userIdStr = claims.get("userID", String.class);
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

        if (userId == null) {
            return ResponseEntity.status(498).body(new AccountResponse(false, "Token inválido o sin userId",0));
        }

       return ResponseEntity.status(200).body(userId);
    }
}