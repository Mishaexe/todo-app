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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        Authentication mockAuthentication = mock(Authentication.class);
        lenient().when(mockAuthentication.getName()).thenReturn("testUser");
        lenient().when(mockAuthentication.isAuthenticated()).thenReturn(true);

        SecurityContext mockSecurityContext = mock(SecurityContext.class);
        lenient().when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);

        SecurityContextHolder.setContext(mockSecurityContext);

        lenient().when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(mockUser));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
    }

    @BeforeEach
    void resetMocks() {
        reset(taskRepository, mapper);
    }

    // ------------------------------------------ GET TESTS -------------------------------------------

    @Test
    @DisplayName("getTaskById должен вернуть задачу, если она существует и принадлежит пользователю")
    void getTaskById_Success() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
        when(mapper.toResponse(mockTask)).thenReturn(mockResponse);

        TaskResponse result = taskService.getTaskById(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());

        verify(taskRepository).findById(100L);
        verify(mapper).toResponse(mockTask);
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
        verify(taskRepository).findById(999L);
    }

    @Test
    @DisplayName("getTaskByStatus должен вернуть список задач, если они существуют")
    void getTaskByStatus_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(mockTask), pageable, 1);

        when(taskRepository.findByUserAndStatus(eq(mockUser), eq(StatusType.TODO), eq(pageable)))
                .thenReturn(taskPage);
        when(mapper.toResponse(mockTask)).thenReturn(mockResponse);

        List<TaskResponse> result = taskService.getTasksByStatus(StatusType.TODO, pageable);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(taskRepository).findByUserAndStatus(mockUser, StatusType.TODO, pageable);
    }

    // ------------------------------------------ CREATE TESTS ----------------------------------------

    @Test
    @DisplayName("createTask должен создать задачу и вернуть TaskResponse")
    void createTask_Success() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Купить молоко");
        request.setDescription("Срочно");
        request.setStatus(StatusType.TODO);

        when(mapper.toEntity(request)).thenReturn(mockTask);
        when(taskRepository.save(mockTask)).thenReturn(mockTask);
        when(mapper.toResponse(mockTask)).thenReturn(mockResponse);

        TaskResponse result = taskService.createTask(request);

        assertNotNull(result);
        assertEquals("Купить молоко", result.getTitle());

        verify(mapper).toEntity(request);
        verify(taskRepository).save(mockTask);
    }

    @Test
    @DisplayName("createTask с пустым title должен выбросить исключение")
    void createTask_whenTitleIsEmpty_shouldThrowException() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("");
        request.setStatus(StatusType.TODO);

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTask — пользователь не найден в БД")
    void createTask_whenUserNotFound_shouldThrowException() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());

        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Milk");
        request.setStatus(StatusType.TODO);

        assertThrows(UserNotFoundException.class, () -> taskService.createTask(request));
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTask - status = null должен выбрасывать исключение")
    void createTask_whenStatusIsNull_shouldThrowException() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Купить молоко");
        request.setStatus(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.createTask(request)
        );

        assertEquals("Status cannot be null", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    // ------------------------------------------ UPDATE TESTS ----------------------------------------

    @Test
    @DisplayName("updateTask должен обновить задачу")
    void updateTask_Success() {
        TaskUpdateRequest updateRequest = new TaskUpdateRequest();
        updateRequest.setTitle("Новое название");

        when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
        doNothing().when(mapper).updateEntity(any(Task.class), any(TaskUpdateRequest.class));
        when(mapper.toResponse(mockTask)).thenReturn(mockResponse);

        TaskResponse result = taskService.updateTask(100L, updateRequest);

        assertSame(mockResponse, result);

        verify(taskRepository).findById(100L);
        verify(mapper).updateEntity(eq(mockTask), eq(updateRequest));
        verify(mapper).toResponse(mockTask);
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
        verify(taskRepository).findById(999L);
    }

    @Test
    @DisplayName("updateTask должен выбросить исключение, если задача не принадлежит пользователю")
    void updateTask_WrongUser_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setUsername("otherUser");
        mockTask.setUser(otherUser);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

        TaskUpdateRequest updateRequest = new TaskUpdateRequest();
        updateRequest.setTitle("Попытка взлома");

        assertThrows(TaskNotFoundException.class, () -> taskService.updateTask(100L, updateRequest));
        verify(mapper, never()).updateEntity(any(), any());
    }

    // ------------------------------------------ DELETE TESTS ----------------------------------------

    @Test
    @DisplayName("deleteTask должен удалить задачу")
    void deleteTask_Success() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));
        doNothing().when(taskRepository).delete(mockTask);

        taskService.deleteTask(100L);

        verify(taskRepository).findById(100L);
        verify(taskRepository).delete(mockTask);
    }

    @Test
    @DisplayName("deleteTask должен выбросить исключение, если задача не найдена")
    void deleteTask_TaskNotFound_throwsException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> taskService.deleteTask(999L)
        );

        assertEquals("Задача с id 999 не найдена", exception.getMessage());
        verify(taskRepository).findById(999L);
    }

    @Test
    @DisplayName("deleteTask должен выбросить исключение, если задача не принадлежит пользователю")
    void deleteTask_WrongUser_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setUsername("otherUser");
        mockTask.setUser(otherUser);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(100L));
        verify(taskRepository, never()).delete(any());
    }
}