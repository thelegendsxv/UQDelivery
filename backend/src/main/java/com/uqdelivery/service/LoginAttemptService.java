package com.uqdelivery.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bloquea temporalmente un correo tras varios intentos fallidos (fuerza bruta). En memoria. */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private static class State {
        int failures;
        Instant lockedUntil;
    }

    private final Map<String, State> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String key) {
        State s = attempts.get(key);
        if (s == null || s.lockedUntil == null) return false;
        if (Instant.now().isAfter(s.lockedUntil)) {
            attempts.remove(key);
            return false;
        }
        return true;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, s) -> {
            if (s == null) s = new State();
            s.failures++;
            if (s.failures >= MAX_FAILURES) s.lockedUntil = Instant.now().plus(LOCK_TIME);
            return s;
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
