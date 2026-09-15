package Task.tracker.todo.dto;

import Task.tracker.todo.entity.StatusType;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskUpdateRequest {
    @Size(min = 2, max = 50, message = "Название должно содержать от 2 до 50 символов")
    private String title;

    @Size(min = 10, message = "Описание должно быть не менее 10 символов")
    private String description;

    private StatusType status;
}
