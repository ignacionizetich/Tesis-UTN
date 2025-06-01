package com.EDJ.ArCash;

import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@Transactional
class ArCashApplicationTests {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    static void loadEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
    }

    @Test
    void testFindAllByUserAndRevokedFalse() {
        User user = new User();
        user.setName("testUser");
        user.setLastName("testLastName");
        user.setDni("12345678");
        user.setEmail("test@example.com");
        user.setAlias("testAlias");
        user.setEnabled(true);
        userRepository.save(user);

        RefreshToken token1 = new RefreshToken(null, user, "token1", LocalDateTime.now(), LocalDateTime.now().plusDays(7), false);
        RefreshToken token2 = new RefreshToken(null, user, "token2", LocalDateTime.now(), LocalDateTime.now().plusDays(7), true);
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);

        List<RefreshToken> activos = refreshTokenRepository.findAllByUserAndRevokedFalse(user);

        System.out.println("Tokens activos: " + activos);
        assertEquals(1, activos.size());
        assertEquals("token1", activos.get(0).getRefreshToken());
    }
}