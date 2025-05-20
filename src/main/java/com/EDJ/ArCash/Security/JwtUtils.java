package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
}