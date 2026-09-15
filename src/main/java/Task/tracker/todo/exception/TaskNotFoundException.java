package Task.tracker.todo.exception;

public class TaskNotFoundException extends RuntimeException{
    public TaskNotFoundException(long id) {
        super("Задача с id %s не найдена".formatted(id));
    }

    public TaskNotFoundException(String user) {
        super("Задача с user %s не найдена".formatted(user));
    }

}
