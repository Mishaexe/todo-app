package Task.tracker.todo.service;

import Task.tracker.todo.dto.TaskCreateRequest;
import Task.tracker.todo.dto.TaskResponse;
import Task.tracker.todo.dto.TaskUpdateRequest;
import Task.tracker.todo.entity.StatusType;
import Task.tracker.todo.entity.Task;
import Task.tracker.todo.entity.User;
import Task.tracker.todo.exception.TaskNotFoundException;
import Task.tracker.todo.exception.UserNotFoundException;
import Task.tracker.todo.mapper.TaskMapper;
import Task.tracker.todo.repository.TaskRepository;
import Task.tracker.todo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper mapper;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + username));
    }
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        User user = getCurrentUser();

        Task task = mapper.toEntity(request);
        task.setUser(user);

        Task savedTask = taskRepository.save(task);
        return mapper.toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTask(Pageable pageable) {
        User user = getCurrentUser();
        return taskRepository.findByUser(user, pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByStatus(StatusType status, Pageable pageable) {
        User user = getCurrentUser();
        return taskRepository.findByUserAndStatus(user, status, pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
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
