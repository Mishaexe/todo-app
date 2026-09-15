package Task.tracker.todo.exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(Long id) {
        super("Пользователь с id " + id + " не найден");
    }

    public UserNotFoundException(String username) {
        super("Пользователь с username " +  username + " не найден");
    }
}
