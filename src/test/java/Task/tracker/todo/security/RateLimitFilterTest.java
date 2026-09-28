package Task.tracker.todo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter")
public class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Bucket generalBucket;

    @Mock
    private Bucket createTaskBucket;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);

        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("Неаутентифицированные / anonymous пользователи")
    class UnauthenticatedUsers {

        @Test
        @DisplayName("должен пропускать запрос без проверки лимитов, если authentication == null")
        void shouldPassThroughWhenAuthenticationIsNull() throws ServletException, IOException {
            when(securityContext.getAuthentication()).thenReturn(null);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(rateLimitService);
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("должен пропускать запрос без проверки лимитов для anonymousUser")
        void shouldPassThroughForAnonymousUser() throws ServletException, IOException {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(rateLimitService);
        }

        @Test
        @DisplayName("должен пропускать запрос, если пользователь не аутентифицирован")
        void shouldPassThroughWhenNotAuthenticated() throws ServletException, IOException {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(rateLimitService);
        }
    }

    @Nested
    @DisplayName("Успешные запросы (в пределах лимита)")
    class SuccessfulRequests {

        @BeforeEach
        void setUpAuthenticatedUser() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("user");
            when(authentication.getName()).thenReturn("testuser");
            when(request.getRequestURI()).thenReturn("/api/tasks");
            when(request.getMethod()).thenReturn("GET");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        }

        @Test
        @DisplayName("должен пропустить запрос и добавить rate-limit заголовки")
        void shouldAllRequestAndHeaders() throws ServletException, IOException {
            when(rateLimitService.resolveBucket("testuser")).thenReturn(generalBucket);
            when(generalBucket.tryConsume(1)).thenReturn(true);
            when(generalBucket.getAvailableTokens()).thenReturn(99L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-RateLimit-Limit", "100");
            verify(response).setHeader("X-RateLimit-Remaining", "99");
            verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        @Test
        @DisplayName("должен проверить create-task лимит для POST /api/tasks")
        void shouldCheckCreateTaskLimitForPostApiTasks() throws ServletException, IOException {
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/tasks");

            when(rateLimitService.resolveBucket("testuser")).thenReturn(generalBucket);
            when(generalBucket.tryConsume(1)).thenReturn(true);
            when(generalBucket.getAvailableTokens()).thenReturn(99L);

            when(rateLimitService.resolveCreateTaskBucket("testuser")).thenReturn(createTaskBucket);
            when(createTaskBucket.tryConsume(1)).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitService).resolveCreateTaskBucket("testuser");
            verify(createTaskBucket).tryConsume(1);
            verify(filterChain).doFilter(request, response);
        }
        @Test
        @DisplayName("не должен проверять create-task лимит для других методов/URI")
        void shouldNotCheckCreateTaskLimitForOtherRequests() throws ServletException, IOException {
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/tasks");

            when(rateLimitService.resolveBucket("testuser")).thenReturn(generalBucket);
            when(generalBucket.tryConsume(1)).thenReturn(true);
            when(generalBucket.getAvailableTokens()).thenReturn(99L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitService, never()).resolveCreateTaskBucket(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Превышение лимитов")
    class RateLimitExceeded {

        @BeforeEach
        void setUpAuthenticatedUser() throws IOException {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("user");
            when(authentication.getName()).thenReturn("testuser");
            when(request.getRequestURI()).thenReturn("/api/tasks");
            when(request.getMethod()).thenReturn("GET");
            when(request.getRemoteAddr()).thenReturn("192.168.1.10");
            when(response.getWriter()).thenReturn(printWriter);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":429}");
        }

        @Test
        @DisplayName("должен вернуть 429 при превышении общего лимита")
        void shouldReturn429WhenGeneralLimitExceeded() throws ServletException, IOException {
            when(rateLimitService.resolveBucket("testuser")).thenReturn(generalBucket);
            when(generalBucket.tryConsume(1)).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(response).setHeader("X-RateLimit-Limit", "100");
            verify(response).setHeader("X-RateLimit-Remaining", "0");
            verify(response).setHeader("Retry-After", "3600");
            verify(filterChain, never()).doFilter(any(), any());

            ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
            verify(objectMapper).writeValueAsString(bodyCaptor.capture());

            Map<String, Object> body = bodyCaptor.getValue();
            assertThat(body.get("status")).isEqualTo(429);
            assertThat(body.get("message")).isEqualTo("Превышен общий лимит запросов (100 в час)");
            assertThat(body.get("retryAfter")).isEqualTo(3600);
        }

        @Test
        @DisplayName("должен вернуть 429 при превышении лимита создания задач")
        void shouldReturn429WhenCreateTaskLimitExceeded() throws ServletException, IOException {
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/tasks");

            when(rateLimitService.resolveBucket("testuser")).thenReturn(generalBucket);
            when(generalBucket.tryConsume(1)).thenReturn(true);
            when(generalBucket.getAvailableTokens()).thenReturn(50L);

            when(rateLimitService.resolveCreateTaskBucket("testuser")).thenReturn(createTaskBucket);
            when(createTaskBucket.tryConsume(1)).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            verify(response).setHeader("X-RateLimit-Limit", "10");
            verify(response).setHeader("X-RateLimit-Remaining", "0");
            verify(response).setHeader("Retry-After", "3600");
            verify(filterChain, never()).doFilter(any(), any());

            ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
            verify(objectMapper).writeValueAsString(bodyCaptor.capture());

            Map<String, Object> body = bodyCaptor.getValue();
            assertThat(body.get("message")).isEqualTo("Превышен лимит создания задач (10 в минуту)");
        }
    }

    @Nested
    @DisplayName("Определение IP-адреса клиента")
    class ClientIpResolution {
        @BeforeEach
        void setUpAuthenticatedUser() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("user");
            when(authentication.getName()).thenReturn("testuser");
            when(request.getRequestURI()).thenReturn("/api/other");
            when(request.getMethod()).thenReturn("GET");

            when(rateLimitService.resolveBucket("testuser")).thenReturn(generalBucket);
            when(generalBucket.tryConsume(1)).thenReturn(true);
            when(generalBucket.getAvailableTokens()).thenReturn(99L);
        }

        @Test
        @DisplayName("должен брать IP из X-Forwarded-For")
        void shouldUseXForwardedFor() throws ServletException, IOException {
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("должен брать IP из X-Real-IP, если X-Forwarded-For отсутствует")
        void shouldUseXpRealIp() throws ServletException, IOException {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.5");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Должен брать RemoteAddr, если заголовки отсутствуют")
        void shouldFallBackToRemoteAddr() throws ServletException, IOException {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
