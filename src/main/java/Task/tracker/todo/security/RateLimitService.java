package Task.tracker.todo.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RateLimitService {

    private final ProxyManager<String> proxyManager;

    public RateLimitService(RedissonClient redissonClient) {
        CommandAsyncExecutor commandExecutor = ((Redisson) redissonClient).getCommandExecutor();
        this.proxyManager = RedissonBasedProxyManager.builderFor(commandExecutor).build();
    }


    public Bucket resolveBucket(String username) {
        String key = "rate-limit:general:" + username;
        return proxyManager.builder().build(key, this::createGeneralBucketConfig);
    }

    public Bucket resolveCreateTaskBucket(String username) {
        String key = "rate-limit:create:" + username;
        return proxyManager.builder().build(key, this::createTaskBucketConfig);
    }

    private BucketConfiguration createGeneralBucketConfig() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillGreedy(100, Duration.ofHours(1))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    private BucketConfiguration createTaskBucketConfig() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}