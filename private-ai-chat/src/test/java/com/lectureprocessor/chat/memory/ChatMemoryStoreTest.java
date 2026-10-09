package com.lectureprocessor.chat.memory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.lectureprocessor.chat.llm.ChatTurn;
import com.lectureprocessor.chat.support.ChatTestProperties;

class ChatMemoryStoreTest {

    private final ChatMemoryStore store = new ChatMemoryStore(ChatTestProperties.defaults());

    @Test
    void keepsOnlyMostRecentMessages() {
        for (int i = 0; i < 10; i++) {
            store.append("s", new ChatTurn(ChatTurn.ROLE_USER, "m" + i));
        }
        assertThat(store.history("s")).hasSize(6);
        assertThat(store.history("s").getLast().content()).isEqualTo("m9");
    }

    @Test
    void clearsSession() {
        store.append("s", new ChatTurn(ChatTurn.ROLE_USER, "hi"));
        store.clear("s");
        assertThat(store.history("s")).isEmpty();
    }
}
