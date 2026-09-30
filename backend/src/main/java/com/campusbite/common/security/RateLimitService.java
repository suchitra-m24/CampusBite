package com.campusbite.common.security;

import com.campusbite.common.exception.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Fixed-window limiter for the single-instance CampusBite deployment.
 * It deliberately lives at the application edge; a multi-instance deployment
 * should swap this implementation for a shared Redis-backed limiter. */
@Service
public class RateLimitService {
    private record Window(Instant startedAt, AtomicInteger count) {}
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public void check(String key, int maxRequests) {
        Instant now = Instant.now();
        Window updated = windows.compute(key, (k, current) -> {
            if (current == null || now.isAfter(current.startedAt().plus(WINDOW))) {
                return new Window(now, new AtomicInteger(1));
            }
            int count = current.count().incrementAndGet();
            if (count > maxRequests) return current;
            return current;
        });
        if (updated.count().get() > maxRequests) {
            throw new TooManyRequestsException("Too many requests. Please wait a minute and try again.");
        }
    }

    @Scheduled(fixedRate = 300000)
    public void cleanup() {
        Instant cutoff = Instant.now().minus(WINDOW);
        windows.entrySet().removeIf(e -> e.getValue().startedAt().isBefore(cutoff));
    }
}
