package Task.tracker.todo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }
        String username = authentication.getName();
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();
        String clientIp = getClientIp(request);

        log.debug("Request from user= '{}', method='{}', uri= '{}', ip='{}'", username, requestMethod, requestUri, clientIp);

        Bucket generalBucket = rateLimitService.resolveBucket(username);
        if (!generalBucket.tryConsume(1)) {
            log.warn("GENERAL RATE LIMIT EXCEEDED | user='{}' | method= '{}' | uri= '{}' | ip= '{}'"
            , username, requestMethod, requestUri, clientIp);
            sendRateLimitResponse(response, "Превышен общий лимит запросов (100 в час)", 100, 0);
            return;
        }

        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().equals("/api/tasks")) {
            Bucket createBucket = rateLimitService.resolveCreateTaskBucket(username);
            if (!createBucket.tryConsume(1)) {
                log.warn("CREATE TASK RATE LIMIT EXCEEDED | user='{}' | ip='{}' | remaining_general={}",
                        username, clientIp, generalBucket.getAvailableTokens());
                sendRateLimitResponse(response, "Превышен лимит создания задач (10 в минуту)", 10, 0);
                return;
            }
        }

        addRateLimitHeaders(response, generalBucket, 100);

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message, int limit, int remaining) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("Retry-After", "3600"); // Через час сбросится

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 429);
        errorResponse.put("message", message);
        errorResponse.put("retryAfter", 3600);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private void addRateLimitHeaders(HttpServletResponse response, Bucket bucket, int limit) {
        long remainingTokens = bucket.getAvailableTokens();
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remainingTokens));
    }
}