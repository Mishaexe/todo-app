package Task.tracker.todo.security;

import Task.tracker.todo.entity.User;
import Task.tracker.todo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername с существующим пользователем должен вернуть UserDetails")
    void loadUserByUsername_withExistingUser_shouldReturnUserDetails() {

        User user = new User();
        user.setUsername("testUser");
        user.setPassword("password123");
        user.setEmail("testEmail@test.com");

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testUser");

        assertNotNull(userDetails);
        assertEquals("testUser", userDetails.getUsername());
        assertEquals("password123", userDetails.getPassword());
        verify(userRepository).findByUsername("testUser");
    }

    @Test
    @DisplayName("loadUserByUsername с несуществующим пользователеме должен выбросить исключение")
    void loadUserByUsername_withNonExistentUser_shouldThrowException() {

        when(userRepository.findByUsername("nonexistUser")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nonexistUser"));

        verify(userRepository).findByUsername("nonexistUser");
    }
}
