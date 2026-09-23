package Task.tracker.todo.dto;

import Task.tracker.todo.entity.StatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private StatusType status;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
