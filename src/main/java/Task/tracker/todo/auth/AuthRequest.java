package Task.tracker.todo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
    @NotBlank(message = "Username не может быть пустым")
    @Size(min = 3, max = 50, message = "Username должен содержать от 3 до 50 символов")
    private String username;

    @NotBlank(message = "Password не может быть пустым")
    @Size(min = 6, message = "Password должен содержать минимум 6 символов")
    private String password;
}
