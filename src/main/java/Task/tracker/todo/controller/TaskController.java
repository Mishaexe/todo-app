package Task.tracker.todo.controller;

import Task.tracker.todo.dto.TaskCreateRequest;
import Task.tracker.todo.dto.TaskResponse;
import Task.tracker.todo.dto.TaskUpdateRequest;
import Task.tracker.todo.entity.StatusType;
import Task.tracker.todo.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "API для управления задачами пользователя")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Создать новую задачу", description = "Создает задачу и привязывает её к текущему авторизованному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Задача успешно создана"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody @Valid TaskCreateRequest request) {
        TaskResponse response = taskService.createTask(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Получить все задачи", description = "Возвращает paginated список задач текущего пользователя")
    @GetMapping
    public Page<TaskResponse> getAllTasks(
            @ParameterObject
            @Parameter(description = "Параметры пагинации: page, size, sort")
            @PageableDefault(size = 10)Pageable pageable) {
        return taskService.getAllTask(pageable);
    }

    @Operation(summary = "Получить задачи текущего пользователя по статусу", description = "Фильтрует задачи текущего пользователя по статусу")
    @GetMapping("/by-status")
    public Page<TaskResponse> getTasksByStatus(
            @Parameter(description = "типы статуса: TODO, IN_PROGRESS, DONE")
            @RequestParam StatusType status,
            @ParameterObject
            @PageableDefault(size = 10) Pageable pageable) {
        return taskService.getTasksByStatus(status, pageable);
    }

    @Operation(summary = "Получить задачу по id", description = "получает задачу по ID (только если она принадлежит текущему пользователю)")
    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @Operation(summary = "Обновить задачу по id", description = "Обновляет задачу по ID (только если она принадлежит текущему пользователю")
    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @RequestBody @Valid TaskUpdateRequest request) {
        return taskService.updateTask(id, request);
    }

    @Operation(summary = "Удалить задачу по id", description = "Удаляет задачу по ID (только если она принадлежит текущему пользователю)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

}
