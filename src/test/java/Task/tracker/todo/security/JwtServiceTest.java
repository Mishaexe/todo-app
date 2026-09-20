package Task.tracker.todo.security;

import Task.tracker.todo.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "my-very-secret-key-that-is-long-enough-for-hs256-algorithm-123456");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1000L * 60 * 60);
    }

    @Test
    @DisplayName("generateToken должен вернуть непустую строку")
    void generateToken_shouldReturnNonEmptyString() {

        User user = new User();
        user.setUsername("testUser");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("extractUsername должен вернуть правильный username из токена")
    void extractUsername_shouldReturnCorrectUsername() {

        User user = new User();
        user.setUsername("testUser");

        String token = jwtService.generateToken(user);

        String extractedUsername = jwtService.extractUsername(token);

        assertEquals("testUser", extractedUsername);
    }

    @Test
    @DisplayName("isTokenValid должен вернуть true для валидного токена и правильного пользователя")
    void isTokenValid_withValidToken_shouldReturnTrue() {
        User user = new User();
        user.setUsername("testUser");

        String token = jwtService.generateToken(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testUser")
                .password("password")
                .authorities("USER")
                .build();

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("isTokenValid должен вернуть false, если username не совпадает")
    void isTokenValid_withWrongUsername_shouldReturnFalse() {
        User user = new User();
        user.setUsername("testUser");

        String token = jwtService.generateToken(user);

        UserDetails anotherUser = org.springframework.security.core.userdetails.User
                .withUsername("anotherUser")
                .password("password")
                .authorities("USER")
                .build();

        boolean isValid = jwtService.isTokenValid(token, anotherUser);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("isTokenValid должен вернуть false для просроченного токена")
    void isTokenValid_withExpiredToken_shouldReturnFalse() throws InterruptedException {

        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L);

        User user = new User();
        user.setUsername("testUser");
        String token = jwtService.generateToken(user);

        Thread.sleep(10);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testUser")
                .password("password")
                .authorities("USER")
                .build();

        assertThrows(ExpiredJwtException.class, () -> {
           jwtService.isTokenValid(token, userDetails);
        });
    }

    @Test
    void shouldThrowException_whenTokenIsInvalid() {

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testUser")
                .password("password")
                .authorities("USER")
                .build();

        String fakeToken = "eyJfake.token.here";

        assertThrows(MalformedJwtException.class,
                () -> jwtService.isTokenValid(fakeToken, userDetails)
                );

    }
}
