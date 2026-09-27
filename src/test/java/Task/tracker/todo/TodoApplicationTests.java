package Task.tracker.todo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.cache.type=none")
class TodoApplicationTests {

	@Test
	void contextLoads() {
	}

}
