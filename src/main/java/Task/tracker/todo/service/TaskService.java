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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper mapper;
    private final UserRepository userRepository;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Пользователь не авторизован");
        }

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + username));
    }

    private User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse createTask(TaskCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        User user = getCurrentUser();

        Task task = mapper.toEntity(request);
        task.setUser(user);

        Task savedTask = taskRepository.save(task);
        return mapper.toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "'all:' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public List<TaskResponse> getAllTask(Pageable pageable) {
        User user = getCurrentUser();
        return taskRepository.findByUser(user, pageable)
                .map(mapper::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "'status:' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + ':' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public List<TaskResponse> getTasksByStatus(StatusType status, Pageable pageable) {
        User user = getCurrentUser();
        return taskRepository.findByUserAndStatus(user, status, pageable)
                .map(mapper::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "taskById", key = "#id")
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        User currentUser = getCurrentUser();
        if (!task.getUser().getId().equals(currentUser.getId())) {
            throw new TaskNotFoundException("Задача не найдена");
        }

        return mapper.toResponse(task);
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse updateTask(Long id, TaskUpdateRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        User currentUser = getCurrentUser();
        if (!task.getUser().getId().equals(currentUser.getId())) {
            throw new TaskNotFoundException("Задача не найдена");
        }

        mapper.updateEntity(task, request);
        return mapper.toResponse(task);
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public void deleteTask(Long id) {
       Task task = taskRepository.findById(id)
               .orElseThrow(() -> new TaskNotFoundException(id));

       User currentUser = getCurrentUser();
       if (!task.getUser().getId().equals(currentUser.getId())) {
           throw new TaskNotFoundException("Задача не найдена");
       }
       taskRepository.delete(task);
    }
}
