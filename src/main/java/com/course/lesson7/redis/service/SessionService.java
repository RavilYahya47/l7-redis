package com.course.lesson7.redis.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_PREFIX = "session:";
    private static final String ACTIVE_SESSIONS_PREFIX = "active_sessions:";
    private static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(30);

    public String createSession(Long userId, Map<String, Object> attributes) {
        String sessionId = UUID.randomUUID().toString();
        String key = SESSION_PREFIX + sessionId;

        SessionData sessionData = SessionData.builder()
                .sessionId(sessionId)
                .userId(userId)
                .attributes(attributes != null ? attributes : new HashMap<>())
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        redisTemplate.opsForValue().set(key, sessionData, DEFAULT_SESSION_TTL);

        String userSessionsKey = ACTIVE_SESSIONS_PREFIX + userId;
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, DEFAULT_SESSION_TTL);

        log.info("Session created for user {}: {}", userId, sessionId);
        return sessionId;
    }

    public SessionData getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        SessionData sessionData = (SessionData) redisTemplate.opsForValue().get(key);

        if (sessionData != null) {
            sessionData.setLastAccessedAt(LocalDateTime.now());
            redisTemplate.opsForValue().set(key, sessionData, DEFAULT_SESSION_TTL);
            log.debug("Session accessed: {}", sessionId);
        } else {
            log.debug("Session not found: {}", sessionId);
        }

        return sessionData;
    }

    public void setAttribute(String sessionId, String attributeName, Object value) {
        SessionData sessionData = getSession(sessionId);

        if (sessionData != null) {
            sessionData.getAttributes().put(attributeName, value);
            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, sessionData, DEFAULT_SESSION_TTL);
            log.debug("Session attribute updated: {} -> {}", sessionId, attributeName);
        } else {
            log.warn("Cannot set attribute on non-existent session: {}", sessionId);
        }
    }

    public Object getAttribute(String sessionId, String attributeName) {
        SessionData sessionData = getSession(sessionId);
        return sessionData != null ? sessionData.getAttributes().get(attributeName) : null;
    }

    public void removeAttribute(String sessionId, String attributeName) {
        SessionData sessionData = getSession(sessionId);

        if (sessionData != null) {
            sessionData.getAttributes().remove(attributeName);
            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, sessionData, DEFAULT_SESSION_TTL);
            log.debug("Session attribute removed: {} -> {}", sessionId, attributeName);
        }
    }

    public void invalidateSession(String sessionId) {
        SessionData sessionData = getSession(sessionId);

        if (sessionData != null) {
            String key = SESSION_PREFIX + sessionId;
            redisTemplate.delete(key);

            String userSessionsKey = ACTIVE_SESSIONS_PREFIX + sessionData.getUserId();
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);

            log.info("Session invalidated: {}", sessionId);
        }
    }

    public Set<Object> getActiveSessions(Long userId) {
        String userSessionsKey = ACTIVE_SESSIONS_PREFIX + userId;
        return redisTemplate.opsForSet().members(userSessionsKey);
    }

    public void invalidateAllUserSessions(Long userId) {
        Set<Object> sessionIds = getActiveSessions(userId);

        if (sessionIds != null) {
            for (Object sessionId : sessionIds) {
                invalidateSession((String) sessionId);
            }
            log.info("All sessions invalidated for user: {}", userId);
        }
    }

    public void extendSession(String sessionId, Duration duration) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.expire(key, duration);
        log.debug("Session extended: {} for {}", sessionId, duration);
    }

    public boolean sessionExists(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionData implements Serializable {
        private String sessionId;
        private Long userId;
        private Map<String, Object> attributes;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessedAt;
    }
}
