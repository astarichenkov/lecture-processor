package com.lectureprocessor.chat.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.lectureprocessor.chat.config.ChatProperties;
import com.lectureprocessor.chat.llm.ChatTurn;

/**
 * Bounded in-memory conversation store keyed by session id. Old turns are
 * evicted so memory does not grow without bound.
 */
@Component
public class ChatMemoryStore {

    private final int maxMessages;
    private final Map<String, Deque<ChatTurn>> sessions = new ConcurrentHashMap<>();

    public ChatMemoryStore(ChatProperties properties) {
        this.maxMessages = properties.limits().maxHistoryMessages();
    }

    public List<ChatTurn> history(String sessionId) {
        Deque<ChatTurn> deque = sessions.get(sessionId);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

    public void append(String sessionId, ChatTurn turn) {
        Deque<ChatTurn> deque = sessions.computeIfAbsent(sessionId, key -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(turn);
            while (deque.size() > maxMessages) {
                deque.pollFirst();
            }
        }
    }

    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }
}
