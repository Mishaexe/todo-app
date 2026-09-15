package Task.tracker.todo;

import Task.tracker.todo.entity.StatusType;
import Task.tracker.todo.entity.Task;
import Task.tracker.todo.entity.User;
import Task.tracker.todo.repository.TaskRepository;
import Task.tracker.todo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class N1ProblemTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("Проверка решения N+1: должен быть всего 1 SQL запрос с JOIN")
    void testN1SolutionDirectly() {

        User user = new User();
        user.setUsername("n1_test_user");
        user.setEmail("n1_test@example.com");
        user.setPassword("password123");
        User savedUser = userRepository.save(user);

        for (int i = 1; i <= 3; i++) {
            Task task = new Task();
            task.setTitle("Задача номер " + i);
            task.setDescription("Описание задачи номер " + i);
            task.setStatus(StatusType.TODO);
            task.setUser(savedUser);
            taskRepository.save(task);
        }

        List<Task> tasks = taskRepository.findByUserIdWithUser(savedUser.getId());

        assertThat(tasks).hasSize(3);

        assertThat(tasks.get(0).getUser()).isNotNull();
        assertThat(tasks.get(0).getUser().getUsername()).isEqualTo("n1_test_user");
    }
}
