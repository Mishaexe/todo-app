package Task.tracker.todo.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService")
public class RateLimitServiceTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private RemoteBucketBuilder<String> remoteBucketBuilder;

    @Mock
    private BucketProxy bucket;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitService = (RateLimitService) org.objenesis.ObjenesisHelper.newInstance(RateLimitService.class);

        Field proxyManagerField = RateLimitService.class.getDeclaredField("proxyManager");
        proxyManagerField.setAccessible(true);
        proxyManagerField.set(rateLimitService, proxyManager);
    }

    @Test
    @DisplayName("resolveBucket должен строить бакет с правильным ключом и конфигурацией")
    void resolveBucket_shouldBuildBucketWithCorrectKeyAndConfig() {
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(eq("rate-limit:general:john"), any(Supplier.class)))
                .thenReturn(bucket);

        Bucket result = rateLimitService.resolveBucket("john");

        assertThat(result).isSameAs(bucket);

        ArgumentCaptor<Supplier<BucketConfiguration>> configCaptor = ArgumentCaptor.forClass(Supplier.class);

        verify(remoteBucketBuilder).build(eq("rate-limit:general:john"), configCaptor.capture());

        BucketConfiguration configuration = configCaptor.getValue().get();
        assertThat(configuration.getBandwidths()).hasSize(1);
        assertThat(configuration.getBandwidths()[0].getCapacity()).isEqualTo(100);
    }

    @Test
    @DisplayName("resolveCreateTaskBucket должен строить бакет с правильным ключом и конфигурацией")
    void resolveCreateTaskBucket_shouldBuildBucketWithCorrectKeyAndConfig() {
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(eq("rate-limit:create:john"), any(Supplier.class)))
                .thenReturn(bucket);

        Bucket result = rateLimitService.resolveCreateTaskBucket("john");

        assertThat(result).isSameAs(bucket);

        ArgumentCaptor<Supplier<BucketConfiguration>> configCaptor = ArgumentCaptor.forClass(Supplier.class);

        verify(remoteBucketBuilder).build(eq("rate-limit:create:john"), configCaptor.capture());

        BucketConfiguration configuration = configCaptor.getValue().get();
        assertThat(configuration.getBandwidths()).hasSize(1);
        assertThat(configuration.getBandwidths()[0].getCapacity()).isEqualTo(10);
    }

}
