package Task.tracker.todo.auth;

import Task.tracker.todo.entity.User;
import Task.tracker.todo.exception.UserNotFoundException;
import Task.tracker.todo.repository.UserRepository;
import Task.tracker.todo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                authenticationManager
        );
    }

    private User createUser(String userName, String email, String password) {
        User user = new User();
        user.setUsername(userName);
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }

    @Test
    @DisplayName("Успешная регистрация должна вернуть JWT токен")
    void register_withValidData_shouldReturnToken() {

        AuthRequest request = new AuthRequest("testUser", "password123");
        User user = createUser("testUser",
                "testEmail@test.com",
                "password123");

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any());
    }

    @Test
    @DisplayName("Регистрация с существующим username должна выбросить исключение")
    void register_whenUsernameExists_shouldThrowException() {

        AuthRequest request =
                new AuthRequest("testUser", "password123");

        when(userRepository.findByUsername("testUser"))
                .thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(request)
        );

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успешный логин должен вернуть JWT токен")
    void login_withValidCredentials_shouldReturnToken() {

        AuthRequest request = new AuthRequest("testUser", "password123");
        User user = createUser("testUser", "testMail@test.com", "password123");

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(new User()));
        when(jwtService.generateToken(any())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        verify(jwtService).generateToken(any());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Логин с неправильным паролем должен выбросить исключение")
    void login_withWrongPassword_shouldThrowException() {

        AuthRequest request = new AuthRequest("testUser", "wrongPassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any());

        assertThrows(BadCredentialsException.class,
                () -> authService.authenticate(request)
        );

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Логин с несуществующим пользователем должен выбросить исключение")
    void login_withNonExistentUser_shouldThrowException() {

        AuthRequest request = new AuthRequest("nonExists", "password123");

        assertThrows(UserNotFoundException.class,
                () -> authService.authenticate(request));

        verify(jwtService, never()).generateToken(any());
    }
}