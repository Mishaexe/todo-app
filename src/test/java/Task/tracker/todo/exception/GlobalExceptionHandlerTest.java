package Task.tracker.todo.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("Обработка UserNotFoundException должна вернуть 404")
    void handleUserNotFoundException_shouldReturn404() {
        UserNotFoundException exception = new UserNotFoundException("Пользователь не найден");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleUserNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Пользователь не найден", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Обработка TaskNotFoundException должна вернуть 404")
    void handleTaskNotFoundException_shouldReturn404() {
        TaskNotFoundException exception = new TaskNotFoundException("Задача не найдена");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handlerTaskNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Задача не найдена", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Обработка MethodArgumentNotValidException должна вернуть 400")
    void handleMethodArgumentNotValidException_shouldReturn400() {

        BindingResult bindingResult = mock(BindingResult.class);

        FieldError titleError = new FieldError(
                "TaskCreateRequest",
                "title",
                "Title не может быть пустым"
        );

        FieldError descriptionError = new FieldError(
                "TaskCreateRequest",
                "description",
                "Description должен быть не менее 10 символов"
        );

        when(bindingResult.getFieldErrors())
                .thenReturn(Arrays.asList(titleError, descriptionError));

        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Ошибка валидации", body.getMessage());
        assertNotNull(body.getTimestamp());

        List<String> errors = body.getErrors();
        assertNotNull(body.getErrors());
        assertEquals(2, errors.size());
        assertTrue(errors.contains("title: Title не может быть пустым"));
        assertTrue(errors.contains("description: Description должен быть не менее 10 символов"));
    }

    @Test
    @DisplayName("Обработка общего Exception должна вернуть 500")
    void handleAllExceptions_shouldReturn500() {
        Exception exception = new RuntimeException("Что-то пошло не так");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleAllExceptions(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("Внутренняя ошибка сервера", body.getMessage());
        assertEquals(1, body.getErrors().size());
        assertEquals("Что-то пошло не так", body.getErrors().get(0));
        assertNotNull(body.getTimestamp());
    }
}
