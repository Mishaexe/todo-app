package Task.tracker.todo.dto;

import Task.tracker.todo.entity.StatusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskCreateRequest {

    @NotNull(message = "Название должно быть заполнено")
    @Size(min = 2, max = 50, message = "Название должно содержать от 2 до 50 символов")
    private String title;

    @NotBlank(message = "Описание должно присутствовать")
    @Size(min = 10, message = "Описание должно быть не менее 10 символов")
    private String description;

    @NotNull(message = "Статус должен быть указан")
    private StatusType status;

}
