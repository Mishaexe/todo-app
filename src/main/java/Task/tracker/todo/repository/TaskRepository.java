package Task.tracker.todo.repository;

import Task.tracker.todo.entity.StatusType;
import Task.tracker.todo.entity.Task;
import Task.tracker.todo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(StatusType statusType);

    Page<Task> findByStatus(StatusType statusType, Pageable pageable);

    List<Task> findByUser(User user);
    Page<Task> findByUser(User user, Pageable pageable);

    Page<Task> findByUserAndStatus(User user, StatusType status, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.user WHERE t.user.id = :userId")
    List<Task> findByUserIdWithUser(@Param("userId") Long userId);
}
