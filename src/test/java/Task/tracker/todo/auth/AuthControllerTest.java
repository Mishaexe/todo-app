package Task.tracker.todo.auth;

import Task.tracker.todo.entity.User;
import Task.tracker.todo.exception.UserNotFoundException;
import Task.tracker.todo.security.CustomUserDetailsService;
import Task.tracker.todo.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/auth/register с валидными данными должен вернуть 200 и вернуть токен")
    void register_withValidData_shouldReturn200AndToken() throws Exception {

        AuthRequest request = new AuthRequest("testUser", "password123");
        AuthResponse response = new AuthResponse("fake-jwt-token");

        when(authService.register(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));

        verify(authService).register(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register с пустым username должен вернуть 400")
    void register_withEmptyUsername_shouldReturn400() throws Exception {

        AuthRequest request = new AuthRequest(null, "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/auth/register с пустым password должен вернуть 400")
    void register_withEmptyPassword_shouldReturn400() throws Exception {

        AuthRequest request = new AuthRequest("testUser", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/auth/login с валидными данными должен вернуть 200 и токен")
    void login_withValidCredentials_shouldReturn200AndToken() throws Exception {

        AuthRequest request = new AuthRequest("testUser", "password123");
        AuthResponse response = new AuthResponse("fake-jwt-token");

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));

        verify(authService).authenticate(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login с неправильным паролем должен вернуть 401")
    void login_withWrongPassword_shouldReturn401() throws Exception {

        AuthRequest request = new AuthRequest("testUser", "wrongPassword");

        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Неверный пароль"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authService).authenticate(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login с несуществующим пользователем должен вернуть 404")
    void login_withNonExistentUser_shouldReturn404() throws Exception {

        AuthRequest request = new AuthRequest("nonexistent", "password123");

        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new UserNotFoundException("Пользователь не найден"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Пользователь не найден"));

        verify(authService).authenticate(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register с невалидным JSON должен вернуть 400")
    void register_withInvalidJson_shouldReturn400() throws Exception {

        String invalidJson = "{invalid json}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }
}

