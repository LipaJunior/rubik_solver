package com.smse.rubik_solver.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

import org.springframework.stereotype.Service;

import com.smse.rubik_solver.model.Cube;
import com.smse.rubik_solver.model.UserSession;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SessionManager {

    // Maksymalna liczba przechowywanych sesji. Bez limitu mapa roslaby w
    // nieskonczonosc; LinkedHashMap w trybie dostepu usuwa najdawniej uzywana.
    private static final int MAX_SESSIONS = 1000;

    private final Map<String, UserSession> sessions = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, UserSession> eldest) {
                    return size() > MAX_SESSIONS;
                }
            });

    public String createSession() {
        String sessionId = java.util.UUID.randomUUID().toString();
        sessions.put(sessionId, new UserSession(sessionId, new Cube()));
        log.debug("Created new session with ID: {}", sessionId);
        return sessionId;
    }

    public UserSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
}
