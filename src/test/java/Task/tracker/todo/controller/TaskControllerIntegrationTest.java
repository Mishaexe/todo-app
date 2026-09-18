package Task.tracker.todo.controller;

import Task.tracker.todo.dto.TaskCreateRequest;
import Task.tracker.todo.dto.TaskUpdateRequest;
import Task.tracker.todo.entity.StatusType;
import Task.tracker.todo.entity.Task;
import Task.tracker.todo.entity.User;
import Task.tracker.todo.repository.TaskRepository;
import Task.tracker.todo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createAndSaveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password123");
        return userRepository.save(user);
    }

    private Task createAndSaveTask(String taskName, String description, StatusType statusType, User user) {
        Task task = new Task();
        task.setTitle(taskName);
        task.setDescription(description);
        task.setStatus(statusType);
        task.setUser(user);
        return taskRepository.save(task);
    }

    @Test
    @DisplayName("Создание задачи должно вернуть 201 и сохраненную задачу")
    void createTask_shouldReturn201AndSavedTask() throws Exception {
        User savedUser = createAndSaveUser("testUser_create");

        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Изучить тестирование");
        request.setDescription("Написать интеграционные тесты");
        request.setStatus(StatusType.TODO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Изучить тестирование"))
                .andExpect(jsonPath("$.userId").value(savedUser.getId()));

        assertThat(taskRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Получение всех задач должно вернуть страницу со список")
    void getAllTasks_shouldReturnListOfTasks() throws Exception {

        User savedUser = createAndSaveUser("testuser_getall");

        Task task1 = createAndSaveTask("Задача 1", "Описание 1", StatusType.TODO, savedUser);

        Task task2 = createAndSaveTask("Задача 2", "Описание 2", StatusType.IN_PROGRESS, savedUser);

        mockMvc.perform(get("/api/tasks")
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("Получение задачи по ID должно вернуть задачу")
    void getTaskById_shouldReturnTask() throws Exception {
        User savedUser = createAndSaveUser("testuser_getid");

        Task savedTask = createAndSaveTask("Найти меня", "Я существую и принадлежу пользователю", StatusType.TODO, savedUser);

        mockMvc.perform(get("/api/tasks/{id}", savedTask.getId())
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTask.getId()))
                .andExpect(jsonPath("$.title").value("Найти меня"));
    }

    @Test
    @DisplayName("Получение несуществующей задачи должно вернуть 404")
    void getTaskById_whenTaskNotFound_shouldReturn404() throws Exception {
        User savedUser = createAndSaveUser("testuser_404");

        mockMvc.perform(get("/api/tasks/{id}", 9999L)
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Задача не найдена"));
    }

    @Test
    @DisplayName("Обновление задачи должно вернуть обновленную задачу")
    void updateTask_shouldReturnUpdatedTask() throws Exception {
        User savedUser = createAndSaveUser("testuser_update");

        Task savedTask = createAndSaveTask("Старое название", "Старое описание", StatusType.TODO, savedUser);

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Новое название");

        mockMvc.perform(put("/api/tasks/{id}", savedTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername())))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новое название"))
                .andExpect(jsonPath("$.description").value("Старое описание"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("Удаление задачи должно вернуть 204")
    void deleteTask_shouldReturn204() throws Exception {
        User savedUser = createAndSaveUser("testuser_delete");

        Task savedTask = createAndSaveTask("Удали меня", "Пожалуйста, описание длинное", StatusType.TODO, savedUser);

        mockMvc.perform(delete("/api/tasks/{id}", savedTask.getId())
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername())))
                )
                .andExpect(status().isNoContent());

        assertThat(taskRepository.findById(savedTask.getId())).isEmpty();
    }

    @Test
    @DisplayName("Поиск задач по статусу должен вернуть отфильтрованный список")
    void getTasksByStatus_shouldReturnFilteredTasks() throws Exception {
        User savedUser = createAndSaveUser("testuser_status");

        Task task1 = createAndSaveTask("TODO задача", "Описание задачи 1", StatusType.TODO, savedUser);

        Task task2 = createAndSaveTask("IN_PROGRESS задача", "Описание задачи 2", StatusType.IN_PROGRESS, savedUser);

        Task task3 = createAndSaveTask("Еще TODO", "Описание задачи 3", StatusType.TODO, savedUser);

        mockMvc.perform(get("/api/tasks/by-status")
                        .param("status", "TODO")
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername())))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Обновление несуществующей задачи 404")
    void updateTask_whenTaskNonExistentTask_shouldReturn404() throws Exception {
        User savedUser = createAndSaveUser("testuser_update404");
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Новое название");

        mockMvc.perform(put("/api/tasks/{id}", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername())))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Задача не найдена"));
    }

    @Test
    @DisplayName("Создание задачи с невалидным заголовком")
    void createTask_whenInvalidTitleName_shouldReturn400() throws Exception {
        User savedUser = createAndSaveUser("testuser_400");
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("A");
        request.setDescription("Валидное описание задачи");
        request.setStatus(StatusType.TODO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername())))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isNotEmpty());

    }

    @Test
    @DisplayName("Получение задач из пустой базы должно вернуть пустую страницу")
    void getTasks_ForEmptyDb() throws Exception {
        User savedUser = createAndSaveUser("testuser_empty");

        mockMvc.perform(get("/api/tasks")
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername())))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }


    @Test
    @DisplayName("Получение ответа с пагинацией")
    void getTasksAll_with_Pagination() throws Exception {
        User savedUser = createAndSaveUser("testuser_page");

        Task task1 = createAndSaveTask("Первая задача", "Описание первой задачи", StatusType.TODO, savedUser);

        Task task2 = createAndSaveTask("Вторая задача", "Описание второй задачи", StatusType.IN_PROGRESS, savedUser);

        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "createdAt,desc")
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername())))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("Создание задачи для пользователя должно вернуть задачу с userId")
    void createTask_forUser_shouldReturnTaskWithUserId() throws Exception {

        User savedUser = createAndSaveUser("testuser_page");

        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Задача пользователя");
        request.setDescription("Описание задачи пользователя");
        request.setStatus(StatusType.TODO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("sub", savedUser.getUsername())))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Задача пользователя"));
    }


}
