package com.course.lesson7.redis.controller;

import com.course.lesson7.redis.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<String> createSession(
            @RequestParam Long userId,
            @RequestBody(required = false) Map<String, Object> attributes) {
        String sessionId = sessionService.createSession(userId, attributes);
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionId);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionService.SessionData> getSession(@PathVariable String sessionId) {
        SessionService.SessionData sessionData = sessionService.getSession(sessionId);
        if (sessionData != null) {
            return ResponseEntity.ok(sessionData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{sessionId}/attributes/{attributeName}")
    public ResponseEntity<Void> setAttribute(
            @PathVariable String sessionId,
            @PathVariable String attributeName,
            @RequestBody Object value) {
        sessionService.setAttribute(sessionId, attributeName, value);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{sessionId}/attributes/{attributeName}")
    public ResponseEntity<Object> getAttribute(
            @PathVariable String sessionId,
            @PathVariable String attributeName) {
        Object value = sessionService.getAttribute(sessionId, attributeName);
        return ResponseEntity.ok(value);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> invalidateSession(@PathVariable String sessionId) {
        sessionService.invalidateSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Set<Object>> getActiveSessions(@PathVariable Long userId) {
        Set<Object> sessions = sessionService.getActiveSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> invalidateAllUserSessions(@PathVariable Long userId) {
        sessionService.invalidateAllUserSessions(userId);
        return ResponseEntity.ok().build();
    }
}
