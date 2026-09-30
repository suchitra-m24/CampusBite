package com.campusbite.common.security;

import com.campusbite.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    @Test
    void blocksRequestsBeyondWindowLimit() {
        RateLimitService limiter = new RateLimitService();
        limiter.check("login:test", 2);
        limiter.check("login:test", 2);
        assertThatThrownBy(() -> limiter.check("login:test", 2))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void limitsAreIndependentPerKey() {
        RateLimitService limiter = new RateLimitService();
        limiter.check("login:a", 1);
        limiter.check("login:b", 1);
        assertThatThrownBy(() -> limiter.check("login:a", 1))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
