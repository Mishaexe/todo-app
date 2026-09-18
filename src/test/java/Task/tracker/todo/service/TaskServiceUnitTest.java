package Task.tracker.todo.service;

import Task.tracker.todo.dto.TaskCreateRequest;
import Task.tracker.todo.dto.TaskResponse;
import Task.tracker.todo.dto.TaskUpdateRequest;
import Task.tracker.todo.entity.StatusType;
import Task.tracker.todo.entity.Task;
import Task.tracker.todo.entity.User;
import Task.tracker.todo.exception.TaskNotFoundException;
import Task.tracker.todo.exception.UnauthorizedException;
import Task.tracker.todo.exception.UserNotFoundException;
import Task.tracker.todo.mapper.TaskMapper;
import Task.tracker.todo.repository.TaskRepository;
import Task.tracker.todo.repository.UserRepository;
import jakarta.xml.bind.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceUnitTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskMapper mapper;

    @InjectMocks
    private TaskService taskService;

    private User mockUser;
    private Task mockTask;
    private TaskResponse mockResponse;

    @BeforeEach
    void setUp() {

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");

        mockTask = new Task();
        mockTask.setId(100L);
        mockTask.setTitle("Купить молоко");
        mockTask.setDescription("Срочно");
        mockTask.setStatus(StatusType.TODO);
        mockTask.setUser(mockUser);

        mockResponse = new TaskResponse(100L, "Купить молоко", "Срочно", StatusType.TODO, 1L, null, null);

    }

    private User createUser(long id, String name) {
        User user = new User();
        user.setId(id);
        user.setUsername(name);
        mockTask.setUser(user);
        return user;
    }

    private void withMockUser(String username, Runnable test) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(username);


        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurity = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            test.run();
        }
    }

    private TaskCreateRequest createRequest(String setTitle, String setDescription, StatusType statusType) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle(setTitle);
        request.setDescription(setDescription);
        request.setStatus(statusType);
        return request;
    }

    /// ------------------------------------------GET-TEST-------------------------------------------

    @Test
    @DisplayName("getTaskById должен вернуть задачу, если она существует и принадлежит пользователю")
    void getTaskById_Success() {
        withMockUser("testUser", () -> {

            when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(mockUser));
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
            when(mapper.toResponse(mockTask)).thenReturn(mockResponse);

            TaskResponse result = taskService.getTaskById(100L);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("Купить молоко", result.getTitle());

            verify(userRepository, times(1)).findByUsername("testUser");
            verify(taskRepository, times(1)).findById(100L);
            verify(mapper, times(1)).toResponse(mockTask);
        });
    }

    @Test
    @DisplayName("getTaskById должен выбросить исключение, если задача не найдена")
    void getTaskById_TaskNotFound_ThrowsException() {

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> taskService.getTaskById(999L)
        );

        assertEquals("Задача с id 999 не найдена", exception.getMessage());

        verify(taskRepository, times(1)).findById(999L);
        verify(mapper, never()).toResponse(any());

    }

    /// ------------------------------CREATE-TEST----------------------------------------

    @Test
    @DisplayName("createTask Должен создать задачу и вернуть TaskResponse (201) ")
    void createTask_Success() {

        withMockUser("testUser", () -> {
            when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(mockUser));
            when(mapper.toEntity(any(TaskCreateRequest.class))).thenReturn(mockTask);
            when(taskRepository.save(mockTask)).thenReturn(mockTask);
            when(mapper.toResponse(mockTask)).thenReturn(mockResponse);

            TaskCreateRequest request = createRequest("Купить молоко", "Срочно", StatusType.TODO);

            TaskResponse result = taskService.createTask(request);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("Купить молоко", result.getTitle());

            verify(userRepository, times(1)).findByUsername("testUser");
            verify(mapper, times(1)).toEntity(request);
            verify(taskRepository, times(1)).save(mockTask);
            verify(mapper, times(1)).toResponse(mockTask);
        });
    }

    @Test
    @DisplayName("Create Task с пустым title должен выбросить исключение валидации (400) ")
    void createTask_whenTitleIsEmpty_shouldThrowValidationException() {

        TaskCreateRequest request = createRequest("", "Срочно", StatusType.TODO);

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTask — пользователь не найден в БД (400) ")
    void createTask_whenUserNotFound_shouldThrowException() {
        withMockUser("Erw", () -> {

            when(userRepository.findByUsername("Erw")).thenReturn(Optional.empty());

            TaskCreateRequest request = createRequest("Milk", "Срочно", StatusType.TODO);

            assertThrows(UserNotFoundException.class,
                    () -> taskService.createTask(request)
                    );

            verify(taskRepository, never()).save(any());
        });
    }

    @Test
    @DisplayName("createTask - пользователь не авторизован (401) ")
    void createTask_whenNotAuthenticated_shouldThrowException() {

        SecurityContextHolder.clearContext();

        TaskCreateRequest request = createRequest("Milk", "Срочно", StatusType.TODO);

        assertThrows(UnauthorizedException.class,
                () -> taskService.createTask(request));

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTask - status = null Должен выбрасывать исключение (400)")
    void createTask_whenStatusIsNull_shouldThrowException() {

            TaskCreateRequest request = createRequest("Купить молоко", "Срочно", null);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> taskService.createTask(request)
            );

            assertEquals("Status cannot be null", exception.getMessage());

            verify(taskRepository, never()).save(any());
            verify(mapper, never()).toEntity(any());
            verify(userRepository, never()).findByUsername(any());

    }

    /// ----------------------------UPDATE_TEST-----------------------------------------------

    @Test
    @DisplayName("updateTask должен обновить задачу, если она существует и принадлежит пользователю")
    void updateTask_Success() {

        withMockUser("testUser", () -> {
            TaskUpdateRequest updateRequest = new TaskUpdateRequest();
            updateRequest.setTitle("Новое название");

            when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(mockUser));
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

            doNothing().when(mapper).updateEntity(any(Task.class), any(TaskUpdateRequest.class));

            TaskResponse expectedUpdatedResponse = new TaskResponse(
                    100L,
                    "Новое название",
                    "Срочно",
                    StatusType.TODO,
                    1L,
                    null,
                    null
            );

            when(mapper.toResponse(mockTask)).thenReturn(expectedUpdatedResponse);

            TaskResponse result = taskService.updateTask(100L, updateRequest);

            assertNotNull(result);
            assertEquals("Новое название", result.getTitle());
            assertEquals(100L, result.getId());

            verify(userRepository, times(1)).findByUsername("testUser");
            verify(taskRepository, times(1)).findById(100L);
            verify(mapper, times(1)).updateEntity(eq(mockTask), eq(updateRequest));
            verify(mapper, times(1)).toResponse(mockTask);

            verify(taskRepository, never()).save(any());
        });
    }

    @Test
    @DisplayName("updateTask должен выбросить исключение, если задача не найдена")
    void updateTask_TaskNotFound_ThrowsException() {

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        TaskUpdateRequest updateRequest = new TaskUpdateRequest();
        updateRequest.setTitle("Новое название");

        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> taskService.updateTask(999L, updateRequest)
        );

        assertEquals("Задача с id 999 не найдена", exception.getMessage());

        verify(taskRepository, times(1)).findById(999L);

        verify(mapper, never()).updateEntity(any(), any());
    }


    @Test
    @DisplayName("updateTask должен выбросить исключение, если задача не принадлежит текущему пользователю")
    void updateTask_WrongUser_ThrowsException() {

        withMockUser("testUser", () -> {
            User otherUser = createUser(999L, "otherUser");
            when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(mockUser));
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

            TaskUpdateRequest updateRequest = new TaskUpdateRequest();
            updateRequest.setTitle("Попытка взлома");

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.updateTask(100L, updateRequest)
            );

            verify(mapper, never()).updateEntity(any(), any());
        });
    }

    /// -----------------------------DELETE_TEST-------------------------------------

    @Test
    @DisplayName("deleteTask должен удалить задачу, если она существует и принадлежит пользователю")
    void deleteTask_Success() {

        withMockUser("testUser", () -> {
            when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(mockUser));
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

            doNothing().when(taskRepository).delete(mockTask);

            taskService.deleteTask(100L);

            verify(userRepository, times(1)).findByUsername("testUser");
            verify(taskRepository, times(1)).findById(100L);
            verify(taskRepository, times(1)).delete(mockTask);
        });
    }

    @Test
    @DisplayName("deleteTask Должен выбросить исключение, если задача не найдена")
    void deleteTask_TaskNotFound_throwsException() {

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> taskService.deleteTask(999L)
        );

        assertEquals("Задача с id 999 не найдена", exception.getMessage());

        verify(taskRepository, times(1)).findById(999L);

        verify(taskRepository, never()).delete(any());

    }

    @Test
    @DisplayName("deleteTask должен выбросить исключение, если задача не пренадлежит текущему пользователю")
    void deleteTask_WrongUser_ThrowsException() {

        withMockUser("testUser", () -> {

            User otherUser = createUser(999L, "otherUser");

            when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(mockUser));
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

            assertThrows(TaskNotFoundException.class,
                    () -> taskService.deleteTask(100L)
            );

            verify(taskRepository, never()).delete(any());
        });
    }
}
